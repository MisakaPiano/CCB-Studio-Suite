package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.ui.input.key.*
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.awt.Toolkit
import java.awt.datatransfer.*
import java.util.UUID

/** A bounded, explicit object/page exchange. No image bytes, paths or external reads. */
internal object TypesetClipboard {
    private const val APP="CCB_STUDIO_EDITOR_CLIPBOARD";private const val MAX=20*1024*1024
    var write:(String)->Unit={Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(it),null)}
    var read:()->String?={val c=Toolkit.getDefaultToolkit().systemClipboard;if(c.isDataFlavorAvailable(DataFlavor.stringFlavor))c.getData(DataFlavor.stringFlavor) as? String else null}
    private fun namespace(c:WorkbenchController)=sha256(c.workspace.root.toString().toByteArray())
    fun copy(c:WorkbenchController,e:TypesetEditorController,pages:Boolean,cut:Boolean=false):Boolean=guard(c){
        val d=e.document?:return@guard false
        val selected=if(pages)d.pages.filter{it.id in e.selectedPages}else listOfNotNull(e.page?.let{p->
            val t=e.text;val m=e.mask;if(t==null&&m==null)return@let null
            if(cut&&(t?.locked==true||m?.locked==true))error("对象已锁定，不能剪切")
            p.copy(texts=listOfNotNull(t),masks=listOfNotNull(m),layerOrder=emptyList())})
        if(selected.isEmpty())return@guard false
        val text=buildJsonObject{put("app",APP);put("version",1);put("kind",if(pages)"pages"else"object");put("workspace",namespace(c));put("document",documentJson.encodeToJsonElement(TypesetDocument(pages=selected)))}.toString()
        require(text.toByteArray().size<=MAX){"所选内容超过剪贴板容量；请减少页数"}
        write(text) // A failed clipboard write must never remove a source object/page.
        if(cut){if(pages)e.removePages()else e.removeObject()};true
    }
    fun paste(c:WorkbenchController,e:TypesetEditorController,pages:Boolean):Boolean=guard(c){
        val raw=read()?:return@guard false;require(raw.length<=MAX&&raw.toByteArray().size<=MAX){"剪贴板内容过大"}
        val root=runCatching{documentJson.parseToJsonElement(raw).jsonObject}.getOrNull()?:return@guard false
        if(root["app"]?.jsonPrimitive?.content!=APP)return@guard false
        require(root["version"]?.jsonPrimitive?.int==1){"未知剪贴板版本；未应用"}
        require(root["kind"]?.jsonPrimitive?.content==(if(pages)"pages"else"object")){"请在原复制区域粘贴：页面队列或画布对象"}
        val from=documentJson.decodeFromJsonElement<TypesetDocument>(root.getValue("document")).also{it.validate()}
        val current=e.document?:return@guard false
        if(pages){
            require(root["workspace"]?.jsonPrimitive?.content==namespace(c)){"页面引用属于其他工作区；请通过工程交换导入"}
            require(from.pages.isNotEmpty()&&current.pages.size+from.pages.size<=1000)
            from.pages.forEach{c.workspace.assets.read(UUID.fromString(it.assetId)).require()}
            val added=from.pages.map(::fresh);val at=current.pages.indexOfFirst{it.id==e.pageId}.let{if(it<0)current.pages.size else it+1}
            e.change{it.copy(pages=it.pages.take(at)+added+it.pages.drop(at))};e.pageId=added.first().id;e.selectedPages=added.map{it.id}.toSet();e.objectId=null
        }else{
            if(e.page==null)return@guard false
            val p=from.pages.single();require(p.texts.size+p.masks.size==1)
            val t=p.texts.singleOrNull()?.let{it.copy(id=newId(),x=it.x+20,y=it.y+20,locked=false)}
            val m=p.masks.singleOrNull()?.let{it.copy(id=newId(),x=it.x+20,y=it.y+20,points=it.points.map{q->q.copy(x=q.x+20,y=q.y+20)},locked=false)}
            val id=t?.id?:m!!.id
            e.pageChange{it.copy(texts=it.texts+listOfNotNull(t),masks=it.masks+listOfNotNull(m),layerOrder=TypesetObjects.order(it)+id)};e.objectId=id
        };true
    }
    private fun fresh(p:TypesetPage):TypesetPage {
        val ids=(p.texts.map{it.id}+p.masks.map{it.id}).associateWith{newId()}
        return p.copy(id=newId(),texts=p.texts.map{it.copy(id=ids.getValue(it.id))},masks=p.masks.map{it.copy(id=ids.getValue(it.id))},layerOrder=p.layerOrder.map{ids.getValue(it)})
    }
    private inline fun guard(c:WorkbenchController,action:()->Boolean)=try{if(c.busy||c.maintenance)false else action()}catch(ex:Exception){c.problem="剪贴板操作未完成：${ex.message}";false}
}

/** Attached to canvas/queue focus scopes, never above text-entry fields. */
internal fun typesetShortcut(event:KeyEvent,c:WorkbenchController,e:TypesetEditorController,pages:Boolean=false):Boolean {
    if(event.type!=KeyEventType.KeyDown||c.busy||c.maintenance)return false
    if(event.isCtrlPressed&&!event.isAltPressed)return when(event.key){
        Key.Z->{e.history(event.isShiftPressed);true};Key.Y->{e.history(true);true}
        Key.C->TypesetClipboard.copy(c,e,pages);Key.X->TypesetClipboard.copy(c,e,pages,true);Key.V->TypesetClipboard.paste(c,e,pages)
        Key.A->if(pages){e.selectedPages=e.document?.pages.orEmpty().map{it.id}.toSet();true}else false
        Key.D->if(!pages){e.copyObject();true}else false
        else->false
    }
    if(event.key==Key.Delete&&!event.isCtrlPressed){if(pages)e.removePages()else if(e.text?.locked!=true&&e.mask?.locked!=true)e.removeObject();return true}
    return false
}
