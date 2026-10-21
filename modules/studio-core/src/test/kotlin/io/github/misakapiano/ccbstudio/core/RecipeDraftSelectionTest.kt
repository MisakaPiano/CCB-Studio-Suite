package io.github.misakapiano.ccbstudio.core

import com.example.chatbar.domain.image.*
import kotlinx.serialization.encodeToString
import kotlin.test.*

class RecipeDraftSelectionTest {
    @Test fun missingHistoryGuidanceBlocksGuidanceOnlyAndKeepsPromptReuseAvailable() {
        val current=NovelAiStudioDraft(basePrompt="old")
        val recipe=NovelAiGenerationRecipe(basePrompt="new",imageGuidance=NovelAiImageGuidanceDraft(referenceMode=NovelAiReferenceMode.PRECISE))
        assertFailsWith<IllegalArgumentException>{RecipeDraftSelection.apply(current,recipe,10,setOf(RecipeDraftField.IMAGE_GUIDANCE))}
        assertEquals(current.copy(basePrompt="new"),RecipeDraftSelection.apply(current,recipe,10,setOf(RecipeDraftField.BASE)))
    }
    @Test fun selectingOnePromptDoesNotReplaceOtherDraftContentOrSettings() {
        val current=NovelAiStudioDraft(basePrompt="old",stylePrompt="keep",extraPrompt="extra",naturalLanguageMode=true,
            imageDescription="description",negativePrompt="negative",followDefaultNovelAiImageModel=true)
        val recipe=NovelAiGenerationRecipe(basePrompt="new",stylePrompt="not chosen",settings=NovelAiGenerationSettings(model=NovelAiImageModel.V5_FULL,steps=40))
        assertEquals(current.copy(basePrompt="new"),RecipeDraftSelection.apply(current,recipe,123,setOf(RecipeDraftField.BASE)))
        assertFailsWith<IllegalArgumentException>{RecipeDraftSelection.apply(current,recipe,123,emptySet())}
    }
    @Test fun individualSettingsUseActualImageSeedAndRetainUnselectedModelSettings() {
        val current=NovelAiStudioDraft().withActiveSettings(NovelAiGenerationSettings(steps=20,guidance=4f,seed=1))
        val recipe=NovelAiGenerationRecipe(settings=NovelAiGenerationSettings(model=NovelAiImageModel.V5_FULL,steps=40,guidance=9f,seed=100))
        val selected=RecipeDraftSelection.apply(current,recipe,102,setOf(RecipeDraftField.STEPS,RecipeDraftField.SEED))
        assertEquals(current.activeSettings.copy(steps=40,seedMode=NovelAiSeedMode.FIXED,seed=102),selected.activeSettings)
        assertEquals(current.v5Settings,selected.v5Settings)
        val model=RecipeDraftSelection.apply(current,recipe,102,setOf(RecipeDraftField.MODEL))
        assertEquals(current.activeSettings.copy(model=NovelAiImageModel.V5_FULL),model.activeSettings)
        assertEquals(current.v45Settings,model.v45Settings)
        assertFailsWith<IllegalArgumentException>{RecipeDraftSelection.apply(current,recipe.copy(settings=recipe.settings.copy(steps=999)),102,setOf(RecipeDraftField.STEPS))}
    }
    @Test fun historicalReceiptSurvivesUndoRedoAndSerializationWithoutClaimingCurrentState() {
        val before=NovelAiStudioDraft(basePrompt="original");val after=before.copy(basePrompt="applied")
        val record=RecipeApplicationRecord(newId(),"a".repeat(64),listOf(RecipeDraftField.BASE),RecipeDraftSelection.fingerprint(before),RecipeDraftSelection.fingerprint(after))
        val state=CreationState(draft=before).change(after).copy(recipeApplications=listOf(record));state.validate()
        val undone=documentJson.decodeFromString<CreationState>(documentJson.encodeToString(state.undoDraft()))
        assertEquals(before,undone.draft);assertEquals(listOf(record),undone.recipeApplications)
        assertEquals(after,undone.redoDraft().draft)
        val legacy=documentJson.decodeFromString<CreationState>("{}");assertTrue(legacy.recipeApplications.isEmpty())
    }
}
