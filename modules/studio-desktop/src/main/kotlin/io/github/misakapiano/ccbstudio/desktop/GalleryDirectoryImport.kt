package io.github.misakapiano.ccbstudio.desktop

import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import java.nio.file.*
import java.nio.file.LinkOption.NOFOLLOW_LINKS

internal class GalleryDirectoryImportProblem(message:String):IllegalArgumentException(message)
internal data class GalleryDirectoryPlan(val folders:List<VirtualAlbum>,val files:List<Pair<Path,String?>>)

/** Explicit one-shot intake. No symlink following, directory watcher, or writes to source paths. */
internal suspend fun planGalleryDirectory(root:Path,recursive:Boolean,state:LibraryState):GalleryDirectoryPlan {
    val source=root.toAbsolutePath().normalize();safePath(source)
    if(!Files.isDirectory(source,NOFOLLOW_LINKS))throw GalleryDirectoryImportProblem("所选位置不是可读取的文件夹")
    val folders=mutableListOf<VirtualAlbum>();val files=mutableListOf<Pair<Path,String?>>()
    val suffixes=setOf("png","apng","jpg","jpeg","webp","gif","bmp")
    suspend fun visit(path:Path,parent:String?,depth:Int) {
        currentCoroutineContext().ensureActive();safePath(path)
        val album=if(recursive) {
            if(depth>32)throw GalleryDirectoryImportProblem("目录超过相册支持的32层；尚未开始入库")
            val name=path.fileName?.toString()?:"导入文件夹"
            if(name.isBlank()||name.length>100)throw GalleryDirectoryImportProblem("有目录名称超过相册支持的100字；尚未开始入库，不截断目录名")
            if(state.virtualAlbums.size+folders.size>=500)throw GalleryDirectoryImportProblem("目录结构超过当前剩余相册容量（总计500个）；尚未开始入库")
            VirtualAlbum(name=name,parent=parent).also{folders+=it}
        }else null
        Files.newDirectoryStream(path).use{entries->
            for(child in entries) {
                currentCoroutineContext().ensureActive();safePath(child)
                if(Files.isSymbolicLink(child))throw GalleryDirectoryImportProblem("目录包含链接；尚未开始入库，不跟随链接")
                if(Files.isDirectory(child,NOFOLLOW_LINKS)) {
                    if(recursive)visit(child,album!!.id,depth+1)
                }else if(Files.isRegularFile(child,NOFOLLOW_LINKS)&&child.fileName.toString().substringAfterLast('.',"").lowercase() in suffixes) {
                    // Existing metadata limits stay explicit; 1000 is not a directory-total limit.
                    if(state.observations.size+files.size>=20000)throw GalleryDirectoryImportProblem("图片超过当前目录回执容量（总计20000项）；尚未开始入库，请分开资料库")
                    files+=child.toAbsolutePath().normalize() to album?.id
                }
            }
        }
    }
    visit(source,null,1)
    return GalleryDirectoryPlan(folders,files.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER){it.first.toString()}))
}

internal fun WorkbenchController.importGalleryDirectory(root:Path,recursive:Boolean)=run("导入图片") {
    gallery.importStartedNanos=System.nanoTime();gallery.importDurationMs=null;gallery.importTimings=emptyMap()
    gallery.imports=emptyList();gallery.importProgress="正在整理文件夹${if(recursive)"及子文件夹"else"本层"}…"
    val plan=try{planGalleryDirectory(root,recursive,library.repository.load())}
    catch(e:CancellationException){throw e}
    catch(e:GalleryDirectoryImportProblem){problem=e.message;return@run}
    catch(e:Exception){problem="读取目录列表失败（${e.javaClass.simpleName}）；尚未开始入库，原文件保留";return@run}
    if(plan.files.isEmpty()){problem=if(recursive)"所选文件夹及子文件夹没有支持的图片，未添加"else"所选目录本层没有支持的图片，未添加；可选择含子文件夹导入";return@run}
    val groups=plan.files.groupBy{it.second}.values
    gallery.imports=groups.flatten().map{GalleryImportResult(root.toAbsolutePath().normalize().relativize(it.first).toString(),"WAITING")}
    var completed=0;var partial=false
    try {
        if(recursive)library.repository.update{state->
            val next=state.virtualAlbums+plan.folders
            VirtualAlbums.validate(next);state.copy(virtualAlbums=next)
        }
        // Bound each original/copy transaction batch, not the total selected directory.
        for(group in groups)for(chunk in group.chunked(250)) {
            currentCoroutineContext().ensureActive()
            val offset=completed
            val albumId=chunk.first().second
            problem=null
            importGalleryBatch(GalleryIngress(files=chunk.map{it.first},channel=if(recursive)"gallery-directory-tree"else"gallery-directory-dialog"),
                reportOffset=offset,onCommitted={ids->
                    if(albumId!=null)library.repository.update{state->
                        val album=state.virtualAlbums.single{it.id==albumId}
                        state.copy(virtualAlbums=VirtualAlbums.save(state.virtualAlbums,album.copy(images=(album.images+ids).distinct())))
                    }
                })
            partial=partial||problem!=null;completed+=chunk.size
            gallery.importProgress="已处理 $completed / ${plan.files.size} · 原件已入库 ${gallery.imports.count{it.assetId!=null}} 张"
        }
        if(partial)problem="部分文件或目录副本未完成，请查看逐项回执；已提交原件及目录组织保留"
    }finally {
        gallery.imports=gallery.imports.map{if(it.status=="WAITING")it.copy(status="NOT_IMPORTED · 未开始或已停止")else it}
        gallery.importProgress="导入结束 · ${gallery.imports.count{it.assetId!=null}} / ${plan.files.size} 原件已入库；未导入项保留回执"
    }
}
