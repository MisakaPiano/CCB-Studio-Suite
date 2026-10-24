package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.focusable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import com.example.chatbar.desktop.StudioAction
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import java.nio.file.Path
import java.nio.file.Files
import java.nio.file.StandardCopyOption

internal class StudioUpdateController(private val scope:CoroutineScope,cache:Path,
    private val invoke:(suspend(Boolean)->JsonObject)? = null) {
    private val updateCache=cache
    var result by mutableStateOf<JsonObject?>(null);private set
    var progress by mutableStateOf<JsonObject?>(null);private set
    var message by mutableStateOf("检查当前渠道的可信更新");private set
    var busy by mutableStateOf(false);private set
    var job:Job?=null;private set
    private var closed=false
    private var startupChecked=false
    var pendingVersion by mutableStateOf<String?>(null);private set
    var showStartupNotice by mutableStateOf(false);private set
    fun dismissStartupNotice(){showStartupNotice=false}
    fun checkOnStartup() {
        if(startupChecked||closed)return
        startupChecked=true
        // Development/test hosts do not contact an update service implicitly.
        if(invoke==null && System.getProperty("jpackage.app-path")==null)return
        val hint=updateCache.resolve("pending-notice.txt")
        pendingVersion=runCatching{if(Files.isRegularFile(hint)&&Files.size(hint)<=160)Files.readString(hint).takeIf{it.isNotBlank()}else null}.getOrNull()
        check(startup=true)
    }
    private suspend fun rememberPending(version:String?)=withContext(Dispatchers.IO) {
        Files.createDirectories(updateCache)
        val hint=updateCache.resolve("pending-notice.txt")
        if(version==null)Files.deleteIfExists(hint)else {
            val staged=updateCache.resolve("pending-notice-${java.util.UUID.randomUUID()}.tmp")
            Files.writeString(staged,version.take(160))
            Files.move(staged,hint,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING)
        }
    }
    internal fun receiveProgress(value:JsonObject) {
        val phase=value["phase"]?.jsonPrimitive?.content
        require(phase in setOf("CHECKING","DOWNLOADING","VERIFYING","DOWNLOADED_VERIFIED")){"未知更新进度"}
        if(phase!="CHECKING") {
            val received=value["received"]?.jsonPrimitive?.longOrNull?:error("缺少下载进度")
            val total=value["total"]?.jsonPrimitive?.longOrNull?:error("缺少下载大小")
            require(total in 1..1073741824L && received in 0..total){"下载进度无效"}
            require(received >= (progress?.get("received")?.jsonPrimitive?.longOrNull?:0)){"下载进度倒退"}
            value["bytesPerSecond"]?.let{require((it.jsonPrimitive.longOrNull?:-1)>=0){"下载速度无效"}}
        }
        progress=value
        message=when(phase){"CHECKING"->"正在检查签名、版本和渠道…";"DOWNLOADING"->"正在下载安装包…";else->"正在校验安装包，请稍候…"}
    }
    fun check(download:Boolean=false,startup:Boolean=false) {
        if(busy||closed)return
        result=null;progress=null
        busy=true;message=if(download)"正在下载并验证…" else "正在检查签名、版本和渠道…"
        job=scope.launch {
            try {
                result=if(invoke!=null)invoke(download) else {
                    val app=System.getProperty("jpackage.app-path")?.let{Path.of(it).parent}
                        ?:error("当前不是配置了可信更新源的安装版本")
                    StudioUpdateClient(app,updateCache).check(download,::receiveProgress)
                }
                message=when(result?.get("status")?.jsonPrimitive?.content) {
                    "AVAILABLE"->"发现可信候选；尚未下载或安装"
                    "DOWNLOADED_VERIFIED"->"下载完成，安全校验已通过"
                    "UP_TO_DATE"->"已是最新版本"
                    else->"更新未完成：${result?.get("reason")?.jsonPrimitive?.content?:"未知回执"}"
                }
                when(result?.get("status")?.jsonPrimitive?.content){
                    "AVAILABLE","DOWNLOADED_VERIFIED"->{pendingVersion=result?.get("displayVersion")?.jsonPrimitive?.content?:"新版";rememberPending(pendingVersion);if(startup)showStartupNotice=true}
                    "UP_TO_DATE"->{pendingVersion=null;showStartupNotice=false;rememberPending(null)}
                }
            } catch(e:CancellationException){message="已取消；当前安装和资料未替换";throw e}
            catch(e:Exception){message="更新未完成：${e.message?.take(240)?:e.javaClass.simpleName}"}
            finally{busy=false;progress=null}
        }
    }
    suspend fun closeAndDrain(){closed=true;job?.cancelAndJoin()}
}

@Composable internal fun UpdateWorkspace(c:StudioUpdateController,workspacePath:String?=null,onShortcut:((String)->Unit)?=null,onInstall:(()->Unit)?=null) {
    var confirm by remember{mutableStateOf(false)}
    val runningVersion=remember{runCatching {
        val app=Path.of(requireNotNull(System.getProperty("jpackage.app-path"))).parent
        val config=Json.parseToJsonElement(Files.readString(app.resolve("update-config.json"))).jsonObject
        config["displayVersion"]?.jsonPrimitive?.content?:config["version"]?.jsonPrimitive?.content
    }.getOrNull()?:"开发版本"}
    val focus=remember{FocusRequester()}
    LaunchedEffect(Unit){focus.requestFocus()}
    Column(Modifier.fillMaxSize().focusRequester(focus).onPreviewKeyEvent {event->
        if(event.type!=KeyEventType.KeyDown || c.busy || confirm)false
        else when {
            event.key==Key.F5->{c.check();true}
            event.isCtrlPressed && event.key==Key.D && c.result?.get("status")?.jsonPrimitive?.content=="AVAILABLE"->{c.check(true);true}
            event.isCtrlPressed && event.isShiftPressed && event.key==Key.I && c.result?.get("status")?.jsonPrimitive?.content=="DOWNLOADED_VERIFIED" && onInstall!=null->{confirm=true;true}
            else->false
        }
    }.focusable().padding(24.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("应用更新",style=MaterialTheme.typography.h5)
        Text("当前运行版本：$runningVersion")
        workspacePath?.let{Text("当前工作区：$it",color=Quiet)}
        if(onShortcut!=null){Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
            StudioAction("创建桌面快捷方式",!c.busy){onShortcut("desktop")}
            StudioAction("添加到开始菜单",!c.busy){onShortcut("start")}
        };Text("入口随更新保持有效。右键桌面或开始菜单中的 CCB Studio Suite，可选择固定到任务栏或开始菜单。以前固定的旧版本图标请取消，再从新入口固定一次，以后更新无需重做。",color=Quiet)}
        Text("更新签名与安装身份由随包固定的信任根验证；资料与下载缓存分别保存。")
        Text("F5 检查 · Ctrl+D 确认下载 · Ctrl+Shift+I 打开安装确认",color=Quiet)
        StudioAction("检查更新",!c.busy){c.check()}
        Card(backgroundColor=if(c.result?.get("status")?.jsonPrimitive?.content in setOf("DOWNLOADED_VERIFIED","UP_TO_DATE"))MaterialTheme.colors.primary.copy(alpha=0.15f) else MaterialTheme.colors.surface,modifier=Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text(c.message,style=MaterialTheme.typography.h6)
                if(c.busy) {
                    val p=c.progress
                    val received=p?.get("received")?.jsonPrimitive?.longOrNull?:0
                    val total=p?.get("total")?.jsonPrimitive?.longOrNull?:0
                    if(p?.get("phase")?.jsonPrimitive?.content=="DOWNLOADING" && total>0) {
                        LinearProgressIndicator(progress=received.toFloat()/total,modifier=Modifier.fillMaxWidth())
                        Text("${received*100/total}% · ${updateSize(received)} / ${updateSize(total)} · ${updateSize(p["bytesPerSecond"]?.jsonPrimitive?.longOrNull?:0)}/秒")
                    } else LinearProgressIndicator(modifier=Modifier.fillMaxWidth())
                }
                when(c.result?.get("status")?.jsonPrimitive?.content) {
                    "DOWNLOADED_VERIFIED"->{
                        Text("安装包已准备好。点击下方按钮保存工作、安装并重启；目前尚未安装。")
                        Button(onClick={confirm=true},enabled=!c.busy && onInstall!=null){Text("安装并重启")}
                    }
                    "UP_TO_DATE"->Text("当前版本 ${c.result?.get("displayVersion")?.jsonPrimitive?.content.orEmpty()} 与可信更新源一致，无需再次安装。")
                }
            }
        }
        c.result?.takeIf{it["status"]?.jsonPrimitive?.content in setOf("AVAILABLE","DOWNLOADED_VERIFIED")}?.let{r->
            Text("可更新至 ${r["displayVersion"]?.jsonPrimitive?.content} · 渠道 ${r["channel"]?.jsonPrimitive?.content}")
            Text(r["notes"]?.jsonPrimitive?.content.orEmpty())
            Text("下载大小 ${updateSize(r["bytes"]?.jsonPrimitive?.longOrNull?:0)}")
            Text("目标资料格式 ${r["dataVersion"]?.jsonPrimitive?.content?:"与当前相同"}；需要转换时在新目录建立副本，保留原目录。")
            Text("源码 ${r["sourceSha"]?.jsonPrimitive?.content}")
            if(r["status"]?.jsonPrimitive?.content=="AVAILABLE")Button(onClick={c.check(true)},enabled=!c.busy){Text("确认下载并验证")}
        }
        if(c.busy)StudioAction("取消更新操作"){c.job?.cancel()}
        Text("下载验证后仍需安装、重启和资料核验；任何一步未通过都不会报告更新成功。",color=Quiet)
    }
    if(confirm)AlertDialog(onDismissRequest={confirm=false},title={Text("安装并重启")},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        workspacePath?.let{Text("本次更新使用的工作区：$it")}
        Text("将保存编辑并停止活动任务，等待资料写入退出后安装新版。若资料格式升级，将建立独立副本并核验数据和凭据状态，再切换启动入口；旧程序和原目录保留，不自动重发请求。恢复后尚未重新配置的凭据仍保持未配置。")
    }},confirmButton={
        val confirmFocus=remember{FocusRequester()};LaunchedEffect(Unit){withFrameNanos{};confirmFocus.requestFocus()}
        TextButton(onClick={confirm=false;onInstall?.invoke()},modifier=Modifier.focusRequester(confirmFocus)){Text("保存后安装")}
    },dismissButton={TextButton(onClick={confirm=false}){Text("暂不安装")}})
}

internal fun updateSize(bytes:Long)=String.format(java.util.Locale.ROOT,"%.1f MiB",bytes/1048576.0)
