package io.github.misakapiano.ccbstudio.desktop

import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import java.nio.file.*
import java.util.concurrent.TimeUnit

/** Installed-app background maintenance; no data traversal and no update-state writes. */
internal object StudioResidueCleanup {
    suspend fun run(data:Path,runtime:Path,ordinaryStartup:Boolean)=withContext(Dispatchers.IO) {
        runCatching {
            val app=System.getProperty("jpackage.app-path")?.let{Path.of(it).toAbsolutePath().parent}?:return@withContext
            val script=app.resolve("updater/lib/residue_cleanup.py")
            if(!Files.isRegularFile(script))return@withContext // Older packages have no cleanup feature.
            safePath(script);safePath(runtime)
            val cache=runtime.resolve("updates");val log=cache.resolve("cleanup");safePath(log);Files.createDirectories(log)
            val output=log.resolve("last-result.json");val error=log.resolve("last-process-error.log");safePath(output);safePath(error)
            val args=mutableListOf(app.resolve("updater/pythonw.exe").toString(),"-B",script.toString(),"--package",app.toString(),"--cache",cache.toString(),"--data",data.toString(),"--runtime",runtime.toString())
            if(ordinaryStartup)args+=listOf("--startup-token",newId())
            val process=ProcessBuilder(args).directory(app.toFile()).redirectOutput(output.toFile()).redirectError(error.toFile()).start()
            process.waitFor(45,TimeUnit.SECONDS) // An occupied file is deferred by the helper, never forced.
        }
        Unit
    }
}
