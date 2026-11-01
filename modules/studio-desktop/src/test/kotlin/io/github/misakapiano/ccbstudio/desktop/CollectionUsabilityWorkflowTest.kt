@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class,androidx.compose.ui.InternalComposeUiApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.*
import com.example.chatbar.desktop.*
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import java.nio.file.*
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.test.*

class CollectionUsabilityWorkflowTest {
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    private fun root()=Path.of(System.getProperty("preview.test.root"),newId())
    private suspend fun ImageComposeScene.frames(){repeat(3){render(System.nanoTime()).close();yield()};delay(70)}
    private fun ImageComposeScene.nodes():List<SemanticsNode>{
        fun walk(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::walk)
        return semanticsOwners.flatMap{walk(it.rootSemanticsNode)}
    }
    private fun SemanticsNode.matches(s:String)=config.getOrNull(SemanticsProperties.Text)?.any{it.text==s}==true||config.getOrNull(SemanticsProperties.ContentDescription)?.contains(s)==true
    private fun ImageComposeScene.has(s:String)=nodes().any{it.matches(s)}
    private suspend fun ImageComposeScene.click(s:String){val n=nodes().first{it.matches(s)&&it.config.getOrNull(SemanticsActions.OnClick)!=null};assertTrue(n.config[SemanticsActions.OnClick].action!!.invoke());frames()}
    private suspend fun ImageComposeScene.until(check:()->Boolean){withTimeout(20000){while(!check())frames()};frames()}
    private fun ImageComposeScene.shot(name:String){val p=Path.of("H:/CCB-Studio-Suite/.local/daily-clean-usable-ui-20261101/components/$name.png");Files.createDirectories(p.parent);render(System.nanoTime()).use{it.encodeToData()!!.use{data->Files.write(p,data.bytes)}}}
    @Test fun addPauseResumeAndStopAreUsableWithoutExposingReferenceMaintenance():Unit=runBlocking(awt) {
        val base=root();val folder=Files.createDirectories(base.resolve("new-pictures"));val raw=EditorRenderer.synthetic(39,40);val first=Files.write(folder.resolve("a.png"),raw);val nested=Files.createDirectories(folder.resolve("child"));Files.write(nested.resolve("b.png"),EditorRenderer.synthetic(41,40))
        val w=StudioWorkspace.openStudio(base.resolve("workspace"));val scope=CoroutineScope(SupervisorJob()+awt);val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(base.resolve("runtime")),base.resolve("budget"),pauseCollectionsOnStart=true,admissionOverride=RequestAdmission{error("No HTTP")})
        c.view="资产管理";c.library.chooseCollectionDirectory={folder}
        val scene=ImageComposeScene(1320,880){StudioTheme{Workbench(c,DesktopWindowChrome{},false)}}
        try {
            scene.frames();assertTrue(scene.has("自动收集"));assertFalse(scene.has("目录入口"));assertTrue(scene.has("添加文件夹"));assertFalse(scene.has("添加Prompt文件夹"));assertFalse(scene.has("启用默认收集目录"));assertFalse(scene.has("图库引用 · 历史导入（不额外存图）"));scene.shot("collections-empty")
            scene.click("添加文件夹");assertTrue(scene.has("包含子文件夹"));scene.shot("collections-add");scene.click("添加");c.active!!.join();scene.frames()
            assertTrue(c.library.state!!.folders.single().recursive);assertTrue(c.library.sessionPaused);assertTrue(c.assets.isEmpty())
            scene.click("继续自动收集");c.active!!.join();scene.until{c.assets.size==2 && !c.library.collectionBusy};scene.shot("collections-active")
            val id=c.library.state!!.folders.single().id;val ids=c.assets.map{it.request().id().toString()};scene.click("暂停收集");c.active!!.join();scene.frames();assertTrue(c.library.repository.isFolderPaused(id));assertTrue(c.library.repository.automaticFolders().isEmpty())
            Files.write(folder.resolve("c.png"),EditorRenderer.synthetic(43,40));scene.click("继续收集");c.active!!.join();scene.until{c.assets.size==3 && !c.library.collectionBusy}
            scene.click("停止收集");assertTrue(scene.has("确认停止"));assertEquals(1,c.library.state!!.folders.size);scene.shot("collections-stop-confirmation");scene.click("取消");assertEquals(1,c.library.state!!.folders.size)
            scene.click("停止收集");scene.click("确认停止");c.active!!.join();scene.frames();assertTrue(c.library.state!!.folders.isEmpty());assertEquals(3,c.assets.size);ids.forEach{assertTrue(w.bytes(it).isNotEmpty())};assertContentEquals(raw,Files.readAllBytes(first))
            scene.click("▸ 更多选项");assertTrue(scene.has("添加Prompt文件夹"));scene.shot("collections-more")
            c.view="创作";scene.frames();scene.click("Prompt 资产");assertEquals("Prompt 资产",c.library.tab);assertTrue(scene.has("独立 Prompt 正文"))
        }finally{scene.close();c.close();scope.cancel()}
    }
    @Test fun folderImportUsesOneDialogAndFlatAndNestedAlbumsWithoutMonitoring():Unit=runBlocking(awt) {
        val base=root();val folder=Files.createDirectories(base.resolve("one-shot"));val raw=EditorRenderer.synthetic(45,40);Files.write(folder.resolve("root.png"),raw);Files.write(Files.createDirectories(folder.resolve("child")).resolve("nested.png"),raw)
        val flat=planGalleryDirectory(folder,false,LibraryState());assertEquals(1,flat.folders.size);assertEquals(1,flat.files.size);assertEquals(flat.folders.single().id,flat.files.single().second)
        val w=StudioWorkspace.openStudio(base.resolve("workspace"));val scope=CoroutineScope(SupervisorJob()+awt);val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(base.resolve("runtime")),base.resolve("budget"),pauseCollectionsOnStart=true,admissionOverride=RequestAdmission{error("No HTTP")});c.view="图库";c.gallery.chooseImportDirectory={folder}
        val scene=ImageComposeScene(1320,880){StudioTheme{Workbench(c,DesktopWindowChrome{},false)}}
        try {
            scene.frames();scene.click("导入图片");assertTrue(scene.has("导入文件夹…"));assertFalse(scene.has("导入目录（本层）"));assertFalse(scene.has("导入文件夹及子文件夹（保留结构）"));scene.click("导入文件夹…");assertTrue(scene.has("包含子文件夹"));scene.shot("gallery-folder-import-confirmation");scene.click("开始导入");c.active!!.join();scene.frames();assertNull(c.problem)
            assertEquals(2,c.gallery.imports.size);val state=c.library.repository.load();assertEquals(2,state.virtualAlbums.size);assertEquals(2,VirtualAlbums.members(state.virtualAlbums,state.virtualAlbums.single{it.name=="one-shot"}.id).size);assertTrue(state.folders.isEmpty());assertTrue(c.library.repository.automaticFolders().isEmpty());assertFalse(Files.exists(base.resolve("workspace-collections/imports")));c.gallery.imports.mapNotNull{it.assetId}.forEach{assertContentEquals(raw,w.bytes(it))}
        }finally{scene.close();c.close();scope.cancel()}
    }
}
