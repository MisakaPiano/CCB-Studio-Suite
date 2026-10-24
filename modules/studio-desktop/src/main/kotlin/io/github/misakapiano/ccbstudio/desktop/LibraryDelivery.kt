package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.chatbar.desktop.*
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import java.io.OutputStream
import java.nio.channels.Channels
import java.nio.channels.FileChannel
import java.nio.file.*
import java.security.MessageDigest

/** Publish a new independent batch only after every original and its receipt is written. */
internal suspend fun exportLibraryOriginals(c:WorkbenchController,ids:List<String>,parent:Path):Path =
    exportLibraryOriginals(c.workspace,ids,parent)

internal suspend fun exportLibraryOriginals(workspace:StudioWorkspace,ids:List<String>,parent:Path):Path {
    require(ids.isNotEmpty())
    val root=workspace.root.toAbsolutePath().normalize();val destination=parent.toAbsolutePath().normalize()
    require(!destination.startsWith(root)){"请选择资料库之外的导出目录"}
    val name="Studio-Originals-${newId()}";val stage=Files.createDirectory(destination.resolve(".$name.incomplete"))
    val files=ids.distinct().mapIndexed{i,id->
        currentCoroutineContext().ensureActive()
        val asset=workspace.assets.read(java.util.UUID.fromString(id)).require()
        val filename=asset.request().name().replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"),"_").takeLast(160).ifBlank{"image.png"}
        val file="${(i+1).toString().padStart(4,'0')}-$filename";val target=stage.resolve(file)
        safePath(target)
        val context=currentCoroutineContext();val digest=MessageDigest.getInstance("SHA-256");var count=0L
        FileChannel.open(target,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE,LinkOption.NOFOLLOW_LINKS).use{channel->
            val output=Channels.newOutputStream(channel)
            val checked=object:OutputStream() {
                override fun write(bytes:ByteArray,offset:Int,length:Int) {
                    context.ensureActive();output.write(bytes,offset,length);digest.update(bytes,offset,length);count+=length
                }
                override fun write(value:Int) {
                    context.ensureActive();output.write(value);digest.update(value.toByte());count++
                }
            }
            // Keep the existing admitted workspace/store export and its full checksum checks.
            // Hash/count exactly the bytes written for the receipt, without whole-file arrays.
            workspace.operation{workspace.assets.export(java.util.UUID.fromString(id),checked)}
            context.ensureActive();channel.force(true)
        }
        buildJsonObject{put("file",file);put("sourceId",id);put("sha256",java.util.HexFormat.of().formatHex(digest.digest()));put("bytes",count)}
    }
    currentCoroutineContext().ensureActive()
    writeNewForced(stage.resolve("COMPLETE.json"),buildJsonObject{put("version",1);put("kind","originals");put("files",JsonArray(files))}.toString().toByteArray())
    return Files.move(stage,destination.resolve(name),StandardCopyOption.ATOMIC_MOVE)
}

internal fun startLibraryExport(c:WorkbenchController,ids:List<String>) {
    if(c.busy||ids.isEmpty())return
    val fixed=ids.distinct().toList();val destination=c.gallery.chooseExportDirectory()?:return
    c.run("导出所选原始文件"){val path=exportLibraryOriginals(c,fixed,destination);c.message="已导出 ${fixed.size} 份原件：$path"}
}

@Composable internal fun LibraryInteractionHelp() {
    var open by remember{mutableStateOf(false)}
    Box {
        LibraryAction("操作说明",style=StudioActionStyle.TERTIARY){open=true}
        StudioMenu(open,{open=false}){Text("图片单击选择，双击看大图；相册整卡单击进入。\nCtrl 切换选择，Shift 连选，空白处拖动框选。\n拖到下方工具处理整组；Esc 取消拖动。\nCtrl+C 复制原始文件；Delete 可逆移入回收站。\nCtrl+Z / Ctrl+Y 撤销与重做；输入框先处理文字。\n标签相册剪切/粘贴只移动标签归属，不移动外部文件。",Modifier.width(390.dp).padding(12.dp))}
    }
}
