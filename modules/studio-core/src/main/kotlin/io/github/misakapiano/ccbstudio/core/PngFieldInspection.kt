package io.github.misakapiano.ccbstudio.core

import com.example.chatbar.domain.image.StealthAlphaMetadata
import kotlinx.serialization.json.*
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.util.zip.InflaterInputStream

/** Read-only projection. It never chooses parameter precedence or generates a request. */
object PngFieldInspection {
    private const val LIMIT=8*1024*1024
    private class Limit:RuntimeException()
    fun inspect(bytes:ByteArray):RecipeInspection {
        if(bytes.size<8 || !bytes.copyOfRange(0,8).contentEquals(byteArrayOf(-119,80,78,71,13,10,26,10)))return RecipeInspection("UNSUPPORTED_FORMAT",emptyMap(),null)
        return try {
            val source=TypesetPngMetadata.textChunks(bytes);var remaining=LIMIT
            fun text(data:ByteArray,compressed:Boolean,utf8:Boolean):String {
                val decoded=if(compressed)InflaterInputStream(ByteArrayInputStream(data)).use{input->input.readNBytes(remaining+1)}else data
                if(decoded.size>remaining)throw Limit();remaining-=decoded.size
                return if(utf8)decoded.decodeToString(throwOnInvalidSequence=true)else decoded.toString(Charsets.ISO_8859_1)
            }
            fun projection(value:String):JsonObject=buildJsonObject {
                put("text",value)
                if(value.trimStart().startsWith("{") || value.trimStart().startsWith("[")){
                    val inspection=RecipeComparison.inspect(value);put("jsonStatus",inspection.status)
                    if(inspection.status in setOf("READABLE","UNSUPPORTED_VERSION"))put("parsedJson",Json.parseToJsonElement(value))
                }
            }
            val chunks=source.map{chunk->
                val type=chunk.copyOfRange(4,8).toString(Charsets.US_ASCII);val size=ByteBuffer.wrap(chunk,0,4).int
                val data=chunk.copyOfRange(8,8+size);var pos=0
                fun part():ByteArray {val start=pos;while(pos<data.size && data[pos]!=0.toByte())pos++;require(pos<data.size);return data.copyOfRange(start,pos).also{pos++}}
                var keyword=""
                try {
                    keyword=part().toString(Charsets.ISO_8859_1);require(keyword.length in 1..79)
                    var compressed=false;var language="";var translated=""
                    when(type){
                        "zTXt"->{require(pos<data.size);if(data[pos++].toInt()!=0)return@map buildJsonObject{put("type",type);put("keyword",keyword);put("status","UNSUPPORTED_COMPRESSION")};compressed=true}
                        "iTXt"->{require(pos+2<=data.size);val flag=data[pos++].toInt();val method=data[pos++].toInt()
                            if(flag !in 0..1 || method!=0)return@map buildJsonObject{put("type",type);put("keyword",keyword);put("status","UNSUPPORTED_COMPRESSION")}
                            compressed=flag==1;language=part().toString(Charsets.US_ASCII);translated=part().decodeToString(throwOnInvalidSequence=true)}
                    }
                    val value=text(data.copyOfRange(pos,data.size),compressed,type=="iTXt")
                    JsonObject(buildJsonObject{put("type",type);put("keyword",keyword);put("language",language);put("translatedKeyword",translated);put("status","READABLE")}+projection(value))
                }catch(e:Limit){throw e}catch(e:Exception){buildJsonObject{put("type",type);put("keyword",keyword);put("status","CORRUPT");put("rawSha256",sha256(chunk))}}
            }
            val alpha=try{val image=EditorRenderer.decode(bytes)
                try{val value=StealthAlphaMetadata.decode(image.width,image.height){x,y->image.getRGB(x,y) ushr 24}
                    value?.let{if(it.toByteArray().size>remaining)throw Limit();JsonObject(projection(it)+("status" to JsonPrimitive("READABLE")))}
                }finally{image.flush()}
            }catch(e:Limit){throw e}catch(e:Exception){buildJsonObject{put("status","READ_ERROR")}}
            if(chunks.isEmpty() && alpha==null)return RecipeInspection("MISSING",emptyMap(),null)
            val raw=buildJsonObject{put("textChunks",JsonArray(chunks));alpha?.let{put("alpha",it)}}.toString()
            val read=RecipeComparison.inspect(raw)
            if(read.status=="READABLE" && (chunks.any{it["status"]?.jsonPrimitive?.content!="READABLE" || it["jsonStatus"]?.jsonPrimitive?.content=="CORRUPT"} || alpha?.get("status")?.jsonPrimitive?.content=="READ_ERROR"))read.copy(status="PARTIAL")else read
        }catch(e:Limit){RecipeInspection("LIMIT_EXCEEDED",emptyMap(),null)}catch(e:Exception){RecipeInspection("CORRUPT",emptyMap(),null)}
    }
}
