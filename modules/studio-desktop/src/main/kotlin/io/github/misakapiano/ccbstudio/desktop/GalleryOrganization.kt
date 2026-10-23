@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class,androidx.compose.foundation.ExperimentalFoundationApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.example.chatbar.desktop.*
import com.composables.icons.lucide.*
import io.github.misakapiano.ccbstudio.core.*
import com.example.chatbar.domain.image.*
import com.example.chatbar.ui.imageprompt.*

internal fun organizationNavigate(c:WorkbenchController,kind:String,id:String?,label:String,ids:Set<String>) {
    val g=c.gallery;g.enterBatch(ids,label,"org:$kind:${id.orEmpty()}");if(kind=="all")g.levels=g.levels.take(1);c.selection=emptySet()
}
internal fun removeGalleryAlbumMembers(c:WorkbenchController){val id=c.gallery.organizationId?:return;val selected=c.selection.toSet();c.library.changeAlbums{list->list.map{if(it.id==id)it.copy(images=it.images-selected)else it}}}

@Composable private fun OrganizationRow(label:String,icon:ImageVector,selected:Boolean=false,indent:Int=0,onClick:()->Unit) {
    Row(Modifier.fillMaxWidth().padding(start=(indent*12).dp).heightIn(min=30.dp)
        .background(if(selected)LibrarySelected else Color.Transparent,RoundedCornerShape(4.dp))
        .semantics{this.selected=selected}.clickable(role=Role.Button,onClick=onClick).padding(horizontal=7.dp,vertical=6.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)) {
        Icon(icon,null,Modifier.size(16.dp));Text(label,maxLines=1,overflow=TextOverflow.Ellipsis)
    }
}
@Composable internal fun GalleryOrganizationTree(c:WorkbenchController,p:GalleryHistoryProjection,drag:LibraryDragState,offset:()->Offset) {
    val g=c.gallery
    val albums=c.library.state?.virtualAlbums.orEmpty();val all=c.assets.map{it.request().id().toString()}.toSet()-c.library.archived
    if(g.treeOpen)Column(Modifier.width(g.treeWidth.dp).fillMaxHeight().padding(horizontal=6.dp).semantics{contentDescription="图库组织树"}) {
        Row(Modifier.fillMaxWidth().padding(top=6.dp),verticalAlignment=Alignment.CenterVertically) {
            Text("组织",Modifier.weight(1f))
            Box(Modifier.background(LibrarySelected,RoundedCornerShape(5.dp))){LibraryAction("",icon=Lucide.FolderPlus,style=StudioActionStyle.TERTIARY,accessibilityLabel="新建虚拟相册"){g.organizationDialog="new"}}
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            OrganizationRow("全部资产 · ${all.size}",Lucide.Images,g.organizationKind=="all"&&g.depth==0){organizationNavigate(c,"all",null,"全部资产",all)}
            Text("生成来源",color=LibraryMuted,modifier=Modifier.padding(start=6.dp,top=14.dp,bottom=4.dp))
            OrganizationRow("批次",Lucide.Layers,g.organizationKind=="batches"){organizationNavigate(c,"batches",null,"批次",p.batchOrigins.keys)}
            p.batchOrigins.entries.groupBy{it.value.batchId}.forEach{(batch,entries)->
                val name=entries.first().value.batchName.ifBlank{c.state.batch.takeIf{it.id==batch}?.name?:"批次 ${batch.take(8)}"}
                OrganizationRow(name,Lucide.Layers,g.organizationKind=="batch"&&g.organizationId==batch,1){organizationOpen(c,OrganizationFolder("batch",batch,name,entries.map{it.key}.toSet()),p)}
                if(g.organizationKind=="batch"&&g.organizationId==batch||g.organizationKind=="group"&&entries.any{it.value.groupId==g.organizationId})entries.groupBy{it.value.groupId}.forEach{(group,images)->
                    OrganizationRow(images.first().value.groupName,Lucide.Images,g.organizationKind=="group"&&g.organizationId==group,2){organizationOpen(c,OrganizationFolder("group",group,images.first().value.groupName,images.map{it.key}.toSet()),p)}
                }
            }
            Text("自定义组织",color=LibraryMuted,modifier=Modifier.padding(start=6.dp,top=14.dp,bottom=4.dp))
            OrganizationRow("虚拟相册",Lucide.Layers,g.organizationKind=="albums"){organizationNavigate(c,"albums",null,"虚拟相册",albums.flatMap{it.images}.toSet())}
            OrganizationAlbums(c,albums,null,1,drag,offset)
            Text("虚拟组织\n原图保留原位",color=LibraryMuted,modifier=Modifier.padding(start=6.dp,top=20.dp,bottom=12.dp))
        }
    }
    GalleryOrganizationDivider(g)
}

/** One persistent divider owns both resizing and folding. The handle remains when folded. */
@Composable private fun GalleryOrganizationDivider(g:GalleryViewState) {
    val density=androidx.compose.ui.platform.LocalDensity.current
    val interaction=remember{MutableInteractionSource()};val hover by interaction.collectIsHoveredAsState()
    val label=if(g.treeOpen)"收起组织树"else"展开组织树"
    Box(Modifier.width(14.dp).fillMaxHeight().hoverable(interaction).semantics{contentDescription="组织树分割线"}
        .pointerInput(g.treeOpen){detectDragGestures{change,delta->
            change.consume();if(g.treeOpen)g.treeWidth=(g.treeWidth+with(density){delta.x.toDp().value}).coerceIn(130f,320f)
        }}) {
        Box(Modifier.fillMaxSize().pointerHoverIcon(PointerIcon(java.awt.Cursor(java.awt.Cursor.E_RESIZE_CURSOR)))
            .semantics{contentDescription="调整组织树宽度"})
        Box(Modifier.align(Alignment.Center).width(1.dp).fillMaxHeight().background(LibraryLine))
        TooltipArea(tooltip={Surface(color=LibraryPanel){Text(label,Modifier.padding(8.dp))}},modifier=Modifier.align(Alignment.Center)) {
            Box(Modifier.width(14.dp).height(52.dp).background(if(hover||!g.treeOpen)LibraryPanel else LibraryBackground,RoundedCornerShape(4.dp))
                .semantics{contentDescription=label;stateDescription=if(g.treeOpen)"已展开"else"已收起"}
                .clickable(role=Role.Button){g.treeOpen=!g.treeOpen},contentAlignment=Alignment.Center) {
                if(hover||!g.treeOpen)Icon(if(g.treeOpen)Lucide.ChevronLeft else Lucide.ChevronRight,null,Modifier.size(14.dp),tint=Gold)
            }
        }
    }
}
@Composable private fun OrganizationAlbums(c:WorkbenchController,albums:List<VirtualAlbum>,parent:String?,depth:Int,drag:LibraryDragState,offset:()->Offset) {
    albums.filter{it.parent==parent}.forEach{album->
        val target="加入相册：${album.name} (${album.id.take(4)})"
        Box(Modifier.fillMaxWidth().libraryTarget(drag,target,{ids->c.library.changeAlbums{VirtualAlbums.associate(it,ids.toSet(),setOf(album.id))}},offset,!c.busy)
            .background(if(drag.activeTarget==target)LibrarySelected else Color.Transparent)) {
            ContextMenuArea(items={listOf(ContextMenuItem("相册名称与上级…"){organizationOpen(c,OrganizationFolder("album",album.id,album.name,VirtualAlbums.members(albums,album.id)),GalleryHistoryProjection(emptyList(),emptyMap()));c.gallery.organizationDialog="edit"},ContextMenuItem("移除相册…"){organizationNavigate(c,"album",album.id,album.name,VirtualAlbums.members(albums,album.id));c.gallery.organizationDialog="remove"})}) {
                OrganizationRow(album.name,Lucide.Folder,c.gallery.organizationKind=="album"&&c.gallery.organizationId==album.id,depth){organizationOpen(c,OrganizationFolder("album",album.id,album.name,VirtualAlbums.members(albums,album.id)),GalleryHistoryProjection(emptyList(),emptyMap()))}
            }
        }
        OrganizationAlbums(c,albums,album.id,depth+1,drag,offset)
    }
}
@Composable internal fun GalleryOrganizationSource(c:WorkbenchController,id:String,p:GalleryHistoryProjection) {
    StudioDisclosure("组织与批次来源") {
        p.batchOrigins[id]?.let{o->Text("批次 ${o.batchId}\n${o.groupName} → ${o.direction} / 第${o.round}次\nSeed ${o.seed}",style=MaterialTheme.typography.caption)
            LibraryAction("定位原始分组"){organizationNavigate(c,"group",o.groupId,o.groupName,p.batchOrigins.filterValues{it.groupId==o.groupId}.keys)}}
        c.library.state?.virtualAlbums.orEmpty().filter{id in it.images}.forEach{Text("相册：${it.name}",style=MaterialTheme.typography.caption)}
        Text("组织只改变关联，原件和生成来源保留",style=MaterialTheme.typography.caption,color=LibraryMuted)
    }
}
@Composable internal fun GalleryOrganizationDialog(c:WorkbenchController) {
    val g=c.gallery;val mode=g.organizationDialog?:return;val albums=c.library.state?.virtualAlbums.orEmpty();val current=albums.find{it.id==g.organizationId}
    var name by remember(mode,current?.id){mutableStateOf(if(mode=="edit")current?.name.orEmpty()else"")}
    var parent by remember(mode,current?.id){mutableStateOf(if(mode=="edit")current?.parent else current?.id)}
    var targets by remember(mode){mutableStateOf(setOf<String>())}
    val ids=remember(mode){c.selection.toSet()}
    AlertDialog(onDismissRequest={g.organizationDialog=null},title={Text(when(mode){"new"->"新建虚拟相册";"edit"->"相册名称与上级";"remove"->"移除相册（原件保留）";"moveImages"->"调整相册归属";else->"加入虚拟相册"})},text={Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(6.dp)){
        Text("原图位置、字节和批次来源保持不变。",style=MaterialTheme.typography.caption)
        when(mode){
            "new","edit"->{OutlinedTextField(name,{name=it},label={Text("相册名称")},singleLine=true)
                Text("上级相册")
                LibraryAction(if(parent==null)"✓ 顶层相册"else"顶层相册"){parent=null}
                val excluded=if(current!=null&&mode=="edit")VirtualAlbums.descendants(albums,current.id)else emptySet()
                albums.filter{it.id !in excluded}.forEach{a->LibraryAction((if(parent==a.id)"✓ "else"")+a.name){parent=a.id}}
            }
            "remove"->Text("移除“${current?.name.orEmpty()}”及子相册的组织记录；保留原图、其他相册关联和来源。")
            else->{Text("已选 ${ids.size} 张。"+(if(mode=="moveImages")"从本相册移出关联后加入目标；其他相册归属保留。"else"可同时加入多个相册。"))
                albums.filter{mode!="moveImages"||it.id!=current?.id}.forEach{a->Row(verticalAlignment=Alignment.CenterVertically){Checkbox(a.id in targets,{targets=if(it)targets+a.id else targets-a.id},Modifier.semantics{contentDescription="加入相册 ${a.name}"});Text(a.name)}}}
        }
    }},confirmButton={TextButton(enabled=!c.busy&&when(mode){"new","edit"->name.isNotBlank()&&name.length<=100;"remove"->current!=null;else->ids.isNotEmpty()&&targets.isNotEmpty()},onClick={
        c.library.changeAlbums(onSaved={
            if(mode=="remove")organizationNavigate(c,"albums",null,"虚拟相册",c.library.state?.virtualAlbums.orEmpty().flatMap{it.images}.toSet())
            else if(mode=="edit")current?.id?.let{id->c.library.state?.virtualAlbums?.find{it.id==id}?.let{organizationOpen(c,OrganizationFolder("album",id,it.name,VirtualAlbums.members(c.library.state!!.virtualAlbums,id)),GalleryHistoryProjection(emptyList(),emptyMap()))}}
        }){list->when(mode){
            "new"->VirtualAlbums.save(list,VirtualAlbum(name=name.trim(),parent=parent))
            "edit"->VirtualAlbums.save(list,requireNotNull(current).copy(name=name.trim(),parent=parent))
            "remove"->VirtualAlbums.remove(list,requireNotNull(current).id)
            else->VirtualAlbums.associate(list,ids,targets,if(mode=="moveImages")current?.id else null)
        }};g.organizationDialog=null
    }){Text("确认")}},dismissButton={TextButton(onClick={g.organizationDialog=null}){Text("取消")}})
}

internal data class OrganizationFolder(val kind:String,val id:String,val label:String,val ids:Set<String>) {
    fun album(ids:Set<String>)=NovelAiHistoryAlbum(ids.map{imageId->
        val image=NovelAiGenerationHistoryImage(path=imageId)
        NovelAiHistoryImageItem(NovelAiGenerationHistoryEntry(id="org:$kind:$id:$imageId",images=listOf(image)),image,0)
    },label)
}
internal fun organizationFolders(g:GalleryViewState,albums:List<VirtualAlbum>,p:GalleryHistoryProjection):List<OrganizationFolder> = when(g.organizationKind){
    "batches"->p.batchOrigins.entries.groupBy{it.value.batchId}.map{(id,entries)->OrganizationFolder("batch",id,entries.first().value.batchName.ifBlank{"批次 ${id.take(8)}"},entries.map{it.key}.toSet())}
    "batch"->p.batchOrigins.filterValues{it.batchId==g.organizationId}.entries.groupBy{it.value.groupId}.map{(id,entries)->OrganizationFolder("group",id,entries.first().value.groupName,entries.map{it.key}.toSet())}
    "album","albums"->albums.filter{it.parent==(if(g.organizationKind=="album")g.organizationId else null)}.map{OrganizationFolder("album",it.id,it.name,VirtualAlbums.members(albums,it.id))}
    else->emptyList()
}
internal fun organizationOpen(c:WorkbenchController,f:OrganizationFolder,p:GalleryHistoryProjection){
    organizationNavigate(c,f.kind,f.id,f.label,f.ids)
    if(f.kind=="album"){
        val albums=c.library.state?.virtualAlbums.orEmpty();val path=mutableListOf<VirtualAlbum>();var a=albums.find{it.id==f.id}
        while(a!=null){path.add(0,a);a=albums.find{it.id==a?.parent}}
        c.gallery.levels=c.gallery.levels.take(1)+GalleryScope(albums.flatMap{it.images}.toSet(),"虚拟相册",identity="org:albums:")+path.map{GalleryScope(VirtualAlbums.members(albums,it.id),it.name,identity="org:album:${it.id}")}
    }else if(f.kind=="batch") {
        c.gallery.levels=c.gallery.levels.take(1)+GalleryScope(p.batchOrigins.keys,"批次",identity="org:batches:")+c.gallery.scope
    }else if(f.kind=="group")p.batchOrigins.values.firstOrNull{it.groupId==f.id}?.let{o->
        c.gallery.levels=c.gallery.levels.take(1)+GalleryScope(p.batchOrigins.keys,"批次",identity="org:batches:")+GalleryScope(p.batchOrigins.filterValues{it.batchId==o.batchId}.keys,o.batchName.ifBlank{"批次 ${o.batchId.take(8)}"},identity="org:batch:${o.batchId}")+c.gallery.scope
    }
}
