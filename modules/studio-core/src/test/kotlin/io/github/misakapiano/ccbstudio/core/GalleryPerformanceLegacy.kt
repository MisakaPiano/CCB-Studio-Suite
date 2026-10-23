// Exact Suite HEAD fb20d33 metadata algorithms for before/after comparison only.
package io.github.misakapiano.ccbstudio.core
import com.example.chatbar.domain.image.*
import com.example.chatbar.ui.imageprompt.*
import io.github.misakapiano.ccbstudio.assets.AssetStore
import java.util.Locale
fun legacyNaturalCompare(a:String,b:String):Int {
    val left=Regex("\\d+|\\D+").findAll(a.lowercase(Locale.ROOT)).map{it.value}.toList()
    val right=Regex("\\d+|\\D+").findAll(b.lowercase(Locale.ROOT)).map{it.value}.toList()
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
fun legacySortGalleryAssets(assets:List<AssetStore.Asset>,sort:GallerySort,added:Map<String,Long>,names:Map<String,String> = emptyMap()):List<AssetStore.Asset> = assets.sortedWith { a,b->
    val x=a.request().id().toString();val y=b.request().id().toString()
    fun name(v:AssetStore.Asset)=names[v.request().id().toString()]?.takeIf{it.isNotBlank()}?:v.request().name()
    val result=when(sort.field) {
        GallerySortField.ADDED->(added[x]?:Long.MIN_VALUE).compareTo(added[y]?:Long.MIN_VALUE)
        GallerySortField.NAME->legacyNaturalCompare(name(a),name(b))
        GallerySortField.SIZE->a.size().compareTo(b.size())
        GallerySortField.TYPE->a.request().name().substringAfterLast('.',"").lowercase(Locale.ROOT).compareTo(b.request().name().substringAfterLast('.',"").lowercase(Locale.ROOT))
    }
    val primary=if(sort.descending)-result else result
    primary.takeIf{it!=0}?:legacyNaturalCompare(name(a),name(b)).takeIf{it!=0}?:x.compareTo(y)
}

fun legacyTagAlbums(ids:List<String>,labels:Map<String,AssetLabels>):List<NovelAiHistoryAlbum> {
    val groups=linkedMapOf<String,MutableList<NovelAiHistoryImageItem>>()
    ids.distinct().forEach{id->
        labels[id]?.tags.orEmpty().ifEmpty{listOf("")}.forEach{tag->
            val image=NovelAiGenerationHistoryImage(path=id)
            val item=NovelAiHistoryImageItem(NovelAiGenerationHistoryEntry(id="tag:${sha256(tag.toByteArray())}:$id",images=listOf(image)),image,0)
            groups.getOrPut(tag){mutableListOf()}.add(item)
        }
    }
    return groups.map{(tag,images)->NovelAiHistoryAlbum(images,tag.ifEmpty{"未标记（无标签）"})}
}
