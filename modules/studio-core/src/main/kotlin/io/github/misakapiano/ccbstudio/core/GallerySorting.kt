package io.github.misakapiano.ccbstudio.core

import com.example.chatbar.domain.image.*
import com.example.chatbar.ui.imageprompt.*
import io.github.misakapiano.ccbstudio.assets.AssetStore
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.nio.file.Path
import java.util.Locale

@Serializable enum class GallerySortField(val label:String) { ADDED("添加时间"), NAME("文件名"), SIZE("文件大小"), TYPE("文件类型") }
@Serializable data class GallerySort(val field:GallerySortField=GallerySortField.ADDED,val descending:Boolean=true)
@Serializable private data class GallerySortRecord(val version:Int=1,val albums:Map<String,GallerySort> = emptyMap())

/** Preference identities contain hashes, never private prompts or source paths. */
fun galleryAlbumIdentity(parent:String,album:NovelAiHistoryAlbum,type:NovelAiHistoryFoldType,directory:Boolean,assets:List<AssetStore.Asset>):String {
    val item=if(directory)album.cover else album.images.minWith(compareBy({it.entry.createdAt},{it.entry.id}))
    val key=if(directory) {
        val asset=assets.first{it.request().id().toString()==item.image.path}
        runCatching {
            val opaque=asset.request().recipe()
            require(opaque.size<=16384)
            val receipt=Json.parseToJsonElement(opaque.decodeToString()).jsonObject
            require(receipt["kind"]?.jsonPrimitive?.content=="image-ingress" && receipt["version"]?.jsonPrimitive?.intOrNull==1)
            when(receipt["source"]?.jsonPrimitive?.content) {
                "managed-folder"->listOf("collection",receipt.getValue("folderId").jsonPrimitive.content,Path.of(receipt.getValue("relativePath").jsonPrimitive.content).parent?.toString().orEmpty())
                "file"->listOf("path",Path.of(receipt.getValue("sourcePath").jsonPrimitive.content).parent.toString())
                else->listOf("asset",item.image.path)
            }
        }.getOrElse{listOf("asset",item.image.path)}
    }else listOf(type.name)+NovelAiHistoryFolding.key(item.entry,type)
    return sha256(documentJson.encodeToString(listOf(parent)+key).toByteArray())
}

class GallerySortPreferences(private val library:LibraryRepository) {
    fun load(state:LibraryState=library.load()):Map<String,GallerySort> {
        val raw=state.preferences["gallerySorts"]?:return emptyMap()
        val record=documentJson.decodeFromString<GallerySortRecord>(raw)
        require(record.version==1 && record.albums.size<=4096 && record.albums.keys.all{it=="root"||it.matches(Regex("[a-f0-9]{64}"))})
        return record.albums
    }
    fun save(album:String,sort:GallerySort) { library.update { state->
        require(album=="root"||album.matches(Regex("[a-f0-9]{64}")))
        val values=load(state)+(album to sort);require(values.size<=4096){"相册排序记录已达上限"}
        state.copy(preferences=state.preferences+("gallerySorts" to documentJson.encodeToString(GallerySortRecord(albums=values))))
    }}
}

/** Natural numeric name order; stable ID tie-break keeps paging and range selection deterministic. */
private val naturalParts=Regex("\\d+|\\D+")
private fun naturalKey(value:String)=naturalParts.findAll(value.lowercase(Locale.ROOT)).map{it.value}.toList()
fun galleryNaturalCompare(a:String,b:String):Int=compareNaturalKeys(naturalKey(a),naturalKey(b))
/** One synchronous sort owns its keys; no cross-query or retained corpus cache. */
internal fun galleryNaturalComparator():Comparator<String> {
    val keys=HashMap<String,List<String>>()
    return Comparator{a,b->compareNaturalKeys(keys.getOrPut(a){naturalKey(a)},keys.getOrPut(b){naturalKey(b)})}
}
private fun compareNaturalKeys(left:List<String>,right:List<String>):Int {
    for(i in 0 until minOf(left.size,right.size)) {
        val x=left[i];val y=right[i]
        val compared=if(x.first().isDigit()&&y.first().isDigit()) {
            val n=x.trimStart('0').ifEmpty{"0"};val m=y.trimStart('0').ifEmpty{"0"}
            n.length.compareTo(m.length).takeIf{it!=0}?:n.compareTo(m)
        }else x.compareTo(y)
        if(compared!=0)return compared
    }
    return left.size.compareTo(right.size)
}
fun sortGalleryAssets(assets:List<AssetStore.Asset>,sort:GallerySort,added:Map<String,Long>,names:Map<String,String> = emptyMap()):List<AssetStore.Asset> {
    // Invocation-local metadata only: no retained corpus, image reads or stale alias keys.
    class Key(val asset:AssetStore.Asset) {
        val id=asset.request().id().toString()
        val name by lazy(LazyThreadSafetyMode.NONE){naturalKey(names[id]?.takeIf{it.isNotBlank()}?:asset.request().name())}
        val type=if(sort.field==GallerySortField.TYPE)asset.request().name().substringAfterLast('.',"").lowercase(Locale.ROOT)else ""
    }
    val keys=assets.map{Key(it)}
    return keys.sortedWith { a,b->
    val x=a.id;val y=b.id
    val result=when(sort.field) {
        GallerySortField.ADDED->(added[x]?:Long.MIN_VALUE).compareTo(added[y]?:Long.MIN_VALUE)
        GallerySortField.NAME->compareNaturalKeys(a.name,b.name)
        GallerySortField.SIZE->a.asset.size().compareTo(b.asset.size())
        GallerySortField.TYPE->a.type.compareTo(b.type)
    }
    val primary=if(sort.descending)-result else result
    primary.takeIf{it!=0}?:compareNaturalKeys(a.name,b.name).takeIf{it!=0}?:x.compareTo(y)
    }.map{it.asset}
}
