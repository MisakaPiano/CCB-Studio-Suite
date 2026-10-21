@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class, androidx.compose.ui.InternalComposeUiApi::class)
package com.example.chatbar.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Constraints
import com.example.chatbar.domain.image.*
import io.github.misakapiano.ccbstudio.core.*
import io.github.misakapiano.ccbstudio.desktop.WorkbenchController
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.test.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*

class DesktopCharacterPositionsTest {
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    private fun root()=Path.of(System.getProperty("preview.test.root"),newId())
    private suspend fun ImageComposeScene.frames(){repeat(5){render(System.nanoTime()).close();yield()};delay(80);render(System.nanoTime()).close()}
    private fun ImageComposeScene.nodes():List<SemanticsNode>{
        fun walk(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::walk)
        return semanticsOwners.flatMap{walk(it.rootSemanticsNode)}
    }
    private fun SemanticsNode.matches(s:String)=config.getOrNull(SemanticsProperties.Text)?.any{it.text==s}==true||config.getOrNull(SemanticsProperties.ContentDescription)?.contains(s)==true
    private fun ImageComposeScene.field(label:String)=nodes().first{it.config.getOrNull(SemanticsActions.SetText)!=null&&(it.matches(label)||it.children.any{child->child.matches(label)})}
    private suspend fun ImageComposeScene.type(label:String,value:String){assertTrue(field(label).config[SemanticsActions.SetText].action!!.invoke(AnnotatedString(value)));frames()}
    private suspend fun ImageComposeScene.click(label:String){val n=nodes().first{it.matches(label)&&it.config.getOrNull(SemanticsActions.OnClick)!=null};assertTrue(n.config[SemanticsActions.OnClick].action!!.invoke());frames()}
    private fun ImageComposeScene.shot(name:String){val p=Path.of("build/character-position-evidence/$name.png");Files.createDirectories(p.parent);render(System.nanoTime()).use{im->im.encodeToData()!!.use{Files.write(p,it.bytes)}}}

    @Test fun positionFieldsDoNotOverlapAndTypedCoordinatesReachRequestAndReopen()=runBlocking(awt){
        for(model in NovelAiImageModel.entries){
            val r=root();val w=StudioWorkspace.openStudio(r);val scope=CoroutineScope(SupervisorJob()+awt)
            val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),admissionOverride=RequestAdmission{error("No provider call")})
            c.edit{it.copy(basePrompt="position fixture",characters=listOf(NovelAiCharacterPromptDraft(prompt="first"),NovelAiCharacterPromptDraft(prompt="second"),NovelAiCharacterPromptDraft(prompt="disabled",enabled=false,center=DesignedCharacterCenter(.4f,.4f))))
                .withActiveSettings(it.activeSettings.copy(model=model,useCharacterPositions=true))}
            // Same Surface parent as the real dialog; a test Column would conceal overlapping siblings.
            val scene=ImageComposeScene(640,900,coroutineContext=awt){Surface(color=DesktopBootstrapColors.background,contentColor=DesktopBootstrapColors.foreground,modifier=Modifier.fillMaxSize()){DesktopCharacterPositions(c.draft,c::edit)}}
            var expected=emptyList<NovelAiCharacterPromptDraft>()
            try{
                scene.frames();scene.shot("${model.name}-initial")
                val x=scene.field("X %").boundsInRoot;val y=scene.field("Y %").boundsInRoot
                assertTrue(x.height>0 && y.height>0 && y.top>=x.bottom,"X/Y controls overlap: $x / $y")
                scene.type("X %","34");scene.type("Y %","78")
                val first=if(model==NovelAiImageModel.V4_5_FULL)DesignedCharacterCenter(.3f,.7f)else DesignedCharacterCenter(.34f,.78f)
                assertEquals(first,c.draft.characters[0].center)
                scene.click("角色 2");scene.type("X %","99");scene.type("Y %","1")
                val second=if(model==NovelAiImageModel.V4_5_FULL)DesignedCharacterCenter(.9f,.1f)else DesignedCharacterCenter(.99f,.01f)
                assertEquals(second,c.draft.characters[1].center)
                val request=Json.parseToJsonElement(NovelAiImageService().buildRequestBody(c.draft.toPromptPlan(),c.draft.activeSettings.imageSize(),c.draft.activeSettings)).jsonObject.getValue("parameters").jsonObject
                assertTrue(request.getValue("use_coords").jsonPrimitive.boolean)
                val captions=request.getValue("v4_prompt").jsonObject.getValue("caption").jsonObject.getValue("char_captions").jsonArray
                assertEquals(2,captions.size)
                for((i,center) in listOf(first,second).withIndex()){
                    val wire=captions[i].jsonObject.getValue("centers").jsonArray.single().jsonObject
                    assertEquals(center.x,wire.getValue("x").jsonPrimitive.float);assertEquals(center.y,wire.getValue("y").jsonPrimitive.float)
                }
                scene.shot("${model.name}-custom")
                scene.click("重置为均匀分布");assertTrue(c.draft.activeCharacters.all{it.center==null})
                assertEquals(DesignedCharacterCenter(.4f,.4f),c.draft.characters.last().center)
                scene.type("X %","34");scene.type("Y %","78");expected=c.draft.characters.toList();c.flush()
                scene.constraints=Constraints.fixed(640,650);scene.frames()
                val scroll=scene.nodes().first{it.config.getOrNull(SemanticsActions.ScrollBy)!=null}
                assertTrue(scroll.config[SemanticsProperties.VerticalScrollAxisRange].maxValue()>0f)
                assertTrue(scroll.config[SemanticsActions.ScrollBy].action!!.invoke(0f,1000f));scene.frames()
                val reset=scene.nodes().first{it.matches("重置为均匀分布")&&it.config.getOrNull(SemanticsActions.OnClick)!=null}.boundsInRoot
                assertTrue(reset.height>0 && reset.top>=0 && reset.bottom<=650,"Reset must be reachable in the real dialog size: $reset")
                scene.shot("${model.name}-scrolled-650")
            }finally{scene.close();c.close();scope.cancel()}
            StudioWorkspace.openStudio(r).use{again->assertEquals(expected,again.state.creation!!.draft.characters);assertEquals(model,again.state.creation!!.draft.selectedModel);assertTrue(again.state.creation!!.draft.activeSettings.useCharacterPositions)}
        }
    }
}
