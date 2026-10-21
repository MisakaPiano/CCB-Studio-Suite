package com.example.chatbar.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.text.TextStyle
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import com.example.chatbar.domain.image.*
import io.github.misakapiano.ccbstudio.core.*
import io.github.misakapiano.ccbstudio.desktop.StudioBrand
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import java.nio.file.Path

/** Small platform seams for the pinned Desktop Compose components. No Desktop store/settings access. */
internal object DesktopBootstrapColors {
    val background=Color(0xff0d1117);val foreground=Color(0xffe6edf3);val card=Color(0xff161b22)
    val primary=Color(0xff7db5ff);val primaryForeground=Color(0xff0d1117);val muted=Color(0xff222a35)
    val mutedForeground=Color(0xffaab6c5);val border=Color(0xff303946);val input=Color(0xff111820)
    val accent=Color(0xff263c57);val warning=Color(0xffffc16d)
}
@Composable internal fun StatusText(text:String,color:Color=DesktopBootstrapColors.mutedForeground) {BasicText(text,style=TextStyle(color=color,fontSize=14.sp))}
@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun StudioActions(content:@Composable RowScope.()->Unit) {
    FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){content()}
}
@Composable internal fun StudioField(label:String,value:String,change:(String)->Unit) {DesktopDesignField(label,value,onChange=change)}
@Composable internal fun desktopImageWindowIcon()=StudioBrand.painter
@Composable internal fun rememberDesktopImageWindowSize(width:Dp,height:Dp)=remember(width,height){DpSize(width,height)}
@Composable internal fun DesktopImageToolWindow(title:String,close:()->Unit,width:Dp=800.dp,height:Dp=700.dp,content:@Composable ()->Unit) {
    DialogWindow(onCloseRequest=close,title=title,icon=StudioBrand.painter,state=rememberDialogState(width=width,height=height)) {content()}
}
internal class DesktopChromeLayoutRecorder(private val observe:(LayoutCoordinates?)->Unit={}) {
    fun previewTitle(value:LayoutCoordinates)=observe(value)
    fun clearPreviewTitle()=observe(null)
}

internal class DesktopNovelAiInfrastructure(root:Path) {
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private fun open(name:String)=requireNotNull(javaClass.getResourceAsStream("/$name")){"Bundled catalog unavailable"}
    private val assets=DesktopNovelAiCatalogAssets(root,::open)
    val tags=DesktopNovelAiTagCatalog(assets)
    private val dictionary=NovelAiPromptWordDictionary.fromSource(DesktopNovelAiDictionary(assets))
    val suggestions=NovelAiTagSuggestionService(tags,dictionary,scope)
    val translations=NovelAiPromptTranslationService(dictionary,tags)
    private val tokens=NovelAiPromptTokenCounter(::open)
    private val codex by lazy {NovelAiCodexCatalogParser(Json{ignoreUnknownKeys=true}).parse(open(NOVEL_AI_CODEX_ASSET_PATH).bufferedReader().use{it.readText()}).also {check(it.fatalError==null && it.errors.isEmpty())}}
    fun designer(transport:NovelAiTextTransport)=NovelAiPromptDesigner(transport,
        NovelAiTagResearchService(LlmNovelAiTagSearchPlanner(transport),tags,NovelAiCodexSearchEngine(codex.catalog)),NovelAiPromptPostProcessor(codex.catalog.rewriteRules))
    suspend fun countTokens(draft:NovelAiStudioDraft)=withContext(Dispatchers.IO){tokens.count(draft.toPromptPlan(),draft.selectedModel)}
    suspend fun closeAndDrain(){scope.coroutineContext[Job]?.cancelAndJoin()}
}
