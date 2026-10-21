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
    val versionTwo=Files.exists(marker,LinkOption.NOFOLLOW_LINKS)&&readBounded(marker,1024).decodeToString()=="CCBStudioSuite/WindowsStudio/v2"
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
        require(value["sourceDataVersion"]?.jsonPrimitive?.int==if(versionTwo)2 else StudioWorkspace.DATA_VERSION)
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
        safePath(target);require(!Files.exists(target,LinkOption.NOFOLLOW_LINKS)){"资料副本目录已存在，请直接打开该目录或另选新目录；未覆盖"}
        if(!confirmCopy(source,target))throw DataProblem("MIGRATION_CANCELLED","未转换，原目录保留")
        target
    }
    StudioFormatMigration.copyVersionTwo(source,destination)
    return StudioWorkspace.openStudio(destination)
}
