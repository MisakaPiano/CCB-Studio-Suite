package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow

@Composable internal fun EditorRow(content:@Composable RowScope.()->Unit) {
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp),content=content)
}
@Composable internal fun EditorInput(label:String,value:String,error:Boolean=false,change:(String)->Unit) {
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(4.dp)){
        Text(label,style=MaterialTheme.typography.caption,maxLines=1)
        BasicTextField(value,change,singleLine=true,textStyle=TextStyle(color=MaterialTheme.colors.onSurface,fontSize=13.sp),cursorBrush=SolidColor(Gold),
            modifier=Modifier.fillMaxWidth().height(34.dp).border(1.dp,if(error)MaterialTheme.colors.error else MaterialTheme.colors.onSurface.copy(alpha=.25f),MaterialTheme.shapes.small)
                .background(Ink,MaterialTheme.shapes.small).padding(horizontal=8.dp,vertical=7.dp).semantics{contentDescription=label})
    }
}
@Composable internal fun EditorCommand(label:String,enabled:Boolean=true,icon:ImageVector?=null,selected:Boolean=false,modifier:Modifier=Modifier,destructive:Boolean=false,action:()->Unit) {
    OutlinedButton(onClick=action,enabled=enabled,modifier=modifier.defaultMinSize(minWidth=1.dp).height(32.dp),contentPadding=PaddingValues(horizontal=8.dp,vertical=3.dp),
        colors=ButtonDefaults.outlinedButtonColors(backgroundColor=if(selected)Gold.copy(alpha=.22f)else Panel,contentColor=MaterialTheme.colors.onSurface),
        border=BorderStroke(1.dp,if(selected)Gold else MaterialTheme.colors.onSurface.copy(alpha=.22f))){
        icon?.let{Icon(it,null,Modifier.size(15.dp),tint=if(destructive&&enabled)MaterialTheme.colors.error else LocalContentColor.current.copy(alpha=LocalContentAlpha.current));Spacer(Modifier.width(5.dp))};Text(label,fontSize=12.sp,maxLines=1)
    }
}

@Composable internal fun <T> EditorChoice(label:String,options:List<T>,selected:T?,text:(T)->String,choose:(T)->Unit) {
    var expanded by remember{mutableStateOf(false)}
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(4.dp)){
        Text(label,style=MaterialTheme.typography.caption)
        Box(Modifier.fillMaxWidth()){
            OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth().height(34.dp).semantics{contentDescription="$label · ${selected?.let(text)?:"选择"} ▾"},contentPadding=PaddingValues(horizontal=7.dp)){
                Text(selected?.let(text)?:"选择",Modifier.weight(1f),fontSize=12.sp,maxLines=1,overflow=TextOverflow.Ellipsis);Text("▾")
            }
            DropdownMenu(expanded,onDismissRequest={expanded=false},modifier=Modifier.heightIn(max=280.dp).widthIn(min=190.dp,max=320.dp)){
                options.forEach{option->DropdownMenuItem(onClick={choose(option);expanded=false},modifier=Modifier.height(30.dp)){Text(text(option),fontSize=12.sp,maxLines=1,overflow=TextOverflow.Ellipsis)}}
            }
        }
    }
}
