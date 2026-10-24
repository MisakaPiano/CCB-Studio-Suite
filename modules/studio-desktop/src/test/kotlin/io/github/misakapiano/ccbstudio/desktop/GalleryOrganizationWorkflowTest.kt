@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class,androidx.compose.ui.InternalComposeUiApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.material.*
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.*
import com.example.chatbar.desktop.*
import io.github.misakapiano.ccbstudio.core.*
import com.example.chatbar.domain.image.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import kotlinx.coroutines.*
import java.nio.file.*
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.test.*

class GalleryOrganizationWorkflowTest {
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    @Test fun realGalleryKeepsToolsAndSupportsTreeMembershipUndoAndCollapse()=runBlocking(awt){
        val root=Path.of(System.getProperty("preview.test.root"),newId());val w=StudioWorkspace.openStudio(root);val bytes=EditorRenderer.synthetic(48,48)
        val account=AccountProfile(alias="synthetic",service="NAI",provider="NOVELAI",endpoint="https://image.novelai.net",model=NovelAiImageModel.V4_5_FULL.apiId)
        val origin=BatchResultOrigin(newId(),newId(),"湖畔晨光",newId(),"横",1,123,"风景构图")
        val raw=documentJson.encodeToString(GenerationReceipt(recipe=NovelAiStudioDraft(basePrompt="landscape, soft light").toRecipe(),account=account,request=buildJsonObject{},batchOrigin=origin))
        val id=w.importImage("synthetic-original.png",bytes,raw);val album=VirtualAlbum(name="精选")
        w.save(w.state.copy(jobs=listOf(JobRecord(account=account,recipe=raw,results=listOf(id),state="COMPLETED"))))
        LibraryRepository(w).update{it.copy(virtualAlbums=listOf(album))}
        val scope=CoroutineScope(SupervisorJob()+awt);val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root),root,admissionOverride=RequestAdmission{error("No HTTP")},pauseCollectionsOnStart=true)
        c.view="图库";val scene=ImageComposeScene(1320,880){MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel)){Workbench(c,DesktopWindowChrome{},false)}}
        suspend fun frames(){repeat(3){scene.render(System.nanoTime()).close();yield()};delay(100)}
        fun nodes():List<SemanticsNode>{
            fun walk(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::walk)
            return scene.semanticsOwners.flatMap{walk(it.rootSemanticsNode)}
        }
        fun matches(n:SemanticsNode,text:String)=n.config.getOrNull(SemanticsProperties.Text)?.any{it.text==text}==true||n.config.getOrNull(SemanticsProperties.ContentDescription)?.contains(text)==true
        suspend fun click(text:String){val n=nodes().first{matches(it,text)&&it.config.getOrNull(SemanticsActions.OnClick)!=null};n.config[SemanticsActions.OnClick].action!!.invoke();frames()}
        suspend fun idle(){c.active?.join();assertNull(c.problem);frames()}
        try{
            withTimeout(15000){while(c.gallery.foldPreferences==null||c.library.state==null){frames()}}
            assertTrue(nodes().any{matches(it,"图库组织树")});assertTrue(nodes().any{matches(it,"调整组织树宽度")})
            val start=nodes().first{matches(it,"调整组织树宽度")}.boundsInRoot.center
            val beforeWidth=c.gallery.treeWidth
            scene.sendPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Press,start,button=androidx.compose.ui.input.pointer.PointerButton.Primary);frames()
            for(step in 1..5){scene.sendPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Move,start+androidx.compose.ui.geometry.Offset(step*12f,0f),button=androidx.compose.ui.input.pointer.PointerButton.Primary);frames()}
            scene.sendPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Release,start+androidx.compose.ui.geometry.Offset(60f,0f),button=androidx.compose.ui.input.pointer.PointerButton.Primary);frames()
            assertTrue(c.gallery.treeWidth>beforeWidth,"组织树宽度必须通过真实指针拖动改变")
            assertTrue(nodes().any{matches(it,"导入图片")});assertTrue(nodes().any{matches(it,"操作")})
            c.selection=setOf(id);frames();click("操作");click("加入虚拟相册…");click("加入相册 精选");click("确认");idle()
            assertEquals(listOf(id),c.library.repository.load().virtualAlbums.single().images)
            c.library.undo();idle();assertTrue(c.library.repository.load().virtualAlbums.single().images.isEmpty())
            c.library.redo();idle();assertEquals(listOf(id),c.library.repository.load().virtualAlbums.single().images)
            click("展开虚拟相册");click("精选");assertEquals("album",c.gallery.organizationKind)
            click("收起组织树");assertFalse(c.gallery.treeOpen);click("展开组织树");assertTrue(c.gallery.treeOpen)
            c.gallery.treeWidth=240f;frames();assertTrue(nodes().any{matches(it,"图库组织树")})
            val path=Path.of("build/gallery-organization-113/native-gallery.png");Files.createDirectories(path.parent);scene.render(System.nanoTime()).use{it.encodeToData()!!.use{Files.write(path,it.bytes)}}
            assertContentEquals(bytes,w.bytes(id));assertEquals(listOf(id),c.state.jobs.single().results)
        }finally{scene.close();c.close();scope.cancel();w.close()}
    }
}
