package io.github.misakapiano.ccbstudio.core

import kotlinx.serialization.json.*
import java.nio.file.*
import kotlin.test.*

class TypesetSourceExchangeTest {
    @Test fun exchangeFileFailureAndConcurrentDestinationNeverOverwrite() {
        val parent=Files.createDirectories(root());val path=parent.resolve("queue.json");val bytes=source()
        assertFails{TypesetSourceExchange.saveFile(path,bytes){error("before publish")}};assertFalse(Files.exists(path))
        assertFails{TypesetSourceExchange.saveFile(path,bytes){Files.writeString(path,"concurrent file")}};assertEquals("concurrent file",Files.readString(path))
        assertFails{TypesetSourceExchange.saveFile(path,bytes)};assertEquals("concurrent file",Files.readString(path))
        val success=parent.resolve("new.json");TypesetSourceExchange.saveFile(success,bytes);assertContentEquals(bytes,Files.readAllBytes(success))
    }
    private fun legacyImport(w:StudioWorkspace,bytes:ByteArray):TypesetProject {
        val inspection=TypesetSourceExchange.inspect(bytes);val id=java.util.UUID.randomUUID()
        w.assets.put(io.github.misakapiano.ccbstudio.assets.AssetStore.Request(id,"source.typesetter-workspace.json",io.github.misakapiano.ccbstudio.assets.AssetStore.Kind.ORIGINAL,null,byteArrayOf(),TypesetSourceExchange.TAG.toByteArray()),java.io.ByteArrayInputStream(bytes))
        val pages=inspection.document.pages.mapIndexed{i,p->p.copy(assetId=w.importImage(p.name,inspection.originals[i]))}
        return TypesetRepository(w).importDocument(inspection.name,inspection.document.copy(pages=pages),id.toString(),inspection.activeIndex)
    }
    @Test fun actualSourcePresetExportMatchesStyleAndDefaults() {
        val bytes=javaClass.getResourceAsStream("/typeset/source-presets.json")!!.readBytes();assertEquals("60c6a6248dde41c8d870190861cd6a042373cf4ad6f8e6ef6a161f6de77894bf",sha256(bytes))
        val p=TypesetSourceExchange.inspectPresets(bytes).preferences;assertEquals("heavy_mosaic",p.preferredMaskSlot);assertEquals(48.0,p.textPresets.single().text.style.fontSize);assertTrue(p.textPresets.single().text.vertical)
    }
    @Test fun legacyPresetMigrationMergeReplaceAndUnknownFieldsRoundTrip() {
        val bytes="""{"app":"image_typesetter_frontend_presets","version":1,"prefs":{"preferredPreset":"user_custom","userMaskPreset":{"maskType":"white","maskStrength":25},"unknownSetting":{"value":7},"textPresets":[{"id":"legacy-id","name":"源竖排","style":{"fontSize":48,"writingMode":"vertical"}}],"selectedTextPresetId":"legacy-id"}}""".toByteArray()
        val inspection=TypesetSourceExchange.inspectPresets(bytes);assertEquals("light_mosaic",inspection.preferences.preferredMaskSlot);assertEquals("white",inspection.preferences.resolveMask("light_mosaic").effect)
        assertEquals(inspection.preferences.textPresets.single().id,inspection.preferences.selectedTextPresetId)
        StudioWorkspace.openStudio(root()).use{w->w.save(w.state.copy(typesetPreferences=TypesetPreferences().saveText("已有",TypesetText())))
            TypesetSourceExchange.importPresets(w,inspection,true);assertEquals(2,w.state.typesetPreferences.textPresets.size)
            val exported=TypesetSourceExchange.exportPresets(w);val json=documentJson.parseToJsonElement(exported.decodeToString()).jsonObject.getValue("prefs").jsonObject
            assertEquals(7,json.getValue("unknownSetting").jsonObject.getValue("value").jsonPrimitive.int);assertFalse("userMaskPreset" in json)
            val again=TypesetSourceExchange.inspectPresets(exported);assertEquals(2,again.preferences.textPresets.size);assertEquals(48.0,again.preferences.textPresets.last().text.style.fontSize)
            TypesetSourceExchange.importPresets(w,inspection,false);assertEquals(1,w.state.typesetPreferences.textPresets.size)
            assertContentEquals(bytes,w.bytes(w.state.typesetPreferences.sourceExchangeId!!))
        }
    }
    private fun root()=Path.of(System.getProperty("preview.test.root")).resolve(newId())
    private fun source()=javaClass.getResourceAsStream("/typeset/source-workspace.json")!!.readBytes()
    @Test fun actualSourceExportImportsEditableContentWithoutRawAcrossBackup() {
        val original=source();assertEquals("45a6215884ede5f7193f88fc2a219f98aae09461b60d44546ad575b7fbf9958c",sha256(original))
        val inspection=TypesetSourceExchange.inspect(original);assertEquals(1,inspection.document.pages.size)
        val page=inspection.document.pages.single();assertEquals(3,page.masks.size);assertTrue(page.texts.single().vertical);assertEquals("[b]源文字[/]「竖排」",page.texts.single().text);assertEquals("源旁白，保留。",page.caption)
        val r=root();val backup=StudioWorkspace.openStudio(r).use{w->val p=TypesetSourceExchange.import(w,inspection);val d=TypesetRepository(w).load(p)
            assertContentEquals(inspection.originals.single(),w.bytes(d.pages.single().assetId));assertNull(p.sourceExchangeId);assertTrue(w.catalog().none{it.request().project().contentEquals(TypesetSourceExchange.TAG.toByteArray())});w.backup()}
        val restored=root();StudioRestore.restore(backup,restored);StudioWorkspace.openStudio(restored).use{w->val p=w.state.typesetProjects.single();assertNull(p.sourceExchangeId);assertContentEquals(inspection.originals.single(),w.bytes(TypesetRepository(w).load(p).pages.single().assetId));assertEquals(page.caption,TypesetRepository(w).load(p).pages.single().caption)}
    }
    @Test fun editedExportRoundTripsStudioExtensionsAndRetainsUnknownSourceFields() {
        val raw=documentJson.parseToJsonElement(source().decodeToString()).jsonObject
        val augmented=JsonObject(raw+mapOf("futureField" to JsonPrimitive("retained"),"entries" to JsonArray(raw.getValue("entries").jsonArray.map{JsonObject(it.jsonObject+mapOf("futureEntry" to JsonPrimitive(72)))}))).toString().toByteArray()
        StudioWorkspace.openStudio(root()).use{w->val repository=TypesetRepository(w);var p=legacyImport(w,augmented)
            p=repository.update(p.id,p.revision){d->d.copy(pages=d.pages.map{page->page.copy(texts=page.texts.map{it.copy(text="编辑后 [b]正文[/]",locked=true)},layerOrder=listOf(page.texts.single().id)+page.masks.map{it.id},caption="修改后旁白")})}
            val document=repository.load(p);val bytes=TypesetSourceExchange.export(w,p,document);val parsed=documentJson.parseToJsonElement(bytes.decodeToString()).jsonObject
            assertEquals("retained",parsed.getValue("futureField").jsonPrimitive.content);assertEquals(72,parsed.getValue("entries").jsonArray[0].jsonObject.getValue("futureEntry").jsonPrimitive.int)
            val imported=TypesetSourceExchange.import(w,TypesetSourceExchange.inspect(bytes));val next=repository.load(imported);assertEquals(document.pages.single().texts,next.pages.single().texts);assertEquals(document.pages.single().layerOrder,next.pages.single().layerOrder)
            assertContentEquals(w.bytes(document.pages.single().assetId),w.bytes(next.pages.single().assetId));assertNotEquals(p.id,imported.id)
            val out=Path.of("build/typeset-exchange");Files.createDirectories(out);Files.write(out.resolve("suite-workspace.json"),bytes)
        }
    }
    @Test fun unknownVersionRemoteDataAndCorruptionRefusedBeforeMutation() {
        val raw=documentJson.parseToJsonElement(source().decodeToString()).jsonObject
        val future=JsonObject(raw+mapOf("version" to JsonPrimitive(22))).toString().toByteArray()
        assertEquals("UNSUPPORTED_VERSION",assertFailsWith<DataProblem>{TypesetSourceExchange.inspect(future)}.status)
        val bad=JsonObject(raw+mapOf("entries" to JsonArray(raw.getValue("entries").jsonArray.map{JsonObject(it.jsonObject+mapOf("blobDataUrl" to JsonPrimitive("https://example.invalid/image.png")))}))).toString().toByteArray()
        assertFails{TypesetSourceExchange.inspect(bad)};assertFails{TypesetSourceExchange.inspect(byteArrayOf(0xff.toByte()))}
        val futureExtension=JsonObject(raw+mapOf("entries" to JsonArray(raw.getValue("entries").jsonArray.map{JsonObject(it.jsonObject+mapOf("ccbStudio" to buildJsonObject{put("version",2)}))}))).toString().toByteArray()
        assertEquals("UNSUPPORTED_VERSION",assertFailsWith<DataProblem>{TypesetSourceExchange.inspect(futureExtension)}.status)
    }
}
