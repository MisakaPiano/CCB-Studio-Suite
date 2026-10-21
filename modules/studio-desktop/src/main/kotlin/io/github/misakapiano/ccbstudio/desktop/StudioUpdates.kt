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

internal class StudioUpdateController(private val scope:CoroutineScope,cache:Path,
    private val invoke:suspend(Boolean)->JsonObject = {download->
        val app=System.getProperty("jpackage.app-path")?.let{Path.of(it).parent}
            ?:error("当前不是配置了可信更新源的安装版本")
        StudioUpdateClient(app,cache).check(download)
    }) {
    var result by mutableStateOf<JsonObject?>(null);private set
    var message by mutableStateOf("检查当前渠道的可信更新");private set
    var busy by mutableStateOf(false);private set
    var job:Job?=null;private set
    private var closed=false
    fun check(download:Boolean=false) {
        if(busy||closed)return
        busy=true;message=if(download)"正在下载并验证…" else "正在检查签名、版本和渠道…"
        job=scope.launch {
            try {
                result=invoke(download)
                message=when(result?.get("status")?.jsonPrimitive?.content) {
                    "AVAILABLE"->"发现可信候选；尚未下载或安装"
                    "DOWNLOADED_VERIFIED"->"下载已验证；尚未安装或重启"
                    else->"更新未完成：${result?.get("reason")?.jsonPrimitive?.content?:"未知回执"}"
                }
            } catch(e:CancellationException){message="已取消；当前安装和资料未替换";throw e}
            catch(e:Exception){message="更新未完成：${e.message?.take(240)?:e.javaClass.simpleName}"}
            finally{busy=false}
        }
    }
    suspend fun closeAndDrain(){closed=true;job?.cancelAndJoin()}
}

@Composable internal fun UpdateWorkspace(c:StudioUpdateController,onInstall:(()->Unit)?=null) {
    var confirm by remember{mutableStateOf(false)}
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
        Text("更新签名与安装身份由随包固定的信任根验证；资料与下载缓存分别保存。")
        Text("F5 检查 · Ctrl+D 确认下载 · Ctrl+Shift+I 打开安装确认",color=Quiet)
        StudioAction("检查更新",!c.busy){c.check()}
        Text(c.message)
        c.result?.takeIf{it["status"]?.jsonPrimitive?.content in setOf("AVAILABLE","DOWNLOADED_VERIFIED")}?.let{r->
            Text("版本 ${r["displayVersion"]?.jsonPrimitive?.content} · 渠道 ${r["channel"]?.jsonPrimitive?.content}")
            Text(r["notes"]?.jsonPrimitive?.content.orEmpty())
            Text("下载大小 ${r["bytes"]?.jsonPrimitive?.content} bytes")
            Text("源码 ${r["sourceSha"]?.jsonPrimitive?.content}")
            StudioAction("确认下载并验证",!c.busy && r["status"]?.jsonPrimitive?.content=="AVAILABLE"){c.check(true)}
            if(r["status"]?.jsonPrimitive?.content=="DOWNLOADED_VERIFIED")StudioAction("安装并重启",!c.busy && onInstall!=null){confirm=true}
        }
        if(c.busy)StudioAction("取消更新操作"){c.job?.cancel()}
        Text("下载验证后仍需安装、重启和资料核验；任何一步未通过都不会报告更新成功。",color=Quiet)
    }
    if(confirm)AlertDialog(onDismissRequest={confirm=false},title={Text("安装并重启")},text={Text("将保存编辑并停止活动任务，等待资料写入退出后安装新版。旧程序和资料保留，不自动重发请求。")},confirmButton={
        val confirmFocus=remember{FocusRequester()};LaunchedEffect(Unit){withFrameNanos{};confirmFocus.requestFocus()}
        TextButton(onClick={confirm=false;onInstall?.invoke()},modifier=Modifier.focusRequester(confirmFocus)){Text("保存后安装")}
    },dismissButton={TextButton(onClick={confirm=false}){Text("暂不安装")}})
}
