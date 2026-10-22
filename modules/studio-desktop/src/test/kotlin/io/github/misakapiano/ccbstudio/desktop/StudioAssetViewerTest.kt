@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class,androidx.compose.ui.InternalComposeUiApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.ContextMenuState
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.semantics.*
import com.example.chatbar.desktop.DesktopNovelAiInfrastructure
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import java.nio.file.Path
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.test.*

class StudioAssetViewerTest {
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    private fun root()=Path.of(System.getProperty("preview.test.root"),newId())
    private suspend fun ImageComposeScene.frames(){repeat(4){render(System.nanoTime()).close();yield()};delay(80)}
    private fun ImageComposeScene.nodes():List<SemanticsNode>{
        fun walk(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::walk)
        return semanticsOwners.flatMap{walk(it.rootSemanticsNode)}
    }
    private fun SemanticsNode.matches(s:String)=config.getOrNull(SemanticsProperties.ContentDescription)?.contains(s)==true
    private suspend fun ImageComposeScene.click(s:String){assertTrue(nodes().first{it.matches(s)&&it.config.getOrNull(SemanticsActions.OnClick)!=null}.config[SemanticsActions.OnClick].action!!.invoke());frames()}
    @Test fun hoverAndContextActionsUseTheViewedImageAndPreserveExistingProject()=runBlocking(awt){
        val r=root();val w=StudioWorkspace.openStudio(r);val bytes=EditorRenderer.synthetic(80,60)
        val a=w.importImage("first.png",bytes);val b=w.importImage("second.png",bytes)
        val scope=CoroutineScope(SupervisorJob()+awt);val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root())
        c.editor.create("Existing project",listOf(a));c.active?.join();assertNull(c.problem,c.problem)
        val oldProject=c.editor.project!!.id;c.editor.change{it.copy(mode="storybook")}
        c.current=a;c.selection=setOf(a);c.edit{it.copy(basePrompt="Existing generation draft")};c.flush();val draft=c.draft
        val source=mutableListOf(a,b);val selection=StudioViewerSelection(source,0);source.reverse()
        var closed=0
        val scene=ImageComposeScene(1000,800,coroutineContext=awt){MaterialTheme{StudioAssetViewerContent(c,selection){closed++}}}
        try{
            scene.frames();assertEquals(a,selection.id)
            assertTrue(scene.nodes().none{it.matches("悬浮下一张")})
            val center=scene.nodes().first{it.matches("浏览图片区域")}.boundsInRoot.center
            scene.sendPointerEvent(PointerEventType.Enter,center);scene.sendPointerEvent(PointerEventType.Move,center);scene.frames()
            scene.click("悬浮下一张");assertEquals(b,selection.id);assertEquals(a,c.current)
            scene.sendPointerEvent(PointerEventType.Press,center,buttons=PointerButtons(isSecondaryPressed=true),button=PointerButton.Secondary)
            scene.sendPointerEvent(PointerEventType.Release,center,buttons=PointerButtons(),button=PointerButton.Secondary);scene.frames()
            assertTrue(selection.menu.status is ContextMenuState.Status.Open,"A real secondary click must open the image context menu")
            selection.menu.status=ContextMenuState.Status.Closed;scene.frames()
            val menu=viewerMenu(c,selection.id!!,{},{} ){closed++}
            assertEquals(listOf("复制图片","另存原始文件","进入编辑","APNG 伪装","APNG 还原"),menu.map{it.label})
            menu.single{it.label=="进入编辑"}.onClick();c.active?.join();assertNull(c.problem,c.problem)
            assertEquals(1,closed);assertEquals("编辑",c.view);assertEquals(b,c.editor.document!!.pages.single().assetId)
            assertNotEquals(oldProject,c.editor.project!!.id)
            assertEquals("storybook",TypesetRepository(w).load(w.state.typesetProjects.single{it.id==oldProject}).mode)
            assertEquals(draft,c.draft);assertContentEquals(bytes,w.bytes(a));assertContentEquals(bytes,w.bytes(b))
            viewerMenu(c,b,{},{} ){closed++}.single{it.label=="APNG 伪装"}.onClick()
            assertEquals(listOf(b),c.animation.ids);assertEquals("disguise",c.animation.mode);assertTrue(w.state.savedCopies.isEmpty())
            assertEquals("图库",c.view);assertEquals(2,closed)
            viewerMenu(c,b,{},{} ){closed++}.single{it.label=="APNG 还原"}.onClick()
            assertEquals("restore",c.animation.mode);assertTrue(w.state.savedCopies.isEmpty());assertEquals(3,closed)
            val hold=CompletableDeferred<Unit>();c.run("Fixture task"){hold.await()}
            assertEquals(listOf("复制图片","另存原始文件"),viewerMenu(c,b,{},{} ){}.map{it.label})
            c.active?.cancelAndJoin();assertContentEquals(bytes,w.bytes(b));assertEquals(draft,c.draft)
            val animationScene=ImageComposeScene(1000,800,coroutineContext=awt){MaterialTheme{GalleryAnimationWorkspace(c)}}
            try {
                c.run("导出APNG文件批次"){CompletableDeferred<Unit>().await()}
                animationScene.frames();animationScene.click("停止APNG处理");c.active?.join();assertFalse(c.busy)
            }finally{animationScene.close()}
        }finally{scene.close();c.close();scope.cancel()}
    }
}
