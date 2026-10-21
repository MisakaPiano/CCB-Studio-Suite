@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class,androidx.compose.ui.InternalComposeUiApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.material.*
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.*
import com.example.chatbar.desktop.*
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import java.nio.file.*
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.test.*

class LibraryBackupWorkflowTest {
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    private fun root()=Path.of(System.getProperty("preview.test.root"),newId())
    private suspend fun ImageComposeScene.frames(){repeat(5){render(System.nanoTime()).close();yield();delay(25)}}
    private fun ImageComposeScene.nodes():List<SemanticsNode>{
        fun walk(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::walk)
        return semanticsOwners.flatMap{walk(it.rootSemanticsNode)}
    }
    private suspend fun ImageComposeScene.click(label:String){
        nodes().first{(it.config.getOrNull(SemanticsProperties.Text)?.any{t->t.text==label}==true || it.config.getOrNull(SemanticsProperties.ContentDescription)?.contains(label)==true)&&it.config.getOrNull(SemanticsActions.OnClick)!=null}.config[SemanticsActions.OnClick].action!!.invoke();frames()
    }
    @Test fun currentWorkbenchBackupConfirmRestoreAndOpenPreservesSharedAssetsWithoutCredentials():Unit=runBlocking(awt) {
        val w=StudioWorkspace.openStudio(root());val scope=CoroutineScope(SupervisorJob()+awt)
        val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),admissionOverride=RequestAdmission{error("No network")})
        val image=EditorRenderer.synthetic(48,48);val id=w.importImage("shared.png",image)
        val book=TypesetRepository(w).create("整册工程",listOf(id))
        val prompt=c.library.repository.savePrompt("standalone prompt","独立文字","fixture")
        c.library.repository.label(id,"图像别名",listOf("保留标签"))
        c.library.repository.addFolder(Files.createDirectories(root()),"images",false)
        val ref=c.vault.put("SYNTHETIC-BACKUP-NOT-REAL".toByteArray())
        w.save(w.state.copy(accounts=listOf(AccountProfile(alias="test",provider="SYNTHETIC",service="NAI",model="fixture",secretRef=ref))))
        c.input("尚未发送的草稿")
        val parent=Files.createDirectories(root());val b=c.backups;var selected=0;var opened:Path?=null
        b.chooseFolder={_,_->if(selected++%2==0)b.backup else parent}
        b.launchWorkspace={opened=it}
        val scene=ImageComposeScene(1200,900,coroutineContext=awt){MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel)){Workbench(c,DesktopWindowChrome{},false)}}
        try {
            scene.frames();scene.click("资产管理");scene.click("备份与恢复");scene.click("备份图库与工程");c.active!!.join();scene.frames();assertNull(c.problem)
            assertNotNull(b.backup);assertTrue(Files.isRegularFile(b.backup!!.resolve("COMPLETE.txt")))
            assertFalse(Files.exists(b.backup!!.resolve("secrets")))
            scene.click("从备份恢复到新工作区");scene.click("选择恢复位置");val cancelled=b.choice!!.destination;scene.click("取消恢复");assertFalse(Files.exists(cancelled))
            scene.click("从备份恢复到新工作区");scene.click("选择恢复位置");scene.click("确认恢复新工作区");c.active!!.join();scene.frames();assertNull(c.problem)
            val restored=requireNotNull(b.restored);assertNotEquals(w.root,restored)
            scene.click("打开恢复工作区");assertEquals(restored,opened)
            StudioWorkspace.openStudio(restored).use{next->
                assertContentEquals(image,next.bytes(id));assertEquals("尚未发送的草稿",next.state.creation!!.composer)
                assertEquals("standalone prompt",LibraryRepository(next).prompt(prompt).text)
                assertEquals(AssetLabels("图像别名",listOf("保留标签")),LibraryRepository(next).load().labels[id])
                assertEquals(book,next.state.typesetProjects.single())
                assertEquals(TypesetRepository(w).load(book),TypesetRepository(next).load(book))
                assertTrue(LibraryRepository(next).automaticFolders().isEmpty())
                assertEquals(ref,next.state.accounts.single().secretRef)
                assertFails{SecretVault(next.root.resolve("secrets")).use(ref){}}
            }
            c.vault.use(ref){assertEquals("SYNTHETIC-BACKUP-NOT-REAL",it.decodeToString())}
            assertContentEquals(image,w.bytes(id))
            val out=Path.of("build/ux-library-backup/restored-workspace.png");Files.createDirectories(out.parent)
            scene.render().use{im->im.encodeToData()!!.use{Files.write(out,it.bytes)}}
        }finally{scene.close();c.close();scope.cancel()}
    }
    @Test fun restoreRejectsCurrentRootExistingDestinationAndCorruptBackupWithoutPublishing():Unit=runBlocking(awt) {
        val w=StudioWorkspace.openStudio(root());val scope=CoroutineScope(SupervisorJob()+awt)
        val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),admissionOverride=RequestAdmission{error("No network")})
        val id=w.importImage("retained.png",EditorRenderer.synthetic(32,32));val original=w.bytes(id)
        val b=c.backups;val parent=Files.createDirectories(root());var target=w.root;var calls=0
        b.chooseFolder={_,_->if(calls++%2==0)b.backup else target}
        try {
            b.createBackup();c.active!!.join();assertNotNull(b.backup)
            b.selectRestore();b.selectDestination();assertNull(b.choice);assertTrue(c.problem!!.contains("当前工作区之外"))
            target=parent;b.selectRestore();b.selectDestination();val destination=b.choice!!.destination;Files.createDirectory(destination);Files.writeString(destination.resolve("keep.txt"),"retained")
            b.restore();c.active!!.join();assertNull(b.restored);assertTrue(c.problem!!.startsWith("CONFLICT"));assertEquals("retained",Files.readString(destination.resolve("keep.txt")))
            Files.writeString(b.backup!!.resolve("unexpected.txt"),"unknown retained")
            b.selectRestore();b.selectDestination();val corruptDestination=b.choice!!.destination;b.restore();c.active!!.join()
            assertNull(b.restored);assertTrue(c.problem!!.startsWith("CORRUPT"));assertFalse(Files.exists(corruptDestination));assertTrue(b.receipt.startsWith("恢复未完成"))
            assertContentEquals(original,w.bytes(id));assertTrue(Files.exists(b.backup!!.resolve("unexpected.txt")))
        }finally{c.close();scope.cancel()}
    }
    @Test fun sourceAndDestinationAreSeparateAndLatestBackupCannotBeConfusedWithEmptyParent():Unit=runBlocking(awt) {
        val w=StudioWorkspace.openStudio(root());val scope=CoroutineScope(SupervisorJob()+awt)
        val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),admissionOverride=RequestAdmission{error("No network")})
        val b=c.backups;val parent=Files.createDirectories(root());var calls=0
        try {
            b.createBackup();c.active!!.join();val complete=Files.readAllBytes(b.backup!!.resolve("COMPLETE.txt"))
            b.chooseFolder={_,_->calls++;parent}
            b.selectRestore();assertEquals(1,calls);assertNull(b.restoreSource);assertNull(b.choice)
            assertTrue(c.problem!!.contains("所选目录不是完整"));assertEquals(0,Files.list(parent).use{it.count()}.toInt())
            c.problem=null;b.useLatestBackup();assertEquals(1,calls);assertEquals(b.backup,b.restoreSource);assertNull(b.choice)
            b.chooseFolder={title,_->calls++;assertTrue(title.startsWith("第 2 步"));null}
            b.selectDestination();assertEquals(2,calls);assertEquals(b.backup,b.restoreSource);assertNull(b.choice)
            b.chooseFolder={_,_->calls++;parent};b.selectDestination()
            assertEquals(b.backup,b.choice!!.backup);assertEquals(parent,b.choice!!.destination.parent)
            val target=b.choice!!.destination;b.cancelRestore();assertNull(b.choice);assertFalse(Files.exists(target))
            assertContentEquals(complete,Files.readAllBytes(b.backup!!.resolve("COMPLETE.txt")))
        }finally{c.close();scope.cancel()}
    }
    @Test fun legacyBackupRestoresThenCopiesToCurrentFormatWithoutTouchingBackup():Unit=runBlocking(awt) {
        val original=EditorRenderer.synthetic(32,32);var id=""
        val source=StudioWorkspace.open(root()).use{old->id=old.importImage("legacy.png",original);old.newProject("旧工程",listOf(id));old.backup()}
        val complete=Files.readAllBytes(source.resolve("COMPLETE.txt"))
        val w=StudioWorkspace.openStudio(root());val scope=CoroutineScope(SupervisorJob()+awt)
        val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),admissionOverride=RequestAdmission{error("No network")})
        val b=c.backups;val parent=Files.createDirectories(root());var calls=0
        b.chooseFolder={_,_->if(calls++%2==0)source else parent}
        try {
            b.selectRestore();b.selectDestination();val first=b.choice!!.destination;b.restore();c.active!!.join();assertNull(c.problem)
            assertEquals("CCBStudioSuite/WindowsPreview/SYNTHETIC_ONLY/v1",Files.readString(first.resolve("preview-root.txt")))
            assertNotEquals(first,b.restored)
            StudioWorkspace.openStudio(requireNotNull(b.restored)).use{restored->assertEquals(StudioWorkspace.DATA_VERSION,restored.state.version);assertContentEquals(original,restored.bytes(id));assertEquals("旧工程",restored.state.projects.single().name)}
            assertContentEquals(complete,Files.readAllBytes(source.resolve("COMPLETE.txt")));assertTrue(w.state.projects.isEmpty())
        }finally{c.close();scope.cancel()}
    }
}
