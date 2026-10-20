package io.github.misakapiano.ccbstudio.core

import com.example.chatbar.domain.image.*
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import java.nio.file.*
import kotlin.test.*

class StudioCoreTest {
    private fun root():Path=Paths.get(System.getProperty("preview.test.root")).resolve(newId())
    private fun nai(alias:String="NAI synthetic")=AccountProfile(alias=alias,service="NAI",provider="SYNTHETIC",model="nai-diffusion-4-5-full")
    private fun llm()=AccountProfile(alias="LLM synthetic",service="LLM",provider="SYNTHETIC",model="fixture")
    @Test fun originalProjectUndoRedoAndRestart() {
        val r=root();val bytes=EditorRenderer.synthetic(128,128);var id="";var project=""
        StudioWorkspace.open(r).use {w->
            id=w.importImage("original.png",bytes);project=w.newProject("First",listOf(id))
            w.applyLayers(project,setOf(id),listOf(Layer(kind="RECT")))
            w.edit(project){it.undo()};assertTrue(w.state.projects[0].current.pages[0].layers.isEmpty())
            w.edit(project){it.redo()};assertEquals(1,w.state.projects[0].current.pages[0].layers.size)
            assertContentEquals(bytes,w.bytes(id));assertFails {StudioWorkspace.open(r)}
        }
        StudioWorkspace.open(r).use {w->assertContentEquals(bytes,w.bytes(id));assertEquals(project,w.state.projects[0].id);assertEquals(1,w.state.projects[0].current.pages[0].layers.size)}
    }
    @Test fun stagedUnknownCorruptAndFutureDocumentsNeverDefault() {
        val r=root();StudioWorkspace.open(r).use {w->w.save(w.state.copy(accounts=listOf(nai())))}
        val record=Files.list(r.resolve("documents")).use {it.findFirst().get()};val original=Files.readAllBytes(record)
        Files.writeString(record,"corrupt");assertEquals("CORRUPT",assertFailsWith<DataProblem>{StudioWorkspace.open(r)}.status)
        assertEquals("corrupt",Files.readString(record))
        Files.write(record,original)
        val future=original.decodeToString().replaceFirst("\"version\":1","\"version\":2")
        Files.writeString(record,future);assertEquals("UNSUPPORTED_VERSION",assertFailsWith<DataProblem>{StudioWorkspace.open(r)}.status)
        assertEquals(future,Files.readString(record))
        val r2=root()
        StudioWorkspace.open(r2){if(it=="DOCUMENT_STAGED")throw java.io.IOException("injected")}.use {w->
            assertFails {w.save(w.state.copy(accounts=listOf(nai())))};assertFails {w.save(StudioState())}
        }
        assertEquals("RECOVERY_REQUIRED",assertFailsWith<DataProblem>{StudioWorkspace.open(r2)}.status)
    }
    @Test fun completeChainRejectsDeletedRevisionAndUnknownEntry() {
        val r=root();StudioWorkspace.open(r).use {w->w.save(StudioState(accounts=listOf(nai())));w.save(w.state.copy(accounts=w.state.accounts+llm()))}
        val files=Files.list(r.resolve("documents")).use {it.sorted().toList()}
        // This is a disposable synthetic fault fixture; no application delete path exists.
        Files.move(files[0],r.resolve("held-record"));assertFails {StudioWorkspace.open(r)}
        Files.move(r.resolve("held-record"),files[0]);Files.writeString(r.resolve("documents/unknown.bin"),"retained")
        assertEquals("RECOVERY_REQUIRED",assertFailsWith<DataProblem>{StudioWorkspace.open(r)}.status)
    }
    @Test fun generationDesignRevisionFrozenIdentityAndUncertainRestart()=runBlocking {
        val r=root();StudioWorkspace.open(r).use {w->
            val engine=GenerationEngine(w);val account=nai()
            val first=engine.design("cup",llm());val revised=engine.design("warm light",llm(),first)
            assertNotEquals(first.baseCaption,revised.baseCaption);assertEquals(2,w.state.designs.size)
            var intermediate=false
            val job=engine.generate(revised,NovelAiGenerationSettings(customWidth=128,customHeight=128,seedMode=NovelAiSeedMode.FIXED,seed=42,count=2),account){intermediate=true}
            assertTrue(intermediate);assertEquals("COMPLETED",job.state);assertEquals(2,job.results.size)
            assertEquals(account,job.account);assertTrue(job.recipe.contains(AUTHORITY_SHA));assertFalse(job.recipe.contains("SYNTHETIC_NOT_A_CREDENTIAL"))
            assertEquals("FAILED",engine.generate(first,NovelAiGenerationSettings(customWidth=128,customHeight=128),account,scenario="HTTP_429").state)
            w.save(w.state.copy(jobs=w.state.jobs+JobRecord(account=account,recipe=job.recipe,state="RUNNING")))
        }
        StudioWorkspace.open(r).use {w->assertEquals("UNCERTAIN",w.state.jobs.last().state);assertEquals(2,w.catalog().size)}
    }
    @Test fun batchOrderUndoAndExportCategoriesAreNonDestructive() {
        StudioWorkspace.open(root()).use {w->
            val bytes=EditorRenderer.synthetic(128,128);val ids=(1..2).map {w.importImage("$it.png",bytes,"{\"private\":\"synthetic marker\"}")}
            val pid=w.newProject("Batch",ids)
            val layers=listOf(Layer(kind="RECT",mosaic=true),Layer(kind="BRUSH",width=8f,points=listOf(Point(1f,1f),Point(40f,40f))),Layer(kind="POLYGON",points=listOf(Point(0f,0f),Point(50f,0f),Point(25f,40f))),Layer(kind="TEXT",text="合成",vertical=true))
            w.applyLayers(pid,ids.toSet(),layers);w.edit(pid){it.change(it.current.copy(pages=it.current.pages.reversed()))}
            assertEquals(ids.reversed(),w.state.projects[0].current.pages.map {it.assetId})
            w.edit(pid){it.undo()};assertEquals(ids,w.state.projects[0].current.pages.map {it.assetId})
            val privacy=w.export(pid,ids,"PRIVACY");val normal=w.export(pid,ids,"NORMAL");val raw=w.export(pid,ids,"ORIGINAL")
            assertTrue(Files.exists(privacy.resolve("COMPLETE.txt")));assertTrue(Files.exists(normal.resolve("COMPLETE.txt")))
            for(id in ids)assertContentEquals(bytes,w.bytes(id))
            Files.list(raw).use {it.filter {p->p.toString().endsWith(".png")}.forEach {p->assertContentEquals(bytes,Files.readAllBytes(p))}}
            val p=Files.list(privacy).use {it.filter {p->p.toString().endsWith(".png")}.findFirst().get()}
            val n=Files.list(normal).use {it.filter {p->p.toString().endsWith(".png")}.findFirst().get()}
            assertFalse(Files.readAllBytes(p).decodeToString().contains("CCBStudioRecipe"));assertTrue(Files.readAllBytes(n).decodeToString().contains("CCBStudioRecipe"))
            assertTrue(Files.exists(w.backup().resolve("COMPLETE.txt")))
        }
    }
    @Test fun missingFontPreventsFalseCompleteExportAndUnknownLayerRefused() {
        val rendered=EditorRenderer.render(EditorRenderer.synthetic(128,128),listOf(Layer(kind="TEXT",font="__missing_fixture_font__")))
        assertTrue(rendered.warnings.isNotEmpty());assertFails {EditorRenderer.encode(rendered,true)}
        assertFails {Layer(kind="FUTURE").validate()}
    }
    @Test fun completeRecipeComparisonIncludesNestedCharactersSeedAndIdentity() {
        val a="""{"plan":{"characters":[{"prompt":"a","x":0.2}]},"seed":1,"provider":"A"}"""
        val b="""{"plan":{"characters":[{"prompt":"b","x":0.8}]},"seed":2,"provider":"B"}"""
        val diff=compareRecipes(a,b);assertEquals(4,diff.size);assertTrue(diff.any {it.startsWith("recipe.plan.characters[0].x")})
    }
    @Test fun dpapiSyntheticSecretRoundTripNotInDocuments() {
        val r=root();StudioWorkspace.open(r).use {w->
            val vault=SecretVault(r.resolve("secrets"));val fixture="SYNTHETIC-SECRET-ONLY".toByteArray();val ref=vault.put(fixture)
            vault.use(ref){assertContentEquals(fixture,it)}
            assertFalse(Files.readAllBytes(r.resolve("secrets/$ref.dpapi")).decodeToString().contains("SYNTHETIC-SECRET-ONLY"))
            w.save(w.state.copy(accounts=listOf(nai().copy(secretRef=ref))))
            assertFalse(documentJson.encodeToString(w.state).contains("SYNTHETIC-SECRET-ONLY"))
            val backup=w.backup();assertFalse(Files.exists(backup.resolve("secrets")))
        }
    }
}
