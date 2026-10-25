@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class,androidx.compose.ui.InternalComposeUiApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.example.chatbar.desktop.DesktopNovelAiInfrastructure
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.nio.file.Path
import javax.imageio.ImageIO
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.test.*

/** Targeted synthetic/offscreen checks, not Windows theme events or user visual acceptance. */
class StudioSelectedContrastTest {
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    private fun root()=Path.of(requireNotNull(System.getProperty("preview.test.root")),newId())
    private val extremes=listOf(StudioDisplaySettings(StudioThemeMode.DARK,accentRgb=0),
        StudioDisplaySettings(StudioThemeMode.LIGHT,accentRgb=0xffffff))
    private suspend fun ImageComposeScene.frames(){repeat(4){render(System.nanoTime()).close();yield()};delay(80)}
    private fun ImageComposeScene.nodes():List<SemanticsNode> {
        fun walk(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::walk)
        return semanticsOwners.flatMap{walk(it.rootSemanticsNode)}
    }
    private fun SemanticsNode.matches(label:String)=config.getOrNull(SemanticsProperties.Text)?.any{it.text==label}==true||
        config.getOrNull(SemanticsProperties.ContentDescription)?.contains(label)==true
    private fun ImageComposeScene.pixels():BufferedImage=render(System.nanoTime()).use {image->
        image.encodeToData()!!.use {ImageIO.read(ByteArrayInputStream(it.bytes))}
    }
    private fun assertReadableMark(image:BufferedImage,node:SemanticsNode,background:Color,label:String,edgeOnly:Boolean=false,leadingWidth:Int?=null) {
        val bounds=node.boundsInRoot
        val left=floor(bounds.left).toInt().coerceAtLeast(0)
        val right=minOf(ceil(bounds.right).toInt(),image.width,leadingWidth?.let{left+it}?:image.width)
        val top=floor(bounds.top).toInt().coerceAtLeast(0);val bottom=ceil(bounds.bottom).toInt().coerceAtMost(image.height)
        var marks=0
        for(y in top until bottom)for(x in left until right) {
            if(edgeOnly && !(y==top && x>=left+6 && x<right-6))continue
            val pixel=Color(image.getRGB(x,y))
            if(studioContrastRatio(pixel,background)>=4.5)marks++
        }
        assertTrue(marks>=3,"$label has no readable ${if(edgeOnly)"border" else "foreground"}: $marks pixels")
        println("selected-contrast consumer=$label readablePixels=$marks edgeOnly=$edgeOnly")
    }

    @Test fun indicatorUsesOriginalAccentWhenReadableAndPreservesEveryStoredRgb() {
        for(mode in StudioThemeMode.entries)for(style in StudioColorStyle.entries)for(system in listOf(false,true))
            for(rgb in listOf(0,0xffffff,0xff0000,0x00ff00,0x0000ff,0x777777,0x7db5ff)) {
                val settings=StudioDisplaySettings(mode,style,rgb);val encoded=settings.encode()
                val p=studioSemanticColors(settings,system)
                val surfaces=listOf(p.background,p.surface,p.muted,p.input,p.accent,p.libraryBackground,p.libraryPanel)
                assertTrue(surfaces.all{studioContrastRatio(p.selectionInk,it)>=4.5},"$mode/$style/$system/$rgb")
                if(surfaces.all{studioContrastRatio(p.primary,it)>=4.5})assertEquals(p.primary,p.selectionInk)
                if(style==StudioColorStyle.CUSTOM_ACCENT)assertEquals(Color(0xff000000.toInt() or rgb),p.primary)
                assertEquals(encoded,settings.encode());assertEquals(settings,StudioDisplaySettings.decode(encoded))
            }
        val defaults=studioSemanticColors(StudioDisplaySettings(),true)
        assertEquals(defaults.primary,defaults.selectionInk)
        println("selected-contrast default blue remains unchanged; all mode/style/system/RGB combinations keep format/RGB")
    }

    @Test fun filledBatchCoverAndLibraryControlsRenderReadableForegrounds()=runBlocking(awt) {
        val original=StudioAppearanceRuntime.palette
        try {for(settings in extremes) {
            val palette=studioSemanticColors(settings,true);StudioAppearanceRuntime.palette=palette
            val w=StudioWorkspace.openStudio(root());val scope=CoroutineScope(SupervisorJob()+awt)
            val infrastructure=DesktopNovelAiInfrastructure(root())
            val c=WorkbenchController(w,scope,infrastructure,root(),admissionOverride=RequestAdmission{error("No network permitted")})
            val scene=ImageComposeScene(600,280,coroutineContext=awt){StudioTheme {
                Surface(color=palette.libraryPanel){Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    BatchCreationEntry(c)
                    ActiveCoverBadge()
                    Box(Modifier.semantics{contentDescription="fixture selection checkbox"}){LibrarySelectionCheckbox(true,{})}
                    Box(Modifier.semantics{contentDescription="fixture library toggle"}){LibraryToggle("fixture toggle",true){}}
                }}
            }}
            try {
                scene.frames();val image=scene.pixels()
                for(label in listOf("单次","✓ 使用中","fixture selection checkbox","fixture library toggle"))
                    assertReadableMark(image,scene.nodes().first{it.matches(label)},palette.primary,"${settings.mode}/$label",
                        leadingWidth=if(label=="fixture library toggle")18 else null)
                assertEquals(settings.accentRgb,StudioDisplaySettings.decode(settings.encode()).accentRgb)
            }finally {scene.close();scope.cancel();infrastructure.closeAndDrain();w.close()}
        }}finally {StudioAppearanceRuntime.palette=original}
    }

    @Test fun actualRouteAndLibrarySelectionBordersStayVisibleAndClickable()=runBlocking(awt) {
        val original=StudioAppearanceRuntime.palette
        try {for(settings in extremes+StudioDisplaySettings()) {
            val p=studioSemanticColors(settings,true);StudioAppearanceRuntime.palette=p
            var routeClicks=0;var actionClicks=0
            val scene=ImageComposeScene(360,150,coroutineContext=awt){StudioTheme {
                Surface(color=p.libraryPanel){Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    StudioRouteButton("创作",true,false){routeClicks++}
                    LibraryAction("fixture selected action",selected=true,icon=null){actionClicks++}
                }}
            }}
            try {
                scene.frames();val image=scene.pixels()
                val route=scene.nodes().first{it.matches("创作")&&it.config.getOrNull(SemanticsActions.OnClick)!=null}
                val action=scene.nodes().first{it.matches("fixture selected action")&&it.config.getOrNull(SemanticsActions.OnClick)!=null}
                assertReadableMark(image,route,p.muted,"${settings.mode}/${settings.accentRgb}/route")
                assertReadableMark(image,action,p.accent,"${settings.mode}/${settings.accentRgb}/selected action",edgeOnly=true)
                assertTrue(route.config[SemanticsActions.OnClick].action!!.invoke())
                assertTrue(action.config[SemanticsActions.OnClick].action!!.invoke())
                assertEquals(1,routeClicks);assertEquals(1,actionClicks)
            }finally {scene.close()}
        }}finally {StudioAppearanceRuntime.palette=original}
    }
}
