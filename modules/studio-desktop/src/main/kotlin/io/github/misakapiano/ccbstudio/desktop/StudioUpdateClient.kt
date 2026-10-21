package io.github.misakapiano.ccbstudio.desktop

import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import java.nio.file.*
import java.time.Duration
import java.util.UUID

/** Local process adapter. The bundled python-tuf runtime owns all signature verification. */
internal class StudioUpdateClient(private val installed:Path,private val cache:Path) {
    suspend fun check(download:Boolean=false,onProgress:suspend(JsonObject)->Unit={}):JsonObject {
      val caller=currentCoroutineContext().minusKey(Job)
      return withContext(Dispatchers.IO) {
        val python=installed.resolve("updater/python.exe")
        val script=installed.resolve("updater/lib/studio_update.py")
        require(listOf(python,script,installed.resolve("update-config.json"),installed.resolve("update-root.json")).all{Files.isRegularFile(it)}){"此构建尚未配置可信更新运行时"}
        require(!cache.toAbsolutePath().normalize().startsWith(installed.toAbsolutePath().normalize())){"更新缓存必须独立于安装目录"}
        Files.createDirectories(cache.resolve("operations"))
        val operation=cache.resolve("operations").resolve(UUID.randomUUID().toString());Files.createDirectory(operation)
        val output=operation.resolve("result.json");val error=operation.resolve("process-error.log")
        val progress=operation.resolve("progress.json")
        val args=mutableListOf(python.toString(),"-B",script.toString(),"--package",installed.toString(),"--cache",cache.toString(),"--progress",progress.toString())
        if(download)args.add("--download")
        val process=ProcessBuilder(args).directory(installed.toFile()).redirectOutput(output.toFile()).redirectError(error.toFile()).start()
        try {
            withTimeout(Duration.ofMinutes(if(download)30 else 2).toMillis()) {
                while(process.isAlive) {
                    require((if(Files.exists(output))Files.size(output) else 0)<128*1024 && (if(Files.exists(error))Files.size(error) else 0)<128*1024){"更新进程输出超过限制"}
                    if(Files.exists(progress)) {
                        require(Files.size(progress)<16*1024){"更新进度超过限制"}
                        val value=Json.parseToJsonElement(Files.readString(progress)).jsonObject
                        withContext(caller){onProgress(value)}
                    }
                    delay(100)
                }
            }
            require(Files.size(output)<128*1024){"更新回执超过限制"}
            val result=Json.parseToJsonElement(Files.readString(output)).jsonObject
            require(result["status"]?.jsonPrimitive?.content in setOf("AVAILABLE","UP_TO_DATE","DOWNLOADED_VERIFIED","FAILED")){"更新进程返回未知状态"}
            require(process.exitValue()==0 || result["status"]?.jsonPrimitive?.content=="FAILED"){"更新进程未正常退出"}
            result
        } finally {
            if(process.isAlive) {process.destroy();if(!process.waitFor(2,java.util.concurrent.TimeUnit.SECONDS))process.destroyForcibly().waitFor()}
        }
      }
    }
}
