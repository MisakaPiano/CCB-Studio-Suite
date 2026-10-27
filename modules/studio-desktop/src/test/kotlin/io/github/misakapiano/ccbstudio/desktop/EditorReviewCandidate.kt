package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.example.chatbar.desktop.DesktopNovelAiInfrastructure
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import java.awt.*
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.file.*
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import javax.imageio.ImageIO

/** Local review host only: real editor components, never part of the shipped JAR.
 * No application launcher, default entries, updater startup or service UI is exposed. */
object EditorReviewCandidate {
    @Composable internal fun ImportActions(c:WorkbenchController) {
        TextButton(onClick={c.importImages()},enabled=!c.busy&&!c.maintenance){Text("导入测试图片")}
        Text("导入后点“从资产库追加”选入队列",style=MaterialTheme.typography.caption)
    }
    private fun repository()=generateSequence(Path.of("").toAbsolutePath().normalize()){it.parent}.first{Files.isRegularFile(it.resolve("AGENTS.md"))&&Files.isRegularFile(it.resolve("docs/SOURCE_LOCK.json"))}
    internal fun checkedRoot(path:Path,reviewOnly:Boolean=true):Path {
        val root=path.toAbsolutePath().normalize();val repository=repository()
        require(root.startsWith(repository.resolve(".local/disposable")) && root.fileName.toString()=="payload")
        safePath(root);val owner=Json.parseToJsonElement(Files.readString(root.parent.resolve("OWNER.json"))).jsonObject
        require(owner["kind"]?.jsonPrimitive?.content=="EXPLICIT_SYNTHETIC_DISPOSABLE" && Path.of(owner.getValue("root").jsonPrimitive.content)==root.parent)
        require(owner["task"]?.jsonPrimitive?.content in if(reviewOnly)setOf("editor-native-review")else setOf("editor-native-review","gradle-tests"))
        return root
    }
    internal fun prepare(path:Path) {
        val root=checkedRoot(path,false);val data=root.resolve("workspace");require(!Files.exists(data)){"Review data already exists; never reseed or replace"}
        StudioWorkspace.openStudio(data).use{w->
            val ids=(0 until 12).map{i->w.importImage("合成审阅-${(i+1).toString().padStart(2,'0')}.png",landscape(i))}
            val repo=TypesetRepository(w);var p=repo.create("编辑审阅 · 合成样例",ids)
            p=repo.update(p.id,p.revision){d->d.copy(queueWidth=220,thumbnailSize=64,pages=d.pages.mapIndexed{i,page->page.copy(
                name=when(i){0->"竖排与对象操作";1->"九层打码与连续画笔";2->"横排文字与图层";else->"队列浏览 ${i+1}"},
                texts=when(i){0->listOf(TypesetText(text="“竖排测试”‘单引号’『原角引号』「保留」ABC123，。",x=1060.0,y=70.0,width=550.0,vertical=true,style=TypesetStyle(fontSize=42.0,fill="#fff4cf",strokeWidth=2.0)))
                    2->listOf(TypesetText(text="拖动文字，调整样式",x=600.0,y=90.0,width=800.0,style=TypesetStyle(fontSize=48.0,strokeWidth=2.0)),TypesetText(text="图层、预设与撤销",x=600.0,y=180.0,width=700.0,style=TypesetStyle(fontSize=32.0,fill="#7db5ff",strokeWidth=1.0)))
                    else->emptyList()},
                masks=if(i==1)(0 until 9).map{n->TypesetMask(effect=if(n%2==0)"blur"else"mosaic",x=90.0+n%3*340.0,y=80.0+n/3*215.0,width=220.0,height=135.0,strength=16)}else emptyList(),
                caption="合成图片第 ${i+1} 页。小人书旁白可在此编辑；原图保留。",
                story=StoryStyle(fontFamily="Microsoft YaHei",pageWidth=1000,fontSize=32.0))})}
            val d=repo.load(p);repo.view(p.id,p.revision,d.pages.first().id,listOf(d.pages.first().id))
            TypesetEditorExtrasStore.save(w,TypesetEditorExtras().saveMask("柔和模糊",MaskToolSettings(effect="blur",strength=24)).saveMask("条纹遮挡",MaskToolSettings(effect="black",size=60.0)))
            w.save(w.state.copy(typesetPreferences=w.state.typesetPreferences.saveText("对白竖排",d.pages.first().texts.single())))
            val originals=buildJsonObject{ids.forEach{id->put(id,sha256(w.bytes(id)))}}
            Files.writeString(root.resolve("originals.json"),originals.toString(),StandardOpenOption.CREATE_NEW)
            require(w.state.accounts.isEmpty())
        }
        Files.createDirectories(root.resolve("runtime"));Files.createDirectories(root.resolve("review-exports"))
    }
    internal fun verifyOriginals(path:Path) {
        val root=checkedRoot(path,false);val expected=Json.parseToJsonElement(Files.readString(root.resolve("originals.json"))).jsonObject
        StudioWorkspace.openStudio(root.resolve("workspace")).use{w->expected.forEach{(id,hash)->check(sha256(w.bytes(id))==hash.jsonPrimitive.content){"Original bytes changed"}}}
    }
    private fun landscape(index:Int):ByteArray {
        val image=BufferedImage(1216,832,BufferedImage.TYPE_INT_ARGB);val g=image.createGraphics()
        try{
            g.paint=GradientPaint(0f,0f,Color(65+index*4,85,104),1216f,832f,Color(215,175-index*4,110));g.fillRect(0,0,1216,832)
            g.color=Color(255,244,211);g.fillOval(130+index*20,90,150,150)
            g.color=Color(39,75+index*3,81);g.fillPolygon(intArrayOf(0,360,700,1216,1216,0),intArrayOf(650,350,610,300,832,832),6)
            g.font=Font("Dialog",Font.BOLD,40);g.color=Color.WHITE;g.drawString("SYNTHETIC REVIEW ${index+1}",55,770)
            for(n in 0..12){g.color=Color(240,218,160,95);g.drawLine(n*100,0,n*100,832);g.drawLine(0,n*70,1216,n*70)}
            return ByteArrayOutputStream().also{check(ImageIO.write(image,"png",it))}.toByteArray()
        }finally{g.dispose();image.flush()}
    }
    @JvmStatic fun main(args:Array<String>) {
        require(args.size in 2..3);val root=checkedRoot(Path.of(args[1]))
        when(args[0]){"prepare"->{prepare(root);return};"verify"->{verifyOriginals(root);return};"open"->Unit;else->error("Unknown review action")}
        require(args.size==3);val out=Path.of(args[2]).toAbsolutePath().normalize();require(out.startsWith(repository().resolve(".local/editor-native-20261027")));safePath(out);Files.createDirectories(out)
        fun note(event:String,detail:String=""){Files.writeString(out.resolve("native-events.jsonl"),buildJsonObject{put("event",event);put("utc",Instant.now().toString());put("pid",ProcessHandle.current().pid());put("detail",detail)}.toString()+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND)}
        verifyOriginals(root);StudioBrand.configureDefaultWindows();val w=StudioWorkspace.openStudio(root.resolve("workspace"));note("WORKSPACE_OPENED",root.toString())
        application {
            val scope=rememberCoroutineScope()
            val http=remember{studioHttpClient().newBuilder().addInterceptor{note("HTTP_REFUSED");throw java.io.IOException("This isolated editor review has no service access")}.build()}
            val c=remember{WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root.resolve("runtime/catalog")),root.resolve("runtime/budget"),http,
                admissionOverride=RequestAdmission{error("Provider requests forbidden in editor review")},pauseCollectionsOnStart=true,allowDefaultEntrypoint=false)}
            StudioAppearanceHost(c);var closing by remember{mutableStateOf(false)}
            fun close(){if(!closing){closing=true;scope.launch{try{c.close();verifyOriginals(root);note("NORMAL_CLOSE_ORIGINALS_UNCHANGED");exitApplication()}catch(e:Exception){closing=false;c.problem="关闭检查未完成：${e.message}";note("CLOSE_FAILED",e.javaClass.simpleName)}}}}
            c.requestClose=::close
            LaunchedEffect(Unit){c.editor.open(w.state.typesetProjects.last().id);c.active?.join();check(c.problem==null);c.editor.queueView="list";c.message="隔离编辑审阅 · 合成资料 · 服务调用已禁止";note("EDITOR_OPENED")}
            Window(onCloseRequest=::close,title="Studio 编辑本地审阅 · AI导入补充 · 非日常118",icon=StudioBrand.painter,state=rememberWindowState(width=1320.dp,height=900.dp)) {
                val drawn=remember{AtomicBoolean(false)}
                StudioTheme {Surface(color=Ink,contentColor=MaterialTheme.colors.onSurface){Column(Modifier.fillMaxSize().drawWithContent{drawContent();if(drawn.compareAndSet(false,true))note("NATIVE_WINDOW_DRAWN")} ){
                    Row(Modifier.fillMaxWidth().height(36.dp).background(Panel).padding(horizontal=12.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){Text("编辑隔离审阅",style=MaterialTheme.typography.subtitle2);ImportActions(c);Spacer(Modifier.weight(1f));Text("不修改日常118 · 本地处理",style=MaterialTheme.typography.caption)}
                    Box(Modifier.weight(1f)){TypesetWorkspace(c)}
                    Text(if(closing)"正在保存并关闭…"else c.message,Modifier.padding(6.dp),style=MaterialTheme.typography.caption)
                }}
                c.problem?.let{problem->AlertDialog(onDismissRequest={c.problem=null},title={Text("操作提示")},text={Text(problem)},confirmButton={TextButton(onClick={c.problem=null}){Text("知道了")}})}
                }
                LaunchedEffect(Unit){withTimeout(30000){while(!window.isShowing)delay(30)};note("NATIVE_WINDOW_SHOWING","${window.width}x${window.height}")}
            }
        }
    }
}
