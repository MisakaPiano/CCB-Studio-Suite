package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.chatbar.domain.appearance.DefaultThemeColorHsv
import io.github.misakapiano.ccbstudio.core.LibraryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

internal enum class StudioThemeMode(val label:String) { SYSTEM("跟随系统"), LIGHT("浅色"), DARK("深色") }
internal enum class StudioColorStyle(val label:String) { NEUTRAL("中性"), CCB_NATIVE("CCB 原生"), CUSTOM_ACCENT("自定义强调色") }

/** Local presentation only. Existing installations retain their dark/blue appearance. */
internal data class StudioDisplaySettings(
    val mode:StudioThemeMode=StudioThemeMode.DARK,
    val colorStyle:StudioColorStyle=StudioColorStyle.CUSTOM_ACCENT,
    val accentRgb:Int=0x7db5ff,
) {
    init { require(accentRgb in 0..0xffffff) }
    fun encode():String=buildJsonObject {
        put("format",1);put("mode",mode.name);put("colorStyle",colorStyle.name);put("accentRgb",accentRgb)
    }.toString()
    companion object {
        const val KEY="studioDisplayAppearance"
        fun decode(raw:String?):StudioDisplaySettings {
            if(raw==null)return StudioDisplaySettings()
            require(raw.length<=1024){"Display settings exceed their limit"}
            val value=Json.parseToJsonElement(raw).jsonObject
            require(value.keys==setOf("format","mode","colorStyle","accentRgb")) {"Unknown display settings fields"}
            require(!value.getValue("format").jsonPrimitive.isString&&!value.getValue("accentRgb").jsonPrimitive.isString)
            require(value.getValue("mode").jsonPrimitive.isString&&value.getValue("colorStyle").jsonPrimitive.isString)
            require(value.getValue("format").jsonPrimitive.int==1){"Unsupported display settings format"}
            return StudioDisplaySettings(StudioThemeMode.valueOf(value.getValue("mode").jsonPrimitive.content),
                StudioColorStyle.valueOf(value.getValue("colorStyle").jsonPrimitive.content),value.getValue("accentRgb").jsonPrimitive.int)
        }
    }
}

internal class StudioAppearanceStore(private val repository:LibraryRepository) {
    fun load()=StudioDisplaySettings.decode(repository.load().preferences[StudioDisplaySettings.KEY])
    fun save(settings:StudioDisplaySettings) {
        val raw=settings.encode()
        repository.update { latest ->
            // Recheck the latest revision: a failed/unknown record must never be silently replaced.
            StudioDisplaySettings.decode(latest.preferences[StudioDisplaySettings.KEY])
            latest.copy(preferences=latest.preferences+(StudioDisplaySettings.KEY to raw))
        }
    }
}

internal data class StudioSemanticColors(
    val dark:Boolean,val background:Color,val foreground:Color,val surface:Color,val muted:Color,
    val mutedForeground:Color,val border:Color,val input:Color,val primary:Color,val primaryForeground:Color,
    val accent:Color,val warning:Color,val warningBackground:Color,val destructive:Color,
    val libraryBackground:Color,val libraryPanel:Color,val libraryLine:Color,val libraryText:Color,val libraryMuted:Color,
)

/** Adapted to Suite tokens from DesktopTheme.kt at Desktop d53d46ca8fe0dbac11c33e85b38e07099b105da8.
 * Pure palette math only; no Desktop store, credentials, background-image or character appearance access. */
internal fun studioSemanticColors(settings:StudioDisplaySettings,systemDark:Boolean):StudioSemanticColors {
    val dark=when(settings.mode){StudioThemeMode.SYSTEM->systemDark;StudioThemeMode.LIGHT->false;StudioThemeMode.DARK->true}
    val primary=when(settings.colorStyle) {
        StudioColorStyle.NEUTRAL->if(dark)Color(0xffdce4ed)else Color(0xff253244)
        StudioColorStyle.CCB_NATIVE->Color(DefaultThemeColorHsv.toOpaqueArgb())
        StudioColorStyle.CUSTOM_ACCENT->Color(0xff000000.toInt() or settings.accentRgb)
    }
    val card=if(dark)Color(0xff161b22)else Color.White
    val fraction=if(dark).28f else .14f
    val accent=if(dark && primary==Color(0xff7db5ff))Color(0xff233e5e)else Color(
        primary.red*fraction+card.red*(1-fraction),primary.green*fraction+card.green*(1-fraction),primary.blue*fraction+card.blue*(1-fraction))
    return StudioSemanticColors(dark,
        if(dark)Color(0xff0d1117)else Color(0xfff8fafc),if(dark)Color(0xffe6edf3)else Color(0xff0f172a),card,
        if(dark)Color(0xff222a35)else Color(0xfff1f5f9),if(dark)Color(0xffaab6c5)else Color(0xff5a687a),
        if(dark)Color(0xff303946)else Color(0xffd9e1ea),if(dark)Color(0xff111820)else Color.White,
        primary,if(studioContrastRatio(primary,Color.White)>=studioContrastRatio(primary,Color.Black))Color.White else Color.Black,
        accent,if(dark)Color(0xffffc16d)else Color(0xff9a5700),if(dark)Color(0xff34291d)else Color(0xfffff3dd),
        if(dark)Color(0xffff7b7b)else Color(0xffb91c1c),
        if(dark)Color(0xff0d1219)else Color(0xfff8fafc),if(dark)Color(0xff171e28)else Color.White,
        if(dark)Color(0xff344254)else Color(0xffd9e1ea),if(dark)Color(0xffe0e7f1)else Color(0xff0f172a),
        if(dark)Color(0xffa4b2c5)else Color(0xff5a687a))
}

internal fun studioContrastRatio(first:Color,second:Color):Double {
    fun linear(v:Float):Double=if(v<=.04045f)v.toDouble()/12.92 else ((v+.055)/1.055).pow(2.4)
    fun luminance(c:Color)=.2126*linear(c.red)+.7152*linear(c.green)+.0722*linear(c.blue)
    val a=luminance(first);val b=luminance(second)
    return (max(a,b)+.05)/(min(a,b)+.05)
}

/** One application/workspace per process; all detached windows observe this same palette.
 * Regular state-reading getters also work in the existing non-composable drawing callbacks. */
internal object StudioAppearanceRuntime {
    var palette by mutableStateOf(studioSemanticColors(StudioDisplaySettings(),true))
}

internal class StudioAppearanceController(private val c:WorkbenchController) {
    private val store=StudioAppearanceStore(c.library.repository)
    var settings by mutableStateOf(StudioDisplaySettings());private set
    var loaded by mutableStateOf(false);private set
    var loading by mutableStateOf(false);private set
    var error by mutableStateOf<String?>(null);private set
    var shown by mutableStateOf(false)
    fun load() {
        if(loading)return
        loading=true
        c.scope.launch {
            try {settings=withContext(Dispatchers.IO){store.load()};loaded=true;error=null}
            catch(e:CancellationException){throw e}
            catch(e:Exception){loaded=false;error="显示主题记录无法读取，原记录保留；请先处理后重试（${e.javaClass.simpleName}）"}
            finally {loading=false}
        }
    }
    fun save(next:StudioDisplaySettings) {
        if(!loaded||loading)return
        c.run("保存显示主题") {store.save(next);settings=next;error=null}
    }
}

@Composable internal fun StudioAppearanceHost(c:WorkbenchController) {
    val appearance=c.appearance
    val systemDark=isSystemInDarkTheme()
    val palette=remember(appearance.settings,systemDark){studioSemanticColors(appearance.settings,systemDark)}
    SideEffect {StudioAppearanceRuntime.palette=palette}
    LaunchedEffect(c){withFrameNanos{};appearance.load()}
}

@Composable internal fun StudioTheme(content:@Composable ()->Unit) {
    val p=StudioAppearanceRuntime.palette
    val colors=if(p.dark)darkColors()else lightColors()
    MaterialTheme(colors=colors.copy(primary=p.primary,primaryVariant=p.primary,secondary=p.primary,secondaryVariant=p.primary,
        background=p.background,surface=p.surface,error=p.destructive,onPrimary=p.primaryForeground,onSecondary=p.primaryForeground,
        onBackground=p.foreground,onSurface=p.foreground,onError=if(p.dark)Color.Black else Color.White),content=content)
}

@Composable internal fun StudioAppearanceDialog(c:WorkbenchController) {
    val appearance=c.appearance
    if(!appearance.shown)return
    Dialog(onDismissRequest={appearance.shown=false}) {
        StudioTheme {Surface(color=Ink,contentColor=com.example.chatbar.desktop.DesktopBootstrapColors.foreground,shape=RoundedCornerShape(8.dp)) {
            Column(Modifier.widthIn(min=360.dp,max=620.dp).heightIn(max=650.dp).verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Text("显示主题",style=MaterialTheme.typography.h6)
                if(appearance.loading)Text("正在读取显示设置…")
                appearance.error?.let {Text(it,color=MaterialTheme.colors.error);TextButton(onClick=appearance::load){Text("重新读取")}}
                val enabled=appearance.loaded&&!appearance.loading&&!c.busy&&!c.maintenance
                val settings=appearance.settings
                Text("深浅模式")
                Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){StudioThemeMode.entries.forEach {mode->
                    LibraryAction(mode.label,enabled,settings.mode==mode,icon=null){appearance.save(settings.copy(mode=mode))}
                }}
                Text("配色风格")
                Column(verticalArrangement=Arrangement.spacedBy(4.dp)){StudioColorStyle.entries.forEach {style->
                    LibraryAction(style.label,enabled,settings.colorStyle==style,icon=null){appearance.save(settings.copy(colorStyle=style))}
                }}
                if(settings.colorStyle==StudioColorStyle.CUSTOM_ACCENT) {
                    Text("强调色")
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        listOf(0x7db5ff,DefaultThemeColorHsv.rgbKey(),0x3b66e6,0x8254d1,0xd15747,0xd9942b).distinct().forEach {rgb->
                            Box(Modifier.size(32.dp).background(Color(0xff000000.toInt() or rgb),RoundedCornerShape(5.dp))
                                .border(if(settings.accentRgb==rgb)3.dp else 1.dp,com.example.chatbar.desktop.DesktopBootstrapColors.foreground,RoundedCornerShape(5.dp))
                                .semantics {contentDescription="强调色 #%06X".format(rgb)}
                                .clickable(enabled=enabled){appearance.save(settings.copy(accentRgb=rgb))})
                        }
                    }
                    var hex by remember(settings.accentRgb){mutableStateOf("%06X".format(settings.accentRgb))}
                    val rgb=hex.takeIf{it.matches(Regex("[0-9a-fA-F]{6}"))}?.toIntOrNull(16)
                    OutlinedTextField(hex,{hex=it.take(6)},label={Text("RGB 十六进制（6位）")},singleLine=true,enabled=enabled)
                    TextButton(enabled=enabled&&rgb!=null,onClick={appearance.save(settings.copy(accentRgb=requireNotNull(rgb)))}){Text("应用颜色")}
                }
                Text("切换后自动保存；不会改变图片、编辑画布内容或原文件。",style=MaterialTheme.typography.caption,color=Quiet)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    TextButton(enabled=enabled,onClick={appearance.save(StudioDisplaySettings())}){Text("恢复 Studio 默认")}
                    TextButton(onClick={appearance.shown=false}){Text("关闭")}
                }
            }
        }}
    }
}
