package io.github.misakapiano.ccbstudio.core

/** Suite layer controls extend TypeSet's default mask-then-text order without flattening originals. */
object TypesetObjects {
    fun order(page:TypesetPage):List<String> {val ids=page.masks.map{it.id}+page.texts.map{it.id};return page.layerOrder+ids.filterNot{it in page.layerOrder}}
    fun move(page:TypesetPage,id:String,direction:Int):TypesetPage {
        require(direction in setOf(-1,1));val order=order(page).toMutableList();val i=order.indexOf(id);val j=i+direction
        if(i<0 || j !in order.indices)return page
        val other=order[j];order[j]=id;order[i]=other;return page.copy(layerOrder=order)
    }
    fun remove(page:TypesetPage,id:String)=page.copy(texts=page.texts.filterNot{it.id==id},masks=page.masks.filterNot{it.id==id},layerOrder=page.layerOrder.filterNot{it==id})
    fun lock(page:TypesetPage,id:String,locked:Boolean)=page.copy(texts=page.texts.map{if(it.id==id)it.copy(locked=locked)else it},masks=page.masks.map{if(it.id==id)it.copy(locked=locked)else it})
    fun copy(page:TypesetPage,id:String,newId:String):TypesetPage {
        val text=page.texts.firstOrNull{it.id==id};val mask=page.masks.firstOrNull{it.id==id}
        require(text!=null || mask!=null)
        return page.copy(texts=page.texts+listOfNotNull(text?.copy(id=newId,x=text.x+20,y=text.y+20,locked=false)),
            masks=page.masks+listOfNotNull(mask?.copy(id=newId,x=mask.x+20,y=mask.y+20,points=mask.points.map{it.copy(x=it.x+20,y=it.y+20)},locked=false)),layerOrder=order(page)+newId)
    }
}
