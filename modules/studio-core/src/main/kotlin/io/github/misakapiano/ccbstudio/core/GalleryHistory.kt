package io.github.misakapiano.ccbstudio.core

import com.example.chatbar.domain.image.*
import com.example.chatbar.ui.imageprompt.*
import java.time.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

data class GalleryHistoryProjection(val entries:List<NovelAiGenerationHistoryEntry>,val unavailable:Map<String,String>)

fun galleryAlbums(ids:List<String>,history:GalleryHistoryProjection,type:NovelAiHistoryFoldType?):List<NovelAiHistoryAlbum> {
    val selected=ids.toSet()
    val known=NovelAiHistoryFilterPolicy.filter(history.entries,"",null).filter{it.image.path in selected}
    val covered=known.map{it.image.path}.toSet()
    val grouped=foldHistoryImages(known,type)
    // Identity-only items support the inherited selection policy. They NEVER enter Prompt folding.
    val independent=ids.filter{it !in covered}.map{id->
        val image=NovelAiGenerationHistoryImage(path=id)
        NovelAiHistoryAlbum(listOf(NovelAiHistoryImageItem(NovelAiGenerationHistoryEntry(id="asset:$id",images=listOf(image)),image,0)),"")
    }
    val order=ids.withIndex().associate{it.value to it.index}
    return (grouped+independent).sortedBy{album->album.images.minOf{order.getValue(it.image.path)}}
}

/** Verified receipt projection only. Unreadable receipts never become empty, matchable recipes. */
fun readGalleryHistory(workspace:StudioWorkspace):GalleryHistoryProjection=workspace.operation {
    val assets=SharedAssets.images(workspace).associateBy{it.request().id().toString()}
    val entries=mutableListOf<NovelAiGenerationHistoryEntry>();val unavailable=mutableMapOf<String,String>()
    workspace.state.jobs.forEach { job->
        val ids=(job.results+job.composedResults).filter{it in assets}.distinct()
        if(ids.isNotEmpty())try {
            if(Json.parseToJsonElement(job.recipe).jsonObject["kind"]!=null)return@forEach
            val receipt=documentJson.decodeFromString<GenerationReceipt>(job.recipe)
            receipt.expandedRequest(workspace)
            val images=ids.map{id->
                val index=job.results.indexOf(id).takeIf{it>=0}?:job.results.indexOf(assets.getValue(id).request().parentId()?.toString())
                require(index>=0){"Missing parent for composed history image"}
                NovelAiGenerationHistoryImage(id,receipt.recipe.settings.seed+index)
            }
            entries+=NovelAiGenerationHistoryEntry(job.id,images,receipt.recipe,job.createdAt)
        }catch(e:Exception){ids.forEach{unavailable[it]=e.javaClass.simpleName}}
    }
    GalleryHistoryProjection(entries,unavailable)
}

/** Exact Desktop date input grammar; java.time validates calendar values before filtering. */
fun galleryDateFilter(text:String):NovelAiHistoryDateFilter? {
    val value=text.trim()
    if(value.isEmpty())return null
    return when {
        Regex("\\d{4}").matches(value)->NovelAiHistoryDateFilter(NovelAiHistoryDateGranularity.YEAR,Year.parse(value).value)
        Regex("\\d{4}-\\d{2}").matches(value)->YearMonth.parse(value).let{NovelAiHistoryDateFilter(NovelAiHistoryDateGranularity.MONTH,it.year,it.monthValue)}
        else->LocalDate.parse(value).let{NovelAiHistoryDateFilter(NovelAiHistoryDateGranularity.DAY,it.year,it.monthValue,it.dayOfMonth)}
    }
}

class GalleryFoldPreferences(private val library:LibraryRepository) {
    fun load():Map<Int,NovelAiHistoryFoldPreference> {
        val raw=library.load().preferences["historyFoldPreferences"]?:return emptyMap()
        val values=documentJson.decodeFromString<List<NovelAiHistoryFoldPreference>>(raw)
        require(values.size<=32 && values.all{it.depth in 0..31} && values.distinctBy{it.depth}.size==values.size)
        return values.associateBy{it.depth}
    }
    fun save(value:NovelAiHistoryFoldPreference) {
        require(value.depth in 0..31)
        library.update{state->val values=load()+(value.depth to value);state.copy(preferences=state.preferences+("historyFoldPreferences" to documentJson.encodeToString(values.values.sortedBy{it.depth})))}
    }
}
