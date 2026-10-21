package io.github.misakapiano.ccbstudio.core

import io.github.misakapiano.ccbstudio.assets.AssetStore
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.io.ByteArrayInputStream
import java.nio.file.*
import java.util.UUID

@Serializable data class AssetLabels(val alias:String="",val tags:List<String> = emptyList())
@Serializable data class LibraryFolder(val id:String=newId(),val path:String,val category:String,val recursive:Boolean=false)
@Serializable data class FolderObservation(val assetId:String,val sha256:String,val size:Long,val status:String="PRESENT")
@Serializable data class LibraryState(val version:Int=1,val labels:Map<String,AssetLabels> = emptyMap(),val folders:List<LibraryFolder> = emptyList(),val observations:Map<String,FolderObservation> = emptyMap(),val preferences:Map<String,String> = emptyMap())
@Serializable private data class LibraryRevision(val version:Int=1,val sequence:Long,val previous:String?)
@Serializable data class PromptSection(val id:String=newId(),val label:String,val text:String,val roleId:String?=null,val attributes:Map<String,String> = emptyMap())
/** Snapshot of the read projection; the original image/receipt remain independently retained. */
@Serializable data class PromptImageContext(val imageId:String,val sha256:String,val status:String,
    val parameters:Map<String,String> = emptyMap(),val recipeJson:String?=null,val requestSha256:String?=null)
@Serializable data class PromptAsset(val version:Int=1,val text:String,val origin:String,val createdAt:Long=System.currentTimeMillis(),val sections:List<PromptSection> = emptyList(),val image:PromptImageContext?=null)
data class FolderScanResult(val path:String,val status:String,val assetId:String?=null)

/** Metadata and prompt revisions use the same transactional asset core and backup path.
 * Headers form a checked append chain. A failed/incomplete write never replaces the last revision. */
class LibraryRepository(private val workspace:StudioWorkspace) {
    companion object {const val TAG="CCB_LIBRARY_STATE_V1";const val PROMPT="CCB_PROMPT_ASSET_V1"}
    private fun records()=workspace.catalog().filter{it.request().project().decodeToString()==TAG}.map { a->
        val h=documentJson.decodeFromString<LibraryRevision>(a.request().recipe().decodeToString())
        if(h.version!=1)throw DataProblem("UNSUPPORTED_VERSION","Library metadata version retained")
        a to h
    }.sortedBy{it.second.sequence}.also { records->records.forEachIndexed { i,(a,h)->
        require(h.sequence==i.toLong()+1 && h.previous==records.getOrNull(i-1)?.first?.request()?.id()?.toString()){"Library history has a missing or conflicting revision; retained"}
    }}
    fun load():LibraryState=workspace.operation {
        val latest=records().lastOrNull()?.first?:return@operation LibraryState()
        val state=documentJson.decodeFromString<LibraryState>(workspace.bytes(latest.request().id().toString()).decodeToString(throwOnInvalidSequence=true))
        validate(state);state
    }
    private fun validate(s:LibraryState) {
        if(s.version!=1)throw DataProblem("UNSUPPORTED_VERSION","Library state version retained")
        require(s.labels.size<=20000 && s.folders.size<=100 && s.observations.size<=20000 && s.preferences.size<=100)
        s.labels.forEach{(id,l)->UUID.fromString(id);require(l.alias.length<=200 && l.tags.size<=32 && l.tags.all{it.isNotBlank() && it.length<=64} && l.tags.distinct().size==l.tags.size)}
        require(s.folders.map{it.id}.distinct().size==s.folders.size)
        s.folders.forEach{require(it.category in setOf("images","prompts","imports"));require(it.path.length<=4096)}
        require(s.preferences.all{it.key.length<=100 && it.value.length<=65536})
        s.preferences["collectionBindings"]?.let{raw->val map=documentJson.decodeFromString<Map<String,String>>(raw);require(map.size<=100);map.forEach{(id,path)->UUID.fromString(id);require(path.length<=4096)}}
    }
    fun update(transform:(LibraryState)->LibraryState):LibraryState=workspace.operation {
        val records=records();val old=load();val next=transform(old);validate(next)
        if(next==old)return@operation old
        val bytes=documentJson.encodeToString(next).toByteArray();require(bytes.size<=4*1024*1024){"Library metadata limit reached; existing data retained"}
        val previous=records.lastOrNull()?.first?.request()?.id()
        val header=LibraryRevision(sequence=records.size.toLong()+1,previous=previous?.toString())
        workspace.assets.put(AssetStore.Request(UUID.randomUUID(),"library-state.json",if(previous==null)AssetStore.Kind.ORIGINAL else AssetStore.Kind.DERIVED,previous,documentJson.encodeToString(header).toByteArray(),TAG.toByteArray()),ByteArrayInputStream(bytes))
        next
    }
    fun label(id:String,alias:String,tags:List<String>)=update { s->workspace.assets.read(UUID.fromString(id)).require();s.copy(labels=s.labels+(id to AssetLabels(alias.trim(),tags.map{it.trim()}.filter{it.isNotEmpty()}.distinct()))) }
    fun prompts()=workspace.catalog().filter{it.request().project().decodeToString()==PROMPT}
    fun prompt(id:String):PromptAsset=workspace.operation {
        val a=workspace.assets.read(UUID.fromString(id)).require();require(a.request().project().decodeToString()==PROMPT)
        documentJson.decodeFromString<PromptAsset>(workspace.bytes(id).decodeToString(throwOnInvalidSequence=true)).also{if(it.version!=1)throw DataProblem("UNSUPPORTED_VERSION","Prompt version retained")}
    }
    fun savePrompt(text:String,alias:String,origin:String,previous:String?=null,sections:List<PromptSection> = emptyList(),image:PromptImageContext?=null):String=workspace.operation {
        val management=load() // Reject unreadable management history before creating another asset.
        require(text.isNotBlank() && text.toByteArray().size<=1024*1024 && origin.length<=4096)
        require(alias.trim().length<=200 && sections.size<=256 && sections.map{it.id}.distinct().size==sections.size)
        require(sections.all{it.id.length<=200 && it.label.length<=200 && (it.roleId?.length?:0)<=200 && it.attributes.size<=32 && it.attributes.all{a->a.key.length<=200 && a.value.length<=4096}})
        image?.let{val asset=workspace.assets.read(UUID.fromString(it.imageId)).require();require(asset.sha256()==it.sha256){"Image identity changed"}}
        previous?.let{prompt(it)}
        val id=UUID.randomUUID();val bytes=documentJson.encodeToString(PromptAsset(text=text,origin=origin,sections=sections.filter{it.text.isNotBlank()},image=image)).toByteArray()
        require(bytes.size<=4*1024*1024){"Prompt snapshot limit exceeded; original retained"}
        workspace.assets.put(AssetStore.Request(id,"prompt.txt",if(previous==null)AssetStore.Kind.ORIGINAL else AssetStore.Kind.DERIVED,previous?.let(UUID::fromString),byteArrayOf(),PROMPT.toByteArray()),ByteArrayInputStream(bytes))
        val oldTags=previous?.let{management.labels[it]?.tags}.orEmpty();label(id.toString(),alias,oldTags);id.toString()
    }
    fun addFolder(path:Path,category:String,recursive:Boolean):LibraryState {
        val absolute=path.toAbsolutePath().normalize();safePath(absolute);require(Files.isDirectory(absolute,LinkOption.NOFOLLOW_LINKS))
        require(!absolute.startsWith(workspace.root) && !workspace.root.startsWith(absolute)){"Choose a collection directory separate from the data root"}
        return update{s->require(s.folders.none{Path.of(it.path).equals(absolute) && it.category==category}){"Folder already registered"}
            val folder=LibraryFolder(path=absolute.toString(),category=category,recursive=recursive)
            bind(s.copy(folders=s.folders+folder),folder.id)
        }
    }
    private fun bindings(state:LibraryState):Map<String,String> = state.preferences["collectionBindings"]?.let{documentJson.decodeFromString<Map<String,String>>(it)}?:emptyMap()
    /** Non-secret local registration identity, deliberately excluded from ordinary backup.
     * A fresh restore at the SAME path must not inherit permission to scan old external paths. */
    private fun localBinding(create:Boolean):String? {
        val dir=workspace.root.resolve("exports/.local-bindings");val marker=dir.resolve("collections.id");safePath(marker)
        if(!Files.exists(marker,LinkOption.NOFOLLOW_LINKS)) {
            if(!create)return null
            Files.createDirectories(dir);writeNewForced(marker,newId().toByteArray())
        }
        val id=readBounded(marker,100).decodeToString(throwOnInvalidSequence=true);UUID.fromString(id)
        return workspace.root.toString()+"\n"+id
    }
    private fun bind(state:LibraryState,id:String)=state.copy(preferences=state.preferences+("collectionBindings" to documentJson.encodeToString(bindings(state)+(id to requireNotNull(localBinding(true))))))
    fun automaticFolders(state:LibraryState=load()):List<LibraryFolder> {
        if(state.preferences["collectionsPaused"]=="true")return emptyList()
        val local=localBinding(false)?:return emptyList();return state.folders.filter{bindings(state)[it.id]==local}
    }
    /** An explicit UI action is required after restore/move. Never scan old absolute paths on restore. */
    fun enableHere(id:String)=update{state->val folder=state.folders.single{it.id==id};safePath(Path.of(folder.path));bind(state,id)}
    fun pauseCollections(paused:Boolean)=update{it.copy(preferences=it.preferences+("collectionsPaused" to paused.toString()))}
    fun removeFolder(id:String)=update{it.copy(folders=it.folders.filterNot{f->f.id==id},preferences=it.preferences+("collectionBindings" to documentJson.encodeToString(bindings(it)-id)))}
    fun defaultFolders():List<Path> {
        val base=workspace.root.resolveSibling(workspace.root.fileName.toString()+"-collections");safePath(base)
        return listOf("images","prompts","imports").map{category->Files.createDirectories(base.resolve(category)).also{p->if(load().folders.none{it.path==p.toString()})addFolder(p,category,false)}}
    }
    /** A visible intake copy is secondary to the already-committed original. Never overwrites a file.
     * The pending observation makes crash/partial-copy status explicit and keeps scans idempotent. */
    fun retainIngress(id:String):Path=workspace.operation {
        val asset=workspace.assets.read(UUID.fromString(id)).require()
        val base=workspace.root.resolveSibling(workspace.root.fileName.toString()+"-collections").resolve("imports");safePath(base);Files.createDirectories(base)
        if(load().folders.none{it.category=="imports" && Path.of(it.path)==base})addFolder(base,"imports",false)
        val folder=load().folders.first{it.category=="imports" && Path.of(it.path)==base}
        val safeName=asset.request().name().replace(Regex("[<>:\"/\\\\|?*\\p{Cntrl}]"),"_").takeLast(160).ifBlank{"image.png"}
        val destination=base.resolve("$id-$safeName");safePath(destination)
        val key=folder.id+":"+destination.fileName
        val observation=FolderObservation(id,asset.sha256(),asset.size(),"COPY_PENDING")
        update{it.copy(observations=it.observations+(key to observation))}
        if(Files.exists(destination,LinkOption.NOFOLLOW_LINKS)) {
            require(Files.isRegularFile(destination,LinkOption.NOFOLLOW_LINKS) && sha256(readBounded(destination,32*1024*1024))==asset.sha256()){"Intake destination exists with different content; retained both source and destination"}
        }else writeNewForced(destination,workspace.bytes(id))
        require(Files.size(destination)==asset.size() && sha256(readBounded(destination,32*1024*1024))==asset.sha256())
        update{it.copy(observations=it.observations+(key to observation.copy(status="PRESENT")))}
        destination
    }
    fun scan(folderId:String):List<FolderScanResult> =workspace.operation {
        val state=load();val folder=state.folders.single{it.id==folderId};val root=Path.of(folder.path);safePath(root)
        if(!Files.isDirectory(root,LinkOption.NOFOLLOW_LINKS)){
            update{it.copy(observations=it.observations.mapValues{(key,value)->if(key.startsWith(folder.id+":"))value.copy(status="SOURCE_MISSING")else value})}
            return@operation listOf(FolderScanResult(folder.path,"DIRECTORY_MISSING; retained existing assets"))
        }
        val files=mutableListOf<Path>();val excluded=mutableListOf<FolderScanResult>()
        Files.walkFileTree(root,emptySet(),if(folder.recursive)32 else 1,object:SimpleFileVisitor<Path>() {
            override fun preVisitDirectory(dir:Path,attrs:java.nio.file.attribute.BasicFileAttributes):FileVisitResult {
                return if(runCatching{safePath(dir)}.isSuccess)FileVisitResult.CONTINUE else {excluded+=FolderScanResult(dir.toString(),"SKIPPED_LINK_OR_REPARSE");FileVisitResult.SKIP_SUBTREE}
            }
            override fun visitFile(file:Path,attrs:java.nio.file.attribute.BasicFileAttributes):FileVisitResult {
                if(attrs.isRegularFile && !attrs.isSymbolicLink && !attrs.isOther)files.add(file)
                else excluded+=FolderScanResult(file.toString(),if(attrs.isDirectory)"SKIPPED_DEPTH_LIMIT"else"SKIPPED_LINK_OR_REPARSE")
                return if(files.size>2000)FileVisitResult.TERMINATE else FileVisitResult.CONTINUE
            }
        })
        require(files.size<=2000){"Scan exceeds 2000 files; use narrower directory entries"}
        val observations=state.observations.toMutableMap();val seen=mutableSetOf<String>();val result=excluded
        for(path in files) {
            val extension=path.fileName.toString().substringAfterLast('.',"").lowercase()
            if(extension !in if(folder.category=="prompts")setOf("txt","md","prompt")else setOf("png","jpg","jpeg","webp","gif","bmp"))continue
            val key=folder.id+":"+root.relativize(path);seen+=key
            val bytes=try {
                safePath(path);val before=Files.readAttributes(path,java.nio.file.attribute.BasicFileAttributes::class.java,LinkOption.NOFOLLOW_LINKS)
                val bytes=readBounded(path,if(folder.category=="prompts")1024*1024 else 32*1024*1024)
                val after=Files.readAttributes(path,java.nio.file.attribute.BasicFileAttributes::class.java,LinkOption.NOFOLLOW_LINKS)
                require(before.size()==after.size() && before.lastModifiedTime()==after.lastModifiedTime() && before.fileKey()==after.fileKey()){"File changed during read; retry after writing finishes"}
                if(observations[key]?.sha256!=sha256(bytes)) {
                    if(folder.category=="prompts")require(bytes.decodeToString(throwOnInvalidSequence=true).isNotBlank())else EditorRenderer.decode(bytes).flush()
                }
                bytes
            }catch(e:Exception){observations[key]?.let{observations[key]=it.copy(status="READ_ERROR")};result+=FolderScanResult(path.toString(),"READ_ERROR: "+e.javaClass.simpleName);continue}
            // After validation, a persistence failure aborts the scan instead of retrying later files.
            val hash=sha256(bytes);val old=observations[key]
                if(old?.sha256==hash){observations[key]=old.copy(status="PRESENT");result+=FolderScanResult(path.toString(),"UNCHANGED",old.assetId);continue}
                val id=if(folder.category=="prompts")savePrompt(bytes.decodeToString(throwOnInvalidSequence=true),old?.assetId?.let{state.labels[it]?.alias}?.takeIf{it.isNotBlank()}?:path.fileName.toString(),"folder:${folder.id}:${root.relativize(path)}",old?.assetId)
                    else workspace.importImage(path.fileName.toString(),bytes,kotlinx.serialization.json.buildJsonObject{
                        put("kind",kotlinx.serialization.json.JsonPrimitive("image-ingress"));put("version",kotlinx.serialization.json.JsonPrimitive(1));put("createdAt",kotlinx.serialization.json.JsonPrimitive(System.currentTimeMillis()))
                        put("source",kotlinx.serialization.json.JsonPrimitive("managed-folder"));put("folderId",kotlinx.serialization.json.JsonPrimitive(folder.id));put("relativePath",kotlinx.serialization.json.JsonPrimitive(root.relativize(path).toString()));put("previousAssetId",kotlinx.serialization.json.JsonPrimitive(old?.assetId.orEmpty()))
                    }.toString())
                if(folder.category!="prompts" && old!=null)state.labels[old.assetId]?.let{label(id,it.alias,it.tags)}
                observations[key]=FolderObservation(id,hash,bytes.size.toLong());result+=FolderScanResult(path.toString(),if(old==null)"IMPORTED"else"NEW_VERSION",id)
        }
        observations.keys.filter{it.startsWith(folder.id+":") && it !in seen}.forEach{key->
            val old=observations.getValue(key);observations[key]=old.copy(status="SOURCE_MISSING")
            result+=FolderScanResult(root.resolve(key.removePrefix(folder.id+":")).toString(),"SOURCE_MISSING; retained existing asset",old.assetId)
        }
        update{it.copy(observations=observations)};result
    }
}
