@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class,androidx.compose.ui.InternalComposeUiApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.material.MaterialTheme
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.*
import com.example.chatbar.desktop.DesktopNovelAiInfrastructure
import com.example.chatbar.domain.image.*
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import java.nio.file.Path
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.test.*

class DesignPromptReferenceTest {
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    private fun root()=Path.of(System.getProperty("preview.test.root"),newId())
    private suspend fun ImageComposeScene.frames(){repeat(4){render(System.nanoTime()).close();yield()};delay(80)}
    private fun ImageComposeScene.nodes():List<SemanticsNode>{
        fun walk(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::walk)
        return semanticsOwners.flatMap{walk(it.rootSemanticsNode)}
    }
    private suspend fun ImageComposeScene.click(s:String){
        val n=nodes().first{it.config.getOrNull(SemanticsProperties.Text)?.any{t->t.text==s}==true&&it.config.getOrNull(SemanticsActions.OnClick)!=null}
        assertTrue(n.config[SemanticsActions.OnClick].action!!.invoke());frames()
    }
    @Test fun candidateAndStandaloneReferencesPreserveRawRolesAndDraftWithoutSending()=runBlocking(awt){
        val r=root();val scope=CoroutineScope(SupervisorJob()+awt);val w=StudioWorkspace.openStudio(r);var requests=0
        val http=okhttp3.OkHttpClient.Builder().addInterceptor{requests++;error("No network allowed for comparison")}.build()
        val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),http)
        val plan=NovelAiPromptPlan("  quiet room， window\n",listOf(
            NovelAiCharacterCaption("first person, red coat",DesignedCharacterCenter(.25f,.5f),"first unwanted"),
            NovelAiCharacterCaption("second person, blue coat",DesignedCharacterCenter(.75f,.5f),"  ")),
            negativePrompt="",stylePrompt="watercolor, ")
        val turn=NovelAiDesignTurn(userText="Synthetic design",reply=NovelAiDesignReply(plan,NovelAiImageModel.V5_FULL,"fixture-language"),status=NovelAiDesignTurnStatus.COMPLETED)
        val conversation=NovelAiDesignConversation(turns=listOf(turn))
        c.session.update{it.copy(conversations=listOf(conversation),currentConversationId=conversation.id)}
        c.edit{it.copy(basePrompt="Existing creation must remain",stylePrompt="Existing style")};c.input("Unsent request must remain");c.flush()
        val before=c.draft;val beforeHistory=w.state.creation!!.conversations
        val scene=ImageComposeScene(1120,820,coroutineContext=awt){MaterialTheme{DesignWorkspace(c,true)}}
        var candidate="";var standalone=""
        try{
            scene.frames();scene.click("候选加入多 Prompt 参照");c.active?.join();assertNull(c.problem,c.problem)
            val board=PromptReferences(c.library.repository);candidate=board.load().ids.single()
            val saved=c.library.repository.prompt(candidate)
            assertEquals(plan.baseCaption,saved.sections.single{it.id=="base"}.text)
            assertEquals(plan.stylePrompt,saved.sections.single{it.id=="style"}.text)
            assertTrue(saved.sections.none{it.id=="negative"||it.id=="role:1:negative"})
            assertEquals(plan.characterCaptions.map{it.prompt},saved.sections.filter{it.label=="角色正向"}.map{it.text})
            assertEquals(turn.id,saved.sections.first().attributes["设计轮次"]);assertNull(saved.image)
            assertEquals(listOf("role:0","role:1"),board.roles(candidate))
            board.moveRole(candidate,"role:1","role:0")
            assertEquals(listOf("role:1","role:0"),board.roles(candidate));assertEquals(saved,c.library.repository.prompt(candidate))
            scene.click("候选加入多 Prompt 参照");c.active?.join();assertNull(c.problem,c.problem)
            assertEquals(listOf(candidate),board.load().ids);assertEquals(1,c.library.repository.prompts().size)
            // Standalone pasted/imported text can be saved directly to comparison, independently of creation.
            val raw="  standalone prompt， keep exact\n"
            c.library.savePromptReference(raw,"Synthetic standalone");c.active?.join();assertNull(c.problem,c.problem)
            standalone=board.load().ids.single{it!=candidate};assertEquals(raw,c.library.repository.prompt(standalone).text)
            assertEquals(before,c.draft);assertEquals(beforeHistory,w.state.creation!!.conversations)
            assertEquals("Unsent request must remain",c.composer);assertNull(DesignProvenance(w).current())
            assertEquals(0,requests);assertTrue(w.state.jobs.isEmpty());c.flush()
        }finally{scene.close();c.close();scope.cancel()}
        StudioWorkspace.openStudio(r).use{again->
            val repository=LibraryRepository(again);val board=PromptReferences(repository)
            assertEquals(listOf(candidate,standalone),board.load().ids);assertEquals(listOf("role:1","role:0"),board.roles(candidate))
            assertEquals(plan.baseCaption,repository.prompt(candidate).sections.single{it.id=="base"}.text)
            assertEquals(before,again.state.creation!!.draft);assertEquals("Unsent request must remain",again.state.creation!!.composer)
        }
    }
}
