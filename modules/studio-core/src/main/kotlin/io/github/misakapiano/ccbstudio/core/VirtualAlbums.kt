package io.github.misakapiano.ccbstudio.core

import kotlinx.serialization.Serializable
import java.util.UUID

/** Organization metadata only. Originals and generation receipts never participate in these writes. */
@Serializable data class VirtualAlbum(val id:String=newId(),val name:String,val parent:String?=null,val images:List<String> = emptyList())

object VirtualAlbums {
    fun validate(albums:List<VirtualAlbum>) {
        require(albums.size<=500 && albums.map{it.id}.distinct().size==albums.size)
        val byId=albums.associateBy{it.id}
        albums.forEach { a->
            UUID.fromString(a.id);require(a.name.isNotBlank() && a.name.length<=100 && a.images.size<=20000 && a.images.distinct().size==a.images.size)
            a.images.forEach{UUID.fromString(it)}
            val seen=mutableSetOf(a.id);var p=a.parent
            while(p!=null){require(seen.add(p) && seen.size<=32){"相册不能放入自身或子相册，最多32层"};p=requireNotNull(byId[p]){"上级相册不存在"}.parent}
        }
    }
    fun descendants(albums:List<VirtualAlbum>,id:String):Set<String> {
        validate(albums);val result=mutableSetOf(id)
        do {val n=result.size;albums.filter{it.parent in result}.forEach{result+=it.id}}while(result.size!=n)
        return result
    }
    fun members(albums:List<VirtualAlbum>,id:String):Set<String> = descendants(albums,id).flatMap{key->albums.single{it.id==key}.images}.toSet()
    fun save(albums:List<VirtualAlbum>,album:VirtualAlbum):List<VirtualAlbum> = (albums.filter{it.id!=album.id}+album).also(::validate)
    fun remove(albums:List<VirtualAlbum>,id:String):List<VirtualAlbum> = albums.filter{it.id !in descendants(albums,id)}
    fun associate(albums:List<VirtualAlbum>,ids:Set<String>,targets:Set<String>,from:String?=null):List<VirtualAlbum> {
        require(ids.isNotEmpty() && targets.isNotEmpty() && albums.map{it.id}.containsAll(targets) && (from==null||albums.any{it.id==from}))
        return albums.map{a->a.copy(images=when{a.id in targets->(a.images+ids).distinct();a.id==from->a.images-ids;else->a.images})}.also(::validate)
    }
}
