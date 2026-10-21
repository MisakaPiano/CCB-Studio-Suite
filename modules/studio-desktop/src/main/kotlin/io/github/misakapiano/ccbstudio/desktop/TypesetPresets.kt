package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.chatbar.desktop.*
import io.github.misakapiano.ccbstudio.core.*

@Composable internal fun TypesetPresets(c:WorkbenchController,e:TypesetEditorController) {
    val prefs=c.state.typesetPreferences
    var selected by remember(prefs.selectedTextPresetId){mutableStateOf(prefs.selectedTextPresetId)}
    var name by remember(selected){mutableStateOf(prefs.textPresets.firstOrNull{it.id==selected}?.name.orEmpty())}
    var slot by remember{mutableStateOf("light_mosaic")}
    var confirm by remember{mutableStateOf<String?>(null)}
    fun slotName(key:String)=when(key){"light_mosaic"->"轻马赛克";"heavy_mosaic"->"重马赛克";"fine_stripes"->"细条纹";"coarse_stripes"->"粗条纹";else->"已保存自定义参数"}
    StudioDisclosure("预设与默认") {
        Text("文字预设跨工程可用，只保存样式，不保存本页文字和位置。")
        StudioActions{StudioAction("导入 TypeSet 预设",!c.busy){e.inspectPresets()};StudioAction("导出 TypeSet 预设",!c.busy){e.exportPresets()}}
        CompactChoice("文字预设",prefs.textPresets, prefs.textPresets.firstOrNull{it.id==selected},{it.name}){selected=it.id}
        OutlinedTextField(name,{name=it.take(100)},label={Text("预设名称")},modifier=Modifier.fillMaxWidth())
        StudioActions {
            StudioAction("记住新建文字样式",e.text!=null && !c.busy){e.rememberTextDefault()}
            StudioAction("保存新文字预设",e.text!=null && name.isNotBlank() && !c.busy){e.saveTextPreset(name)}
            StudioAction("应用文字预设",e.text!=null && selected!=null && !c.busy){e.applyTextPreset(selected!!)}
            StudioAction("覆盖文字预设",e.text!=null && selected!=null && name.isNotBlank() && !c.busy){confirm="text-overwrite"}
            StudioAction("删除文字预设",selected!=null && !c.busy){confirm="text-remove"}
        }
        Divider()
        CompactChoice("打码预设槽",TypesetPreferences.MASK_SLOTS,slot,::slotName){slot=it}
        StatusText(if(slot in prefs.maskOverrides)"此槽使用自定义值"else"此槽使用TypeSet内置值")
        StudioActions {
            StudioAction("保存自定义打码参数",!c.busy){e.rememberMaskDefault()}
            StudioAction("应用打码预设",!c.busy){e.maskPreset("apply",slot)}
            StudioAction("覆盖打码预设",!c.busy){confirm="mask-overwrite"}
            StudioAction("恢复内置打码预设",slot in prefs.maskOverrides && !c.busy){confirm="mask-reset"}
        }
        CompactChoice("启动默认",listOf("custom")+TypesetPreferences.MASK_SLOTS,prefs.preferredMaskSlot,::slotName){e.maskPreset("prefer",it)}
        StudioAction("记住连续工作打码参数",!c.busy){e.rememberLaterMaskDefault()}
        Text("已存页面优先使用自己的参数；无单页默认的后续切图/新页使用连续工作参数；否则按启动默认选择工具。细/粗条纹保留当前黑白色，否则使用黑色。")
    }
    e.presetInspection?.let{inspection->AlertDialog(onDismissRequest={e.cancelPresetImport()},title={Text("确认导入 TypeSet 预设")},text={Column{
        Text("${inspection.preferences.textPresets.size} 个文字预设，${inspection.preferences.maskOverrides.size} 个自定义打码槽。")
        Text("合并会追加文字预设、覆盖同名打码槽并导入默认值；替换会移除原预设列表。已有页面不变，原交换文件保留。")
        Text("同时导入导出分类/格式/元数据政策、队列尺寸、小人书和后续打码默认。队列及故事书默认只影响新工程/新页面，已有页面不改写。")
    }},confirmButton={Row{TextButton(enabled=!c.busy,onClick={e.importPresets(true)}){Text("合并并导入默认")};TextButton(enabled=!c.busy,onClick={e.importPresets(false)}){Text("替换全部预设")}}},dismissButton={TextButton(onClick={e.cancelPresetImport()}){Text("取消预设导入")}})}
    if(confirm!=null)AlertDialog(onDismissRequest={confirm=null},title={Text("确认预设变更")},text={Text(when(confirm){"text-overwrite"->"用当前文字样式覆盖所选预设；已有页面不变。";"text-remove"->"删除所选预设；已应用的文字与样式保留。";"mask-overwrite"->"用当前工具参数覆盖${slotName(slot)}；其他槽不变。";else->"将${slotName(slot)}恢复为TypeSet内置值；已有遮罩不变。"})},confirmButton={TextButton(onClick={when(confirm){"text-overwrite"->e.saveTextPreset(name,selected);"text-remove"->selected?.let(e::deleteTextPreset);"mask-overwrite"->e.maskPreset("overwrite",slot);"mask-reset"->e.maskPreset("reset",slot)};confirm=null}){Text("确认预设变更")}},dismissButton={TextButton(onClick={confirm=null}){Text("取消")}})
}
