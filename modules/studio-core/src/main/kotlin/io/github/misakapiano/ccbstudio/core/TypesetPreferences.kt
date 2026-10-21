package io.github.misakapiano.ccbstudio.core

import kotlinx.serialization.Serializable

@Serializable data class MaskToolSettings(
    val effect:String="mosaic",val strength:Int=16,val size:Double=60.0,
    val density:Double=6.0,val stripeThickness:Double=6.0,val pointDensity:Double=8.0,
) {
    fun validate(){require(effect in setOf("black","white","mosaic","blur") && strength in 1..256 && size.isFinite() && size in 1.0..2000.0);require(listOf(density,stripeThickness,pointDensity).all{it.isFinite() && it in 1.0..1000.0})}
    fun apply(target:TypesetMask)=target.copy(effect=effect,strength=strength,size=size,density=density,stripeThickness=stripeThickness,pointDensity=pointDensity)
    companion object {fun from(mask:TypesetMask)=MaskToolSettings(mask.effect,mask.strength,mask.size,mask.density,mask.stripeThickness,mask.pointDensity).also{it.validate()}}
}

@Serializable data class TypesetPreferences(
    val textPresets:List<TypesetPreset> = emptyList(),val selectedTextPresetId:String?=null,
    val textDefault:TypesetText?=null,val maskOverrides:Map<String,MaskToolSettings> = emptyMap(),
    val preferredMaskSlot:String="custom",val currentMask:MaskToolSettings=MaskToolSettings(),
) {
    fun validate(){
        require(textPresets.size<=200 && (selectedTextPresetId==null || textPresets.any{it.id==selectedTextPresetId}))
        TypesetDocument(presets=textPresets).validate()
        textDefault?.let{TypesetDocument(presets=listOf(TypesetPreset(id="00000000-0000-0000-0000-000000000000",name="Default",text=it))).validate()}
        require(maskOverrides.keys.all{it in MASK_SLOTS} && preferredMaskSlot in MASK_SLOTS+"custom")
        maskOverrides.values.forEach{it.validate()};currentMask.validate()
    }
    fun resolveMask(slot:String):MaskToolSettings=maskOverrides[slot]?:when(slot){
        "light_mosaic"->MaskToolSettings("mosaic",10,48.0,6.0,5.0,12.0)
        "heavy_mosaic"->MaskToolSettings("mosaic",28,92.0,6.0,8.0,22.0)
        "fine_stripes"->MaskToolSettings(if(currentMask.effect=="white")"white"else"black",16,70.0,14.0,3.0,14.0)
        "coarse_stripes"->MaskToolSettings(if(currentMask.effect=="white")"white"else"black",16,100.0,6.0,12.0,10.0)
        "custom"->currentMask
        else->error("Unknown mask slot")
    }
    fun saveText(name:String,text:TypesetText,replace:String?=null):TypesetPreferences {
        require(name.isNotBlank() && name.length<=100)
        if(replace!=null)require(textPresets.any{it.id==replace})
        val preset=TypesetPreset(id=replace?:newId(),name=name,text=styleOnly(text))
        return copy(textPresets=if(replace==null)textPresets+preset else textPresets.map{if(it.id==replace)preset else it},selectedTextPresetId=preset.id).also{it.validate()}
    }
    fun removeText(id:String)=copy(textPresets=textPresets.filterNot{it.id==id},selectedTextPresetId=selectedTextPresetId?.takeUnless{it==id}).also{it.validate()}
    companion object {
        val MASK_SLOTS=listOf("light_mosaic","heavy_mosaic","fine_stripes","coarse_stripes")
        fun styleOnly(t:TypesetText)=t.copy(id="00000000-0000-0000-0000-000000000000",text="",x=0.0,y=0.0,visible=true,locked=false)
        fun applyText(style:TypesetText,target:TypesetText)=style.copy(id=target.id,text=target.text,x=target.x,y=target.y,visible=target.visible,locked=target.locked)
        /** TypeSet defaultText: remembered style with image-relative width/font limits. */
        fun newText(width:Int,height:Int,remembered:TypesetText?):TypesetText {
            require(width>0 && height>0)
            val size=maxOf(24.0,kotlin.math.floor(width*.045+.5))
            val base=remembered?:TypesetText(width=minOf(width*.75,900.0),style=TypesetStyle(fontSize=size,strokeWidth=maxOf(1.0,kotlin.math.floor(size*.06+.5))))
            return base.copy(id=newId(),text="",x=width*.5,y=height*.15,visible=true,locked=false,
                width=maxOf(20.0,minOf(base.width,width*.95)),style=base.style.copy(fontSize=base.style.fontSize.coerceIn(6.0,maxOf(6.0,width*.25))))
        }
    }
}
