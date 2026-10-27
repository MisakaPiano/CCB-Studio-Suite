package io.github.misakapiano.ccbstudio.core

import java.awt.image.BufferedImage

/** Page-worker owned, never shared across threads. The cached prefix uses the same
 * primitives and immutable original sampling as export, without changing the recipe. */
class TypesetPreviewSession(original:ByteArray,private val coordinates:String):AutoCloseable {
    private val source=TypesetImage.decode(original,coordinates)
    private var prefix:BufferedImage?=null
    private var prefixLayers=emptyList<Layer>()
    private var closed=false
    private var sourceAsset:String?=null
    private data class Layer(val text:TypesetText?=null,val mask:TypesetMask?=null) {
        val id get()=text?.id?:mask!!.id
    }
    internal var layersDrawn=0L;private set
    internal var prefixRebuilds=0L;private set
    private fun layers(page:TypesetPage):List<Layer> {
        val texts=page.texts.filter{it.visible}.associateBy{it.id}
        val masks=page.masks.filter{it.visible}.associateBy{it.id}
        return TypesetObjects.order(page).mapNotNull{id->texts[id]?.let{Layer(text=it)}?:masks[id]?.let{Layer(mask=it)}}
    }
    private fun invalidate(){prefix?.flush();prefix=null;prefixLayers=emptyList()}
    private fun draw(image:BufferedImage,layers:List<Layer>){
        val g=image.createGraphics()
        try{TypesetRenderer.quality(g);layers.forEach{layer->
            layer.text?.let{TypesetRenderer.text(g,it)}
            layer.mask?.let{TypesetMaskRenderer.draw(g,source,it)}
            layersDrawn++
        }}finally{g.dispose()}
    }
    fun render(page:TypesetPage,changingObject:String?):BufferedImage {
        check(!closed){"Preview session is closed"}
        require(page.coordinates==coordinates){"Preview image coordinates changed"}
        require(sourceAsset==null || sourceAsset==page.assetId){"Preview original changed"}
        sourceAsset=page.assetId
        val layers=layers(page)
        val boundary=layers.indexOfFirst{it.id==changingObject}.takeIf{it>=0}?:layers.size
        val stable=layers.take(boundary)
        var output:BufferedImage?=null
        try {
            if(prefix==null || prefixLayers.size>stable.size || prefixLayers!=stable.take(prefixLayers.size)){
                invalidate()
                prefix=BufferedImage(source.width,source.height,BufferedImage.TYPE_INT_ARGB).also{image->
                    val g=image.createGraphics();try{TypesetRenderer.quality(g);g.drawImage(source,0,0,null)}finally{g.dispose()}
                }
                prefixRebuilds++
            }
            draw(prefix!!,stable.drop(prefixLayers.size));prefixLayers=stable
            output=BufferedImage(source.width,source.height,BufferedImage.TYPE_INT_ARGB)
            output.raster.setRect(prefix!!.raster)
            draw(output,layers.drop(boundary))
            return output
        }catch(error:Throwable){
            output?.flush()
            // A failed draw may have changed part of the prefix. Never reuse it on retry.
            invalidate()
            throw error
        }
    }
    override fun close(){if(!closed){closed=true;invalidate();source.flush()}}
}
