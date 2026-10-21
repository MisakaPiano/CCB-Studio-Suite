package io.github.misakapiano.ccbstudio.core

import io.github.misakapiano.ccbstudio.assets.AssetStore
import java.nio.file.*
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.util.UUID
import kotlinx.serialization.json.*

/** Explicit copy migration. Lock and read the old root; publish a fresh root only.
 * Encrypted DPAPI blobs are copied on the same host, never decrypted or exported. */
object StudioFormatMigration {
    fun copyVersionTwo(suppliedSource:Path,suppliedDestination:Path,fault:(String)->Unit={}):Path {
        val source=suppliedSource.toAbsolutePath().normalize();val target=suppliedDestination.toAbsolutePath().normalize()
        safePath(source);safePath(target)
        require(source!=target && !source.startsWith(target) && !target.startsWith(source))
        for(path in listOf(source,target))require(path.none{it.toString().lowercase() in setOf("chatchatbar-desktop","chatchatbardesktop","chatbar","chatchatbar")})
        require(!Files.exists(target,LinkOption.NOFOLLOW_LINKS)){"Migration destination already exists; retained"}
        require(readBounded(source.resolve("preview-root.txt"),1024).decodeToString()=="CCBStudioSuite/WindowsStudio/v2"){"Unsupported source container; retained"}
        val channels=mutableListOf<FileChannel>();val locks=mutableListOf<FileLock>()
        try {
            for(path in listOf(source.resolve(".owner.lock"),source.resolve("assets/.owner.lock"))){
                safePath(path);val channel=FileChannel.open(path,StandardOpenOption.WRITE,LinkOption.NOFOLLOW_LINKS);channels+=channel
                val lock=try{channel.tryLock()}catch(e:java.nio.channels.OverlappingFileLockException){null}
                if(lock==null)throw DataProblem("IN_USE","Migration source is owned; no writer terminated")
                locks+=lock
            }
            fault("SOURCE_LOCKED")
            val allowed=setOf(".owner.lock","preview-root.txt","assets","documents","secrets","exports","backups")
            Files.list(source).use{it.forEach{p->safePath(p);if(p.fileName.toString() !in allowed)throw DataProblem("RECOVERY_REQUIRED","Unknown source entry retained")}}
            require(Files.isDirectory(source.resolve("documents"),LinkOption.NOFOLLOW_LINKS))
            val prior=StateJournal(source.resolve("documents")).state
            if(prior.version!=2)throw DataProblem("UNSUPPORTED_VERSION","Migration accepts only data format 2")
            val files=mutableListOf<Path>();val directories=mutableListOf<Path>()
            for(name in listOf("assets","documents","secrets")){
                val directory=source.resolve(name);safePath(directory)
                if(!Files.exists(directory,LinkOption.NOFOLLOW_LINKS)){require(name=="secrets");continue}
                require(Files.isDirectory(directory,LinkOption.NOFOLLOW_LINKS))
                Files.walk(directory).use{paths->paths.forEach{p->safePath(p)
                    if(Files.isDirectory(p,LinkOption.NOFOLLOW_LINKS)){
                        require(name!="secrets"||p==directory){"Unknown credential directory retained"}
                        directories.add(p);require(directories.size<=100000)
                    }else if(Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS)&&p!=source.resolve("assets/.owner.lock")){
                        if(name=="secrets"){
                            require(p.parent==directory&&p.fileName.toString().endsWith(".dpapi")){"Unknown credential content retained"}
                            require(UUID.fromString(p.fileName.toString().removeSuffix(".dpapi")).toString()+".dpapi"==p.fileName.toString())
                        }
                        files.add(p);require(files.size<=100000){"Migration file budget exceeded; source retained"}
                    }
                }}
            }
            val identities=files.associateWith{p->fingerprintFile(p,if(p.parent==source.resolve("secrets"))65536 else STORED_FILE_LIMIT)}
            Files.createDirectories(target.parent);safePath(target.parent)
            val required=identities.values.sumOf{it.size}+64L*1024*1024
            require(Files.getFileStore(target.parent).usableSpace>=required){"Insufficient migration space; source retained"}
            val stage=target.resolveSibling(".studio-format-migration-${newId()}");Files.createDirectory(stage)
            directories.sortedBy{it.nameCount}.forEach{Files.createDirectories(stage.resolve(source.relativize(it)))}
            files.forEach{p->val copy=stage.resolve(source.relativize(p));Files.createDirectories(copy.parent);safePath(copy)
                Files.newInputStream(p,LinkOption.NOFOLLOW_LINKS).use{input->Files.newOutputStream(copy,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE,LinkOption.NOFOLLOW_LINKS).use{input.copyTo(it,65536)}}
                FileChannel.open(copy,StandardOpenOption.WRITE).use{it.force(true)}
                require(fingerprintFile(copy)==identities.getValue(p)){"Copied file identity mismatch; source and staging retained"}
            }
            fault("MIGRATION_COPIED")
            val journal=StateJournal(stage.resolve("documents"));require(journal.state==prior)
            AssetStore.open(stage.resolve("assets")).use{it.catalog()}
            journal.commit(prior.copy(version=StudioWorkspace.DATA_VERSION))
            writeNewForced(stage.resolve("preview-root.txt"),StudioWorkspace.STUDIO_MARKER)
            fault("MIGRATION_COMMITTED")
            // Validation must not consume crash recovery. Normal opening of the published
            // copy still marks interrupted calls uncertain without resending them.
            StudioWorkspace.openMigrationCandidate(stage).use{w->require(w.state.copy(version=2)==prior);w.catalog();LibraryRepository(w).load();AssetArchive(w).ids()}
            files.forEach{p->require(fingerprintFile(p)==identities.getValue(p)){"Source changed during locked migration; retained"}}
            val receipt=buildJsonObject{
                put("format",1);put("sourceDataVersion",2);put("targetDataVersion",StudioWorkspace.DATA_VERSION)
                put("status","MIGRATED_NOT_APPLICATION_VERIFIED");put("files",files.size)
                put("sourceBytes",identities.values.sumOf{it.size})
                put("sourceInventorySha256",sha256(files.sortedBy{source.relativize(it).toString()}.joinToString("\n"){p->
                    val f=identities.getValue(p);"${source.relativize(p)} ${f.size} ${f.sha256}"
                }.toByteArray()))
                put("credentials","Encrypted OS-bound blobs copied; not decrypted")
                put("exportsAndBackups","Retained in original root; external collection bindings require reconnection")
            }
            Files.createDirectories(stage.resolve("exports/migrations"))
            writeNewForced(stage.resolve("exports/migrations/format-2-to-3.json"),receipt.toString().toByteArray())
            fault("MIGRATION_VALIDATED")
            require(!Files.exists(target,LinkOption.NOFOLLOW_LINKS)){"Migration destination appeared; retained"}
            Files.move(stage,target)
            fault("MIGRATION_PUBLISHED")
            return target
        }finally{locks.asReversed().forEach{it.release()};channels.asReversed().forEach{it.close()}}
    }
}
