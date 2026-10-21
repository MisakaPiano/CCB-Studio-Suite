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
}
