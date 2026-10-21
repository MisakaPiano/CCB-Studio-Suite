package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Sized image viewport; no AlertDialog text-baseline measurement of a large raster. */
@Composable internal fun TypesetPreviewDialog(title:String,dismiss:()->Unit,actions:@Composable RowScope.()->Unit,content:@Composable ()->Unit) {
    Dialog(onDismissRequest=dismiss,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Box(Modifier.fillMaxSize().padding(16.dp),contentAlignment=Alignment.Center){
            Surface(Modifier.widthIn(max=1100.dp).fillMaxWidth().heightIn(max=820.dp).fillMaxHeight(),shape=MaterialTheme.shapes.medium){
                Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                    Text(title,style=MaterialTheme.typography.h6)
                    Box(Modifier.weight(1f).fillMaxWidth().clipToBounds()){content()}
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End,verticalAlignment=Alignment.CenterVertically,content=actions)
                }
            }
        }
    }
}
