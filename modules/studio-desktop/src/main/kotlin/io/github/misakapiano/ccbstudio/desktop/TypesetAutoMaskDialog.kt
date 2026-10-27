package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp
import com.example.chatbar.desktop.*

@Composable internal fun TypesetAutoMaskDialog(c:WorkbenchController,e:TypesetEditorController,dismiss:()->Unit){
    val a=e.autoMask
    LaunchedEffect(Unit){a.readSettings()}
    AlertDialog(onDismissRequest={if(!c.busy)dismiss()},title={Text("自动打码配置")},text={
        Column(Modifier.width(600.dp).heightIn(max=580.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Text("程序已内置默认模型，可直接一键自动打码，无需寻找文件。自定义配置在本工作区保存；重开继续使用。",style=MaterialTheme.typography.caption)
            EditorCommand("使用内置模型",!c.busy){a.useBundledModel()}
            EditorCommand("选择本地 ONNX 模型",!c.busy){a.loadModel()}
            Text(a.model?.name?:"尚未选择模型",style=MaterialTheme.typography.caption)
            StudioSwitch("识别所选页",a.selectedPages){if(!c.busy)a.selectedPages=!a.selectedPages}
            Text(if(a.selectedPages)"执行时处理勾选页，一次完成后可整体撤销"else"执行时只处理当前图片",style=MaterialTheme.typography.caption)
            EditorRow{
                Box(Modifier.weight(1f)){NumberField("置信度",kotlin.math.round(a.options.confidence.toDouble()*1000)/1000,.05..1.0){if(!c.busy)a.options=a.options.copy(confidence=it.toFloat())}}
                Box(Modifier.weight(1f)){NumberField("重叠合并阈值",kotlin.math.round(a.options.iou.toDouble()*1000)/1000,.05..1.0){if(!c.busy)a.options=a.options.copy(iou=it.toFloat())}}
            }
            EditorRow{
                Box(Modifier.weight(1f)){CompactChoice("检测分辨率",listOf(640,832,1024,1280),a.options.resolution,{it.toString()}){if(!c.busy)a.options=a.options.copy(resolution=it)}}
                Box(Modifier.weight(1f)){NumberField("覆盖外扩（像素）",a.options.expansion,0.0..200.0){if(!c.busy)a.options=a.options.copy(expansion=it)}}
            }
            StudioSwitch("跟随当前打码工具",a.followTool){if(!c.busy)a.followTool=!a.followTool}
            if(a.followTool)Text("执行时沿用右侧打码工具的黑/白、马赛克或模糊效果、强度及透明度。检测区域为矩形，画笔路径与条纹参数不改变检测框。",style=MaterialTheme.typography.caption)
            else {
                CompactChoice("应用效果",listOf("mosaic","blur","black","white"),a.effect,{when(it){"mosaic"->"马赛克";"blur"->"模糊";"black"->"黑色覆盖";else->"白色覆盖"}}){if(!c.busy)a.effect=it}
                if(a.effect in listOf("mosaic","blur"))NumberField(if(a.effect=="mosaic")"马赛克块大小"else"模糊强度",a.strength.toDouble(),1.0..256.0){if(!c.busy)a.strength=it.toInt()}
                NumberField("打码透明度",a.opacity,0.0..1.0){if(!c.busy)a.opacity=it}
            }
            Text("检测类别",style=MaterialTheme.typography.subtitle2)
            a.model?.classes?.chunked(2)?.forEach{pair->Row(Modifier.fillMaxWidth()){
                pair.forEach{category->Row(Modifier.weight(1f),verticalAlignment=Alignment.CenterVertically){Checkbox(category in a.classes,{if(!c.busy)a.classes=if(it)a.classes+category else a.classes-category},enabled=!c.busy);Text(autoMaskCategoryName(category),style=MaterialTheme.typography.caption)}}
                if(pair.size==1)Spacer(Modifier.weight(1f))
            }}
            Text("图片不上传。当前采用检测框覆盖；结果直接成为可编辑、可撤销的打码对象。保存配置后生效，关闭不会保存本次修改。",style=MaterialTheme.typography.caption)
            c.problem?.let{Text(it,color=MaterialTheme.colors.error)}
        }
    },confirmButton={TextButton(onClick={a.saveConfiguration(dismiss)},enabled=!c.busy&&a.model!=null&&a.classes.isNotEmpty()){Text("保存配置")}},dismissButton={TextButton(onClick=dismiss,enabled=!c.busy){Text("关闭")}})
}
private fun autoMaskCategoryName(name:String)=when(name){"anus"->"肛门";"nipple"->"乳头";"penis"->"阴茎";"vagina"->"外阴";"female face"->"女性面部";"male face"->"男性面部";"pubic hair"->"阴毛";else->name}
