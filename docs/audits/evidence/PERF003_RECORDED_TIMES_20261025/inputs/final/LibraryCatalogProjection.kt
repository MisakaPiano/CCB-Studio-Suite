package io.github.misakapiano.ccbstudio.core

import com.example.chatbar.domain.image.*
import com.example.chatbar.ui.imageprompt.*
import io.github.misakapiano.ccbstudio.assets.AssetStore
import kotlinx.serialization.json.*
import java.time.*

/** Metadata-only projection. Unknown dates remain unknown; no filesystem lookup or guessed defaults. */
fun libraryRecordedTimes(assets:List<AssetStore.Asset>,jobs:List<JobRecord>):Map<String,Long> {
    val imported=assets.mapNotNull{a->runCatching {
        val opaque=a.request().recipe()
        if(opaque.size>16384)return@runCatching null
        val receipt=Json.parseToJsonElement(opaque.decodeToString()).jsonObject
        if(receipt["kind"]?.jsonPrimitive?.content!="image-ingress" || receipt["version"]?.jsonPrimitive?.intOrNull!=1)return@runCatching null
        a.request().id().toString() to requireNotNull(receipt["createdAt"]?.jsonPrimitive?.longOrNull)
    }.getOrNull()}.toMap()
    return imported+jobs.flatMap{job->(job.results+job.composedResults).map{it to job.createdAt}}.toMap()
}
fun libraryDateMatches(timestamp:Long?,date:String):Boolean {
    val filter=galleryDateFilter(date)?:return true
    return libraryDateMatchesFilter(timestamp,filter)
}
/** Parse once per filter change; invalid input continues to match no assets in the UI. */
fun libraryDatePredicate(date:String):(Long?)->Boolean {
    val parsed=runCatching{galleryDateFilter(date)}
    if(parsed.isFailure)return {false}
    val filter=parsed.getOrNull()?:return {true}
    return {libraryDateMatchesFilter(it,filter)}
}
private fun libraryDateMatchesFilter(timestamp:Long?,filter:NovelAiHistoryDateFilter):Boolean {
    timestamp?:return false
    val (year,month,day)=NovelAiHistoryFilterPolicy.dateParts(timestamp)
    return year==filter.year && when(filter.granularity){
        NovelAiHistoryDateGranularity.YEAR->true
        NovelAiHistoryDateGranularity.MONTH->month==filter.month
        NovelAiHistoryDateGranularity.DAY->month==filter.month&&day==filter.day
    }
}
fun libraryRecordedDay(timestamp:Long?)=timestamp?.let{Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().toString()}.orEmpty()

fun galleryTagAlbums(ids:List<String>,labels:Map<String,AssetLabels>):List<NovelAiHistoryAlbum> {
    val groups=linkedMapOf<String,MutableList<NovelAiHistoryImageItem>>()
    val tagHashes=mutableMapOf<String,String>()
    ids.distinct().forEach{id->
        labels[id]?.tags.orEmpty().ifEmpty{listOf("")}.forEach{tag->
            val image=NovelAiGenerationHistoryImage(path=id)
            val hash=tagHashes.getOrPut(tag){sha256(tag.toByteArray())}
            val item=NovelAiHistoryImageItem(NovelAiGenerationHistoryEntry(id="tag:$hash:$id",images=listOf(image)),image,0)
            groups.getOrPut(tag){mutableListOf()}.add(item)
        }
    }
    return groups.map{(tag,images)->NovelAiHistoryAlbum(images,tag.ifEmpty{"未标记（无标签）"})}
}

/** Visible order first, then selections outside the filter, excluding archived assets. */
fun orderedLibrarySelection(visibleIds:List<String>,allIds:List<String>,activeIds:Set<String>,selected:Set<String>):List<String> {
    if(selected.isEmpty())return emptyList()
    val ordered=linkedSetOf<String>()
    visibleIds.forEach{if(it in selected&&it in activeIds)ordered.add(it)}
    allIds.forEach{if(it in selected&&it in activeIds)ordered.add(it)}
    return ordered.toList()
}
