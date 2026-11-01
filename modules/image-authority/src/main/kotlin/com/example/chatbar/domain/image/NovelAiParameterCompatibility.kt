package com.example.chatbar.domain.image

import kotlinx.serialization.Serializable

/** Typed allowlist; never merge arbitrary imported JSON into a request. */
@Serializable
data class NovelAiParameterReplay(
    val positiveUseOrder: Boolean? = null,
    val negativeUseOrder: Boolean? = null,
    val positiveUseCoords: Boolean? = null,
    val negativeUseCoords: Boolean? = null,
    val preferBrownian: Boolean? = null,
    val deliberateEulerAncestralBug: Boolean? = null,
    val unsupportedSampler: String? = null,
    val unsupportedModel: String? = null,
    val unsupportedNoiseSchedule: String? = null,
    val problems: List<String> = emptyList()
) {
    fun cleared(vararg fields: String): NovelAiParameterReplay = copy(problems=problems.filterNot { it in fields })
    fun validationError(): String? = when {
        unsupportedSampler != null -> "不支持导入的采样器：$unsupportedSampler，请明确选择采样器"
        unsupportedNoiseSchedule != null -> "不支持导入的噪声计划：$unsupportedNoiseSchedule"
        unsupportedModel != null -> "不支持导入的模型：$unsupportedModel，请明确选择模型"
        problems.isNotEmpty() -> "导入参数尚不支持：${problems.joinToString()}"
        else -> null
    }
}

@Serializable enum class NovelAiNoiseSchedule(val apiId: String) {
    NATIVE("native"), KARRAS("karras"), EXPONENTIAL("exponential"), POLYEXPONENTIAL("polyexponential")
}
@Serializable enum class NovelAiNoiseSource { LEGACY, MODEL_DEFAULT, EXPLICIT }

/** Explicit position edits supersede imported coordinate flags, not other replay fields. */
fun NovelAiStudioDraft.withManualCharacterPositions(enabled: Boolean): NovelAiStudioDraft =
    withActiveSettings(activeSettings.copy(useCharacterPositions=enabled,
        parameterReplay=activeSettings.parameterReplay?.copy(positiveUseCoords=null, negativeUseCoords=null)))

object NovelAiParameterCompatibility {
    // Official public JS f1069333…d702a: tT/tM/tP. No unproven 3M catalog expansion.
    fun noises(model: NovelAiImageModel, sampler: NovelAiSampler): List<NovelAiNoiseSchedule> =
        if (model == NovelAiImageModel.V5_FULL || sampler == NovelAiSampler.DDIM) emptyList()
        else listOf(NovelAiNoiseSchedule.KARRAS, NovelAiNoiseSchedule.EXPONENTIAL, NovelAiNoiseSchedule.POLYEXPONENTIAL)

    fun defaultNoise(sampler: NovelAiSampler) = when(sampler) {
        NovelAiSampler.DPM_PLUS_PLUS_2M -> NovelAiNoiseSchedule.EXPONENTIAL
        NovelAiSampler.DDIM -> NovelAiNoiseSchedule.NATIVE // tP helper; not a DDIM editor/wire promise.
        else -> NovelAiNoiseSchedule.KARRAS
    }

    fun wireNoise(settings: NovelAiGenerationSettings): String? = when (settings.noiseSource) {
        NovelAiNoiseSource.LEGACY -> "karras" // Missing old recipe fields must not change replay.
        NovelAiNoiseSource.MODEL_DEFAULT -> when {
            settings.model == NovelAiImageModel.V5_FULL -> "karras"
            settings.sampler == NovelAiSampler.DDIM -> null
            else -> defaultNoise(settings.sampler).apiId
        }
        NovelAiNoiseSource.EXPLICIT -> requireNotNull(settings.noiseSchedule).apiId
    }
    fun varietySigma(width: Int, height: Int): Float =
        (58.0*kotlin.math.sqrt(width.toDouble()*height/(832.0*1216.0))).toFloat()
    fun noiseError(settings: NovelAiGenerationSettings): String? {
        if (settings.noiseSource != NovelAiNoiseSource.EXPLICIT) return null
        val noise=settings.noiseSchedule ?: return "缺少明确的噪声计划"
        // V5 has no noise editor. Existing typed imported values remain replayable;
        // absent official serializer evidence does not authorize deleting the wire key.
        if (settings.model == NovelAiImageModel.V5_FULL) return null
        // DDIM is nonNoiseSchedule: preserve only our pre-existing karras wire
        // replay, without inventing a new configurable/provider serialization policy.
        if (settings.sampler == NovelAiSampler.DDIM && noise == NovelAiNoiseSchedule.KARRAS) return null
        return if (noise in noises(settings.model,settings.sampler)) null else "当前模型／采样器不支持该噪声计划"
    }
}
