package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.example.chatbar.desktop.*
import com.example.chatbar.domain.image.*
import io.github.misakapiano.ccbstudio.core.*
import io.github.misakapiano.ccbstudio.assets.AssetStore
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*

private val Border get()=StudioAppearanceRuntime.palette.border
private val Soft get()=StudioAppearanceRuntime.palette.muted
private val TextInk get()=StudioAppearanceRuntime.palette.foreground

/** Layout measurements feed the unchanged Desktop native hit-test bridge. */
private class ChromeRecorder(private val chrome:DesktopWindowChrome) {
    private var width=0f;private var height=0f;private var title:ChromeRect?=null;private var icon:ChromeRect?=null
    private val buttons=linkedMapOf<String,ChromeRect>();private val captions=linkedMapOf<ChromeHit,ChromeRect>()
    private fun LayoutCoordinates.rect()=boundsInWindow().let {ChromeRect(it.left,it.top,it.right,it.bottom)}
    fun root(c:LayoutCoordinates){width=c.size.width.toFloat();height=c.size.height.toFloat();publish()}
    fun title(c:LayoutCoordinates){title=c.rect();publish()}
    fun icon(c:LayoutCoordinates){icon=c.rect();publish()}
    fun button(id:String):(LayoutCoordinates)->Unit={buttons[id]=it.rect();publish()}
    fun caption(hit:ChromeHit):(LayoutCoordinates)->Unit={captions[hit]=it.rect();publish()}
    private fun publish(){chrome.updateLayout(DesktopChromeLayout(width,height,title,buttons.values.toList(),captions.toMap(),icon))}
}

@Composable internal fun Studio(c:StudioController,closing:Boolean,frames:java.util.concurrent.atomic.AtomicLong,chrome:DesktopWindowChrome) {
    val recorder=remember(chrome){ChromeRecorder(chrome)}
    BoxWithConstraints(Modifier.fillMaxSize().onGloballyPositioned(recorder::root).drawWithContent {drawContent();frames.incrementAndGet()}.background(Ink).onKeyEvent {e->
        if(e.type==KeyEventType.KeyDown && e.isCtrlPressed && e.key==Key.Z){c.undo(e.isShiftPressed);true}
        else if(e.type==KeyEventType.KeyDown && e.isCtrlPressed && e.key==Key.Y){c.undo(true);true}else false
    }) {
        val compact=maxWidth<760.dp
        val wide=maxWidth>1120.dp
        Column(Modifier.fillMaxSize()) {
            StudioTitleBar(c,chrome,recorder,compact)
            Divider(color=Border)
            WorkspaceToolbar(c,compact)
            Divider(color=Border)
            Row(Modifier.weight(1f).fillMaxWidth()) {
                if(compact) {
                    when(c.compactPanel) {
                        "LIBRARY"->Library(c,Modifier.fillMaxSize())
                        "INSPECTOR"->Inspector(c,Modifier.fillMaxSize().background(Panel))
                        else->PreviewPane(c,Modifier.fillMaxSize())
                    }
                } else {
                    if(c.showLibrary){Library(c,Modifier.width(if(wide)224.dp else 188.dp).fillMaxHeight());Divider(Modifier.fillMaxHeight().width(1.dp),color=Border)}
                    PreviewPane(c,Modifier.weight(1f).fillMaxHeight())
                    Divider(Modifier.fillMaxHeight().width(1.dp),color=Border)
                    Inspector(c,Modifier.width(if(wide)344.dp else 304.dp).fillMaxHeight().background(Panel))
                }
            }
            Divider(color=Border)
            Row(Modifier.fillMaxWidth().heightIn(min=30.dp).padding(horizontal=14.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically) {
                if(c.busy||closing)CircularProgressIndicator(Modifier.size(12.dp),strokeWidth=2.dp)
                else Box(Modifier.size(6.dp).background(Gold,RoundedCornerShape(3.dp)))
                Spacer(Modifier.width(8.dp));Text(if(closing)"正在保存并安全退出…"else c.message,Modifier.weight(1f),fontSize=11.sp,color=Quiet,maxLines=1,overflow=TextOverflow.Ellipsis)
                if(!compact)Text("PREVIEW  ·  合成数据",fontSize=10.sp,color=Quiet)
            }
        }
    }
    c.popup?.let {content->
        if(content=="ACCOUNT")AccountDialog(c)
        else AlertDialog(onDismissRequest={c.popup=null},title={Text("Studio Suite")},text={SelectionContainer {Text(content,Modifier.heightIn(max=450.dp).verticalScroll(rememberScrollState()))}},confirmButton={TextButton(onClick={c.popup=null}){Text("关闭")}})
    }
}

@Composable private fun StudioTitleBar(c:StudioController,chrome:DesktopWindowChrome,r:ChromeRecorder,compact:Boolean) {
    val native by chrome.state.collectAsState()
    Row(Modifier.fillMaxWidth().height(44.dp).background(Panel).onGloballyPositioned(r::title),verticalAlignment=Alignment.CenterVertically) {
        Box(Modifier.width(42.dp).fillMaxHeight().onGloballyPositioned(r::icon),contentAlignment=Alignment.Center){Image(StudioBrand.painter,"CCB 蓝色 logo",Modifier.size(26.dp))}
        if(!compact){Text("Studio Suite",fontSize=14.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(end=16.dp));Text("PREVIEW",fontSize=8.sp,color=Quiet,modifier=Modifier.padding(end=18.dp))}
        listOf(Triple(0,"spark","生成"),Triple(1,"edit","编辑"),Triple(2,"history","记录")).forEach {(index,glyph,label)->
            Row(Modifier.height(32.dp).width(if(compact)36.dp else 76.dp).onGloballyPositioned(r.button(label))
                .clip(RoundedCornerShape(6.dp)).background(if(c.tab==index)Soft else Color.Transparent)
                .clickable(role=Role.Tab){c.tab=index;c.compactPanel="INSPECTOR"}.padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center) {
                Glyph(glyph,if(c.tab==index)SelectionInk else Quiet,Modifier.size(16.dp))
                if(!compact){Spacer(Modifier.width(7.dp));Text(label,fontSize=12.sp,color=if(c.tab==index)TextInk else Quiet)}
            }
        }
        Box(Modifier.onGloballyPositioned(r.button("accounts"))){IconAction("账户档案","account",!c.busy){c.popup="ACCOUNT"}}
        Spacer(Modifier.weight(1f))
        listOf(ChromeHit.MINIMIZE,ChromeHit.MAXIMIZE,ChromeHit.CLOSE).forEach {hit->
            StudioCaptionButton(hit,native,compact,Modifier.onGloballyPositioned(r.caption(hit)),chrome::command)
        }
    }
}

@Composable private fun WorkspaceToolbar(c:StudioController,compact:Boolean) {
    var menu by remember {mutableStateOf(false)};var exportMenu by remember {mutableStateOf(false)}
    Row(Modifier.fillMaxWidth().height(52.dp).padding(horizontal=10.dp),verticalAlignment=Alignment.CenterVertically) {
        IconAction("资料库","library"){if(compact)c.compactPanel="LIBRARY"else c.showLibrary=!c.showLibrary}
        if(compact)IconAction("画布预览","image"){c.compactPanel="CANVAS"}
        else Column(Modifier.weight(1f).padding(start=8.dp)) {
            Text(c.project?.name?:"未命名工作区",fontWeight=FontWeight.SemiBold,fontSize=13.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
            Text("${c.assets.size} 份原件  /  ${c.selected.size} 项已选",fontSize=10.sp,color=Quiet)
        }
        if(compact)Spacer(Modifier.weight(1f))
        IconAction("导入图片","import",!c.busy){c.pickImport()}
        IconAction("所选图片建立工程","plus",!c.busy&&c.current!=null){c.createProject();c.compactPanel="CANVAS"}
        if(!compact){Spacer(Modifier.width(8.dp));Divider(Modifier.height(20.dp).width(1.dp),color=Border);Spacer(Modifier.width(8.dp));IconAction("撤销 Ctrl+Z","undo",!c.busy&&c.project?.undo?.isNotEmpty()==true){c.undo()};IconAction("重做 Ctrl+Y","redo",!c.busy&&c.project?.redo?.isNotEmpty()==true){c.undo(true)}}
        Box {
            IconAction("备份与工作区","more",!c.busy){menu=true}
            StudioMenu(menu,{menu=false}) {
                StudioMenuItem(onClick={menu=false;c.run("备份图库与工程"){c.popup=c.workspace.backup().toString()}}){Text("备份图库与工程")}
                StudioMenuItem(onClick={menu=false;c.restoreBackup()}){Text("恢复备份到新目录")}
                StudioMenuItem(onClick={menu=false;c.popup=c.workspace.root.toString()}){Text("查看数据位置")}
            }
        }
        if(!compact)Box {
            Button(onClick={exportMenu=true},enabled=!c.busy&&c.project!=null,shape=RoundedCornerShape(7.dp),elevation=ButtonDefaults.elevation(0.dp,0.dp),contentPadding=PaddingValues(horizontal=14.dp,vertical=7.dp)) {Glyph("export",StudioAppearanceRuntime.palette.primaryForeground,Modifier.size(14.dp));Spacer(Modifier.width(7.dp));Text("导出",fontSize=12.sp)}
            StudioMenu(exportMenu,{exportMenu=false}){listOf("PRIVACY" to "隐私渲染 PNG","NORMAL" to "普通渲染 + Recipe","ORIGINAL" to "原始图片 · 保留字节").forEach {(mode,label)->StudioMenuItem(onClick={exportMenu=false;c.export(mode)}){Text(label)}}}
        }
    }
}

@Composable private fun PreviewPane(c:StudioController,modifier:Modifier) {
    Column(modifier.padding(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(bottom=10.dp),verticalAlignment=Alignment.CenterVertically) {
            Text(if(c.intermediate!=null)"生成中间结果"else"画布",color=Quiet,fontSize=11.sp)
            Spacer(Modifier.weight(1f))
            IconAction("缩小","minus"){c.zoom=(c.zoom-0.25f).coerceAtLeast(0.25f)}
            TextButton(onClick={c.zoom=1f},contentPadding=PaddingValues(4.dp)){Text(if(c.zoom==1f)"适应"else"${(c.zoom*100).toInt()}%",fontSize=10.sp)}
            IconAction("放大","plus"){c.zoom=(c.zoom+0.25f).coerceAtMost(4f)}
        }
        CanvasView(c,Modifier.weight(1f).fillMaxWidth())
        if(c.warnings.isNotBlank())Text(c.warnings,color=Gold,fontSize=11.sp)
        Row(Modifier.fillMaxWidth().padding(vertical=10.dp),horizontalArrangement=Arrangement.SpaceBetween) {
            Text("${c.page?.layers?.size?:0} 图层",fontSize=10.sp,color=Quiet)
            Text("原件保持完整 · 编辑单独保存",fontSize=10.sp,color=Quiet)
        }
        val pages=c.project?.current?.pages?.mapNotNull {p->c.assets.find {it.request().id().toString()==p.assetId}}?:c.assets
        if(pages.isNotEmpty())LazyRow(Modifier.fillMaxWidth().height(74.dp),horizontalArrangement=Arrangement.spacedBy(7.dp)) {
            items(pages,key={it.request().id().toString()}){a->
                val id=a.request().id().toString()
                Column(Modifier.width(64.dp).clip(RoundedCornerShape(6.dp)).border(1.dp,if(id==c.current)SelectionInk else Border,RoundedCornerShape(6.dp)).clickable{c.current=id;c.refreshImage()}.padding(3.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                    Thumbnail(c,a,Modifier.fillMaxWidth().height(48.dp));Text(a.request().name(),fontSize=9.sp,maxLines=1,overflow=TextOverflow.Ellipsis,color=Quiet)
                }
            }
        }
    }
}

@Composable private fun Library(c:StudioController,modifier:Modifier) {
    var query by remember {mutableStateOf("")}
    Column(modifier.background(Panel).padding(12.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically){Text("资料库",Modifier.weight(1f),fontWeight=FontWeight.SemiBold,fontSize=13.sp);TextButton(onClick={c.fixture()},enabled=!c.busy,contentPadding=PaddingValues(4.dp)){Text("+ 样例",fontSize=11.sp)}}
        Field("筛选名称",query,{query=it})
        if(c.state.projects.isNotEmpty()) {
            Text("工程",color=Quiet,fontSize=10.sp,modifier=Modifier.padding(top=14.dp,bottom=5.dp))
            Row(Modifier.horizontalScroll(rememberScrollState())) {c.state.projects.forEach {p->TextButton(onClick={c.projectId=p.id;c.current=p.current.pages.firstOrNull()?.assetId;c.selected=p.current.pages.map{it.assetId}.toSet();c.refreshImage()},contentPadding=PaddingValues(horizontal=7.dp)){Text(p.name,fontSize=11.sp,color=if(c.projectId==p.id)SelectionInk else Quiet)}}}
        }
        Row(Modifier.padding(top=12.dp,bottom=8.dp),verticalAlignment=Alignment.CenterVertically){Text("原始图片",Modifier.weight(1f),fontSize=10.sp,color=Quiet);Text("${c.assets.size}",fontSize=10.sp,color=Quiet)}
        val ordered=c.project?.current?.pages?.mapNotNull {p->c.assets.find {it.request().id().toString()==p.assetId}}?:c.assets
        LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)) {
            items(ordered.filter {it.request().name().contains(query,true)},key={it.request().id().toString()}) {a->
                val id=a.request().id().toString()
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if(id==c.current)Soft else Color.Transparent).clickable{c.select(id);c.compactPanel="CANVAS"}.padding(5.dp),verticalAlignment=Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).semantics {contentDescription="拖动排列 ${a.request().name()}"}.pointerInput(id,c.projectId,c.busy) {
                        var distance=0f
                        detectDragGestures(onDragStart={distance=0f},onDragCancel={distance=0f},onDragEnd={
                            if(!c.busy&&c.projectId!=null&&kotlin.math.abs(distance)>24.dp.toPx())c.reorder(id,if(distance>0)1 else -1)
                        },onDrag={change,amount->if(c.projectId!=null){change.consume();distance+=amount.y}})
                    }){Thumbnail(c,a,Modifier.fillMaxSize().clip(RoundedCornerShape(5.dp)))}
                    Column(Modifier.weight(1f).padding(horizontal=7.dp)){Text(a.request().name(),maxLines=1,overflow=TextOverflow.Ellipsis,fontSize=11.sp);Text("${a.size()/1024} KB · ${a.sha256().take(6)}",fontSize=9.sp,color=Quiet)}
                    Checkbox(id in c.selected,onCheckedChange={c.select(id)},modifier=Modifier.size(22.dp))
                }
            }
        }
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            TextButton(onClick={c.selected=ordered.map {it.request().id().toString()}.toSet()},contentPadding=PaddingValues(4.dp)){Text("全选",fontSize=11.sp)}
            Spacer(Modifier.weight(1f));IconAction("所选图片上移","up",!c.busy&&c.project!=null&&c.current!=null){c.reorder(c.current!!,-1)};IconAction("所选图片下移","down",!c.busy&&c.project!=null&&c.current!=null){c.reorder(c.current!!,1)}
        }
        Text("拖入 PNG / JPEG，保留原件字节",fontSize=9.sp,color=Quiet)
    }
}

private val thumbnailDispatcher=Dispatchers.IO.limitedParallelism(1)
@Composable private fun Thumbnail(c:StudioController,a:AssetStore.Asset,modifier:Modifier) {
    val thumb by produceState<ImageBitmap?>(null,a.sha256()) {
        value=withContext(thumbnailDispatcher) {
            runCatching {
                val source=EditorRenderer.decode(c.workspace.bytes(a.request().id().toString()));val scaled=java.awt.image.BufferedImage(80,80,java.awt.image.BufferedImage.TYPE_INT_ARGB)
                try {val g=scaled.createGraphics();try{val ratio=minOf(80.0/source.width,80.0/source.height);val w=(source.width*ratio).toInt();val h=(source.height*ratio).toInt();g.drawImage(source,(80-w)/2,(80-h)/2,w,h,null)}finally{g.dispose()};scaled.toComposeImageBitmap()} finally {source.flush();scaled.flush()}
            }.getOrNull()
        }
    }
    Box(modifier.background(Ink),contentAlignment=Alignment.Center){thumb?.let {Image(it,"缩略图",Modifier.fillMaxSize(),contentScale=ContentScale.Fit)}?:Glyph("image",Quiet,Modifier.size(18.dp))}
}

@Composable private fun CanvasView(c:StudioController,modifier:Modifier) {
    val bitmap=c.image
    BoxWithConstraints(modifier.clip(RoundedCornerShape(10.dp)).background(if(bitmap==null)StudioAppearanceRuntime.palette.input else Color(0xff090d12)).border(1.dp,Border,RoundedCornerShape(10.dp)).clipToBounds(),contentAlignment=Alignment.Center) {
        if(bitmap==null){Column(horizontalAlignment=Alignment.CenterHorizontally){Glyph("image",StudioAppearanceRuntime.palette.mutedForeground,Modifier.size(44.dp));Spacer(Modifier.height(18.dp));Text("让创作从这里开始",fontSize=18.sp,fontWeight=FontWeight.SemiBold);Spacer(Modifier.height(8.dp));Text("导入合成图片，或先试试内置样例",fontSize=12.sp,color=Quiet);Spacer(Modifier.height(22.dp));Row {Button(onClick={c.pickImport()},enabled=!c.busy,shape=RoundedCornerShape(7.dp)){Text("导入图片")};Spacer(Modifier.width(10.dp));OutlinedButton(onClick={c.fixture()},enabled=!c.busy,shape=RoundedCornerShape(7.dp)){Text("创建样例")}}};return@BoxWithConstraints}
        val ratio=minOf(maxWidth.value/bitmap.width,maxHeight.value/bitmap.height)*c.zoom
        var points by remember {mutableStateOf(listOf<Point>())};val density=androidx.compose.ui.platform.LocalDensity.current.density
        Image(bitmap,"当前图像",contentScale=ContentScale.Fit,modifier=Modifier.requiredSize((bitmap.width*ratio).dp,(bitmap.height*ratio).dp).pointerInput(c.tool,c.projectId,c.current,c.busy,ratio,c.tab) {
            detectDragGestures(onDragStart={p->points=listOf(Point(p.x/(density*ratio),p.y/(density*ratio)))},onDragEnd={
                if(c.tab==1&&!c.busy&&c.projectId!=null&&points.size>=2){val xs=points.map{it.x};val ys=points.map{it.y};val actual=if(c.tool=="POLYGON"&&points.size<3)points+Point(points.last().x,points.first().y)else points;c.addLayer(actual,xs.min(),ys.min(),xs.max()-xs.min(),ys.max()-ys.min())};points=emptyList()
            },onDrag={change,_->change.consume();points=points+Point(change.position.x/(density*ratio),change.position.y/(density*ratio))})
        })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable private fun IconAction(label:String,glyph:String,enabled:Boolean=true,action:()->Unit) {
    val interactions=remember {MutableInteractionSource()};val hovered by interactions.collectIsHoveredAsState()
    TooltipArea(tooltip={Surface(color=Soft,shape=RoundedCornerShape(5.dp)){Text(label,Modifier.padding(8.dp),fontSize=11.sp,color=TextInk)}}) {
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(6.dp)).background(if(hovered&&enabled)Soft else Color.Transparent).hoverable(interactions).semantics {contentDescription=label}.clickable(enabled=enabled,role=Role.Button,onClick=action),contentAlignment=Alignment.Center) {Glyph(glyph,if(enabled)Quiet else Quiet.copy(alpha=0.3f),Modifier.size(16.dp))}
    }
}

@Composable private fun Glyph(name:String,color:Color,modifier:Modifier) {
    Canvas(modifier) {
        val w=size.width;val h=size.height;val stroke=1.4.dp.toPx()
        fun line(x1:Float,y1:Float,x2:Float,y2:Float)=drawLine(color,Offset(w*x1,h*y1),Offset(w*x2,h*y2),stroke)
        fun rect(x:Float,y:Float,rw:Float,rh:Float)=drawRect(color,Offset(w*x,h*y),Size(w*rw,h*rh),style=Stroke(stroke))
        when(name) {
            "minus"->line(.12f,.5f,.88f,.5f)
            "plus"->{line(.12f,.5f,.88f,.5f);line(.5f,.12f,.5f,.88f)}
            "close"->{line(.15f,.15f,.85f,.85f);line(.85f,.15f,.15f,.85f)}
            "maximize"->rect(.14f,.14f,.72f,.72f)
            "restore"->{rect(.3f,.1f,.6f,.6f);drawRect(Panel,Offset(w*.1f,h*.3f),Size(w*.6f,h*.6f));rect(.1f,.3f,.6f,.6f)}
            "image"->{rect(.1f,.12f,.8f,.76f);line(.15f,.75f,.4f,.45f);line(.4f,.45f,.65f,.75f);line(.65f,.75f,.85f,.5f);drawCircle(color,w*.07f,Offset(w*.7f,h*.33f))}
            "library"->{rect(.1f,.12f,.8f,.76f);line(.36f,.12f,.36f,.88f)}
            "account"->{drawCircle(color,w*.18f,Offset(w*.5f,h*.28f),style=Stroke(stroke));drawArc(color,180f,180f,false,Offset(w*.15f,h*.55f),Size(w*.7f,h*.55f),style=Stroke(stroke))}
            "more"->{listOf(.2f,.5f,.8f).forEach {drawCircle(color,w*.06f,Offset(w*it,h*.5f))}}
            "edit"->{line(.2f,.8f,.8f,.2f);line(.3f,.9f,.9f,.3f);line(.2f,.8f,.3f,.9f);line(.8f,.2f,.9f,.3f)}
            "spark"->{line(.5f,.08f,.5f,.92f);line(.08f,.5f,.92f,.5f);line(.23f,.23f,.77f,.77f);line(.77f,.23f,.23f,.77f)}
            "import","export"->{rect(.12f,.6f,.76f,.3f);line(.5f,.12f,.5f,.7f);if(name=="import"){line(.3f,.5f,.5f,.7f);line(.7f,.5f,.5f,.7f)}else{line(.3f,.32f,.5f,.12f);line(.7f,.32f,.5f,.12f)}}
            "up","down"->{val y=if(name=="up").25f else .75f;line(.2f,1-y,.5f,y);line(.5f,y,.8f,1-y)}
            "undo","redo"->{
                fun xx(x:Float)=if(name=="undo")x else 1-x
                drawArc(color,215f,250f,false,Offset(w*.2f,h*.2f),Size(w*.6f,h*.6f),style=Stroke(stroke))
                line(xx(.12f),.17f,xx(.12f),.45f);line(xx(.12f),.45f,xx(.4f),.45f)
            }
            else->{drawCircle(color,w*.36f,Offset(w*.5f,h*.5f),style=Stroke(stroke));line(.5f,.25f,.5f,.5f);line(.5f,.5f,.7f,.6f)}
        }
    }
}

@Composable private fun Disclosure(title:String,summary:String="",openInitially:Boolean=false,content:@Composable ()->Unit) {
    var open by remember {mutableStateOf(openInitially)}
    Column(Modifier.fillMaxWidth().border(1.dp,Border,RoundedCornerShape(8.dp)).padding(10.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().clickable {open=!open},verticalAlignment=Alignment.CenterVertically){Text(title,Modifier.weight(1f),fontSize=12.sp,fontWeight=FontWeight.Medium);Text(if(open)"−"else"+",color=Quiet)}
        if(open)content()else if(summary.isNotBlank())Text(summary,fontSize=10.sp,color=Quiet,maxLines=1,overflow=TextOverflow.Ellipsis)
    }
}

@Composable private fun Inspector(c:StudioController,modifier:Modifier) {
    Column(modifier) {
        Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically){Text(listOf("生成工作台","图层与编辑","来源与记录")[c.tab],fontSize=14.sp,fontWeight=FontWeight.SemiBold)}
        Divider(color=Border)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            when(c.tab) {
                0->{
                    Text("设计与继续修改",fontWeight=FontWeight.Bold)
                    ProfilePicker("LLM",c.state.accounts,c.llmId){c.llmId=it}
                    Field("画面描述 / 修改要求",c.instruction,{c.instruction=it},3)
                    Row {Button(onClick={c.design(false)},enabled=!c.busy){Text("AI 设计")};Spacer(Modifier.width(8.dp));OutlinedButton(onClick={c.design(true)},enabled=!c.busy){Text("继续修改")}}
                    Divider();Field("正向 Prompt",c.prompt,{c.prompt=it},4);Disclosure("负向 Prompt",c.negative){Field("负向 Prompt",c.negative,{c.negative=it},4)}
                    Disclosure("角色与位置",if(c.characters=="[]")"未添加角色"else"保留完整角色字段"){Field("角色列表 JSON",c.characters,{c.characters=it},4)}
                    ProfilePicker("NAI",c.state.accounts,c.naiId){c.naiId=it}
                    Row {NovelAiImageModel.entries.forEach {m->TextButton(onClick={c.imageModel=m}){Text(if(m==NovelAiImageModel.V4_5_FULL)"V4.5"else"V5",color=if(c.imageModel==m)SelectionInk else Quiet)}}}
                    Row {Box(Modifier.weight(1f)){Field("宽",c.width,{c.width=it})};Spacer(Modifier.width(8.dp));Box(Modifier.weight(1f)){Field("高",c.height,{c.height=it})}}
                    Row {Box(Modifier.weight(1f)){Field("Steps",c.steps,{c.steps=it})};Spacer(Modifier.width(8.dp));Box(Modifier.weight(1f)){Field("Guidance",c.guidance,{c.guidance=it})}}
                    Field("固定 Seed",c.seed,{c.seed=it})
                    Button(onClick={c.generate()},enabled=!c.busy,modifier=Modifier.fillMaxWidth()){Text("生成合成结果")}
                    Text("当前界面固定使用 SYNTHETIC。真实限额验证由独立受控入口执行；不会因点击产生付费请求。",color=Quiet,fontSize=11.sp)
                }
                1->{
                    Text("非破坏图层",fontWeight=FontWeight.Bold)
                    Text("先建立工程；在图像上拖绘。动作应用于工程中已选图片。",fontSize=12.sp,color=Quiet)
                    Row {listOf("RECT" to "矩形","BRUSH" to "画笔","POLYGON" to "圈选","TEXT" to "文字").forEach {(id,label)->TextButton(onClick={c.tool=id},contentPadding=PaddingValues(5.dp)){Text(label,color=if(c.tool==id)SelectionInk else Quiet)}}}
                    if(c.tool=="TEXT") {Field("文字",c.text,{c.text=it},3);Field("字体",c.font,{c.font=it});Field("字号",c.fontSize,{c.fontSize=it});Row(verticalAlignment=Alignment.CenterVertically){Checkbox(c.vertical,{c.vertical=it});Text("竖排")}}
                    else Row(verticalAlignment=Alignment.CenterVertically){Checkbox(c.mosaic,{c.mosaic=it});Text(if(c.mosaic)"马赛克" else "纯色遮盖")}
                    OutlinedButton(onClick={c.addLayer(if(c.tool=="POLYGON")listOf(Point(20f,20f),Point(180f,20f),Point(100f,130f))else listOf(Point(20f,20f),Point(180f,130f)),20f,20f,160f,110f)},enabled=!c.busy&&c.project!=null){Text("添加示例图层")}
                    c.page?.layers?.forEachIndexed {i,l->Column {Row(verticalAlignment=Alignment.CenterVertically){Text("${i+1}. ${l.kind} ${if(l.kind=="TEXT")l.text.take(10)else""}",Modifier.weight(1f),fontSize=12.sp);TextButton(onClick={c.run("隐藏 / 显示图层"){c.workspace.edit(c.projectId!!){p->p.change(p.current.copy(pages=p.current.pages.map{page->if(page.assetId==c.current)page.copy(layers=page.layers.map {if(it.id==l.id)it.copy(visible=!it.visible)else it})else page}))}}}){Text(if(l.visible)"隐藏"else"显示")}}
                        Row {
                            if(l.kind=="TEXT")TextButton(onClick={c.run("更新文字图层"){c.workspace.edit(c.projectId!!){p->p.change(p.current.copy(pages=p.current.pages.map {page->if(page.assetId==c.current)page.copy(layers=page.layers.map {if(it.id==l.id)it.copy(text=c.text,font=c.font,fontSize=c.fontSize.toInt(),vertical=c.vertical)else it})else page}))}}}){Text("应用文字设置",fontSize=10.sp)}
                            TextButton(onClick={c.run("移除图层 · 可撤销"){c.workspace.edit(c.projectId!!){p->p.change(p.current.copy(pages=p.current.pages.map {page->if(page.assetId==c.current)page.copy(layers=page.layers.filter {it.id!=l.id})else page}))}}}){Text("移除",fontSize=10.sp)}
                        }
                    }}
                    Divider();Text("分类导出",fontWeight=FontWeight.Bold)
                    Button(onClick={c.export("PRIVACY")},enabled=!c.busy&&c.project!=null,modifier=Modifier.fillMaxWidth()){Text("隐私渲染 PNG")}
                    OutlinedButton(onClick={c.export("NORMAL")},enabled=!c.busy&&c.project!=null,modifier=Modifier.fillMaxWidth()){Text("普通渲染 + Recipe")}
                    OutlinedButton(onClick={c.export("ORIGINAL")},enabled=!c.busy&&c.project!=null,modifier=Modifier.fillMaxWidth()){Text("原始文件 · 保留字节")}
                    Text("遮盖效果请检查后分享。Preview 尚未通过正式更新与真实图库长期保全门槛。",fontSize=11.sp,color=Quiet)
                }
                2->{
                    Text("元数据与来源",fontWeight=FontWeight.Bold)
                    c.assets.firstOrNull {it.request().id().toString()==c.current}?.let {a->
                        SelectionContainer {Text("${a.request().name()}\n${a.size()} bytes\nSHA-256 ${a.sha256()}",fontSize=11.sp)}
                        OutlinedButton(onClick={c.popup=a.request().recipe().decodeToString().ifBlank {"导入原图没有 Suite Recipe；可查看原始 PNG metadata。"}}){Text("完整 Recipe")}
                        OutlinedButton(onClick={c.run("读取 PNG 元数据"){val p=c.workspace.assets.root().resolve("objects/${a.sha256()}.blob");c.popup=NovelAiPngMetadataReader.readStudio(p.toString())?.toString()?:"未发现可识别的 NAI metadata；原件未改动。"}}){Text("PNG Metadata")}
                    }
                    OutlinedButton(onClick={val recipes=c.assets.filter{it.request().id().toString() in c.selected}.map{it.request().recipe().decodeToString()};c.popup=if(recipes.size==2&&recipes.all{it.isNotBlank()})compareRecipes(recipes[0],recipes[1]).joinToString("\n\n").ifBlank{"两份完整 Recipe 一致"}else"请选择两张有完整 Recipe 的生成图。"}){Text("比较所选两份 Recipe")}
                    Divider();Text("设计历史 ${c.state.designs.size} · 任务 ${c.state.jobs.size}",color=Quiet)
                    c.state.designs.takeLast(10).reversed().forEach {d->TextButton(onClick={val p=documentJson.decodeFromString<NovelAiPromptPlan>(d.resultPlan);c.prompt=p.baseCaption;c.characters=documentJson.encodeToString(p.characterCaptions);c.tab=0}){Text(d.instruction.take(30),maxLines=2)}}
                    c.state.jobs.takeLast(10).reversed().forEach {j->Text("${j.state} · ${j.account.provider}\n${j.account.alias} / ${j.account.model}\n${j.results.size} 已保存",fontSize=11.sp,color=Quiet)}
                    Divider();Text("固定核心 ${AUTHORITY_SHA.take(12)}\n应用内安装更新：CP4 NOT TESTED\n当前版本仅供集中 Preview 验收",fontSize=11.sp,color=Quiet)
                    TextButton(onClick={c.popup=c.workspace.root.toString()}){Text("查看数据根路径")}
                }
            }
        }
    }
}

@Composable private fun Field(label:String,value:String,change:(String)->Unit,lines:Int=1) {
    OutlinedTextField(value,change,label={Text(label,fontSize=11.sp)},modifier=Modifier.fillMaxWidth(),singleLine=lines==1,minLines=lines,maxLines=if(lines==1)1 else lines+1,shape=RoundedCornerShape(7.dp),colors=TextFieldDefaults.outlinedTextFieldColors(textColor=TextInk,unfocusedBorderColor=Border,focusedBorderColor=Gold,backgroundColor=StudioAppearanceRuntime.palette.input),textStyle=LocalTextStyle.current.copy(fontSize=12.sp))
}
@Composable private fun ProfilePicker(service:String,accounts:List<AccountProfile>,selected:String,choose:(String)->Unit) {
    var expanded by remember {mutableStateOf(false)}
    Box {OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth()){Text(accounts.firstOrNull{it.id==selected}?.let{"${it.alias} · ${it.provider}"}?:service,fontSize=11.sp)}
        StudioMenu(expanded,{expanded=false}){accounts.filter{it.service==service}.forEach{a->StudioMenuItem(onClick={choose(a.id);expanded=false}){Text("${a.alias} · ${a.provider} / ${a.model}")}}}}
}
@Composable private fun AccountDialog(c:StudioController) {
    var alias by remember {mutableStateOf("")};var service by remember {mutableStateOf("LLM")}
    AlertDialog(onDismissRequest={c.popup=null},title={Text("命名账户档案")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
        Text("Preview 档案仅连接合成 transport。真实凭据验证独立限流，账户标识在任务创建时冻结。",fontSize=12.sp)
        c.state.accounts.forEach {Text("${it.alias} · ${it.service} · ${it.provider}",fontSize=12.sp)}
        Field("新档案名称",alias,{alias=it});Row {TextButton(onClick={service="LLM"}){Text("LLM",color=if(service=="LLM")SelectionInk else Quiet)};TextButton(onClick={service="NAI"}){Text("NAI",color=if(service=="NAI")SelectionInk else Quiet)}}
    }},confirmButton={Button(onClick={c.popup=null;c.run("保存命名档案"){c.workspace.save(c.state.copy(accounts=c.state.accounts+AccountProfile(alias=alias,service=service,provider="SYNTHETIC",model=if(service=="LLM")"fixture-designer" else "nai-diffusion-4-5-full"))) }},enabled=alias.isNotBlank()){Text("新增")}},dismissButton={TextButton(onClick={c.popup=null}){Text("关闭")}})
}
