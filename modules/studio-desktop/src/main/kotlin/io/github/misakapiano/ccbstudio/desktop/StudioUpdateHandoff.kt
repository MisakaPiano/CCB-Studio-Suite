package io.github.misakapiano.ccbstudio.desktop

import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.nio.file.*
import java.nio.channels.FileChannel
import java.security.SecureRandom

/** Fingerprints only. Plain credentials never enter the handoff or log. */
internal fun updateSnapshot(workspace:StudioWorkspace,sourceVersion:Int=workspace.state.version,legacy:Boolean=false):JsonObject=buildJsonObject {
    require(sourceVersion in 1..workspace.state.version)
    // Hash the authenticated committed representation, not defaults introduced by this reader.
    // Old handoffs used serialization; for a same-format, drained workspace its persisted body
    // is exactly the old writer's serialization. Assets and credentials remain fully compared.
    val body=if(sourceVersion==workspace.state.version)workspace.committedStateBody else workspace.committedStateBodyForVersion(sourceVersion)
    put("stateSha256",sha256(body.toByteArray()))
    if(!legacy)put("stateEncoding","COMMITTED_STATE_V1")
    put("assets",buildJsonArray {
        workspace.catalog().sortedBy{it.request().id().toString()}.forEach {asset->
            val request=asset.request();val id=request.id().toString()
            // Export verifies the actual bytes while discarding the stream; large books
            // must not require a second in-memory copy during the update handoff.
            workspace.assets.export(request.id(),java.io.OutputStream.nullOutputStream())
            add(buildJsonObject {
                put("id",id);put("sha256",asset.sha256());put("size",asset.size())
                put("kind",request.kind().name);put("parent",request.parentId()?.toString()?.let(::JsonPrimitive)?:JsonNull)
                put("nameSha256",sha256(request.name().toByteArray()));put("recipeSha256",sha256(request.recipe()));put("projectSha256",sha256(request.project()))
            })
        }
    })
    val vault=SecretVault(workspace.root.resolve("secrets"))
    put("credentials",buildJsonArray {
        workspace.state.accounts.mapNotNull{it.secretRef}.distinct().sorted().forEach{ref->
            add(buildJsonObject {
                put("ref",ref)
                try {put("sha256",vault.use(ref){sha256(it)})}
                catch(e:DataProblem) {
                    // Ordinary backup deliberately omits credentials. Record an absent
                    // baseline, never skip the reference or accept decryption/read errors.
                    // B must match this exact snapshot, so loss of a readable credential
                    // after preflight still fails verification.
                    if(e.status!="MISSING")throw e
                    put("status","MISSING_BEFORE_UPDATE")
                }
            })
        }
    })
}

internal fun writeUpdateJson(path:Path,value:JsonObject) {
    val bytes=value.toString().toByteArray()
    val stage=path.resolveSibling(path.fileName.toString()+"."+newId()+".tmp")
    FileChannel.open(stage,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE).use{channel->
        val buffer=java.nio.ByteBuffer.wrap(bytes);while(buffer.hasRemaining())channel.write(buffer);channel.force(true)
    }
    Files.move(stage,path,StandardCopyOption.ATOMIC_MOVE)
}

internal suspend fun prepareUpdateHandoff(c:WorkbenchController,runtime:Path):Path=withContext(Dispatchers.IO) {
    val installed=System.getProperty("jpackage.app-path")?.let{Path.of(it).parent}?:error("更新需要安装版本")
    val approved=c.updater.result?:error("尚未下载候选")
    require(approved["status"]?.jsonPrimitive?.content=="DOWNLOADED_VERIFIED"){"安装前必须完成验证下载"}
    val cache=runtime.resolve("updates").toAbsolutePath().normalize()
    val folder=cache.resolve("handoffs").resolve(newId());Files.createDirectories(folder)
    val nonce=ByteArray(32).also{SecureRandom().nextBytes(it)}.joinToString(""){"%02x".format(it)}
    val sourceVersion=c.workspace.state.version
    val targetVersion=approved["dataVersion"]?.jsonPrimitive?.int?:sourceVersion
    require(targetVersion in sourceVersion..3){"不支持的目标资料格式"}
    val destination=updateDataDestination(c.workspace.root,sourceVersion,targetVersion,nonce)
    val snapshot=try {updateSnapshot(c.workspace)}catch(e:Exception) {
        writeUpdateJson(folder.resolve("PREPARATION_FAILED.json"),buildJsonObject {
            put("status","FAILED_RETAINED");put("phase","WORKSPACE_SNAPSHOT")
            put("dataRoot",c.workspace.root.toString());put("reason",if(e is DataProblem)e.status else e.javaClass.simpleName)
        })
        throw e
    }
    val plan=buildJsonObject {
        put("format",1);put("nonce",nonce);put("installed",installed.toAbsolutePath().toString());put("cache",cache.toString())
        put("dataRoot",c.workspace.root.toString());put("runtimeRoot",runtime.toAbsolutePath().toString());put("parentPid",ProcessHandle.current().pid())
        put("sourceDataVersion",sourceVersion);put("targetDataVersion",targetVersion);put("destinationDataRoot",destination.toString())
        put("approved",approved);put("snapshot",snapshot)
    }
    val path=folder.resolve("plan.json");writeUpdateJson(path,plan)
    val process=ProcessBuilder(installed.resolve("updater/pythonw.exe").toString(),"-B",installed.resolve("updater/lib/apply_update.py").toString(),"--handoff",path.toString())
        .directory(installed.toFile()).redirectOutput(folder.resolve("helper-stdout.log").toFile()).redirectError(folder.resolve("helper-stderr.log").toFile()).start()
    try {
        withTimeout(120000) {
            while(true) {
                val result=folder.resolve("result.json")
                if(Files.exists(result)) {
                    require(Files.size(result)<=4*1024*1024)
                    val status=Json.parseToJsonElement(Files.readString(result)).jsonObject
                    when(status["status"]?.jsonPrimitive?.content) {
                        "READY_TO_DRAIN"->return@withTimeout
                        "FAILED_RETAINED"->error("更新交接失败：${status["reason"]?.jsonPrimitive?.content}")
                    }
                }
                check(process.isAlive){"独立更新进程提前退出"};delay(100)
            }
        }
    } catch(e:Exception) {
        Files.writeString(folder.resolve("CANCEL"),"Cancelled before parent exit",StandardOpenOption.CREATE_NEW)
        throw e
    }
    path
}

internal fun updateDataDestination(root:Path,sourceVersion:Int,targetVersion:Int,nonce:String):Path {
    require(nonce.matches(Regex("[0-9a-f]{64}")) && targetVersion>=sourceVersion)
    val source=root.toAbsolutePath().normalize()
    if(sourceVersion==targetVersion)return source
    val destination=source.resolveSibling("${source.fileName}-format$targetVersion-${nonce.take(12)}")
    safePath(destination);require(!Files.exists(destination,LinkOption.NOFOLLOW_LINKS)){"资料迁移目标已存在；保留并停止本次交接"}
    return destination
}

internal fun verifyUpdatedWorkspace(workspace:StudioWorkspace,planPath:Path):JsonObject {
    require(Files.size(planPath)<=4*1024*1024)
    val plan=Json.parseToJsonElement(Files.readString(planPath)).jsonObject
    require(plan["format"]?.jsonPrimitive?.int==1)
    val cache=Path.of(plan["cache"]!!.jsonPrimitive.content).toAbsolutePath().normalize()
    require(planPath.toAbsolutePath().normalize().startsWith(cache.resolve("handoffs")))
    val destination=(plan["destinationDataRoot"]?:plan["dataRoot"])!!.jsonPrimitive.content
    require(Path.of(destination).toAbsolutePath().normalize()==workspace.root)
    val installed=System.getProperty("jpackage.app-path")?.let{Path.of(it).parent}?:error("更新验证需要安装版本")
    val config=Json.parseToJsonElement(Files.readString(installed.resolve("update-config.json"))).jsonObject
    val approved=plan["approved"]!!.jsonObject
    require(config["version"]==approved["version"] && config["sourceSha"]==approved["sourceSha"]){"程序身份不匹配"}
    val sourceVersion=plan["sourceDataVersion"]?.jsonPrimitive?.int?:workspace.state.version
    val targetVersion=plan["targetDataVersion"]?.jsonPrimitive?.int?:sourceVersion
    require(workspace.state.version==targetVersion && config["reader"]?.jsonPrimitive?.int==targetVersion){"资料格式与程序身份不匹配"}
    val expected=plan["snapshot"]!!.jsonObject
    require(expected["stateEncoding"]==null || expected["stateEncoding"]?.jsonPrimitive?.content=="COMMITTED_STATE_V1"){"未知更新快照格式"}
    val snapshot=updateSnapshot(workspace,sourceVersion,legacy=expected["stateEncoding"]==null)
    if(snapshot!=expected)throw DataProblem("UPDATE_SNAPSHOT_MISMATCH","更新后资料或凭据验证不一致；已保留旧程序与现有资料")
    return buildJsonObject{put("nonce",plan["nonce"]!!);put("version",config["version"]!!);put("sourceSha",config["sourceSha"]!!);put("snapshot",snapshot)
        put("dataRoot",workspace.root.toString());put("dataVersion",workspace.state.version);put("actualStateSha256",sha256(documentJson.encodeToString(workspace.state).toByteArray()))}
}
