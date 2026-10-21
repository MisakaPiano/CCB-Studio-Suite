package io.github.misakapiano.ccbstudio.core

/** TypeSet 1.1 STORYBOOK_PRESETS, with explicit local Windows font equivalents. */
object TypesetBook {
    val presets=linkedMapOf(
        "traditional" to StoryStyle(),
        "clean" to StoryStyle(pagePadding=56,gap=34,textPadding=26,textMinHeight=150,fontFamily="Microsoft YaHei",fontSize=34.0,
            pageBackground="#ffffff",textColor="#16191d",borderColor="#d8dde3",pageBorder=false,imageBorder=false),
        "vintage" to StoryStyle(pagePadding=52,gap=26,textMinHeight=180,fontFamily="FangSong",lineHeight=1.6,
            pageBackground="#efe2c4",textColor="#241b12",borderColor="#5a4630"),
    )
    fun next(document:TypesetDocument,current:String):String?=document.pages.indexOfFirst{it.id==current}.let{if(it<0)null else document.pages.getOrNull(it+1)?.id}
}
