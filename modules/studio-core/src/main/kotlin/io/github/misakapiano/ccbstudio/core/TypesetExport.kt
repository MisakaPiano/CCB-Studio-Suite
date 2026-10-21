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
@Serializable data class TypesetExportReceipt(val version:Int=1,val projectId:String,val revision:Long,val mode:String,val variant:String,val format:String,
    val pageIds:List<String>,val files:List<TypesetExportFile>)
data class TypesetExportRequest(val pageIds:List<String>,val variant:String="all",val format:String="png",val zip:Boolean=false,val pdf:Boolean=false)

/** A fresh output directory is published only after every file and completion receipt is durable.
 * Failed staging directories remain identifiable and never replace a user's previous export. */
object TypesetExport {
    fun create(workspace:StudioWorkspace,project:TypesetProject,document:TypesetDocument,parent:Path,request:TypesetExportRequest,
        hook:(String)->Unit={}):Path=workspace.operation {
        document.validate();require(Files.isDirectory(parent,LinkOption.NOFOLLOW_LINKS)){"Select an existing destination directory"}
        require(request.variant in setOf("all","text","mask","original") && request.format in setOf("png","jpeg"))
        require(request.pageIds.isNotEmpty() && request.pageIds.distinct().size==request.pageIds.size)
        val selected=request.pageIds.toSet();val pages=document.pages.filter{it.id in selected};require(pages.size==selected.size)
        require(document.mode=="storybook" || !request.pdf){"PDF export is for book pages"}
        require(request.variant!="original" || (!request.pdf && document.mode!="storybook")){"Original files are separate from book rendering"}
        if(request.variant!="original")TypesetFonts.verifyRenderable(workspace,document.copy(pages=pages),request.variant)
        val stem="Studio-export-${newId()}";val staging=parent.resolve(".$stem.incomplete");val target=parent.resolve(stem)
        Files.createDirectory(staging)
        val outputs=mutableListOf<Path>()
        try {
            val pdf=if(request.pdf)RasterBookPdf(staging.resolve("book.pdf"),pages.size)else null
            try {
                pages.forEach{page->
                    hook("BEFORE_PAGE")
                    val original=workspace.bytes(page.assetId);val index=document.pages.indexOfFirst{it.id==page.id}
                    val number=TypesetQueue.number(document,index)
                    val extension=if(request.variant=="original")when{original.size>=8 && original[0]==0x89.toByte() && original[1]==0x50.toByte()->"png";original.size>=2 && original[0]==0xff.toByte() && original[1]==0xd8.toByte()->"jpg";else->"bin"}else if(request.format=="jpeg")"jpg" else "png"
                    val file=staging.resolve("$number.$extension")
                    if(request.variant=="original")Files.write(file,original,StandardOpenOption.CREATE_NEW)
                    else {
                        val image=if(document.mode=="storybook")TypesetRenderer.story(original,page,document.startNumber+index) else TypesetRenderer.render(original,page,request.variant)
                        try {writeImage(file,image,request.format);pdf?.page(image)}finally{image.flush()}
                    }
                    force(file);outputs.add(file);hook("PAGE_WRITTEN")
                }
                pdf?.finish()
            }finally{pdf?.close()}
            if(request.pdf){force(staging.resolve("book.pdf"));outputs.add(staging.resolve("book.pdf"))}
            if(request.zip){val zip=staging.resolve("pages.zip");ZipOutputStream(Files.newOutputStream(zip,StandardOpenOption.CREATE_NEW)).use{out->
                outputs.filter{it.fileName.toString()!="book.pdf"}.forEach{file->out.putNextEntry(ZipEntry(file.fileName.toString()));Files.newInputStream(file).use{it.copyTo(out)};out.closeEntry()}}
                force(zip);outputs.add(zip)}
            val receipt=TypesetExportReceipt(projectId=project.id,revision=project.revision,mode=document.mode,variant=request.variant,format=request.format,pageIds=pages.map{it.id},files=outputs.map(::identity))
            val marker=staging.resolve("COMPLETE.json");Files.writeString(marker,documentJson.encodeToString(receipt),StandardOpenOption.CREATE_NEW);force(marker)
            hook("BEFORE_PUBLISH")
            Files.move(staging,target,StandardCopyOption.ATOMIC_MOVE)
            target
        } catch(e:Exception) {
            // The incomplete directory (even if it contains a receipt) is not a successful delivery.
            throw DataProblem("EXPORT_INCOMPLETE","Export did not complete; retained at $staging",e)
        }
    }
    fun verify(directory:Path):TypesetExportReceipt {
        require(!directory.fileName.toString().endsWith(".incomplete")){"Unpublished export"}
        val receipt=documentJson.decodeFromString<TypesetExportReceipt>(readBounded(directory.resolve("COMPLETE.json"),1024*1024).decodeToString())
        require(receipt.version==1 && receipt.files.isNotEmpty() && receipt.files.size<=1002 && receipt.files.map{it.name}.distinct().size==receipt.files.size)
        receipt.files.forEach{expected->require(Regex("[A-Za-z0-9._-]+").matches(expected.name) && expected.name!="." && expected.name!="..");val path=directory.resolve(expected.name);require(Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS));require(identity(path)==expected){"Export checksum mismatch"}}
        return receipt
    }
    private fun identity(path:Path):TypesetExportFile {
        val hash=MessageDigest.getInstance("SHA-256");Files.newInputStream(path).use{input->val buffer=ByteArray(65536);while(true){val n=input.read(buffer);if(n<0)break;hash.update(buffer,0,n)}}
        return TypesetExportFile(path.fileName.toString(),Files.size(path),hash.digest().joinToString(""){"%02x".format(it)})
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
