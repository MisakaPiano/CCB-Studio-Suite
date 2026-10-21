@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class, androidx.compose.ui.InternalComposeUiApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Constraints
import com.example.chatbar.desktop.*
import com.example.chatbar.domain.image.*
import com.sun.net.httpserver.HttpServer
import io.github.misakapiano.ccbstudio.core.*
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.file.*
import java.util.concurrent.CopyOnWriteArrayList
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.test.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.msgpack.core.MessagePack

/** Actual Suite controller and Compose tree. Loopback replaces only the HTTP destination. */
class WorkbenchWorkflowTest {
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    private fun root()=Paths.get(System.getProperty("preview.test.root")).resolve(newId())
    private suspend fun ImageComposeScene.frames(){repeat(6){render(System.nanoTime()).close();yield()};delay(180);render(System.nanoTime()).close()}
    private fun ImageComposeScene.nodes():List<SemanticsNode>{
        fun walk(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::walk)
        return semanticsOwners.flatMap{walk(it.rootSemanticsNode)}
    }
    private fun SemanticsNode.matches(text:String)=config.getOrNull(SemanticsProperties.Text)?.any{it.text==text}==true || config.getOrNull(SemanticsProperties.ContentDescription)?.contains(text)==true
    private suspend fun ImageComposeScene.click(text:String,index:Int=0){val n=nodes().filter{it.matches(text)&&it.config.getOrNull(SemanticsActions.OnClick)!=null}[index];assertTrue(n.config[SemanticsActions.OnClick].action!!.invoke());frames()}
    private suspend fun ImageComposeScene.field(label:String,text:String,index:Int=0){val n=nodes().filter{it.config.getOrNull(SemanticsActions.SetText)!=null && (it.matches(label)||it.children.any{child->child.matches(label)})}[index];assertTrue(n.config[SemanticsActions.SetText].action!!.invoke(AnnotatedString(text)));frames()}
    private suspend fun WorkbenchController.idle(){withTimeout(30000){active?.join()};assertNull(problem,problem)}
    private fun ImageComposeScene.shot(name:String){val out=Path.of("build/ux-r1a-workbench/$name.png");Files.createDirectories(out.parent);render(System.nanoTime()).use{im->im.encodeToData()!!.use{Files.write(out,it.bytes)}}}
    private fun sse(text:String):ByteArray {
        val json=buildJsonObject {
            put("choices",buildJsonArray {
                add(buildJsonObject {
                    put("delta",buildJsonObject {put("content",text)})
                    put("finish_reason","stop")
                })
            })
        }
        return ("data: $json\n\ndata: [DONE]\n\n").toByteArray()
    }
    private fun frame(png:ByteArray):ByteArray{val p=MessagePack.newDefaultBufferPacker();p.packMapHeader(2);p.packString("event_type");p.packString("final");p.packString("image");p.packBinaryHeader(png.size);p.writePayload(png);p.close();val b=p.toByteArray();return ByteBuffer.allocate(4+b.size).putInt(b.size).put(b).array()}

    @Test fun accountsDesignApplyGenerateGalleryAndRestart()=runBlocking(awt){
        val r=root();val png=EditorRenderer.synthetic(96,96);val requests=CopyOnWriteArrayList<String>();var designRequests=0
        val server=HttpServer.create(InetSocketAddress("127.0.0.1",0),0)
        server.createContext("/"){e->val body=e.requestBody.readBytes().decodeToString();requests+=body
            val bytes=if(body.contains("\"messages\""))sse(if(++designRequests==1)"{\"sceneDescription\":\"Quiet room\",\"queries\":[]}" else "{\"baseCaption\":\"quiet room, window\",\"characters\":[]}")else frame(png)
            e.sendResponseHeaders(200,bytes.size.toLong());e.responseBody.use{it.write(bytes)}};server.start()
        val client=studioHttpClient().newBuilder().addInterceptor{chain->chain.proceed(chain.request().newBuilder().url("http://127.0.0.1:${server.address.port}/").removeHeader("Authorization").build())}.build()
        val scope=CoroutineScope(SupervisorJob()+awt);val w=StudioWorkspace.openStudio(r)
        val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),client,RequestAdmission{})
        val scene=ImageComposeScene(1240,880,coroutineContext=awt){MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel)){Workbench(c,DesktopWindowChrome{},false)}}
        var asset="";var copied="";c.gallery.copyText={copied=it}
        try{
            scene.frames();scene.click("账户")
            scene.field("档案名称","Local LLM");scene.field("HTTPS API 地址","https://fixture.invalid/v1");scene.field("模型 ID","fixture");scene.field("API 凭据","LOCAL_TEST_ONLY")
            scene.click("安全保存并选用");c.idle();scene.frames();assertEquals(1,w.state.accounts.size)
            scene.click("新建档案");scene.click("NAI");scene.field("档案名称","Local NAI");scene.field("API 凭据","LOCAL_TEST_ONLY")
            scene.click("安全保存并选用");c.idle();scene.frames();assertEquals(2,w.state.accounts.size)
            scene.click("AI 设计");scene.field("画面需求 / 继续修改","Quiet room");scene.click("发送");c.idle();scene.frames()
            assertEquals("",c.composer);assertEquals("",c.draft.basePrompt);assertEquals(NovelAiDesignTurnStatus.COMPLETED,w.state.creation!!.conversations.single().turns.single().status)
            scene.shot("design-candidate");scene.click("应用候选到创作");assertEquals("",c.draft.basePrompt);scene.shot("apply-difference");scene.click("保留当前草稿");assertEquals("",c.draft.basePrompt)
            scene.click("应用候选到创作");scene.click("确认应用");c.idle();scene.frames();assertEquals("quiet room, window",c.draft.basePrompt)
            assertNotNull(DesignProvenance(w).current())
            scene.field("基础 Prompt","quiet room, window, warm light")
            scene.click("生成图片");c.idle();scene.frames();assertEquals(1,c.assets.size);asset=c.current!!;assertContentEquals(png,w.bytes(asset));scene.shot("generated-original")
            scene.click("图库");scene.click("全选");assertEquals(setOf(asset),c.selection);scene.click("反选");assertTrue(c.selection.isEmpty());scene.click("全选");scene.click("全不选");assertTrue(c.selection.isEmpty())
            withTimeout(30000){while(scene.nodes().none{it.matches("查看 AI 设计过程")})scene.frames()}
            assertEquals("已核验 · 应用后有修改",readImagePrompt(c,asset).context.parameters["AI 设计关联"])
            scene.click("查看 AI 设计过程")
            withTimeout(30000){while(scene.nodes().none{it.matches("应用后又有修改：以最终生成内容和请求快照为准。")})scene.frames()}
            assertTrue(scene.nodes().any{it.matches("quiet room, window, warm light")})
            scene.click("复制生成 基础 Prompt");assertEquals("quiet room, window, warm light",copied);scene.shot("design-generated-origin")
            scene.click("设计过程");assertTrue(scene.nodes().any{it.matches("Quiet room")});assertTrue(scene.nodes().any{it.matches("本轮候选被明确应用")});scene.shot("design-origin-history");scene.click("关闭设计来源")
            scene.click("全选");scene.click("所选图片多 Prompt 参照");c.idle();scene.frames()
            val promptId=c.library.repository.prompts().single().request().id().toString()
            scene.click("设计来源 ${promptId.take(8)}")
            withTimeout(30000){while(scene.nodes().none{it.matches("应用后又有修改：以最终生成内容和请求快照为准。")})scene.frames()}
            scene.click("关闭设计来源");assertEquals("quiet room, window, warm light",c.draft.basePrompt)
            assertEquals(3,requests.size);assertFalse(w.state.jobs.single().recipe.contains("LOCAL_TEST_ONLY"));assertEquals("COMPLETED",w.state.jobs.single().state)
        }finally{scene.close();c.close();scope.cancel();server.stop(0)}
        StudioWorkspace.openStudio(r).use{again->
            assertEquals("quiet room, window, warm light",again.state.creation!!.draft.basePrompt);assertEquals(2,again.state.accounts.size);assertContentEquals(png,again.bytes(asset));assertEquals(asset,again.state.jobs.single().results.single())
            assertEquals("quiet room, window",DesignProvenance(again).forGeneration(again.state.jobs.single())!!.applied.base)
            // Seed a persistent update fixture, not evidence of GUI typesetting acceptance.
            val repository=TypesetRepository(again)
            val project=repository.create("更新保全测试册",listOf(asset))
            repository.update(project.id,project.revision){document->document.copy(pages=document.pages.map{page->page.copy(
                caption="更新前的小人书说明。",texts=listOf(TypesetText(text="更新前：中文、かな。",x=48.0,y=12.0,width=70.0,vertical=true,style=TypesetStyle(fontSize=12.0))),
                masks=listOf(TypesetMask(width=20.0,height=20.0)))})}
            val receipt=Path.of("build/ux-r4-update-fixture/${newId()}.json")
            Files.createDirectories(receipt.parent)
            writeUpdateJson(receipt,buildJsonObject{put("dataRoot",r.toAbsolutePath().toString());put("fixture","LOOPBACK_GUI_WITH_SEEDED_TYPESET_DOCUMENT");put("snapshot",updateSnapshot(again))})
        }
    }

    @Test fun accountRefreshAndNewSelectionNeverReuseAnotherAccountsQuota()=runBlocking(awt){
        val r=root();val requests=CopyOnWriteArrayList<String>()
        val server=HttpServer.create(InetSocketAddress("127.0.0.1",0),0)
        server.createContext("/"){e->requests+=e.requestMethod;val bytes="""{"trainingStepsLeft":1234,"tier":3,"active":true}""".toByteArray()
            e.sendResponseHeaders(200,bytes.size.toLong());e.responseBody.use{it.write(bytes)}};server.start()
        val client=studioHttpClient().newBuilder().addInterceptor{chain->chain.proceed(chain.request().newBuilder().url("http://127.0.0.1:${server.address.port}/").removeHeader("Authorization").build())}.build()
        val scope=CoroutineScope(SupervisorJob()+awt);val w=StudioWorkspace.openStudio(r)
        val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),client,RequestAdmission{})
        val scene=ImageComposeScene(1240,880,coroutineContext=awt){MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel)){Workbench(c,DesktopWindowChrome{},false)}}
        var selected=""
        try{
            c.saveAccount(null,"First quota fixture","NAI","https://image.novelai.net",NovelAiImageModel.V4_5_FULL.apiId,"LOCAL_TEST_ONLY");c.idle()
            scene.frames();assertNull(c.usage);assertNull(c.usageAt);assertTrue(requests.isEmpty())
            scene.click("刷新额度");c.idle();scene.frames();assertEquals(1234L,c.usage!!.anlas);assertNotNull(c.usageAt)
            assertTrue(scene.nodes().any{it.matches("额度 1234 Anlas")});scene.shot("quota-first-account")
            scene.click("账户");scene.click("新建档案");scene.click("NAI");scene.field("档案名称","Second quota fixture");scene.field("API 凭据","LOCAL_SECOND_TEST_ONLY")
            scene.click("安全保存并选用");c.idle();scene.click("创作");scene.frames()
            selected=c.naiId!!;assertEquals("Second quota fixture",w.state.accounts.single{it.id==selected}.alias)
            assertNull(c.usage,"A newly selected account must not inherit the previous account's quota")
            assertNull(c.usageAt);assertTrue(scene.nodes().any{it.matches("账户额度未知，未自动请求")})
            assertEquals(listOf("GET"),requests.toList());scene.shot("quota-new-account-unknown")
            scene.click("刷新额度");c.idle();assertNotNull(c.usage)
            val current=w.state.accounts.single{it.id==selected}
            c.saveAccount(current,current.alias,"NAI",current.endpoint,current.model,"LOCAL_REPLACEMENT_TEST_ONLY");c.idle()
            assertNull(c.usage,"Replacing credentials also invalidates quota for the same profile ID");assertNull(c.usageAt)
            assertEquals(2,requests.size);c.flush()
        }finally{scene.close();c.close();scope.cancel();server.stop(0)}
        StudioWorkspace.openStudio(r).use{again->assertEquals(selected,again.state.creation!!.naiAccountId);assertEquals(2,again.state.accounts.size)}
    }

    @Test fun roleEditingReorderDisableAndDeleteReachRequestAndSurviveReopen()=runBlocking(awt){
        val r=root();val png=EditorRenderer.synthetic(48,48);val requests=CopyOnWriteArrayList<String>()
        val server=HttpServer.create(InetSocketAddress("127.0.0.1",0),0)
        server.createContext("/"){e->requests+=e.requestBody.readBytes().decodeToString();val bytes=frame(png)
            e.sendResponseHeaders(200,bytes.size.toLong());e.responseBody.use{it.write(bytes)}};server.start()
        val client=studioHttpClient().newBuilder().addInterceptor{chain->chain.proceed(chain.request().newBuilder().url("http://127.0.0.1:${server.address.port}/").removeHeader("Authorization").build())}.build()
        val scope=CoroutineScope(SupervisorJob()+awt);val w=StudioWorkspace.openStudio(r)
        val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),client,RequestAdmission{})
        val scene=ImageComposeScene(1240,880,coroutineContext=awt){MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel)){Workbench(c,DesktopWindowChrome{},false)}}
        var expected=emptyList<NovelAiCharacterPromptDraft>()
        fun captions(body:String,key:String)=Json.parseToJsonElement(body).jsonObject.getValue("parameters").jsonObject
            .getValue(key).jsonObject.getValue("caption").jsonObject.getValue("char_captions").jsonArray
            .map{it.jsonObject.getValue("char_caption").jsonPrimitive.content}
        try {
            c.saveAccount(null,"Roles fixture","NAI","https://image.novelai.net",NovelAiImageModel.V4_5_FULL.apiId,"LOCAL_TEST_ONLY");c.idle()
            scene.frames();scene.field("基础 Prompt","two people in a room")
            scene.click("添加角色");scene.field("角色正向","first person, red coat");scene.field("角色负面","first unwanted")
            val first=c.draft.characters.single().id
            scene.click("添加角色");scene.field("角色正向","second person, blue coat",1);scene.field("角色负面","second unwanted",1)
            val second=c.draft.characters.last().id
            scene.click("↑",1);assertEquals(listOf(second,first),c.draft.characters.map{it.id})
            scene.click("⌃",0);scene.click("⌄");assertEquals("second person, blue coat",c.draft.characters.first().prompt)
            scene.click("✓",0);assertFalse(c.draft.characters.first().enabled)
            scene.click("添加角色");val removed=c.draft.characters.last().id;scene.click("删除",2)
            assertFalse(c.draft.characters.any{it.id==removed});assertEquals(2,c.draft.characters.size)
            scene.click("生成图片");c.idle();scene.frames()
            assertEquals(listOf("first person, red coat"),captions(requests.single(),"v4_prompt"))
            assertEquals(listOf("first unwanted"),captions(requests.single(),"v4_negative_prompt"))
            assertFalse(requests.single().contains("second person"));assertFalse(requests.single().contains("second unwanted"))
            scene.shot("role-disabled-request")
            scene.click("×");scene.click("生成图片");c.idle();scene.frames()
            assertEquals(listOf("second person, blue coat","first person, red coat"),captions(requests.last(),"v4_prompt"))
            assertEquals(listOf("second unwanted","first unwanted"),captions(requests.last(),"v4_negative_prompt"))
            scene.click("✓",0);expected=c.draft.characters.toList();c.flush();scene.shot("role-reordered-persisted")
            assertEquals(2,requests.size);assertEquals(2,w.state.jobs.size)
            c.assets.forEach{assertContentEquals(png,w.bytes(it.request().id().toString()))}
        } finally {scene.close();c.close();scope.cancel();server.stop(0)}
        StudioWorkspace.openStudio(r).use{again->
            assertEquals(expected,again.state.creation!!.draft.characters)
            assertEquals(listOf("COMPLETED","COMPLETED"),again.state.jobs.map{it.state})
            val reopenedScope=CoroutineScope(SupervisorJob()+awt)
            val reopened=WorkbenchController(again,reopenedScope,DesktopNovelAiInfrastructure(root()),root(),admissionOverride=RequestAdmission{error("Reopen must not submit a request")})
            val reopenedScene=ImageComposeScene(1240,880,coroutineContext=awt){MaterialTheme{Workbench(reopened,DesktopWindowChrome{},false)}}
            try {reopenedScene.frames();assertEquals(expected,reopened.draft.characters);assertNull(reopened.active)
                assertTrue(reopenedScene.nodes().any{it.matches("角色 1 · 已停用")})
                assertTrue(reopenedScene.nodes().any{it.matches("角色 2 · 已启用")})
                reopenedScene.shot("role-state-reopened")
            } finally {reopenedScene.close();reopened.close();reopenedScope.cancel()}
        }
    }

    @Test fun measuredSuiteNavigationSurvivesResizeAndUnsentInput()=runBlocking(awt){
        val scope=CoroutineScope(SupervisorJob()+awt);val w=StudioWorkspace.openStudio(root());val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),admissionOverride=RequestAdmission{})
        val scene=ImageComposeScene(1240,880,coroutineContext=awt){MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel)){Workbench(c,DesktopWindowChrome{},false)}}
        try{
            scene.frames();scene.click("AI 设计");scene.field("画面需求 / 继续修改","尚未发送的中文草稿")
            for(width in listOf(640,900,1240)){scene.constraints=Constraints.fixed(width,880);scene.frames();assertTrue(scene.nodes().any{it.config.getOrNull(SemanticsProperties.EditableText)?.text=="尚未发送的中文草稿"});scene.shot("design-width-$width")}
            scene.click("返回创作");scene.frames();scene.click("预览 · 展开预览 ▾");scene.click("预览专注");scene.click("返回编辑");scene.click("AI 设计");assertEquals("尚未发送的中文草稿",c.composer)
            c.flush();assertEquals("尚未发送的中文草稿",w.state.creation!!.composer)
        }finally{scene.close();c.close();scope.cancel()}
    }

    @Test fun continuousGenerationCompletesRemainderStopsAndRetainsPartialResultsWithoutReplay()=runBlocking(awt){
        val r=root();val png=EditorRenderer.synthetic(64,64)
        val requests=CopyOnWriteArrayList<Int>()
        val admissions=java.util.concurrent.atomic.AtomicInteger()
        val waiting=CompletableDeferred<Unit>()
        val partial=java.util.concurrent.atomic.AtomicBoolean(false)
        val server=HttpServer.create(InetSocketAddress("127.0.0.1",0),0)
        server.createContext("/"){e->
            val request=Json.parseToJsonElement(e.requestBody.readBytes().decodeToString()).jsonObject
            val count=request.getValue("parameters").jsonObject.getValue("n_samples").jsonPrimitive.int
            requests+=count
            val bytes=(0 until if(partial.get())1 else count).fold(byteArrayOf()){all,_->all+frame(png)}
            e.sendResponseHeaders(200,bytes.size.toLong());e.responseBody.use{it.write(bytes)}
        };server.start()
        val client=studioHttpClient().newBuilder().addInterceptor{chain->chain.proceed(chain.request().newBuilder().url("http://127.0.0.1:${server.address.port}/").removeHeader("Authorization").build())}.build()
        val scope=CoroutineScope(SupervisorJob()+awt);val w=StudioWorkspace.openStudio(r)
        val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),client,RequestAdmission{
            if(admissions.incrementAndGet()==4){waiting.complete(Unit);awaitCancellation()}
        })
        val scene=ImageComposeScene(1240,880,coroutineContext=awt){MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel)){Workbench(c,DesktopWindowChrome{},false)}}
        try {
            c.saveAccount(null,"Continuous fixture","NAI","https://image.novelai.net",NovelAiImageModel.V4_5_FULL.apiId,"LOCAL_TEST_ONLY");c.idle()
            scene.frames();scene.field("基础 Prompt","Continuous fixture")
            val toggle=scene.nodes().first{it.config.getOrNull(SemanticsProperties.Role)==androidx.compose.ui.semantics.Role.Switch && (it.matches("连续生成")||it.children.any{child->child.matches("连续生成")})}
            assertTrue(toggle.config[SemanticsActions.OnClick].action!!.invoke());scene.frames()
            scene.field("目标图片数","3");scene.click("2")
            assertTrue(c.draft.continuousModeEnabled);assertEquals(2,c.draft.activeSettings.count)
            scene.click("生成图片");c.idle();scene.frames()
            assertEquals(listOf(2,1),requests.toList());assertEquals(3,c.assets.size)
            assertEquals(listOf("COMPLETED","COMPLETED"),w.state.jobs.map{it.state})
            scene.shot("continuous-completed-remainder")

            scene.field("目标图片数","5");scene.click("生成图片")
            withTimeout(30000){waiting.await()};scene.frames()
            assertEquals(5,c.assets.size);assertEquals(listOf(2,1,2),requests.toList())
            scene.shot("continuous-waiting-stoppable");scene.click("停止")
            withTimeout(30000){c.active!!.join()};scene.frames()
            assertFalse(c.busy);assertNull(c.problem);assertEquals("CANCELLED",w.state.jobs.last().state)
            assertTrue(w.state.jobs.last().results.isEmpty());assertEquals(5,c.assets.size)

            partial.set(true);scene.click("生成图片")
            withTimeout(30000){c.active!!.join()};scene.frames()
            assertNotNull(c.problem);assertEquals(listOf(2,1,2,2),requests.toList())
            assertEquals("FAILED",w.state.jobs.last().state);assertEquals(1,w.state.jobs.last().results.size)
            assertEquals(6,c.assets.size);scene.shot("continuous-partial-retained")
            c.assets.forEach{assertContentEquals(png,w.bytes(it.request().id().toString()))};c.flush()
        } finally {scene.close();c.close();scope.cancel();server.stop(0)}
        StudioWorkspace.openStudio(r).use{again->
            assertEquals(6,SharedAssets.images(again).size)
            assertEquals(listOf("COMPLETED","COMPLETED","COMPLETED","CANCELLED","FAILED"),again.state.jobs.map{it.state})
            assertTrue(again.state.creation!!.draft.continuousModeEnabled)
            assertEquals(5,again.state.creation!!.draft.continuousTargetCount)
            val reopenedScope=CoroutineScope(SupervisorJob()+awt)
            val reopened=WorkbenchController(again,reopenedScope,DesktopNovelAiInfrastructure(root()),root(),admissionOverride=RequestAdmission{error("Reopen must not submit a request")})
            val reopenedScene=ImageComposeScene(1240,880,coroutineContext=awt){MaterialTheme{Workbench(reopened,DesktopWindowChrome{},false)}}
            try {reopenedScene.frames();assertNull(reopened.active);assertFalse(reopened.busy);assertEquals(6,reopened.assets.size)}
            finally {reopenedScene.close();reopened.close();reopenedScope.cancel()}
        }
    }

    @Test fun creationFooterAndImageSummaryAdaptWithoutLosingDraftOrSelection()=runBlocking(awt){
        val r=root();val w=StudioWorkspace.openStudio(r)
        val asset=w.importImage("layout-fixture.png",EditorRenderer.synthetic(96,96))
        val scope=CoroutineScope(SupervisorJob()+awt)
        val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),admissionOverride=RequestAdmission{})
        c.current=asset;c.edit{it.copy(basePrompt="保留的布局测试草稿")}
        val scene=ImageComposeScene(1240,720,coroutineContext=awt){MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel)){Workbench(c,DesktopWindowChrome{},false)}}
        try {
            for(width in listOf(1240,900,640,480,1240)) {
                scene.constraints=Constraints.fixed(width,720);scene.frames()
                val footer=scene.nodes().first{it.matches("Studio 固定生成栏")}.boundsInRoot
                val editor=scene.nodes().first{it.matches("Studio Prompt 编辑区")}.boundsInRoot
                assertTrue(footer.height in 48f..125f,"footer at $width: $footer")
                assertTrue(editor.height>250f,"editor at $width: $editor")
                val action=scene.nodes().first{it.matches("撤销上次载入/重置")}.boundsInRoot
                assertTrue(action.width>=48f && action.height>=48f)
                assertEquals(width<900,scene.nodes().any{it.matches("Studio 当前图片摘要")})
                assertEquals("保留的布局测试草稿",c.draft.basePrompt);assertEquals(asset,c.current)
                scene.shot("creation-layout-$width")
            }
            scene.click("撤销上次载入/重置");c.idle();assertEquals("",c.draft.basePrompt)
            scene.click("重做");c.idle();assertEquals("保留的布局测试草稿",c.draft.basePrompt)
            c.flush()
        } finally {scene.close();c.close();scope.cancel()}
        StudioWorkspace.openStudio(r).use{assertEquals("保留的布局测试草稿",it.state.creation!!.draft.basePrompt)}
    }

    @Test fun editedAssetIsSelectableAcrossGuidanceAndEditorAndSurvivesRestart()=runBlocking(awt){
        val r=root();val w=StudioWorkspace.openStudio(r);val original=w.importImage("shared.png",EditorRenderer.synthetic(96,96))
        val scope=CoroutineScope(SupervisorJob()+awt);val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),admissionOverride=RequestAdmission{})
        val scene=ImageComposeScene(1240,880,coroutineContext=awt){MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel)){Workbench(c,DesktopWindowChrome{},false)}}
        var derived="";var project=""
        try {
            c.editor.create("Shared project",listOf(original));c.idle();project=c.editor.project!!.id
            c.editor.chooseExportDirectory={Files.createDirectories(root())}
            c.editor.export(TypesetExportRequest(c.editor.document!!.pages.map{it.id}));c.idle()
            derived=TypesetExport.verify(c.editor.exportDirectory!!).assetIds.values.single()
            assertTrue(c.assets.any{it.request().id().toString()==derived})
            c.view="创作";c.section="图像引导";scene.frames();scene.click("从资产库选择引导图片")
            scene.click("选用资产 $derived");scene.click("确认选用资产");assertEquals(derived,c.current)
            val target=NovelAiImageUseTarget.IMAGE_TO_IMAGE
            scene.click("当前图片 → ${target.displayName}");c.idle();assertEquals(derived,c.draft.imageGuidance.baseImage!!.id)
            c.view="编辑";scene.frames();scene.click("从资产库追加");scene.click("选用资产 $derived");scene.click("确认选用资产");c.idle()
            assertEquals(listOf(original,derived),c.editor.document!!.pages.map{it.assetId});c.editor.drain();c.flush()
            scene.shot("shared-asset-editor")
        }finally{scene.close();c.close();scope.cancel()}
        StudioWorkspace.openStudio(r).use{again->
            assertEquals(derived,again.state.creation!!.draft.imageGuidance.baseImage!!.id)
            val p=again.state.typesetProjects.single{it.id==project};assertEquals(listOf(original,derived),TypesetRepository(again).load(p).pages.map{it.assetId})
            assertTrue(SharedAssets.images(again).any{it.request().id().toString()==derived})
        }
    }

    @Test fun continuedEditingRetainsExistingAssetsAndCredentials()=runBlocking(awt){
        val supplied=System.getProperty("preview.update.fixture")
        val r=supplied?.let{Path.of(it).toAbsolutePath().normalize()}?:root()
        if(supplied!=null){
            val allowed=Path.of(System.getProperty("preview.test.root")).toAbsolutePath().parent.parent.resolve("windows-rc")
            require(r.parent==allowed && r.fileName.toString().startsWith("ab-data-")){"Only explicit isolated update fixtures allowed"}
        }else StudioWorkspace.openStudio(r).use{it.importImage("synthetic.png",EditorRenderer.synthetic(48,48))}
        val w=StudioWorkspace.openStudio(r);val before=updateSnapshot(w)
        val scope=CoroutineScope(SupervisorJob()+awt)
        val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),admissionOverride=RequestAdmission{})
        val scene=ImageComposeScene(1240,880,coroutineContext=awt){MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel)){Workbench(c,DesktopWindowChrome{},false)}}
        val input="更新后新增的中文草稿：保留原件与历史。"
        try{
            scene.frames();scene.click("AI 设计");scene.field("画面需求 / 继续修改",input);c.flush()
            assertEquals(input,w.state.creation!!.composer)
        }finally{scene.close();c.close();scope.cancel()}
        StudioWorkspace.openStudio(r).use{again->
            assertEquals(input,again.state.creation!!.composer)
            val after=updateSnapshot(again)
            assertEquals(before["assets"],after["assets"]);assertEquals(before["credentials"],after["credentials"])
            val output=Path.of("build/ux-r4-post-update/${newId()}.json");Files.createDirectories(output.parent)
            writeUpdateJson(output,buildJsonObject{
                put("fixture",if(supplied==null)"FRESH_SYNTHETIC"else"ACTUAL_UPDATED_DATA_ROOT")
                put("dataRoot",r.toString());put("gui","OFFSCREEN_COMPOSE_CURRENT_SOURCE; not native B input")
                put("result","SAVED_REOPENED_ASSETS_CREDENTIALS_RETAINED");put("before",before);put("after",after)
            })
        }
    }
}
