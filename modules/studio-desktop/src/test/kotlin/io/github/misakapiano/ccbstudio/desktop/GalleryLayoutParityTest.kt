@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class,androidx.compose.ui.InternalComposeUiApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.material.*
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.sp
import com.example.chatbar.desktop.*
import com.example.chatbar.domain.image.*
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.nio.file.*
import java.awt.image.BufferedImage
import java.awt.Color
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.test.*

class GalleryLayoutParityTest {
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    private fun synthetic(index:Int):ByteArray {
        val image=BufferedImage(240,240,BufferedImage.TYPE_INT_RGB);val g=image.createGraphics()
        g.paint=java.awt.GradientPaint(0f,0f,Color(73+index,89,94),240f,240f,Color(158,138+index,92));g.fillRect(0,0,240,240)
        g.color=Color(239,233,211);g.fillOval(34,30,52,52);g.color=Color(44,74,77);g.fillOval(-35,157,330,160);g.dispose()
        return ByteArrayOutputStream().also{ImageIO.write(image,"png",it)}.toByteArray()
    }
    @Test fun approvedHierarchyAndDividerWorkWithTwentyImagesAndAllMenuStyles()=runBlocking(awt) {
        val root=Path.of(System.getProperty("preview.test.root"),newId());val w=StudioWorkspace.openStudio(root)
        val account=AccountProfile(alias="合成演示账户",service="NAI",provider="NOVELAI",endpoint="https://image.novelai.net",model=NovelAiImageModel.V4_5_FULL.apiId)
        val batches=listOf(newId(),newId());val names=listOf("湖畔晨光","林间小屋","海边黄昏","雨后街角","夜色步道");val groups=names.map{newId()}
        val ids=mutableListOf<String>();val jobs=mutableListOf<JobRecord>();val originals=mutableMapOf<String,ByteArray>()
        for(group in names.indices)repeat(4){round->
            val index=group*4+round;val batch=if(group<3)0 else 1
            val raw=documentJson.encodeToString(GenerationReceipt(recipe=NovelAiStudioDraft(basePrompt="landscape, lake, morning atmosphere").toRecipe(),account=account,request=buildJsonObject{},batchOrigin=BatchResultOrigin(batches[batch],groups[group],names[group],newId(),"横",round+1,100L+index,if(batch==0)"风景构图"else"雨后街景")))
            val bytes=synthetic(index);val id=w.importImage("${names[group]} ${round+1}.png",bytes,raw);ids+=id;originals[id]=bytes
            jobs+=JobRecord(account=account,recipe=raw,results=listOf(id),state="COMPLETED",createdAt=1792800000000L+index)
        }
        val vision=AccountProfile(alias="合成视觉模型",service="LLM",provider="OPENAI_COMPATIBLE",endpoint="https://example.invalid/v1",model="gpt-4o",isMultimodal=true)
        val project=TypesetRepository(w).create("合成编辑工程",listOf(ids[0]))
        w.save(w.state.copy(jobs=jobs,accounts=listOf(account,vision)))
        val satisfied=VirtualAlbum(name="满意作品",images=listOf(ids[0],ids[4],ids[13]));val cover=VirtualAlbum(name="封面候选",parent=satisfied.id,images=listOf(ids[0],ids[13]));val pending=VirtualAlbum(name="待精修",images=listOf(ids[1],ids[9]))
        LibraryRepository(w).update{it.copy(virtualAlbums=listOf(satisfied,pending,cover))}
        val scope=CoroutineScope(SupervisorJob()+awt);val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root),root,admissionOverride=RequestAdmission{error("No real requests")},pauseCollectionsOnStart=true)
        c.view="图库";val scene=ImageComposeScene(1320,880){MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel)){Workbench(c,DesktopWindowChrome{},false)}}
        suspend fun frames(){repeat(3){scene.render(System.nanoTime()).close();yield()};delay(60)}
        fun nodes():List<SemanticsNode>{
            fun walk(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::walk)
            return scene.semanticsOwners.flatMap{walk(it.rootSemanticsNode)}
        }
        fun matches(n:SemanticsNode,text:String)=n.config.getOrNull(SemanticsProperties.Text)?.any{it.text==text}==true||n.config.getOrNull(SemanticsProperties.ContentDescription)?.contains(text)==true
        suspend fun click(text:String){nodes().first{matches(it,text)&&it.config.getOrNull(SemanticsActions.OnClick)!=null}.config[SemanticsActions.OnClick].action!!.invoke();frames()}
        suspend fun clickPrefix(text:String){nodes().first{n->n.config.getOrNull(SemanticsProperties.ContentDescription)?.any{it.startsWith(text)}==true&&n.config.getOrNull(SemanticsActions.OnClick)!=null}.config[SemanticsActions.OnClick].action!!.invoke();frames()}
        fun size(text:String):Float {
            val n=nodes().first{it.config.getOrNull(SemanticsProperties.Text)?.any{t->t.text==text}==true&&it.config.getOrNull(SemanticsActions.GetTextLayoutResult)!=null}
            val layouts=mutableListOf<TextLayoutResult>();assertTrue(n.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(layouts));return layouts.first().layoutInput.style.fontSize.value
        }
        fun snapshot(name:String){val path=Path.of("build/gallery-layout-114/$name.png");Files.createDirectories(path.parent);scene.render(System.nanoTime()).use{it.encodeToData()!!.use{data->Files.write(path,data.bytes)}}}
        suspend fun dismiss(){scene.sendKeyEvent(KeyEvent(Key.Escape,KeyEventType.KeyDown));scene.sendKeyEvent(KeyEvent(Key.Escape,KeyEventType.KeyUp));frames()}
        try {
            withTimeout(15000){while(c.gallery.foldPreferences==null||c.library.state==null||c.gallery.historyCache==null){frames()}}
            assertEquals(20,readGalleryHistory(w).batchOrigins.size)
            listOf("全部资产 · 20","生成来源","批次","自定义组织","虚拟相册","项目与章节","新建组织").forEach{label->assertTrue(nodes().any{matches(it,label)},label)}
            assertFalse(nodes().any{matches(it,"收起")||matches(it,"展开或收起组织树")})
            assertTrue(c.gallery.expandedOrganizations.isEmpty());click("展开批次");click("展开自动批次");click("展开虚拟相册");click("展开满意作品");
            click("批次");assertEquals(2,organizationFolders(c.gallery,c.library.state!!.virtualAlbums,readGalleryHistory(w)).size)
            assertTrue(nodes().any{matches(it,"12 张")});assertTrue(nodes().any{matches(it,"8 张")});snapshot("batch-root")
            val card=nodes().first{it.config.getOrNull(SemanticsProperties.ContentDescription)?.any{t->t.startsWith("图库相册 ")}==true}.boundsInRoot.center
            scene.sendPointerEvent(PointerEventType.Press,card,button=PointerButton.Primary);frames();scene.sendPointerEvent(PointerEventType.Release,card,button=PointerButton.Primary)
            withTimeout(3000){while(c.gallery.organizationKind=="batches")frames()};assertEquals("batch",c.gallery.organizationKind,"中央卡片也必须进入批次，不能退化成普通图片")
            click("批次")
            nodes().first{n->n.config.getOrNull(SemanticsProperties.Text)?.any{it.text.startsWith("风景构图")}==true&&n.config.getOrNull(SemanticsActions.OnClick)!=null}.config[SemanticsActions.OnClick].action!!.invoke();frames();assertEquals("batch",c.gallery.organizationKind);assertTrue(nodes().any{matches(it,"湖畔晨光")});snapshot("batch-groups")
            click("满意作品");assertEquals("album",c.gallery.organizationKind);snapshot("virtual-album")
            val divider=nodes().first{matches(it,"组织树分割线")}.boundsInRoot
            scene.sendPointerEvent(PointerEventType.Move,divider.center);frames();snapshot("divider-hover")
            // Actual pointer clicks use the same divider location to close and reopen.
            scene.sendPointerEvent(PointerEventType.Press,divider.center,button=PointerButton.Primary);scene.sendPointerEvent(PointerEventType.Release,divider.center,button=PointerButton.Primary);frames();assertFalse(c.gallery.treeOpen)
            val reopen=nodes().first{matches(it,"展开组织树")}.boundsInRoot.center
            scene.sendPointerEvent(PointerEventType.Press,reopen,button=PointerButton.Primary);scene.sendPointerEvent(PointerEventType.Release,reopen,button=PointerButton.Primary);frames();assertTrue(c.gallery.treeOpen);assertEquals("album",c.gallery.organizationKind)
            click("全部来源 ▾");assertEquals(13f,size("✓ 全部来源"));assertEquals(13f,size("目录收集"))
            val menuItem=nodes().first{matches(it,"目录收集")&&it.config.getOrNull(SemanticsActions.OnClick)!=null}
            assertTrue(menuItem.boundsInRoot.height<=36f,"单行菜单必须使用桌面紧凑行距，而非48dp触摸行高")
            snapshot("source-menu");dismiss()
            click("同时包含 ▾");assertEquals(13f,size("包含任一"));snapshot("tag-mode-menu");dismiss()
            click("操作");assertEquals(13f,size("新建虚拟相册…"));snapshot("operations-menu");dismiss()
            click("导入图片");assertEquals(13f,size("导入目录（本层）"));snapshot("import-menu");dismiss()
            c.view="创作";frames();clickPrefix("生图模型 · ");assertEquals(13f,size("✓ ${c.draft.selectedModel.displayName}"));snapshot("creation-model-menu");dismiss()
            c.view="AI 设计";frames();clickPrefix("设计历史 · ");snapshot("design-history-menu");dismiss()
            c.view="编辑";frames();clickPrefix("编辑工程 · ");assertEquals(13f,size(project.name));snapshot("editor-project-menu");dismiss()
            c.view="资产管理";c.library.tab="目录入口";frames();clickPrefix("入口类型 · ");assertEquals(13f,size("✓ 图片资产收集"));snapshot("collection-type-menu");dismiss()
            c.library.tab="回收站";frames();click("类型 · 全部 ▾");assertEquals(13f,size("图片"));snapshot("recycle-type-menu");dismiss()
            c.view="账户";frames();clickPrefix("使用独立视觉模型 · ");assertEquals(13f,size(vision.alias));snapshot("account-vision-menu");dismiss()
            c.view="创作";c.batch.shown=true;frames();click("添加 ▾");assertEquals(13f,size("空白分组"));snapshot("batch-add-menu");dismiss();c.batch.shown=false
            c.view="图库";c.animation.open(listOf(ids[0]));frames();click("添加 ▾");assertEquals(13f,size("添加目录（本层）"));snapshot("apng-add-menu");dismiss();c.animation.close()
            c.view="图库";frames();click("批次");frames()
            for((id,bytes)in originals)assertContentEquals(bytes,w.bytes(id));assertEquals(20,c.state.jobs.size)
            val report=Path.of("build/gallery-layout-114/fixture.json");Files.writeString(report,buildJsonObject{put("root",root.toString());put("images",20);put("batches",2);put("groups",5);put("albums",3);put("realCalls",0)}.toString())
        }finally{scene.close();c.close();scope.cancel();w.close()}
        Unit
    }
}
