package io.github.misakapiano.ccbstudio.core

import java.nio.file.*
import kotlin.test.*

class LibraryRepositoryTest {
    private fun root()=Path.of(System.getProperty("preview.test.root")).resolve(newId())
    @Test fun damagedOrFutureManagementRecordsDoNotResetMetadataOrHideOriginals() {
        for(header in listOf("{\"version\":2,\"sequence\":2,\"previous\":null}","{\"version\":1,\"sequence\":9,\"previous\":null}")) {
            StudioWorkspace.openStudio(root()).use{w->val repo=LibraryRepository(w);val bytes=EditorRenderer.synthetic(32,32);val image=w.importImage("retained.png",bytes)
                repo.label(image,"original label",listOf("kept"));val count=w.catalog().size
                w.assets.put(io.github.misakapiano.ccbstudio.assets.AssetStore.Request(java.util.UUID.randomUUID(),"future.json",io.github.misakapiano.ccbstudio.assets.AssetStore.Kind.ORIGINAL,null,header.toByteArray(),LibraryRepository.TAG.toByteArray()),java.io.ByteArrayInputStream("{}".toByteArray()))
                assertFails{repo.load()};assertFails{repo.label(image,"must not replace",emptyList())}
                assertFails{repo.savePrompt("must not write","blocked","manual")}
                assertEquals(count+1,w.catalog().size);assertContentEquals(bytes,w.bytes(image));assertEquals(image,SharedAssets.images(w).single().request().id().toString())
            }
        }
    }
    @Test fun referencesExcludeWhitespaceAlignRolesWithoutMutatingOriginalAndRestore() {
        val data=root();val restored=root();var a="";var b="";var backup:Path?=null;var original=byteArrayOf()
        StudioWorkspace.openStudio(data).use{w->val repo=LibraryRepository(w);val refs=PromptReferences(repo)
            a=repo.savePrompt("room, red hair, blue hair","A","manual",sections=listOf(
                PromptSection("base","基础 Prompt","room"),PromptSection("empty","负向 Prompt"," \n\t"),
                PromptSection("ar","角色正向","red hair","red"),PromptSection("ab","角色正向","blue hair","blue")))
            b=repo.savePrompt("room, blue hair, red hair","B","manual",sections=listOf(
                PromptSection("base","基础 Prompt","room"),PromptSection("bb","角色正向","blue hair","blue"),
                PromptSection("br","角色正向","red hair","red"),PromptSection("empty-role","角色负向"," ","empty")))
            refs.add(a);refs.add(b);original=w.bytes(b)
            assertEquals(listOf("blue","red"),refs.roles(b));assertEquals(3,refs.rows().size)
            assertEquals("blue hair",refs.rows().single{it.key=="role:0:角色正向"}.cells[b]!!.text)
            refs.moveRole(b,"red","blue")
            assertEquals("red hair",refs.rows().single{it.key=="role:0:角色正向"}.cells[b]!!.text)
            assertTrue(refs.rows().all{it.cells.values.all{s->s.text.isNotBlank()}})
            assertContentEquals(original,w.bytes(b));assertEquals(listOf("room","blue hair","red hair"),refs.comparisonValues(b))
            assertFails{repo.savePrompt(" \n","blank","manual")}
            assertFails{refs.moveRole(b,"empty",null)}
            backup=w.backup()
        }
        StudioRestore.restore(backup!!,restored)
        StudioWorkspace.openStudio(restored).use{w->val refs=PromptReferences(LibraryRepository(w));assertEquals(listOf(a,b),refs.load().ids);assertEquals(listOf("red","blue"),refs.roles(b));assertContentEquals(original,w.bytes(b))}
    }
    @Test fun directoriesVersionAndMissingSourcesKeepAssetsLabelsPromptsAndBackup() {
        val data=root();val folder=Files.createDirectories(root());val textFolder=Files.createDirectories(root());val target=root()
        val file=folder.resolve("a.png");val old=EditorRenderer.synthetic(48,48);Files.write(file,old)
        Files.writeString(textFolder.resolve("a.prompt"),"quiet room, blue light")
        var image="";var prompt="";var backup:Path?=null
        StudioWorkspace.openStudio(data).use {w->val repo=LibraryRepository(w)
            val f=repo.addFolder(folder,"images",false).folders.single();repo.scan(f.id);image=repo.load().observations.values.single().assetId
            assertEquals("UNCHANGED",repo.scan(f.id).single().status)
            repo.label(image,"蓝色房间",listOf("收藏","参考"))
            Files.write(file,EditorRenderer.synthetic(60,48));assertEquals("NEW_VERSION",repo.scan(f.id).single().status)
            val next=repo.load().observations.values.single().assetId;assertNotEquals(image,next);assertEquals(repo.load().labels[image],repo.load().labels[next]);assertContentEquals(old,w.bytes(image))
            Files.move(file,folder.resolve("outside.txt"));repo.scan(f.id);assertEquals("SOURCE_MISSING",repo.load().observations.values.single().status)
            val tf=repo.addFolder(textFolder,"prompts",false).folders.last();repo.scan(tf.id);prompt=repo.prompts().single().request().id().toString()
            assertEquals("quiet room, blue light",repo.prompt(prompt).text);repo.label(prompt,"灯光",listOf("参考"))
            assertEquals(2,SharedAssets.images(w).size);assertFalse(SharedAssets.images(w).any{it.request().id().toString()==prompt})
            backup=w.backup()
        }
        StudioRestore.restore(backup!!,target)
        StudioWorkspace.openStudio(target).use {w->val repo=LibraryRepository(w);assertEquals("灯光",repo.load().labels[prompt]!!.alias);assertContentEquals(old,w.bytes(image));assertEquals("quiet room, blue light",repo.prompt(prompt).text)}
    }
    @Test fun promptEditsPreserveEarlierVersionAndRejectDataRootAsIngress() {
        StudioWorkspace.openStudio(root()).use {w->val repo=LibraryRepository(w)
            val a=repo.savePrompt("first prompt","name","manual");repo.label(a,"name",listOf("style"));val b=repo.savePrompt("second prompt","name v2","manual",a)
            assertEquals("first prompt",repo.prompt(a).text);assertEquals("second prompt",repo.prompt(b).text);assertEquals(listOf("style"),repo.load().labels[b]!!.tags)
            assertFails{repo.addFolder(w.root,"images",true)};assertFails{repo.addFolder(w.root.parent,"images",true)}
            assertEquals(2,repo.prompts().size);assertTrue(SharedAssets.images(w).isEmpty())
        }
    }
}
