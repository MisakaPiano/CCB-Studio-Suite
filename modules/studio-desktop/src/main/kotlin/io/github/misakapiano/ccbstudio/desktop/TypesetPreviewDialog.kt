package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Sized image viewport; no AlertDialog text-baseline measurement of a large raster. */
@Composable internal fun TypesetPreviewDialog(title:String,dismiss:()->Unit,actions:@Composable RowScope.()->Unit,content:@Composable ()->Unit) {
    var maximized by remember{mutableStateOf(false)}
    Dialog(onDismissRequest=dismiss,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Box(Modifier.fillMaxSize().padding(if(maximized)0.dp else 16.dp),contentAlignment=Alignment.Center){
            Surface((if(maximized)Modifier.fillMaxSize()else Modifier.widthIn(max=1100.dp).fillMaxWidth().heightIn(max=820.dp).fillMaxHeight())
                .semantics{contentDescription="预览窗口";stateDescription=if(maximized)"已最大化"else"普通大小"},shape=MaterialTheme.shapes.medium){
                Column(Modifier.padding(if(maximized)12.dp else 20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        Text(title,Modifier.weight(1f),style=MaterialTheme.typography.h6,maxLines=2,overflow=TextOverflow.Ellipsis)
                        IconButton(onClick={maximized=!maximized},modifier=Modifier.size(32.dp)){
                            Icon(if(maximized)TypesetIcons.Restore else TypesetIcons.Maximize,if(maximized)"还原预览窗口"else"最大化预览窗口",Modifier.size(18.dp))
                        }
                        IconButton(onClick=dismiss,modifier=Modifier.size(32.dp)){Icon(TypesetIcons.Close,"关闭预览窗口",Modifier.size(18.dp))}
                    }
                    Divider()
                    Box(Modifier.weight(1f).fillMaxWidth().clipToBounds()){content()}
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End,verticalAlignment=Alignment.CenterVertically,content=actions)
                }
            }
        }
    }
}
