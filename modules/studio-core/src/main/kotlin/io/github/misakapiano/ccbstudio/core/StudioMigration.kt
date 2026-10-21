package io.github.misakapiano.ccbstudio.core

import java.nio.file.*

/** Copy migration only. Original root/backup stays untouched; failed staging is retained. */
object StudioMigration {
    fun fromLegacyBackup(backup:Path,destination:Path,fault:(String)->Unit={}):Path {
        val target=destination.toAbsolutePath().normalize()
        val source=backup.toAbsolutePath().normalize()
        safePath(target);safePath(source)
        require(!target.startsWith(source) && !source.startsWith(target))
        require(!Files.exists(target,LinkOption.NOFOLLOW_LINKS)) { "Destination already exists" }
        val staging=target.parent.resolve(".studio-migration-${newId()}")
        StudioRestore.restore(source,staging)
        val journal=StateJournal(staging.resolve("documents"))
        require(journal.state.version==1) { "This migration only accepts Preview v1" }
        fault("MIGRATION_COPIED")
        // Legacy fixture accounts remain explicitly synthetic. No secret is copied or converted.
        journal.commit(journal.state.copy(version=StudioWorkspace.DATA_VERSION,creation=CreationState()))
        val marker=staging.resolve(".marker-current")
        writeNewForced(marker,StudioWorkspace.STUDIO_MARKER)
        Files.move(marker,staging.resolve("preview-root.txt"),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING)
        StudioWorkspace.openStudio(staging).use {it.catalog()}
        fault("MIGRATION_VALIDATED")
        Files.move(staging,target) // Never replace a concurrently created destination.
        return target
    }
}
