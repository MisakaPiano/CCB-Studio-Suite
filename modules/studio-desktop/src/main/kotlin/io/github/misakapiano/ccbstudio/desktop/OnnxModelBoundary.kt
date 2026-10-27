package io.github.misakapiano.ccbstudio.desktop

/** Inspect protobuf references before native parsing. This adapter accepts a single
 * self-contained inference graph, never external tensor paths or function libraries. */
internal object OnnxModelBoundary {
    private enum class Kind { MODEL,GRAPH,NODE,ATTRIBUTE,TENSOR,SPARSE }
    fun validate(bytes:ByteArray){
        require(bytes.size in 1..128*1024*1024);var fields=0
        fun parse(start:Int,end:Int,kind:Kind,depth:Int){
            require(depth<=32);var at=start
            fun variable():Long{var value=0L;for(n in 0..9){require(at<end);val b=bytes[at++].toInt()and 255;if(n==9)require(b<=1);value=value or ((b and 127).toLong()shl(n*7));if(b<128)return value};error("Invalid ONNX integer")}
            while(at<end){require(++fields<=1_000_000);val tag=variable();val field=(tag ushr 3).toInt();val wire=(tag and 7).toInt();require(field>0)
                var number=0L;var body=at;var limit=at
                when(wire){0->number=variable();1->{require(end-at>=8);at+=8};5->{require(end-at>=4);at+=4};2->{val size=variable();require(size>=0&&size<=end-at);body=at;limit=at+size.toInt();at=limit};else->error("Unsupported ONNX wire type")}
                if(kind==Kind.MODEL&&field in setOf(20,25))error("只支持独立推理模型，不加载训练图或自定义函数库")
                if(kind==Kind.TENSOR&&(field==13||field==14&&number!=0L))error("模型引用外部权重文件；仅接受权重内嵌的ONNX模型")
                val next=when(kind){
                    Kind.MODEL->if(field==7)Kind.GRAPH else null
                    Kind.GRAPH->when(field){1->Kind.NODE;5->Kind.TENSOR;15->Kind.SPARSE;else->null}
                    Kind.NODE->if(field==5)Kind.ATTRIBUTE else null
                    Kind.ATTRIBUTE->when(field){5,10->Kind.TENSOR;6,11->Kind.GRAPH;22,23->Kind.SPARSE;else->null}
                    Kind.SPARSE->if(field==1||field==2)Kind.TENSOR else null
                    else->null
                }
                if(next!=null){require(wire==2);parse(body,limit,next,depth+1)}
            };require(at==end)
        }
        parse(0,bytes.size,Kind.MODEL,0)
    }
}
