@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import io.github.misakapiano.ccbstudio.core.TypesetColors
import kotlin.math.roundToInt

internal data class EditorColor(val hue:Float,val saturation:Float,val brightness:Float,val alpha:Float){
    fun hex():String{val rgb=java.awt.Color.HSBtoRGB(hue,saturation,brightness) and 0xffffff;val a=(alpha*255).roundToInt().coerceIn(0,255)
        return "#"+rgb.toString(16).padStart(6,'0')+if(a<255)a.toString(16).padStart(2,'0')else""}
    companion object{fun parse(value:String):EditorColor{val c=TypesetColors.parse(value);val hsv=java.awt.Color.RGBtoHSB(c.red,c.green,c.blue,null);return EditorColor(hsv[0],hsv[1],hsv[2],c.alpha/255f)}}
}

/** Local draft until Apply: picking never floods project history or image rendering. */
@Composable internal fun EditorColorPicker(label:String,value:String,dismiss:()->Unit,apply:(String)->Unit){
    var draft by remember{mutableStateOf(runCatching{EditorColor.parse(value)}.getOrElse{EditorColor(0f,0f,1f,1f)})}
    AlertDialog(onDismissRequest=dismiss,title={Text("$label · 色盘")},text={Column(Modifier.width(320.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
        val tint=Color(java.awt.Color.HSBtoRGB(draft.hue,1f,1f))
        Canvas(Modifier.fillMaxWidth().height(160.dp).semantics{contentDescription="饱和度与明度色盘"}
            .pointerInput(draft.hue){fun pick(p:Offset){draft=draft.copy(saturation=(p.x/size.width).coerceIn(0f,1f),brightness=(1-p.y/size.height).coerceIn(0f,1f))}
                detectDragGestures(onDragStart=::pick){p,_->p.consume();pick(p.position)}}
            .pointerInput(draft.hue){detectTapGestures{p->draft=draft.copy(saturation=(p.x/size.width).coerceIn(0f,1f),brightness=(1-p.y/size.height).coerceIn(0f,1f))}}){
            drawRect(Brush.horizontalGradient(listOf(Color.White,tint)));drawRect(Brush.verticalGradient(listOf(Color.Transparent,Color.Black)))
            val p=Offset(draft.saturation*size.width,(1-draft.brightness)*size.height);drawCircle(Color.Black,7f,p);drawCircle(Color.White,5f,p,style=androidx.compose.ui.graphics.drawscope.Stroke(2f))
        }
        Text("色相",style=MaterialTheme.typography.caption)
        Slider(draft.hue,{draft=draft.copy(hue=it)},modifier=Modifier.semantics{contentDescription="色相滑条"})
        Text("不透明度 · ${(draft.alpha*100).roundToInt()}%",style=MaterialTheme.typography.caption)
        Slider(draft.alpha,{draft=draft.copy(alpha=it)},modifier=Modifier.semantics{contentDescription="颜色不透明度滑条"})
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
            Box(Modifier.size(32.dp).background(Color(TypesetColors.parse(draft.hex()).rgb)).border(1.dp,MaterialTheme.colors.onSurface))
            Text(draft.hex(),Modifier.align(Alignment.CenterVertically).semantics{contentDescription="色盘当前颜色"})
        }
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
            listOf("#ffffff","#000000","#7db5ff","#d15747","#2b9181","#dfad42").forEach{color->Box(Modifier.size(28.dp).background(Color(TypesetColors.parse(color).rgb)).border(1.dp,MaterialTheme.colors.onSurface.copy(alpha=.4f)).clickable{draft=EditorColor.parse(color)}.semantics{contentDescription="常用颜色 $color"})}
        }
    }},confirmButton={TextButton(onClick={apply(draft.hex())}){Text("应用所选颜色")}},dismissButton={TextButton(onClick=dismiss){Text("取消取色")}})
}
