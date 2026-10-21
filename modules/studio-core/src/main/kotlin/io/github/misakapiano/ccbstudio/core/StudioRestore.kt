package io.github.misakapiano.ccbstudio.core

import io.github.misakapiano.ccbstudio.assets.AssetBackup
import io.github.misakapiano.ccbstudio.assets.AssetStore
import java.nio.file.*
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.util.UUID

/** Restore only into a previously nonexistent root. Failed staging is retained for inspection. */
object StudioRestore {
    fun restore(suppliedBackup:Path,suppliedDestination:Path,fault:(String)->Unit={}):Path {
        val backup=suppliedBackup.toAbsolutePath().normalize()
        val destination=suppliedDestination.toAbsolutePath().normalize()
        safePath(backup);safePath(destination)
        require(!destination.startsWith(backup) && !backup.startsWith(destination))
        require(destination.none {it.toString().lowercase() in setOf("chatchatbar-desktop","chatchatbardesktop","chatbar","chatchatbar")})
        if(Files.exists(destination,NOFOLLOW_LINKS))throw DataProblem("CONFLICT","Restore destination already exists; retained")
        val manifest=readBounded(backup.resolve("COMPLETE.txt"),4*1024*1024).decodeToString(throwOnInvalidSequence=true).lines()
        if(manifest.firstOrNull()!="Secrets excluded; references remain OS-bound.")throw DataProblem("UNSUPPORTED_VERSION","Unknown backup format retained")
        val expected=linkedMapOf<String,Pair<Long,String>>()
        manifest.drop(1).forEach {line->
            val m=Regex("(.+) ([0-9]+) ([0-9a-f]{64})").matchEntire(line)?:throw DataProblem("CORRUPT","Invalid snapshot entry")
            val name=m.groupValues[1].replace('\\','/')
            if(name.split('/').any {it.isEmpty() || it in setOf(".","..")} || ':' in name || !(name.startsWith("assets/") || name.matches(Regex("documents/[0-9]{12}-[0-9a-f-]{36}\\.json"))))throw DataProblem("CORRUPT","Unsafe snapshot entry retained")
            if(expected.put(name,m.groupValues[2].toLong() to m.groupValues[3])!=null)throw DataProblem("CORRUPT","Duplicate snapshot entry")
        }
        if("assets/backup-manifest.rec" !in expected)throw DataProblem("CORRUPT","Asset completion record missing")
        val actual=Files.walk(backup).use {s->s.peek {safePath(it)}.filter {Files.isRegularFile(it,NOFOLLOW_LINKS)}.map {backup.relativize(it).toString().replace('\\','/')}.toList().toSet()}
        if(actual!=expected.keys+"COMPLETE.txt")throw DataProblem("CORRUPT","Missing or unknown backup content retained")
        Files.walk(backup).use {s->s.filter {Files.isDirectory(it,NOFOLLOW_LINKS)}.forEach {p->
            val relative=backup.relativize(p).toString().replace('\\','/')
            if(relative.isNotEmpty() && relative !in setOf("assets","documents") && expected.keys.none {it.startsWith("$relative/")})throw DataProblem("CORRUPT","Unknown snapshot directory retained")
        }}
        fun verified(name:String) {
            val value=expected.getValue(name);val max=if(name.startsWith("documents/"))4L*1024*1024 else STORED_FILE_LIMIT
            if(value.first !in 0..max)throw DataProblem("CORRUPT","Backup file exceeds stored-file budget; retained")
            val actual=fingerprintFile(backup.resolve(name),max)
            if(actual.size!=value.first || actual.sha256!=value.second)throw DataProblem("CORRUPT","Backup size/hash mismatch; retained")
        }
        expected.keys.forEach {verified(it)} // Validate all bytes before creating destination or staging.
        Files.createDirectories(destination.parent);safePath(destination.parent)
        val staging=destination.parent.resolve(".studio-restore-${newId()}");Files.createDirectory(staging)
        fault("RESTORE_STAGING")
        AssetBackup.restore(backup.resolve("assets"),staging.resolve("assets"))
        Files.createDirectory(staging.resolve("documents"))
        expected.keys.filter {it.startsWith("documents/")}.forEach {name->
            val bytes=readBounded(backup.resolve(name),4*1024*1024);val value=expected.getValue(name)
            if(bytes.size.toLong()!=value.first || sha256(bytes)!=value.second)throw DataProblem("CORRUPT","Backup document changed; retained")
            writeNewForced(staging.resolve(name),bytes);fault("RESTORE_DOCUMENT")
        }
        val state=StateJournal(staging.resolve("documents")).state
        AssetStore.open(staging.resolve("assets")).use {store->
            store.catalog()
            val ids=state.projects.flatMap {p->(listOf(p.current)+p.undo+p.redo).flatMap {it.pages.map {page->page.assetId}}}+state.jobs.flatMap {it.results}
            ids.distinct().forEach {store.read(UUID.fromString(it)).require()}
        }
        // Secret refs deliberately remain unresolved; no credentials are copied by ordinary backup.
        writeNewForced(staging.resolve("preview-root.txt"),if(state.version==2) StudioWorkspace.STUDIO_MARKER else "CCBStudioSuite/WindowsPreview/SYNTHETIC_ONLY/v1".toByteArray())
        fault("RESTORE_VALIDATED")
        if(Files.exists(destination,NOFOLLOW_LINKS))throw DataProblem("CONFLICT","Restore destination appeared; retained")
        Files.move(staging,destination) // No REPLACE_EXISTING: never overwrite a concurrently created target.
        return destination
    }
}
