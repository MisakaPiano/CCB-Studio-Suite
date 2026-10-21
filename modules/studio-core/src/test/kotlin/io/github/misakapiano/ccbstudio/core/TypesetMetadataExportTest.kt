package io.github.misakapiano.ccbstudio.core

import com.example.chatbar.domain.image.StealthAlphaMetadata
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.file.*
import java.util.zip.ZipFile
import javax.imageio.ImageIO
import kotlin.test.*

class TypesetMetadataExportTest {
    @Test fun largeValidPageRecipeExportsAndReimportsWithoutLosingSourceMetadata() {
        val original=EditorRenderer.synthetic(24,24);val sourceRecipe="s".repeat(1024*1024)
        StudioWorkspace.openStudio(root()).use{w->
            val id=w.importImage("large-recipe.png",original,sourceRecipe);val repo=TypesetRepository(w)
            val initial=repo.create("large valid page",listOf(id))
            val project=repo.update(initial.id,initial.revision){d->d.copy(pages=d.pages.map{p->p.copy(texts=List(158){TypesetText(text="x".repeat(100000),visible=false)})})}
            val document=repo.load(project)
            val out=TypesetExport.create(w,project,document,Files.createDirectories(root()),TypesetExportRequest(document.pages.map{it.id},variant="original",format="source",includeRecipe=true))
            TypesetExport.verify(out)
            val bytes=Files.readAllBytes(out.resolve("001_recipe.json"));assertTrue(bytes.size>16*1024*1024)
            val recipe=TypesetEditRecipes.inspect(bytes)
            assertEquals(document.pages.single(),recipe.page)
            assertContentEquals(sourceRecipe.toByteArray(),java.util.Base64.getDecoder().decode(recipe.sourceRecipeBase64))
            assertContentEquals(original,w.bytes(id))
        }
    }
    @Test fun recipeAppliesOnlyToMatchingOriginalAndIsUndoable() {
        StudioWorkspace.openStudio(root()).use{w->val id=w.importImage("source.png",fixture());val other=w.importImage("other.png",EditorRenderer.synthetic(90,70));val repo=TypesetRepository(w)
            var p=repo.create("recipe",listOf(id,other));p=repo.update(p.id,p.revision){d->d.copy(pages=d.pages.mapIndexed{i,page->if(i==0)page.copy(texts=listOf(TypesetText(text="恢复样式",vertical=true)))else page})}
            val d=repo.load(p);val out=TypesetExport.create(w,p,d,Files.createDirectories(root()),TypesetExportRequest(listOf(d.pages.first().id),includeRecipe=true))
            val recipe=TypesetEditRecipes.inspect(Files.readAllBytes(out.resolve("001_recipe.json")))
            p=repo.update(p.id,p.revision){doc->doc.copy(pages=doc.pages.mapIndexed{i,page->if(i==0)page.copy(texts=emptyList())else page})}
            val revision=p.revision;assertEquals("SOURCE_MISMATCH",assertFailsWith<DataProblem>{TypesetEditRecipes.apply(w,p,d.pages.last().id,recipe)}.status);assertEquals(revision,w.state.typesetProjects.single().revision)
            p=TypesetEditRecipes.apply(w,p,d.pages.first().id,recipe);assertEquals(d.pages.first().texts,repo.load(p).pages.first().texts)
            p=repo.history(p.id,p.revision,false);assertTrue(repo.load(p).pages.first().texts.isEmpty());assertContentEquals(fixture(),w.bytes(id))
        }
    }
    private fun root()=Path.of(System.getProperty("preview.test.root")).resolve(newId())
    private fun fixture():ByteArray {
        val image=EditorRenderer.decode(EditorRenderer.synthetic(100,80));val text="{\"description\":\"synthetic metadata\"}".toByteArray()
        val stream=ByteArrayOutputStream().also{out->DataOutputStream(out).apply{write("stealth_pnginfo".toByteArray());writeInt(text.size*8);write(text)}}.toByteArray()
        repeat(stream.size*8){bit->val x=bit/image.height;val y=bit%image.height;image.setRGB(x,y,(image.getRGB(x,y) and 0xfeffffff.toInt()) or (((stream[bit/8].toInt() ushr (7-bit%8)) and 1) shl 24))}
        return try{EditorRenderer.encode(EditorRenderer.Result(image,emptyList()),false,"source recipe fixture")}finally{image.flush()}
    }
    @Test fun preserveTextChunksAlphaAndPrivacyUseIndependentAuthorityDecoder() {
        val bytes=fixture();val source=TypesetPngMetadata.extract(bytes);assertNotNull(source.alpha);assertTrue(source.chunks.isNotEmpty())
        StudioWorkspace.openStudio(root()).use{w->val id=w.importImage("source.png",bytes);val repository=TypesetRepository(w);val p=repository.create("metadata",listOf(id));val d=repository.load(p);val parent=Files.createDirectories(root())
            val keep=TypesetExport.create(w,p,d,parent,TypesetExportRequest(d.pages.map{it.id}));val copied=TypesetPngMetadata.extract(Files.readAllBytes(keep.resolve("001.png")))
            assertContentEquals(source.alpha,copied.alpha);assertEquals(source.chunks.map(::sha256),copied.chunks.map(::sha256))
            val image=ImageIO.read(keep.resolve("001.png").toFile());assertEquals("{\"description\":\"synthetic metadata\"}",StealthAlphaMetadata.decode(image.width,image.height){x,y->image.getRGB(x,y) ushr 24});image.flush()
            val strip=TypesetExport.create(w,p,d,parent,TypesetExportRequest(d.pages.map{it.id},metadata="strip"));val removed=TypesetPngMetadata.extract(Files.readAllBytes(strip.resolve("001.png")));assertNull(removed.alpha);assertTrue(removed.chunks.isEmpty())
            val receipt=TypesetExport.verify(strip);assertEquals("removed-text-and-pixel-lsb",receipt.metadataStatuses.getValue("001.png"));assertContentEquals(bytes,w.bytes(id))
            val jpeg=TypesetExport.create(w,p,d,parent,TypesetExportRequest(d.pages.map{it.id},format="jpeg"));assertEquals("dropped-jpeg",TypesetExport.verify(jpeg).metadataStatuses.getValue("001.jpg"))
        }
    }
    @Test fun everySelectedCategoryIncludesUneditedPagesAndRecipeZipPathsVerify() {
        StudioWorkspace.openStudio(root()).use{w->val bytes=fixture();val id=w.importImage("source.png",bytes);val r=TypesetRepository(w);val p=r.create("batch",listOf(id,id));val d=r.load(p)
            val out=TypesetExport.create(w,p,d,Files.createDirectories(root()),TypesetExportRequest(d.pages.map{it.id},zip=true,variants=listOf("original","mask","all","text"),metadata="strip",includeRecipe=true))
            val receipt=TypesetExport.verify(out);assertEquals(17,receipt.files.size);assertEquals(8,receipt.metadataStatuses.size)
            for(category in listOf("original","mask","all","text"))for(n in listOf("001","002")){assertTrue(Files.exists(out.resolve("$category/$n.png")));assertTrue(Files.exists(out.resolve("$category/${n}_recipe.json")))}
            assertContentEquals(bytes,Files.readAllBytes(out.resolve("original/001.png")))
            ZipFile(out.resolve("pages.zip").toFile()).use{zip->assertEquals(16,zip.size());assertContentEquals(bytes,zip.getInputStream(zip.getEntry("original/002.png")).readBytes())}
        }
    }
    @Test fun damagedMetadataIsNotReportedPreserved() {
        val bytes=fixture();val text=TypesetPngMetadata.extract(bytes).chunks.single();val location=bytes.indices.first{i->i+text.size<=bytes.size && bytes.copyOfRange(i,i+text.size).contentEquals(text)}
        bytes[location+text.size-1]=(bytes[location+text.size-1].toInt() xor 1).toByte();assertFails{TypesetPngMetadata.extract(bytes)}
    }
}
