package io.github.misakapiano.ccbstudio.core

import com.example.chatbar.domain.image.*
import kotlinx.serialization.encodeToString
import java.nio.file.Paths
import kotlin.test.*

class ParameterReplayPersistenceTest {
    private fun imported()=NovelAiStudioDraft(basePrompt=" synthetic，base ",negativePrompt="",literalBasePrompt=true,literalNegativePrompt=true)
        .withActiveSettings(NovelAiGenerationSettings(sampler=NovelAiSampler.DPM_PLUS_PLUS_2M_SDE,
            noiseSource=NovelAiNoiseSource.EXPLICIT,noiseSchedule=NovelAiNoiseSchedule.EXPONENTIAL,
            varietyPlus=true,parameterReplay=NovelAiParameterReplay(negativeUseOrder=false,preferBrownian=false)))

    @Test fun actualWorkspaceCloseReopenKeepsReplayAndUnknownValuesWithoutGeneration() {
        val path=Paths.get(System.getProperty("preview.test.root")).resolve(newId())
        val draft=imported().withActiveSettings(imported().activeSettings.copy(parameterReplay=NovelAiParameterReplay(
            unsupportedSampler="k_dpmpp_3m_sde",preferBrownian=false)))
        StudioWorkspace.openStudio(path).use { CreationSession(it).editDraft { draft } }
        StudioWorkspace.openStudio(path).use {
            val restored=it.state.creation!!.draft
            assertEquals(draft,restored)
            assertEquals("k_dpmpp_3m_sde",restored.activeSettings.parameterReplay?.unsupportedSampler)
            assertNotNull(restored.activeSettings.validationError(0))
            assertTrue(it.state.jobs.isEmpty())
        }
    }
    @Test fun fieldSelectionDoesNotSmuggleSamplingFlagsWithTextOrSampler() {
        val current=NovelAiStudioDraft(negativePrompt="manual，negative")
        val recipe=imported().toRecipe()
        val negative=RecipeDraftSelection.apply(current,recipe,12,setOf(RecipeDraftField.NEGATIVE))
        assertEquals("",negative.negativePrompt);assertTrue(negative.literalNegativePrompt)
        assertEquals(current.activeSettings,negative.activeSettings)
        val sampler=RecipeDraftSelection.apply(current,recipe,12,setOf(RecipeDraftField.SAMPLER))
        assertEquals(NovelAiSampler.DPM_PLUS_PLUS_2M_SDE,sampler.activeSettings.sampler)
        assertNull(sampler.activeSettings.parameterReplay)
        assertFalse(sampler.literalNegativePrompt)
        val all=RecipeDraftSelection.apply(current,recipe,12,setOf(RecipeDraftField.PARAMETERS,RecipeDraftField.NOISE,RecipeDraftField.VARIETY))
        assertEquals(false,all.activeSettings.parameterReplay?.preferBrownian)
        assertEquals(NovelAiNoiseSchedule.EXPONENTIAL,all.activeSettings.noiseSchedule)
        assertTrue(all.activeSettings.varietyPlus)
    }
    @Test fun legacyRecipeAndCreationJsonDoNotAcquireExplicitImportedModes() {
        val recipe=documentJson.decodeFromString<NovelAiGenerationRecipe>("""{"settings":{"sampler":"DPM_PLUS_PLUS_2M"}}""")
        val draft=NovelAiStudioDraft().applyHistoryRecipe(recipe,12,NovelAiHistoryApplyMode.FULL)
        assertEquals("karras",NovelAiParameterCompatibility.wireNoise(draft.activeSettings))
        assertFalse(draft.literalBasePrompt);assertNull(draft.activeSettings.parameterReplay)
        val serialized=documentJson.encodeToString(recipe)
        assertFalse("parameterReplay" in serialized);assertFalse("literalBasePrompt" in serialized)
    }
    @Test fun existingWorkspaceFormatKeeps32V5CharactersAcrossReopen() {
        val path=Paths.get(System.getProperty("preview.test.root")).resolve(newId())
        val d=NovelAiStudioDraft(characters=List(32){NovelAiCharacterPromptDraft(prompt="synthetic-$it")})
            .withActiveSettings(NovelAiGenerationSettings(model=NovelAiImageModel.V5_FULL))
        StudioWorkspace.openStudio(path).use { CreationSession(it).editDraft { d } }
        StudioWorkspace.openStudio(path).use {
            val restored=it.state.creation!!.draft
            assertEquals(d,restored);assertEquals(32,restored.activeCharacters.size)
            assertNull(restored.activeSettings.validationError(32))
            assertTrue(it.state.jobs.isEmpty())
        }
    }
}
