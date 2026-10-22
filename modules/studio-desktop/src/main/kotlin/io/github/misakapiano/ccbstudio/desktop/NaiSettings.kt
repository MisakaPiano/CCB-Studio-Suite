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

internal data class NaiAllowanceDisplay(val text:String,val fraction:Float?)
internal fun naiAllowanceDisplay(usage:NovelAiAccountUsage?):NaiAllowanceDisplay {
    if(usage==null)return NaiAllowanceDisplay("V5 Opus 体力：未知，请刷新账户",null)
    if(!usage.isActiveOpus)return NaiAllowanceDisplay("V5 Opus 体力：当前账户未确认有效 Opus 资格",null)
    val percent=if(usage.v5AllowanceExhausted)0.0 else usage.v5AllowancePercent?.takeIf{it.isFinite()}?.coerceIn(0.0,100.0)
    return NaiAllowanceDisplay(if(percent==null)"V5 Opus 体力：服务未返回百分比" else "V5 Opus 体力 ${percent.toInt()}% · 约 ${usage.approximateV5Images?:"—"} 张${if(usage.v5AllowanceExhausted)" · 已用尽" else ""}",percent?.div(100)?.toFloat())
}
internal class NaiSettingsController(private val c:WorkbenchController) {
    var candidate by mutableStateOf<DanbooruCatalogUpdateInfo?>(null);private set
    var status by mutableStateOf("尚未检查词库更新");private set
    fun checkCatalog()=c.run("检查 Danbooru 词库更新") {
        candidate=null
        val result=SharedDanbooruCatalogUpdateChecker(c.infrastructure.tags,"CCB-Studio-Suite",c.http).checkLatestCatalog()
        candidate=result;status=if(result==null)"词库已是最新版本" else "发现词库更新 · ${result.sizeBytes} 字节"
    }
    fun installCatalog() {
        val info=candidate?:return
        c.run("下载并校验 Danbooru 词库") {
            DesktopNovelAiCatalogUpdate(c.infrastructure.tags,c.http).install(info){status=it}
            c.infrastructure.refreshTranslations();candidate=null;status="词库更新完成，补全和中文注释已刷新"
        }
    }
}
@Composable internal fun NaiAllowanceBar(usage:NovelAiAccountUsage?) {
    val value=naiAllowanceDisplay(usage)
    Text(value.text)
    if(value.fraction!=null)LinearProgressIndicator(value.fraction,Modifier.fillMaxWidth().height(8.dp).semantics{contentDescription="V5 Opus 体力百分比"})
}
@Composable internal fun NaiSettingsDialog(c:WorkbenchController,close:()->Unit) {
    DesktopImageToolWindow("创作设置",close,width=620.dp,height=480.dp) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("Danbooru 词库")
            Text(c.naiSettings.status)
            c.problem?.let { Text(it, color=MaterialTheme.colors.error) }
            StudioAction("检查 Danbooru 词库更新",enabled=!c.busy){c.naiSettings.checkCatalog()}
            if(c.naiSettings.candidate!=null)StudioAction("下载并校验词库更新",enabled=!c.busy){c.naiSettings.installCatalog()}
            if(c.busy)StudioAction("取消当前操作"){c.active?.cancel()}
            StudioSwitch("复制正向时忽略画风",c.draft.copyPositivePromptIgnoreStyle){c.edit{it.copy(copyPositivePromptIgnoreStyle=!it.copyPositivePromptIgnoreStyle)}}
            StudioSwitch("本地中文注释",c.state.creation!!.translatedTags){c.defaults(translated=!c.state.creation!!.translatedTags)}
            Text("中文注释使用本地 Danbooru 翻译和离线词典，不改写 Prompt，也不调用语言模型翻译。")
            StudioAction("返回创作",enabled=!c.busy,onClick=close)
        }
    }
}
