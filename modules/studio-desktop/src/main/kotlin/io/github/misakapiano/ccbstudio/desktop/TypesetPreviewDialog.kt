package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp

/** Sized image viewport; no AlertDialog text-baseline measurement of a large raster. */
@Composable internal fun TypesetPreviewDialog(title:String,dismiss:()->Unit,actions:@Composable RowScope.()->Unit,content:@Composable ()->Unit) {
    StudioImageWindow(title,dismiss){
                Column(Modifier.fillMaxSize().padding(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                    Box(Modifier.weight(1f).fillMaxWidth().clipToBounds()){content()}
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End,verticalAlignment=Alignment.CenterVertically,content=actions)
                }
    }
}
