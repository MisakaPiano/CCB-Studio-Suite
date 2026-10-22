@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class,androidx.compose.ui.InternalComposeUiApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.material.*
import androidx.compose.runtime.*
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

class GallerySortingWorkflowTest {
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    private fun root()=Path.of(System.getProperty("preview.test.root"),newId())
    @Test fun controlsStayVisibleWhileHistoryLoadsAndSameDepthAlbumsRememberDifferentSorts()=runBlocking(awt) {
        val r=root();val w=StudioWorkspace.openStudio(r.resolve("workspace"));val bytes=EditorRenderer.synthetic(40,40)
        fun add(folder:String,n:Int)=w.importImage("image$n.png",bytes,buildJsonObject{put("kind","image-ingress");put("version",1);put("source","file");put("sourcePath",r.resolve(folder).resolve("image$n.png").toString())}.toString())
        val first=listOf(add("A",10),add("A",2));val second=listOf(add("B",10),add("B",2))
        val scope=CoroutineScope(SupervisorJob()+awt);val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(r.resolve("runtime")),r.resolve("budget"),admissionOverride=RequestAdmission{error("No HTTP")},pauseCollectionsOnStart=true)
        c.library.refresh();var reads=0;val release=CompletableDeferred<Unit>()
        c.gallery.historyReadOverride={reads++;release.await();readGalleryHistory(w)}
        var mounted by mutableStateOf(true)
        val scene=ImageComposeScene(1320,880,coroutineContext=awt){MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel)){Surface(color=Ink,contentColor=DesktopBootstrapColors.foreground){if(mounted)GalleryWorkspace(c)}}}
        suspend fun frames(){repeat(4){scene.render().close();yield()};delay(120)}
        fun nodes():List<SemanticsNode>{
            fun walk(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::walk)
            return scene.semanticsOwners.flatMap{walk(it.rootSemanticsNode)}
        }
        fun SemanticsNode.matches(text:String)=config.getOrNull(SemanticsProperties.Text)?.any{it.text==text}==true||config.getOrNull(SemanticsProperties.ContentDescription)?.contains(text)==true
        suspend fun click(text:String){nodes().first{it.matches(text)&&it.config.getOrNull(SemanticsActions.OnClick)!=null}.config[SemanticsActions.OnClick].action!!.invoke();frames()}
        suspend fun idle(){c.active?.join();assertNull(c.problem,c.problem);frames()}
        suspend fun open(ids:List<String>){val node=nodes().first{it.config.getOrNull(SemanticsProperties.ContentDescription)?.any{label->label.startsWith("图库相册 ")&&label.removePrefix("图库相册 ") in ids}==true};node.config[SemanticsActions.OnClick].action!!.invoke();frames()}
        var keyA="";var keyB=""
        try {
            frames();click("▸ 筛选与分组");assertTrue(nodes().any{it.matches("折叠为相册")});assertEquals(1,reads)
            val saving=CompletableDeferred<Unit>();c.run("测试保存等待"){saving.await()};frames();assertTrue(c.busy);assertTrue(nodes().any{it.matches("折叠方式 · 完整 ▾")});saving.complete(Unit);idle()
            release.complete(Unit);withTimeout(10000){while(c.gallery.historyCache==null)frames()}
            click("折叠方式 · 完整 ▾");click("目录");idle();click("折叠为相册");idle()
            open(first);keyA=c.gallery.scope.identity;click("本相册默认排序 · 添加时间 ▾");click("文件名");idle();click("降序 ↓");idle()
            assertEquals(GallerySort(GallerySortField.NAME,false),c.gallery.sort);click("返回上层相册")
            open(second);keyB=c.gallery.scope.identity;assertNotEquals(keyA,keyB);click("本相册默认排序 · 添加时间 ▾");click("文件类型");idle();click("返回上层相册")
            open(first);assertEquals(GallerySort(GallerySortField.NAME,false),c.gallery.sort);click("返回上层相册")
            mounted=false;frames();mounted=true;frames();assertEquals(1,reads,"Unchanged gallery re-entry must reuse verified history")
            click("▸ 筛选与分组");frames()
            val shot=Path.of("build/gallery-sorting/sort-controls.png");Files.createDirectories(shot.parent);scene.render().use{it.encodeToData()!!.use{Files.write(shot,it.bytes)}}
            assertTrue(w.state.jobs.isEmpty());(first+second).forEach{assertContentEquals(bytes,w.bytes(it))}
        }finally{release.complete(Unit);scene.close();c.close();scope.cancel()}
        StudioWorkspace.openStudio(r.resolve("workspace")).use{reopened->val prefs=GallerySortPreferences(LibraryRepository(reopened)).load();assertEquals(GallerySort(GallerySortField.NAME,false),prefs[keyA]);assertEquals(GallerySort(GallerySortField.TYPE,true),prefs[keyB])}
    }
}
