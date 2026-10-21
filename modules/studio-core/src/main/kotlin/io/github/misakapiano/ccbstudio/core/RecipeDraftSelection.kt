package io.github.misakapiano.ccbstudio.core

import com.example.chatbar.domain.image.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

/** Typed adapter only: request serialization and validation remain in the pinned authority. */
enum class RecipeDraftField(val label:String) {
    STYLE("画风 Prompt"), BASE("基础 Prompt"), EXTRA("附加 Prompt"), NEGATIVE("负面 Prompt"),
    CHARACTERS("全部角色与位置"), MODEL("模型"), SIZE("图片尺寸"), COUNT("生成数量"),
    STEPS("Steps"), GUIDANCE("Guidance"), RESCALE("CFG Rescale"), SAMPLER("采样器"),
    SEED("图片 Seed"), POSITIONS("启用角色位置"), IMAGE_GUIDANCE("图像引导")
}

object RecipeDraftSelection {
    fun apply(current:NovelAiStudioDraft,recipe:NovelAiGenerationRecipe,imageSeed:Long,fields:Set<RecipeDraftField>):NovelAiStudioDraft {
        require(fields.isNotEmpty()) { "请先选择字段" }
        if(RecipeDraftField.IMAGE_GUIDANCE in fields) require(!recipe.imageGuidance.hasMissingHistorySource()) { "原图引导缺失，不能选择应用" }
        // FULL is the existing authority's history adapter, including guidance restore and actual image seed.
        val source=current.applyHistoryRecipe(recipe,imageSeed,NovelAiHistoryApplyMode.FULL)
        var next=current
        fields.forEach { field -> next=when(field) {
            RecipeDraftField.STYLE->next.copy(stylePrompt=source.stylePrompt)
            RecipeDraftField.BASE->next.copy(basePrompt=source.basePrompt)
            RecipeDraftField.EXTRA->next.copy(extraPrompt=source.extraPrompt)
            RecipeDraftField.NEGATIVE->next.copy(negativePrompt=source.negativePrompt)
            RecipeDraftField.CHARACTERS->next.copy(characters=source.characters)
            RecipeDraftField.IMAGE_GUIDANCE->next.copy(imageGuidance=source.imageGuidance)
            else->next
        } }
        val old=current.activeSettings;val s=source.activeSettings
        fun selected(f:RecipeDraftField)=f in fields
        val settings=old.copy(
            model=if(selected(RecipeDraftField.MODEL))s.model else old.model,
            sizeTier=if(selected(RecipeDraftField.SIZE))s.sizeTier else old.sizeTier,
            aspectRatio=if(selected(RecipeDraftField.SIZE))s.aspectRatio else old.aspectRatio,
            customWidth=if(selected(RecipeDraftField.SIZE))s.customWidth else old.customWidth,
            customHeight=if(selected(RecipeDraftField.SIZE))s.customHeight else old.customHeight,
            count=if(selected(RecipeDraftField.COUNT))s.count else old.count,
            steps=if(selected(RecipeDraftField.STEPS))s.steps else old.steps,
            guidance=if(selected(RecipeDraftField.GUIDANCE))s.guidance else old.guidance,
            cfgRescale=if(selected(RecipeDraftField.RESCALE))s.cfgRescale else old.cfgRescale,
            sampler=if(selected(RecipeDraftField.SAMPLER))s.sampler else old.sampler,
            seedMode=if(selected(RecipeDraftField.SEED))s.seedMode else old.seedMode,
            seed=if(selected(RecipeDraftField.SEED))s.seed else old.seed,
            useCharacterPositions=if(selected(RecipeDraftField.POSITIONS))s.useCharacterPositions else old.useCharacterPositions)
        // Do not normalize unrelated settings for a prompt-only choice.
        if(fields.any {it in settingsFields}) next=next.withActiveSettings(settings)
        if(selected(RecipeDraftField.MODEL))next=next.copy(followDefaultNovelAiImageModel=false)
        if(fields.any{it in promptFields})next=next.copy(conversionSnapshot=null)
        val error=next.activeSettings.validationError(next.activeCharacters.size)
        require(error==null) { error.orEmpty() }
        return next
    }
    private val promptFields=setOf(RecipeDraftField.STYLE,RecipeDraftField.BASE,RecipeDraftField.EXTRA,RecipeDraftField.CHARACTERS)
    private val settingsFields=RecipeDraftField.entries.toSet()-promptFields-setOf(RecipeDraftField.NEGATIVE,RecipeDraftField.IMAGE_GUIDANCE)
    fun fingerprint(draft:NovelAiStudioDraft)=sha256(documentJson.encodeToString(draft).toByteArray(Charsets.UTF_8))
}

/** Historical action receipt; undo does not erase history or claim the receipt is the current draft. */
@Serializable data class RecipeApplicationRecord(val sourceId:String,val sourceSha256:String,val fields:List<RecipeDraftField>,
    val beforeSha256:String,val afterSha256:String,val appliedAt:Long=System.currentTimeMillis()) {
    fun validate() {
        java.util.UUID.fromString(sourceId)
        require(fields.isNotEmpty() && fields.distinct().size==fields.size)
        listOf(sourceSha256,beforeSha256,afterSha256).forEach{require(it.matches(Regex("[0-9a-f]{64}")))}
    }
}
