package com.example.chatbar.domain.image

data class NovelAiImportedCharacterPrompt(
    val prompt: String = "",
    val negativePrompt: String = "",
    val center: DesignedCharacterCenter? = null
)

data class NovelAiImportedGenerationSettings(
    val model: NovelAiImageModel? = null,
    val sizeTier: NovelAiSizeTier? = null,
    val aspectRatio: NovelAiAspectRatio? = null,
    val count: Int? = null,
    val steps: Int? = null,
    val guidance: Float? = null,
    val cfgRescale: Float? = null,
    val sampler: NovelAiSampler? = null,
    val customWidth: Int? = null,
    val customHeight: Int? = null,
    val useCharacterPositions: Boolean? = null,
    val varietyPlus: Boolean? = null,
    val varietySigma: Float? = null,
    val noiseSchedule: NovelAiNoiseSchedule? = null,
    val parameterReplay: NovelAiParameterReplay? = null
) {
    val hasAny: Boolean
        get() = model != null || sizeTier != null || aspectRatio != null || count != null ||
            steps != null || guidance != null || cfgRescale != null || sampler != null ||
            customWidth != null || customHeight != null || useCharacterPositions != null ||
            varietyPlus != null || noiseSchedule != null || parameterReplay != null
}

data class NovelAiImportedImageGuidance(
    val action: NovelAiGenerationAction = NovelAiGenerationAction.TEXT_TO_IMAGE,
    val baseImageBase64: String? = null,
    val maskBase64: String? = null,
    val imageToImageStrength: Float? = null,
    val imageToImageNoise: Float? = null,
    val inpaintStrength: Float? = null,
    val preciseImageBase64: String? = null,
    val preciseType: NovelAiPreciseReferenceType? = null,
    val preciseStrength: Float? = null,
    val preciseFidelity: Float? = null,
    val vibes: List<NovelAiVibeReferenceDraft> = emptyList(),
    val unsupportedAction: String? = null
) {
    val hasAny: Boolean get() = unsupportedAction != null || action != NovelAiGenerationAction.TEXT_TO_IMAGE ||
        baseImageBase64 != null || maskBase64 != null || preciseImageBase64 != null || vibes.isNotEmpty()
}

data class NovelAiStudioPngMetadata(
    val imagePath: String,
    val positivePrompt: String,
    val negativePrompt: String? = null,
    val characters: List<NovelAiImportedCharacterPrompt> = emptyList(),
    val hasCharacterPrompts: Boolean = false,
    val settings: NovelAiImportedGenerationSettings = NovelAiImportedGenerationSettings(),
    val imageGuidance: NovelAiImportedImageGuidance = NovelAiImportedImageGuidance(),
    val seed: Long? = null,
    val width: Int,
    val height: Int,
    val seedInvalid: Boolean = false
)

enum class NovelAiCharacterImportMode(val displayName: String) {
    OFF("关"),
    REPLACE("覆盖"),
    APPEND("新增")
}

data class NovelAiStudioMetadataSelection(
    val positivePrompt: Boolean = true,
    val negativePrompt: Boolean = true,
    val characterPrompts: NovelAiCharacterImportMode = NovelAiCharacterImportMode.REPLACE,
    val generationSettings: Boolean = true,
    val seed: Boolean = true,
    val imageGuidance: Boolean = true
)

fun NovelAiStudioDraft.applyImportedMetadata(
    metadata: NovelAiStudioPngMetadata,
    selection: NovelAiStudioMetadataSelection
): NovelAiStudioDraft {
    val importCharacters = metadata.hasCharacterPrompts && when (selection.characterPrompts) {
        NovelAiCharacterImportMode.OFF -> false
        NovelAiCharacterImportMode.REPLACE -> true
        NovelAiCharacterImportMode.APPEND -> metadata.characters.isNotEmpty()
    }
    require(!importCharacters || "character_centers" !in metadata.settings.parameterReplay?.problems.orEmpty()) {
        "导入的多中心或独立负面位置尚不支持，未应用角色"
    }
    var result = copy(
        basePrompt = metadata.positivePrompt.takeIf { selection.positivePrompt } ?: basePrompt,
        stylePrompt = if (selection.positivePrompt) "" else stylePrompt,
        literalBasePrompt = if (selection.positivePrompt) true else literalBasePrompt,
        literalNegativePrompt = if (selection.negativePrompt && metadata.negativePrompt != null) true else literalNegativePrompt,
        extraPrompt = if (selection.positivePrompt) "" else extraPrompt,
        negativePrompt = metadata.negativePrompt
            ?.takeIf { selection.negativePrompt }
            ?: negativePrompt,
        characters = if (importCharacters) {
            val importedCharacters = metadata.characters.map { character ->
                NovelAiCharacterPromptDraft(
                    prompt = character.prompt,
                    negativePrompt = character.negativePrompt,
                    center = character.center,
                    literalPrompt = true,
                    literalNegative = true,
                    literalCoordinates = true
                )
            }
            if (selection.characterPrompts == NovelAiCharacterImportMode.APPEND) characters + importedCharacters
            else importedCharacters
        } else {
            characters
        },
        conversionSnapshot = if (selection.positivePrompt || importCharacters) {
            null
        } else {
            conversionSnapshot
        }
    )

    if (selection.generationSettings && metadata.settings.hasAny) {
        val imported = metadata.settings
        val targetModel = imported.model ?: result.selectedModel
        if (imported.model != null) {
            result = result.copy(followDefaultNovelAiImageModel = false)
        }
        val current = when (targetModel) {
            NovelAiImageModel.V4_5_FULL -> result.v45Settings
            NovelAiImageModel.V5_FULL -> result.v5Settings
        }
        val oldReplay=current.parameterReplay
        val incoming=imported.parameterReplay
        val present=buildSet {
            if(imported.steps!=null)add("steps")
            if(imported.count!=null)add("n_samples")
            if(imported.guidance!=null)add("scale")
            if(imported.cfgRescale!=null)add("cfg_rescale")
            if(imported.noiseSchedule!=null)add("noise_schedule")
            if(imported.varietyPlus!=null)add("skip_cfg_above_sigma")
            if(incoming?.positiveUseOrder!=null)add("positive.use_order")
            if(incoming?.negativeUseOrder!=null)add("negative.use_order")
            if(incoming?.positiveUseCoords!=null)add("positive.use_coords")
            if(incoming?.negativeUseCoords!=null)add("negative.use_coords")
            if(incoming?.preferBrownian!=null)add("prefer_brownian")
            if(incoming?.deliberateEulerAncestralBug!=null)add("deliberate_euler_ancestral_bug")
            if(importCharacters)add("character_centers")
            add("dimensions")
        }
        val replay=incoming?.copy(
            positiveUseOrder=incoming.positiveUseOrder ?: oldReplay?.positiveUseOrder,
            negativeUseOrder=incoming.negativeUseOrder ?: oldReplay?.negativeUseOrder,
            positiveUseCoords=incoming.positiveUseCoords ?: oldReplay?.positiveUseCoords,
            negativeUseCoords=incoming.negativeUseCoords ?: oldReplay?.negativeUseCoords,
            preferBrownian=incoming.preferBrownian ?: oldReplay?.preferBrownian,
            deliberateEulerAncestralBug=incoming.deliberateEulerAncestralBug ?: oldReplay?.deliberateEulerAncestralBug,
            unsupportedSampler=if(imported.sampler!=null || incoming.unsupportedSampler!=null)incoming.unsupportedSampler else oldReplay?.unsupportedSampler,
            unsupportedModel=if(imported.model!=null || incoming.unsupportedModel!=null)incoming.unsupportedModel else oldReplay?.unsupportedModel,
            unsupportedNoiseSchedule=if(imported.noiseSchedule!=null || incoming.unsupportedNoiseSchedule!=null)incoming.unsupportedNoiseSchedule else oldReplay?.unsupportedNoiseSchedule,
            problems=((oldReplay?.problems.orEmpty()-present)+incoming.problems.filterNot {
                it == "character_centers" && !importCharacters
            }).distinct()
        ) ?: oldReplay
        val effectiveReplay = if (targetModel == NovelAiImageModel.V5_FULL && imported.varietySigma != null) {
            (replay ?: NovelAiParameterReplay()).copy(problems=(replay?.problems.orEmpty()+"skip_cfg_above_sigma").distinct())
        } else replay
        result = result.withActiveSettings(
            current.copy(
                model = targetModel,
                sizeTier = imported.sizeTier ?: current.sizeTier,
                aspectRatio = imported.aspectRatio ?: current.aspectRatio,
                customWidth = if (imported.sizeTier != null || imported.customWidth != null) imported.customWidth else current.customWidth,
                customHeight = if (imported.sizeTier != null || imported.customHeight != null) imported.customHeight else current.customHeight,
                count = imported.count ?: current.count,
                steps = imported.steps ?: current.steps,
                guidance = imported.guidance ?: current.guidance,
                cfgRescale = imported.cfgRescale ?: current.cfgRescale,
                sampler = imported.sampler ?: current.sampler,
                useCharacterPositions = imported.useCharacterPositions ?: current.useCharacterPositions,
                varietyPlus = imported.varietyPlus ?: current.varietyPlus,
                varietySigma = if(imported.varietyPlus!=null)imported.varietySigma else current.varietySigma,
                noiseSource = if (imported.noiseSchedule != null) NovelAiNoiseSource.EXPLICIT else current.noiseSource,
                noiseSchedule = imported.noiseSchedule ?: current.noiseSchedule,
                parameterReplay = effectiveReplay
            )
        )
    }

    require(!selection.seed || !metadata.seedInvalid) { "导入的Seed不受支持，未应用参数" }
    if (selection.seed && metadata.seed != null) {
        result = result.withActiveSettings(
            result.activeSettings.copy(
                seedMode = NovelAiSeedMode.FIXED,
                seed = metadata.seed
            )
        )
    }
    if (selection.imageGuidance && metadata.imageGuidance.hasAny) {
        val imported = metadata.imageGuidance
        require(imported.unsupportedAction == null) { "不支持导入的图像操作：${imported.unsupportedAction}；可取消图像引导选择" }
        require(result.selectedModel==NovelAiImageModel.V4_5_FULL ||
            imported.preciseImageBase64==null && imported.vibes.isEmpty()) {
            "V5 Full 不支持本产品的参考图／氛围参考；请取消图像引导选择，不会静默忽略"
        }
        require(imported.action==NovelAiGenerationAction.TEXT_TO_IMAGE ||
            imported.baseImageBase64!=null && (imported.action!=NovelAiGenerationAction.INPAINT || imported.maskBase64!=null)) {
            "缺少原始引导图或蒙版，不能还原图像引导；可取消图像引导选择，仅复用文本和参数"
        }
        val activeAction = imported.action.takeIf {
            imported.baseImageBase64 != null && (it != NovelAiGenerationAction.INPAINT || imported.maskBase64 != null)
        } ?: NovelAiGenerationAction.TEXT_TO_IMAGE
        result = result.copy(
            imageGuidance = result.imageGuidance.copy(
                action = activeAction,
                imageToImageStrength = imported.imageToImageStrength ?: result.imageGuidance.imageToImageStrength,
                imageToImageNoise = imported.imageToImageNoise ?: result.imageGuidance.imageToImageNoise,
                inpaintStrength = imported.inpaintStrength ?: result.imageGuidance.inpaintStrength,
                referenceMode = when {
                    imported.preciseImageBase64 != null -> NovelAiReferenceMode.PRECISE
                    imported.vibes.isNotEmpty() -> NovelAiReferenceMode.VIBE
                    else -> NovelAiReferenceMode.NONE
                },
                preciseReference = result.imageGuidance.preciseReference.copy(
                    type = imported.preciseType ?: result.imageGuidance.preciseReference.type,
                    strength = imported.preciseStrength ?: result.imageGuidance.preciseReference.strength,
                    fidelity = imported.preciseFidelity ?: result.imageGuidance.preciseReference.fidelity
                ),
                vibes = imported.vibes
            )
        )
    }
    return result
}
