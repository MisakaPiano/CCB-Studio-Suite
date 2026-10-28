@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class,androidx.compose.ui.InternalComposeUiApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.material.*
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import io.github.misakapiano.ccbstudio.core.*
import com.example.chatbar.desktop.DesktopNovelAiInfrastructure
import kotlinx.coroutines.*
import java.nio.file.*
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.math.abs
import kotlin.test.*

class TypesetToolbarWorkflowTest {
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    private fun ImageComposeScene.nodes():List<SemanticsNode>{
        fun walk(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::walk)
        return semanticsOwners.flatMap{walk(it.rootSemanticsNode)}
    }
    private fun SemanticsNode.matches(s:String)=config.getOrNull(SemanticsProperties.Text)?.any{it.text==s}==true||config.getOrNull(SemanticsProperties.ContentDescription)?.contains(s)==true
    private fun ImageComposeScene.node(s:String)=nodes().first{it.matches(s)}
    private suspend fun ImageComposeScene.frames(){repeat(3){androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications();render(System.nanoTime()).close();yield()};delay(70);androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications();render(System.nanoTime()).close()}
    private suspend fun ImageComposeScene.click(s:String){val n=nodes().first{it.matches(s)&&it.config.getOrNull(SemanticsActions.OnClick)!=null};assertTrue(n.config[SemanticsActions.OnClick].action!!.invoke());frames()}
    private fun ImageComposeScene.shot(name:String){val p=Path.of(System.getProperty("preview.test.root")).resolve("editor-toolbar/$name.png");Files.createDirectories(p.parent);render(System.nanoTime()).use{image->image.encodeToData()!!.use{Files.write(p,it.bytes)}}}
    private suspend fun <T> editor(block:suspend(WorkbenchController,ImageComposeScene,ByteArray,String)->T):T{
        val r=Path.of(System.getProperty("preview.test.root")).resolve(newId());val w=StudioWorkspace.openStudio(r.resolve("workspace"));val bytes=EditorRenderer.synthetic(480,320);val id=w.importImage("synthetic-toolbar.png",bytes);val scope=CoroutineScope(SupervisorJob()+awt)
        val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(r.resolve("catalog")),r.resolve("budget"),admissionOverride=RequestAdmission{error("No service calls")})
        c.editor.create("合成工程 · 工程栏整合检查",listOf(id,id));withTimeout(30000){c.active?.join()};assertNull(c.problem)
        val scene=ImageComposeScene(1320,950,coroutineContext=awt){StudioTheme{Surface{TypesetWorkspace(c)}}}
        try{scene.frames();withTimeout(30000){while(c.editor.canvasSource==null)scene.frames()};return block(c,scene,bytes,id)}finally{scene.close();c.close();scope.cancel();w.close()}
    }
    @Test fun singleProjectHeaderAndCanvasToolbarKeepSecondaryActionsReachableWithoutExpandedRows()=runBlocking(awt){editor{c,scene,bytes,id->
        val top=scene.node("工程主工具栏").boundsInRoot;val canvas=scene.node("排版编辑画布").boundsInRoot
        val primary=listOf("新建编辑工程","命名并归档","导出","废弃当前工程")
        primary.forEach{label->val actions=scene.nodes().filter{it.matches(label)&&it.config.getOrNull(SemanticsActions.OnClick)!=null};assertEquals(1,actions.size,label);assertTrue(actions.single().boundsInRoot.top>=top.top&&actions.single().boundsInRoot.bottom<=top.bottom)}
        assertTrue(scene.node("当前工程信息").boundsInRoot.width>500f)
        assertTrue(scene.node("废弃当前工程").boundsInRoot.right<scene.node("导出").boundsInRoot.left)
        assertTrue(scene.node("导出").boundsInRoot.right>top.right-15f)
        assertTrue(top.height<60);assertTrue(scene.nodes().none{it.matches("工程选项")||it.matches("工程交换文件")||it.matches("打开本机工程列表")})
        for(label in listOf("撤销编辑","重做编辑","完整预览","适应画布")){val n=scene.node(label);assertTrue(n.boundsInRoot.top>=canvas.bottom-1,"$label belongs below canvas");assertEquals(1,scene.nodes().count{it.matches(label)&&it.config.getOrNull(SemanticsActions.OnClick)!=null})}
        assertTrue(abs(scene.node("完整预览").boundsInRoot.top-scene.node("适应画布").boundsInRoot.top)<1)
        scene.shot("single-header-and-bottom-toolbar")
        val document=c.editor.document;val project=c.editor.project
        scene.click("工程菜单")
        for(label in listOf("打开本机工程","导入…","当前工程设置…","收起当前工程","保存工程"))assertEquals(1,scene.nodes().count{it.matches(label)&&it.config.getOrNull(SemanticsActions.OnClick)!=null},label)
        assertTrue(scene.nodes().none{it.matches("移出工程列表")});scene.shot("grouped-project-menu");scene.click("当前工程设置…");scene.click("工程保存与分享说明");assertTrue(scene.nodes().any{it.config.getOrNull(SemanticsProperties.Text)?.any{t->"当前资料位置" in t.text}==true});scene.click("知道了")
        scene.click("工程菜单");scene.click("当前工程设置…");scene.click("名称与组织…");scene.click("取消设置")
        scene.click("工程菜单");scene.click("打开本机工程");assertTrue(scene.nodes().any{it.matches("打开本机工程 · ${project!!.name}")});scene.click("归档历史");assertTrue(scene.nodes().none{it.matches("返回编辑")});assertTrue(scene.nodes().none{it.matches("打开本机工程 · ${project!!.name}")});scene.click("进行中／已收起");scene.click("关闭列表")
        assertEquals(1,scene.nodes().count{it.matches("从资产库追加")&&it.config.getOrNull(SemanticsActions.OnClick)!=null})
        assertEquals(document,c.editor.document);assertEquals(project,c.editor.project);assertContentEquals(bytes,c.workspace.bytes(id))
    }}
    @Test fun collapsedProjectsReopenFromActiveListAndArchivesStayInHistory()=runBlocking(awt){editor{c,scene,bytes,id->
        suspend fun idle(){withTimeout(30000){c.active?.join()};assertNull(c.problem);scene.frames()}
        val p=c.editor.project!!;val d=c.editor.document!!
        scene.click("工程菜单");scene.click("收起当前工程");idle();assertNull(c.editor.project)
        scene.click("工程菜单");scene.click("打开本机工程");scene.click("打开本机工程 · ${p.name}");idle();assertEquals(p.current,c.editor.project!!.current);assertEquals(d,c.editor.document)
        scene.click("命名并归档");scene.click("保存并归档");idle();assertNull(c.editor.project)
        scene.click("工程菜单");scene.click("打开本机工程");assertTrue(scene.nodes().none{it.matches("打开本机工程 · ${p.name}")})
        scene.click("归档历史");withTimeout(30000){while(scene.nodes().none{it.matches("重新打开工程")&&it.config.getOrNull(SemanticsProperties.Disabled)==null})scene.frames()}
        val reopen=scene.node("重新打开工程").boundsInRoot;assertTrue(reopen.height>=25f&&reopen.bottom<=scene.node("关闭列表").boundsInRoot.top)
        scene.shot("archive-tab-nonempty");scene.click("重新打开工程");idle()
        assertEquals(p.id,c.editor.project!!.id);assertEquals(p.current,c.editor.project!!.current);assertEquals(d,c.editor.document);assertContentEquals(bytes,c.workspace.bytes(id))
        scene.click("废弃当前工程");idle();assertNotNull(c.editor.deletionPlan);c.editor.cancelDeletion();scene.frames();assertEquals(p.id,c.editor.project!!.id)
    }}
    @Test fun previewHeaderMaximizeRestoreClosePreservesPageSelectionAndProject()=runBlocking(awt){editor{c,scene,bytes,id->
        val before=c.editor.document;val project=c.editor.project;val selected=c.editor.selectedPages;val current=c.editor.pageId
        scene.click("完整预览");withTimeout(30000){while(scene.nodes().none{it.matches("完整页面预览")})scene.frames()}
        val normal=scene.node("预览窗口").boundsInRoot;assertEquals("普通大小",scene.node("预览窗口").config[SemanticsProperties.StateDescription]);scene.shot("preview-normal-header")
        scene.click("最大化预览窗口");val expanded=scene.node("预览窗口").boundsInRoot;assertEquals("已最大化",scene.node("预览窗口").config[SemanticsProperties.StateDescription]);assertTrue(expanded.width>normal.width&&expanded.height>normal.height)
        scene.shot("preview-maximized-header");scene.click("还原预览窗口");val restored=scene.node("预览窗口").boundsInRoot;assertTrue(abs(restored.width-normal.width)<1&&abs(restored.height-normal.height)<1)
        scene.click("关闭预览窗口");assertTrue(scene.nodes().none{it.matches("预览窗口")});scene.click("完整预览");scene.click("关闭预览窗口")
        scene.click("导出");scene.click("图片／PDF 等成品");scene.click("最大化预览窗口");scene.shot("export-preview-maximized-header");scene.click("关闭预览窗口")
        assertEquals(before,c.editor.document);assertEquals(project,c.editor.project);assertEquals(selected,c.editor.selectedPages);assertEquals(current,c.editor.pageId);assertContentEquals(bytes,c.workspace.bytes(id))
    }}
}
