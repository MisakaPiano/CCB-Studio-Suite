@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class,androidx.compose.ui.InternalComposeUiApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.material.*
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import com.example.chatbar.desktop.*
import com.example.chatbar.domain.image.*
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.nio.file.*
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.test.*

class GalleryAlbumWorkflowTest {
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    private fun root()=Path.of(System.getProperty("preview.test.root"),newId())
    private suspend fun ImageComposeScene.frames(){repeat(3){render(System.nanoTime()).close();yield()};delay(180);render(System.nanoTime()).close()}
    private fun ImageComposeScene.nodes():List<SemanticsNode>{
        fun walk(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::walk)
        return semanticsOwners.flatMap{walk(it.rootSemanticsNode)}
    }
    private fun SemanticsNode.matches(text:String)=config.getOrNull(SemanticsProperties.Text)?.any{it.text==text}==true || config.getOrNull(SemanticsProperties.ContentDescription)?.contains(text)==true
    private suspend fun ImageComposeScene.click(text:String){val n=nodes().first{it.matches(text)&&it.config.getOrNull(SemanticsActions.OnClick)!=null};assertTrue(n.config[SemanticsActions.OnClick].action!!.invoke());frames()}
    private suspend fun ImageComposeScene.field(label:String,value:String){val n=nodes().first{it.config.getOrNull(SemanticsActions.SetText)!=null&&(it.matches(label)||it.children.any{it.matches(label)})};n.config[SemanticsActions.SetText].action!!.invoke(AnnotatedString(value));frames()}
    private fun ImageComposeScene.shot(name:String){val path=Path.of("build/gallery-albums/$name.png");Files.createDirectories(path.parent);render(System.nanoTime()).use{it.encodeToData()!!.use{Files.write(path,it.bytes)}}}
    @Test fun sourceAlbumsNavigateSelectFilterAndPersistDepthPreferencesWithoutChangingRecipes()=runBlocking(awt){
        val data=root();val w=StudioWorkspace.openStudio(data);val bytes=EditorRenderer.synthetic(80,80)
        val account=AccountProfile(alias="synthetic",service="NAI",provider="NOVELAI",endpoint="https://image.novelai.net",model=NovelAiImageModel.V4_5_FULL.apiId)
        val jobs=mutableListOf<JobRecord>();val ids=mutableListOf<String>()
        for((n,prompt) in listOf("forest","forest ","ocean").withIndex()) {
            val recipe=NovelAiStudioDraft(basePrompt=prompt).toRecipe()
            val raw=documentJson.encodeToString(GenerationReceipt(recipe=recipe,account=account,request=buildJsonObject{put("input",prompt)}))
            val id=w.importImage("$n.png",bytes,raw);ids+=id
            jobs+=JobRecord(account=account,recipe=raw,results=listOf(id),state="COMPLETED",createdAt=1767225600000L+n)
        }
        val unknown=w.importImage("independent.png",bytes);w.save(w.state.copy(jobs=jobs))
        val scope=CoroutineScope(SupervisorJob()+awt);val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),admissionOverride=RequestAdmission{error("No HTTP")})
        c.view="图库";var preview:Pair<List<String>,Int>?=null;c.gallery.previewOverride={a,b->preview=a to b}
        val before=c.draft;val scene=ImageComposeScene(1320,1000){MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel)){Workbench(c,DesktopWindowChrome{},false)}}
        suspend fun until(check:()->Boolean){withTimeout(20000){while(!check())scene.frames()};scene.frames()}
        suspend fun idle(){until{!c.busy};assertNull(c.problem)}
        try {
            until{c.gallery.foldPreferences!=null};scene.click("折叠方式 · 完整 ▾");scene.click("基础");idle()
            scene.click("折叠为相册");idle()
            until{scene.nodes().any{it.config.getOrNull(SemanticsProperties.ContentDescription)?.any{d->d.startsWith("图库相册 ")}==true}}
            val cover=scene.nodes().first{it.config.getOrNull(SemanticsProperties.ContentDescription)?.any{d->d.startsWith("图库相册 ")}==true}.config[SemanticsProperties.ContentDescription].first().removePrefix("图库相册 ")
            scene.click("选择图库图片 $cover");assertEquals(ids.take(2).toSet(),c.selection)
            scene.shot("source-folded-album");scene.click("图库相册 $cover");assertEquals(1,c.gallery.depth);assertTrue(c.selection.isEmpty());assertFalse(c.gallery.fold.enabled)
            scene.click("全选");assertEquals(ids.take(2).toSet(),c.selection)
            scene.click("折叠方式 · 完整 ▾");scene.click("日");idle();scene.click("折叠为相册");idle()
            scene.click("返回上层相册");assertEquals(0,c.gallery.depth);assertEquals(NovelAiHistoryFoldType.BASE,c.gallery.fold.type)
            scene.field("日期（YYYY / YYYY-MM / YYYY-MM-DD）","2026-02-30");scene.click("应用日期");assertEquals("",c.gallery.date);assertNotNull(c.gallery.historyProblem)
            scene.field("日期（YYYY / YYYY-MM / YYYY-MM-DD）","2026-01");scene.click("应用日期");scene.click("全选");assertEquals(ids.toSet(),c.selection)
            scene.click("清除日期");scene.click("图库图片 $unknown");assertEquals(unknown,preview!!.first[preview!!.second]);assertEquals(4,preview!!.first.size)
            assertEquals(before,c.draft);assertEquals(jobs,w.state.jobs);(ids+unknown).forEach{assertContentEquals(bytes,w.bytes(it))}
            scene.shot("source-folding-controls")
        }finally{scene.close();c.close();scope.cancel();w.close()}
        StudioWorkspace.openStudio(data).use {r->val prefs=GalleryFoldPreferences(LibraryRepository(r)).load();assertEquals(NovelAiHistoryFoldType.BASE,prefs.getValue(0).type);assertEquals(NovelAiHistoryFoldType.DAY,prefs.getValue(1).type);assertTrue(prefs.values.all{it.enabled});assertEquals(jobs,r.state.jobs)}
    }
}
