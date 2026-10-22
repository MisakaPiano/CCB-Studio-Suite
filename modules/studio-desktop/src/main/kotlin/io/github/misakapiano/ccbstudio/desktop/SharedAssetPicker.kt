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
    val available=c.assets.filterNot{it.request().id().toString() in c.library.archived}
    val availableIds=available.map{it.request().id().toString()}.toSet()
    val chosen=selected.intersect(availableIds)
    AlertDialog(onDismissRequest=onClose,title={Text("从全程序资产库选用")},text={
        Column(Modifier.widthIn(min=280.dp,max=720.dp).heightIn(max=520.dp)) {
            Text("导入、生成、引导和编辑输出共用同一资产库；选用只建立引用，原件不变。")
            c.library.archiveError?.let{Text(it,color=MaterialTheme.colors.error)}
            OutlinedTextField(query,{query=it},label={Text("查找文件名、别名或标签")},modifier=Modifier.fillMaxWidth())
            LazyVerticalGrid(GridCells.Adaptive(120.dp),Modifier.weight(1f)) {
                items(available.filter{a->val labels=c.library.labels(a.request().id().toString());a.request().name().contains(query,true)||labels.alias.contains(query,true)||labels.tags.any{it.contains(query,true)}},key={it.request().id().toString()}) {a->
                    val id=a.request().id().toString()
                    Column(Modifier.padding(4.dp).border(1.dp,if(id in selected)Gold else Quiet)
                        .clickable{selected=if(multiple){if(id in selected)selected-id else selected+id}else setOf(id)}
                        .semantics{contentDescription="选用资产 $id";this.selected=id in selected}) {
                        val labels=c.library.labels(id)
                        AssetImage(c,id,Modifier.fillMaxWidth().height(100.dp));Text(labels.alias.ifBlank{a.request().name()},maxLines=2)
                        if(labels.tags.isNotEmpty())Text(labels.tags.joinToString(" · "),maxLines=2,style=MaterialTheme.typography.caption)
                    }
                }
            }
            Text("${chosen.size} 项已选")
        }
    },confirmButton={TextButton(enabled=chosen.isNotEmpty(),onClick={onChoose(available.map{it.request().id().toString()}.filter{it in chosen});onClose()}){Text("确认选用资产")}},dismissButton={TextButton(onClick=onClose){Text("取消")}})
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
    val documents=remember(c.documentAssets,c.library.archived){c.documentAssets.filterNot{it.request().id().toString() in c.library.archived}}
    if(documents.isNotEmpty())StudioDisclosure("整册与归档资产 · ${documents.size}") {
        Column(Modifier.heightIn(max=180.dp).verticalScroll(rememberScrollState())) {
            documents.asReversed().forEach{asset->
                val id=asset.request().id().toString();val label=c.library.labels(id)
                val origin=io.github.misakapiano.ccbstudio.core.SharedAssets.provenance(asset)!!
                val project=c.state.typesetProjects.firstOrNull{it.id==origin.projectId}
                Text("${label.alias.ifBlank{asset.request().name()}} · ${project?.name?:origin.projectId.take(8)} · 版本 ${origin.projectRevision} · ${asset.size()} bytes")
                if(label.tags.isNotEmpty())Text(label.tags.joinToString(" · "))
                AssetLabelEditor(c,id)
                StudioActions {
                    StudioAction("另存资产 ${asset.request().id().toString().take(8)}",!c.busy){
                        StudioFileDialogs.save("另存资产（不覆盖已有文件）",asset.request().name())?.let{target->
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
                    StudioAction("移入回收站 ${id.take(8)}",!c.busy&&c.library.error==null&&c.library.archiveError==null){c.library.archiveRequest=setOf(id)}
                }
            }
        }
    }
}
