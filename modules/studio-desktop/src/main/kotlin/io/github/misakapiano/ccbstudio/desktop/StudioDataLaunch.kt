package io.github.misakapiano.ccbstudio.desktop

import io.github.misakapiano.ccbstudio.core.*
import kotlinx.serialization.json.*
import java.nio.file.*

/** A signed update plan or an explicit local confirmation selects a copy migration.
 * No startup silently changes an old root or treats an existing destination as empty. */
internal fun openStudioForLaunch(root:Path,runtime:Path,planPath:Path?,confirmCopy:(Path,Path)->Boolean,
    installed:Path?=System.getProperty("jpackage.app-path")?.let{Path.of(it).parent}):StudioWorkspace {
    val source=root.toAbsolutePath().normalize();safePath(source)
    val marker=source.resolve("preview-root.txt")
    val markerText=if(Files.exists(marker,LinkOption.NOFOLLOW_LINKS))readBounded(marker,1024).decodeToString()else null
    if(markerText!=null&&markerText !in setOf("CCBStudioSuite/WindowsStudio/v2","CCBStudioSuite/WindowsStudio/v3"))
        throw DataProblem("UNSUPPORTED_VERSION","Workspace format is not supported; retained")
    val versionTwo=markerText=="CCBStudioSuite/WindowsStudio/v2"
    val plan=planPath?.let{path->
        safePath(path);require(path.toAbsolutePath().normalize().startsWith(runtime.toAbsolutePath().normalize().resolve("updates/handoffs")))
        val value=Json.parseToJsonElement(readBounded(path,4*1024*1024).decodeToString()).jsonObject
        require(value["format"]?.jsonPrimitive?.int==1)
        require(value["nonce"]?.jsonPrimitive?.content?.matches(Regex("[0-9a-f]{64}"))==true)
        require(Path.of(value["dataRoot"]!!.jsonPrimitive.content).toAbsolutePath().normalize()==source)
        val config=Json.parseToJsonElement(readBounded(requireNotNull(installed).resolve("update-config.json"),65536).decodeToString()).jsonObject
        val approved=value["approved"]!!.jsonObject
        require(config["version"]==approved["version"]&&config["sourceSha"]==approved["sourceSha"]&&config["reader"]?.jsonPrimitive?.int==StudioWorkspace.DATA_VERSION)
        require(value["targetDataVersion"]?.jsonPrimitive?.int==StudioWorkspace.DATA_VERSION)
        if(value["sourceDataVersion"]?.jsonPrimitive?.int!=if(versionTwo)2 else StudioWorkspace.DATA_VERSION)
            throw DataProblem("UPDATE_DATA_VERSION_MISMATCH","Workspace format differs from approved update plan; retained")
        value
    }
    if(!versionTwo){
        if(plan!=null)require(Path.of(plan["destinationDataRoot"]!!.jsonPrimitive.content).toAbsolutePath().normalize()==source)
        return StudioWorkspace.openStudio(source)
    }
    val destination=if(plan!=null){
        val expected=updateDataDestination(source,2,StudioWorkspace.DATA_VERSION,plan["nonce"]!!.jsonPrimitive.content)
        require(Path.of(plan["destinationDataRoot"]!!.jsonPrimitive.content).toAbsolutePath().normalize()==expected)
        expected
    }else{
        val target=source.resolveSibling(if(source.fileName.toString()=="workspace-v2")"workspace-v3"else source.fileName.toString()+"-format3")
        safePath(target);if(Files.exists(target,LinkOption.NOFOLLOW_LINKS))throw DataProblem("MIGRATION_DESTINATION_EXISTS","Migration destination already exists; retained")
        if(!confirmCopy(source,target))throw DataProblem("MIGRATION_CANCELLED","未转换，原目录保留")
        target
    }
    StudioFormatMigration.copyVersionTwo(source,destination)
    return StudioWorkspace.openStudio(destination)
}

/** Never display arbitrary exception messages: they may contain private paths or request data. */
internal fun studioStartupFailureText(error:Exception):String {
    val status=(error as? DataProblem)?.status
    val detail=when(status){
        "UNSUPPORTED_VERSION"->"此工作区的资料格式不受当前版本支持。请使用支持该格式的版本打开；不要删除或重置资料目录。"
        "UPDATE_DATA_VERSION_MISMATCH"->"工作区的资料格式与本次更新确认的格式不一致，已停止更新启动。请保留原目录，使用原版本核查后再重新检查更新。"
        "MIGRATION_DESTINATION_EXISTS"->"升级副本目录已经存在，未覆盖。请直接打开已有副本，或选择另一个全新目录。"
        "MIGRATION_CANCELLED"->"已取消资料格式升级。原目录保留，可继续使用原版本。"
        "IN_USE"->"另一个窗口或进程正在使用此工作区。请正常关闭占用它的窗口后再试。"
        "FOREIGN_ROOT","RECOVERY_REQUIRED"->"目录中存在无法确认的内容，已停止打开。请保留全部文件并检查备份或恢复记录。"
        else->"启动安全检查未通过。请保留现有文件和更新记录，核对程序版本与工作区后再试。"
    }
    val safeCode=status?.takeIf{it.matches(Regex("[A-Z_]{1,64}"))}?:"STARTUP_VALIDATION_FAILED"
    return "工作区无法安全打开；现有文件保留。\n\n$detail\n\n诊断代码：$safeCode"
}
