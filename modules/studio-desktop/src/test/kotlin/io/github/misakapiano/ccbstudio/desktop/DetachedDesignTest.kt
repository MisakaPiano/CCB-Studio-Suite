@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class,androidx.compose.ui.InternalComposeUiApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.material.*
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import com.example.chatbar.desktop.*
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import java.nio.file.Path
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.test.*

class DetachedDesignTest {
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    private fun root()=Path.of(System.getProperty("preview.test.root"),newId())
    private suspend fun ImageComposeScene.frames(){repeat(4){render(System.nanoTime()).close();yield()};delay(70)}
    private fun ImageComposeScene.nodes():List<SemanticsNode>{
        fun walk(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::walk)
        return semanticsOwners.flatMap{walk(it.rootSemanticsNode)}
    }
    private fun SemanticsNode.matches(s:String)=config.getOrNull(SemanticsProperties.ContentDescription)?.contains(s)==true||config.getOrNull(SemanticsProperties.Text)?.any{it.text==s}==true
    private suspend fun ImageComposeScene.click(s:String){assertTrue(nodes().first{it.matches(s)&&it.config.getOrNull(SemanticsActions.OnClick)!=null}.config[SemanticsActions.OnClick].action!!.invoke());frames()}
    @Test fun detachingReattachingAndClosingRetainUnsentInputAndNeverSend()=runBlocking(awt){
        val r=root();val w=StudioWorkspace.openStudio(r);val scope=CoroutineScope(SupervisorJob()+awt)
        var requests=0
        val http=okhttp3.OkHttpClient.Builder().addInterceptor{requests++;error("No network allowed while moving design UI")}.build()
        val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),http)
        c.view="AI 设计";c.input("Unsent synthetic scene")
        val main=ImageComposeScene(1280,880,coroutineContext=awt){MaterialTheme{Workbench(c,DesktopWindowChrome{},false)}}
        val detached=ImageComposeScene(1120,820,coroutineContext=awt){MaterialTheme{DesignWorkspace(c,true)}}
        try{
            main.frames();main.click("独立窗口");assertTrue(c.designDetached);assertEquals("创作",c.view)
            detached.frames()
            val field=detached.nodes().first{it.config.getOrNull(SemanticsActions.SetText)!=null&&(it.matches("画面需求 / 继续修改")||it.children.any{child->child.matches("画面需求 / 继续修改")})}
            assertTrue(field.config[SemanticsActions.SetText].action!!.invoke(AnnotatedString("Unsent edit in detached view")))
            detached.frames();assertEquals("Unsent edit in detached view",c.composer)
            detached.click("切回主窗口");assertFalse(c.designDetached);assertEquals("AI 设计",c.view)
            main.frames();assertTrue(main.nodes().any{it.config.getOrNull(SemanticsProperties.EditableText)?.text=="Unsent edit in detached view"})
            main.click("独立窗口");c.dismissDesign();assertFalse(c.designDetached);assertEquals("Unsent edit in detached view",c.composer)
            assertEquals(0,requests);assertTrue(c.state.jobs.isEmpty());c.flush()
        }finally{main.close();detached.close();c.close();scope.cancel()}
        StudioWorkspace.openStudio(r).use{assertEquals("Unsent edit in detached view",it.state.creation!!.composer)}
    }
    @Test fun stopLabelsAlwaysDescribeTheActiveTask()=runBlocking(awt){
        val w=StudioWorkspace.openStudio(root());val scope=CoroutineScope(SupervisorJob()+awt)
        val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root())
        val main=ImageComposeScene(1280,880,coroutineContext=awt){MaterialTheme{Workbench(c,DesktopWindowChrome{},false)}}
        val design=ImageComposeScene(1120,820,coroutineContext=awt){MaterialTheme{DesignWorkspace(c,true)}}
        try{
            for((operation,label)in listOf("AI 设计" to "停止 AI 设计","NovelAI 生成" to "停止生图","刷新账户额度" to "取消刷新额度")){
                val hold=CompletableDeferred<Unit>();c.run(operation){hold.await()}
                assertTrue(c.busy);assertEquals(label,c.cancelLabel);val running=c.active!!
                main.frames();design.frames()
                val owner=if(operation=="AI 设计")design else main
                val other=if(operation=="AI 设计")main else design
                assertTrue(other.nodes().none{it.matches(label)},"A stop action must stay in its own task UI")
                owner.click(label);running.join();assertFalse(c.busy);assertEquals("",c.activeOperation,"Finished task labels must not leak into a later operation")
            }
        }finally{main.close();design.close();c.close();scope.cancel()}
    }
}
