package io.github.misakapiano.ccbstudio.core

/** TypeSet 1.1 batchApplyCurrentSettingsToSelected / batchCopyPreviousSettingsToSelected.
 * All previous-page sources come from the input snapshot, never the evolving result. */
object TypesetBatch {
    fun apply(document:TypesetDocument, selected:Set<String>, previous:Boolean,
              currentText:TypesetText?, currentMask:TypesetMask, kind:String="both", imageWidth:(TypesetPage)->Int):TypesetDocument {
        require(kind in setOf("text","mask","both"))
        val pages=document.pages.mapIndexed { index, page ->
            if(page.id !in selected || previous && index==0) return@mapIndexed page
            val source=if(previous)document.pages[index-1]else null
            val maskStyle=if(previous)source!!.toolDefaults?:source.masks.firstOrNull()else currentMask
            val texts=if(kind=="mask")page.texts else page.texts.mapIndexed { i,target ->
                val style=if(previous)source!!.texts.getOrNull(minOf(i,source.texts.lastIndex))else currentText
                if(style==null)target else style.copy(id=target.id,text=target.text,x=target.x,y=target.y,
                    visible=target.visible,locked=target.locked,width=maxOf(20.0,minOf(style.width,imageWidth(page)*.95)))
            }
            page.copy(texts=texts,toolDefaults=if(kind=="text")page.toolDefaults else maskStyle?:page.toolDefaults,masks=if(kind=="text")page.masks else page.masks.map { mask ->
                if(maskStyle==null)mask else mask.copy(effect=maskStyle.effect,strength=maskStyle.strength,
                    size=if(mask.shape=="brush")maskStyle.size else mask.size,
                    density=if(mask.shape=="brush")maskStyle.density else mask.density,
                    stripeThickness=if(mask.shape=="brush")maskStyle.stripeThickness else mask.stripeThickness,
                    pointDensity=if(mask.shape=="brush")maskStyle.pointDensity else mask.pointDensity)
            })
        }
        return document.copy(pages=pages).also{it.validate()}
    }
}
