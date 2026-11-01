package com.example.chatbar.domain.image

import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import org.junit.Test
import kotlin.test.*

class NovelAiParameterCompatibilityTest {
    private val codec=Json { encodeDefaults=true }
    private fun metadata(extra: String=""): NovelAiStudioPngMetadata = requireNotNull(NovelAiPngMetadataReader.parseStudioComment(
        """{"model":"nai-diffusion-4-5-full","width":832,"height":1216,"steps":28,"sampler":"k_dpmpp_2m_sde","seed":12,
        "prompt":" synthetic，base ","uc":" negative，literal "$extra} """, "synthetic.png"))
    private fun wire(draft: NovelAiStudioDraft): JsonObject = Json.parseToJsonElement(NovelAiImageService().buildRequestBody(
        draft.toPromptPlan(),draft.activeSettings.imageSize(),draft.activeSettings)).jsonObject.getValue("parameters").jsonObject

    @Test fun legacyAndNewNoiseAreDistinctAcrossSerialization() {
        val legacy=codec.decodeFromString<NovelAiGenerationSettings>("""{"sampler":"DPM_PLUS_PLUS_2M"}""")
        assertEquals("karras",NovelAiParameterCompatibility.wireNoise(legacy))
        val fresh=NovelAiStudioDraft().activeSettings.copy(sampler=NovelAiSampler.DPM_PLUS_PLUS_2M)
        assertEquals("exponential",NovelAiParameterCompatibility.wireNoise(fresh))
        assertEquals("exponential",NovelAiParameterCompatibility.wireNoise(codec.decodeFromString(codec.encodeToString(fresh))))
        val old=codec.encodeToString(NovelAiGenerationSettings())
        assertFalse("noiseSource" in old);assertFalse("parameterReplay" in old);assertFalse("varietyPlus" in old)
    }
    @Test fun bothFullModelsSendSdeWithoutFallbackAndV5KeepsImportedNoise() {
        for(model in NovelAiImageModel.entries) {
            val imported=metadata(",\"noise_schedule\":\"karras\"").copy(settings=metadata().settings.copy(
                model=model,noiseSchedule=NovelAiNoiseSchedule.KARRAS))
            val draft=NovelAiStudioDraft().applyImportedMetadata(imported,NovelAiStudioMetadataSelection())
            assertEquals("k_dpmpp_2m_sde",wire(draft)["sampler"]?.jsonPrimitive?.content)
            assertEquals("karras",wire(draft)["noise_schedule"]?.jsonPrimitive?.content)
        }
    }
    @Test fun importedFlagsAndTextSurviveRecipeHistoryAndWire() {
        val m=metadata(""", "prefer_brownian":false,"deliberate_euler_ancestral_bug":true,
          "v4_prompt":{"use_coords":true,"use_order":false,"caption":{"base_caption":" synthetic，base ","char_captions":[
          {"char_caption":" char，positive ","centers":[{"x":0.27,"y":0.62}]}]}},
          "v4_negative_prompt":{"use_coords":false,"use_order":false,"caption":{"base_caption":" negative，literal ","char_captions":[
          {"char_caption":" char，negative ","centers":[{"x":0.27,"y":0.62}]}]}}""")
        val draft=NovelAiStudioDraft(stylePrompt="old style").applyImportedMetadata(m,NovelAiStudioMetadataSelection())
        val recipe=codec.decodeFromString<NovelAiGenerationRecipe>(codec.encodeToString(draft.toRecipe()))
        val restored=NovelAiStudioDraft().applyHistoryRecipe(recipe,12,NovelAiHistoryApplyMode.FULL)
        val body=wire(restored)
        assertEquals(false,body["prefer_brownian"]?.jsonPrimitive?.boolean)
        assertEquals(true,body["deliberate_euler_ancestral_bug"]?.jsonPrimitive?.boolean)
        assertEquals(" negative，literal ",body["negative_prompt"]?.jsonPrimitive?.content)
        val positive=body.getValue("v4_prompt").jsonObject;val negative=body.getValue("v4_negative_prompt").jsonObject
        assertEquals(false,positive["use_order"]?.jsonPrimitive?.boolean);assertEquals(false,negative["use_order"]?.jsonPrimitive?.boolean)
        assertEquals(true,positive["use_coords"]?.jsonPrimitive?.boolean);assertEquals(false,negative["use_coords"]?.jsonPrimitive?.boolean)
        assertEquals(" synthetic，base ",positive.getValue("caption").jsonObject["base_caption"]?.jsonPrimitive?.content)
        val character=positive.getValue("caption").jsonObject.getValue("char_captions").jsonArray.single().jsonObject
        assertEquals(" char，positive ",character["char_caption"]?.jsonPrimitive?.content)
        assertEquals(0.27f,character.getValue("centers").jsonArray.single().jsonObject.getValue("x").jsonPrimitive.float)
    }
    @Test fun unsupportedSamplerAndModelAreRetainedAndPreventSending() {
        for ((key,value) in listOf("sampler" to "k_dpmpp_3m_sde","model" to "nai-diffusion-5-curated")) {
            val original=metadata();val text=if(key=="sampler") "k_dpmpp_2m_sde" else "nai-diffusion-4-5-full"
            val raw="""{"model":"nai-diffusion-4-5-full","width":832,"height":1216,"steps":28,"sampler":"k_dpmpp_2m_sde","seed":12,"prompt":"synthetic","uc":""}""".replace(text,value)
            val parsed=requireNotNull(NovelAiPngMetadataReader.parseStudioComment(raw,"synthetic.png"))
            val draft=NovelAiStudioDraft().applyImportedMetadata(parsed,NovelAiStudioMetadataSelection())
            assertTrue(value in codec.encodeToString(draft))
            assertNotNull(draft.activeSettings.validationError(0));assertFailsWith<IllegalStateException> { wire(draft) }
            assertNotNull(original)
        }
    }
    @Test fun unselectedImportsDoNotChangeSamplingOrTextModes() {
        val draft=NovelAiStudioDraft(basePrompt="manual，prompt",negativePrompt="manual，negative")
        val applied=draft.applyImportedMetadata(metadata(",\"prefer_brownian\":false"),NovelAiStudioMetadataSelection(
            positivePrompt=false,negativePrompt=false,generationSettings=false,seed=false,characterPrompts=NovelAiCharacterImportMode.OFF,imageGuidance=false))
        assertEquals(draft,applied)
        assertEquals("manual,negative",wire(applied)["negative_prompt"]?.jsonPrimitive?.content)
    }
    @Test fun malformedRecognizedValuesBlockInsteadOfUsingCurrentDefaults() {
        for (extra in listOf(",\"noise_schedule\":\"unknown\"",",\"prefer_brownian\":\"false\"",",\"scale\":200",",\"skip_cfg_above_sigma\":-1")) {
            val draft=NovelAiStudioDraft().applyImportedMetadata(metadata(extra),NovelAiStudioMetadataSelection())
            assertNotNull(draft.activeSettings.validationError(0),extra)
            assertFailsWith<IllegalStateException> { wire(draft) }
        }
    }
    @Test fun explicitNoiseIsValidatedAndDoesNotSilentlyResetOnSamplerChange() {
        val s=NovelAiGenerationSettings(noiseSource=NovelAiNoiseSource.EXPLICIT,noiseSchedule=NovelAiNoiseSchedule.NATIVE)
        assertNotNull(s.validationError(0))
        assertNotNull(s.copy(sampler=NovelAiSampler.DDIM).validationError(0))
        assertNull(s.copy(noiseSchedule=NovelAiNoiseSchedule.EXPONENTIAL).validationError(0))
    }
    @Test fun varietyIsOnlyV45EvenWhenEnhanceCarriesSigma() {
        for(model in NovelAiImageModel.entries) {
            val draft=NovelAiStudioDraft().withActiveSettings(NovelAiGenerationSettings(model=model,varietyPlus=true))
            val params=wire(draft)
            if(model==NovelAiImageModel.V4_5_FULL) assertEquals(58,params["skip_cfg_above_sigma"]?.jsonPrimitive?.int)
            else assertFalse("skip_cfg_above_sigma" in params)
            val enhanced=Json.parseToJsonElement(NovelAiImageService().buildRequestBody(
                draft.toPromptPlan(),draft.activeSettings.imageSize(),draft.activeSettings,
                NovelAiPreparedImageGuidance(action=NovelAiGenerationAction.IMAGE_TO_IMAGE,imageBase64="synthetic-not-executed"),
                NovelAiEnhanceRequestOptions(false,buildJsonObject { put("skip_cfg_above_sigma",58) })
            )).jsonObject.getValue("parameters").jsonObject
            assertEquals(model==NovelAiImageModel.V4_5_FULL,"skip_cfg_above_sigma" in enhanced)
        }
    }
    @Test fun missingFlagsKeepCurrentExplicitFalseAndInvalidSeedIsNotIgnored() {
        val first=NovelAiStudioDraft().applyImportedMetadata(metadata(",\"prefer_brownian\":false"),NovelAiStudioMetadataSelection())
        val next=first.applyImportedMetadata(metadata(),NovelAiStudioMetadataSelection())
        assertEquals(false,wire(next)["prefer_brownian"]?.jsonPrimitive?.boolean)
        val bad=metadata().copy(seed=null,seedInvalid=true)
        assertFailsWith<IllegalArgumentException> { first.applyImportedMetadata(bad,NovelAiStudioMetadataSelection()) }
        assertEquals(first.activeSettings.seed,first.applyImportedMetadata(bad,NovelAiStudioMetadataSelection(seed=false)).activeSettings.seed)
    }
    @Test fun emptyImportedNegativeDoesNotBecomeDefaultAndManualV5BaseStillUsesItsPolicy() {
        val draft=NovelAiStudioDraft().applyImportedMetadata(metadata().copy(negativePrompt=""),NovelAiStudioMetadataSelection())
        assertEquals("",wire(draft)["negative_prompt"]?.jsonPrimitive?.content)
        val p=NovelAiPromptPlan("sign says \"HELLO\"",listOf(NovelAiCharacterCaption("literal，character",DesignedCharacterCenter(.5f,.5f),literalPrompt=true)))
        val result=NovelAiV5TextPromptPolicy.apply(NovelAiPromptDelimiterPolicy.normalizeForRequest(p),NovelAiImageModel.V5_FULL)
        assertTrue("teXt:" in result.baseCaption)
        assertEquals("literal，character",result.characterCaptions.single().prompt)
    }
    @Test fun knownUnsupportedGenerationFieldsBlockButInformationAndEquivalentDefaultsDoNot() {
        for(extra in listOf(",\"extra_noise_seed\":13",",\"uncond_scale\":0.4",",\"skip_cfg_below_sigma\":10",
            ",\"sm\":true",",\"sm_dyn\":true",",\"autoSmea\":true",",\"dynamic_thresholding\":true",
            ",\"legacy\":true",",\"legacy_v3_extend\":true")) {
            val d=NovelAiStudioDraft().applyImportedMetadata(metadata(extra),NovelAiStudioMetadataSelection())
            assertNotNull(d.activeSettings.validationError(0),extra)
        }
        val d=NovelAiStudioDraft().applyImportedMetadata(metadata(""", "extra_noise_seed":12,"uncond_scale":1,"skip_cfg_below_sigma":null,
            "sm":false,"sm_dyn":false,"autoSmea":false,"dynamic_thresholding":false,"legacy":false,"legacy_v3_extend":false,
            "model_hash":"synthetic-info","signed_hash":"synthetic-info","tag_hint":"synthetic-info","Generation time":10"""),NovelAiStudioMetadataSelection())
        assertNull(d.activeSettings.validationError(0));assertEquals("k_dpmpp_2m_sde",wire(d)["sampler"]?.jsonPrimitive?.content)
    }
    @Test fun exportedGoldenFixturesUseTheSameTypedImportAndSerializer() {
        val text=requireNotNull(javaClass.getResourceAsStream("/nai-parameter-compatibility-fixtures.json")).bufferedReader().use { it.readText() }
        val fixtures=Json.parseToJsonElement(text).jsonObject.getValue("cases").jsonArray
        for(item in fixtures) {
            val c=item.jsonObject;val id=c.getValue("id").jsonPrimitive.content
            if(id=="import-false-true-literal") {
                val expected=" synthetic\uFF0Cbase "
                assertEquals(expected,c.getValue("input").jsonObject.getValue("prompt").jsonPrimitive.content,id)
                assertEquals(expected,c.getValue("expectedBaseCaption").jsonPrimitive.content,id)
            }
            val d=if(c.getValue("kind").jsonPrimitive.content=="settings") {
                NovelAiStudioDraft().withActiveSettings(codec.decodeFromString<NovelAiGenerationSettings>(c.getValue("input").toString()))
            } else NovelAiStudioDraft().applyImportedMetadata(requireNotNull(NovelAiPngMetadataReader.parseStudioComment(c.getValue("input").toString(),"synthetic.png")),NovelAiStudioMetadataSelection())
            if(c["reject"]?.jsonPrimitive?.boolean==true) {
                assertNotNull(d.activeSettings.validationError(0),id)
                c["preservedSampler"]?.let { assertEquals(it.jsonPrimitive.content,d.activeSettings.parameterReplay?.unsupportedSampler,id) }
                assertFailsWith<IllegalStateException>(id) { wire(d) };continue
            }
            val actual=wire(d)
            for((key,value) in c.getValue("expected").jsonObject)assertEquals(value,actual[key],"$id:$key")
            for(key in c["absent"]?.jsonArray.orEmpty())assertFalse(key.jsonPrimitive.content in actual,id)
            for((key,value) in c["expectedNumbers"]?.jsonObject.orEmpty())assertEquals(value.jsonPrimitive.float,actual.getValue(key).jsonPrimitive.float,0.0001f,"$id:$key")
            c["expectedNegativeUseOrder"]?.let { assertEquals(it,actual.getValue("v4_negative_prompt").jsonObject["use_order"],id) }
            c["expectedBaseCaption"]?.let { assertEquals(it,actual.getValue("v4_prompt").jsonObject.getValue("caption").jsonObject["base_caption"],id) }
        }
    }
    @Test fun sigmaOriginsAndDdimWirePolicyDoNotRewriteOldOrImportedValues() {
        val fresh=NovelAiStudioDraft().activeSettings.copy(varietyPlus=true,customWidth=1664,customHeight=1216)
        val d=NovelAiStudioDraft().withActiveSettings(fresh)
        assertEquals(82.024386f,wire(d).getValue("skip_cfg_above_sigma").jsonPrimitive.float,0.0001f)
        val legacy=fresh.copy(varietyRescale=false)
        assertEquals(58,wire(d.withActiveSettings(legacy)).getValue("skip_cfg_above_sigma").jsonPrimitive.int)
        val imported=NovelAiStudioDraft().applyImportedMetadata(metadata(",\"skip_cfg_above_sigma\":99"),NovelAiStudioMetadataSelection())
        assertEquals(99f,wire(imported).getValue("skip_cfg_above_sigma").jsonPrimitive.float)
        val ddim=d.withActiveSettings(fresh.copy(sampler=NovelAiSampler.DDIM))
        assertFalse("noise_schedule" in wire(ddim))
        assertEquals("karras",wire(ddim.withActiveSettings(ddim.activeSettings.copy(noiseSource=NovelAiNoiseSource.LEGACY)))["noise_schedule"]?.jsonPrimitive?.content)
        assertEquals("karras",wire(ddim.withActiveSettings(ddim.activeSettings.copy(model=NovelAiImageModel.V5_FULL)))["noise_schedule"]?.jsonPrimitive?.content)
    }
    @Test fun exactSupportedInpaintingModelRequiresMatchingActionNotSubstringFallback() {
        for(model in NovelAiImageModel.entries) {
            val input="""{"model":"${model.apiId}-inpainting","action":"infill","width":832,"height":1216,
                "steps":28,"sampler":"k_dpmpp_2m_sde","seed":9,"prompt":"synthetic","uc":""}"""
            val parsed=requireNotNull(NovelAiPngMetadataReader.parseStudioComment(input,"synthetic.png"))
            assertEquals(model,parsed.settings.model);assertNull(parsed.settings.parameterReplay?.unsupportedModel)
            assertEquals(NovelAiGenerationAction.INPAINT,parsed.imageGuidance.action)
            val incompatible=requireNotNull(NovelAiPngMetadataReader.parseStudioComment(input.replace("infill","generate"),"synthetic.png"))
            assertNotNull(incompatible.settings.parameterReplay?.unsupportedModel)
        }
    }
    @Test fun v5Allows32ButRejects33WithoutChangingTokenizerOrV45Limit() {
        val s=NovelAiGenerationSettings(model=NovelAiImageModel.V5_FULL)
        assertNull(s.validationError(32));assertNotNull(s.validationError(33))
        assertEquals(1471,s.model.promptTokenLimit);assertEquals(NovelAiTokenizerKind.QWEN,s.model.tokenizerKind)
        assertNull(s.copy(model=NovelAiImageModel.V4_5_FULL).validationError(6))
        assertNotNull(s.copy(model=NovelAiImageModel.V4_5_FULL).validationError(7))
        val chars=List(32) { NovelAiCharacterCaption("synthetic-$it",DesignedCharacterCenter(.5f,.5f),"synthetic-negative-$it") }
        val body=Json.parseToJsonElement(NovelAiImageService().buildRequestBody(NovelAiPromptPlan("synthetic",chars),s.imageSize(),s)).jsonObject.getValue("parameters").jsonObject
        for(key in listOf("v4_prompt","v4_negative_prompt"))assertEquals(32,body.getValue(key).jsonObject.getValue("caption").jsonObject.getValue("char_captions").jsonArray.size)
        assertFailsWith<IllegalStateException> {
            NovelAiImageService().buildRequestBody(NovelAiPromptPlan("synthetic",chars+chars.first()),s.imageSize(),s)
        }
        val encoded=Json.parseToJsonElement(codec.encodeToString(s)).jsonObject
        assertEquals("V5_FULL",encoded.getValue("model").jsonPrimitive.content)
        assertFalse("maxCharacters" in encoded);assertFalse("promptTokenLimit" in encoded)
    }
    @Test fun missingGuidanceMediaIsVisibleRejectionRatherThanTextGenerationFallback() {
        for(action in listOf(NovelAiGenerationAction.IMAGE_TO_IMAGE,NovelAiGenerationAction.INPAINT)) {
            val m=metadata().copy(imageGuidance=NovelAiImportedImageGuidance(action=action))
            assertFailsWith<IllegalArgumentException> { NovelAiStudioDraft().applyImportedMetadata(m,NovelAiStudioMetadataSelection()) }
            val explicitTextOnly=NovelAiStudioDraft().applyImportedMetadata(m,NovelAiStudioMetadataSelection(imageGuidance=false))
            assertEquals(NovelAiGenerationAction.TEXT_TO_IMAGE,explicitTextOnly.imageGuidance.action)
            assertEquals(m.positivePrompt,explicitTextOnly.basePrompt)
        }
    }
    @Test fun unselectedCharactersDoNotImportTheirCenterProblem() {
        val m=metadata(",\"v4_prompt\":{\"caption\":{\"base_caption\":\"synthetic\",\"char_captions\":[{\"char_caption\":\"role\",\"centers\":[{\"x\":0.1,\"y\":0.2},{\"x\":0.8,\"y\":0.9}]}]}}")
        assertTrue("character_centers" in m.settings.parameterReplay!!.problems)
        assertFailsWith<IllegalArgumentException> { NovelAiStudioDraft().applyImportedMetadata(m,NovelAiStudioMetadataSelection()) }
        val d=NovelAiStudioDraft().applyImportedMetadata(m,NovelAiStudioMetadataSelection(characterPrompts=NovelAiCharacterImportMode.OFF))
        assertNull(d.activeSettings.validationError(0));wire(d)
        val other=m.copy(settings=m.settings.copy(parameterReplay=m.settings.parameterReplay!!.copy(problems=listOf("character_centers","params_version"))))
        assertEquals(listOf("params_version"),NovelAiStudioDraft().applyImportedMetadata(other,NovelAiStudioMetadataSelection(characterPrompts=NovelAiCharacterImportMode.OFF)).activeSettings.parameterReplay!!.problems)
    }
    @Test fun unknownActionRejectsOnlySelectedImageGuidance() {
        for(raw in listOf("\"future-action\"","null","{}")) {
            val m=metadata(",\"action\":$raw")
            assertNotNull(m.imageGuidance.unsupportedAction)
            assertFailsWith<IllegalArgumentException> { NovelAiStudioDraft().applyImportedMetadata(m,NovelAiStudioMetadataSelection()) }
            val d=NovelAiStudioDraft().applyImportedMetadata(m,NovelAiStudioMetadataSelection(imageGuidance=false))
            assertNull(d.activeSettings.validationError(0));wire(d)
        }
        for(extra in listOf("",",\"action\":\"generate\"")) {
            val m=metadata(extra);assertNull(m.imageGuidance.unsupportedAction)
            assertEquals(NovelAiGenerationAction.TEXT_TO_IMAGE,NovelAiStudioDraft().applyImportedMetadata(m,NovelAiStudioMetadataSelection()).imageGuidance.action)
        }
    }
    @Test fun explicitV5SigmaRejectsOnlyWhenParametersAreSelected() {
        val m=metadata(",\"skip_cfg_above_sigma\":99").let { it.copy(settings=it.settings.copy(model=NovelAiImageModel.V5_FULL)) }
        val d=NovelAiStudioDraft().applyImportedMetadata(m,NovelAiStudioMetadataSelection())
        assertEquals(99f,d.activeSettings.varietySigma);assertNotNull(d.activeSettings.validationError(0))
        assertFailsWith<IllegalStateException> { wire(d) }
        val base=NovelAiStudioDraft().copy(selectedModel=NovelAiImageModel.V5_FULL,followDefaultNovelAiImageModel=false)
        val text=base.applyImportedMetadata(m,NovelAiStudioMetadataSelection(generationSettings=false))
        assertNull(text.activeSettings.validationError(0));assertFalse("skip_cfg_above_sigma" in wire(text))
        val existing=base.withActiveSettings(base.activeSettings.copy(varietyPlus=true,varietySigma=99f))
        assertNull(existing.activeSettings.validationError(0));assertFalse("skip_cfg_above_sigma" in wire(existing))
        val cleared=d.applyImportedMetadata(metadata(",\"skip_cfg_above_sigma\":null").let { it.copy(settings=it.settings.copy(model=NovelAiImageModel.V5_FULL)) },NovelAiStudioMetadataSelection())
        assertNull(cleared.activeSettings.validationError(0));assertEquals(99f,m.settings.varietySigma)
    }
    @Test fun manualPositionSelectionSupersedesOnlyCoordinateReplay() {
        val base=NovelAiStudioDraft(characters=listOf(NovelAiCharacterPromptDraft(prompt="synthetic-role"))).withActiveSettings(NovelAiStudioDraft().activeSettings.copy(parameterReplay=NovelAiParameterReplay(positiveUseCoords=false,negativeUseCoords=true,positiveUseOrder=false,preferBrownian=false)))
        for(enabled in listOf(true,false)) {
            val d=base.withManualCharacterPositions(enabled);val replay=d.activeSettings.parameterReplay!!
            assertNull(replay.positiveUseCoords);assertNull(replay.negativeUseCoords)
            assertEquals(false,replay.positiveUseOrder);assertEquals(false,replay.preferBrownian)
            val body=wire(d)
            assertEquals(enabled,body.getValue("v4_prompt").jsonObject.getValue("use_coords").jsonPrimitive.boolean)
            assertEquals(enabled,body.getValue("v4_negative_prompt").jsonObject.getValue("use_coords").jsonPrimitive.boolean)
        }
        assertEquals(false,base.activeSettings.parameterReplay!!.positiveUseCoords)
    }
    @Test fun v5ReferenceImportRejectsInsteadOfSilentlyUsingNone() {
        val source=metadata()
        val m=source.copy(settings=source.settings.copy(model=NovelAiImageModel.V5_FULL),
            imageGuidance=NovelAiImportedImageGuidance(preciseImageBase64="synthetic-not-decoded"))
        assertFailsWith<IllegalArgumentException> { NovelAiStudioDraft().applyImportedMetadata(m,NovelAiStudioMetadataSelection()) }
        val textOnly=NovelAiStudioDraft().applyImportedMetadata(m,NovelAiStudioMetadataSelection(imageGuidance=false))
        assertEquals(NovelAiImageModel.V5_FULL,textOnly.selectedModel)
        assertEquals(NovelAiReferenceMode.NONE,textOnly.imageGuidance.referenceMode)
    }
}
