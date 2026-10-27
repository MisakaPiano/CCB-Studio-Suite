package io.github.misakapiano.ccbstudio.core

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.nio.file.*
import java.util.zip.ZipFile
import javax.imageio.ImageIO
import kotlin.test.*

class TypesetExportCategoriesTest {
    private fun root()=Path.of(System.getProperty("preview.test.root")).resolve(newId())
    @Test fun completeCategoriesKeepQueueNumbersAndSeparateEncodedPlainFromOriginalBytes(){
        StudioWorkspace.openStudio(root()).use{w->
            val bytes=EditorRenderer.synthetic(100,80);val asset=w.importImage("source.png",bytes);val repo=TypesetRepository(w)
            var p=repo.create("categories",listOf(asset,asset,asset))
            p=repo.update(p.id,p.revision){d->d.copy(startNumber=7,pages=d.pages.mapIndexed{i,page->if(i==0)page.copy(masks=listOf(TypesetMask(effect="black",x=0.0,y=0.0,width=30.0,height=30.0)))else page})}
            val d=repo.load(p);val out=TypesetExport.create(w,p,d,Files.createDirectories(root()),TypesetExportRequest(
                listOf(d.pages[2].id,d.pages[0].id),format="jpeg",zip=true,variants=listOf("plain","mask","all","text","original"),includeRecipe=true,categoryNames=true,filePrefix="第1章"))
            val receipt=TypesetExport.verify(out);assertEquals(4,receipt.version);assertEquals(listOf(d.pages[0].id,d.pages[2].id),receipt.pageIds)
            val folders=listOf("01_原始排序","02_仅打码","03_打码并嵌字","04_仅嵌字","原图副本")
            val expected=folders.flatMap{folder->listOf("007","009").flatMap{n->listOf("$folder/第1章_$n.${if(folder=="原图副本")"png"else"jpg"}","$folder/第1章_${n}_recipe.json")}}.toSet()
            assertEquals(expected+"pages.zip",receipt.files.map{it.name}.toSet())
            ZipFile(out.resolve("pages.zip").toFile()).use{z->assertEquals(expected,z.entries().asSequence().map{it.name}.toSet())}
            assertContentEquals(bytes,Files.readAllBytes(out.resolve("原图副本/第1章_007.png")))
            val plain=ImageIO.read(out.resolve("01_原始排序/第1章_007.jpg").toFile());val masked=ImageIO.read(out.resolve("02_仅打码/第1章_007.jpg").toFile())
            assertEquals(100,plain.width);assertNotEquals(plain.getRGB(5,5),masked.getRGB(5,5));plain.flush();masked.flush()
            val recipe=TypesetEditRecipes.inspect(Files.readAllBytes(out.resolve("01_原始排序/第1章_007_recipe.json")))
            assertEquals("plain",recipe.variant)
            assertContentEquals(bytes,w.bytes(asset));assertEquals(d,repo.load(p))
        }
    }
    @Test fun oldRawDeliveryStaysCompatibleAndUnsafeNamesNeverPublish(){
        StudioWorkspace.openStudio(root()).use{w->
            val bytes=EditorRenderer.synthetic(40,40);val asset=w.importImage("raw.png",bytes);val repo=TypesetRepository(w);val p=repo.create("old",listOf(asset));val d=repo.load(p)
            val parent=Files.createDirectories(root());val out=TypesetExport.create(w,p,d,parent,TypesetExportRequest(d.pages.map{it.id},variant="original",format="webp"))
            val receipt=TypesetExport.verify(out);assertEquals(3,receipt.version);assertEquals("001.png",receipt.files.single().name);assertContentEquals(bytes,Files.readAllBytes(out.resolve("001.png")))
            for(prefix in listOf("../escape","x\\bad","trailing.","x:y",".")){
                assertFailsWith<IllegalArgumentException>{TypesetExport.create(w,p,d,parent,TypesetExportRequest(d.pages.map{it.id},filePrefix=prefix))}
            }
            assertEquals(1,Files.list(parent).use{it.count()}.toInt())
            val marker=out.resolve("COMPLETE.json");Files.writeString(marker,documentJson.encodeToString(receipt.copy(version=4,files=listOf(receipt.files.single().copy(name="../001.png")))))
            assertFailsWith<IllegalArgumentException>{TypesetExport.verify(out)}
            assertContentEquals(bytes,w.bytes(asset))
        }
    }
    @Test fun sortedAndRawCopyPreferencesRoundTripIndependentlyWithoutNewJournalKeys(){
        StudioWorkspace.openStudio(root()).use{w->
            val oldKeys=documentJson.parseToJsonElement(documentJson.encodeToString(w.state.typesetPreferences)).jsonObject.keys
            for(sorted in listOf(false,true))for(copies in listOf(false,true)){
                w.save(w.state.copy(typesetPreferences=w.state.typesetPreferences.copy(exportVariants=if(copies)listOf("original","all")else listOf("all"))))
                TypesetEditorExtrasStore.save(w,TypesetEditorExtras(originalSorted=sorted).saveMask("alias",MaskToolSettings(strength=27)))
                val bytes=TypesetSourceExchange.exportPresets(w);val raw=documentJson.parseToJsonElement(bytes.decodeToString()).jsonObject
                assertEquals(sorted,raw.getValue("prefs").jsonObject.getValue("exportVariantSelection").jsonObject.getValue("original").jsonPrimitive.boolean)
                assertEquals(copies,"original" in TypesetSourceExchange.inspectPresets(bytes).preferences.exportVariants)
                TypesetSourceExchange.importPresets(w,TypesetSourceExchange.inspectPresets(bytes),false)
                val extras=TypesetEditorExtrasStore.load(w);assertEquals(sorted,extras.originalSorted);assertEquals(copies,extras.originalCopies);assertEquals(27,extras.namedMasks.single().settings.strength)
                assertEquals(oldKeys,documentJson.parseToJsonElement(documentJson.encodeToString(w.state.typesetPreferences)).jsonObject.keys)
            }
        }
    }
}
