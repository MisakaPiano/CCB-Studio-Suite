package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.chatbar.desktop.*
import com.example.chatbar.domain.image.*
import kotlinx.coroutines.*

/** Source Studio guidance controls, using immutable Suite IDs instead of Desktop filesystem paths. */
@Composable internal fun GuidanceWorkspace(c:WorkbenchController) {
    val d=c.draft;val g=d.imageGuidance
    var tools by remember{mutableStateOf<Triple<String,ByteArray,ByteArray?>?>(null)}
    val scope=rememberCoroutineScope()
    fun change(transform:(NovelAiImageGuidanceDraft)->NovelAiImageGuidanceDraft)=c.edit{it.copy(imageGuidance=transform(it.imageGuidance))}
    StatusText("先导入或选择图库中的原件，再指定用途。原件保持不变。")
    c.current?.let{id->
        StudioActions{desktopImageUseTargets(d.selectedModel).forEach{target->StudioAction("当前图片 → ${target.displayName}",!c.busy){c.useImage(id,target)}}}
    }
    StudioAction("导入原件",!c.busy){c.importImages()}
    CompactChoice("操作",NovelAiGenerationAction.entries,g.action,{it.displayName}){v->change{it.copy(action=v)}}
    CompactChoice("参考",if(d.selectedModel==NovelAiImageModel.V4_5_FULL)NovelAiReferenceMode.entries else listOf(NovelAiReferenceMode.NONE),g.effectiveReferenceMode(d.selectedModel),{it.name}){v->change{it.copy(referenceMode=v)}}
    if(d.selectedModel!=NovelAiImageModel.V4_5_FULL)StatusText("当前模型不使用精确参考或 Vibe；已保存的参考保留。")
    g.baseImage?.let{asset->
        DesktopOwnedImage(asset.path,{c.readAsset(asset.path)},Modifier.fillMaxWidth().height(180.dp))
        StudioActions{
            StudioAction("编辑基图 / 聚焦框 / 蒙版",!c.busy){scope.launch{tools=withContext(Dispatchers.IO){Triple(asset.id,c.readAsset(asset.path),g.maskImage?.let{c.readAsset(it.path)})}}}
            StudioAction("移除基图",!c.busy){change{it.withoutImageSource(NovelAiImageUseTarget.IMAGE_TO_IMAGE)}}
        }
    }
    if(g.action==NovelAiGenerationAction.INPAINT) {
        StudioField("Minimum Context 32–96",g.focusedInpaintMinimumContext.toString()){text->text.toIntOrNull()?.let{v->change{it.copy(focusedInpaintMinimumContext=v)}}}
        StudioField("Inpaint Strength",g.inpaintStrength.toString()){text->text.toFloatOrNull()?.let{v->change{it.copy(inpaintStrength=v)}}}
    }
    if(g.action==NovelAiGenerationAction.IMAGE_TO_IMAGE) {
        StudioField("图生图 Strength",g.imageToImageStrength.toString()){text->text.toFloatOrNull()?.let{v->change{it.copy(imageToImageStrength=v)}}}
        StudioField("Noise",g.imageToImageNoise.toString()){text->text.toFloatOrNull()?.let{v->change{it.copy(imageToImageNoise=v)}}}
    }
    if(g.effectiveReferenceMode(d.selectedModel)==NovelAiReferenceMode.PRECISE) {
        g.preciseReference.asset?.let{a->DesktopOwnedImage(a.path,{c.readAsset(a.path)},Modifier.fillMaxWidth().height(140.dp))}
        CompactChoice("精确参考类型",NovelAiPreciseReferenceType.entries,g.preciseReference.type,{it.displayName}){v->change{it.copy(preciseReference=it.preciseReference.copy(type=v))}}
        StudioField("参考 Strength",g.preciseReference.strength.toString()){text->text.toFloatOrNull()?.let{v->change{it.copy(preciseReference=it.preciseReference.copy(strength=v))}}}
        StudioField("Fidelity",g.preciseReference.fidelity.toString()){text->text.toFloatOrNull()?.let{v->change{it.copy(preciseReference=it.preciseReference.copy(fidelity=v))}}}
        StudioAction("移除精确参考"){change{it.withoutImageSource(NovelAiImageUseTarget.PRECISE_REFERENCE)}}
    }
    if(g.effectiveReferenceMode(d.selectedModel)==NovelAiReferenceMode.VIBE) {
        g.vibes.forEachIndexed{index,vibe->
            StatusText("Vibe ${index+1}")
            vibe.asset?.let{a->DesktopOwnedImage(a.path,{c.readAsset(a.path)},Modifier.fillMaxWidth().height(100.dp))}
            if(vibe.informationEditable)StudioField("信息提取 ${index+1}",vibe.informationExtracted.toString()){text->text.toFloatOrNull()?.let{v->change{it.copy(vibes=it.vibes.map{old->if(old.id==vibe.id)old.copy(informationExtracted=v,encodedVibe=null)else old})}}}
            StudioField("强度 ${index+1}",vibe.strength.toString()){text->text.toFloatOrNull()?.let{v->change{it.copy(vibes=it.vibes.map{old->if(old.id==vibe.id)old.copy(strength=v)else old})}}}
            StudioAction("移除 Vibe ${index+1}"){change{it.copy(vibes=it.vibes.filterNot{old->old.id==vibe.id})}}
        }
        StudioToggle("归一化 Vibe 强度",g.normalizeVibeStrengths){change{it.copy(normalizeVibeStrengths=!it.normalizeVibeStrengths)}}
    }
    StudioAction("重置图像引导",!c.busy){change{NovelAiImageGuidanceDraft()}}
    g.validationError(d.selectedModel)?.let{StatusText(it,Gold)}
    tools?.let{(parent,bytes,mask)->DesktopImageToolsDialog(bytes,c.picker,{tools=null},
        onGuidanceApply={image,newMask,region->c.applyGuidanceTools(parent,image,newMask,region)},initialMask=mask,initialRegion=g.focusedInpaintRegion)}
}
