@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class,androidx.compose.ui.InternalComposeUiApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.material.*
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.*
import com.example.chatbar.desktop.*
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import java.nio.file.Path
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.test.*

class AccountRolesTest {
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    private fun root()=Path.of(System.getProperty("preview.test.root"),newId())
    @Test fun separateSelectionsAreVisibleAndPersistWithoutStaleEditorOverwrite()=runBlocking(awt) {
        val r=root();val w=StudioWorkspace.openStudio(r);val scope=CoroutineScope(SupervisorJob()+awt)
        val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root())
        var language="";var visual="";var nai=""
        val scene=ImageComposeScene(1280,1100,coroutineContext=awt){MaterialTheme{Workbench(c,DesktopWindowChrome{},false)}}
        suspend fun frames(){repeat(4){scene.render(System.nanoTime()).close();yield()};delay(80)}
        fun nodes():List<SemanticsNode> {
            fun walk(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::walk)
            return scene.semanticsOwners.flatMap{walk(it.rootSemanticsNode)}
        }
        fun label(n:SemanticsNode,s:String)=n.config.getOrNull(SemanticsProperties.Text)?.any{it.text.contains(s)}==true
        try {
            val e=c.modelAccounts
            e.edit{it.copy(alias="Language A",model="text",key="SYNTHETIC_TEXT")};e.save();c.active?.join();language=c.llmId!!
            e.newVision();e.discard();assertTrue(e.visionEditing);assertTrue(e.draft.multimodal)
            e.edit{it.copy(alias="Vision B",model="vision",key="SYNTHETIC_VISION",useGlobalKey=false)};e.save(false);c.active?.join();visual=e.draft.existing!!.id
            assertEquals(language,c.llmId);assertNull(e.vision)
            c.chooseVision(visual);c.active?.join();assertEquals(visual,e.vision!!.id);assertEquals(language,c.llmId)
            e.open(c.state.accounts.single{it.id==language})
            c.chooseVision(null);c.active?.join();assertNull(e.draft.visionId)
            c.chooseVision(visual);c.active?.join();assertEquals(visual,e.draft.visionId)
            e.save(false);c.active?.join();assertEquals(visual,e.vision!!.id)
            e.newNai();e.edit{it.copy(alias="NAI C",key="SYNTHETIC_NAI")};e.save();c.active?.join();nai=c.naiId!!
            assertEquals(language,c.llmId);assertEquals(visual,e.vision!!.id);assertEquals(nai,e.nai!!.id)
            c.view="账户";frames()
            assertTrue(nodes().any{label(it,"默认语言模型：Language A")})
            assertTrue(nodes().any{label(it,"当前视觉模型：Vision B")})
            assertTrue(nodes().any{label(it,"NAI 生图：NAI C")})
            e.category="视觉模型";frames()
            nodes().first{label(it,"停用当前视觉模型")&&it.config.getOrNull(SemanticsActions.OnClick)!=null}.config[SemanticsActions.OnClick].action!!.invoke()
            c.active?.join();frames();assertNull(e.vision);assertEquals(language,c.llmId);assertEquals(nai,c.naiId)
            c.chooseVision(visual);c.active?.join();c.flush();assertNull(c.problem)
        } finally {scene.close();c.modelAccounts.discard();c.close();scope.cancel()}
        StudioWorkspace.openStudio(r).use{reopened->
            assertEquals(language,reopened.state.creation!!.llmAccountId)
            assertEquals(nai,reopened.state.creation!!.naiAccountId)
            assertEquals(visual,reopened.state.accounts.single{it.id==language}.visionAccountId)
        }
    }
}
