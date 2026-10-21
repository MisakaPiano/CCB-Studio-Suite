package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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

@Composable internal fun UpdateWorkspace(c:StudioUpdateController) {
    Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("应用更新",style=MaterialTheme.typography.h5)
        Text("更新签名与安装身份由随包固定的信任根验证；资料与下载缓存分别保存。")
        StudioAction("检查更新",!c.busy){c.check()}
        Text(c.message)
        c.result?.takeIf{it["status"]?.jsonPrimitive?.content in setOf("AVAILABLE","DOWNLOADED_VERIFIED")}?.let{r->
            Text("版本 ${r["displayVersion"]?.jsonPrimitive?.content} · 渠道 ${r["channel"]?.jsonPrimitive?.content}")
            Text(r["notes"]?.jsonPrimitive?.content.orEmpty())
            Text("下载大小 ${r["bytes"]?.jsonPrimitive?.content} bytes")
            Text("源码 ${r["sourceSha"]?.jsonPrimitive?.content}")
            StudioAction("确认下载并验证",!c.busy && r["status"]?.jsonPrimitive?.content=="AVAILABLE"){c.check(true)}
        }
        if(c.busy)StudioAction("取消更新操作"){c.job?.cancel()}
        Text("当前恢复构建的安装接续仍在实施，下载通过不代表更新完成。",color=Quiet)
    }
}
