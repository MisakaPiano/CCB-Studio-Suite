package io.github.misakapiano.ccbstudio.core

import io.github.misakapiano.ccbstudio.assets.*
import kotlinx.serialization.encodeToString
import java.nio.file.*
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Duration
import java.util.UUID

/** One admitted serial workspace owns assets, document history and credential references. */
class StudioWorkspace private constructor(val root:Path,private val owner:FileChannel,private val lock:FileLock,
    val assets:AssetStore,private val journal:StateJournal):AutoCloseable {
    private var closed=false
    private var poisoned=false
    val state:StudioState @Synchronized get() {checkOpen();return journal.state}
    @Synchronized fun <T> operation(body:()->T):T {checkOpen();return body()}
    private fun checkOpen() {check(!closed && !poisoned) { "Workspace closed or requires recovery; reopen after inspection" }}
    @Synchronized fun save(next:StudioState) {
        checkOpen()
        try {journal.commit(next)} catch(e:Exception) {poisoned=true;throw e}
    }
    @Synchronized fun importImage(name:String,bytes:ByteArray,recipe:String=""):String = operation {
        EditorRenderer.decode(bytes).flush()
        val id=UUID.randomUUID()
        assets.put(AssetStore.Request(id,name.take(512),AssetStore.Kind.ORIGINAL,null,recipe.toByteArray(),byteArrayOf()),ByteArrayInputStream(bytes))
        assets.read(id).require()
        id.toString()
    }
    @Synchronized fun bytes(id:String):ByteArray = operation {
        ByteArrayOutputStream().also {assets.export(UUID.fromString(id),it)}.toByteArray()
    }
    @Synchronized fun catalog():List<AssetStore.Asset> = operation {assets.catalog()}
    @Synchronized fun newProject(name:String,ids:List<String>):String = operation {
        require(ids.isNotEmpty());ids.forEach {assets.read(UUID.fromString(it)).require()}
        val project=EditProject(name=name,current=EditSnapshot(ids.distinct().map {ProjectPage(it)}))
        save(state.copy(projects=state.projects+project));project.id
    }
    @Synchronized fun edit(id:String,change:(EditProject)->EditProject) = operation {
        val old=state.projects.first {it.id==id};val next=change(old)
        require(next.id==id)
        next.current.pages.forEach {assets.read(UUID.fromString(it.assetId)).require()}
        save(state.copy(projects=state.projects.map {if(it.id==id)next else it}))
    }
    @Synchronized fun applyLayers(projectId:String,ids:Set<String>,layers:List<Layer>) = edit(projectId) { p ->
        require(ids.isNotEmpty() && ids.all {id->p.current.pages.any {it.assetId==id}})
        p.change(p.current.copy(pages=p.current.pages.map {if(it.assetId in ids) it.copy(layers=it.layers+layers.map {l->l.copy(id=newId())}) else it}))
    }
    @Synchronized fun export(projectId:String,ids:List<String>,mode:String):Path = operation {
        require(mode in setOf("PRIVACY","NORMAL","ORIGINAL"))
        val project=state.projects.first {it.id==projectId}
        val pages=ids.map {id->project.current.pages.first {it.assetId==id}}
        require(pages.isNotEmpty())
        val dir=root.resolve("exports").resolve(newId());Files.createDirectories(dir)
        val receipts=mutableListOf<String>()
        pages.forEachIndexed {i,page ->
            val original=bytes(page.assetId)
            val record=assets.read(UUID.fromString(page.assetId)).require()
            val output=if(mode=="ORIGINAL") original else {
                val rendered=EditorRenderer.render(original,page.layers)
                try {EditorRenderer.encode(rendered,mode=="PRIVACY",documentJson.encodeToString(page)+"\n"+record.request().recipe().decodeToString())} finally {rendered.image.flush()}
            }
            val extension=if(mode=="ORIGINAL" && original[0].toInt() and 255==255)"jpg" else "png"
            val name="%03d-%s.%s".format(i+1,page.assetId,extension)
            val pending=dir.resolve(".$name.pending");writeNewForced(pending,output)
            Files.move(pending,dir.resolve(name),StandardCopyOption.ATOMIC_MOVE)
            if(mode!="ORIGINAL") assets.put(AssetStore.Request(UUID.randomUUID(),name,AssetStore.Kind.DERIVED,UUID.fromString(page.assetId),if(mode=="PRIVACY")byteArrayOf() else record.request().recipe(),byteArrayOf()),ByteArrayInputStream(output))
            receipts+="$name ${output.size} ${sha256(output)}"
        }
        writeNewForced(dir.resolve("COMPLETE.txt"),("CCB Studio Suite $mode\n"+receipts.joinToString("\n")).toByteArray())
        dir
    }
    @Synchronized fun backup():Path = operation {
        val dest=root.resolve("backups").resolve(newId());Files.createDirectories(dest)
        AssetBackup.create(assets,dest.resolve("assets"))
        val documents=dest.resolve("documents");Files.createDirectory(documents)
        Files.list(root.resolve("documents")).use {list ->list.forEach {p->writeNewForced(documents.resolve(p.fileName),readBounded(p,4*1024*1024))}}
        StateJournal(documents) // Validate copied chain before marking complete.
        val manifest=Files.walk(dest).use {stream->stream.filter {Files.isRegularFile(it,NOFOLLOW_LINKS)}.sorted().map {p->"${dest.relativize(p)} ${Files.size(p)} ${sha256(Files.readAllBytes(p))}"}.toList()}
        writeNewForced(dest.resolve("COMPLETE.txt"),("Secrets excluded; references remain OS-bound.\n"+manifest.joinToString("\n")).toByteArray())
        dest
    }
    @Synchronized override fun close() {
        if(closed)return
        if(!assets.close(Duration.ofSeconds(30))) throw DataProblem("DRAIN_TIMEOUT","Ownership retained")
        lock.release();owner.close();closed=true
    }
    companion object {
        private val MARKER="CCBStudioSuite/WindowsPreview/SYNTHETIC_ONLY/v1".toByteArray()
        fun open(supplied:Path,fault:(String)->Unit={}):StudioWorkspace {
            val root=supplied.toAbsolutePath().normalize();safePath(root)
            require(root.none {it.toString().lowercase() in setOf("chatchatbar-desktop","chatchatbardesktop","chatbar","chatchatbar")})
            Files.createDirectories(root)
            val marker=root.resolve("preview-root.txt")
            if(!Files.exists(marker,NOFOLLOW_LINKS) && Files.list(root).use {it.anyMatch {p->p.fileName.toString()!=".owner.lock"}}) throw DataProblem("FOREIGN_ROOT","Unrecognized nonempty root retained")
            val c=FileChannel.open(root.resolve(".owner.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE,NOFOLLOW_LINKS)
            val lock=try {c.tryLock() ?: throw DataProblem("IN_USE","Preview root already owned")} catch(e:Exception) {c.close();throw e}
            var assets:AssetStore?=null
            try {
                if(!Files.exists(marker,NOFOLLOW_LINKS)) writeNewForced(marker,MARKER)
                if(!readBounded(marker,1024).contentEquals(MARKER)) throw DataProblem("UNSUPPORTED_VERSION","Unknown Preview root identity retained")
                val allowed=setOf(".owner.lock","preview-root.txt","assets","documents","secrets","exports","backups")
                Files.list(root).use {it.forEach {p->safePath(p);if(p.fileName.toString() !in allowed)throw DataProblem("RECOVERY_REQUIRED","Unknown root entry retained")}}
                val journal=StateJournal(root.resolve("documents"),fault)
                assets=AssetStore.open(root.resolve("assets"))
                val workspace=StudioWorkspace(root,c,lock,assets,journal)
                // A dispatched job after process death is uncertain, never automatically resent.
                if(journal.state.jobs.any {it.state in setOf("QUEUED","RUNNING")}) workspace.save(journal.state.copy(jobs=journal.state.jobs.map {if(it.state in setOf("QUEUED","RUNNING"))it.copy(state="UNCERTAIN",detail="Interrupted; no automatic retry")else it}))
                return workspace
            } catch(e:Exception) {assets?.close();lock.release();c.close();throw e}
        }
    }
}
