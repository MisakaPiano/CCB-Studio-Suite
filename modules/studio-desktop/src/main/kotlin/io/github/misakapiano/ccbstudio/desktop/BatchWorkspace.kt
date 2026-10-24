@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class,androidx.compose.foundation.ExperimentalFoundationApi::class,androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.*
import com.composables.icons.lucide.Play
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import com.example.chatbar.desktop.*
import com.example.chatbar.domain.image.*
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*

internal fun batchStatus(s:BatchState)=when(s.mode){"ERROR"->if(s.error?.code=="429")"429 已暂停"else"异常暂停";"RUNNING"->if(s.pauseRequested)"完成后暂停"else"正在生成";"WAITING"->"间隔等待";"RESTING"->"定时休息";"DONE"->"全部完成";"ENDED"->"已结束";else->"待开始 / 已暂停"}

@Composable internal fun BatchCreationEntry(c:WorkbenchController) {
    val b=c.batch;var adjust by remember{mutableStateOf(false)}
    Column(Modifier.fillMaxWidth().border(1.dp,if(b.batchMode)Gold else LibraryLine,RoundedCornerShape(5.dp)).padding(5.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(3.dp)) {
            Text("生成方式",fontSize=12.sp,color=LibraryMuted)
            listOf("单次","连续生成","批量任务").forEachIndexed{i,label->
                val active=if(i==2)b.batchMode else !b.batchMode&&(c.draft.continuousModeEnabled==(i==1))
                Text(label,Modifier.background(if(active)Gold else Color.Transparent,RoundedCornerShape(4.dp)).clickable{b.batchMode=i==2;c.edit{it.copy(continuousModeEnabled=i==1)}}.padding(horizontal=8.dp,vertical=6.dp),fontSize=12.sp,color=if(active)Ink else LibraryText)
            }
            Spacer(Modifier.weight(1f))
            LibraryAction("工作台 ${b.state.groups.size}",icon=DesktopAppIcons.DetailsOpen,style=StudioActionStyle.GHOST){b.open()}
        }
        if(b.batchMode)BoxWithConstraints(Modifier.fillMaxWidth()) {
            @Composable fun rules(){Row(verticalAlignment=Alignment.CenterVertically){Text(b.directions.joinToString(" / "){"${it.label}${it.repeats}"}+" 次 · ${b.directions.sumOf{it.repeats}} 张",fontSize=12.sp,color=Gold);LibraryAction("调整",icon=DesktopAppIcons.Settings,style=StudioActionStyle.GHOST){adjust=true}}}
            @Composable fun add(){LibraryAction("当前参数加入批次",b.editing&&!c.busy&&c.draft.basePrompt.isNotBlank(),selected=true,icon=DesktopAppIcons.Add,accessibilityLabel="当前参数加入批次，仅保存快照，不启动生成"){b.addCurrent()}}
            if(maxWidth>=470.dp)Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){rules();Spacer(Modifier.weight(1f));add()}
            else FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){rules();add()}
        } else if(c.draft.continuousModeEnabled)Row(verticalAlignment=Alignment.CenterVertically){StatusText("目标图片数");StudioNumericInput("目标图片数",c.draft.continuousTargetCount.toString(),Modifier.width(90.dp)){it.toIntOrNull()?.takeIf{it>0}?.let{n->c.edit{it.copy(continuousTargetCount=n)}}}}
    }
    if(adjust)BatchDialog("横纵方规则",{adjust=false}) {
        var dirs by remember{mutableStateOf(b.directions)}
        BatchDirectionInputs(dirs){dirs=it}
        StudioAction("保存规则"){b.directions=dirs;adjust=false}
    }
}

@Composable private fun BatchDialog(title:String,onClose:()->Unit,content:@Composable ColumnScope.()->Unit) {
    Dialog(onDismissRequest=onClose){MaterialTheme(colors=darkColors(primary=Gold,background=LibraryBackground,surface=LibraryPanel,onSurface=LibraryText,onPrimary=Ink)){Surface(color=LibraryBackground,contentColor=LibraryText,shape=RoundedCornerShape(8.dp),border=androidx.compose.foundation.BorderStroke(1.dp,LibraryLine)){
        Column(Modifier.widthIn(min=480.dp,max=840.dp).heightIn(max=700.dp).padding(16.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(title,Modifier.weight(1f),fontSize=19.sp);LibraryAction("关闭",onClick=onClose)}
            Spacer(Modifier.height(12.dp));val scroll=rememberScrollState()
            Box(Modifier.weight(1f,false)){
                Column(Modifier.padding(end=12.dp).verticalScroll(scroll),verticalArrangement=Arrangement.spacedBy(10.dp),content=content)
                VerticalScrollbar(rememberScrollbarAdapter(scroll),Modifier.align(Alignment.CenterEnd).fillMaxHeight(),style=studioVisibleScrollbarStyle())
            }
        }
    }}}
}
@Composable private fun BatchDirectionInputs(directions:List<BatchDirection>,change:(List<BatchDirection>)->Unit) {
    directions.forEachIndexed{i,d->Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically){
        Text(d.label,Modifier.width(24.dp))
        StudioNumericInput("${d.label}宽",d.width.toString(),Modifier.width(92.dp)){raw->raw.toIntOrNull()?.let{v->change(directions.toMutableList().also{it[i]=d.copy(width=v)})}}
        Text("×")
        StudioNumericInput("${d.label}高",d.height.toString(),Modifier.width(92.dp)){raw->raw.toIntOrNull()?.let{v->change(directions.toMutableList().also{it[i]=d.copy(height=v)})}}
        StudioNumericInput("${d.label}次数",d.repeats.toString(),Modifier.width(68.dp)){raw->raw.toIntOrNull()?.takeIf{it in 0..8}?.let{v->change(directions.toMutableList().also{it[i]=d.copy(repeats=v)})}}
        Text("次 · 每次 1 张")
    }}
}
@Composable private fun BatchMenu(label:String,enabled:Boolean=true,items:List<Pair<String,()->Unit>>,icon:androidx.compose.ui.graphics.vector.ImageVector?=null) {
    var open by remember{mutableStateOf(false)}
    Box{LibraryAction("$label ▾",enabled,icon=icon){open=true};StudioMenu(open,{open=false}){items.forEach{(text,action)->StudioMenuItem({open=false;action()}){Text(text)}}}}
}

@Composable internal fun BatchWorkspace(c:WorkbenchController,detached:Boolean=false) {
    val b=c.batch;val s=b.state
    var settings by remember{mutableStateOf(false)};var edit by remember{mutableStateOf<BatchGroup?>(null)}
    var blank by remember{mutableStateOf(false)};var source by remember{mutableStateOf<String?>(null)}
    var contextAdd by remember{mutableStateOf(false)}
    var confirm by remember{mutableStateOf<String?>(null)};var logs by remember{mutableStateOf(false)}
    var now by remember{mutableStateOf(System.currentTimeMillis())}
    LaunchedEffect(s.mode,s.error){while(true){now=System.currentTimeMillis();delay(1000)}}
    val group=s.groups.firstOrNull{it.id==b.current}?:s.groups.firstOrNull()
    val addItems=listOf("当前创作参数" to {b.addCurrent()},"Prompt 资产" to {source="prompt"},"图片参数与来源" to {source="image"},"空白分组" to {blank=true})
    val canRemove=b.editing&&b.selected.isNotEmpty()&&s.groups.none{it.id in b.selected&&it.started}
    fun commands()=buildList<Pair<String,()->Unit>> {
        if(b.editing)add("添加分组…" to {contextAdd=true})
        if(b.canUndo)add("撤销 · Ctrl+Z" to {b.undo()})
        if(b.canRedo)add("重做 · Ctrl+Y / Ctrl+Shift+Z" to {b.redo()})
        if(group!=null)add("编辑参数 · Enter" to {edit=group})
        if(b.editing&&b.selected.isNotEmpty())add("复制所选 · Ctrl+D" to {b.duplicate()})
        if(canRemove)add("移出批次 · Delete" to {b.remove()})
        if(s.completed>0)add("本批结果" to {b.results()})
        add("全选 · Ctrl+A" to {b.selected=s.groups.map{it.id}.toSet()})
        add("清空选择 · Esc" to {b.selected=emptySet()})
    }
    LibraryPresentation {
    ContextMenuArea(items={commands().map{(label,action)->ContextMenuItem(label,action)}}) {
    Column(Modifier.fillMaxSize().semantics{contentDescription="批量生图工作台"}.onKeyEvent {event->
        if(event.type!=KeyEventType.KeyDown)false else when {
            event.isCtrlPressed&&event.key==Key.Z->{if(event.isShiftPressed)b.redo()else b.undo();true}
            event.isCtrlPressed&&event.key==Key.Y->{b.redo();true}
            event.isCtrlPressed&&event.key==Key.A->{b.selected=s.groups.map{it.id}.toSet();true}
            event.isCtrlPressed&&event.key==Key.D->{if(b.editing)b.duplicate();true}
            event.key==Key.Delete->{if(canRemove)b.remove();true}
            event.key==Key.Escape->{b.selected=emptySet();true}
            else->false
        }
    }) {
        LibraryBand {
            LibraryAction("返回创作"){b.shown=false;if(detached)b.detached=false}
            Text(s.name+(s.createdAt.takeIf{it>0}?.let{" · "+GenerationBatch(s.id,"auto",s.name,it).title.substringAfterLast(" · ")}.orEmpty()),maxLines=1)
            BatchMenu("批次管理",items=listOf("保存并归档…" to {c.generations.openArchive("auto",detached)},"历史批次…" to {c.generations.openHistory(detached)},"管理项目与章节…" to {c.generations.newFolder(detached=detached)}),icon=DesktopAppIcons.History)
            Text(c.state.accounts.firstOrNull{it.id==s.accountId}?.alias?:"未选账户",color=LibraryMuted)
            Spacer(Modifier.weight(1f))
            LibraryAction(if(detached)"返回主窗口"else"独立窗口"){b.detached=!detached;b.shown=detached}
            LibraryAction(if(s.running)"暂停"else if(s.groups.any{it.started})"继续"else"开始",!b.changing&&s.total>0&&s.mode !in setOf("ERROR","DONE","ENDED"),icon=if(s.running)DesktopAppIcons.Pause else com.composables.icons.lucide.Lucide.Play) {if(s.running)b.pause()else confirm="start"}
            LibraryAction("运行设置",icon=DesktopAppIcons.Settings){settings=true}
            LibraryAction("结束批次",s.total>0&&s.mode !in setOf("DONE","ENDED")&&!s.endRequested,icon=DesktopAppIcons.Stop){confirm="end"}
        }
        LibraryBand(true){
            Text(if(s.endRequested)"当前完成后结束"else batchStatus(s),color=Gold);Text("${s.completed} / ${s.total} 张")
            LinearProgressIndicator(if(s.total==0)0f else s.completed.toFloat()/s.total,Modifier.weight(1f).height(5.dp),color=Gold,backgroundColor=LibraryLine)
            val until=if(s.mode=="RESTING")s.restUntil else s.nextRequestAt
            Text(if(s.running && s.mode!="RUNNING")"${b.phase} · ${((until-now).coerceAtLeast(0)+999)/1000} 秒"else b.phase.ifBlank{"等待手动操作"},fontSize=12.sp,color=LibraryMuted)
            LibraryAction("记录"){logs=true}
        }
        s.error?.let{error->Row(Modifier.fillMaxWidth().background(Color(0xff34291d)).padding(horizontal=12.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)){Text(error.message,color=Color(0xffedbb7c));Text("${error.code}${if(error.notBefore>now)" · 冷却剩余 ${(error.notBefore-now+999)/1000} 秒"else" · 等待手动确认"}",fontSize=12.sp)}
            LibraryAction("确认重试",s.mode!="ENDED"&&now>=error.notBefore&&!b.changing){confirm="retry"}
            if(error.code!="429")LibraryAction("跳过并继续",s.mode!="ENDED"&&!b.changing){confirm="skip"}
            LibraryAction("结束批次",!b.changing){confirm="end"}
        }}
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val width=maxWidth
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.weight(1f)){
                    LibraryBand{Text("${s.groups.size} 组",Modifier.weight(1f));LibraryToggle("勾选模式",b.checkMode){b.checkMode=!b.checkMode};LibraryAction("全选"){b.selected=s.groups.map{it.id}.toSet()};LibraryAction("清空"){b.selected=emptySet()};BatchMenu("添加",b.editing,addItems)}
                    BatchGroupList(c,Modifier.weight(1f),{edit=it})
                    Row(Modifier.fillMaxWidth().padding(10.dp)){Text("已选 ${b.selected.size} 组",Modifier.weight(1f),color=LibraryMuted,fontSize=12.sp);Text("Ctrl / Shift 多选 · 拖动排序",color=LibraryMuted,fontSize=12.sp)}
                }
                val density=androidx.compose.ui.platform.LocalDensity.current
                Box(Modifier.width(8.dp).fillMaxHeight().background(LibraryLine).semantics{contentDescription="批次预览分隔线"}.pointerInput(width,density){val pixels=with(density){width.toPx()};detectDragGestures{change,drag->change.consume();b.detailFraction=(b.detailFraction-drag.x/pixels).coerceIn(.28f,.58f)}})
                Column(Modifier.width((width*b.detailFraction).coerceAtLeast(260.dp)).fillMaxHeight().padding(12.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically){Text(group?.name?:"尚无分组",Modifier.weight(1f),fontSize=16.sp,maxLines=1);LibraryAction("参数快照",group!=null){edit=group}}
                    Text(group?.let{"${it.draft.selectedModel.displayName} · ${it.draft.activeSettings.steps} Steps · ${it.slots.size} 张"}?:"从当前创作或其他来源添加",Modifier.padding(vertical=8.dp),fontSize=12.sp,color=LibraryMuted)
                    BatchResults(c,group,Modifier.weight(1f))
                }
            }
        }
        LibraryBand(true){
            val p=s.policy
            LibraryAction("间隔 ${p.intervalSeconds}–${p.intervalSeconds+p.intervalJitterSeconds} 秒"){settings=true}
            LibraryAction("运行 ${p.runMinutes} 分 · 休息 ${p.restMinutes}–${p.restMinutes+p.restJitterMinutes} 分"){settings=true}
            Spacer(Modifier.weight(1f));LibraryAction(if(p.anlasLimit==0)"仅免费 · 额度不足暂停"else"Anlas ${s.reservedAnlas} / ${p.anlasLimit}"){settings=true}
        }
        Row(Modifier.fillMaxWidth().padding(12.dp).horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically){
            var addOpen by remember{mutableStateOf(false)}
            Box{LibraryToolTile("添加分组","添加批次分组",DesktopAppIcons.Add,b.editing,false){addOpen=true};StudioMenu(addOpen,{addOpen=false}){addItems.forEach{(label,action)->StudioMenuItem({addOpen=false;action()}){Text(label)}}}}
            LibraryToolTile("编辑参数","编辑当前批次参数",DesktopAppIcons.Settings,group!=null&&!s.running,false){edit=group}
            LibraryToolTile("复制所选","复制所选分组",DesktopAppIcons.Copy,b.editing&&b.selected.isNotEmpty(),false){b.duplicate()}
            LibraryToolTile("移出批次","移出未开始的分组",DesktopAppIcons.Delete,b.editing&&b.selected.isNotEmpty()&&s.groups.none{it.id in b.selected&&it.started},false){b.remove()}
            LibraryToolTile("本批结果","查看本批次入库结果",DesktopAppIcons.Data,s.completed>0,false){b.results()}
            LibraryAction("撤销",b.canUndo,icon=DesktopAppIcons.Undo){b.undo()}
            LibraryAction("重做",b.canRedo,icon=DesktopAppIcons.Redo){b.redo()}
            BatchMenu("操作",items=commands())
            Spacer(Modifier.weight(1f));Text("参数快照已保留\n原草稿不受影响",color=LibraryMuted,fontSize=12.sp)
        }
    }
    }
    }
    if(settings)BatchSettings(c,onCancelWait={settings=false;confirm="cancel"}){settings=false}
    if(contextAdd)BatchDialog("添加分组",{contextAdd=false}){addItems.forEach{(label,action)->LibraryAction(label){contextAdd=false;action()}}}
    if(blank||edit!=null)BatchGroupEditor(c,edit){blank=false;edit=null}
    source?.let{kind->BatchSourcePicker(c,kind){source=null}}
    if(logs)BatchDialog("批次记录",{logs=false}){Text("${s.name} · ${s.completed} 完成 · ${s.skipped} 跳过 · ${s.total} 总计\n预计额度预留 ${s.reservedAnlas} Anlas（含未确认尝试）");s.groups.forEach{g->Text(g.name);g.slots.filter{it.attempts.isNotEmpty()||it.status=="SKIPPED"}.forEach{Text("${it.direction.label} ${it.round} · ${it.status} · Seed ${it.seed} · ${it.results.size} 张已入库",fontSize=12.sp)}};s.archives.forEach{a->Text("已归档 · ${a.name} · ${a.completed}/${a.total} 张；原件与请求来源仍在图库",color=LibraryMuted)}}
    confirm?.let{action->AlertDialog(onDismissRequest={confirm=null},title={Text(when(action){"retry"->"手动确认恢复批次";"skip"->"跳过当前并继续？";"cancel"->"立即中止请求？";"end"->"结束批次？";"new"->"归档并新建批次？";else->"开始批次？"})},text={Text(when(action){
        "retry"->if(s.error?.uncertain==true)"上次结果未确认，重试可能再次计费。已有结果不会删除。"else"将重新检查额度并重试；再次收到 429 会再次整批暂停。"
        "cancel"->"本地中止不代表服务端取消；结果将标为未确认，之后不会自动重发。"
        "end"->"已发送的请求继续接收并保存，之后不再发送下一条。结束后不能直接继续此批次；图片和记录保留，未完成分组可复制到新批次。"
        "new"->"归档当前队列和参数，建立空白批次。已入库结果及其来源保留；未完成项不会自动转入新批次。"
        "skip"->"当前项记为跳过，再继续剩余任务；不会删除已收到的原件。"
        else->"${s.groups.size} 组，共 ${s.total-s.completed-s.skipped} 次待请求，每次 1 张。间隔 ${s.policy.intervalSeconds}–${s.policy.intervalSeconds+s.policy.intervalJitterSeconds} 秒。${if(s.policy.anlasLimit==0)"仅免费/体力，预计需要扣费时暂停。"else"允许预计 Anlas 总上限 ${s.policy.anlasLimit}。"} 发生错误整批暂停，等待人工确认。"
    })},confirmButton={TextButton(onClick={when(action){"retry"->b.start(retry=true);"skip"->b.start(skip=true);"cancel"->b.cancel();"end"->b.end();"new"->b.newBatch();else->b.start()};confirm=null},enabled=!b.changing){Text("确认")}},dismissButton={TextButton({confirm=null}){Text("取消")}})}
}

@Composable private fun BatchGroupList(c:WorkbenchController,modifier:Modifier,onEdit:(BatchGroup)->Unit) {
    val b=c.batch;val groups=b.state.groups;val scroll=rememberLazyListState()
    val focus=remember{FocusRequester()}
    val bounds=remember{mutableMapOf<String,Rect>()};var root by remember{mutableStateOf(Offset.Zero)};var height by remember{mutableStateOf(0)}
    var dragged by remember{mutableStateOf(setOf<String>())};var point by remember{mutableStateOf(Offset.Zero)};var target by remember{mutableStateOf<String?>(null)}
    LaunchedEffect(dragged.isNotEmpty()) {while(dragged.isNotEmpty()){
        val delta=when{point.y<48f->-14f;point.y>height-48f->14f;else->0f}
        if(delta!=0f){scroll.scrollBy(delta);target=bounds.entries.firstOrNull{it.key !in dragged&&it.value.contains(point+root)}?.key}
        delay(16)
    }}
    Box(modifier.fillMaxWidth().semantics{contentDescription="批次分组列表"}.focusRequester(focus).onKeyEvent{event->
        if(event.type!=KeyEventType.KeyDown)false else when(event.key){
            Key.DirectionUp->{b.navigate(-1,event.isShiftPressed);true}
            Key.DirectionDown->{b.navigate(1,event.isShiftPressed);true}
            Key.MoveHome->{b.navigate(-groups.size,event.isShiftPressed);true}
            Key.MoveEnd->{b.navigate(groups.size,event.isShiftPressed);true}
            Key.Enter->{groups.firstOrNull{it.id==b.current}?.let(onEdit);true}
            else->false
        }
    }.focusable().onGloballyPositioned{root=it.positionInRoot();height=it.size.height}) {
        LaunchedEffect(b.current){groups.indexOfFirst{it.id==b.current}.takeIf{it>=0}?.let{scroll.animateScrollToItem(it)}}
        LazyColumn(Modifier.fillMaxSize().padding(8.dp),state=scroll,verticalArrangement=Arrangement.spacedBy(6.dp)){
            itemsIndexed(groups,key={_,g->g.id}){index,g->
                val selected=g.id in b.selected;var ctrl by remember{mutableStateOf(false)};var shift by remember{mutableStateOf(false)}
                DisposableEffect(g.id){onDispose{bounds.remove(g.id)}}
                Row(Modifier.fillMaxWidth().onGloballyPositioned{bounds[g.id]=it.boundsInRoot()}
                    .background(if(selected)LibrarySelected else LibraryPanel,RoundedCornerShape(6.dp))
                    .border(if(target==g.id)2.dp else 1.dp,if(selected||target==g.id)Gold else Color.Transparent,RoundedCornerShape(6.dp))
                    .semantics{contentDescription="批次分组 ${g.name}";this.selected=selected}
                    .onPointerEvent(PointerEventType.Press){ctrl=it.keyboardModifiers.isCtrlPressed;shift=it.keyboardModifiers.isShiftPressed;focus.requestFocus();if(it.buttons.isSecondaryPressed&&g.id !in b.selected)b.select(g.id)}
                    .pointerInput(g.id,b.editing,b.selected){if(b.editing)detectDragGestures(onDragStart={p->dragged=if(g.id in b.selected)b.selected else setOf(g.id);point=(bounds[g.id]?.topLeft?:Offset.Zero)+p-root},onDragCancel={dragged=emptySet();target=null},onDragEnd={target?.let{b.reorder(dragged,it)};dragged=emptySet();target=null}){change,delta->change.consume();point+=delta;target=bounds.entries.firstOrNull{it.key !in dragged&&it.value.contains(point+root)}?.key}}
                    .combinedClickable(onClick={b.select(g.id,ctrl,shift)},onDoubleClick={onEdit(g)}).padding(10.dp),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically){
                    if(b.checkMode)LibrarySelectionCheckbox(selected,{b.select(g.id,true)})else Text("%02d".format(index+1),color=LibraryMuted,modifier=Modifier.width(27.dp))
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)){
                        Text(g.name,maxLines=1,overflow=TextOverflow.Ellipsis)
                        FlowRow(horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(3.dp)) {g.slots.groupBy{it.direction.label}.forEach{(label,slots)->
                            val done=slots.count{it.status=="DONE"}
                            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) {Text("$label $done/${slots.size}",color=LibraryMuted,fontSize=12.sp);LinearProgressIndicator(done.toFloat()/slots.size,Modifier.width(26.dp).height(3.dp).semantics{contentDescription="${g.name} $label 进度"},color=Gold,backgroundColor=LibraryLine)}
                        }}
                    }
                    Column(horizontalAlignment=Alignment.End){Text("${g.slots.count{it.status=="DONE"}}/${g.slots.size}");Text(when{g.slots.any{it.status=="FAILED"}->"等待处理";g.slots.any{it.status=="RUNNING"}->"生成中";g.slots.all{it.status in setOf("DONE","SKIPPED")}->"已完成";else->"待继续"},color=if(g.slots.any{it.status=="FAILED"})Color(0xffedbb7c)else LibraryMuted,fontSize=12.sp)}
                }
            }
        }
        VerticalScrollbar(rememberScrollbarAdapter(scroll),Modifier.align(Alignment.CenterEnd).fillMaxHeight(),style=studioVisibleScrollbarStyle())
        if(groups.isEmpty())Text("添加当前创作、Prompt 资产或图片参数\n组成批次后手动开始",Modifier.align(Alignment.Center),color=LibraryMuted)
        if(dragged.isNotEmpty())Surface(Modifier.offset{IntOffset(point.x.toInt()+12,point.y.toInt()+12)}.width(180.dp),color=LibrarySelected,elevation=8.dp){Text("${dragged.size} 组 · 拖动排序",Modifier.padding(12.dp))}
    }
}

@Composable private fun BatchResults(c:WorkbenchController,group:BatchGroup?,modifier:Modifier) {
    val b=c.batch;val slots=group?.slots.orEmpty();var index by remember(group?.id){mutableStateOf(0)}
    val scroll=rememberScrollState();val completed=slots.count{it.status=="DONE"}
    LaunchedEffect(group?.id,completed,b.follow){if(b.follow){index=slots.indexOfLast{it.results.isNotEmpty()}.coerceAtLeast(0)}}
    fun move(delta:Int){if(slots.isNotEmpty()){b.follow=false;index=(index+delta).coerceIn(0,slots.lastIndex)}}
    val selected=slots.getOrNull(index.coerceAtMost((slots.size-1).coerceAtLeast(0)))
    fun status(slot:BatchSlot)=when(slot.status){"DONE"->"已完成";"RUNNING"->"生成中";"FAILED"->"等待处理";"SKIPPED"->"已跳过";else->"待生成"}
    BoxWithConstraints(modifier.onPreviewKeyEvent{if(it.type==KeyEventType.KeyDown&&(it.key==Key.DirectionLeft||it.key==Key.DirectionRight)){move(if(it.key==Key.DirectionLeft)-1 else 1);true}else false}.focusable()){
        val previewHeight=(maxHeight*.48f).coerceIn(160.dp,300.dp)
        Column(Modifier.fillMaxSize().padding(end=12.dp).verticalScroll(scroll)) {
        var hover by remember{mutableStateOf(false)}
        Box(Modifier.height(previewHeight).fillMaxWidth().background(LibraryPanel,RoundedCornerShape(5.dp)).onPointerEvent(PointerEventType.Enter){hover=true}.onPointerEvent(PointerEventType.Exit){hover=false}.clickable{move(1)}){
            val asset=selected?.results?.firstOrNull()
            if(asset==null)Text(if(selected==null)"选择分组查看任务"else status(selected),Modifier.align(Alignment.Center),color=LibraryMuted)else AssetImage(c,asset,Modifier.fillMaxSize())
            selected?.let{slot->Text("${slot.direction.label} ${slot.round} · ${slot.direction.width}×${slot.direction.height} · ${status(slot)}",Modifier.align(Alignment.BottomStart).padding(6.dp).background(LibraryBackground.copy(alpha=.9f),RoundedCornerShape(4.dp)).padding(5.dp),fontSize=12.sp)}
            if(hover&&slots.size>1){Box(Modifier.align(Alignment.CenterStart)){LibraryAction("‹",index>0){move(-1)}};Box(Modifier.align(Alignment.CenterEnd)){LibraryAction("›",index<slots.lastIndex){move(1)}}}
        }
        Row(Modifier.fillMaxWidth().padding(vertical=6.dp),verticalAlignment=Alignment.CenterVertically){Text("本组 $completed/${slots.size} 张",Modifier.weight(1f));LibraryToggle("跟随最新",b.follow){b.follow=!b.follow}}
        // The complete, bounded plan is visible before generation; thumbnails only decode
        // saved assets. Placeholders never masquerade as completed or copyable results.
        slots.withIndex().toList().chunked(4).forEach {row->
            Row(Modifier.fillMaxWidth().padding(bottom=5.dp),horizontalArrangement=Arrangement.spacedBy(5.dp)){
                row.forEach{(i,slot)->Column(Modifier.weight(1f).height(72.dp).border(if(i==index)2.dp else 1.dp,if(i==index)Gold else LibraryLine,RoundedCornerShape(4.dp))
                    .semantics{contentDescription="批次位置 ${slot.direction.label} ${slot.round}";stateDescription=status(slot);this.selected=i==index}
                    .clickable{b.follow=false;index=i}.padding(4.dp),horizontalAlignment=Alignment.CenterHorizontally){
                    Box(Modifier.weight(1f).fillMaxWidth().background(LibraryPanel)) {slot.results.firstOrNull()?.let{id->QueueStaticThumbnail(c,id,Modifier.fillMaxSize())}}
                    Text("${slot.direction.label} ${slot.round}",fontSize=11.sp,maxLines=1,color=LibraryMuted)
                }}
                repeat(4-row.size){Spacer(Modifier.weight(1f))}
            }
        }
        Text("参数快照 · 每次 1 张",Modifier.padding(vertical=6.dp),color=LibraryMuted,fontSize=12.sp)
        }
        VerticalScrollbar(rememberScrollbarAdapter(scroll),Modifier.align(Alignment.CenterEnd).fillMaxHeight(),style=studioVisibleScrollbarStyle())
    }
}

@Composable private fun BatchSettings(c:WorkbenchController,onCancelWait:()->Unit={},onClose:()->Unit) {
    val b=c.batch;var p by remember{mutableStateOf(b.state.policy)};var account by remember{mutableStateOf(b.state.accountId?:c.naiId)}
    BatchDialog("节奏、账户与额度",onClose){
        CompactChoice("NAI 账户",c.state.accounts.filter{it.service=="NAI"},c.state.accounts.firstOrNull{it.id==account},{it.alias}){account=it.id}
        fun number(label:String,v:Int,range:IntRange,change:(Int)->Unit):@Composable ()->Unit={Row(verticalAlignment=Alignment.CenterVertically){Text(label,Modifier.width(220.dp));StudioNumericInput(label,v.toString(),Modifier.width(100.dp)){raw->raw.toIntOrNull()?.takeIf{it in range}?.let(change)}}}
        number("请求最短间隔（秒）",p.intervalSeconds,10..3600){p=p.copy(intervalSeconds=it)}()
        number("额外随机等待（0～秒）",p.intervalJitterSeconds,0..600){p=p.copy(intervalJitterSeconds=it)}()
        number("每轮运行（分钟）",p.runMinutes,1..240){p=p.copy(runMinutes=it)}()
        number("休息最短时间（分钟）",p.restMinutes,1..120){p=p.copy(restMinutes=it)}()
        number("休息随机增加（0～分钟）",p.restJitterMinutes,0..60){p=p.copy(restJitterMinutes=it)}()
        LibraryToggle("轮询各组",p.roundRobin){p=p.copy(roundRobin=!p.roundRobin)}
        number("Anlas 总上限（0 = 仅免费）",p.anlasLimit,0..100000){p=p.copy(anlasLimit=it)}()
        Text("每次请求前确认额度；未知结果的预计消耗仍计入上限。等待期间不自动重试错误，429 必须手动确认。",color=LibraryMuted)
        LibraryAction("保存设置",!b.state.running&&account!=null){b.configure(account,p);onClose()};LibraryAction("异常处理：取消等待…",b.state.mode=="RUNNING",onClick=onCancelWait)
    }
}

@Composable private fun BatchGroupEditor(c:WorkbenchController,existing:BatchGroup?,onClose:()->Unit) {
    val b=c.batch;var draft by remember{mutableStateOf(existing?.draft?:c.draft.copy(basePrompt="",continuousModeEnabled=false))}
    var name by remember{mutableStateOf(existing?.name?:"分组 ${b.state.groups.size+1}")}
    var dirs by remember{mutableStateOf(existing?.slots?.map{it.direction}?.distinct()?:b.directions)}
    var shared by remember{mutableStateOf(false)};var error by remember{mutableStateOf<String?>(null)}
    val locked=existing?.started==true
    BatchDialog(if(locked)"参数快照 · 已开始，原组不可修改"else"编辑分组参数",onClose){
        OutlinedTextField(name,{name=it},label={Text("分组名称")},modifier=Modifier.fillMaxWidth())
        StudioGenerationModelChoice(draft){draft=it(draft)}
        StudioPromptField("画风",draft.stylePrompt,c.infrastructure,false){draft=draft.copy(stylePrompt=it)}
        StudioPromptField("基础 Prompt",draft.basePrompt,c.infrastructure,false){draft=draft.copy(basePrompt=it)}
        StudioPromptField("补充 Prompt",draft.extraPrompt,c.infrastructure,false){draft=draft.copy(extraPrompt=it)}
        StudioPromptField("负向 Prompt",draft.negativePrompt,c.infrastructure,false){draft=draft.copy(negativePrompt=it)}
        DesktopStudioCharacters(draft,c.infrastructure,false,{draft=it(draft)})
        StudioDisclosure("生成参数",initiallyOpen=false){StudioAdvancedSettings(draft.activeSettings){draft=draft.withActiveSettings(it(draft.activeSettings))}}
        BatchDirectionInputs(dirs){dirs=it};LibraryToggle("三种尺寸共用每轮 Seed",shared){shared=!shared}
        error?.let{Text(it,color=Color(0xffedbb7c))}
        LibraryAction(if(locked)"保存为新分组"else"保存分组",b.editing){runCatching{BatchGroup.create(name,draft,dirs,shared,existing?.designApplication)}.onSuccess{g->if(existing!=null&&!locked)b.replace(g.copy(id=existing.id))else b.addGroup(g);onClose()}.onFailure{error=it.message}}
    }
}

@Composable private fun BatchSourcePicker(c:WorkbenchController,kind:String,onClose:()->Unit) {
    val scope=rememberCoroutineScope();var items by remember{mutableStateOf(emptyList<Pair<String,String>>())};var search by remember{mutableStateOf("")};var error by remember{mutableStateOf<String?>(null)}
    LaunchedEffect(kind){items=withContext(Dispatchers.IO){if(kind=="prompt")c.library.repository.prompts().map{it.request().id().toString() to it.request().name()}else c.assets.map{it.request().id().toString() to it.request().name()}}}
    BatchDialog(if(kind=="prompt")"从 Prompt 资产添加"else"从图片生成参数添加",onClose){
        OutlinedTextField(search,{search=it},label={Text("查找名称")},modifier=Modifier.fillMaxWidth())
        error?.let{Text(it,color=Color(0xffedbb7c))}
        items.filter{it.second.contains(search,true)}.take(100).forEach{(id,name)->LibraryAction(name){scope.launch {
            try{val draft=withContext(Dispatchers.IO){if(kind=="prompt")NovelAiStudioPromptClipboard.apply(c.library.repository.prompt(id).text,c.draft)else {
                val history=c.historyRecipe(id)
                if(history!=null)c.draft.applyHistoryRecipe(history.first,history.second,NovelAiHistoryApplyMode.FULL)
                else c.metadataFile(id){path->
                    val metadata=DesktopImageMetadata.readStudio(path.toString())?:throw DataProblem("METADATA","这张图没有可复用的生成参数")
                    NovelAiStudioDraft().applyImportedMetadata(metadata,NovelAiStudioMetadataSelection())
                }
            }};c.batch.add(draft,name.take(100));onClose()}catch(e:Exception){error=e.message}
        }}}
        if(items.isEmpty())Text("没有可选来源",color=LibraryMuted)
    }
}
