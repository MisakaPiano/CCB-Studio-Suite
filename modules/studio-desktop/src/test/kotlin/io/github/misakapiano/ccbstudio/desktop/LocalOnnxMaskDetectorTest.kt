package io.github.misakapiano.ccbstudio.desktop

import io.github.misakapiano.ccbstudio.core.*
import com.example.chatbar.desktop.*
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.nio.*
import java.nio.file.*
import kotlin.test.*

/** Tiny self-authored ONNX graph, executed by the real Windows ORT DLL.
 * Constant detections test wiring/coordinates/NMS; they are NOT recognition accuracy. */
class LocalOnnxMaskDetectorTest {
    @Test fun externalTensorPathsAreRejectedBeforeNativeModelParsing(){
        assertFailsWith<IllegalStateException>{LocalOnnxMaskDetector.inspect("external.onnx",syntheticModel(external=true))}
    }
    @Test fun realRuntimeModelMetadataLetterboxNmsExpansionAndCancellation(){
        val bytes=syntheticModel();val model=LocalOnnxMaskDetector.inspect("synthetic.onnx",bytes)
        assertEquals(listOf("synthetic_target"),model.classes);assertEquals(sha256(bytes),model.sha256)
        val image=EditorRenderer.decode(EditorRenderer.synthetic(128,64))
        try{
            val result=LocalOnnxMaskDetector.detect(model,image,AutoMaskOptions(expansion=0.0),model.classes.toSet())
            assertEquals(1,result.size);val region=result.single();assertEquals(44.0,region.x,.001);assertEquals(12.0,region.y,.001);assertEquals(40.0,region.width,.001)
            val expanded=LocalOnnxMaskDetector.detect(model,image,AutoMaskOptions(expansion=4.0),model.classes.toSet()).single();assertEquals(40.0,expanded.x,.001);assertEquals(48.0,expanded.width,.001)
            assertFailsWith<CancellationException>{LocalOnnxMaskDetector.detect(model,image,AutoMaskOptions(),model.classes.toSet()){throw CancellationException("synthetic cancellation")}}
            assertFailsWith<IllegalArgumentException>{LocalOnnxMaskDetector.detect(model,image,AutoMaskOptions(),setOf("unknown"))}
        }finally{image.flush()}
    }
    @Test fun reviewAppliesOneUndoableRecipeRejectsStaleResultsAndPreservesOriginals()=runBlocking{
        val root=Path.of(System.getProperty("preview.test.root")).resolve(newId());val w=StudioWorkspace.openStudio(root)
        val scope=CoroutineScope(SupervisorJob()+Dispatchers.Default);val source=EditorRenderer.synthetic(128,64);val id=w.importImage("synthetic.png",source)
        val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root.resolve("catalog")),root.resolve("budget"),admissionOverride=RequestAdmission{error("No service")})
        suspend fun idle(){withTimeout(30000){c.active?.join()};assertNull(c.problem,c.problem)}
        try{
            c.editor.create("AI合成接线",listOf(id,id));idle();val a=c.editor.autoMask;a.chooseModel={"synthetic.onnx" to syntheticModel()};a.loadModel();idle();assertNotNull(a.model)
            a.selectedPages=true;c.editor.selectedPages=c.editor.document!!.pages.map{it.id}.toSet();a.detect();idle()
            assertEquals(2,a.review!!.results.size);assertEquals(2,a.selectedRegions.size);assertTrue(c.editor.document!!.pages.all{it.masks.isEmpty()})
            c.editor.pageChange{it.copy(caption="changed after detection")};assertFalse(a.apply());assertTrue(c.editor.document!!.pages.all{it.masks.isEmpty()})
            c.problem=null;c.editor.drain();a.detect();idle();val revision=c.editor.project!!.revision
            assertTrue(a.apply());c.editor.drain();assertEquals(revision+1,c.editor.project!!.revision);assertTrue(c.editor.document!!.pages.all{it.masks.single().shape=="rect"})
            assertContentEquals(source,w.bytes(id));c.editor.history();idle();assertTrue(c.editor.document!!.pages.all{it.masks.isEmpty()})
            assertFalse(a.apply(),"A completed review cannot be applied twice")
            val out=Path.of(System.getProperty("preview.test.root")).resolve("ai-native-results");Files.createDirectories(out)
            Files.writeString(out.resolve("pipeline.txt"),"engine=ONNX Runtime 1.31.1 Windows CPU\nmodel=SELF_AUTHORED_CONSTANT_GRAPH\naccuracy=NOT TESTED\noriginalSha256=${sha256(source)}\n")
            Unit
        }finally{c.close();scope.cancel();w.close()}
    }
    @Test fun referenceModelExecutesOnlyAgainstGeneratedAbstractRasterNotPrivateImages(){
        val path=Path.of(requireNotNull(System.getProperty("studio.syntheticReferenceModel")))
        val bytes=readBounded(path,128*1024*1024)
        assertEquals("a12ac5532e93be9dfeb96a77fc3f3647791335c9df0de9a18fcd503f7877a828",sha256(bytes))
        val model=LocalOnnxMaskDetector.inspect(path.fileName.toString(),bytes)
        val input=EditorRenderer.synthetic(320,240);val image=EditorRenderer.decode(input);val start=System.nanoTime()
        try{
            val regions=LocalOnnxMaskDetector.detect(model,image,AutoMaskOptions(),model.classes.filterNot{it.contains("face",true)}.toSet())
            assertTrue(regions.all{it.x>=0&&it.y>=0&&it.x+it.width<=320&&it.y+it.height<=240})
            val out=Path.of(System.getProperty("preview.test.root")).resolve("ai-native-results");Files.createDirectories(out)
            Files.writeString(out.resolve("pretrained-native.txt"),"modelSha256=${model.sha256}\ninput=GENERATED_ABSTRACT_RASTER\ninputSha256=${sha256(input)}\nclasses=${model.classes}\nregions=${regions.size}\nseconds=${(System.nanoTime()-start)/1e9}\nrecognitionAccuracy=NOT TESTED\nmodelRedistributionLicense=UNKNOWN_NOT_DISTRIBUTED\n")
        }finally{image.flush()}
    }
    companion object {
        internal fun syntheticModel(external:Boolean=false):ByteArray {
            fun v(value:Long):ByteArray{var n=value;val out=ByteArrayOutputStream();while(n>=128){out.write((n.toInt()and 127)or 128);n=n ushr 7};out.write(n.toInt());return out.toByteArray()}
            fun i(field:Int,value:Long)=v((field*8).toLong())+v(value)
            fun b(field:Int,value:ByteArray)=v((field*8+2).toLong())+v(value.size.toLong())+value
            fun s(field:Int,value:String)=b(field,value.toByteArray())
            fun info(name:String,dims:List<Int>):ByteArray{val shape=dims.fold(byteArrayOf()){all,n->all+b(1,i(1,n.toLong()))};return s(1,name)+b(2,b(1,i(1,1)+b(2,shape)))}
            val values=floatArrayOf(32f,33f,10f,32f,33f,10f,20f,20f,8f,20f,20f,8f,.9f,.8f,.1f)
            val raw=ByteBuffer.allocate(values.size*4).order(ByteOrder.LITTLE_ENDIAN).also{buf->values.forEach{buf.putFloat(it)}}.array()
            val tensor=i(1,1)+i(1,5)+i(1,3)+i(2,1)+s(8,"fixed")+b(9,raw)
            val node=s(2,"predictions")+s(4,"Constant")+b(5,s(1,"value")+b(5,tensor)+i(20,4))
            val initializer=if(external)b(5,i(1,1)+i(2,1)+s(8,"forbidden-external")+b(13,s(1,"location")+s(2,"../must-not-read.bin"))+i(14,1))else byteArrayOf()
            val graph=b(1,node)+s(2,"synthetic-wiring-only")+initializer+b(11,info("images",listOf(1,3,64,64)))+b(12,info("predictions",listOf(1,5,3)))
            return i(1,8)+s(2,"CCB synthetic test - not a recognition model")+b(7,graph)+b(8,i(2,13))+b(14,s(1,"names")+s(2,"{0: 'synthetic_target'}"))
        }
    }
}
