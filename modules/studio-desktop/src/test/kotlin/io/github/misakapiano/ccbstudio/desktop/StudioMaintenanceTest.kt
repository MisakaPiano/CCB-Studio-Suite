package io.github.misakapiano.ccbstudio.desktop

import com.example.chatbar.desktop.DesktopNovelAiInfrastructure
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import java.nio.file.*
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.test.*

class StudioMaintenanceTest {
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    private fun root()=Path.of(System.getProperty("preview.test.root"),newId())
    @Test fun maintenanceRejectsNewEditsAndKeepsRootOwnedUntilWriterDrains()=runBlocking {
        val root=root();val w=StudioWorkspace.openStudio(root);val scope=CoroutineScope(SupervisorJob()+awt)
        val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root.resolveSibling(newId())),root.resolveSibling(newId()))
        val entered=CompletableDeferred<Unit>();val release=CompletableDeferred<Unit>()
        try {
            withContext(awt){c.input("saved input");c.run("controlled writer") {w.beginTask().use {withContext(NonCancellable){entered.complete(Unit);release.await()}}}}
            entered.await()
            val drain=scope.launch{c.enterMaintenance()}
            withTimeout(5000){while(!c.maintenance)delay(10)}
            withContext(awt){c.input("must not replace input");c.edit{it.copy(basePrompt="must not apply")};c.run("must not start"){error("Admission failed")}}
            assertNull(withTimeoutOrNull(100){drain.join();true});assertFails{StudioWorkspace.openStudio(root)}
            assertEquals("saved input",c.composer);assertNotEquals("must not apply",c.draft.basePrompt)
            release.complete(Unit);withTimeout(10000){drain.join()};assertTrue(c.maintenance);assertFails{StudioWorkspace.openStudio(root)}
            withContext(awt){c.resumeAfterPreflightFailure();c.input("resumed input");c.close()}
            StudioWorkspace.openStudio(root).use{assertEquals("resumed input",it.state.creation!!.composer)}
        } finally {release.complete(Unit);runCatching{withContext(awt){c.close()}};scope.cancel()}
    }
    @Test fun snapshotPreservesOriginalAssociationsAndDpapiCredentialWithoutPlaintext() {
        val root=root();val bytes=EditorRenderer.synthetic(50,40);val plain="synthetic-update-secret-only".toByteArray()
        val before=StudioWorkspace.openStudio(root).use{w->
            val id=w.importImage("synthetic.png",bytes,"synthetic recipe");w.importDerived("copy.png",bytes,id,"derived recipe")
            val ref=SecretVault(root.resolve("secrets")).put(plain)
            w.save(w.state.copy(accounts=listOf(AccountProfile(alias="synthetic update account",service="LLM",provider="SYNTHETIC",model="fixture",secretRef=ref))))
            updateSnapshot(w).also{assertFalse(it.toString().contains(plain.decodeToString()));assertTrue(it.toString().contains("recipeSha256"))}
        }
        StudioWorkspace.openStudio(root).use{w->assertEquals(before,updateSnapshot(w));assertEquals(2,w.catalog().size)}
        plain.fill(0)
    }
}
