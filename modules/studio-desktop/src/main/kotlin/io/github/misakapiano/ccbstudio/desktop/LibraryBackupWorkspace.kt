package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.chatbar.desktop.*
import io.github.misakapiano.ccbstudio.core.*
import java.nio.file.*

internal data class RestoreChoice(val backup:Path,val destination:Path)

/** Product entry points over the existing verified backup, restore and copy-migration core. */
internal class LibraryBackupController(private val c:WorkbenchController,
    internal var chooseFolder:(String,Path?)->Path?={title,initial->
        StudioFileDialogs.folder(title,initial?.takeIf{Files.isDirectory(it)})
    },internal var launchWorkspace:(Path)->Unit={root->
        val executable=System.getProperty("jpackage.app-path")?.let(Path::of)
            ?:throw DataProblem("INSTALLED_APP_REQUIRED","请使用安装版打开另一个工作区")
        require(Files.isRegularFile(executable))
        ProcessBuilder(executable.toString(),"--studio-root",root.toString(),"--runtime-root",root.resolveSibling(root.fileName.toString()+"-runtime").toString(),"--secondary-workspace").directory(executable.parent.toFile()).start()
        Unit
    }) {
    var backup by mutableStateOf<Path?>(null);private set
    var restored by mutableStateOf<Path?>(null);private set
    var choice by mutableStateOf<RestoreChoice?>(null);private set
    var restoreSource by mutableStateOf<Path?>(null);private set
    var receipt by mutableStateOf("");private set
    fun createBackup()=c.run("备份图库与工程") {
        backup=null;receipt="正在保存草稿并校验备份…"
        try {
        val path=c.workspace.backup()
        require(Files.isRegularFile(path.resolve("COMPLETE.txt")))
        backup=path;receipt="备份完成：原件、工程、独立 Prompt、标签和历史已保存。凭据与本机外部目录授权不随备份导出。"
        }catch(e:Exception){receipt="备份未完成；已有原件和备份保留，请查看错误回执。";throw e}
    }
    fun selectRestore() {
        if(c.busy||c.maintenance)return
        val source=chooseFolder("第 1 步：选择备份来源（包含 COMPLETE.txt 的目录）",backup?:c.workspace.root.resolve("backups"))?:return
        prepareSource(source)
    }
    fun useLatestBackup(){if(!c.busy&&!c.maintenance)backup?.let(::prepareSource)}
    private fun prepareSource(source:Path) {
        choice=null;restoreSource=null
        try {
            safePath(source)
            if(!Files.isRegularFile(source.resolve("COMPLETE.txt"),LinkOption.NOFOLLOW_LINKS) ||
                !Files.isRegularFile(source.resolve("assets/backup-manifest.rec"),LinkOption.NOFOLLOW_LINKS) ||
                !Files.isDirectory(source.resolve("documents"),LinkOption.NOFOLLOW_LINKS)) {
                c.problem="所选目录不是完整的 Studio 备份。请选择备份完成后显示的目录，其中应有 COMPLETE.txt、assets 和 documents。恢复位置将在下一步单独选择；当前资料未改变。";return
            }
            restoreSource=source.toAbsolutePath().normalize()
        }catch(e:Exception){c.problem="备份来源无法读取；尚未开始恢复，请重新选择完整备份目录。"}
    }
    fun selectDestination() {
        if(c.busy||c.maintenance)return
        val source=restoreSource?:return
        val parent=chooseFolder("第 2 步：选择新工作区的存放位置（不是备份来源）",c.workspace.root.parent)?:return
        val destination=parent.toAbsolutePath().normalize().resolve("Studio-Restored-${newId()}")
        val current=c.workspace.root.toAbsolutePath().normalize()
        if(destination.startsWith(current)||current.startsWith(destination)){
            c.problem="恢复位置必须在当前工作区之外，请重新选择存放位置。当前工作区未改变。";return
        }
        choice=RestoreChoice(source.toAbsolutePath().normalize(),destination)
    }
    fun cancelRestore(){choice=null}
    fun restore() {
        val requested=choice?:return
        if(c.busy||c.maintenance)return
        choice=null
        c.run("验证并恢复工作区") {
            restored=null;receipt="正在验证全部备份文件并恢复到新目录…"
            try {
            val target=StudioRestore.restore(requested.backup,requested.destination)
            val marker=readBounded(target.resolve("preview-root.txt"),1024).decodeToString()
            val opened=when(marker) {
                "CCBStudioSuite/WindowsStudio/v2"->StudioFormatMigration.copyVersionTwo(target,target.resolveSibling(target.fileName.toString()+"-format3"))
                "CCBStudioSuite/WindowsPreview/SYNTHETIC_ONLY/v1"->StudioMigration.fromLegacyBackup(requested.backup,target.resolveSibling(target.fileName.toString()+"-format3"))
                else->target
            }
            StudioWorkspace.openStudio(opened).use{it.catalog();LibraryRepository(it).load()}
            restored=opened
            receipt="恢复并校验完成。可直接打开新工作区；当前工作区和备份保留。账户需要重新填入凭据，外部收集目录需要明确重新启用。"
            }catch(e:Exception){receipt="恢复未完成；没有覆盖当前工作区。备份和可能存在的暂存副本保留，请查看错误回执。";throw e}
        }
    }
    fun openRestored() {
        val path=restored?:return
        if(c.busy||c.maintenance)return
        try{launchWorkspace(path);receipt="已请求在独立窗口打开恢复工作区；当前窗口保留。"}
        catch(e:Exception){c.problem=if(e is DataProblem && e.status=="INSTALLED_APP_REQUIRED")"请从已安装的 Studio 打开恢复工作区；恢复文件已保留。"else"恢复工作区未能打开；文件已保留，可重试。"}
    }
}

@Composable internal fun LibraryBackupWorkspace(c:WorkbenchController,b:LibraryBackupController=c.backups) {
    Text("备份与恢复",style=MaterialTheme.typography.h6)
    Text("备份整个工作区的原件、派生资产、Prompt、分类、工程和历史。账户配置保留，API 凭据不导出；恢复后需要重新配置凭据和外部目录入口。")
    Text("当前工作区")
    SelectionContainer{Text(c.workspace.root.toString())}
    StudioActions {
        StudioAction("备份图库与工程",!c.busy&&!c.maintenance){b.createBackup()}
        StudioAction("从备份恢复到新工作区",!c.busy&&!c.maintenance){b.selectRestore()}
        if(c.busy&&c.activeOperation in setOf("备份图库与工程","验证并恢复工作区"))StudioAction(if(c.activeOperation=="备份图库与工程")"取消备份" else "取消恢复"){c.active?.cancel()}
    }
    if(c.busy)LinearProgressIndicator(Modifier.fillMaxWidth())
    if(b.receipt.isNotBlank())Text(b.receipt)
    b.backup?.let{path->
        Text("完整备份目录（可复制到其他磁盘保管）")
        SelectionContainer{Text(path.toString())}
        StudioActions {
            StudioAction("恢复这份备份",!c.busy&&!c.maintenance){b.useLatestBackup()}
            StudioAction("打开备份文件夹"){runCatching{java.awt.Desktop.getDesktop().open(path.toFile())}.onFailure{c.problem="文件夹未能打开，备份仍保留在显示的路径。"}}
            StudioAction("复制备份路径"){desktopCopyText(path.toString())}
        }
    }
    b.restoreSource?.let{source->
        Card(backgroundColor=MaterialTheme.colors.primary.copy(alpha=.10f),modifier=Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("第 1 步已选择备份来源")
                SelectionContainer{Text(source.toString())}
                Text("第 2 步选择新工作区的存放位置；开始恢复时会校验全部备份文件。")
                Button(onClick=b::selectDestination,enabled=!c.busy&&!c.maintenance){Text("选择恢复位置")}
            }
        }
    }
    b.restored?.let{path->
        Card(backgroundColor=MaterialTheme.colors.primary.copy(alpha=.15f),modifier=Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("新工作区已就绪")
                SelectionContainer{Text(path.toString())}
                Button(onClick=b::openRestored,enabled=!c.busy&&!c.maintenance){Text("打开恢复工作区")}
            }
        }
    }
    b.choice?.let{choice->AlertDialog(onDismissRequest=b::cancelRestore,title={Text("恢复到独立新目录")},text={
        Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("完整校验后恢复。不会覆盖当前资料或已有目录。旧格式会再建立格式 3 副本，旧恢复目录仍保留。")
            Text("读取这份备份（不会修改）")
            SelectionContainer{Text(choice.backup.toString())}
            Text("建立新工作区（不会覆盖已有目录）")
            SelectionContainer{Text(choice.destination.toString())}
            Text("API 凭据不会恢复；外部收集目录不会自动扫描。")
        }
    },confirmButton={TextButton(onClick=b::restore){Text("确认恢复新工作区")}},dismissButton={TextButton(onClick=b::cancelRestore){Text("取消恢复")}})}
}
