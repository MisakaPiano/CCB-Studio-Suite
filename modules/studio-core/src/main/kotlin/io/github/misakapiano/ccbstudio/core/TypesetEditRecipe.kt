package io.github.misakapiano.ccbstudio.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.util.Base64
import java.util.UUID

@Serializable data class TypesetEditRecipe(val version:Int=1,val variant:String,val originalSha256:String,val page:TypesetPage,val sourceRecipeBase64:String)
object TypesetEditRecipes {
    // A valid editor document is at most 16 MiB; an original's 1 MiB Recipe
    // adds base64 overhead. Sidecars must round-trip without truncating either.
    const val MAX_BYTES=20*1024*1024
    fun encode(recipe:TypesetEditRecipe):ByteArray = documentJson.encodeToString(recipe).toByteArray().also {
        if(it.size>MAX_BYTES)throw DataProblem("RECIPE_TOO_LARGE","编辑 Recipe 超过 20 MiB，未发布完整导出；请拆分页面对象后重试")
    }
    fun inspect(bytes:ByteArray):TypesetEditRecipe {
        require(bytes.size in 1..MAX_BYTES)
        val raw=documentJson.parseToJsonElement(bytes.decodeToString(throwOnInvalidSequence=true)).jsonObject
        if(raw["version"]?.jsonPrimitive?.intOrNull!=1)throw DataProblem("UNSUPPORTED_VERSION","未知编辑Recipe版本，文件未改动")
        return documentJson.decodeFromJsonElement<TypesetEditRecipe>(raw).also{r->
            require(r.originalSha256.matches(Regex("[0-9a-f]{64}")) && r.variant in setOf("original","mask","all","text"))
            TypesetDocument(pages=listOf(r.page)).validate();require(r.sourceRecipeBase64.length<=4*1024*1024);Base64.getDecoder().decode(r.sourceRecipeBase64)
        }
    }
    fun apply(workspace:StudioWorkspace,project:TypesetProject,pageId:String,recipe:TypesetEditRecipe):TypesetProject=workspace.operation {
        val repository=TypesetRepository(workspace);val document=repository.load(project);val target=document.pages.single{it.id==pageId}
        if(workspace.assets.read(UUID.fromString(target.assetId)).require().sha256()!=recipe.originalSha256)
            throw DataProblem("SOURCE_MISMATCH","Recipe原图SHA-256与当前页不同，未应用；请先选择对应原图")
        // Source request metadata stays attached to the immutable original, never replaced by an edit sidecar.
        repository.update(project.id,project.revision){d->d.copy(pages=d.pages.map{p->if(p.id==pageId)recipe.page.copy(id=p.id,assetId=p.assetId,name=p.name,sourceEntryIndex=p.sourceEntryIndex)else p})}
    }
}
