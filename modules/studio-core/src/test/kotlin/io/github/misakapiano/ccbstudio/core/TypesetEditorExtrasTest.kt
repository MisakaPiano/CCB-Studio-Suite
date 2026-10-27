package io.github.misakapiano.ccbstudio.core

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.nio.file.Path
import kotlin.test.*

class TypesetEditorExtrasTest {
    private fun root()=Path.of(System.getProperty("preview.test.root")).resolve(newId())
    @Test fun namedMaskSnapshotsKeepKnownPreferencesAndOriginalExchangeBytes(){
        StudioWorkspace.openStudio(root()).use{w->
            val prefs=TypesetPreferences(maskOverrides=mapOf("heavy_mosaic" to MaskToolSettings(strength=40))).saveText("对白",TypesetText(style=TypesetStyle(fontSize=36.0)))
            w.save(w.state.copy(typesetPreferences=prefs))
            val raw=documentJson.parseToJsonElement(TypesetSourceExchange.exportPresets(w).decodeToString()).jsonObject
            val bytes=documentJson.encodeToString(JsonObject(raw+mapOf("unknownSourceField" to JsonPrimitive("preserve-me")))).toByteArray()
            TypesetSourceExchange.importPresets(w,TypesetSourceExchange.inspectPresets(bytes),false)
            val known=w.state.typesetPreferences;val source=known.sourceExchangeId!!
            var extras=TypesetEditorExtrasStore.load(w).saveMask("柔和模糊",MaskToolSettings(effect="blur",strength=24))
            TypesetEditorExtrasStore.save(w,extras)
            val id=extras.namedMasks.single().id;val snapshot=w.state.typesetPreferences.sourceExchangeId!!;val snapshotBytes=w.bytes(snapshot)
            extras=extras.renameMask(id,"对白区域").saveMask("对白区域",MaskToolSettings(strength=32),id)
            TypesetEditorExtrasStore.save(w,extras)
            assertEquals(extras,TypesetEditorExtrasStore.load(w))
            assertEquals(known.copy(sourceExchangeId=w.state.typesetPreferences.sourceExchangeId),w.state.typesetPreferences)
            assertContentEquals(bytes,w.bytes(source));assertContentEquals(snapshotBytes,w.bytes(snapshot))
            val exported=documentJson.parseToJsonElement(TypesetSourceExchange.exportPresets(w).decodeToString()).jsonObject
            assertEquals("preserve-me",exported.getValue("unknownSourceField").jsonPrimitive.content)
            assertEquals(extras,TypesetEditorExtrasStore.inspect(exported))
            // Global journal schema remains exactly the old preference schema.
            assertEquals(documentJson.parseToJsonElement(documentJson.encodeToString(known)).jsonObject.keys,
                documentJson.parseToJsonElement(documentJson.encodeToString(w.state.typesetPreferences)).jsonObject.keys)
            TypesetEditorExtrasStore.save(w,extras.removeMask(id));assertTrue(TypesetEditorExtrasStore.load(w).namedMasks.isEmpty())
            assertEquals(40,w.state.typesetPreferences.resolveMask("heavy_mosaic").strength)
        }
    }
    @Test fun extensionSurvivesRestartBackupAndMergeWithoutIdCollisions(){
        val first=root();val restored=root();lateinit var backup:Path;lateinit var exchange:ByteArray
        val initial=TypesetEditorExtras().saveMask("画笔",MaskToolSettings(effect="blur",size=90.0))
        StudioWorkspace.openStudio(first).use{w->TypesetEditorExtrasStore.save(w,initial);exchange=TypesetSourceExchange.exportPresets(w);backup=w.backup()}
        StudioWorkspace.openStudio(first).use{w->assertEquals(initial,TypesetEditorExtrasStore.load(w))}
        StudioRestore.restore(backup,restored)
        StudioWorkspace.openStudio(restored).use{w->
            assertEquals(initial,TypesetEditorExtrasStore.load(w))
            TypesetSourceExchange.importPresets(w,TypesetSourceExchange.inspectPresets(exchange),true)
            val merged=TypesetEditorExtrasStore.load(w);assertEquals(2,merged.namedMasks.size);assertEquals(2,merged.namedMasks.map{it.id}.distinct().size)
            assertTrue(merged.namedMasks.all{it.settings==initial.namedMasks.single().settings})
            TypesetSourceExchange.importPresets(w,TypesetSourceExchange.inspectPresets(exchange),false)
            assertEquals(initial,TypesetEditorExtrasStore.load(w))
        }
    }
    @Test fun unknownVersionAndInvalidNamesFailBeforeChangingPointers(){
        StudioWorkspace.openStudio(root()).use{w->
            val extras=TypesetEditorExtras().saveMask("保存",MaskToolSettings());TypesetEditorExtrasStore.save(w,extras)
            val before=w.state.typesetPreferences;val raw=documentJson.parseToJsonElement(TypesetSourceExchange.exportPresets(w).decodeToString()).jsonObject
            val future=JsonObject(raw+mapOf(TypesetEditorExtrasStore.KEY to buildJsonObject{put("version",2)}))
            assertFailsWith<DataProblem>{TypesetSourceExchange.inspectPresets(documentJson.encodeToString(future).toByteArray())}
            assertFailsWith<DataProblem>{TypesetEditorExtrasStore.save(w,extras.copy(version=2))}
            assertFailsWith<IllegalArgumentException>{extras.renameMask(extras.namedMasks.single().id,"")}
            assertEquals(before,w.state.typesetPreferences);assertEquals(extras,TypesetEditorExtrasStore.load(w))
        }
    }
}
