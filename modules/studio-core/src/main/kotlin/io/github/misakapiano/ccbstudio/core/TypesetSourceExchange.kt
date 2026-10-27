package io.github.misakapiano.ccbstudio.core

import io.github.misakapiano.ccbstudio.assets.AssetStore
import kotlinx.serialization.json.*
import kotlinx.serialization.encodeToString
import java.io.ByteArrayInputStream
import java.util.Base64
import java.util.UUID
import java.nio.file.*

/** TypeSet Forge 1.1 workspace exchange. Never evaluates HTML, URLs, scripts or source history. */
object TypesetSourceExchange {
    const val TAG="CCB_TYPESET_SOURCE_EXCHANGE_V1"
    const val MAX_BYTES=64*1024*1024
    data class Inspection(val bytes:ByteArray,val name:String,val document:TypesetDocument,val originals:List<ByteArray>,val activeIndex:Int,val notes:List<String>)
    data class PresetInspection(val bytes:ByteArray,val preferences:TypesetPreferences)
    fun saveFile(path:Path,bytes:ByteArray,hook:()->Unit={}) {
        require(!Files.exists(path,LinkOption.NOFOLLOW_LINKS)){"目标已存在，请选择新文件名；没有覆盖"}
        val pending=path.resolveSibling(".${path.fileName}.${newId()}.incomplete")
        writeNewForced(pending,bytes);hook()
        // Same-directory rename, explicitly without REPLACE_EXISTING (including a concurrent creator).
        Files.move(pending,path)
    }
    fun inspectPresets(bytes:ByteArray):PresetInspection {
        require(bytes.size in 1..4*1024*1024);val root=documentJson.parseToJsonElement(bytes.decodeToString(throwOnInvalidSequence=true)).jsonObject
        require(root.s("app")=="image_typesetter_frontend_presets")
        if(root["version"]?.jsonPrimitive?.intOrNull!=1)throw DataProblem("UNSUPPORTED_VERSION","未知TypeSet预设版本，原文件保持不变")
        val editorExtras=TypesetEditorExtrasStore.inspect(root)
        val raw=root.getValue("prefs").jsonObject;val texts=raw.a("textPresets");require(texts.size<=200)
        val selected=raw.s("selectedTextPresetId");var selectedId:String?=null
        val presets=texts.map{val p=it.jsonObject;val id=newId();if(p.s("id")==selected)selectedId=id;TypesetPreset(id=id,name=p.s("name"),text=TypesetPreferences.styleOnly(text(p.o("style"))))}
        val overrides=raw.o("presetOverrides").mapValues{settings(it.value.jsonObject)}.toMutableMap()
        var preferred=raw.s("preferredPreset","custom")
        if(overrides.isEmpty() && raw["userMaskPreset"]!=null && raw["userMaskPreset"]!=JsonNull){val target=preferred.takeIf{it in TypesetPreferences.MASK_SLOTS}?:"light_mosaic";overrides[target]=settings(raw.o("userMaskPreset"));if(preferred=="user_custom")preferred=target}
        if(preferred !in TypesetPreferences.MASK_SLOTS+"custom")preferred="custom"
        val prefs=TypesetPreferences(presets,selectedId,raw["textDefaults"]?.takeUnless{it==JsonNull}?.let{TypesetPreferences.styleOnly(text(it.jsonObject))},overrides,preferred,settings(raw),
            exportFormat=raw.s("exportFormat","png").let{if(it=="original")"source"else it},preserveSourceMetadata=raw.b("preserveSourceMetadata",true),
            exportVariants=listOf("original" to "original","maskOnly" to "mask","maskText" to "all","textOnly" to "text").filter{if(it.second=="original"&&TypesetEditorExtrasStore.KEY in root)editorExtras.originalCopies else raw.o("exportVariantSelection").b(it.first,true)}.map{it.second},
            sidebarWidth=raw.n("sidebarWidth",300.0).toInt(),queueThumbSize=raw.n("queueThumbSize",120.0).toInt(),
            storyDefault=raw["storybookDefaults"]?.takeUnless{it==JsonNull}?.let{story(it.jsonObject)},
            batchToolDefault=raw["batchToolDefaults"]?.takeUnless{it==JsonNull}?.let{settings(it.jsonObject)}).also{it.validate()}
        return PresetInspection(bytes.copyOf(),prefs)
    }
    fun importPresets(workspace:StudioWorkspace,inspection:PresetInspection,merge:Boolean)=workspace.operation {
        val source=inspectPresets(inspection.bytes);val old=workspace.state.typesetPreferences
        val incomingExtras=TypesetEditorExtrasStore.inspect(documentJson.parseToJsonElement(source.bytes.decodeToString()).jsonObject)
        val oldExtras=TypesetEditorExtrasStore.load(workspace)
        val extras=(if(merge)incomingExtras.copy(namedMasks=oldExtras.namedMasks+incomingExtras.namedMasks.map{it.copy(id=newId())})else incomingExtras).copy(autoMask=oldExtras.autoMask,lastProjectId=oldExtras.lastProjectId,projectSelectionRecorded=oldExtras.projectSelectionRecorded,projectRecords=oldExtras.projectRecords,outputs=oldExtras.outputs,retirements=oldExtras.retirements)
        extras.validate()
        val next=if(merge)source.preferences.copy(textPresets=old.textPresets+source.preferences.textPresets,maskOverrides=old.maskOverrides+source.preferences.maskOverrides)else source.preferences
        next.validate();val id=UUID.randomUUID()
        workspace.assets.put(AssetStore.Request(id,"source.typesetter-presets.json",AssetStore.Kind.ORIGINAL,null,byteArrayOf(),TAG.toByteArray()),ByteArrayInputStream(source.bytes));workspace.assets.read(id).require()
        workspace.save(workspace.state.copy(typesetPreferences=next.copy(sourceExchangeId=id.toString())))
        if((merge&&oldExtras.namedMasks.isNotEmpty())||oldExtras.autoMask!=null||oldExtras.lastProjectId!=null||incomingExtras.autoMask!=null||incomingExtras.lastProjectId!=null||oldExtras.projectSelectionRecorded||incomingExtras.projectSelectionRecorded||oldExtras.projectRecords.isNotEmpty()||incomingExtras.projectRecords.isNotEmpty()||oldExtras.outputs.isNotEmpty()||incomingExtras.outputs.isNotEmpty()||oldExtras.retirements.isNotEmpty()||incomingExtras.retirements.isNotEmpty())TypesetEditorExtrasStore.save(workspace,extras)
    }
    fun exportPresets(workspace:StudioWorkspace):ByteArray=workspace.operation {
        val prefs=workspace.state.typesetPreferences
        val editorExtras=TypesetEditorExtrasStore.load(workspace).copy(originalCopies="original" in prefs.exportVariants,autoMask=null,lastProjectId=null,projectSelectionRecorded=false,projectRecords=emptyList(),outputs=emptyList(),retirements=emptyList())
        val raw=prefs.sourceExchangeId?.let{id->
            require(workspace.assets.read(UUID.fromString(id)).require().request().project().contentEquals(TAG.toByteArray()))
            documentJson.parseToJsonElement(workspace.bytes(id).decodeToString()).jsonObject.also{require(it.s("app")=="image_typesetter_frontend_presets")}
        }?:JsonObject(emptyMap())
        val result=buildJsonObject {raw.forEach{(k,v)->put(k,v)};put("app","image_typesetter_frontend_presets");put("version",1);put("exportedAt",System.currentTimeMillis())
            put(TypesetEditorExtrasStore.KEY,documentJson.encodeToJsonElement(editorExtras))
            putJsonObject("prefs") {raw.o("prefs").forEach{(k,v)->if(k!="userMaskPreset")put(k,v)};settingsJson(prefs.currentMask).forEach{(k,v)->put(k,v)}
                put("exportFormat",if(prefs.exportFormat=="source")"original"else prefs.exportFormat);put("preserveSourceMetadata",prefs.preserveSourceMetadata)
                put("sidebarWidth",prefs.sidebarWidth);put("queueThumbSize",prefs.queueThumbSize)
                put("storybookDefaults",prefs.storyDefault?.let{s->buildJsonObject{raw.o("prefs").o("storybookDefaults").forEach{(k,v)->put(k,v)};storyJson(TypesetPage(assetId="",name="",story=s)).filterKeys{it!="caption"}.forEach{(k,v)->put(k,v)}}}?:JsonNull)
                put("batchToolDefaults",prefs.batchToolDefault?.let(::settingsJson)?:JsonNull)
                putJsonObject("exportVariantSelection"){listOf("original" to "original","maskOnly" to "mask","maskText" to "all","textOnly" to "text").forEach{(source,native)->put(source,if(source=="original")editorExtras.originalSorted else native in prefs.exportVariants)}}
                put("preferredPreset",prefs.preferredMaskSlot);put("selectedTextPresetId",prefs.selectedTextPresetId.orEmpty());put("textDefaults",prefs.textDefault?.let(::textJson)?:JsonNull)
                putJsonObject("presetOverrides"){prefs.maskOverrides.forEach{(k,v)->put(k,settingsJson(v))}}
                putJsonArray("textPresets"){prefs.textPresets.forEach{p->add(buildJsonObject{put("id",p.id);put("name",p.name);put("style",textJson(p.text))})}}
            }
        };documentJson.encodeToString(result).toByteArray().also{require(it.size<=4*1024*1024)}
    }
    private fun JsonObject.s(key:String,default:String="")=this[key]?.let{if(it==JsonNull)default else it.jsonPrimitive.content}?:default
    private fun JsonObject.n(key:String,default:Double)=this[key]?.let{if(it==JsonNull)default else it.jsonPrimitive.double.also{v->require(v.isFinite())}}?:default
    private fun JsonObject.b(key:String,default:Boolean)=this[key]?.let{if(it==JsonNull)default else it.jsonPrimitive.boolean}?:default
    private fun JsonObject.o(key:String)=this[key]?.takeUnless{it==JsonNull}?.jsonObject?:JsonObject(emptyMap())
    private fun JsonObject.a(key:String)=this[key]?.takeUnless{it==JsonNull}?.jsonArray?:JsonArray(emptyList())
    private fun text(raw:JsonObject)=TypesetText(text=raw.s("text"),x=raw.n("x",300.0),y=raw.n("y",40.0),width=raw.n("width",400.0),
        style=TypesetStyle(raw.s("fontFamily","Microsoft YaHei"),raw.n("fontSize",32.0),raw.n("fontWeight",400.0).toInt(),raw.s("fontStyle","normal")=="italic",raw.b("underline",false),raw.b("strike",false),raw.s("fill","#ffffff"),raw.s("stroke","#000000"),raw.n("strokeWidth",0.0)),
        vertical=raw.s("writingMode","horizontal")=="vertical",align=raw.s("align","center"),lineHeight=raw.n("lineHeight",1.2),letterSpacing=raw.n("letterSpacing",0.0),
        punctuationOptimization=raw.b("verticalPuncOpt",true),punctuationTightness=raw.s("verticalPuncTightness","standard"),opacity=raw.n("textOpacity",1.0),
        background=raw.s("bgColor","#000000"),backgroundOpacity=raw.n("bgOpacity",0.0),border=raw.s("boxBorderColor","#ffffff"),borderWidth=raw.n("boxBorderWidth",0.0),borderOpacity=raw.n("boxBorderOpacity",1.0))
    private fun settings(raw:JsonObject)=MaskToolSettings(raw.s("maskType","mosaic"),raw.n("maskStrength",16.0).toInt(),raw.n("brushSize",60.0),raw.n("stripeDensity",6.0),raw.n("stripeThickness",6.0),raw.n("brushPointDensity",8.0))
    private fun mask(raw:JsonObject)=TypesetMask(shape=raw.s("shape","rect"),effect=raw.s("type","mosaic"),x=raw.n("x",0.0),y=raw.n("y",0.0),width=raw.n("w",100.0),height=raw.n("h",100.0),
        points=raw.a("points").map{val p=it.jsonObject;TypesetPoint(p.n("x",0.0),p.n("y",0.0))},size=raw.n("size",60.0),strength=raw.n("strength",16.0).toInt(),density=raw.n("density",6.0),stripeThickness=raw.n("stripeThickness",6.0),pointDensity=raw.n("pointDensity",8.0))
    private fun story(raw:JsonObject)=StoryStyle(pageWidth=raw.n("pageWidth",1200.0).toInt(),pagePadding=raw.n("pagePadding",48.0).toInt(),gap=raw.n("gap",28.0).toInt(),textPadding=raw.n("textPadding",30.0).toInt(),textMinHeight=raw.n("textMinHeight",170.0).toInt(),
        fontFamily=raw.s("fontFamily","SimSun"),fontSize=raw.n("fontSize",36.0),lineHeight=raw.n("lineHeight",1.55),textAlign=raw.s("textAlign","left"),firstIndent=raw.b("firstIndent",true),showPageNumber=raw.b("showPageNumber",true),
        pageBackground=raw.s("pageBg","#fffdf7"),textColor=raw.s("textColor","#17120d"),borderColor=raw.s("borderColor","#2c241d"),pageBorder=raw.b("pageBorder",true),imageBorder=raw.b("imageBorder",true))
    fun inspect(bytes:ByteArray):Inspection {
        require(bytes.size in 1..MAX_BYTES){"Exchange exceeds 64 MiB; split the source queue explicitly"}
        val raw=documentJson.parseToJsonElement(bytes.decodeToString(throwOnInvalidSequence=true)).jsonObject
        require(raw.s("app")=="image_typesetter_frontend_workspace"){"Not a TypeSet workspace exchange"}
        if(raw["version"]?.jsonPrimitive?.intOrNull!=1)throw DataProblem("UNSUPPORTED_VERSION","未知TypeSet工程版本，原文件保持不变")
        val entries=raw.a("entries");require(entries.size in 1..1000);val originals=mutableListOf<ByteArray>();var history=0
        val pages=entries.mapIndexed{index,item->val e=item.jsonObject;val url=e.s("blobDataUrl")
            require(url.startsWith("data:") && url.substringBefore(',').endsWith(";base64")){"Only embedded original bytes are supported"}
            val data=Base64.getDecoder().decode(url.substringAfter(',',""));EditorRenderer.decode(data).flush();originals+=data;history+=e.a("history").size
            val extension=e["ccbStudio"]?.takeUnless{it==JsonNull}?.jsonObject
            if(extension!=null && extension["version"]?.jsonPrimitive?.intOrNull!=1)throw DataProblem("UNSUPPORTED_VERSION","Unknown Studio exchange extension; retained")
            if(extension!=null)documentJson.decodeFromJsonElement<TypesetPage>(extension.getValue("page")).copy(assetId=newId(),sourceEntryIndex=index)
            else TypesetPage(coordinates=TypesetImage.DISPLAY,assetId=newId(),name=e.s("name",e.s("fileName","Imported image")),texts=e.a("texts").map{ text(it.jsonObject)},masks=e.a("masks").map{mask(it.jsonObject)},
                caption=e.o("storybook").s("caption"),story=story(e.o("storybook")),toolDefaults=e["toolDefaults"]?.takeUnless{it==JsonNull}?.let{settings(it.jsonObject).apply(TypesetMask())},sourceEntryIndex=index)
        }
        val mode=raw.s("workspaceMode","image");require(mode in setOf("image","storybook"))
        val document=TypesetDocument(pages=pages,startNumber=raw.n("batchStart",1.0).toInt(),numberDigits=raw.n("batchDigits",3.0).toInt(),mode=if(mode=="storybook")mode else "editor").also{it.validate()}
        val name=raw.s("name","导入TypeSet工程");require(name.isNotBlank() && name.length<=100)
        return Inspection(bytes.copyOf(),name,document,originals,raw.n("activeIndex",0.0).toInt(),listOf("源文件、未知字段、metadata和 $history 条源历史原样保留；源历史不执行为Studio撤销命令。","字体按源名称保留；缺失字体会阻止渲染导出，请在编辑器中选择或导入字体。"))
    }
    fun import(workspace:StudioWorkspace,inspection:Inspection):TypesetProject=workspace.operation {
        // Reparse immutable submitted bytes rather than trusting UI-mutated inspection collections.
        val source=inspect(inspection.bytes);val sourceId=UUID.randomUUID()
        workspace.assets.put(AssetStore.Request(sourceId,"source.typesetter-workspace.json",AssetStore.Kind.ORIGINAL,null,byteArrayOf(),TAG.toByteArray()),ByteArrayInputStream(source.bytes))
        workspace.assets.read(sourceId).require()
        val pages=source.document.pages.mapIndexed{index,page->page.copy(assetId=workspace.importImage(page.name,source.originals[index]))}
        TypesetRepository(workspace).importDocument(source.name,source.document.copy(pages=pages),sourceId.toString(),source.activeIndex)
    }
    fun originalExchange(workspace:StudioWorkspace,project:TypesetProject):ByteArray=workspace.operation {
        val id=project.sourceExchangeId?:throw DataProblem("MISSING","This project has no imported source exchange")
        val asset=workspace.assets.read(UUID.fromString(id)).require();require(asset.request().project().contentEquals(TAG.toByteArray()))
        workspace.bytes(id)
    }
    fun export(workspace:StudioWorkspace,project:TypesetProject,document:TypesetDocument):ByteArray=workspace.operation {
        document.validate()
        val source=if(project.sourceExchangeId==null)JsonObject(emptyMap())else documentJson.parseToJsonElement(originalExchange(workspace,project).decodeToString()).jsonObject
        val entries=source.a("entries")
        val result=buildJsonObject {
            source.forEach{(key,value)->put(key,value)}
            put("app","image_typesetter_frontend_workspace");put("version",1);put("name",project.name);put("updatedAt",System.currentTimeMillis())
            put("batchStart",document.startNumber);put("batchDigits",document.numberDigits);put("workspaceMode",if(document.mode=="storybook")"storybook"else"image")
            put("activeIndex",document.pages.indexOfFirst{it.id==project.currentPageId}.coerceAtLeast(0))
            putJsonArray("entries"){document.pages.forEach{p->add(buildJsonObject {
                p.sourceEntryIndex?.let{entries.getOrNull(it)?.jsonObject}?.forEach{(key,value)->if(key!="blob")put(key,value)}
                val bytes=workspace.bytes(p.assetId);val mime="image/"+ImageFormats.format(bytes)
                put("id",p.id);put("name",p.name);put("fileName",p.name);put("originalMime",mime);put("originalExt",ImageFormats.extension(bytes))
                put("blobDataUrl","data:$mime;base64,"+Base64.getEncoder().encodeToString(bytes))
                putJsonArray("texts"){p.texts.filter{it.visible}.forEach{add(textJson(it))}}
                putJsonArray("masks"){p.masks.filter{it.visible}.forEach{add(maskJson(it))}}
                put("toolDefaults",p.toolDefaults?.let{settingsJson(MaskToolSettings.from(it))}?:JsonNull)
                put("storybook",storyJson(p));putJsonObject("ccbStudio"){put("version",1);put("page",documentJson.encodeToJsonElement(p))}
            })}}
        }
        documentJson.encodeToString(result).toByteArray().also{require(it.size<=MAX_BYTES){"Exchange exceeds 64 MiB; split the queue explicitly"}}
    }
    private fun textJson(t:TypesetText)=buildJsonObject {
        put("text",t.text);put("x",t.x);put("y",t.y);put("width",t.width);put("fontFamily",t.style.fontFamily);put("fontSize",t.style.fontSize);put("fontWeight",t.style.fontWeight)
        put("fontStyle",if(t.style.italic)"italic"else"normal");put("underline",t.style.underline);put("strike",t.style.strike);put("fill",t.style.fill);put("stroke",t.style.stroke);put("strokeWidth",t.style.strokeWidth)
        put("writingMode",if(t.vertical)"vertical"else"horizontal");put("align",t.align);put("lineHeight",t.lineHeight);put("letterSpacing",t.letterSpacing);put("verticalPuncOpt",t.punctuationOptimization);put("verticalPuncTightness",t.punctuationTightness)
        put("textOpacity",t.opacity);put("bgColor",t.background);put("bgOpacity",t.backgroundOpacity);put("boxBorderColor",t.border);put("boxBorderWidth",t.borderWidth);put("boxBorderOpacity",t.borderOpacity)
    }
    private fun maskJson(m:TypesetMask)=buildJsonObject {
        put("shape",m.shape);put("type",m.effect);put("x",m.x);put("y",m.y);put("w",m.width);put("h",m.height);put("size",m.size);put("strength",m.strength);put("density",m.density);put("stripeThickness",m.stripeThickness);put("pointDensity",m.pointDensity)
        putJsonArray("points"){m.points.forEach{p->add(buildJsonObject{put("x",p.x);put("y",p.y)})}}
    }
    private fun settingsJson(s:MaskToolSettings)=buildJsonObject {put("maskType",s.effect);put("maskStrength",s.strength);put("brushSize",s.size);put("stripeDensity",s.density);put("stripeThickness",s.stripeThickness);put("brushPointDensity",s.pointDensity)}
    private fun storyJson(p:TypesetPage)=buildJsonObject {val s=p.story
        put("caption",p.caption);put("pageWidth",s.pageWidth);put("pagePadding",s.pagePadding);put("gap",s.gap);put("textPadding",s.textPadding);put("textMinHeight",s.textMinHeight);put("fontFamily",s.fontFamily);put("fontSize",s.fontSize);put("lineHeight",s.lineHeight);put("textAlign",s.textAlign)
        put("firstIndent",s.firstIndent);put("showPageNumber",s.showPageNumber);put("pageBg",s.pageBackground);put("textColor",s.textColor);put("borderColor",s.borderColor);put("pageBorder",s.pageBorder);put("imageBorder",s.imageBorder)
    }
}
