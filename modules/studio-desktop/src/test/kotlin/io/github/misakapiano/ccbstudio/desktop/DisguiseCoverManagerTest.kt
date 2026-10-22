@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class,androidx.compose.ui.InternalComposeUiApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.material.*
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import com.example.chatbar.desktop.*
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import java.nio.file.*
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.test.*

class DisguiseCoverManagerTest {
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    private fun root()=Path.of(System.getProperty("preview.test.root"),newId())
    @Test fun filePickerAddsMultipleNamedThumbnailsAndRenameRemoveDefaultReopenPreserveBytes()=runBlocking(awt){
        val data=root();val w=StudioWorkspace.openStudio(data);val scope=CoroutineScope(SupervisorJob()+awt)
        val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),admissionOverride=RequestAdmission{error("No HTTP")})
        val inputs=Files.createDirectories(root());val green=EditorRenderer.synthetic(32,34);val blue=EditorRenderer.synthetic(34,32)
        val paths=listOf(Files.write(inputs.resolve("cover-green.png"),green),Files.write(inputs.resolve("cover-blue.png"),blue))
        var closed=false
        val scene=ImageComposeScene(1000,740,coroutineContext=awt){MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel)){DisguiseCoverManagerContent(c,{closed=true})}}
        suspend fun frames(){repeat(4){scene.render().close();yield()};delay(80)}
        fun nodes():List<SemanticsNode>{
            fun walk(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::walk)
            return scene.semanticsOwners.flatMap{walk(it.rootSemanticsNode)}
        }
        fun SemanticsNode.has(label:String)=config.getOrNull(SemanticsProperties.ContentDescription)?.contains(label)==true||config.getOrNull(SemanticsProperties.Text)?.any{it.text==label}==true
        suspend fun click(label:String){nodes().first{it.has(label)&&it.config.getOrNull(SemanticsActions.OnClick)!=null}.config[SemanticsActions.OnClick].action!!.invoke();c.active?.join();frames();assertNull(c.problem)}
        var greenId="";var blueId=""
        try {
            frames();assertTrue(nodes().none{it.config.getOrNull(SemanticsActions.SetText)!=null&&it.has("封面名称（留空用文件名）")})
            for(path in paths){c.animation.chooseCoverFile={path};click("添加封面")}
            val covers=c.animation.coverState!!.covers;assertEquals(listOf("cover-green","cover-blue"),covers.map{it.name});greenId=covers[0].id;blueId=covers[1].id
            assertTrue(nodes().any{it.has("封面缩略图 $greenId")});assertTrue(nodes().any{it.has("封面缩略图 $blueId")})
            c.animation.chooseCoverFile={null};click("添加封面");assertEquals(2,c.animation.coverState!!.covers.size)
            click("重命名")
            nodes().first{it.config.getOrNull(SemanticsActions.SetText)!=null&&it.config.getOrNull(SemanticsProperties.EditableText)?.text=="cover-blue"}.config[SemanticsActions.SetText].action!!.invoke(AnnotatedString("Blue renamed"))
            click("保存名称");assertEquals("Blue renamed",c.animation.coverState!!.covers.last().name)
            click("恢复默认封面");assertNull(c.animation.coverState!!.selected);assertEquals(2,c.animation.coverState!!.covers.size)
            click("选择封面 Blue renamed");click("移除封面");click("确认移除")
            assertEquals(listOf(greenId),c.animation.coverState!!.covers.map{it.id});assertContentEquals(blue,w.bytes(blueId))
            click("选择封面 cover-green");click("完成");assertTrue(closed);assertContentEquals(green,w.bytes(greenId))
        }finally{scene.close();c.close();scope.cancel();w.close()}
        StudioWorkspace.openStudio(data).use{again->val saved=DisguiseCovers(again).load();assertEquals(greenId,saved.selected);assertEquals(1,saved.covers.size);assertContentEquals(blue,again.bytes(blueId))}
    }
}
