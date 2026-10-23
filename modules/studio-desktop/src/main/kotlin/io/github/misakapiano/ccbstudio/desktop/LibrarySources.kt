package io.github.misakapiano.ccbstudio.desktop

import io.github.misakapiano.ccbstudio.assets.AssetStore
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.serialization.json.*

internal fun librarySourceLabel(source:String)=when(source){"generated"->"生成结果";"processed"->"处理结果";"directory"->"目录收集";"imported"->"文件导入";"other"->"其他";"unknown"->"来源未记录";else->"全部来源"}

/** Only admitted metadata; never inspect or rescan an external file to classify it. */
internal fun librarySources(assets:List<AssetStore.Asset>,jobs:List<JobRecord>):Map<String,String> {
    val generated=jobs.flatMap{it.results}.toSet();val composed=jobs.flatMap{it.composedResults}.toSet()
    return assets.associate{a->
        val id=a.request().id().toString()
        val source=when {
            id in composed->"processed"
            id in generated->"generated"
            a.request().kind()==AssetStore.Kind.DERIVED->"processed"
            else->runCatching {
                if(a.request().recipe().size>16384)return@runCatching "unknown"
                val data=Json.parseToJsonElement(a.request().recipe().decodeToString()).jsonObject
                if(data["kind"]?.jsonPrimitive?.content!="image-ingress"||data["version"]?.jsonPrimitive?.intOrNull!=1)return@runCatching "unknown"
                when(data["source"]?.jsonPrimitive?.content){"managed-folder"->"directory";"file","clipboard","clipboard-raster"->"imported";else->"unknown"}
            }.getOrDefault("unknown")
        }
        id to source
    }
}
