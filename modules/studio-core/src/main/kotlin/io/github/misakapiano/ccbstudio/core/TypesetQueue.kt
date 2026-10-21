package io.github.misakapiano.ccbstudio.core

object TypesetQueue {
    /** Move the selected block in its existing queue order. An anchor in the selection is a no-op. */
    fun move(document:TypesetDocument,selected:Set<String>,anchor:String?,after:Boolean):TypesetDocument {
        if(selected.isEmpty() || anchor in selected)return document
        val moving=document.pages.filter{it.id in selected};val rest=document.pages.filterNot{it.id in selected}.toMutableList()
        val index=if(anchor==null){if(after)rest.size else 0}else{val i=rest.indexOfFirst{it.id==anchor};require(i>=0);i+if(after)1 else 0}
        rest.addAll(index,moving);return document.copy(pages=rest)
    }
    fun number(document:TypesetDocument,index:Int)=(document.startNumber+index).toString().padStart(document.numberDigits,'0')
    /** TypeSet moveEntryByPrompt targets the displayed serial, rounded like Math.round, not a list index. */
    fun moveByNumber(document:TypesetDocument,selected:Set<String>,number:Double,after:Boolean):TypesetDocument {
        require(number.isFinite()) {"输入的序号无效"}
        val index=kotlin.math.floor(number+.5)-document.startNumber
        require(index>=0 && index<document.pages.size) {"目标序号超出当前列表范围"}
        return move(document,selected,document.pages[index.toInt()].id,after)
    }
}
