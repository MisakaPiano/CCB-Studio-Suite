package io.github.misakapiano.ccbstudio.core

import java.nio.file.*
import java.util.zip.ZipFile
import javax.imageio.ImageIO
import kotlin.test.*

class TypesetExportTest {
    private fun root()=Path.of(System.getProperty("preview.test.root")).resolve(newId())
    @Test fun longBookPagesKeepPhysicalSizeWithoutLegacyPdfClipping() {
        StudioWorkspace.openStudio(root()).use{w->
            val bytes=EditorRenderer.synthetic(200,80);val id=w.importImage("source.png",bytes);val repo=TypesetRepository(w)
            var p=repo.create("长页PDF",listOf(id,id));val style=StoryStyle(pageWidth=200,pagePadding=10,gap=10,textPadding=10,fontSize=12.0,lineHeight=1.0,fontFamily="Dialog",firstIndent=false)
            p=repo.update(p.id,p.revision){d->d.copy(mode="storybook",pages=d.pages.mapIndexed{i,page->page.copy(story=style,caption=if(i==0)"Short page" else (1..1700).joinToString("\n"){"Row ${it.toString().padStart(4,'0')}"})})}
            val d=repo.load(p);val parent=Files.createDirectories(Path.of("build/typeset-long-pdf"))
            val out=TypesetExport.create(w,p,d,parent,TypesetExportRequest(d.pages.map{it.id},pdf=true,includeRecipe=false))
            TypesetExport.verify(out)
            val sizes=d.pages.map{page->TypesetLayout.story(200,80,page.caption,style).let{it.width to it.height}}
            assertTrue(sizes[1].second*.75>14400);assertTrue(sizes[0].second*.75<14400)
            val pdf=Files.readAllBytes(out.resolve("book.pdf")).toString(Charsets.ISO_8859_1)
            assertTrue(pdf.startsWith("%PDF-2.0"));assertFalse(pdf.contains("/UserUnit"))
            Files.writeString(parent.resolve("latest.json"),"{\"directory\":\"${out.toAbsolutePath().toString().replace("\\","\\\\")}\",\"pixels\":[${sizes.joinToString(","){"[${it.first},${it.second}]"}}]}\n")
            assertContentEquals(bytes,w.bytes(id));assertEquals(d,repo.load(p))
        }
    }
    @Test fun missingFontBlocksRenderingButOriginalDeliveryRemainsAvailable() {
        StudioWorkspace.openStudio(root()).use{w->val id=w.importImage("original.png",EditorRenderer.synthetic(100,80));val repository=TypesetRepository(w)
            var p=repository.create("字体门禁",listOf(id));p=repository.update(p.id,p.revision){d->d.copy(pages=d.pages.map{it.copy(texts=listOf(TypesetText(text="Hello",style=TypesetStyle(fontFamily="CCB-missing-${newId()}"))))})}
            val d=repository.load(p);val parent=Files.createDirectories(root())
            assertEquals("FONT_MISSING",assertFailsWith<DataProblem>{TypesetExport.create(w,p,d,parent,TypesetExportRequest(d.pages.map{it.id}))}.status)
            assertEquals(0,Files.list(parent).use{it.count()}.toInt())
            val original=TypesetExport.create(w,p,d,parent,TypesetExportRequest(d.pages.map{it.id},variant="original"));TypesetExport.verify(original)
        }
    }
    @Test fun fourVariantsOriginalBytesJpegAndFailurePublication() {
        StudioWorkspace.openStudio(root()).use{w->
            val bytes=EditorRenderer.synthetic(180,120);val id=w.importImage("original.png",bytes);val repository=TypesetRepository(w)
            var p=repository.create("导出",listOf(id));p=repository.update(p.id,p.revision){d->d.copy(pages=d.pages.map{it.copy(texts=listOf(TypesetText(text="A",x=70.0,y=20.0,width=100.0)),masks=listOf(TypesetMask(effect="black",x=0.0,y=0.0,width=20.0,height=20.0)))})}
            val d=repository.load(p);val parent=Files.createDirectories(root());val imageHashes=mutableSetOf<String>()
            for(v in listOf("all","text","mask","original")){
                val out=TypesetExport.create(w,p,d,parent,TypesetExportRequest(d.pages.map{it.id},variant=v,zip=true))
                val receipt=TypesetExport.verify(out);assertEquals(2,receipt.files.size);val file=out.resolve("001.png");imageHashes+=sha256(Files.readAllBytes(file))
                if(v=="original")assertContentEquals(bytes,Files.readAllBytes(file))
                ZipFile(out.resolve("pages.zip").toFile()).use{zip->assertContentEquals(Files.readAllBytes(file),zip.getInputStream(zip.getEntry("001.png")).readBytes())}
            }
            assertEquals(4,imageHashes.size)
            val jpeg=TypesetExport.create(w,p,d,parent,TypesetExportRequest(d.pages.map{it.id},format="jpeg"));assertEquals(180,ImageIO.read(jpeg.resolve("001.jpg").toFile()).width)
            Files.write(jpeg.resolve("001.jpg"),byteArrayOf(1));assertFails{TypesetExport.verify(jpeg)}
            for(point in listOf("BEFORE_PAGE","PAGE_WRITTEN","BEFORE_PUBLISH"))assertFailsWith<DataProblem>{TypesetExport.create(w,p,d,parent,TypesetExportRequest(d.pages.map{it.id})){if(it==point)error("injected")}}
            val incomplete=Files.list(parent).use{it.filter{p->p.fileName.toString().endsWith(".incomplete")}.toList()};assertEquals(3,incomplete.size);incomplete.forEach{assertFails{TypesetExport.verify(it)}}
            assertContentEquals(bytes,w.bytes(id));assertEquals(d,repository.load(p))
        }
    }
    @Test fun storyPresetsVariablePagesAndIndependentInspectionArtifact() {
        StudioWorkspace.openStudio(root()).use{w->
            val id=w.importImage("source.png",EditorRenderer.synthetic(180,120));val repository=TypesetRepository(w)
            var p=repository.create("整册",listOf(id,id,id));p=repository.update(p.id,p.revision){d->d.copy(mode="storybook",startNumber=8,pages=d.pages.mapIndexed{i,page->page.copy(caption=listOf("","短句",("长段落，用于换行和可变页面高度。".repeat(80)))[i],story=TypesetBook.presets.values.elementAt(i))})}
            val d=repository.load(p);val parent=Files.createDirectories(Path.of("build/typeset-book-export"))
            val out=TypesetExport.create(w,p,d,parent,TypesetExportRequest(d.pages.map{it.id},zip=true,pdf=true));val receipt=TypesetExport.verify(out)
            assertEquals(5,receipt.files.size);assertEquals(d.pages[1].id,TypesetBook.next(d,d.pages[0].id));assertNull(TypesetBook.next(d,d.pages.last().id))
            val sizes=d.pages.mapIndexed{i,page->val m=TypesetLayout.story(180,120,page.caption,page.story);val image=ImageIO.read(out.resolve("${(8+i).toString().padStart(3,'0')}.png").toFile());assertEquals(m.width,image.width);assertEquals(m.height,image.height);"[${m.width*.75},${m.height*.75}]"}
            Files.writeString(parent.resolve("latest.json"),"{\"directory\":\"${out.toAbsolutePath().toString().replace("\\","\\\\")}\",\"sizes\":[${sizes.joinToString(",")}]}\n")
            assertEquals(34.0,TypesetBook.presets.getValue("clean").fontSize);assertFalse(TypesetBook.presets.getValue("clean").pageBorder)
        }
    }
}
