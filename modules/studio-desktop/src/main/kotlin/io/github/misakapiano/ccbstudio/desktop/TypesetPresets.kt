package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.chatbar.desktop.*
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.launch

@Composable internal fun TypesetPresets(c:WorkbenchController,e:TypesetEditorController) {
    val prefs=c.state.typesetPreferences;val masks=e.editorExtras.namedMasks
    val scope=rememberCoroutineScope()
    var kind by remember{mutableStateOf("text")}
    var textId by remember{mutableStateOf(prefs.selectedTextPresetId)}
    var maskId by remember{mutableStateOf<String?>(null)}
    LaunchedEffect(prefs.textPresets,masks){
        if(prefs.textPresets.none{it.id==textId})textId=prefs.textPresets.firstOrNull()?.id
        if(masks.none{it.id==maskId})maskId=masks.firstOrNull()?.id
    }
    val textPreset=prefs.textPresets.firstOrNull{it.id==textId};val maskPreset=masks.firstOrNull{it.id==maskId}
    val selected=if(kind=="text")textId else maskId
    val selectedName=if(kind=="text")textPreset?.name else maskPreset?.name
    var name by remember(kind,selected,selectedName){mutableStateOf(selectedName.orEmpty())}
    var confirm by remember{mutableStateOf<String?>(null)}
    var slot by remember{mutableStateOf("light_mosaic")}
    fun slotName(key:String)=when(key){"light_mosaic"->"轻马赛克";"heavy_mosaic"->"重马赛克";"fine_stripes"->"细条纹";"coarse_stripes"->"粗条纹";else->"已保存自定义参数"}
    val currentAvailable=kind=="mask"||e.text!=null
    StudioActions {
        StudioAction("文字配置",!c.busy,selected=kind=="text",icon=TypesetIcons.Text){kind="text"}
        StudioAction("打码配置",!c.busy,selected=kind=="mask",icon=TypesetIcons.Brush){kind="mask"}
    }
    if(kind=="text")CompactChoice("已保存文字预设",prefs.textPresets,textPreset,{it.name}){textId=it.id}
    else CompactChoice("已保存打码预设",masks,maskPreset,{it.name}){maskId=it.id}
    if(selected==null)StatusText("尚无命名预设；填写名称后保存当前配置。")
    OutlinedTextField(name,{name=it.take(100)},label={Text("预设名称")},modifier=Modifier.fillMaxWidth())
    StudioActions {
        StudioAction("保存新预设",!c.busy&&name.isNotBlank()&&currentAvailable,icon=DesktopAppIcons.Add){
            if(kind=="text"){e.saveTextPreset(name.trim());val job=c.active;scope.launch{job?.join();textId=c.workspace.state.typesetPreferences.selectedTextPresetId}}
            else {e.saveNamedMask(name.trim());val job=c.active;scope.launch{job?.join();maskId=e.editorExtras.namedMasks.lastOrNull()?.id}}
        }
        StudioAction("应用到当前对象",!c.busy&&selected!=null&&currentAvailable){
            if(kind=="text")textId?.let(e::applyTextPreset)else maskId?.let{e.applyNamedMask(it)}
        }
        StudioAction("设为新建默认",!c.busy&&selected!=null){
            if(kind=="text")textId?.let(e::setTextPresetDefault)else maskId?.let{e.applyNamedMask(it,true)}
        }
    }
    StudioActions {
        StudioAction("重命名预设",!c.busy&&selected!=null&&name.isNotBlank(),icon=DesktopAppIcons.Edit){
            if(kind=="text")textId?.let{e.renameTextPreset(it,name.trim())}else maskId?.let{e.renameNamedMask(it,name.trim())}
        }
        StudioAction("覆盖所选预设",!c.busy&&selected!=null&&currentAvailable&&name.isNotBlank()){confirm="overwrite"}
        StudioAction("删除预设",!c.busy&&selected!=null,icon=DesktopAppIcons.Delete){confirm="remove"}
    }
    Text("只保存配置，保留各页正文、位置与打码范围。新建默认不会改写已有对象。",style=MaterialTheme.typography.caption)
    StudioDisclosure("连续编辑与 TypeSet 内置槽") {
        Text("四个原始打码槽及旧自定义值继续保留，和上方命名预设分别管理。")
        CompactChoice("打码预设槽",TypesetPreferences.MASK_SLOTS,slot,::slotName){slot=it}
        StatusText(if(slot in prefs.maskOverrides)"此槽使用自定义值"else"此槽使用 TypeSet 内置值")
        StudioActions {
            StudioAction("应用内置槽",!c.busy){e.maskPreset("apply",slot)}
            StudioAction("覆盖内置槽",!c.busy){confirm="slot-overwrite"}
            StudioAction("恢复内置槽",!c.busy&&slot in prefs.maskOverrides){confirm="slot-reset"}
        }
        CompactChoice("旧启动默认",listOf("custom")+TypesetPreferences.MASK_SLOTS,prefs.preferredMaskSlot,::slotName){e.maskPreset("prefer",it)}
        StudioActions {
            StudioAction("记住当前文字默认",!c.busy&&e.text!=null){e.rememberTextDefault()}
            StudioAction("保存当前打码默认",!c.busy){e.rememberMaskDefault()}
            StudioAction("记住连续工作打码参数",!c.busy){e.rememberLaterMaskDefault()}
        }
        Text("已有页面保留自己的打码参数；新文字沿用最近样式。")
    }
    StudioDisclosure("导入与导出预设") {
        Text("跨工程保存文字、命名打码和内置槽；TypeSet 1.1 忽略的 Studio 扩展仍单独保留。")
        StudioActions {StudioAction("导入 TypeSet 预设",!c.busy){e.inspectPresets()};StudioAction("导出 TypeSet 预设",!c.busy){e.exportPresets()}}
    }
    e.presetInspection?.let{inspection->AlertDialog(onDismissRequest={e.cancelPresetImport()},title={Text("确认导入 TypeSet 预设")},text={Column{
        Text("${inspection.preferences.textPresets.size} 个文字预设，${inspection.preferences.maskOverrides.size} 个自定义打码槽；文件含有的命名打码扩展会一并保留。")
        Text("合并追加命名预设并覆盖同名内置槽；替换会替换预设列表。已有页面不改写，原交换文件保留。")
        Text("同时导入导出格式、来源政策、队列尺寸、小人书和连续工作默认；队列及小人书默认只影响新工程／新页面。")
    }},confirmButton={Row{TextButton(enabled=!c.busy,onClick={e.importPresets(true)}){Text("合并并导入默认")};TextButton(enabled=!c.busy,onClick={e.importPresets(false)}){Text("替换全部预设")}}},dismissButton={TextButton(onClick={e.cancelPresetImport()}){Text("取消预设导入")}})}
    if(confirm!=null)AlertDialog(onDismissRequest={confirm=null},title={Text("确认预设变更")},text={Text(when(confirm){
        "overwrite"->"用当前配置覆盖所选预设；已应用的对象保持原样。"
        "remove"->"删除所选预设；已应用的文字、打码与配置保持原样。"
        "slot-overwrite"->"用当前工具参数覆盖 ${slotName(slot)}；其他槽不变。"
        else->"恢复 ${slotName(slot)} 的 TypeSet 内置值；已有遮罩不变。"
    })},confirmButton={TextButton(enabled=!c.busy,onClick={
        when(confirm){
            "overwrite"->if(kind=="text")textId?.let{e.saveTextPreset(name.trim(),it)}else maskId?.let{e.saveNamedMask(name.trim(),it)}
            "remove"->if(kind=="text")textId?.let(e::deleteTextPreset)else maskId?.let(e::deleteNamedMask)
            "slot-overwrite"->e.maskPreset("overwrite",slot)
            "slot-reset"->e.maskPreset("reset",slot)
        };confirm=null
    }){Text("确认预设变更")}},dismissButton={TextButton(onClick={confirm=null}){Text("取消")}})
}
