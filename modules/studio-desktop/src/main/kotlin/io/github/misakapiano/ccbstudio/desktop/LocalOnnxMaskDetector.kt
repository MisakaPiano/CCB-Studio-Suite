package io.github.misakapiano.ccbstudio.desktop

import ai.onnxruntime.*
import io.github.misakapiano.ccbstudio.core.*
import java.awt.image.BufferedImage
import java.nio.FloatBuffer
import kotlin.math.*

internal data class LocalMaskModel(val name:String,val bytes:ByteArray,val sha256:String,val classes:List<String>)
internal data class AutoMaskRegion(val id:String=newId(),val category:String,val confidence:Float,val x:Double,val y:Double,val width:Double,val height:Double)
internal data class AutoMaskOptions(val confidence:Float=.3f,val iou:Float=.5f,val resolution:Int=640,val expansion:Double=4.0){
    fun validate(){require(confidence in .05f..1f&&iou in .05f..1f&&resolution in listOf(640,832,1024,1280)&&expansion in 0.0..200.0)}
}

/** Independent local YOLO adapter. Uses detection boxes for conservative coverage;
 * segmentation prototypes are not represented as pixel masks in TypeSet documents.
 * No image upload, model download, custom operator library, Python or service call. */
internal object LocalOnnxMaskDetector {
    private fun sessionOptions()=OrtSession.SessionOptions().apply{setIntraOpNumThreads(2);setInterOpNumThreads(1)}
    fun inspect(name:String,bytes:ByteArray):LocalMaskModel {
        require(bytes.size in 1..128*1024*1024){"模型上限128MiB"}
        OnnxModelBoundary.validate(bytes)
        sessionOptions().use{opt->OrtEnvironment.getEnvironment().createSession(bytes,opt).use{s->
            val info=s.inputInfo.values.single().info as? TensorInfo?:error("需要单个图像张量输入")
            require(info.type==OnnxJavaType.FLOAT&&info.shape.size==4&&info.shape[0] in listOf(1L,-1L)&&info.shape[1]==3L){"需要NCHW float32 RGB模型"}
            val metadata=s.metadata.customMetadata["names"]?:error("模型缺少names类别表；为避免错认类别，不猜测顺序")
            require(metadata.length<=65536)
            val pairs=Regex("[\"']?(\\d+)[\"']?\\s*:\\s*[\"']([^\"']{1,80})[\"']").findAll(metadata).map{it.groupValues[1].toInt() to it.groupValues[2]}.toList()
            require(pairs.size in 1..80&&pairs.map{it.first}.sorted()==pairs.indices.toList()){ "模型类别表不受支持" }
            return LocalMaskModel(name.take(200),bytes,sha256(bytes),pairs.sortedBy{it.first}.map{it.second})
        }}
    }
    fun open(model:LocalMaskModel):Session {
        require(sha256(model.bytes)==model.sha256)
        OnnxModelBoundary.validate(model.bytes)
        val options=sessionOptions()
        try{return Session(model,options,OrtEnvironment.getEnvironment().createSession(model.bytes,options))}catch(ex:Throwable){options.close();throw ex}
    }
    fun detect(model:LocalMaskModel,image:BufferedImage,options:AutoMaskOptions,allowed:Set<String>,checkpoint:()->Unit={}):List<AutoMaskRegion> = open(model).use{it.detect(image,options,allowed,checkpoint)}
    class Session internal constructor(private val model:LocalMaskModel,private val settings:OrtSession.SessionOptions,private val s:OrtSession):AutoCloseable {
        override fun close(){try{s.close()}finally{settings.close()}}
        fun detect(image:BufferedImage,options:AutoMaskOptions,allowed:Set<String>,checkpoint:()->Unit={}):List<AutoMaskRegion> {
            options.validate();require(allowed.isNotEmpty()&&allowed.all{it in model.classes});checkpoint()
            val shape=(s.inputInfo.values.single().info as TensorInfo).shape
            val h=if(shape[2]>0)shape[2].toInt()else options.resolution;val w=if(shape[3]>0)shape[3].toInt()else options.resolution
            require(w in 32..1280&&h in 32..1280){"模型输入尺寸不受支持"}
            val ratio=min(w.toDouble()/image.width,h.toDouble()/image.height);val iw=(image.width*ratio).roundToInt().coerceAtLeast(1);val ih=(image.height*ratio).roundToInt().coerceAtLeast(1)
            val left=(w-iw)/2;val top=(h-ih)/2
            val input=BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);val tensor=FloatArray(w*h*3)
            try{val g=input.createGraphics();try{g.color=java.awt.Color(114,114,114);g.fillRect(0,0,w,h);g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);g.drawImage(image,left,top,iw,ih,null)}finally{g.dispose()}
                for(y in 0 until h)for(x in 0 until w){val rgb=input.getRGB(x,y);val i=y*w+x;tensor[i]=(rgb ushr 16 and 255)/255f;tensor[w*h+i]=(rgb ushr 8 and 255)/255f;tensor[2*w*h+i]=(rgb and 255)/255f}
            }finally{input.flush()}
            checkpoint()
            OnnxTensor.createTensor(OrtEnvironment.getEnvironment(),FloatBuffer.wrap(tensor),longArrayOf(1,3,h.toLong(),w.toLong())).use{t->
                s.run(mapOf(s.inputNames.single() to t)).use{result->
                    checkpoint()
                    val prediction=result.map{it.value}.filterIsInstance<OnnxTensor>().singleOrNull{it.info.shape.let{d->d.size==3&&d[0]==1L}}?:error("需要YOLO预测张量 [1,4+类别+掩膜系数,候选数]")
                    val dims=prediction.info.shape;require(prediction.info.type==OnnxJavaType.FLOAT&&prediction.info.numElements<=12_000_000)
                    val transposed=dims[1]>512&&dims[2]<=512;val channels=(if(transposed)dims[2]else dims[1]).toInt();val anchors=(if(transposed)dims[1]else dims[2]).toInt()
                    require(channels in (4+model.classes.size)..512&&anchors in 1..100000)
                    val data=prediction.floatBuffer;fun value(c:Int,a:Int)=data.get(if(transposed)a*channels+c else c*anchors+a)
                    val candidates=mutableListOf<AutoMaskRegion>()
                    for(a in 0 until anchors){if(a%256==0)checkpoint()
                        for((category,name)in model.classes.withIndex())if(name in allowed){
                            val score=value(4+category,a);if(!score.isFinite()||score<options.confidence)continue
                            require(score<=1f){"模型输出不是0至1的类别置信度"}
                            val cx=value(0,a);val cy=value(1,a);val bw=value(2,a);val bh=value(3,a)
                            if(!listOf(cx,cy,bw,bh).all{it.isFinite()}||bw<=0||bh<=0)continue
                            val x1=((cx-bw/2-left)/ratio).coerceIn(0.0,image.width.toDouble());val x2=((cx+bw/2-left)/ratio).coerceIn(0.0,image.width.toDouble())
                            val y1=((cy-bh/2-top)/ratio).coerceIn(0.0,image.height.toDouble());val y2=((cy+bh/2-top)/ratio).coerceIn(0.0,image.height.toDouble())
                            if(x2>x1&&y2>y1)candidates+=AutoMaskRegion(category=name,confidence=score,x=x1,y=y1,width=x2-x1,height=y2-y1)
                            require(candidates.size<=10000){"候选区域过多，请提高置信度"}
                        }
                    }
                    val kept=mutableListOf<AutoMaskRegion>()
                    for(candidate in candidates.sortedByDescending{it.confidence}){
                        if(kept.none{it.category==candidate.category&&overlap(it,candidate)>options.iou})kept+=candidate
                        require(kept.size<=100){"检测超过100个区域，请缩小范围或提高置信度"}
                    };return kept.map{region->
                        val x=(region.x-options.expansion).coerceAtLeast(0.0);val y=(region.y-options.expansion).coerceAtLeast(0.0)
                        region.copy(x=x,y=y,width=min(image.width.toDouble(),region.x+region.width+options.expansion)-x,height=min(image.height.toDouble(),region.y+region.height+options.expansion)-y)
                    }
                }
            }
        }
    }
    private fun overlap(a:AutoMaskRegion,b:AutoMaskRegion):Double {val area=max(0.0,min(a.x+a.width,b.x+b.width)-max(a.x,b.x))*max(0.0,min(a.y+a.height,b.y+b.height)-max(a.y,b.y));return area/(a.width*a.height+b.width*b.height-area).coerceAtLeast(.0001)}
    fun mask(region:AutoMaskRegion,style:TypesetMask)=style.copy(id=newId(),shape="rect",x=region.x,y=region.y,width=region.width,height=region.height,points=emptyList(),visible=true,locked=false)
}
