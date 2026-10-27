package io.github.misakapiano.ccbstudio.core

import io.github.misakapiano.ccbstudio.assets.AssetStore
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.ByteArrayInputStream
import java.util.UUID

@Serializable data class TypesetNamedMaskPreset(val id:String=newId(),val name:String,val settings:MaskToolSettings)
@Serializable data class TypesetEditorExtras(val version:Int=1,val namedMasks:List<TypesetNamedMaskPreset> = emptyList(),val originalSorted:Boolean=true,val originalCopies:Boolean=false,val autoMask:TypesetAutoMaskSettings?=null,val lastProjectId:String?=null,val projectSelectionRecorded:Boolean=false,val projectRecords:List<TypesetProjectRecord> = emptyList(),val outputs:List<TypesetManagedOutput> = emptyList(),val retirements:List<TypesetSnapshotRetirement> = emptyList()) {
    fun validate(){
        if(version!=1)throw DataProblem("UNSUPPORTED_VERSION","Unknown editor preset extension retained")
        require(namedMasks.size<=200 && namedMasks.map{it.id}.distinct().size==namedMasks.size)
        autoMask?.validate()
        lastProjectId?.let(UUID::fromString)
        require(projectRecords.size<=1000&&projectRecords.distinctBy{it.projectId}.size==projectRecords.size)
        projectRecords.forEach{it.validate()}
        require(outputs.size<=10000&&outputs.distinctBy{it.id}.size==outputs.size);outputs.forEach{it.validate()}
        require(retirements.size<=1000&&retirements.distinctBy{it.projectId}.size==retirements.size);retirements.forEach{it.validate()}
        namedMasks.forEach{UUID.fromString(it.id);require(it.name.isNotBlank()&&it.name.length<=100);it.settings.validate()}
    }
    fun saveMask(name:String,settings:MaskToolSettings,replace:String?=null):TypesetEditorExtras {
        require(name.isNotBlank()&&name.length<=100);if(replace!=null)require(namedMasks.any{it.id==replace})
        val entry=TypesetNamedMaskPreset(replace?:newId(),name,settings)
        return copy(namedMasks=if(replace==null)namedMasks+entry else namedMasks.map{if(it.id==replace)entry else it}).also{it.validate()}
    }
    fun renameMask(id:String,name:String):TypesetEditorExtras {
        require(namedMasks.any{it.id==id})
        return copy(namedMasks=namedMasks.map{if(it.id==id)it.copy(name=name)else it}).also{it.validate()}
    }
    fun removeMask(id:String)=copy(namedMasks=namedMasks.filterNot{it.id==id}).also{it.validate()}
}

/** Reuse the existing immutable preset-exchange asset and journal pointer. No new
 * workspace-state keys: older Studio readers retain this unknown exchange extension. */
object TypesetEditorExtrasStore {
    const val KEY="ccbStudioEditor"
    fun inspect(root:JsonObject):TypesetEditorExtras=root[KEY]?.let{documentJson.decodeFromJsonElement<TypesetEditorExtras>(it)}?.also{it.validate()}?:TypesetEditorExtras(
        originalSorted=root["prefs"]?.jsonObject?.get("exportVariantSelection")?.jsonObject?.get("original")?.jsonPrimitive?.booleanOrNull?:true)
    fun load(workspace:StudioWorkspace):TypesetEditorExtras=workspace.operation {
        val id=workspace.state.typesetPreferences.sourceExchangeId?:return@operation TypesetEditorExtras()
        val asset=workspace.assets.read(UUID.fromString(id)).require()
        require(asset.request().project().contentEquals(TypesetSourceExchange.TAG.toByteArray()))
        inspect(documentJson.parseToJsonElement(workspace.bytes(id).decodeToString()).jsonObject)
    }
    fun save(workspace:StudioWorkspace,value:TypesetEditorExtras,projects:List<TypesetProject>?=null,organization:GenerationOrganization?=null)=workspace.operation {
        value.validate()
        // Validate the old extension before replacing it; unknown versions are retained.
        load(workspace)
        val raw=documentJson.parseToJsonElement(TypesetSourceExchange.exportPresets(workspace).decodeToString()).jsonObject
        val actual=value.copy(originalCopies="original" in workspace.state.typesetPreferences.exportVariants)
        val root=JsonObject(raw+mapOf(KEY to documentJson.encodeToJsonElement(actual)))
        val bytes=documentJson.encodeToString(root).toByteArray();require(bytes.size<=4*1024*1024)
        val parent=workspace.state.typesetPreferences.sourceExchangeId?.let(UUID::fromString)
        val id=UUID.randomUUID()
        workspace.assets.put(AssetStore.Request(id,"Studio.typesetter-presets.json",if(parent==null)AssetStore.Kind.ORIGINAL else AssetStore.Kind.DERIVED,parent,byteArrayOf(),TypesetSourceExchange.TAG.toByteArray()),ByteArrayInputStream(bytes))
        workspace.assets.read(id).require()
        workspace.save(workspace.state.copy(typesetProjects=projects?:workspace.state.typesetProjects,generationOrganization=organization?:workspace.state.generationOrganization,typesetPreferences=workspace.state.typesetPreferences.copy(sourceExchangeId=id.toString())))
        actual
    }
}
