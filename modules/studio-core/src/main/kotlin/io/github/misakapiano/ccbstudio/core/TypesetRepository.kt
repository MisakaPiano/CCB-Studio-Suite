package io.github.misakapiano.ccbstudio.core

import io.github.misakapiano.ccbstudio.assets.AssetStore
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.UUID

/** Immutable snapshot assets first, journal reference last. Failed writes retain orphans; no originals are deleted. */
class TypesetRepository(private val workspace:StudioWorkspace) {
    fun importDocument(name:String,document:TypesetDocument,sourceExchangeId:String,current:Int):TypesetProject=workspace.operation {
        require(name.isNotBlank() && name.length<=100);document.validate();workspace.assets.read(UUID.fromString(sourceExchangeId)).require()
        val page=document.pages.getOrNull(current)?.id?:document.pages.firstOrNull()?.id
        val project=TypesetProject(name=name,current=write(document),sourceExchangeId=sourceExchangeId,currentPageId=page,selectedPageIds=listOfNotNull(page))
        workspace.save(workspace.state.copy(typesetProjects=workspace.state.typesetProjects+project));project
    }
    fun create(name:String,ids:List<String>):TypesetProject=workspace.operation {
        require(name.length in 1..100 && ids.isNotEmpty() && ids.size<=1000)
        val document=TypesetDocument(pages=ids.map{id->val a=workspace.assets.read(UUID.fromString(id)).require();
            TypesetPage(assetId=id,name=a.request().name())})
        val project=TypesetProject(name=name,current=write(document))
        workspace.save(workspace.state.copy(typesetProjects=workspace.state.typesetProjects+project));project
    }
    fun load(project:TypesetProject):TypesetDocument=workspace.operation {read(project.current)}
    fun view(id:String,revision:Long,currentPageId:String?,selected:List<String>):TypesetProject=workspace.operation {
        val old=workspace.state.typesetProjects.first{it.id==id};require(old.revision==revision){"Stale editor revision"}
        val ids=read(old.current).pages.map{it.id}.toSet();require(currentPageId==null || currentPageId in ids);require(selected.all{it in ids})
        if(old.currentPageId==currentPageId && old.selectedPageIds==selected)return@operation old
        old.copy(revision=old.revision+1,currentPageId=currentPageId,selectedPageIds=selected).also(::commit)
    }
    fun rename(id:String,revision:Long,name:String):TypesetProject=workspace.operation {
        require(name.isNotBlank() && name.length<=100)
        val old=workspace.state.typesetProjects.first{it.id==id};require(old.revision==revision){"Stale editor revision"}
        old.copy(name=name,revision=old.revision+1).also(::commit)
    }
    fun copy(id:String,revision:Long,name:String):TypesetProject=workspace.operation {
        require(name.isNotBlank() && name.length<=100)
        val old=workspace.state.typesetProjects.first{it.id==id};require(old.revision==revision){"Stale editor revision"}
        read(old.current)
        val next=TypesetProject(name=name,current=old.current,currentPageId=old.currentPageId,selectedPageIds=old.selectedPageIds,sourceExchangeId=old.sourceExchangeId)
        workspace.save(workspace.state.copy(typesetProjects=workspace.state.typesetProjects+next));next
    }
    fun remove(id:String,revision:Long)=workspace.operation {
        val old=workspace.state.typesetProjects.first{it.id==id};require(old.revision==revision){"Stale editor revision"}
        // Remove only the named project entry. Originals and immutable snapshots remain retained.
        workspace.save(workspace.state.copy(typesetProjects=workspace.state.typesetProjects.filterNot{it.id==id}))
    }
    fun update(id:String,revision:Long,change:(TypesetDocument)->TypesetDocument):TypesetProject=workspace.operation {
        val old=workspace.state.typesetProjects.first{it.id==id};require(old.revision==revision){"Stale editor revision; reload before editing"}
        val before=read(old.current);val document=change(before);document.validate()
        if(document==before)return@operation old
        val ref=write(document)
        val next=old.copy(revision=old.revision+1,current=ref,undo=(old.undo+old.current).takeLast(30),redo=emptyList())
        commit(next);next
    }
    fun history(id:String,revision:Long,redo:Boolean):TypesetProject=workspace.operation {
        val old=workspace.state.typesetProjects.first{it.id==id};require(old.revision==revision){"Stale editor revision"}
        val refs=if(redo)old.redo else old.undo
        if(refs.isEmpty())return@operation old
        read(refs.last()) // A corrupt or missing history reference must not become the current document.
        val next=if(redo)old.copy(revision=old.revision+1,current=refs.last(),redo=refs.dropLast(1),undo=(old.undo+old.current).takeLast(30))
            else old.copy(revision=old.revision+1,current=refs.last(),undo=refs.dropLast(1),redo=(old.redo+old.current).takeLast(30))
        commit(next);next
    }
    private fun commit(project:TypesetProject) {workspace.save(workspace.state.copy(typesetProjects=workspace.state.typesetProjects.map{if(it.id==project.id)project else it}))}
    private fun write(document:TypesetDocument):TypesetReference {
        document.validate();document.pages.forEach {workspace.assets.read(UUID.fromString(it.assetId)).require()}
        val bytes=documentJson.encodeToString(document).toByteArray();require(bytes.size<=16*1024*1024){"Editor document exceeds 16 MiB; split the book explicitly"}
        val ids=(bytes.indices step CHUNK).map{start->
            val id=UUID.randomUUID();workspace.assets.put(AssetStore.Request(id,"typeset-document.json.part",AssetStore.Kind.ORIGINAL,null,
                byteArrayOf(),TAG.toByteArray()),ByteArrayInputStream(bytes.copyOfRange(start,minOf(start+CHUNK,bytes.size))))
            workspace.assets.read(id).require();id.toString()
        }
        return TypesetReference(ids,bytes.size,sha256(bytes))
    }
    private fun read(reference:TypesetReference):TypesetDocument {
        reference.validate()
        val bytes=ByteArrayOutputStream().use{out->reference.chunks.forEach{id->
            val asset=workspace.assets.read(UUID.fromString(id)).require()
            if(!asset.request().project().contentEquals(TAG.toByteArray()))throw DataProblem("CORRUPT","Editor payload kind mismatch; retained")
            val part=workspace.bytes(id)
            if(part.size>CHUNK || out.size().toLong()+part.size>reference.bytes)throw DataProblem("CORRUPT","Editor payload size mismatch; retained")
            out.write(part)
        };out.toByteArray()}
        if(bytes.size!=reference.bytes || sha256(bytes)!=reference.sha256)throw DataProblem("CORRUPT","Editor document checksum mismatch; retained")
        return try{
            val json=documentJson.parseToJsonElement(bytes.decodeToString(throwOnInvalidSequence=true)).jsonObject
            if(json["version"]?.jsonPrimitive?.intOrNull!=1)throw DataProblem("UNSUPPORTED_VERSION","Editor document version retained")
            documentJson.decodeFromJsonElement<TypesetDocument>(json).also{it.validate()}}
        catch(e:DataProblem){throw e}catch(e:Exception){throw DataProblem("CORRUPT","Editor document is invalid; retained",e)}
    }
    companion object {const val TAG="CCB_EDITOR_DOCUMENT_V1";private const val CHUNK=4*1024*1024}
}
