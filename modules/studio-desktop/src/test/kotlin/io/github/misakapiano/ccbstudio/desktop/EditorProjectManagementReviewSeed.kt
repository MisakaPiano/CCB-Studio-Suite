package io.github.misakapiano.ccbstudio.desktop

import io.github.misakapiano.ccbstudio.core.*
import java.nio.file.*
import kotlinx.serialization.json.*

/** Preparation only. The candidate itself launches production MainKt without a test host. */
object EditorProjectManagementReviewSeed {
    @JvmStatic fun main(args:Array<String>){
        require(args.size==1);val root=EditorReviewCandidate.checkedRoot(Path.of(args[0]));EditorReviewCandidate.prepare(root)
        StudioWorkspace.openStudio(root.resolve("workspace")).use{w->
            require(w.state.accounts.isEmpty());val ids=SharedAssets.images(w).map{it.request().id().toString()};require(ids.size==12)
            val a=GenerationFolder(name="合成画册");val b=GenerationFolder(name="第一卷",parent=a.id,kind="folder");val c=GenerationFolder(name="湖畔章节",parent=b.id,kind="chapter");val d=GenerationFolder(name="开篇插页",parent=c.id,kind="folder")
            w.save(w.state.copy(generationOrganization=GenerationOrganization(folders=listOf(a,b,c,d))))
            val service=TypesetProjectArchives(w);val repo=TypesetRepository(w)
            val archived=service.create("合成归档工程 · 可测试删除",ids.takeLast(2),c.id);service.archive(archived.id,archived.revision,archived.name,c.id)
            var p=service.create("合成当前工程 · 可测试收起与废弃",ids.take(3),d.id)
            p=repo.update(p.id,p.revision){doc->doc.copy(pages=doc.pages.mapIndexed{i,page->page.copy(texts=listOf(TypesetText(text="合成审阅 ${i+1}",x=120.0,y=80.0)))})}
            val out=Files.createDirectories(w.root.resolve("exports/仅合成删除测试"));val current=out.resolve("当前工程生成图.png");val prior=out.resolve("归档工程生成图.png");val shared=out.resolve("共享依赖_应保留.png")
            Files.write(current,EditorRenderer.synthetic(640,480),StandardOpenOption.CREATE_NEW);Files.write(prior,EditorRenderer.synthetic(320,240),StandardOpenOption.CREATE_NEW);Files.write(shared,EditorRenderer.synthetic(160,120),StandardOpenOption.CREATE_NEW);Files.writeString(out.resolve("未登记文件_应保留.txt"),"SYNTHETIC UNKNOWN FILE MUST REMAIN",StandardOpenOption.CREATE_NEW)
            fun record(project:String,file:Path,asset:String?=null)=Files.readAllBytes(file).let{TypesetManagedOutput(projectId=project,path=file.toAbsolutePath().toString(),size=it.size.toLong(),sha256=sha256(it),assetId=asset)}
            TypesetProjectDeletion(w).register(p.id,listOf(record(p.id,current),record(p.id,shared,ids.first())))
            TypesetProjectDeletion(w).register(archived.id,listOf(record(archived.id,prior)))
            val extras=TypesetEditorExtrasStore.load(w);TypesetEditorExtrasStore.save(w,extras.copy(lastProjectId=p.id,projectSelectionRecorded=true))
            Files.writeString(root.resolve("MANAGEMENT_SEED.json"),buildJsonObject{put("syntheticOnly",true);put("originals",12);put("accounts",0);put("currentProject",p.id);put("archivedProject",archived.id);put("outputRoot",out.toString());put("originalCopiesCreatedByArchive",0)}.toString(),StandardOpenOption.CREATE_NEW)
        };EditorReviewCandidate.verifyOriginals(root)
    }
}
