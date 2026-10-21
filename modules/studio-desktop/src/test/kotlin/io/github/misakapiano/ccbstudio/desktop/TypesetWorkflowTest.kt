@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class, androidx.compose.ui.InternalComposeUiApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.material.*
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Constraints
import com.example.chatbar.desktop.*
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import java.nio.file.*
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.test.*

class TypesetWorkflowTest {
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    private fun root()=Path.of(System.getProperty("preview.test.root")).resolve(newId())
    private suspend fun ImageComposeScene.frames(){repeat(8){render().close();yield()};delay(80);render().close()}
    private fun ImageComposeScene.nodes():List<SemanticsNode>{
        fun walk(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::walk)
        return semanticsOwners.flatMap{walk(it.rootSemanticsNode)}
    }
    private fun SemanticsNode.matches(s:String)=config.getOrNull(SemanticsProperties.Text)?.any{it.text==s}==true || config.getOrNull(SemanticsProperties.ContentDescription)?.contains(s)==true
    private suspend fun ImageComposeScene.click(s:String){val n=nodes().first{it.matches(s) && it.config.getOrNull(SemanticsActions.OnClick)!=null};assertTrue(n.config[SemanticsActions.OnClick].action!!.invoke());frames()}
    private suspend fun ImageComposeScene.field(s:String,value:String){val n=nodes().first{it.config.getOrNull(SemanticsActions.SetText)!=null && (it.matches(s)||it.children.any{it.matches(s)})};assertTrue(n.config[SemanticsActions.SetText].action!!.invoke(AnnotatedString(value)));frames()}
    private suspend fun WorkbenchController.idle(){withTimeout(30000){active?.join()};assertNull(problem,problem)}
    private suspend fun ImageComposeScene.rendered(){withTimeout(30000){while(nodes().none{it.matches("排版编辑画布") && it.config.getOrNull(SemanticsProperties.StateDescription)=="已更新"}){delay(40);frames()}}}
    private fun ImageComposeScene.shot(name:String){val p=Path.of("build/ux-r2a-workbench/$name.png");Files.createDirectories(p.parent);render().use{im->im.encodeToData()!!.use{Files.write(p,it.bytes)}}}

    @Test fun namedProjectLiveVerticalEditingHistoryAndRestart()=runBlocking(awt) {
        val r=root();val w=StudioWorkspace.openStudio(r);val bytes=EditorRenderer.synthetic(600,400);val id=w.importImage("fixture.png",bytes)
        val scope=CoroutineScope(SupervisorJob()+awt);val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),admissionOverride=RequestAdmission{})
        c.selection=setOf(id);c.view="编辑"
        val scene=ImageComposeScene(1320,900){MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel)){Workbench(c,DesktopWindowChrome{},false)}}
        try {
            scene.frames();scene.click("新建编辑工程");scene.field("工程名称","排版回归册");scene.click("建立工程");c.idle();scene.frames()
            assertEquals("排版回归册",c.editor.project!!.name)
            withTimeout(30000){while(scene.nodes().none{it.matches("添加文字")}){delay(100);scene.frames()}}
            scene.click("添加文字");scene.field("文字内容","「你好，世界！」[b]粗体[/]123ABC");scene.click("竖排")
            scene.field("竖排框高度","240")
            assertTrue(c.editor.text!!.vertical)
            scene.rendered();scene.shot("vertical-live");c.editor.drain();assertFalse(c.editor.dirty)
            val text=c.editor.text!!;assertEquals(text,TypesetRepository(w).load(w.state.typesetProjects.single()).pages.single().texts.single())
            scene.field("文字内容","即时改字");c.editor.drain();scene.click("撤销编辑");c.idle();scene.frames();assertEquals(text.text,c.editor.page!!.texts.single().text)
            scene.click("重做编辑");c.idle();scene.frames();assertEquals("即时改字",c.editor.page!!.texts.single().text)
            scene.constraints=Constraints.fixed(700,900);scene.frames();scene.shot("compact-canvas");scene.click("属性");scene.frames();scene.shot("compact-properties")
            assertEquals("即时改字",c.editor.page!!.texts.single().text);assertContentEquals(bytes,w.bytes(id))
            assertEquals(1,c.visibleAssets().size)
        }finally{scene.close();c.close();scope.cancel()}
        StudioWorkspace.openStudio(r).use{again->val p=again.state.typesetProjects.single();assertEquals("排版回归册",p.name);val t=TypesetRepository(again).load(p).pages.single().texts.single();assertEquals("即时改字",t.text);assertTrue(t.vertical);assertContentEquals(bytes,again.bytes(id))}
    }
}
