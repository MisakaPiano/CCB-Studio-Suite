package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.runtime.*
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.ContinuationInterceptor

/** UI draft is separate from the committed immutable snapshot. A failed save keeps the draft visible. */
internal class TypesetEditorController(private val c:WorkbenchController) {
    private val ui=c.scope.coroutineContext[ContinuationInterceptor] as CoroutineDispatcher
    private val repository=TypesetRepository(c.workspace)
    private val commit=Mutex()
    private var pending:Job?=null
    var project by mutableStateOf<TypesetProject?>(null);private set
    var document by mutableStateOf<TypesetDocument?>(null);private set
    var dirty by mutableStateOf(false);private set
    var pageId by mutableStateOf<String?>(null)
    var selectedPages by mutableStateOf(setOf<String>())
    var objectId by mutableStateOf<String?>(null)
    var tool by mutableStateOf("select")
    var zoom by mutableStateOf(1f)
    var maskDefault by mutableStateOf(TypesetMask())
    val page get()=document?.pages?.firstOrNull{it.id==pageId}
    val text get()=page?.texts?.firstOrNull{it.id==objectId}
    val mask get()=page?.masks?.firstOrNull{it.id==objectId}
    fun change(transform:(TypesetDocument)->TypesetDocument) {
        if(c.busy)return
        val old=document?:return;val next=transform(old)
        try{next.validate()}catch(e:Exception){c.problem="编辑值无效，原内容保留：${e.message}";return}
        if(next==old)return
        document=next;dirty=true;pending?.cancel()
        pending=c.scope.launch{delay(400);try{flush()}catch(e:CancellationException){throw e}catch(e:Exception){c.problem="工程未能保存，当前编辑仍保留；请重试保存或导出工程"}}
    }
    fun pageChange(transform:(TypesetPage)->TypesetPage)=change{d->d.copy(pages=d.pages.map{if(it.id==pageId)transform(it)else it})}
    fun textChange(transform:(TypesetText)->TypesetText)=pageChange{p->p.copy(texts=p.texts.map{if(it.id==objectId)transform(it)else it})}
    suspend fun flush()=withContext(NonCancellable) {commit.withLock {
        val p=project?:return@withLock;val snapshot=document?:return@withLock
        val ids=snapshot.pages.map{it.id}.toSet();val current=pageId?.takeIf{it in ids};val selected=selectedPages.filter{it in ids}
        if(!dirty && p.currentPageId==current && p.selectedPageIds==selected)return@withLock
        val next=withContext(Dispatchers.IO){val updated=repository.update(p.id,p.revision){snapshot};repository.view(updated.id,updated.revision,current,selected)}
        project=next;dirty=document!=snapshot
    }}
    suspend fun drain(){pending?.cancelAndJoin();flush()}
    fun create(name:String,ids:List<String>)=c.run("建立编辑工程") {
        drain();val p=repository.create(name,ids);val d=repository.load(p)
        withContext(ui){project=p;document=d;dirty=false;pageId=d.pages.firstOrNull()?.id;selectedPages=setOfNotNull(pageId);objectId=null;maskDefault=page?.toolDefaults?:TypesetMask();c.view="编辑"}
    }
    fun open(id:String)=c.run("打开编辑工程") {
        drain();val p=c.workspace.state.typesetProjects.first{it.id==id};val d=repository.load(p)
        val fontProblem=runCatching{TypesetFonts.activate(c.workspace,TypesetFonts.documentFonts(d))}.exceptionOrNull()
        withContext(ui){project=p;document=d;dirty=false;pageId=p.currentPageId?.takeIf{id->d.pages.any{it.id==id}}?:d.pages.firstOrNull()?.id;selectedPages=p.selectedPageIds.filter{id->d.pages.any{it.id==id}}.toSet();objectId=null;maskDefault=page?.toolDefaults?:TypesetMask();c.view="编辑"}
        if(fontProblem!=null)c.problem="字体资源不可用，工程已保留并打开。请重新导入字体或选择可用字体；导出前必须解决。"
    }
    fun manageProject(action:String,name:String="")=c.run("编辑工程管理") {
        drain();val p=project?:return@run
        when(action){
            "rename"->{val next=repository.rename(p.id,p.revision,name);withContext(ui){project=next}}
            "copy"->{val next=repository.copy(p.id,p.revision,name);withContext(ui){project=next;dirty=false}}
            "remove"->{repository.remove(p.id,p.revision);withContext(ui){project=null;document=null;pageId=null;selectedPages=emptySet();objectId=null;dirty=false}}
            else->error("Unknown project operation")
        }
    }
    fun importFont()=c.run("导入本地字体") {
        drain()
        val file=withContext(ui){val chooser=javax.swing.JFileChooser();chooser.dialogTitle="导入字体（仅保存在 Studio）";chooser.fileFilter=javax.swing.filechooser.FileNameExtensionFilter("字体 TTF / OTF / WOFF / WOFF2","ttf","otf","woff","woff2");if(chooser.showOpenDialog(null)==javax.swing.JFileChooser.APPROVE_OPTION)chooser.selectedFile else null}?:return@run
        TypesetFonts.import(c.workspace,file.name,readBounded(file.toPath(),64*1024*1024))
    }
    fun history(redo:Boolean=false)=c.run(if(redo)"重做编辑"else"撤销编辑") {
        drain();val p=project?:return@run;val next=repository.history(p.id,p.revision,redo);val d=repository.load(next)
        withContext(ui){project=next;document=d;dirty=false;if(d.pages.none{it.id==pageId})pageId=d.pages.firstOrNull()?.id;objectId=null;selectedPages=selectedPages.intersect(d.pages.map{it.id}.toSet())}
    }
    fun selectPage(id:String,extend:Boolean=false,range:Boolean=false) {
        val pages=document?.pages.orEmpty();val previous=pageId
        selectedPages=when{range && previous!=null->{val a=pages.indexOfFirst{it.id==previous};val b=pages.indexOfFirst{it.id==id};selectedPages+pages.subList(minOf(a,b),maxOf(a,b)+1).map{it.id}}
            extend->if(id in selectedPages)selectedPages-id else selectedPages+id
            else->setOf(id)}
        pageId=id;objectId=null;page?.toolDefaults?.let{maskDefault=it}
    }
    fun addText(width:Int,height:Int) {
        val template=text?:document?.presets?.firstOrNull()?.text
        val next=(template?:TypesetText(style=TypesetStyle(fontSize=maxOf(24.0,width*.045),strokeWidth=maxOf(1.0,width*.045*.06))))
            .copy(id=newId(),text="",x=width*.5,y=height*.15,width=minOf(width*.75,900.0))
        pageChange{it.copy(texts=it.texts+next)};objectId=next.id;tool="select"
    }
    fun removeObject()=pageChange{TypesetObjects.remove(it,objectId?:return@pageChange it)}.also{objectId=null}
    fun copyObject(){val id=objectId?:return;val next=newId();pageChange{TypesetObjects.copy(it,id,next)};objectId=next}
    fun moveObject(direction:Int)=pageChange{TypesetObjects.move(it,objectId?:return@pageChange it,direction)}
    fun lockObject(locked:Boolean)=pageChange{TypesetObjects.lock(it,objectId?:return@pageChange it,locked)}
    fun movePages(direction:Int)=change{d->
        val list=d.pages.toMutableList();val indexes=if(direction<0)list.indices else list.indices.reversed()
        for(i in indexes){val j=i+direction;if(j in list.indices && list[i].id in selectedPages && list[j].id !in selectedPages){val tmp=list[i];list[i]=list[j];list[j]=tmp}}
        d.copy(pages=list)
    }
    fun movePagesTo(anchor:String?,after:Boolean)=change{TypesetQueue.move(it,selectedPages,anchor,after)}
    fun appendSelected()=c.run("追加图库图片") {
        drain();val p=project?:return@run
        val ids=c.assets.filter{it.request().id().toString() in c.selection}.map{it.request().id().toString()}
        val added=ids.map{id->val asset=c.workspace.assets.read(java.util.UUID.fromString(id)).require();TypesetPage(assetId=id,name=asset.request().name())}
        val next=repository.update(p.id,p.revision){it.copy(pages=it.pages+added)};val d=repository.load(next)
        withContext(ui){project=next;document=d;dirty=false;pageId=added.firstOrNull()?.id?:pageId;selectedPages=added.map{it.id}.toSet();objectId=null}
    }
    fun removePages() {change{it.copy(pages=it.pages.filterNot{it.id in selectedPages})};pageId=document?.pages?.firstOrNull()?.id;selectedPages=emptySet();objectId=null}
    fun applyStyleToSelection(previous:Boolean=false)=c.run("应用批次样式") {
        drain();val p=project?:return@run;val snapshot=document?:return@run
        val selected=selectedPages.toSet();val textStyle=text?:snapshot.presets.firstOrNull()?.text
        val widths=mutableMapOf<String,Int>()
        val updated=TypesetBatch.apply(snapshot,selected,previous,textStyle,mask?:maskDefault){page->
            widths.getOrPut(page.assetId){val image=EditorRenderer.decode(c.workspace.bytes(page.assetId));try{image.width}finally{image.flush()}}
        }
        if(updated!=snapshot){val next=repository.update(p.id,p.revision){updated}
            withContext(ui){project=next;document=updated;dirty=false;page?.toolDefaults?.let{maskDefault=it}}
        }
    }
}
