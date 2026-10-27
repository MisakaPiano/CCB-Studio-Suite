package io.github.misakapiano.ccbstudio.desktop

import io.github.misakapiano.ccbstudio.core.*
import java.awt.image.BufferedImage

/** Immutable text/font identities are checked once per page worker, not once per pointer move.
 * A changed text/style or durable font reference forces validation; export still checks afresh. */
internal class TypesetPreviewFonts(private val workspace:StudioWorkspace) {
    private var checked:Pair<List<Triple<String,TypesetStyle,Boolean>>,List<TypesetFontResource>>?=null
    internal var checks=0;private set
    fun ensure(page:TypesetPage){
        val key=page.texts.filter{it.visible}.map{Triple(it.text,it.style,it.vertical)} to workspace.state.typesetFonts
        if(key==checked)return
        val document=TypesetDocument(pages=listOf(page))
        TypesetFonts.activate(workspace,TypesetFonts.documentFonts(document))
        TypesetFonts.verifyRenderable(workspace,document,"all")
        checked=key;checks++
    }
}

/** All preview consumers activate durable fonts on their IO dispatcher before measuring text. */
internal fun typesetPreviewRaster(workspace:StudioWorkspace,page:TypesetPage,book:Boolean,number:Int,variant:String="all"):BufferedImage {
    if(book || variant in setOf("all","text")) {
        val visible=page.copy(texts=page.texts.filter{it.visible},story=if(book)page.story else page.story.copy(fontFamily="Dialog"))
        val document=TypesetDocument(pages=listOf(visible),mode=if(book)"storybook"else"editor")
        // Empty text boxes still need their durable font before the first entered glyph.
        TypesetFonts.activate(workspace,TypesetFonts.documentFonts(document))
        TypesetFonts.verifyRenderable(workspace,document,variant)
    }
    val bytes=workspace.bytes(page.assetId)
    return if(book)TypesetRenderer.story(bytes,page,number) else TypesetRenderer.render(bytes,page,if(variant=="plain")"original"else variant)
}
