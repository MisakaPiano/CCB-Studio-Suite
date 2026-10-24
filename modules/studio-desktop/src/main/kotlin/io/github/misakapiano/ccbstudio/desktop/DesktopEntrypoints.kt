package io.github.misakapiano.ccbstudio.desktop

import com.sun.jna.Native
import com.sun.jna.WString
import com.sun.jna.win32.StdCallLibrary
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.serialization.json.*
import java.nio.file.*
import java.util.concurrent.TimeUnit

internal object DesktopEntrypoints {
    const val APP_ID="MisakaPiano.CCBStudioSuite.Preview"
    private interface Shell:StdCallLibrary {fun SetCurrentProcessExplicitAppUserModelID(id:WString):Int}
    fun initialize(){if(System.getProperty("os.name").startsWith("Windows"))runCatching{check(Native.load("shell32",Shell::class.java).SetCurrentProcessExplicitAppUserModelID(WString(APP_ID))==0)}}
    private fun installed()=System.getProperty("jpackage.app-path")?.let{Path.of(it).toAbsolutePath().parent}?:error("请使用安装版本创建入口")
    fun stable()=installed().parent.resolve("CCBStudioLauncher.exe")
    private fun helper()=installed().resolve("updater/CCBStudioLauncher.exe")
    private fun execute(vararg args:String){
        val executable=if(args.firstOrNull()=="--shortcut")stable()else helper()
        val p=ProcessBuilder(listOf(executable.toString())+args).redirectErrorStream(true).start()
        check(p.waitFor(20,TimeUnit.SECONDS)&&p.exitValue()==0){"Windows入口设置未完成"}
    }
    /** This is called only after B completes handoff, or during an ordinary verified installation launch. */
    fun ensure(data:Path,runtime:Path) {
        val app=installed();val root=app.parent;safePath(root);safePath(stable())
        val manifest=Json.parseToJsonElement(Files.readString(app.resolve("INSTALL_MANIFEST.json"))).jsonObject
        val config=Json.parseToJsonElement(Files.readString(app.resolve("update-config.json"))).jsonObject
        val descriptor=root.resolve("launch-current.json")
        if(Files.exists(descriptor)){safePath(descriptor);require(Files.size(descriptor)<16384);val old=Json.parseToJsonElement(Files.readString(descriptor)).jsonObject
            require((old["version"]?.jsonPrimitive?.int?:0)<=(config["version"]?.jsonPrimitive?.int?:0)){"较新版本已有固定入口，请从原入口启动"}}
        fun verified(path:String):ByteArray {
            val entry=manifest["files"]!!.jsonArray.single{it.jsonObject["path"]!!.jsonPrimitive.content==path}.jsonObject
            safePath(app.resolve(path));val bytes=Files.readAllBytes(app.resolve(path));require(bytes.size.toLong()==entry["bytes"]!!.jsonPrimitive.long&&sha256(bytes)==entry["sha256"]!!.jsonPrimitive.content);return bytes
        }
        val launcher=verified("updater/CCBStudioLauncher.exe");val exe=verified("CCBStudioSuitePreview.exe")
        fun atomic(target:Path,bytes:ByteArray){safePath(target);val stage=target.resolveSibling(target.fileName.toString()+"."+newId()+".tmp");writeNewForced(stage,bytes);try{Files.move(stage,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING)}finally{Files.deleteIfExists(stage)}}
        // Avoid replacing a currently executing stable launcher when its content is identical.
        if(!Files.exists(stable())||!Files.readAllBytes(stable()).contentEquals(launcher))atomic(stable(),launcher)
        atomic(descriptor,buildJsonObject{put("format",1);put("version",config["version"]!!);put("installed",app.toString());put("data",data.toAbsolutePath().normalize().toString());put("runtime",runtime.toAbsolutePath().normalize().toString());put("exeSha256",sha256(exe))}.toString().toByteArray())
        execute("--shortcut",root.resolve("Launch-CCB-Studio.lnk").toString())
    }
    fun create(kind:String,data:Path,runtime:Path):String {
        ensure(data,runtime)
        // Known Folder lookup honors redirected desktop and roaming Start Menu paths.
        val special=if(kind=="desktop")"DesktopDirectory"else"Programs"
        val p=ProcessBuilder("powershell.exe","-NoProfile","-NonInteractive","-Command","[Console]::OutputEncoding=[Text.Encoding]::UTF8;[Environment]::GetFolderPath([Environment+SpecialFolder]::$special)").start()
        val folder=p.inputStream.bufferedReader(Charsets.UTF_8).readText().trim();check(p.waitFor(10,TimeUnit.SECONDS)&&p.exitValue()==0&&folder.isNotBlank())
        val destination=Path.of(folder).resolve("CCB Studio Suite.lnk");safePath(destination)
        require(!Files.exists(destination)||runCatching{Files.size(destination)<65536}.getOrDefault(false)){"入口同名文件已存在，请检查"}
        execute("--shortcut",destination.toString());return destination.toString()
    }
    fun configureWindow(window:java.awt.Window){if(Files.isRegularFile(helper()))execute("--window",com.sun.jna.Pointer.nativeValue(Native.getComponentPointer(window)).toString(),stable().toString())}
}
