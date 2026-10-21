package io.github.misakapiano.ccbstudio.desktop

import com.example.chatbar.desktop.DesktopNovelAiInfrastructure
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.test.*

class AutosaveCancellationTest {
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    private fun root()=Path.of(System.getProperty("preview.test.root")).resolve(newId())
    @Test fun cancellingDebouncedSaveDuringCommitDoesNotReportDataFailure()=runBlocking(awt) {
        val reached=CountDownLatch(1);val release=CountDownLatch(1);val armed=AtomicBoolean(false);val r=root()
        val w=StudioWorkspace.openStudio(r){if(it=="DOCUMENT_STAGED" && armed.compareAndSet(true,false)){reached.countDown();check(release.await(10,TimeUnit.SECONDS))}}
        val scope=CoroutineScope(SupervisorJob()+awt);val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),admissionOverride=RequestAdmission{})
        try {
            armed.set(true);c.edit{it.copy(basePrompt="retained draft")}
            assertTrue(withContext(Dispatchers.IO){reached.await(10,TimeUnit.SECONDS)})
            c.run("受控后续操作"){}
            yield() // run cancels and joins the in-flight autosave before proceeding.
            release.countDown();withTimeout(10000){c.active!!.join()}
            assertNull(c.problem);assertEquals("retained draft",w.state.creation!!.draft.basePrompt)
        }finally{release.countDown();c.close();scope.cancel()}
        StudioWorkspace.openStudio(r).use{assertEquals("retained draft",it.state.creation!!.draft.basePrompt)}
    }
}
