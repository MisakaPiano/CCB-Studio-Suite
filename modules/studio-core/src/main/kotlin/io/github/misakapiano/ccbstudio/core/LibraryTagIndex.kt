package io.github.misakapiano.ccbstudio.core

/** Management labels are independent of original Prompt tags. Counts use unique assets in scope. */
class LibraryTagIndex(ids:Collection<String>,labels:Map<String,AssetLabels>) {
    val assetCount:Int
    val counts:Map<String,Int>
    init {
        val unique=ids.toSet();assetCount=unique.size
        val totals=linkedMapOf<String,Int>();val perAsset=hashSetOf<String>()
        unique.forEach{id->
            perAsset.clear()
            labels[id]?.tags.orEmpty().forEach{tag->if(perAsset.add(tag))totals[tag]=(totals[tag]?:0)+1}
        }
        counts=totals
    }
    fun suggestions(text:String):List<String> = counts.keys.filter{it.contains(text.trim(),ignoreCase=true)}
        .sortedWith { a,b->
            val prefixA=a.startsWith(text.trim(),true);val prefixB=b.startsWith(text.trim(),true)
            if(prefixA!=prefixB)if(prefixA)-1 else 1 else galleryNaturalCompare(a,b)
        }
}
fun matchesLibraryTags(actual:List<String>,conditions:List<String>,any:Boolean=false):Boolean =
    conditions.isEmpty() || if(any)conditions.any{it in actual}else conditions.all{it in actual}

/** Only visible order participates in a range. Selecting is separate from opening and navigation. */
fun selectLibraryRange(order:List<String>,current:Set<String>,target:String,anchor:String?,ctrl:Boolean,shift:Boolean):Set<String> {
    if(target !in order)return current
    if(shift) {
        val from=order.indexOf(anchor).takeIf{it>=0}?:order.indexOf(target)
        val to=order.indexOf(target);val range=order.subList(minOf(from,to),maxOf(from,to)+1).toSet()
        return if(ctrl)current+range else range
    }
    return if(ctrl){if(target in current)current-target else current+target}else setOf(target)
}
