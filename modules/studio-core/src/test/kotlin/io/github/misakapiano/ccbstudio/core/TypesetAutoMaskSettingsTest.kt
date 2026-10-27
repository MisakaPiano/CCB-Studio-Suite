package io.github.misakapiano.ccbstudio.core

import java.nio.file.Path
import kotlin.test.*
import kotlinx.serialization.json.*

class TypesetAutoMaskSettingsTest {
    @Test fun localModelConfigurationSurvivesReopenBackupAndIsNotPresetExchangeOrGalleryContent(){
        val root=Path.of(System.getProperty("preview.test.root")).resolve(newId());val restored=root.resolveSibling(newId());val bytes=ByteArray(96){it.toByte()}
        lateinit var saved:TypesetAutoMaskSettings;lateinit var backup:Path
        StudioWorkspace.openStudio(root).use{w->
            val id=TypesetAutoMaskResources.retain(w,"synthetic-resource.onnx",bytes)
            assertEquals(id,TypesetAutoMaskResources.retain(w,"same.onnx",bytes));assertEquals(1,w.catalog().size);assertTrue(SharedAssets.images(w).isEmpty())
            saved=TypesetAutoMaskSettings(id,sha256(bytes),listOf("synthetic_target"),confidence=.45,effect="blur",strength=24,opacity=.7,selectedPages=true)
            val lastProject=newId()
            TypesetEditorExtrasStore.save(w,TypesetEditorExtras().copy(autoMask=saved,lastProjectId=lastProject))
            TypesetEditorExtrasStore.save(w,TypesetEditorExtrasStore.load(w).saveMask("existing preset",MaskToolSettings()))
            assertEquals(saved,TypesetEditorExtrasStore.load(w).autoMask)
            val exchange=TypesetSourceExchange.exportPresets(w)
            assertNull(TypesetEditorExtrasStore.inspect(documentJson.parseToJsonElement(exchange.decodeToString()).let{it as kotlinx.serialization.json.JsonObject}).autoMask)
            assertNull(TypesetEditorExtrasStore.inspect(documentJson.parseToJsonElement(exchange.decodeToString()).let{it as kotlinx.serialization.json.JsonObject}).lastProjectId)
            TypesetSourceExchange.importPresets(w,TypesetSourceExchange.inspectPresets(exchange),false)
            assertEquals(saved,TypesetEditorExtrasStore.load(w).autoMask);assertContentEquals(bytes,TypesetAutoMaskResources.read(w,saved).second)
            assertEquals(lastProject,TypesetEditorExtrasStore.load(w).lastProjectId)
            val raw=documentJson.parseToJsonElement(exchange.decodeToString()).jsonObject
            val foreign=JsonObject(raw+mapOf(TypesetEditorExtrasStore.KEY to documentJson.encodeToJsonElement(TypesetEditorExtras.serializer(),TypesetEditorExtras(autoMask=saved,lastProjectId=lastProject))))
            val foreignBytes=documentJson.encodeToString(JsonObject.serializer(),foreign).toByteArray()
            StudioWorkspace.openStudio(root.resolveSibling(newId())).use{empty->
                TypesetSourceExchange.importPresets(empty,TypesetSourceExchange.inspectPresets(foreignBytes),false)
                assertNull(TypesetEditorExtrasStore.load(empty).autoMask)
                assertNull(TypesetEditorExtrasStore.load(empty).lastProjectId,"An imported preset must not activate another workspace's project")
            }
            backup=w.backup()
        }
        StudioWorkspace.openStudio(root).use{w->assertEquals(saved,TypesetEditorExtrasStore.load(w).autoMask);assertContentEquals(bytes,TypesetAutoMaskResources.read(w,saved).second)}
        StudioRestore.restore(backup,restored)
        StudioWorkspace.openStudio(restored).use{w->assertEquals(saved,TypesetEditorExtrasStore.load(w).autoMask);assertContentEquals(bytes,TypesetAutoMaskResources.read(w,saved).second);assertTrue(SharedAssets.images(w).isEmpty())}
    }
}
