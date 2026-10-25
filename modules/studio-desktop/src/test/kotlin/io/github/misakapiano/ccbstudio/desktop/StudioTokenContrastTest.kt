@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class,androidx.compose.ui.InternalComposeUiApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.material.Surface
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextLayoutResult
import com.example.chatbar.desktop.DesktopStudioFooter
import com.example.chatbar.domain.image.NovelAiPromptTokenUsage
import kotlinx.coroutines.*
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.test.*

/** Actual footer pixels and semantics, synthetic counts only; no tokenizer/provider/workspace. */
class StudioTokenContrastTest {
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    private fun ImageComposeScene.nodes():List<SemanticsNode> {
        fun walk(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::walk)
        return semanticsOwners.flatMap{walk(it.rootSemanticsNode)}
    }
    @Test fun actualFooterTokenTextIsReadableWithoutChangingCountsWarningsOrAccentFills()=runBlocking {withContext(awt) {
        val previous=StudioAppearanceRuntime.palette
        try {for(mode in listOf(StudioThemeMode.DARK,StudioThemeMode.LIGHT))for(rgb in listOf(0,0xffffff,0x7db5ff)) {
            val p=studioSemanticColors(StudioDisplaySettings(mode,StudioColorStyle.CUSTOM_ACCENT,rgb),false)
            StudioAppearanceRuntime.palette=p
            for(width in listOf(300,660))for(counts in listOf(1 to 152,461 to 513)) {
                val scene=ImageComposeScene(width,230,coroutineContext=coroutineContext)
                try {
                    scene.setContent {StudioTheme {Surface(color=p.surface) {
                        DesktopStudioFooter(NovelAiPromptTokenUsage(counts.first,counts.second,512),false,false,
                            "合成生成入口",false,{},{},{})
                    }}}
                    repeat(4){scene.render(System.nanoTime()).close();yield()};delay(40)
                    val nodes=scene.nodes()
                    val pixels=scene.render(System.nanoTime()).use{image->image.encodeToData()!!.use{ImageIO.read(ByteArrayInputStream(it.bytes))}}
                    val expected=listOf(
                        Triple("正向 Tokens ${counts.first}/512"+if(counts.first==461)" · 接近上限" else "",counts.first,
                            if(counts.first==461)p.warning else p.selectionInk),
                        Triple("负向 Tokens ${counts.second}/512"+if(counts.second==513)" · 超出上限" else "",counts.second,
                            if(counts.second==513)p.destructive else p.selectionInk))
                    for((label,_,ink) in expected) {
                        val text=nodes.single{it.config.getOrNull(SemanticsProperties.Text)?.any{t->t.text==label}==true}
                        val layout=mutableListOf<TextLayoutResult>()
                        assertTrue(text.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(layout))
                        assertEquals(ink,layout.single().layoutInput.style.color,"$mode/$rgb/$width/$label ink")
                        assertTrue(studioContrastRatio(ink,p.surface)>=4.5,"$mode/$rgb/$label foreground contrast")
                        val b=text.boundsInRoot;var readable=0
                        for(y in floor(b.top).toInt().coerceAtLeast(0) until ceil(b.bottom).toInt().coerceAtMost(pixels.height))
                            for(x in floor(b.left).toInt().coerceAtLeast(0) until ceil(b.right).toInt().coerceAtMost(pixels.width))
                                if(studioContrastRatio(Color(pixels.getRGB(x,y)),p.surface)>=4.5)readable++
                        assertTrue(readable>=3,"$mode/$rgb/$width/$label rendered foreground pixels=$readable")
                    }
                    val bars=nodes.filter{it.config.getOrNull(SemanticsProperties.ProgressBarRangeInfo)!=null}.sortedBy{it.boundsInRoot.top}
                    assertEquals(2,bars.size)
                    expected.forEachIndexed {index,(_,count,_)->
                        val fraction=(count/512f).coerceIn(0f,1f)
                        assertEquals(ProgressBarRangeInfo(fraction,0f..1f),bars[index].config[SemanticsProperties.ProgressBarRangeInfo])
                        // Count1 may be subpixel-wide; the second bar has a stable interior sample.
                        if(index==1) {
                            val b=bars[index].boundsInRoot
                            val actual=Color(pixels.getRGB(floor(b.left+b.width*fraction/2).toInt(),floor(b.center.y).toInt()))
                            assertEquals(if(count==513)p.destructive else p.primary,actual,"Original progress fill must remain unchanged")
                        }
                    }
                    assertEquals(Color(0xff000000.toInt() or rgb),p.primary)
                    println("token-contrast mode=$mode rgb=%06X width=$width counts=${counts.first}/${counts.second}: actual text pixels, warning and original fill verified".format(rgb))
                } finally {scene.close()}
            }
        }} finally {StudioAppearanceRuntime.palette=previous}
    }}
}
