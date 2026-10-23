@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class,androidx.compose.ui.InternalComposeUiApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.material.*
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.*
import com.example.chatbar.desktop.*
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import java.nio.file.*
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.test.*

class QueueCompactWorkflowTest {
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    @Test fun queueSelectionRemovalAndSubsetCopyKeepOriginalsAndCompactCalendarWorks()=runBlocking(awt) {
        val root=Path.of(System.getProperty("preview.test.root"),newId());val w=StudioWorkspace.openStudio(root.resolve("workspace"));val bytes=EditorRenderer.synthetic(480,640)
        val ids=(1..24).map{w.importImage("synthetic-$it.png",bytes)};val scope=CoroutineScope(SupervisorJob()+awt)
        val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root.resolve("runtime")),root.resolve("budget"),admissionOverride=RequestAdmission{error("No HTTP")},pauseCollectionsOnStart=true)
        c.view="图库";c.animation.open(ids)
        val scene=ImageComposeScene(1320,880,coroutineContext=awt){MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel)){Workbench(c,DesktopWindowChrome{},false)}}
        suspend fun frames(){repeat(4){scene.render(System.nanoTime()).close();yield()};delay(100)}
        fun nodes():List<SemanticsNode> {
            fun walk(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::walk)
            return scene.semanticsOwners.flatMap{walk(it.rootSemanticsNode)}
        }
        suspend fun click(label:String){nodes().first{(it.config.getOrNull(SemanticsProperties.Text)?.any{t->t.text==label}==true||it.config.getOrNull(SemanticsProperties.ContentDescription)?.contains(label)==true)&&it.config.getOrNull(SemanticsActions.OnClick)!=null}.config[SemanticsActions.OnClick].action!!.invoke();frames()}
        fun shot(name:String){val p=Path.of("build/queue-compact/$name.png");Files.createDirectories(p.parent);scene.render(System.nanoTime()).use{it.encodeToData()!!.use{Files.write(p,it.bytes)}}}
        try {
            c.animation.awaitPreview();frames()
            click("勾选模式");click("选择队列项目 ${ids.last()}");assertEquals(setOf(ids.last()),c.animation.queueSelection)
            c.animation.toggleQueue(ids[21],range=true);assertEquals(ids.takeLast(3).toSet(),c.animation.queueSelection)
            c.animation.selectIndex(0);c.animation.awaitPreview();assertEquals(3,c.animation.queueSelection.size)
            var copied=emptyList<Path>();c.animation.copyFiles={copied=it};c.animation.export(true,selectedOnly=true);c.active!!.join();assertNull(c.problem)
            assertEquals(3,copied.size);val receipt=Json.parseToJsonElement(Files.readString(c.animation.completed!!.resolve("COMPLETE.json"))).jsonObject
            assertEquals(ids.takeLast(3).toSet(),receipt.getValue("files").jsonArray.map{it.jsonObject.getValue("sourceId").jsonPrimitive.content}.toSet())
            c.animation.selectIndex(23);c.animation.awaitPreview();frames();shot("queue-selection")
            click("移除所选（保留原件）");c.animation.awaitPreview();assertEquals(ids.dropLast(3),c.animation.ids);assertTrue(c.animation.queueSelection.isEmpty())
            click("全选");assertEquals(21,c.animation.queueSelection.size);click("全不选");assertTrue(c.animation.queueSelection.isEmpty())
            c.animation.selectQueue(true);c.animation.removeQueue();frames();assertTrue(c.animation.ids!!.isEmpty());assertFalse(c.animation.previewBusy);assertEquals(0,c.animation.progress)
            click("返回图库");frames();click("日历 ▾");click("今天");assertEquals(java.time.LocalDate.now().toString(),c.gallery.date)
            click("清除日期");assertEquals("",c.gallery.date);shot("gallery-calendar-layout")
            assertTrue(nodes().any{it.config.getOrNull(SemanticsProperties.ContentDescription)?.contains("展开搜索")==true})
            // Render the exact dialog content separately; this scene does not own native Window semantics.
            var text="synthetic long query";var closed=false
            val expanded=ImageComposeScene(760,420,coroutineContext=awt){MaterialTheme{GalleryLongSearchContent(text,{text=it},{closed=true})}}
            try {
                expanded.render(System.nanoTime()).close();yield();expanded.render(System.nanoTime()).close()
                fun walk(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::walk)
                val all=expanded.semanticsOwners.flatMap{walk(it.rootSemanticsNode)}
                val field=all.first{it.config.getOrNull(SemanticsActions.SetText)!=null}
                field.config[SemanticsActions.SetText].action!!.invoke(androidx.compose.ui.text.AnnotatedString("changed query"));assertEquals("changed query",text)
                all.first{it.config.getOrNull(SemanticsProperties.ContentDescription)?.contains("完成")==true}.config[SemanticsActions.OnClick].action!!.invoke();assertTrue(closed)
            }finally{expanded.close()}
            ids.forEach{assertContentEquals(bytes,w.bytes(it))};assertEquals(24,SharedAssets.images(w).size);assertTrue(w.state.savedCopies.isEmpty())
        }finally{scene.close();c.close();scope.cancel()}
    }
}
