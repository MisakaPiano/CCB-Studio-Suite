package io.github.misakapiano.ccbstudio.desktop

import kotlinx.serialization.json.*
import java.nio.file.*

/** Isolated A only: normal entry uses the production GUI, update drive uses the
 * existing synthetic native-A harness. Never part of a shipped application JAR. */
object RelocationNativeA {
    @JvmStatic fun main(args:Array<String>) {
        if(args.size==1){ReviewUpdateNativeA.main(args);return}
        fun option(name:String)=Path.of(args[args.indexOf(name)+1]).toAbsolutePath().normalize()
        val data=option("--studio-root");val runtime=option("--runtime-root")
        require(data.any{it.toString()=="review-update-native"}&&runtime.any{it.toString()=="review-update-native"})
        Files.createDirectories(runtime)
        writeUpdateJson(runtime.resolve("NATIVE_ENTRY_OBSERVATION.json"),buildJsonObject{
            put("data",data.toString());put("runtime",runtime.toString());put("pid",ProcessHandle.current().pid())
            put("command",ProcessHandle.current().info().command().orElse("unknown"));put("scope","TEST_ONLY_ADAPTER_DELEGATES_TO_PRODUCTION_LAUNCH_STUDIO")
        })
        launchStudio(args)
    }
}
