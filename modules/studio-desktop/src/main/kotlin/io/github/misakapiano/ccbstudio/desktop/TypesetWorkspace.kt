@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.*
import com.example.chatbar.desktop.*
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import java.awt.GraphicsEnvironment
import kotlin.math.*

@Composable internal fun TypesetWorkspace(c:WorkbenchController) {
    var picking by remember{mutableStateOf(false)}
    var appendPicking by remember{mutableStateOf(false)}
    val e=c.editor;var newName by remember{mutableStateOf("")};var creating by remember{mutableStateOf(false)}
    var projectAction by remember{mutableStateOf<String?>(null)};var projectName by remember{mutableStateOf("")};
    var deletePages by remember{mutableStateOf(false)};var tab by remember{mutableStateOf("画布")}
    var exporting by remember{mutableStateOf(false)};var fullPreview by remember{mutableStateOf(false)}
    if(picking)SharedAssetPicker(c,true,{picking=false}){ids->c.selection=ids.toSet();creating=true}
    if(appendPicking)SharedAssetPicker(c,true,{appendPicking=false}){ids->e.appendSelected(confirmedIds=ids)}
    if(fullPreview)TypesetFullPreview(c){fullPreview=false}
    Column(Modifier.fillMaxSize()) {
        StudioActions {
            CompactChoice("编辑工程",c.workspace.state.typesetProjects,e.project,{it.name}){e.open(it.id)}
            StudioAction("新建编辑工程",!c.busy){creating=true}
            StudioAction("从资产库新建",!c.busy){picking=true}
            if(e.project!=null)StudioAction("从资产库追加",!c.busy){appendPicking=true}
            StudioAction("导入 TypeSet 工程",!c.busy){e.inspectExchange()}
            if(e.project!=null){StudioAction("重命名工程",!c.busy){projectName=e.project!!.name;projectAction="rename"};StudioAction("另存工程",!c.busy){projectName=e.project!!.name+" 副本";projectAction="copy"};StudioAction("删除工程",!c.busy){projectAction="remove"}}
            StudioAction("保存工程",e.dirty && !c.busy){c.run("保存工程"){e.drain()}}
            StudioAction("撤销编辑",!c.busy && (e.dirty || e.project?.undo?.isNotEmpty()==true)){e.history()}
            StudioAction("重做编辑",!c.busy && e.project?.redo?.isNotEmpty()==true){e.history(true)}
            StudioAction("完整预览",!c.busy && e.document?.pages?.isNotEmpty()==true){fullPreview=true}
            StudioAction("预览与导出",!c.busy && e.document?.pages?.isNotEmpty()==true){exporting=true}
            StudioAction("导入编辑 Recipe",!c.busy && e.page!=null){e.inspectRecipe()}
            StatusText(if(e.dirty)"编辑待保存"else"编辑已保存")
        }
        if(e.document!=null)Text("静态排版：GIF / 动态WebP编辑首帧，APNG编辑默认静态图，动画原件始终保留。",modifier=Modifier.padding(horizontal=12.dp))
        e.exportDirectory?.let{Text("导出已完成并校验：$it",modifier=Modifier.padding(horizontal=12.dp))}
        e.exportProblem?.let{Text(it,color=MaterialTheme.colors.error,modifier=Modifier.padding(horizontal=12.dp))}
        e.exchangeMessage?.let{Text(it,modifier=Modifier.padding(horizontal=12.dp))}
        if(e.project!=null)StudioDisclosure("工程交换文件") {
            Text("包含原始图片和可编辑内容。TypeSet不支持的锁定与混合层序保存在Studio扩展中；外部TypeSet按其原有先打码后文字规则显示。字体文件不自动再分发。")
            StudioActions {StudioAction("导出当前 TypeSet 工程",!c.busy){e.exportExchange()};StudioAction("另存导入原文件",!c.busy && e.project?.sourceExchangeId!=null){e.exportExchange(true)}}
        }
        if(e.document==null) {
            Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Column(horizontalAlignment=Alignment.CenterHorizontally){Text("把图片编成可再次编辑的工程");Text("从图库选择图片，命名后建立队列；原件保持完整。")
                StudioAction("前往图库选择图片"){c.view="图库"};if(c.workspace.state.typesetProjects.isNotEmpty())StudioAction("继续最近工程"){e.open(c.workspace.state.typesetProjects.last().id)}}}
        }else {
            StudioActions {StudioChips("工作模式",listOf("editor","storybook"),e.document!!.mode,{if(it=="editor")"图片编辑"else"小人书"}){mode->e.change{it.copy(mode=mode)}}
                StudioChips("工具",listOf("select","rect","brush","lasso","pan"),e.tool,{when(it){"select"->"选择 / 移动";"rect"->"矩形";"brush"->"画笔";"lasso"->"圈选";else->"平移"}}){e.tool=it}
                StudioAction("−"){e.zoom=(e.zoom/1.25f).coerceAtLeast(.25f)};StudioAction("适应"){e.zoom=1f};StudioAction("＋"){e.zoom=(e.zoom*1.25f).coerceAtMost(8f)}
            }
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                // Keep a useful canvas even when a source preset requests a wide queue.
                val compact=maxWidth<maxOf(1000.dp,(e.document!!.queueWidth+300+400).dp)
                Column {
                    if(compact)StudioChips("",listOf("队列","画布","属性"),tab,{it}){tab=it}
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        if(!compact || tab=="队列")Column(if(compact)Modifier.fillMaxSize()else Modifier.width(e.document!!.queueWidth.dp).fillMaxHeight()) {
                            TypesetQueue(c,e,Modifier.weight(1f))
                            StudioDisclosure("队列设置") {
                                NumberField("开始编号",e.document!!.startNumber.toDouble(),0.0..1000000.0){v->e.change{it.copy(startNumber=v.toInt())}}
                                NumberField("编号位数",e.document!!.numberDigits.toDouble(),1.0..8.0){v->e.change{it.copy(numberDigits=v.toInt())}}
                                NumberField("缩略图尺寸",e.document!!.thumbnailSize.toDouble(),32.0..220.0){v->e.change{it.copy(thumbnailSize=v.toInt())}}
                                NumberField("队列栏宽度",e.document!!.queueWidth.toDouble(),180.0..720.0){v->e.change{it.copy(queueWidth=v.toInt())}}
                                StudioAction("记住新工程队列尺寸",!c.busy){e.rememberQueueDefaults()}
                            }
                            StudioActions{StudioAction("全选页"){e.selectedPages=e.document!!.pages.map{it.id}.toSet()};StudioAction("全不选页"){e.selectedPages=emptySet()};StudioAction("反选页"){e.selectedPages=e.document!!.pages.map{it.id}.toSet()-e.selectedPages}}
                            StudioActions{StudioAction("上移页"){e.movePages(-1)};StudioAction("下移页"){e.movePages(1)};StudioAction("移除所选页",e.selectedPages.isNotEmpty()){deletePages=true}}
                            StudioActions{StudioAction("置顶页"){e.movePagesTo(null,false)};StudioAction("置底页"){e.movePagesTo(null,true)};StudioAction("追加图库所选",c.selection.isNotEmpty() && !c.busy){e.appendSelected()}}
                            StatusText("${e.selectedPages.size} 页已选 / ${e.document!!.pages.size} 页")
                        }
                        if(!compact || tab=="画布")Box(Modifier.weight(1f).fillMaxHeight().background(Ink)){TypesetCanvas(c,e)}
                        if(!compact || tab=="属性")Column((if(compact)Modifier.fillMaxSize()else Modifier.width(300.dp).fillMaxHeight()).background(Panel).padding(12.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            if(e.document!!.mode=="storybook")StoryProperties(c,e)else TypesetProperties(c,e)
                        }
                    }
                }
            }
        }
    }
    if(exporting)TypesetExportDialog(c,e){exporting=false}
    e.recipeInspection?.let{r->AlertDialog(onDismissRequest={e.cancelRecipe()},title={Text("应用编辑 Recipe 到当前页？")},text={Column{
        Text("${r.page.texts.size} 个文字对象 / ${r.page.masks.size} 个遮罩；包含小人书旁白及样式。")
        Text("将替换当前页编辑内容，可撤销。只有原图SHA-256一致时才能应用；原图与源请求元数据保持不变。")
        Text("原图 SHA-256：${r.originalSha256}")
    }},confirmButton={TextButton(enabled=!c.busy,onClick={e.applyRecipe()}){Text("确认应用编辑 Recipe")}},dismissButton={TextButton(onClick={e.cancelRecipe()}){Text("取消 Recipe 导入")}})}
    e.exchangeInspection?.let{inspection->AlertDialog(onDismissRequest={e.cancelExchange()},title={Text("确认导入 TypeSet 工程")},text={Column{
        Text("${inspection.name} · ${inspection.document.pages.size} 页 · ${inspection.bytes.size} bytes")
        Text("将新建独立工程，不替换当前工程或图片。")
        inspection.notes.forEach{Text(it)}
    }},confirmButton={TextButton(enabled=!c.busy,onClick={e.importExchange()}){Text("建立独立导入工程")}},dismissButton={TextButton(onClick={e.cancelExchange()}){Text("取消导入")}})}
    if(projectAction!=null)AlertDialog(onDismissRequest={projectAction=null},title={Text(when(projectAction){"rename"->"重命名工程";"copy"->"另存为独立工程";else->"删除工程记录？"})},text={Column{
        if(projectAction=="remove")Text("只移除此工程记录；图库原件与已保存资产保留。删除后不能通过工程撤销恢复，请先备份或另存。")
        else {OutlinedTextField(projectName,{projectName=it.take(100)},label={Text("新工程名称")});if(projectAction=="copy")Text("保留当前页面与图层，新工程有独立历史；后续修改互不影响。")}
    }},confirmButton={TextButton(enabled=projectAction=="remove" || projectName.isNotBlank(),onClick={e.manageProject(projectAction!!,projectName);projectAction=null}){Text("确认工程操作")}},dismissButton={TextButton(onClick={projectAction=null}){Text("取消")}})
    if(creating)AlertDialog(onDismissRequest={creating=false},title={Text("新建编辑工程")},text={Column{OutlinedTextField(newName,{newName=it.take(100)},label={Text("工程名称")});Text("将图库选中的 ${c.selection.size} 张图片加入独立队列；编辑不覆盖原件。")}},confirmButton={TextButton(enabled=newName.isNotBlank() && c.selection.isNotEmpty(),onClick={e.create(newName,c.assets.filter{it.request().id().toString() in c.selection}.map{it.request().id().toString()});creating=false;newName=""}){Text("建立工程")}},dismissButton={TextButton(onClick={creating=false}){Text("取消")}})
    e.staticChoice?.let{choice->AlertDialog(onDismissRequest=e::cancelStaticChoice,title={Text("动画图片的静态编辑确认")},
        text={Text("所选 ${choice.ids.size} 张含动画格式。GIF/动态WebP仅编辑首帧；APNG使用默认静态图，伪装APNG的默认图可能只是封面。需要真实内容时先返回图库使用APNG还原。原始动画文件保留，编辑导出不会保留动画。")},
        confirmButton={TextButton(enabled=!c.busy,onClick=e::confirmStaticChoice){Text("同意仅编辑静态图")}},
        dismissButton={TextButton(onClick=e::cancelStaticChoice){Text("取消，保留动画")}})}
    if(deletePages)AlertDialog(onDismissRequest={deletePages=false},title={Text("从工程移除 ${e.selectedPages.size} 页？")},text={Text("图库原件保留，可撤销恢复页面与编辑内容。")},confirmButton={TextButton(onClick={e.removePages();deletePages=false}){Text("移除页面")}},dismissButton={TextButton(onClick={deletePages=false}){Text("取消")}})
}

@Composable private fun TypesetQueue(c:WorkbenchController,e:TypesetEditorController,modifier:Modifier) {
    val list=rememberLazyListState();var insertion by remember{mutableStateOf<String?>(null)}
    LazyColumn(modifier,state=list){itemsIndexed(e.document!!.pages,key={_,p->p.id}){index,page->
        var dragY by remember(page.id){mutableStateOf(0f)}
        Row(Modifier.fillMaxWidth().background(if(page.id==e.pageId)Color(0xff253c55)else Color.Transparent).padding(6.dp),verticalAlignment=Alignment.CenterVertically) {
            Checkbox(page.id in e.selectedPages,{e.selectPage(page.id,extend=true)})
            Row(Modifier.weight(1f).semantics{contentDescription="队列页 ${index+1}";onClick{e.selectPage(page.id);true}}
                .onPointerEvent(PointerEventType.Press){event->if(event.button==PointerButton.Primary && !c.busy)if(page.id in e.selectedPages && !event.keyboardModifiers.isCtrlPressed && !event.keyboardModifiers.isShiftPressed){e.pageId=page.id;e.objectId=null}else e.selectPage(page.id,event.keyboardModifiers.isCtrlPressed,event.keyboardModifiers.isShiftPressed)}
                .pointerInput(page.id,c.busy){detectDragGestures(onDragStart={pos->dragY=(list.layoutInfo.visibleItemsInfo.firstOrNull{it.key==page.id}?.offset?:0)+pos.y},
                    onDragCancel={insertion=null},onDragEnd={if(!c.busy)e.movePagesTo(insertion,insertion==null);insertion=null},
                    onDrag={change,amount->if(!c.busy){change.consume();dragY+=amount.y;insertion=(list.layoutInfo.visibleItemsInfo.firstOrNull{dragY<it.offset+it.size/2}?.key as? String)?:e.document!!.pages.getOrNull((list.layoutInfo.visibleItemsInfo.lastOrNull()?.index?:-1)+1)?.id}})},verticalAlignment=Alignment.CenterVertically){
                DesktopOwnedImage("asset:${page.assetId}",c::readAsset,Modifier.size(e.document!!.thumbnailSize.dp))
                Column {if(insertion==page.id)Text("── 插入此页之前 ──",color=Gold);Text("${io.github.misakapiano.ccbstudio.core.TypesetQueue.number(e.document!!,index)} · ${page.name}",maxLines=2);StatusText("${page.texts.size} 文字 · ${page.masks.size} 打码")}
            }
        }
    }}
}

private data class EditorRaster(val bitmap:ImageBitmap,val width:Int,val height:Int,val page:TypesetPage,val mode:String?)
@Composable private fun TypesetCanvas(c:WorkbenchController,e:TypesetEditorController) {
    val page=e.page;val mode=e.document?.mode;var error by remember{mutableStateOf<String?>(null)}
    var draftMask by remember(page?.id){mutableStateOf<TypesetMask?>(null)};var dragText by remember(page?.id){mutableStateOf<TypesetText?>(null)}
    var dragMask by remember(page?.id){mutableStateOf<TypesetMask?>(null)}
    val previewPage=page?.copy(texts=page.texts.map{if(it.id==dragText?.id)dragText!!else it},
        masks=page.masks.map{if(it.id==dragMask?.id)dragMask!!else it}+listOfNotNull(draftMask?.takeIf{it.shape!="lasso" || it.points.size>=3}))
    val raster by produceState<EditorRaster?>(null,previewPage,mode,e.document?.startNumber) {
        error=null
        try{value=withContext(Dispatchers.IO){previewPage?.let{p->val image=typesetPreviewRaster(c.workspace,p,mode=="storybook",e.document!!.startNumber+e.document!!.pages.indexOf(p))
            try{EditorRaster(image.toComposeImageBitmap(),image.width,image.height,p,mode)}finally{image.flush()}}}}
        catch(e:CancellationException){throw e}catch(ex:Exception){error="页面暂无法绘制：${ex.message}";value=null}
    }
    var pan by remember(page?.id){mutableStateOf(Offset.Zero)};var frame by remember{mutableStateOf(IntSize.Zero)}
    var origin by remember{mutableStateOf(Offset.Zero)}
    var resize by remember{mutableStateOf(false)}
    val picture=raster
    if(picture==null){Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text(error?:if(page==null)"请选择页面"else"正在排版…")};return}
    val scale=min(frame.width.toFloat()/picture.width,frame.height.toFloat()/picture.height)*e.zoom
    val offset=Offset((frame.width-picture.width*scale)/2,(frame.height-picture.height*scale)/2)+pan
    val currentOffset by rememberUpdatedState(offset)
    fun toImage(point:Offset)=(point-currentOffset)/max(.0001f,scale)
    fun hit(point:Offset):String? {
        val p=page?:return null
        return TypesetObjects.order(p).asReversed().firstOrNull{id->
            val t=p.texts.firstOrNull{it.id==id};val m=p.masks.firstOrNull{it.id==id}
            if(t!=null){val layout=TypesetLayout.layout(t);!t.locked && t.visible && point.x>=t.x-layout.width/2 && point.x<=t.x+layout.width/2 && point.y>=t.y && point.y<=t.y+layout.height}
            else m!=null && !m.locked && m.visible && TypesetRenderer.maskShape(m).contains(point.x.toDouble(),point.y.toDouble())
        }
    }
    Column(Modifier.fillMaxSize()) {
        StudioActions {StatusText("${picture.width} × ${picture.height} · ${e.page?.name.orEmpty()}");if(picture.page!=previewPage || picture.mode!=mode)StatusText("排版中…");if(mode=="editor")StudioAction("添加文字",!c.busy){e.addText(picture.width,picture.height)}}
        Canvas(Modifier.weight(1f).fillMaxWidth().clipToBounds().semantics{contentDescription="排版编辑画布";stateDescription=if(picture.page==previewPage && picture.mode==mode)"已更新"else"排版中"}.onSizeChanged{frame=it}
            .pointerInput(page?.id,mode,e.tool,scale,offset,page?.texts,page?.masks,c.busy){detectTapGestures{if(!c.busy && mode=="editor" && e.tool=="select")e.objectId=hit(toImage(it))}}
            .pointerInput(page,mode,e.tool,scale,c.busy){detectDragGestures(
                onDragStart={pos->if(!c.busy){origin=toImage(pos);resize=false
                    if(mode=="editor" && e.tool=="select") {val selected=e.text
                        if(selected!=null && !selected.locked){val l=TypesetLayout.layout(selected);val handle=if(selected.vertical)Offset(selected.x.toFloat(),(selected.y+l.height).toFloat())else Offset((selected.x+l.width/2).toFloat(),(selected.y+l.height/2).toFloat());resize=(origin-handle).getDistance()<14/max(scale,.01f)}
                        if(!resize)e.objectId=hit(origin)
                        dragText=e.text?.takeUnless{it.locked};dragMask=e.mask?.takeUnless{it.locked}
                    }else if(mode=="editor" && e.tool in setOf("rect","brush","lasso"))draftMask=e.maskDefault.copy(id=newId(),shape=e.tool,x=origin.x.toDouble(),y=origin.y.toDouble(),points=listOf(TypesetPoint(origin.x.toDouble(),origin.y.toDouble())))
                }},onDragCancel={draftMask=null;dragText=null;dragMask=null},onDragEnd={
                    draftMask?.let{m->if(m.shape!="lasso"||m.points.size>=3){e.pageChange{it.copy(masks=it.masks+m)};e.objectId=m.id}};draftMask=null
                    dragText?.let{t->e.textChange{t}};dragText=null
                    dragMask?.let{m->e.pageChange{it.copy(masks=it.masks.map{if(it.id==m.id)m else it})}};dragMask=null
                },onDrag={change,amount->if(!c.busy){change.consume();val p=toImage(change.position)
                    if(e.tool=="pan" || mode=="storybook")pan+=amount
                    else if(draftMask!=null){val m=draftMask!!;draftMask=if(m.shape=="rect")m.copy(x=min(origin.x,p.x).toDouble(),y=min(origin.y,p.y).toDouble(),width=max(1f,abs(p.x-origin.x)).toDouble(),height=max(1f,abs(p.y-origin.y)).toDouble())else m.copy(points=(m.points+TypesetPoint(p.x.toDouble(),p.y.toDouble())).take(10000))}
                    else if(dragText!=null){val t=dragText!!;dragText=if(resize)t.copy(width=max(20.0,if(t.vertical)p.y-t.y else 2*abs(p.x-t.x)))else t.copy(x=t.x+amount.x/scale,y=t.y+amount.y/scale)}
                    else if(dragMask!=null){val m=dragMask!!;dragMask=m.copy(x=m.x+amount.x/scale,y=m.y+amount.y/scale,points=m.points.map{it.copy(x=it.x+amount.x/scale,y=it.y+amount.y/scale)})}
                }})}) {
            drawImage(picture.bitmap,dstOffset=IntOffset(offset.x.roundToInt(),offset.y.roundToInt()),dstSize=IntSize(max(1,(picture.width*scale).roundToInt()),max(1,(picture.height*scale).roundToInt())))
            if(mode=="editor") {
                (dragText?:e.text)?.let{t->val l=TypesetLayout.layout(t);val top=Offset((t.x-l.width/2).toFloat(),t.y.toFloat())*scale+offset;val box=Size((l.width*scale).toFloat(),(l.height*scale).toFloat());drawRect(Gold,top,box,style=Stroke(2f))
                    val handle=if(t.vertical)Offset(top.x+box.width/2,top.y+box.height)else Offset(top.x+box.width,top.y+box.height/2);drawCircle(Gold,5f,handle)}
                (draftMask?:dragMask?:e.mask)?.let{m->val b=TypesetRenderer.maskShape(m).bounds2D;drawRect(Gold,Offset(b.x.toFloat(),b.y.toFloat())*scale+offset,Size((b.width*scale).toFloat(),(b.height*scale).toFloat()),style=Stroke(2f))
                    if(m.points.size>1){for(i in 1 until m.points.size)drawLine(Gold,Offset(m.points[i-1].x.toFloat(),m.points[i-1].y.toFloat())*scale+offset,Offset(m.points[i].x.toFloat(),m.points[i].y.toFloat())*scale+offset,2f)}}
            }
        }
    }
}

@Composable private fun TypesetProperties(c:WorkbenchController,e:TypesetEditorController) {
    val page=e.page?:return
    Text("对象与属性")
    TypesetPresets(c,e)
    StudioActions {TypesetObjects.order(page).asReversed().forEach{id->
        val t=page.texts.firstOrNull{it.id==id};val m=page.masks.firstOrNull{it.id==id}
        val label=if(t!=null)"文字 ${page.texts.indexOf(t)+1}"else"打码 ${page.masks.indexOf(m)+1}"
        StudioAction(label+(if(t?.locked==true || m?.locked==true)" · 已锁定"else""),selected=e.objectId==id){e.objectId=id}
    }}
    if(e.text!=null || e.mask!=null)StudioActions {
        val locked=e.text?.locked?:e.mask!!.locked
        StudioAction(if(locked)"解锁对象"else"锁定对象"){e.lockObject(!locked)}
        StudioAction("复制对象"){e.copyObject()};StudioAction("上移图层"){e.moveObject(1)};StudioAction("下移图层"){e.moveObject(-1)}
    }
    e.text?.let{t->
        var input by remember(t.id){mutableStateOf(TextFieldValue(t.text))}
        LaunchedEffect(t.text){if(input.text!=t.text)input=TextFieldValue(t.text,TextRange(input.selection.start.coerceAtMost(t.text.length)))}
        Text("文字内容")
        OutlinedTextField(input,{input=it;e.textChange{t->t.copy(text=it.text)}},modifier=Modifier.fillMaxWidth().heightIn(min=120.dp).semantics{contentDescription="文字内容"})
        fun wrap(tag:String){val a=input.selection.min;val z=input.selection.max;val text=input.text.substring(0,a)+"[$tag]"+input.text.substring(a,z)+"[/]"+input.text.substring(z);input=TextFieldValue(text,TextRange(a+tag.length+2,z+tag.length+2));e.textChange{it.copy(text=text)}}
        StudioActions{StudioAction("选区粗体"){wrap("b")};StudioAction("选区斜体"){wrap("i")};StudioAction("选区下划线"){wrap("u")};StudioAction("选区删除线"){wrap("strike")}}
        StudioSwitch("竖排",t.vertical){e.textChange{it.copy(vertical=!it.vertical)}}
        val fonts=remember{GraphicsEnvironment.getLocalGraphicsEnvironment().availableFontFamilyNames.toList()}
        CompactChoice("字体",(c.state.typesetFonts.map{it.alias}+listOf(t.style.fontFamily)+fonts).distinct(),t.style.fontFamily,{key->c.state.typesetFonts.firstOrNull{it.alias==key}?.let{"${it.name} · 已导入"}?:key}){value->e.textChange{it.copy(style=it.style.copy(fontFamily=value))}}
        StudioAction("导入字体 TTF / OTF / WOFF / WOFF2",!c.busy){e.importFont()}
        StudioAction("选区使用当前字体"){wrap("font=${t.style.fontFamily}")}
        NumberField("字号",t.style.fontSize,6.0..1000.0){v->e.textChange{it.copy(style=it.style.copy(fontSize=v))}}
        StudioAction("选区使用当前字号"){wrap("size=${t.style.fontSize}")}
        NumberField(if(t.vertical)"竖排框高度"else"文本框宽度",t.width,20.0..100000.0){v->e.textChange{it.copy(width=v)}}
        NumberField("行距",t.lineHeight,.2..10.0){v->e.textChange{it.copy(lineHeight=v)}}
        NumberField("字距",t.letterSpacing,-100.0..1000.0){v->e.textChange{it.copy(letterSpacing=v)}}
        CompactChoice("对齐",listOf("left","center","right"),t.align,{when(it){"left"->"左对齐";"right"->"右对齐";else->"居中"}}){v->e.textChange{it.copy(align=v)}}
        CompactChoice("字重",listOf(100,200,300,400,500,600,700,800,900),t.style.fontWeight,{it.toString()}){v->e.textChange{it.copy(style=it.style.copy(fontWeight=v))}}
        StudioSwitch("斜体",t.style.italic){e.textChange{it.copy(style=it.style.copy(italic=!t.style.italic))}}
        StudioSwitch("下划线",t.style.underline){e.textChange{it.copy(style=it.style.copy(underline=!t.style.underline))}}
        StudioSwitch("删除线",t.style.strike){e.textChange{it.copy(style=it.style.copy(strike=!t.style.strike))}}
        ColorField("文字颜色",t.style.fill){v->e.textChange{it.copy(style=it.style.copy(fill=v))}}
        StudioAction("选区使用当前颜色"){wrap("color=${t.style.fill}")}
        ColorField("描边颜色",t.style.stroke){v->e.textChange{it.copy(style=it.style.copy(stroke=v))}}
        NumberField("描边宽度",t.style.strokeWidth,0.0..100.0){v->e.textChange{it.copy(style=it.style.copy(strokeWidth=v))}}
        NumberField("文字透明度",t.opacity,0.0..1.0){v->e.textChange{it.copy(opacity=v)}}
        ColorField("文字背景",t.background){v->e.textChange{it.copy(background=v)}}
        NumberField("背景透明度",t.backgroundOpacity,0.0..1.0){v->e.textChange{it.copy(backgroundOpacity=v)}}
        ColorField("文本框边框",t.border){v->e.textChange{it.copy(border=v)}}
        NumberField("边框宽度",t.borderWidth,0.0..100.0){v->e.textChange{it.copy(borderWidth=v)}}
        NumberField("边框透明度",t.borderOpacity,0.0..1.0){v->e.textChange{it.copy(borderOpacity=v)}}
        if(t.vertical){StudioSwitch("标点优化",t.punctuationOptimization){e.textChange{it.copy(punctuationOptimization=!it.punctuationOptimization)}};CompactChoice("标点紧凑度",listOf("standard","compact","loose"),t.punctuationTightness,{when(it){"compact"->"紧凑";"loose"->"宽松";else->"标准"}}){v->e.textChange{it.copy(punctuationTightness=v)}}}
        StudioAction(if(t.visible)"隐藏对象"else"显示对象"){e.textChange{it.copy(visible=!it.visible)}}
        StudioAction("移除对象"){e.removeObject()}
    }
    if(e.mask!=null || e.tool in setOf("rect","brush","lasso")) {
        val m=e.mask?:e.maskDefault
        Text(if(e.mask!=null)"所选打码属性"else"新打码参数")
        fun update(transform:(TypesetMask)->TypesetMask){
            e.maskDefault=transform(e.maskDefault)
            if(e.mask!=null)e.pageChange{p->p.copy(masks=p.masks.map{if(it.id==e.objectId)transform(it)else it})}
            e.pageChange{it.copy(toolDefaults=e.maskDefault)}
        }
        if(m.effect in setOf("solid","hatch"))Text("旧中间版效果已保留，请明确选择下列效果完成转换。")
        CompactChoice("打码效果",listOf("black","white","mosaic","blur"),m.effect,{when(it){"black"->"黑条 / 黑色";"white"->"白条 / 白色";"mosaic"->"马赛克";else->"模糊"}}){v->update{it.copy(effect=v)}}
        NumberField("效果强度",m.strength.toDouble(),1.0..256.0){v->update{it.copy(strength=v.toInt())}}
        if(m.shape=="brush" || e.tool=="brush") {
            NumberField("画笔宽度",m.size,1.0..2000.0){v->update{it.copy(size=v)}}
            NumberField("条纹密度",m.density,1.0..1000.0){v->update{it.copy(density=v)}}
            NumberField("条纹厚度",m.stripeThickness,1.0..1000.0){v->update{it.copy(stripeThickness=v)}}
            NumberField("笔点密度",m.pointDensity,1.0..1000.0){v->update{it.copy(pointDensity=v)}}
        }
        if(e.mask!=null){StudioAction(if(m.visible)"隐藏打码"else"显示打码"){e.pageChange{p->p.copy(masks=p.masks.map{if(it.id==m.id)it.copy(visible=!it.visible)else it})}};StudioAction("移除打码"){e.removeObject()}}
    }
    if(e.text==null)Text("在画布中点击文字或从对象列表选择。新增文字后输入，样式与方向即时更新。")
    StudioDisclosure("批次样式") {Text("当前文字样式应用到所有所选页面的文字；上一页样式按对象顺序匹配，多出的文字沿用上一页最后一项。保留文字和位置，可整体撤销。")
        StudioAction("当前样式应用到所选页",e.selectedPages.isNotEmpty()){e.applyStyleToSelection()};StudioAction("各自上一页样式",e.selectedPages.isNotEmpty()){e.applyStyleToSelection(true)}}
}

@Composable internal fun NumberField(label:String,value:Double,range:ClosedFloatingPointRange<Double>,change:(Double)->Unit) {
    var text by remember(value){mutableStateOf(if(value%1.0==0.0)value.toInt().toString()else value.toString())}
    val number=text.toDoubleOrNull();val valid=number!=null && number.isFinite() && number in range
    OutlinedTextField(text,{text=it;it.toDoubleOrNull()?.takeIf{n->n.isFinite()&&n in range}?.let(change)},label={Text(label)},isError=!valid,singleLine=true,modifier=Modifier.fillMaxWidth())
}
@Composable internal fun ColorField(label:String,value:String,change:(String)->Unit) {
    var text by remember(value){mutableStateOf(value)};val valid=Regex("#[0-9a-fA-F]{3}([0-9a-fA-F]{3})?").matches(text)
    OutlinedTextField(text,{text=it;if(Regex("#[0-9a-fA-F]{3}([0-9a-fA-F]{3})?").matches(it))change(it)},label={Text("$label (#RRGGBB)")},isError=!valid,singleLine=true,modifier=Modifier.fillMaxWidth())
}
@Composable private fun StoryProperties(c:WorkbenchController,e:TypesetEditorController) {
    val p=e.page?:return;val s=p.story
    Text("小人书 · 第 ${e.document!!.pages.indexOf(p)+e.document!!.startNumber} 页")
    StudioActions {TypesetBook.presets.forEach{(key,preset)->StudioAction(when(key){"traditional"->"传统小人书";"clean"->"清爽画册";else->"复古纸张"}){e.pageChange{it.copy(story=preset)}}}}
    OutlinedTextField(p.caption,{v->e.pageChange{it.copy(caption=v)}},label={Text("本页旁白")},modifier=Modifier.fillMaxWidth().heightIn(min=160.dp).onPreviewKeyEvent{if(it.type==KeyEventType.KeyDown && it.key==Key.Enter && it.isCtrlPressed){e.nextCaption();true}else false})
    StudioAction("下一页旁白 · Ctrl+Enter",TypesetBook.next(e.document!!,p.id)!=null){e.nextCaption()}
    StudioAction("记住新页面小人书样式",!c.busy){e.rememberStoryDefault()}
    fun style(change:(StoryStyle)->StoryStyle)=e.pageChange{it.copy(story=change(it.story))}
    NumberField("页面宽度",s.pageWidth.toDouble(),200.0..8000.0){v->style{it.copy(pageWidth=v.toInt())}}
    NumberField("页边距",s.pagePadding.toDouble(),0.0..2000.0){v->style{it.copy(pagePadding=v.toInt())}}
    NumberField("图文间距",s.gap.toDouble(),0.0..2000.0){v->style{it.copy(gap=v.toInt())}}
    NumberField("文字内边距",s.textPadding.toDouble(),0.0..2000.0){v->style{it.copy(textPadding=v.toInt())}}
    NumberField("文字区最小高度",s.textMinHeight.toDouble(),0.0..8000.0){v->style{it.copy(textMinHeight=v.toInt())}}
    NumberField("旁白字号",s.fontSize,6.0..500.0){v->style{it.copy(fontSize=v)}}
    NumberField("旁白行距",s.lineHeight,.2..10.0){v->style{it.copy(lineHeight=v)}}
    val fonts=remember{GraphicsEnvironment.getLocalGraphicsEnvironment().availableFontFamilyNames.toList()}
    CompactChoice("旁白字体",(c.state.typesetFonts.map{it.alias}+listOf(s.fontFamily)+fonts).distinct(),s.fontFamily,{key->c.state.typesetFonts.firstOrNull{it.alias==key}?.let{"${it.name} · 已导入"}?:key}){v->style{it.copy(fontFamily=v)}}
    StudioAction("导入字体 TTF / OTF / WOFF / WOFF2",!c.busy){e.importFont()}
    CompactChoice("旁白对齐",listOf("left","center","right"),s.textAlign,{it}){v->style{it.copy(textAlign=v)}}
    StudioSwitch("首行缩进",s.firstIndent){style{it.copy(firstIndent=!s.firstIndent)}}
    StudioSwitch("显示页码",s.showPageNumber){style{it.copy(showPageNumber=!s.showPageNumber)}}
    StudioSwitch("页面边框",s.pageBorder){style{it.copy(pageBorder=!s.pageBorder)}}
    StudioSwitch("图片边框",s.imageBorder){style{it.copy(imageBorder=!s.imageBorder)}}
    ColorField("纸张颜色",s.pageBackground){v->style{it.copy(pageBackground=v)}}
    ColorField("旁白颜色",s.textColor){v->style{it.copy(textColor=v)}}
    ColorField("页面描边",s.borderColor){v->style{it.copy(borderColor=v)}}
    StudioAction("样式应用到所选页",e.selectedPages.isNotEmpty()){e.change{d->d.copy(pages=d.pages.map{if(it.id in e.selectedPages)it.copy(story=s)else it})}}
    StudioAction("样式应用到全册"){e.change{d->d.copy(pages=d.pages.map{it.copy(story=s)})}}
}
