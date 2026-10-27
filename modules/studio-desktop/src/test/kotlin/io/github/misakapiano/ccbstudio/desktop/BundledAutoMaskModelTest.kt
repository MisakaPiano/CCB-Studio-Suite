package io.github.misakapiano.ccbstudio.desktop

import io.github.misakapiano.ccbstudio.core.*
import com.example.chatbar.desktop.DesktopNovelAiInfrastructure
import kotlinx.coroutines.*
import java.nio.file.Path
import kotlin.test.*

class BundledAutoMaskModelTest {
    private suspend fun idle(c:WorkbenchController){withTimeout(60000){c.active?.join()};assertNull(c.problem,c.problem)}
    @Test fun shippedResourceHasPinnedBytesMetadataAndRejectsCorruption(){
        val model=BundledAutoMaskModel.load()
        assertEquals(47600269,model.bytes.size);assertEquals(TypesetAutoMaskResources.BUNDLED_SHA256,model.sha256)
        assertEquals(setOf("nipple","vagina","penis","anus","pubic hair","female face","male face"),model.classes.toSet())
        val notice=javaClass.getResourceAsStream("/models/automask/MODEL_NOTICE.md")!!.bufferedReader().use{it.readText()}
        assertTrue("1697d5d1827b6a818b350b44bf3ec27f08837a2a" in notice&&"MIT" in notice)
        assertFailsWith<IllegalArgumentException>{BundledAutoMaskModel.verify(model.bytes.copyOf(100))}
        val changed=model.bytes.clone();changed[changed.lastIndex]=(changed.last().toInt()xor 1).toByte()
        assertFailsWith<IllegalArgumentException>{BundledAutoMaskModel.verify(changed)}
    }
    @Test fun firstUseNeedsNoPickerAndBuiltinSettingsReopenAndRestoreWithoutWeightCopies()=runBlocking{
        val root=Path.of(System.getProperty("preview.test.root")).resolve(newId());val w=StudioWorkspace.openStudio(root.resolve("workspace"));val original=EditorRenderer.synthetic(128,64);val id=w.importImage("synthetic-default.png",original)
        val scope=CoroutineScope(SupervisorJob()+Dispatchers.Default);val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root.resolve("catalog")),root.resolve("budget"),admissionOverride=RequestAdmission{error("No real services")})
        lateinit var backup:Path
        try{
            c.editor.create("Bundled model",listOf(id));idle(c);val a=c.editor.autoMask;a.chooseModel={error("Default must not open a model picker")}
            var settingsNeeded=false;a.runConfigured{settingsNeeded=true};idle(c)
            assertFalse(settingsNeeded);assertTrue(a.configured);assertEquals(TypesetAutoMaskResources.BUNDLED_SHA256,a.model!!.sha256);assertFalse(a.classes.any{it.contains("face")})
            a.saveConfiguration();idle(c);val saved=TypesetEditorExtrasStore.load(w).autoMask!!;assertTrue(TypesetAutoMaskResources.isBundled(saved))
            assertTrue(w.catalog().none{it.request().project().contentEquals(TypesetAutoMaskResources.TAG.toByteArray())})
            assertContentEquals(original,w.bytes(id));backup=w.backup()
        }finally{c.close();scope.cancel()}
        StudioRestore.restore(backup,root.resolve("restored"))
        StudioWorkspace.openStudio(root.resolve("restored")).use{again->
            assertTrue(TypesetAutoMaskResources.isBundled(TypesetEditorExtrasStore.load(again).autoMask!!));assertTrue(again.catalog().none{it.request().project().contentEquals(TypesetAutoMaskResources.TAG.toByteArray())})
            val s=CoroutineScope(SupervisorJob()+Dispatchers.Default);val c2=WorkbenchController(again,s,DesktopNovelAiInfrastructure(root.resolve("catalog2")),root.resolve("budget2"),admissionOverride=RequestAdmission{error("No real services")})
            try{c2.editor.autoMask.chooseModel={error("Restored config must use the bundled model")};c2.editor.autoMask.readSettings();idle(c2);assertEquals(TypesetAutoMaskResources.BUNDLED_SHA256,c2.editor.autoMask.model!!.sha256)}finally{c2.close();s.cancel()}
        }
    }
}

/** Test-only, launched with production JARs to verify the actual shipped classpath. */
internal object BundledAutoMaskPackageProbe {
    @JvmStatic fun main(args:Array<String>){
        val url=BundledAutoMaskModel::class.java.getResource(BundledAutoMaskModel.RESOURCE)?:error("Missing shipped model")
        check(url.protocol=="jar"){"This probe must read production JAR resources"}
        val model=BundledAutoMaskModel.load()
        check(model.sha256==TypesetAutoMaskResources.BUNDLED_SHA256)
        println("BUNDLED_MODEL_JAR_PASS bytes=${model.bytes.size} sha256=${model.sha256} classes=${model.classes.size} resourceProtocol=${url.protocol}")
    }
}
