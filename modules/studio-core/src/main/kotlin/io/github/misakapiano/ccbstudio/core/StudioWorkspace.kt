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
    val assets:AssetStore,private val journal:StateJournal,val studioMode:Boolean=false):AutoCloseable {
    private var closed=false
    private var poisoned=false
    private var activeTasks=0
    @Synchronized fun beginTask():AutoCloseable {
        checkOpen();check(activeTasks==0) { "Another creation task is active" };activeTasks++
        var released=false
        return AutoCloseable { synchronized(this) {if(!released){released=true;activeTasks--}} }
    }
    val state:StudioState @Synchronized get() {checkOpen();return journal.state}
    val creationRevision:String? @Synchronized get() {checkOpen();return journal.creationRevision}
    val committedStateBody:String @Synchronized get() {checkOpen();return requireNotNull(journal.committedBody)}
    @Synchronized fun committedStateBodyForVersion(version:Int):String {checkOpen();return journal.committedBodyForVersion(version)}
    @Synchronized fun <T> operation(body:()->T):T {checkOpen();return body()}
    private fun checkOpen() {check(!closed && !poisoned) { "Workspace closed or requires recovery; reopen after inspection" }}
    @Synchronized fun save(next:StudioState) {
        checkOpen()
        require(next.version == if(studioMode) DATA_VERSION else 1) { "Workspace/document version mismatch; migration required" }
        journal.requireCapacity(next)
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
    @Synchronized fun importDerived(name:String,bytes:ByteArray,parentId:String,recipe:String=""):String = operation {
        EditorRenderer.decode(bytes).flush()
        val parent=UUID.fromString(parentId);assets.read(parent).require()
        val id=UUID.randomUUID()
        assets.put(AssetStore.Request(id,name.take(512),AssetStore.Kind.DERIVED,parent,recipe.toByteArray(),byteArrayOf()),ByteArrayInputStream(bytes))
        assets.read(id).require();id.toString()
    }
    @Synchronized fun requestPayload(bytes:ByteArray,parentId:String?):String = operation {
        val parent=parentId?.let(UUID::fromString);parent?.let{assets.read(it).require()}
        val id=UUID.randomUUID()
        assets.put(AssetStore.Request(id,"request-payload.bin",if(parent==null)AssetStore.Kind.ORIGINAL else AssetStore.Kind.DERIVED,parent,
            byteArrayOf(),"CCB_REQUEST_PAYLOAD_V1".toByteArray()),ByteArrayInputStream(bytes))
        assets.read(id).require();id.toString()
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
            if(mode=="ORIGINAL") {
                // Keep this older entry's naming/receipt rules; only replace whole-file buffering.
                val pending=dir.resolve(".%03d-%s.original.pending".format(i+1,page.assetId))
                val copied=copyVerifiedOriginal(this,page.assetId,pending)
                assets.read(UUID.fromString(page.assetId)).require() // Preserve the existing post-transfer verification.
                val extension=if(copied.prefix[0].toInt() and 255==255)"jpg" else "png"
                val name="%03d-%s.%s".format(i+1,page.assetId,extension)
                Files.move(pending,dir.resolve(name),StandardCopyOption.ATOMIC_MOVE)
                receipts+="$name ${copied.size} ${requireNotNull(copied.sha256)}"
                return@forEachIndexed
            }
            val original=bytes(page.assetId)
            val record=assets.read(UUID.fromString(page.assetId)).require()
            val output=run {
                val rendered=EditorRenderer.render(original,page.layers)
                try {EditorRenderer.encode(rendered,mode=="PRIVACY",documentJson.encodeToString(page)+"\n"+record.request().recipe().decodeToString())} finally {rendered.image.flush()}
            }
            val name="%03d-%s.png".format(i+1,page.assetId)
            val pending=dir.resolve(".$name.pending");writeNewForced(pending,output)
            Files.move(pending,dir.resolve(name),StandardCopyOption.ATOMIC_MOVE)
            assets.put(AssetStore.Request(UUID.randomUUID(),name,AssetStore.Kind.DERIVED,UUID.fromString(page.assetId),if(mode=="PRIVACY")byteArrayOf() else record.request().recipe(),byteArrayOf()),ByteArrayInputStream(output))
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
        val manifest=Files.walk(dest).use {stream->stream.filter {Files.isRegularFile(it,NOFOLLOW_LINKS)}.sorted().map {p->val f=fingerprintFile(p);"${dest.relativize(p)} ${f.size} ${f.sha256}"}.toList()}
        writeNewForced(dest.resolve("COMPLETE.txt"),("Secrets excluded; references remain OS-bound.\n"+manifest.joinToString("\n")).toByteArray())
        dest
    }
    @Synchronized override fun close() {
        if(closed)return
        if(activeTasks>0) throw DataProblem("DRAIN_REQUIRED","Cancel and join active creation tasks before closing; ownership retained")
        if(!assets.close(Duration.ofSeconds(30))) throw DataProblem("DRAIN_TIMEOUT","Ownership retained")
        lock.release();owner.close();closed=true
    }
    companion object {
        private val MARKER="CCBStudioSuite/WindowsPreview/SYNTHETIC_ONLY/v1".toByteArray()
        const val DATA_VERSION=3
        internal val STUDIO_MARKER="CCBStudioSuite/WindowsStudio/v3".toByteArray()
        fun open(supplied:Path,fault:(String)->Unit={}):StudioWorkspace = openRoot(supplied,false,fault)
        fun openStudio(supplied:Path,fault:(String)->Unit={}):StudioWorkspace = openRoot(supplied,true,fault)
        internal fun openMigrationCandidate(supplied:Path):StudioWorkspace = openRoot(supplied,true,{},false)
        private fun openRoot(supplied:Path,studio:Boolean,fault:(String)->Unit,recoverInterrupted:Boolean=true):StudioWorkspace {
            val root=supplied.toAbsolutePath().normalize();safePath(root)
            require(root.none {it.toString().lowercase() in setOf("chatchatbar-desktop","chatchatbardesktop","chatbar","chatchatbar")})
            Files.createDirectories(root)
            val marker=root.resolve("preview-root.txt")
            if(!Files.exists(marker,NOFOLLOW_LINKS) && Files.list(root).use {it.anyMatch {p->p.fileName.toString()!=".owner.lock"}}) throw DataProblem("FOREIGN_ROOT","Unrecognized nonempty root retained")
            val expected=if(studio) STUDIO_MARKER else MARKER
            // Unknown formats are read-only even when their owner marker is absent.
            // Repeat under the lock below so this early check does not authorize writes.
            if(Files.exists(marker,NOFOLLOW_LINKS) && !readBounded(marker,1024).contentEquals(expected))
                throw DataProblem("UNSUPPORTED_VERSION","Root version requires explicit copy migration; retained")
            val c=FileChannel.open(root.resolve(".owner.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE,NOFOLLOW_LINKS)
            val lock=try {c.tryLock() ?: throw DataProblem("IN_USE","Preview root already owned")}
                catch(e:java.nio.channels.OverlappingFileLockException) {c.close();throw DataProblem("IN_USE","Studio root already owned")}
                catch(e:Exception) {c.close();throw e}
            var assets:AssetStore?=null
            try {
                if(!Files.exists(marker,NOFOLLOW_LINKS)) writeNewForced(marker,expected)
                if(!readBounded(marker,1024).contentEquals(expected)) throw DataProblem("UNSUPPORTED_VERSION","Root version requires explicit copy migration; retained")
                val allowed=setOf(".owner.lock","preview-root.txt","assets","documents","secrets","exports","backups")
                Files.list(root).use {it.forEach {p->safePath(p);if(p.fileName.toString() !in allowed)throw DataProblem("RECOVERY_REQUIRED","Unknown root entry retained")}}
                val journal=StateJournal(root.resolve("documents"),fault)
                if(studio && journal.state.version == 1) {
                    // Only a new empty current root may initialize. Older roots need explicit copy migration.
                    if(Files.list(root.resolve("documents")).use {it.findAny().isPresent}) throw DataProblem("UNSUPPORTED_VERSION","Legacy documents require copy migration")
                    journal.commit(StudioState(version=DATA_VERSION,creation=CreationState()))
                }
                if(journal.state.version != if(studio) DATA_VERSION else 1) throw DataProblem("UNSUPPORTED_VERSION","Root/document versions differ; retained")
                assets=AssetStore.open(root.resolve("assets"))
                val workspace=StudioWorkspace(root,c,lock,assets,journal,studio)
                // A dispatched job after process death is uncertain, never automatically resent.
                if(recoverInterrupted && journal.state.jobs.any {it.state in setOf("QUEUED","RUNNING")}) workspace.save(journal.state.copy(jobs=journal.state.jobs.map {if(it.state in setOf("QUEUED","RUNNING"))it.copy(state="UNCERTAIN",detail="Interrupted; no automatic retry")else it}))
                val creation=journal.state.creation
                if(recoverInterrupted && creation!=null && creation.conversations.any {c->c.turns.any {it.status==com.example.chatbar.domain.image.NovelAiDesignTurnStatus.PENDING}}) {
                    workspace.save(journal.state.copy(creation=creation.copy(conversations=creation.conversations.map {c->c.copy(turns=c.turns.map {t->
                        if(t.status==com.example.chatbar.domain.image.NovelAiDesignTurnStatus.PENDING) t.copy(status=com.example.chatbar.domain.image.NovelAiDesignTurnStatus.FAILED,error="进程中断，结果未知；未自动重发") else t
                    })})))
                }
                return workspace
            } catch(e:Exception) {assets?.close();lock.release();c.close();throw e}
        }
    }
}
