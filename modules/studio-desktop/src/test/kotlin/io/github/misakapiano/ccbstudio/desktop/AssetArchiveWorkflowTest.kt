@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class,androidx.compose.ui.InternalComposeUiApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import com.example.chatbar.desktop.*
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import java.nio.file.*
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.test.*

class AssetArchiveWorkflowTest {
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    private fun root()=Path.of(System.getProperty("preview.test.root"),newId())
    private suspend fun ImageComposeScene.frames(){repeat(4){render(System.nanoTime()).close();yield()};delay(100);render(System.nanoTime()).close()}
    private fun ImageComposeScene.nodes():List<SemanticsNode>{
        fun walk(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::walk)
        return semanticsOwners.flatMap{walk(it.rootSemanticsNode)}
    }
    private fun SemanticsNode.matches(s:String)=config.getOrNull(SemanticsProperties.Text)?.any{it.text==s}==true||config.getOrNull(SemanticsProperties.ContentDescription)?.contains(s)==true
    private suspend fun ImageComposeScene.click(s:String){val n=nodes().first{it.matches(s)&&it.config.getOrNull(SemanticsActions.OnClick)!=null};assertTrue(n.config[SemanticsActions.OnClick].action!!.invoke());frames()}
    private suspend fun WorkbenchController.idle(){active?.join();assertNull(problem)}
    @Test fun confirmCancelRestoreAndAliasTagChoicesPreserveExistingProject()=runBlocking(awt){
        val data=root();val w=StudioWorkspace.openStudio(data);val original=EditorRenderer.synthetic(48,48);val image=w.importImage("image.png",original)
        val other=w.importImage("other.png",EditorRenderer.synthetic(64,48));val repo=LibraryRepository(w);repo.label(image,"蓝色场景",listOf("月光"))
        val prompt=repo.savePrompt("quiet moonlight","场景词","manual")
        val scope=CoroutineScope(SupervisorJob()+awt);val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),admissionOverride=RequestAdmission{error("No network")})
        var picker by mutableStateOf(false);var choice=listOf<String>()
        val scene=ImageComposeScene(1400,1200,coroutineContext=awt){MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel)){
            Workbench(c,DesktopWindowChrome{},false);if(picker)SharedAssetPicker(c,true,{picker=false}){choice=it}
        }}
        try{
            c.editor.create("Kept editor",listOf(image));c.idle();c.view="图库";c.selection=setOf(image);scene.frames()
            scene.openGalleryTools();scene.click("所选资产移入回收站");scene.click("取消");assertTrue(c.library.archived.isEmpty())
            scene.openGalleryTools();scene.click("所选资产移入回收站");scene.click("确认移入回收站");c.idle();scene.frames()
            assertEquals(setOf(image),c.library.archived);assertFalse(scene.nodes().any{it.matches("图库图片 $image")});assertTrue(c.selection.isEmpty())
            assertEquals(image,c.editor.document!!.pages.single().assetId);assertContentEquals(original,w.bytes(image))
            picker=true;scene.frames();assertFalse(scene.nodes().any{it.matches("选用资产 $image")});assertTrue(scene.nodes().any{it.matches("选用资产 $other")});scene.click("取消")
            scene.openGalleryTools();scene.click("资产回收站");scene.click("恢复资产 ${image.take(8)}");c.idle();scene.frames();assertTrue(c.library.archived.isEmpty())
            picker=true;scene.frames()
            val field=scene.nodes().first{it.config.getOrNull(SemanticsActions.SetText)!=null&&it.matches("查找文件名、别名或标签")}
            field.config[SemanticsActions.SetText].action!!.invoke(AnnotatedString("月光"));scene.frames()
            assertTrue(scene.nodes().any{it.matches("蓝色场景")});assertFalse(scene.nodes().any{it.matches("选用资产 $other")})
            scene.click("选用资产 $image");scene.click("确认选用资产");assertEquals(listOf(image),choice)
            c.library.tab="Prompt 资产";scene.frames();scene.click("场景词");scene.click("Prompt 移入回收站");scene.click("确认移入回收站");c.idle();scene.frames()
            assertTrue(c.library.prompts.none{it.request().id().toString()==prompt});assertEquals("quiet moonlight",repo.prompt(prompt).text)
            c.library.tab="回收站";scene.frames();val shot=Path.of("build/asset-archive/recycle.png");Files.createDirectories(shot.parent);scene.render(System.nanoTime()).use{it.encodeToData()!!.use{Files.write(shot,it.bytes)}}
        }finally{scene.close();c.close();scope.cancel()}
        StudioWorkspace.openStudio(data).use{again->assertEquals(setOf(prompt),AssetArchive(again).ids());assertContentEquals(original,again.bytes(image));assertEquals("蓝色场景",LibraryRepository(again).load().labels[image]!!.alias)}
    }
    @Test fun archiveMetadataFailureShowsOriginalsAndDisablesMutation()=runBlocking(awt){
        val w=StudioWorkspace.openStudio(root());val image=w.importImage("image.png",EditorRenderer.synthetic(32,32));val repo=LibraryRepository(w)
        repo.update{it.copy(preferences=it.preferences+(AssetArchive.KEY to "{\"version\":99,\"future\":true}"))}
        val scope=CoroutineScope(SupervisorJob()+awt);val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),admissionOverride=RequestAdmission{error("No network")})
        val scene=ImageComposeScene(1400,1200,coroutineContext=awt){MaterialTheme{Workbench(c,DesktopWindowChrome{},false)}}
        try{c.view="图库";c.selection=setOf(image);scene.frames()
            assertNotNull(c.library.archiveError);assertTrue(scene.nodes().any{it.matches("图库图片 $image")})
            scene.openGalleryTools();val action=scene.nodes().first{it.matches("所选资产移入回收站")&&it.config.getOrNull(SemanticsActions.OnClick)!=null}
            assertTrue(action.config.contains(SemanticsProperties.Disabled));assertTrue(w.bytes(image).isNotEmpty())
        }finally{scene.close();c.close();scope.cancel()}
    }
    private suspend fun ImageComposeScene.openGalleryTools(){
        if(nodes().none{it.matches("APNG伪装 / 还原")})click("图片工具")
    }
}
