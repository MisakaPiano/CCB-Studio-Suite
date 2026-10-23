package io.github.misakapiano.ccbstudio.desktop

import com.example.chatbar.desktop.*
import com.example.chatbar.domain.image.*
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.nio.file.*
import kotlin.test.*

/** Isolated, labelled synthetic review data; never reads or clones a user's workspace. */
class InvitationReviewFixtureTest {
    @Test fun prepareSyntheticGalleryRecyclePromptAndTwoPageReview()=runBlocking {
        val root=Path.of(System.getProperty("preview.test.root"),newId());val data=root.resolve("workspace")
        val inputs=Files.createDirectories(root.resolve("inputs"));val ids=mutableListOf<String>()
        StudioWorkspace.openStudio(data).use{w->
            val repo=LibraryRepository(w)
            for((album,count) in listOf("album-A" to 12,"album-B" to 12)) {
                val folder=Files.createDirectory(inputs.resolve(album))
                for(n in 1..count) {
                    val bytes=EditorRenderer.synthetic(320+n,240+(n%3)*30);val path=Files.write(folder.resolve("$album-${n.toString().padStart(2,'0')}.png"),bytes)
                    val receipt=buildJsonObject{put("kind","image-ingress");put("version",1);put("source","file");put("sourcePath",path.toString());put("createdAt",1792670400000L+n*86400000L)}
                    ids+=w.importImage(path.fileName.toString(),bytes,receipt.toString())
                }
            }
            val labels=ids.mapIndexed{i,id->id to AssetLabels(tags=when(i%4){0->listOf("生日","生日派对");1->listOf("生日","待编辑");2->listOf("旅行");else->listOf("归档")})}.toMap()
            repo.changeLabels(ids.associateWith<String,AssetLabels?>{null},labels)
            val promptSections=listOf(PromptSection(id="style",label="画风",text="watercolor, soft light"),PromptSection(id="base",label="基础 Prompt",text="two friends, birthday party, garden"),PromptSection(id="negative",label="负向 Prompt",text="blurry, lowres"),PromptSection(id="a",label="角色正向",text="green scarf, short hair",roleId="A",attributes=mapOf("原始角色位置" to "1")),PromptSection(id="b",label="角色正向",text="blue coat, long hair",roleId="B",attributes=mapOf("原始角色位置" to "2")))
            val board=PromptReferences(repo)
            val a=repo.savePrompt("synthetic candidate","合成 AI 候选（未联网）","synthetic-fixture:ai-candidate-not-network",sections=promptSections);board.add(a)
            val b=repo.savePrompt("synthetic image words","合成图片参数来源","synthetic-fixture:image-not-generated",sections=promptSections.take(3)+promptSections.takeLast(2).reversed(),image=PromptImageContext(ids.first(),w.assets.read(java.util.UUID.fromString(ids.first())).require().sha256(),"MISSING",parameters=mapOf("图片尺寸" to "321 × 270")));board.add(b)
            val t=repo.savePrompt("garden, birthday, colorful balloons","独立文字参照","synthetic-fixture:standalone");board.add(t)
            val archivedPrompt=repo.savePrompt("synthetic archived words","回收站 Prompt 样例","synthetic-fixture:archived")
            AssetArchive(w).change(setOf(ids[22],ids[23],archivedPrompt),true)
            repo.pauseCollections(true)
            val scope=CoroutineScope(SupervisorJob()+Dispatchers.Default)
            val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root.resolve("runtime")),root.resolve("budget"),admissionOverride=RequestAdmission{error("No provider calls in fixture")},pauseCollectionsOnStart=true)
            try {c.editor.create("邀请验收 · 两页合成图",ids.take(2));c.active?.join();assertNull(c.problem);c.editor.drain();assertEquals(2,c.editor.document!!.pages.size)}finally{c.close();scope.cancel()}
        }
        val manifest=buildJsonObject{put("root",root.toString());put("workspace",data.toString());put("syntheticOnly",true);put("providerCalls",0);put("images",ids.size);put("archiveItems",3);put("promptReferences",3)}
        val out=Path.of("build/invitation-review/fixture.json");Files.createDirectories(out.parent);Files.writeString(out,manifest.toString())
        StudioWorkspace.openStudio(data).use{w->assertEquals(24,SharedAssets.images(w).size);assertEquals(3,AssetArchive(w).ids().size);assertEquals(3,PromptReferences(LibraryRepository(w)).load().ids.size)}
    }
}
