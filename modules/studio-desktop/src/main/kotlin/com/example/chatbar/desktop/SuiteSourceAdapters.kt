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
    private val palette get()=io.github.misakapiano.ccbstudio.desktop.StudioAppearanceRuntime.palette
    val background get()=palette.background;val foreground get()=palette.foreground;val card get()=palette.surface
    val selectionInk get()=palette.selectionInk
    val primary get()=palette.primary;val primaryForeground get()=palette.primaryForeground;val muted get()=palette.muted
    val mutedForeground get()=palette.mutedForeground;val border get()=palette.border;val input get()=palette.input
    val accent get()=palette.accent;val warning get()=palette.warning;val destructive get()=palette.destructive
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
    io.github.misakapiano.ccbstudio.desktop.StudioImageWindow(title,close,width,height) {
        io.github.misakapiano.ccbstudio.desktop.StudioTheme {
            androidx.compose.material.Surface(color=DesktopBootstrapColors.background,contentColor=DesktopBootstrapColors.foreground,modifier=Modifier.fillMaxSize()){content()}
        }
    }
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
    var translations by mutableStateOf(NovelAiPromptTranslationService(dictionary,tags));private set
    fun refreshTranslations(){translations=NovelAiPromptTranslationService(dictionary,tags)}
    private val tokens=NovelAiPromptTokenCounter(::open)
    private val codex by lazy {NovelAiCodexCatalogParser(Json{ignoreUnknownKeys=true}).parse(open(NOVEL_AI_CODEX_ASSET_PATH).bufferedReader().use{it.readText()}).also {check(it.fatalError==null && it.errors.isEmpty())}}
    fun designer(transport:NovelAiTextTransport,understanding:com.example.chatbar.domain.chat.ImageUnderstandingService?=null)=NovelAiPromptDesigner(transport,
        NovelAiTagResearchService(LlmNovelAiTagSearchPlanner(transport),tags,NovelAiCodexSearchEngine(codex.catalog)),NovelAiPromptPostProcessor(codex.catalog.rewriteRules),{understanding})
    suspend fun countTokens(draft:NovelAiStudioDraft)=withContext(Dispatchers.IO){tokens.count(draft.toPromptPlan(),draft.selectedModel)}
    suspend fun closeAndDrain(){scope.coroutineContext[Job]?.cancelAndJoin()}
}
