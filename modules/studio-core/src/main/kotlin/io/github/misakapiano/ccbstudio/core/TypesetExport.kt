package io.github.misakapiano.ccbstudio.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.*
import java.nio.channels.FileChannel
import java.nio.file.*
import java.security.MessageDigest
import java.util.zip.*
import javax.imageio.ImageIO

@Serializable data class TypesetExportFile(val name:String,val size:Long,val sha256:String)
@Serializable data class TypesetExportReceipt(val version:Int=2,val projectId:String,val revision:Long,val mode:String,val variant:String,val format:String,
    val pageIds:List<String>,val files:List<TypesetExportFile>,val variants:List<String> = listOf(variant),val metadataStatuses:Map<String,String> = emptyMap())
data class TypesetExportRequest(val pageIds:List<String>,val variant:String="all",val format:String="png",val zip:Boolean=false,val pdf:Boolean=false, val variants:List<String> = listOf(variant),val metadata:String="preserve",val includeRecipe:Boolean=false)

/** A fresh output directory is published only after every file and completion receipt is durable.
 * Failed staging directories remain identifiable and never replace a user's previous export. */
object TypesetExport {
    fun create(workspace:StudioWorkspace,project:TypesetProject,document:TypesetDocument,parent:Path,request:TypesetExportRequest,
        hook:(String)->Unit={}):Path=workspace.operation {
        document.validate();require(Files.isDirectory(parent,LinkOption.NOFOLLOW_LINKS)){"Select an existing destination directory"}
        require(request.variants.isNotEmpty() && request.variants.distinct().size==request.variants.size && request.variants.all{it in setOf("all","text","mask","original")} && request.format in setOf("png","jpeg"))
        require(request.metadata in setOf("preserve","strip"))
        require(request.pageIds.isNotEmpty() && request.pageIds.distinct().size==request.pageIds.size)
        val selected=request.pageIds.toSet();val pages=document.pages.filter{it.id in selected};require(pages.size==selected.size)
        require(document.mode=="storybook" || !request.pdf){"PDF export is for book pages"}
        require("original" !in request.variants || (!request.pdf && document.mode!="storybook")){"Original files are separate from book rendering"}
        require(document.mode!="storybook" || request.variants==listOf("all"))
        request.variants.filter{it!="original"}.forEach{TypesetFonts.verifyRenderable(workspace,document.copy(pages=pages),it)}
        val stem="Studio-export-${newId()}";val staging=parent.resolve(".$stem.incomplete");val target=parent.resolve(stem)
        Files.createDirectory(staging)
        val outputs=mutableListOf<Path>();val statuses=linkedMapOf<String,String>()
        try {
            val pdf=if(request.pdf)RasterBookPdf(staging.resolve("book.pdf"),pages.size)else null
            try {
                for(variant in request.variants) {
                    val directory=if(request.variants.size==1)staging else Files.createDirectory(staging.resolve(variant))
                    pages.forEach{page->
                        hook("BEFORE_PAGE")
                        val original=workspace.bytes(page.assetId);val index=document.pages.indexOfFirst{it.id==page.id}
                        val number=TypesetQueue.number(document,index)
                        val extension=if(variant=="original")when{original.size>=8 && original[0]==0x89.toByte() && original[1]==0x50.toByte()->"png";original.size>=2 && original[0]==0xff.toByte() && original[1]==0xd8.toByte()->"jpg";else->"bin"}else if(request.format=="jpeg")"jpg" else "png"
                        val file=directory.resolve("$number.$extension")
                        val status=if(variant=="original"){Files.write(file,original,StandardOpenOption.CREATE_NEW);"original-byte-identical"}
                        else {
                            val source=if(document.mode!="storybook" && request.metadata=="preserve")TypesetPngMetadata.extract(original)else null
                            val image=if(document.mode=="storybook")TypesetRenderer.story(original,page,document.startNumber+index) else TypesetRenderer.render(original,page,variant)
                            try {
                                if(document.mode=="storybook"){writeImage(file,image,request.format);pdf?.page(image);"composite"}
                                else if(request.metadata=="strip"){
                                    if(request.format=="png")Files.newOutputStream(file,StandardOpenOption.CREATE_NEW).use{out->com.example.chatbar.domain.image.PrivacyPngEncoder.write(out,image.width,image.height){y,row->image.getRGB(0,y,image.width,1,row,0,image.width)}}
                                    else {val sanitized=EditorRenderer.decode(EditorRenderer.encode(EditorRenderer.Result(image,emptyList()),true));try{writeImage(file,sanitized,request.format)}finally{sanitized.flush()}}
                                    "removed-text-and-pixel-lsb"
                                }else{
                                    if(request.format=="png")TypesetPngMetadata.injectAlpha(image,source!!)
                                    writeImage(file,image,request.format)
                                    if(request.format=="png" && source!!.chunks.isNotEmpty())Files.write(file,TypesetPngMetadata.injectChunks(readBounded(file,64*1024*1024),source),StandardOpenOption.TRUNCATE_EXISTING)
                                    if(source!!.chunks.isEmpty() && source.alpha==null)"none"else if(request.format=="jpeg")"dropped-jpeg"else"preserved-source-origin-not-edited-signature"
                                }
                            }finally{image.flush()}
                        }
                        force(file);outputs.add(file);statuses[staging.relativize(file).toString().replace('\\','/')]=status
                        if(request.includeRecipe){val recipe=directory.resolve("${number}_recipe.json");val asset=workspace.assets.read(java.util.UUID.fromString(page.assetId)).require()
                            val json=kotlinx.serialization.json.buildJsonObject{put("version",kotlinx.serialization.json.JsonPrimitive(1));put("variant",kotlinx.serialization.json.JsonPrimitive(variant));put("originalSha256",kotlinx.serialization.json.JsonPrimitive(asset.sha256()));put("page",documentJson.encodeToJsonElement(TypesetPage.serializer(),page));put("sourceRecipeBase64",kotlinx.serialization.json.JsonPrimitive(java.util.Base64.getEncoder().encodeToString(asset.request().recipe())))}
                            Files.writeString(recipe,json.toString(),StandardOpenOption.CREATE_NEW);force(recipe);outputs.add(recipe)}
                        hook("PAGE_WRITTEN")
                    }
                }
                pdf?.finish()
            }finally{pdf?.close()}
            if(request.pdf){force(staging.resolve("book.pdf"));outputs.add(staging.resolve("book.pdf"))}
            if(request.zip){val zip=staging.resolve("pages.zip");ZipOutputStream(Files.newOutputStream(zip,StandardOpenOption.CREATE_NEW)).use{out->
                outputs.filter{it.fileName.toString()!="book.pdf"}.forEach{file->out.putNextEntry(ZipEntry(staging.relativize(file).toString().replace('\\','/')));Files.newInputStream(file).use{it.copyTo(out)};out.closeEntry()}}
                force(zip);outputs.add(zip)}
            val receipt=TypesetExportReceipt(projectId=project.id,revision=project.revision,mode=document.mode,variant=if(request.variants.size==1)request.variants.single()else"multiple",format=request.format,pageIds=pages.map{it.id},files=outputs.map{identity(it,staging)},variants=request.variants,metadataStatuses=statuses)
            val marker=staging.resolve("COMPLETE.json");Files.writeString(marker,documentJson.encodeToString(receipt),StandardOpenOption.CREATE_NEW);force(marker)
            hook("BEFORE_PUBLISH")
            Files.move(staging,target,StandardCopyOption.ATOMIC_MOVE)
            target
        } catch(e:Exception) {
            // The incomplete directory (even if it contains a receipt) is not a successful delivery.
            throw DataProblem("EXPORT_INCOMPLETE","导出未完成：${e.message?.take(300)}。未完成目录保留：$staging",e)
        }
    }
    fun verify(directory:Path):TypesetExportReceipt {
        require(!directory.fileName.toString().endsWith(".incomplete")){"Unpublished export"}
        val receipt=documentJson.decodeFromString<TypesetExportReceipt>(readBounded(directory.resolve("COMPLETE.json"),4*1024*1024).decodeToString())
        require(receipt.version in 1..2 && receipt.files.isNotEmpty() && receipt.files.size<=8002 && receipt.files.map{it.name}.distinct().size==receipt.files.size)
        receipt.files.forEach{expected->require(Regex("(?:(?:all|mask|text|original)/)?[A-Za-z0-9._-]+").matches(expected.name) && expected.name!="." && expected.name!="..");val path=directory.resolve(expected.name);safePath(path);require(Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS));require(identity(path,directory)==expected){"Export checksum mismatch"}}
        return receipt
    }
    private fun identity(path:Path,base:Path):TypesetExportFile {
        val hash=MessageDigest.getInstance("SHA-256");Files.newInputStream(path).use{input->val buffer=ByteArray(65536);while(true){val n=input.read(buffer);if(n<0)break;hash.update(buffer,0,n)}}
        return TypesetExportFile(base.relativize(path).toString().replace('\\','/'),Files.size(path),hash.digest().joinToString(""){"%02x".format(it)})
    }
    private fun force(path:Path){FileChannel.open(path,StandardOpenOption.WRITE).use{it.force(true)}}
    private fun writeImage(path:Path,image:BufferedImage,format:String) {
        val rgb=if(format=="jpeg")BufferedImage(image.width,image.height,BufferedImage.TYPE_INT_RGB).also{out->out.createGraphics().let{g->try{g.color=Color.WHITE;g.fillRect(0,0,out.width,out.height);g.drawImage(image,0,0,null)}finally{g.dispose()}}}else image
        try {Files.newOutputStream(path,StandardOpenOption.CREATE_NEW).use{output->
            if(format=="jpeg"){
                val writer=ImageIO.getImageWritersByFormatName("jpeg").asSequence().firstOrNull()?:error("JPEG encoder unavailable")
                try{ImageIO.createImageOutputStream(output).use{stream->writer.output=stream;val options=writer.defaultWriteParam;options.compressionMode=javax.imageio.ImageWriteParam.MODE_EXPLICIT;options.compressionQuality=.95f;writer.write(null,javax.imageio.IIOImage(rgb,null,null),options)}}finally{writer.dispose()}
            }else require(ImageIO.write(rgb,format,output)){"Image encoder unavailable"}
        }}finally{if(rgb!==image)rgb.flush()}
    }
}

/** Bounded, sequential PDF 1.4 raster pages; 96 px/in -> 72 pt/in matches source jsPDF. */
private class RasterBookPdf(path:Path,private val count:Int):Closeable {
    private val file=RandomAccessFile(path.toFile(),"rw")
    private val offsets=LongArray(3+count*3);private var current=0
    init {require(count in 1..1000);file.write("%PDF-1.4\n%\u00e2\u00e3\u00cf\u00d3\n".toByteArray(Charsets.ISO_8859_1));obj(1,"<< /Type /Catalog /Pages 2 0 R >>");obj(2,"<< /Type /Pages /Count $count /Kids [${(0 until count).joinToString(" "){"${3+it*3} 0 R"}}] >>")}
    private fun ascii(s:String)=file.write(s.toByteArray(Charsets.US_ASCII))
    private fun obj(id:Int,s:String){offsets[id]=file.filePointer;ascii("$id 0 obj\n$s\nendobj\n")}
    private fun stream(id:Int,dict:String,data:ByteArray){offsets[id]=file.filePointer;ascii("$id 0 obj\n<< $dict /Length ${data.size} >>\nstream\n");file.write(data);ascii("\nendstream\nendobj\n")}
    fun page(image:BufferedImage) {
        require(current<count);val id=3+current*3;val w=image.width*.75;val h=image.height*.75
        obj(id,"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 $w $h] /Resources << /XObject << /Im0 ${id+2} 0 R >> >> /Contents ${id+1} 0 R >>")
        stream(id+1,"","q $w 0 0 $h 0 0 cm /Im0 Do Q".toByteArray(Charsets.US_ASCII))
        offsets[id+2]=file.filePointer;ascii("${id+2} 0 obj\n<< /Type /XObject /Subtype /Image /Width ${image.width} /Height ${image.height} /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /FlateDecode /Length ")
        val lengthOffset=file.filePointer;ascii("00000000000000000000 >>\nstream\n");val start=file.filePointer
        DeflaterOutputStream(object:OutputStream(){override fun write(b:Int)=file.write(b);override fun write(b:ByteArray,off:Int,len:Int)=file.write(b,off,len)}).use{out->
            val row=ByteArray(image.width*3);for(y in 0 until image.height){for(x in 0 until image.width){val c=image.getRGB(x,y);row[x*3]=(c shr 16).toByte();row[x*3+1]=(c shr 8).toByte();row[x*3+2]=c.toByte()};out.write(row)}}
        val end=file.filePointer;file.seek(lengthOffset);ascii("%020d".format(java.util.Locale.ROOT,end-start));file.seek(end);ascii("\nendstream\nendobj\n");current++
    }
    fun finish(){require(current==count);val start=file.filePointer;ascii("xref\n0 ${offsets.size}\n0000000000 65535 f \n");for(i in 1 until offsets.size)ascii("%010d 00000 n \n".format(java.util.Locale.ROOT,offsets[i]));ascii("trailer\n<< /Size ${offsets.size} /Root 1 0 R >>\nstartxref\n$start\n%%EOF\n");file.fd.sync()}
    override fun close()=file.close()
}
