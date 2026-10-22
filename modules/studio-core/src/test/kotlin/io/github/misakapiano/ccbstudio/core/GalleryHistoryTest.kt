package io.github.misakapiano.ccbstudio.core

import com.example.chatbar.domain.image.*
import com.example.chatbar.ui.imageprompt.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.nio.file.Path
import kotlin.test.*

class GalleryHistoryTest {
    @Test fun directoryGroupingUsesOnlyRecordedProvenanceAndPreferencesSurviveReopen() {
        val data=root();val bytes=EditorRenderer.synthetic(32,32);var ids=emptyList<String>();var before=emptyMap<String,ByteArray>()
        StudioWorkspace.openStudio(data).use{w->
            val roots=listOf(LibraryFolder(path=root().resolve("same-name").toString(),category="images"),LibraryFolder(path=root().resolve("same-name").toString(),category="images"))
            fun imported(folder:LibraryFolder,name:String)=w.importImage(name,bytes,buildJsonObject{put("kind","image-ingress");put("version",1);put("source","managed-folder");put("folderId",folder.id);put("relativePath",name)}.toString())
            ids=listOf(imported(roots[0],"a.png"),imported(roots[0],"b.png"),imported(roots[1],"c.png"),imported(roots[1],"d.png"),w.importImage("unknown.png",bytes))
            before=ids.associateWith{w.bytes(it)}
            val albums=galleryDirectoryAlbums(ids,w.catalog(),roots)
            assertEquals(listOf(2,2,1),albums.map{it.images.size});assertNotEquals(albums[0].label,albums[1].label)
            assertEquals(ids.take(2),albums[0].images.map{it.image.path})
            assertTrue(galleryDirectoryAlbums(ids.take(2),w.catalog(),emptyList()).single().label.contains("已移除"))
            val preferences=GalleryFoldPreferences(LibraryRepository(w));preferences.save(NovelAiHistoryFoldPreference(0,true),directory=true)
            preferences.save(NovelAiHistoryFoldPreference(1,true,NovelAiHistoryFoldType.BASE))
            assertEquals(setOf(0),preferences.directoryDepths());assertEquals(NovelAiHistoryFoldType.BASE,preferences.load().getValue(1).type)
            ids.forEach{assertContentEquals(before.getValue(it),w.bytes(it))}
        }
        StudioWorkspace.openStudio(data).use{w->val p=GalleryFoldPreferences(LibraryRepository(w));assertEquals(setOf(0),p.directoryDepths())
            p.save(p.load().getValue(0),directory=false);assertTrue(p.directoryDepths().isEmpty());ids.forEach{assertContentEquals(before.getValue(it),w.bytes(it))}}
    }
    private fun root()=Path.of(System.getProperty("preview.test.root"),newId())
    @Test fun verifiedHistoryFoldsUnknownAndCorruptReceiptsStayIndependentAndOriginalsRemain() {
        StudioWorkspace.openStudio(root()).use {w->
            val bytes=EditorRenderer.synthetic(32,32);val account=AccountProfile(alias="local",service="NAI",provider="NOVELAI",endpoint="https://image.novelai.net",model=NovelAiImageModel.V4_5_FULL.apiId)
            val jobs=mutableListOf<JobRecord>();val ids=mutableListOf<String>()
            for((n,prompt) in listOf("forest","forest ","ocean").withIndex()) {
                val recipe=NovelAiStudioDraft(basePrompt=prompt).toRecipe()
                val receipt=GenerationReceipt(recipe=recipe,account=account,request=buildJsonObject{put("input",prompt)})
                val raw=documentJson.encodeToString(receipt);val id=w.importImage("$n.png",bytes,raw);ids+=id
                jobs+=JobRecord(account=account,recipe=raw,results=listOf(id),state="COMPLETED",createdAt=1767225600000L+n)
            }
            val unknown=w.importImage("no-recipe.png",bytes)
            val corrupt=w.importImage("bad-receipt.png",bytes)
            val bad=documentJson.encodeToString(GenerationReceipt(recipe=NovelAiStudioDraft().toRecipe(),account=account,request=JsonObject(emptyMap()),wireSha256="invalid"))
            jobs+=JobRecord(account=account,recipe=bad,results=listOf(corrupt),state="COMPLETED")
            w.save(w.state.copy(jobs=jobs))
            val history=readGalleryHistory(w);assertEquals(3,history.entries.size);assertEquals(setOf(corrupt),history.unavailable.keys)
            val all=ids+unknown+corrupt;val albums=galleryAlbums(all,history,NovelAiHistoryFoldType.BASE)
            assertEquals(4,albums.size);assertEquals(ids.take(2).toSet(),albums.single{it.images.size==2}.images.map{it.image.path}.toSet())
            assertTrue(albums.filter{it.cover.image.path in setOf(unknown,corrupt)}.all{it.images.size==1})
            val selected=NovelAiHistorySelectionPolicy.selectAlbum(NovelAiHistorySelection(),albums,albums.first().key,false)
            assertEquals(2,selected.keys.size);all.forEach{assertContentEquals(bytes,w.bytes(it))}
            assertEquals(5,galleryAlbums(all,history,null).size)
            assertEquals(2,NovelAiHistoryFilterPolicy.filter(history.entries,"forest",null).size)
        }
    }
    @Test fun perDepthPreferencesSurviveBackupRestoreAndDateGrammarRejectsInvalidCalendar() {
        val data=root();val target=root();var backup:Path?=null
        StudioWorkspace.openStudio(data).use {w->
            val p=GalleryFoldPreferences(LibraryRepository(w));p.save(NovelAiHistoryFoldPreference(0,true,NovelAiHistoryFoldType.BASE));p.save(NovelAiHistoryFoldPreference(1,true,NovelAiHistoryFoldType.DAY))
            assertEquals(NovelAiHistoryFoldType.BASE,p.load().getValue(0).type);assertFails{p.save(NovelAiHistoryFoldPreference(32,true))};backup=w.backup()
        }
        StudioRestore.restore(backup!!,target)
        StudioWorkspace.openStudio(target).use{w->val p=GalleryFoldPreferences(LibraryRepository(w));assertEquals(NovelAiHistoryFoldType.DAY,p.load().getValue(1).type)}
        assertNull(galleryDateFilter(" "));assertEquals(NovelAiHistoryDateGranularity.YEAR,galleryDateFilter("2026")!!.granularity)
        assertEquals(NovelAiHistoryDateGranularity.MONTH,galleryDateFilter("2026-10")!!.granularity)
        assertEquals(22,galleryDateFilter("2026-10-22")!!.day);assertFails{galleryDateFilter("2026-02-30")};assertFails{galleryDateFilter("2026-13")}
    }
}
