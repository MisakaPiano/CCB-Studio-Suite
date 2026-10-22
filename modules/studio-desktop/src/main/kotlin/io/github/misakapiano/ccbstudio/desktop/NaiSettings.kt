package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.example.chatbar.desktop.*
import com.example.chatbar.domain.image.*
import com.example.chatbar.domain.update.*
import kotlinx.coroutines.*

internal data class NaiAllowanceDisplay(val text:String,val fraction:Float?)
internal fun naiGenerateLabel(busy:Boolean,configured:Boolean,cost:NovelAiGenerationCost?,quotaKnown:Boolean,continuous:Boolean=false):String = when {
    busy->"停止"
    !configured->"未配置 NAI 账户"
    cost==null->"生成设置无效"
    cost.anlas>0->"生成${if(quotaKnown)"消耗" else "预计"} ${cost.anlas} Anlas"+
        (if(cost.encodingAnlas>0)"（含编码 ${cost.encodingAnlas}）" else "")+
        (if(cost.extraVibeAnlas>0)"（含额外 Vibe ${cost.extraVibeAnlas}）" else "")+
        (if(continuous)" / 批" else "")
    cost.kind==NovelAiGenerationChargeKind.FREE||cost.kind==NovelAiGenerationChargeKind.V5_ALLOWANCE->"生成免费"+(if(continuous)" / 批" else "")
    else->"生成消耗 ${cost.anlas} Anlas"
}
internal fun naiAllowanceDisplay(usage:NovelAiAccountUsage?):NaiAllowanceDisplay {
    if(usage==null)return NaiAllowanceDisplay("V5 Opus 体力：未知，请刷新账户",null)
    if(!usage.isActiveOpus)return NaiAllowanceDisplay("V5 Opus 体力：当前账户未确认有效 Opus 资格",null)
    val percent=if(usage.v5AllowanceExhausted)0.0 else usage.v5AllowancePercent?.takeIf{it.isFinite()}?.coerceIn(0.0,100.0)
    return NaiAllowanceDisplay(if(percent==null)"V5 Opus 体力：服务未返回百分比" else "V5 Opus 体力 ${percent.toInt()}% · 约 ${usage.approximateV5Images?:"—"} 张${if(usage.v5AllowanceExhausted)" · 已用尽" else ""}",percent?.div(100)?.toFloat())
}
internal class NaiSettingsController(private val c:WorkbenchController) {
    var candidate by mutableStateOf<DanbooruCatalogUpdateInfo?>(null);private set
    var status by mutableStateOf("尚未检查词库更新");private set
    var busy by mutableStateOf(false);private set
    var notice by mutableStateOf(false);private set
    var error by mutableStateOf<String?>(null);private set
    var task:Job?=null;private set
    private fun run(label:String,body:suspend ()->Unit) {
        if(busy||c.maintenance)return
        busy=true;notice=true;error=null;status=label
        task=c.scope.launch(start=CoroutineStart.UNDISPATCHED) {
            try {withContext(Dispatchers.IO){body()}}
            catch(e:CancellationException){status="词库任务已停止；已完成的切换保留";throw e}
            catch(e:Exception){
                // OkHttp may surface cancellation as IOException while execute() is unwinding.
                if(!currentCoroutineContext().isActive){status="词库任务已停止；已完成的切换保留";currentCoroutineContext().ensureActive()}
                error="词库操作未完成（${e.javaClass.simpleName}），当前可用词库保留";status=error!!
            }
            finally{busy=false;task=null}
        }
    }
    fun cancel(){task?.cancel()}
    fun dismiss(){if(!busy)notice=false}
    suspend fun cancelAndDrain(){task?.cancelAndJoin()}
    fun checkCatalog()=run("正在检查 Danbooru 词库更新…") {
        candidate=null
        val result=SharedDanbooruCatalogUpdateChecker(c.infrastructure.tags,"CCB-Studio-Suite",c.http).checkLatestCatalog()
        candidate=result;status=if(result==null)"词库已是最新版本" else "发现词库更新 · ${result.sizeBytes} 字节"
    }
    fun installCatalog() {
        val info=candidate?:return
        run("正在下载 Danbooru 词库…") {
            DesktopNovelAiCatalogUpdate(c.infrastructure.tags,c.http).install(info){status=it}
            c.infrastructure.refreshTranslations();candidate=null;status="词库更新完成，补全和中文注释已刷新"
        }
    }
}
@Composable internal fun NaiCatalogBackgroundStatus(c:WorkbenchController) {
    val updates=c.naiSettings
    if(updates.notice)Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
        StatusText(updates.status)
        if(updates.busy) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            StudioAction("取消词库任务"){updates.cancel()}
        }else StudioAction("收起词库状态"){updates.dismiss()}
    }
}
@Composable internal fun NaiAllowanceBar(usage:NovelAiAccountUsage?) {
    val value=naiAllowanceDisplay(usage)
    StatusText(value.text)
    if(value.fraction!=null)LinearProgressIndicator(value.fraction,Modifier.fillMaxWidth().height(8.dp).semantics{contentDescription="V5 Opus 体力百分比"})
}
@Composable internal fun NaiSettingsDialog(c:WorkbenchController,close:()->Unit) {
    DesktopImageToolWindow("创作设置",close,width=620.dp,height=480.dp) {
        NaiSettingsContent(c,close)
    }
}
@Composable internal fun NaiSettingsContent(c:WorkbenchController,close:()->Unit) {
        val updates=c.naiSettings
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("Danbooru 词库")
            Text(updates.status)
            StudioAction("检查 Danbooru 词库更新",enabled=!updates.busy&&!c.maintenance){updates.checkCatalog()}
            if(updates.candidate!=null)StudioAction("下载并校验词库更新",enabled=!updates.busy&&!c.maintenance){updates.installCatalog()}
            if(updates.busy)StudioAction("取消词库任务"){updates.cancel()}
            StudioSwitch("复制正向时忽略画风",c.draft.copyPositivePromptIgnoreStyle){c.edit{it.copy(copyPositivePromptIgnoreStyle=!it.copyPositivePromptIgnoreStyle)}}
            StudioSwitch("本地中文注释",c.state.creation!!.translatedTags){c.defaults(translated=!c.state.creation!!.translatedTags)}
            Text("中文注释使用本地 Danbooru 翻译和离线词典，不改写 Prompt，也不调用语言模型翻译。")
            StudioAction(if(updates.busy)"返回创作（后台继续）" else "返回创作",onClick=close)
        }
}
