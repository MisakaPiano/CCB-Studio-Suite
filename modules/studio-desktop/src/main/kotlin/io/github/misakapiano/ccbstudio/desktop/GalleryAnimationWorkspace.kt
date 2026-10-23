package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.chatbar.desktop.*
import kotlinx.coroutines.*

/** Every image surface in this tool uses this output-only menu, including source thumbnails. */
internal fun animationMenu(c:WorkbenchController,target:String?=c.animation.ids?.getOrNull(c.animation.index)):List<ContextMenuItem> {
    val a=c.animation
    val generation=a.outputGeneration
    fun current(action:()->Unit){if(target==a.ids?.getOrNull(a.index)&&generation==a.outputGeneration)action()}
    if(c.busy||c.maintenance)return emptyList()
    return buildList {
        if(target!=a.ids?.getOrNull(a.index)) {
            add(ContextMenuItem("选中此项"){a.ids?.indexOf(target)?.takeIf{it>=0}?.let(a::selectIndex)})
        }else if(target!=null) {
            if(a.outputReady) {
                add(ContextMenuItem("复制当前处理文件"){current{a.currentFile(true)}})
                add(ContextMenuItem("另存当前处理文件"){current{a.currentFile(false)}})
                add(ContextMenuItem("保存当前副本到图库"){current{a.saveCopy()}})
            }else add(ContextMenuItem("重新预览",a::requestPreview))
            add(ContextMenuItem("从本批移除（保留原件）"){current{a.removeCurrent()}})
        }
        add(ContextMenuItem("添加图片",a::addFiles));add(ContextMenuItem("添加目录（本层）",a::addDirectory))
        add(ContextMenuItem("粘贴图片"){a.paste()})
    }
}

internal fun animationNavigation(a:GalleryAnimationController,event:KeyEvent):Boolean {
    if(event.type!=KeyEventType.KeyDown||event.isCtrlPressed||event.isAltPressed||event.isMetaPressed)return false
    return when(event.key){Key.DirectionLeft->{a.page(-1);true};Key.DirectionRight->{a.page(1);true};else->false}
}

@Composable private fun AnimationActions(label:String,enabled:Boolean=true,items:List<ContextMenuItem>) {
    var open by remember{mutableStateOf(false)}
    Box {
        StudioAction(label,enabled&&items.isNotEmpty(),expanded=open,accessibilityLabel="$label ▾"){open=!open}
        DropdownMenu(open,{open=false}) {
            items.forEach{item->DropdownMenuItem(enabled=enabled,onClick={open=false;item.onClick()}){Text(item.label)}}
        }
    }
}

@Composable internal fun GalleryAnimationWorkspace(c:WorkbenchController) {
    val a=c.animation;val ids=a.ids?:return;val id=ids.getOrNull(a.index)
    val focus=remember{FocusRequester()}
    LaunchedEffect(Unit){focus.requestFocus()}
    val cover by produceState<Result<androidx.compose.ui.graphics.ImageBitmap>?>(null,a.preview){value=withContext(Dispatchers.IO){a.preview?.let{bytes->runCatching{val image=animationDefaultPreview(bytes);try{image.toComposeImageBitmap()}finally{image.flush()}}}}}
    var showSource by remember{mutableStateOf(true)}
    var information by remember{mutableStateOf(false)}
    val canProcess=ids.isNotEmpty()&&!c.busy&&(a.mode=="restore"||a.coverState!=null)
    Column(Modifier.fillMaxSize().galleryIngress(c,animation=true).focusRequester(focus)
        .onKeyEvent{if(it.type!=KeyEventType.KeyDown)false else when {
            it.isCtrlPressed&&it.key==Key.A->{a.selectQueue(true);true}
            it.isCtrlPressed&&it.key==Key.Z->{if(it.isShiftPressed)a.redo()else a.undo();true}
            it.isCtrlPressed&&it.key==Key.Y->{a.redo();true}
            it.isCtrlPressed&&it.key==Key.C->{if(a.queueSelection.isNotEmpty())a.export(true,selectedOnly=true)else if(a.outputReady)a.currentFile(true);true}
            it.key==Key.Escape->{a.selectQueue(false);true}
            it.key==Key.Delete&&a.queueSelection.isNotEmpty()->{a.removeQueue();true}
            else->animationNavigation(a,it)
        }}.focusable().padding(10.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
        StudioActions {
            StudioAction("返回图库",!c.busy){a.close()}
            StudioAction("生成伪装 APNG",!c.busy,selected=a.mode=="disguise"){a.selectMode("disguise")}
            StudioAction("还原伪装 APNG",!c.busy,selected=a.mode=="restore"){a.selectMode("restore")}
            if(a.mode=="disguise") {
                val selected=a.coverState?.covers?.firstOrNull{it.id==a.coverState?.selected}
                CoverThumbnail(c,selected,Modifier.size(32.dp).clickable{a.openCoverManager()})
                Text(selected?.name?:"CCB 默认封面",Modifier.widthIn(max=160.dp),maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.caption)
                StudioAction("更换封面",!c.busy,expanded=a.coverManagerOpen){a.openCoverManager()}
            }
            Box {
                StudioAction("说明",expanded=information){information=!information}
                if(information)Surface(color=Panel,border=BorderStroke(1.dp,DesktopBootstrapColors.border)) {
                    Text("原件保留。静止显示封面，主动播放才显示内容；伪装不是加密或隐私清除。\n静态图片 / GIF 可伪装；已有 APNG 先还原，动态 WebP 不静默转成静态。\n拖入或粘贴可追加本批；目录只导入本层，最多1000张。\n右键只复制处理后的文件，保留动画；不会复制来源原图。",Modifier.width(350.dp).padding(12.dp),style=MaterialTheme.typography.caption)
                }
            }
        }
        StudioActions {
            StudioAction("复制当前处理文件",!c.busy&&a.outputReady){a.currentFile(true)}
            StudioAction("复制全部处理文件",canProcess){a.export(true)}
            if(a.queueSelection.isNotEmpty())StudioAction("复制所选处理文件",canProcess){a.export(true,selectedOnly=true)}
            AnimationActions("添加",!c.busy,listOf(ContextMenuItem("添加图片",a::addFiles),ContextMenuItem("添加目录（本层）",a::addDirectory),ContextMenuItem("粘贴图片"){if(!a.paste())c.problem="剪贴板没有图片或文件列表"}))
            AnimationActions("当前结果",!c.busy&&ids.isNotEmpty(),animationMenu(c).filter{it.label in setOf("复制当前处理文件","另存当前处理文件","保存当前副本到图库","重新预览","从本批移除（保留原件）")})
            AnimationActions("整批处理",canProcess,listOf(ContextMenuItem("选择目录并导出全部"){a.export()},ContextMenuItem("复制全部处理文件"){a.export(true)}))
            StudioAction(if(showSource)"隐藏来源对照"else"显示来源对照"){showSource=!showSource}
            Text("${if(id==null)0 else a.index+1} / ${ids.size} · ← → 翻页 · 点击大图下一张 · 右键操作",style=MaterialTheme.typography.caption,color=Quiet)
            if(c.busy&&c.activeOperation=="导入图片")Text(c.gallery.importProgress,style=MaterialTheme.typography.caption)
            if(a.previewBusy){CircularProgressIndicator(Modifier.size(16.dp),strokeWidth=2.dp);StudioAction("停止预览"){a.cancelPreview()}}
            if(c.busy&&c.activeOperation in setOf("导入图片","保存APNG工具副本到图库","复制APNG文件批次","导出APNG文件批次","复制当前处理文件","另存当前处理文件"))StudioAction("停止当前处理"){c.active?.cancel()}
        }
        Row(Modifier.weight(1f).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            if(showSource)Column(Modifier.weight(1f).fillMaxHeight()) {
                Text("来源对照 · ${c.assets.firstOrNull{it.request().id().toString()==id}?.request()?.name()?:id}",maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.caption)
                ContextMenuArea(items={animationMenu(c)}){if(id!=null)AssetImage(c,id,Modifier.fillMaxSize().clickable{focus.requestFocus();a.page(1)})else Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text("拖入图片或目录，或使用「添加」\nCtrl+V 粘贴图片 / 文件列表",color=Quiet)}}
            }
            Column(Modifier.weight(1f).fillMaxHeight()) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
                    Text(if(a.mode=="disguise")"伪装后的封面预览"else"还原结果预览",style=MaterialTheme.typography.caption)
                    StudioSwitch("播放原内容（查看动画）",a.playResult){a.playback(!a.playResult)}
                }
                ContextMenuArea(items={animationMenu(c)}) {
                    AnimationImageRegion(a,Modifier.fillMaxSize(),onClick={focus.requestFocus();a.page(1)}) {
                        a.preview?.let{bytes->if(a.playResult)DesktopOwnedImage(io.github.misakapiano.ccbstudio.core.sha256(bytes),{bytes},Modifier.fillMaxSize())else cover?.onSuccess{Image(it,"处理结果静态封面",Modifier.fillMaxSize())}?.onFailure{Text("静态预览读取失败，未替换为原内容",color=MaterialTheme.colors.error)}}
                        if(a.preview==null)Text(if(a.previewBusy)"正在自动更新处理结果…"else"处理结果尚未就绪",Modifier.align(Alignment.Center),color=Quiet)
                    }
                }
            }
        }
        StudioActions {
            Text("队列 ${ids.size} 项 · 已选 ${a.queueSelection.size}",style=MaterialTheme.typography.caption)
            StudioSwitch("勾选模式",a.checkboxMode){a.checkboxMode=!a.checkboxMode}
            StudioAction("全选",!c.busy){a.selectQueue(true)};StudioAction("全不选",!c.busy){a.selectQueue(false)}
            StudioAction("反选",!c.busy){val before=a.queueSelection.toSet();a.selectQueue(false);ids.filter{it !in before}.forEach{a.toggleQueue(it)}}
            StudioAction("撤销",a.canUndo){a.undo()};StudioAction("重做",a.canRedo){a.redo()}
            StudioAction("移除所选（保留原件）",!c.busy&&a.queueSelection.isNotEmpty()){a.removeQueue()}
            StudioAction("导出所选处理文件",canProcess&&a.queueSelection.isNotEmpty()){a.export(selectedOnly=true)}
        }
        AnimationFilmstrip(c){focus.requestFocus()}
        a.coverError?.let{Text(it,color=MaterialTheme.colors.error);StudioAction("重新读取封面配置",!c.busy){a.reloadCovers()}}
        a.error?.let{Text(it,color=MaterialTheme.colors.error)}
        if(a.progress>0||a.completed!=null)Text("批次已写 ${a.progress} / ${a.progressTotal}"+(a.completed?.let{" · 已完成：$it"}?:" · 完成前请等待；已有输出保留"),maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.caption)
    }
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable private fun AnimationFilmstrip(c:WorkbenchController,onFocus:()->Unit) {
    val a=c.animation;val ids=a.ids.orEmpty()
    val selectedCover=a.coverState?.covers?.firstOrNull{it.id==a.coverState?.selected}
    val sharedCover by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null,selectedCover?.id){
        value=selectedCover?.let{try{QueueThumbnailLoader.load(c,it.id)}catch(e:CancellationException){throw e}catch(e:Exception){null}}
    }
    val scroll=rememberLazyListState();val scope=rememberCoroutineScope()
    LaunchedEffect(ids,a.index){if(ids.isNotEmpty()&&scroll.layoutInfo.visibleItemsInfo.none{it.index==a.index})scroll.scrollToItem(a.index)}
    Column(Modifier.fillMaxWidth().semantics{contentDescription="APNG 批次缩略图"}) {
        LazyRow(Modifier.fillMaxWidth().onPointerEvent(PointerEventType.Scroll){event->
            val delta=event.changes.sumOf{(it.scrollDelta.x+it.scrollDelta.y).toDouble()}.toFloat()
            if(delta!=0f){scope.launch{scroll.scrollBy(delta*48)};event.changes.forEach{it.consume()}}
        },state=scroll,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            itemsIndexed(ids,key={_,id->id}){index,id->ContextMenuArea(items={animationMenu(c,id)}) {
                Column(Modifier.width(132.dp).border(if(index==a.index)2.dp else 1.dp,if(index==a.index)Gold else if(id in a.queueSelection)androidx.compose.ui.graphics.Color(0xff39c9b1)else Quiet).padding(3.dp)) {
                    var range by remember{mutableStateOf(false)};var control by remember{mutableStateOf(false)}
                    Box(Modifier.fillMaxWidth().height(62.dp).onPointerEvent(PointerEventType.Press){range=it.keyboardModifiers.isShiftPressed;control=it.keyboardModifiers.isCtrlPressed}
                        .semantics{selected=index==a.index;contentDescription="选择结果缩略图 ${index+1}"}.clickable{onFocus();a.selectQueueItem(id,control,range)}) {
                        if(a.mode=="disguise"&&a.coverState!=null)Box(Modifier.fillMaxSize().background(Panel),contentAlignment=Alignment.Center) {
                            if(selectedCover==null)Image(StudioBrand.painter,"默认封面示意",Modifier.fillMaxSize(.4f))
                            else sharedCover?.let{Image(it,"自定义封面示意",Modifier.fillMaxSize())}?:Text("封面读取中",style=MaterialTheme.typography.caption)
                        }else QueueStaticThumbnail(c,id,Modifier.fillMaxSize())
                        if(id in a.queueSelection)Text("✓",Modifier.align(Alignment.TopEnd).background(Gold).padding(horizontal=4.dp),color=Ink)
                    }
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        if(a.checkboxMode)Checkbox(id in a.queueSelection,{a.toggleQueue(id,range)},enabled=!c.busy,colors=CheckboxDefaults.colors(checkedColor=Gold,checkmarkColor=Ink,uncheckedColor=Quiet),modifier=Modifier.size(24.dp).semantics{contentDescription="选择队列项目 $id"})
                        Text(c.assets.firstOrNull{it.request().id().toString()==id}?.request()?.name()?:"第 ${index+1} 项",maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.caption)
                    }
                    Text(a.itemStatus[id]?:if(a.mode=="disguise")"封面示意 · 待处理"else"待处理",maxLines=1,style=MaterialTheme.typography.caption,color=Quiet)
                }
            }}
        }
        HorizontalScrollbar(rememberScrollbarAdapter(scroll),Modifier.fillMaxWidth().height(10.dp).semantics{contentDescription="结果缩略图滚动条"},style=studioVisibleScrollbarStyle())
    }
}

@Composable private fun AnimationImageRegion(a:GalleryAnimationController,modifier:Modifier,onClick:()->Unit,content:@Composable BoxScope.()->Unit) {
    val interaction=remember{MutableInteractionSource()};val hovered by interaction.collectIsHoveredAsState()
    Box(modifier.hoverable(interaction).clickable(onClick=onClick).semantics{contentDescription="APNG处理结果区域"},contentAlignment=Alignment.Center) {
        content()
        if(hovered&&a.ids.orEmpty().size>1) {
            if(a.index>0)Box(Modifier.align(Alignment.CenterStart).background(Panel.copy(alpha=.9f))){StudioAction("上一张",accessibilityLabel="悬浮上一张",icon=DesktopAppIcons.Previous){a.page(-1)}}
            if(a.index<a.ids.orEmpty().lastIndex)Box(Modifier.align(Alignment.CenterEnd).background(Panel.copy(alpha=.9f))){StudioAction("下一张",accessibilityLabel="悬浮下一张",icon=DesktopAppIcons.Next){a.page(1)}}
        }
    }
}
