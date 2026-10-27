@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.*
import io.github.misakapiano.ccbstudio.core.*

@Composable internal fun TypesetLayerPanel(c:WorkbenchController,e:TypesetEditorController){
    val page=e.page?:return;val order=TypesetObjects.order(page).asReversed()
    val bounds=remember(page.id){mutableMapOf<String,Rect>()}
    var dragged by remember(page.id){mutableStateOf<String?>(null)}
    var target by remember(page.id){mutableStateOf<Int?>(null)}
    var pointerY by remember{mutableStateOf(0f)}
    var snapshot by remember{mutableStateOf(emptyList<String>())}
    Text("图层 · ${order.size} 个对象",style=MaterialTheme.typography.subtitle2)
    Text("拖动左侧手柄调整层序 · 顶部为最上层",style=MaterialTheme.typography.caption)
    if(order.isEmpty())Text("暂无图层。添加文字或绘制打码。")
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(4.dp)){
        order.forEachIndexed{index,id->
            val t=page.texts.firstOrNull{it.id==id};val m=page.masks.firstOrNull{it.id==id}
            val visible=t?.visible?:m!!.visible;val locked=t?.locked?:m!!.locked
            val name=if(t!=null)"文字 ${page.texts.indexOf(t)+1}"else"打码 ${page.masks.indexOf(m)+1}"
            if(dragged!=null&&target==index)Divider(color=Gold,thickness=2.dp)
            Row(Modifier.fillMaxWidth().height(42.dp).onGloballyPositioned{bounds[id]=it.boundsInRoot()}
                .background(if(e.objectId==id)Gold.copy(alpha=.22f)else Ink,MaterialTheme.shapes.small)
                .border(1.dp,if(e.objectId==id)Gold else MaterialTheme.colors.onSurface.copy(alpha=.12f),MaterialTheme.shapes.small),verticalAlignment=Alignment.CenterVertically){
                Icon(Lucide.GripVertical,null,Modifier.width(28.dp).fillMaxHeight().padding(5.dp)
                    .pointerHoverIcon(PointerIcon(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.MOVE_CURSOR)))
                    .semantics{contentDescription="拖动图层 $name"}
                    .pointerInput(page.id,id,c.busy,c.maintenance){detectDragGestures(
                        onDragStart={if(!c.busy&&!c.maintenance){dragged=id;snapshot=TypesetObjects.order(e.page!!).asReversed();pointerY=bounds[id]?.center?.y?:0f;target=index}},
                        onDragCancel={dragged=null;target=null},
                        onDragEnd={val destination=target
                            if(dragged==id&&destination!=null&&e.pageId==page.id&&TypesetObjects.order(e.page!!).asReversed()==snapshot){
                                val next=snapshot.filterNot{it==id}.toMutableList();next.add((destination-if(destination>snapshot.indexOf(id))1 else 0).coerceIn(0,next.size),id)
                                e.pageChange{it.copy(layerOrder=next.asReversed())}
                            };dragged=null;target=null},
                        onDrag={change,delta->if(dragged==id){change.consume();pointerY+=delta.y;target=snapshot.indexOfFirst{key->(bounds[key]?.center?.y?:Float.MAX_VALUE)>pointerY}.let{if(it<0)snapshot.size else it}}}
                    )})
                Box(Modifier.weight(1f).fillMaxHeight().clickable(enabled=!c.busy&&!c.maintenance){e.objectId=id}.semantics{contentDescription="图层行 $name"},contentAlignment=Alignment.CenterStart){
                    Text(name+if(locked)" · 已锁定"else"",maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.caption)
                }
                IconButton(onClick={e.pageChange{p->p.copy(texts=p.texts.map{if(it.id==id)it.copy(visible=!visible)else it},masks=p.masks.map{if(it.id==id)it.copy(visible=!visible)else it})}},enabled=!c.busy&&!c.maintenance,modifier=Modifier.size(30.dp)){
                    Icon(if(visible)Lucide.Eye else Lucide.EyeOff,(if(visible)"隐藏"else"显示")+"图层 $name",Modifier.size(15.dp))}
                IconButton(onClick={e.pageChange{TypesetObjects.lock(it,id,!locked)}},enabled=!c.busy&&!c.maintenance,modifier=Modifier.size(30.dp)){
                    Icon(if(locked)Lucide.Lock else Lucide.LockOpen,(if(locked)"解锁"else"锁定")+"图层 $name",Modifier.size(15.dp))}
            }
        }
        if(dragged!=null&&target==order.size)Divider(color=Gold,thickness=2.dp)
    }
    Divider(Modifier.padding(vertical=6.dp))
    val selected=e.text!=null||e.mask!=null;val locked=e.text?.locked?:e.mask?.locked?:false
    EditorRow{
        EditorCommand(if(locked)"解锁对象"else"锁定对象",selected&&!c.busy,if(locked)Lucide.LockOpen else Lucide.Lock,modifier=Modifier.weight(1f)){e.lockObject(!locked)}
        EditorCommand("复制对象",selected&&!c.busy,Lucide.Copy,modifier=Modifier.weight(1f)){e.copyObject()}
    }
    EditorRow{
        EditorCommand("上移图层",selected&&!c.busy,Lucide.ArrowUp,modifier=Modifier.weight(1f)){e.moveObject(1)}
        EditorCommand("下移图层",selected&&!c.busy,Lucide.ArrowDown,modifier=Modifier.weight(1f)){e.moveObject(-1)}
    }
}
