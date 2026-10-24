package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.chatbar.desktop.*
import io.github.misakapiano.ccbstudio.core.*

internal class GenerationManagement(private val c:WorkbenchController) {
    val repository=GenerationOrganizations(c.workspace)
    var archiveKind by mutableStateOf<String?>(null)
    var history by mutableStateOf(false)
    var folderDialog by mutableStateOf(false)
    var folderParentHint by mutableStateOf<String?>(null)
    var createdFolderId by mutableStateOf<String?>(null)
    var moveBatch by mutableStateOf<String?>(null)
    var dialogOwnerDetached by mutableStateOf(false)
    fun openArchive(kind:String,detached:Boolean=false){dialogOwnerDetached=detached;archiveKind=kind}
    fun openHistory(detached:Boolean=false){dialogOwnerDetached=detached;history=true}
    fun newFolder(parent:String?=null,detached:Boolean=false){dialogOwnerDetached=detached;folderParentHint=parent;createdFolderId=null;folderDialog=true}
    val organization get()=c.state.generationOrganization
    fun batch(id:String,p:GalleryHistoryProjection?=null):GenerationBatch?=organization.batches.find{it.id==id}?:p?.batchOrigins?.values?.firstOrNull{it.batchId==id}?.let{o->
        val time=c.state.batch.takeIf{it.id==id}?.createdAt?.takeIf{it>0}?:p.entries.filter{e->e.images.any{p.batchOrigins[it.path]?.batchId==id}}.minOfOrNull{it.createdAt}?:1L
        GenerationBatch(id,"auto",o.batchName.ifBlank{"批次 ${id.take(8)}"},time)
    }
    fun archive(alias:String,parent:String?,onSaved:()->Unit)=c.run("保存并归档批次") {
        require(!c.batch.state.running && !c.batch.changing)
        if(archiveKind=="manual"){repository.archiveManual(requireNotNull(repository.state.currentManual),alias,parent);c.current=null}
        else {repository.archiveAutomatic(alias,parent);c.batch.resetAfterArchive()}
        archiveKind=null;onSaved()
    }
    fun viewBatch(id:String){c.run("查看历史批次"){
        val p=readGalleryHistory(c.workspace);val b=batch(id,p)?:return@run
        organizationOpen(c,OrganizationFolder("batch",id,b.title,generationBatchImages(organization,id,p)),p)
        c.current=generationBatchImages(organization,id,p).firstOrNull();c.view="图库";c.batch.shown=false;history=false
    }}
}

@Composable internal fun GenerationManagementDialogs(c:WorkbenchController,detached:Boolean=false) {
    val m=c.generations;val org=m.organization
    if(m.dialogOwnerDetached!=detached)return
    if(m.folderDialog){
        var name by remember{mutableStateOf("")};var kind by remember{mutableStateOf(if(m.folderParentHint==null)"project"else"chapter")};var parent by remember{mutableStateOf(m.folderParentHint)}
        AlertDialog(onDismissRequest={m.folderDialog=false},title={Text("新建项目 / 章节")},text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
            OutlinedTextField(name,{name=it.take(100)},label={Text("组织名称")},singleLine=true)
            CompactChoice("组织类型",listOf("project","chapter","folder"),kind,{when(it){"project"->"项目 Project";"chapter"->"章节 Chapter";else->"文件夹"}}){kind=it}
            OrganizationParentChoice(org,parent){parent=it}
            Text("组织只改变关联，原图位置与来源保留",color=LibraryMuted)
        }},confirmButton={TextButton(enabled=name.isNotBlank()&&!c.busy,onClick={
            val folder=GenerationFolder(name=name.trim(),kind=kind,parent=parent)
            c.library.changeOrganization(onSaved={m.createdFolderId=folder.id;m.folderDialog=false}){it.copy(folders=it.folders+folder)}
        }){Text("创建组织")}},dismissButton={TextButton(onClick={m.folderDialog=false}){Text("取消")}})
    }
    m.archiveKind?.let{kind->
        val current=if(kind=="manual")org.batches.find{it.id==org.currentManual}else c.batch.state.let{GenerationBatch(it.id,"auto",it.name,it.createdAt.takeIf{it>0}?:System.currentTimeMillis())}
        if(current!=null){
            var name by remember(current.id){mutableStateOf(current.alias)};var parent by remember(current.id){mutableStateOf(current.parent)}
            LaunchedEffect(m.createdFolderId){m.createdFolderId?.let{parent=it;m.createdFolderId=null}}
            if(!m.folderDialog)AlertDialog(onDismissRequest={if(!c.busy)m.archiveKind=null},title={Text("保存并归档${if(kind=="manual")"手动"else"自动"}批次")},text={Column(Modifier.heightIn(max=440.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
                OutlinedTextField(name,{name=it.take(100)},label={Text("自定义名称")},singleLine=true)
                Text("创建时间：${current.title.substringAfterLast(" · ")}")
                OrganizationParentChoice(org,parent){parent=it}
                StudioAction("新建组织…"){m.newFolder(parent,detached)}
                Text("最终名称：${current.copy(alias=name).title}")
                Text(if(kind=="manual")"保存成功后清空当前手动图片栏；原图与来源仍在图库。"else"保存分组、参数和结果；归档后从历史批次查看。",color=LibraryMuted)
            }},confirmButton={TextButton(enabled=name.isNotBlank()&&!c.busy&&!c.batch.changing,onClick={m.archive(name,parent){}}){Text("保存并归档")}},dismissButton={TextButton(onClick={m.archiveKind=null},enabled=!c.busy){Text("取消")}})
        }
    }
    if(m.history)AlertDialog(onDismissRequest={m.history=false},title={Text("历史批次")},text={Column(Modifier.widthIn(min=420.dp,max=680.dp).heightIn(max=480.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){
        val saved=org.batches.filter{it.archivedAt!=null}.sortedByDescending{it.archivedAt}
        if(saved.isEmpty()&&c.batch.state.archives.isEmpty())Text("暂无归档批次")
        saved.forEach{b->Column{Text(b.title);Text((if(b.kind=="manual")"手动"else"自动")+" · "+org.path(b.parent).ifBlank{"未分类"},color=LibraryMuted)
            Row{StudioAction("在图库查看"){m.viewBatch(b.id)};StudioAction("调整组织归属"){m.moveBatch=b.id}}
            if(b.kind=="auto")StudioAction("以新副本重新打开",!c.busy&&!c.batch.changing&&c.batch.state.total==0){c.run("重新打开历史批次"){m.repository.reopenAutomatic(b.id);c.batch.resetAfterArchive();m.history=false;c.batch.open()}}
        }}
        if(c.batch.state.total>0)Text("要重新打开历史自动批次，请先保存并归档当前批次。副本保持暂停，不自动生成。",color=LibraryMuted)
        c.batch.state.archives.filter{a->saved.none{b->b.alias==a.name}}.forEach{a->Text("${a.name} · ${a.completed}/${a.total} 张（历史记录）")}
    }},confirmButton={TextButton(onClick={m.history=false}){Text("关闭")}})
    m.moveBatch?.let{id->val b=org.batches.single{it.id==id};var parent by remember(id){mutableStateOf(b.parent)}
        AlertDialog(onDismissRequest={m.moveBatch=null},title={Text("调整批次上级组织")},text={Column{Text(b.title);OrganizationParentChoice(org,parent){parent=it};Text("原图和批次/分组来源保留",color=LibraryMuted)}},confirmButton={TextButton(onClick={c.library.changeOrganization{it.copy(batches=it.batches.map{b->if(b.id==id)b.copy(parent=parent)else b})};m.moveBatch=null}){Text("保存位置")}},dismissButton={TextButton(onClick={m.moveBatch=null}){Text("取消")}})
    }
}
@Composable private fun OrganizationParentChoice(org:GenerationOrganization,parent:String?,change:(String?)->Unit){
    CompactChoice("上级组织",listOf("")+org.folders.map{it.id},parent.orEmpty(),{if(it.isEmpty())"未分类批次"else org.path(it)}){change(it.takeIf{it.isNotEmpty()})}
}
