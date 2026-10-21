package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.example.chatbar.desktop.*

/** Shared IDs cross workspaces; choosing never copies or mutates original files. */
@Composable internal fun SharedAssetPicker(c:WorkbenchController,multiple:Boolean,onClose:()->Unit,onChoose:(List<String>)->Unit) {
    var query by remember{mutableStateOf("")};var selected by remember{mutableStateOf(setOf<String>())}
    AlertDialog(onDismissRequest=onClose,title={Text("从全程序资产库选用")},text={
        Column(Modifier.widthIn(min=280.dp,max=720.dp).heightIn(max=520.dp)) {
            Text("导入、生成、引导和编辑输出共用同一资产库；选用只建立引用，原件不变。")
            OutlinedTextField(query,{query=it},label={Text("查找资产名称")},modifier=Modifier.fillMaxWidth())
            LazyVerticalGrid(GridCells.Adaptive(120.dp),Modifier.weight(1f)) {
                items(c.assets.filter{it.request().name().contains(query,true)},key={it.request().id().toString()}) {a->
                    val id=a.request().id().toString()
                    Column(Modifier.padding(4.dp).border(1.dp,if(id in selected)Gold else Quiet)
                        .clickable{selected=if(multiple){if(id in selected)selected-id else selected+id}else setOf(id)}
                        .semantics{contentDescription="选用资产 $id";this.selected=id in selected}) {
                        AssetImage(c,id,Modifier.fillMaxWidth().height(100.dp));Text(a.request().name(),maxLines=2)
                    }
                }
            }
            Text("${selected.size} 项已选")
        }
    },confirmButton={TextButton(enabled=selected.isNotEmpty(),onClick={onChoose(c.assets.map{it.request().id().toString()}.filter{it in selected});onClose()}){Text("确认选用资产")}},dismissButton={TextButton(onClick=onClose){Text("取消")}})
}

@Composable internal fun SharedAssetOrigin(c:WorkbenchController,id:String) {
    val asset=c.assets.firstOrNull{it.request().id().toString()==id}?:return
    val origin=remember(id){io.github.misakapiano.ccbstudio.core.SharedAssets.provenance(asset)}
    val parents=origin?.sourceIds?:listOfNotNull(asset.request().parentId()?.toString())
    if(parents.isNotEmpty()) {
        StudioDisclosure("资产来源与编辑工程",initiallyOpen=true) {
            origin?.let{Text("${it.role} · ${it.variant} · 工程版本 ${it.projectRevision}")}
            Text("资产 ${id.take(8)} · ${asset.size()} bytes · SHA-256 ${asset.sha256()}")
            parents.forEach{parent->StudioAction("查看来源 ${parent.take(8)}"){c.current=parent;c.gallery.metadata=parent;c.view="图库"}}
            origin?.let { p->
                val project=c.state.typesetProjects.firstOrNull{it.id==p.projectId}
                if(project!=null)StudioAction("打开来源工程 · ${project.name}",!c.busy){c.editor.open(project.id)}
                else Text("来源工程条目已移除；该版本快照与原件仍保留。")
            }
        }
    }
}

@Composable internal fun SharedDocumentAssets(c:WorkbenchController) {
    val documents=remember(c.assets,c.state){io.github.misakapiano.ccbstudio.core.SharedAssets.documents(c.workspace)}
    if(documents.isNotEmpty())StudioDisclosure("整册与归档资产 · ${documents.size}") {
        Column(Modifier.heightIn(max=180.dp).verticalScroll(rememberScrollState())) {
            documents.asReversed().forEach{asset->
                val origin=io.github.misakapiano.ccbstudio.core.SharedAssets.provenance(asset)!!
                val project=c.state.typesetProjects.firstOrNull{it.id==origin.projectId}
                Text("${asset.request().name()} · ${project?.name?:origin.projectId.take(8)} · 版本 ${origin.projectRevision} · ${asset.size()} bytes")
                StudioActions {
                    StudioAction("另存资产 ${asset.request().id().toString().take(8)}",!c.busy){
                        val chooser=javax.swing.JFileChooser();chooser.selectedFile=java.io.File(asset.request().name())
                        if(chooser.showSaveDialog(null)==javax.swing.JFileChooser.APPROVE_OPTION) {
                            val target=chooser.selectedFile.toPath()
                            c.run("另存资产") {
                                require(!java.nio.file.Files.exists(target)){"Destination already exists; retained"}
                                val pending=target.resolveSibling(".${target.fileName}.${io.github.misakapiano.ccbstudio.core.newId()}.pending")
                                java.nio.file.Files.newOutputStream(pending,java.nio.file.StandardOpenOption.CREATE_NEW).use{c.workspace.assets.export(asset.request().id(),it)}
                                java.nio.channels.FileChannel.open(pending,java.nio.file.StandardOpenOption.WRITE).use{it.force(true)}
                                java.nio.file.Files.move(pending,target)
                            }
                        }
                    }
                    if(project!=null)StudioAction("打开工程 ${project.name}",!c.busy){c.editor.open(project.id)}
                }
            }
        }
    }
}
