package io.github.misakapiano.ccbstudio.desktop

import io.github.misakapiano.ccbstudio.core.*
import kotlinx.serialization.encodeToString
import java.util.UUID
import kotlin.test.*

/** Pure card metadata adapter, no native window/controller/clipboard/source-directory access. */
class FolderSubtreeCardProjectionTest {
    private fun folder(i:Int)=UUID(6L,i.toLong()+1).toString()
    private fun batch(i:Int)=UUID(7L,i.toLong()+1).toString()
    private fun image(i:Int)=UUID(8L,i.toLong()+1).toString()
    @Test fun folderCardsKeepCurrentAndArchivedMembershipOrderingAndReparentedScopes() {
        fun sample(i:Int,parent:String?,archived:Boolean)=GenerationBatch(id=batch(i),kind="auto",alias="Synthetic batch $i",createdAt=1_700_000_000_000L+i,archivedAt=if(archived)1_700_000_100_000L+i else null,parent=parent,
            groups=listOf(GenerationBatchGroup(UUID(9L,i.toLong()+1).toString(),"Synthetic group",listOf(image(i)))))
        val org=GenerationOrganization(folders=listOf(GenerationFolder(id=folder(0),name="root"),GenerationFolder(id=folder(1),name="child",parent=folder(0)),GenerationFolder(id=folder(2),name="leaf",parent=folder(1)),GenerationFolder(id=folder(3),name="other")),
            batches=listOf(sample(5,folder(3),true),sample(4,folder(1),false),sample(3,folder(2),true),sample(2,folder(0),true),sample(1,null,true),sample(0,folder(1),true)))
        StudioState(version=3,generationOrganization=org).validate();val before=documentJson.encodeToString(org)
        val g=GalleryViewState();val history=GalleryHistoryProjection(emptyList(),emptyMap())
        g.levels=listOf(GalleryScope(label="organizations",identity="org:organizations:"))
        val rootRows=organizationFolders(g,emptyList(),history,org)
        assertEquals(listOf("folder:${folder(0)}","folder:${folder(3)}","batch:${batch(1)}"),rootRows.map{"${it.kind}:${it.id}"})
        assertEquals(listOf(4,3,2,0).map(::image).toSet(),rootRows.first().ids)
        assertEquals(setOf(image(5)),rootRows[1].ids);assertEquals(setOf(image(1)),rootRows[2].ids)
        g.levels=listOf(GalleryScope(label="root",identity="org:folder:${folder(0)}"))
        val childRows=organizationFolders(g,emptyList(),history,org)
        assertEquals(listOf("folder:${folder(1)}","batch:${batch(2)}"),childRows.map{"${it.kind}:${it.id}"})
        assertEquals(listOf(4,3,0).map(::image).toSet(),childRows.first().ids)
        val moved=org.copy(folders=org.folders.map{if(it.id==folder(1))it.copy(parent=folder(3))else it});moved.validate()
        val after=organizationFolders(g,emptyList(),history,moved)
        assertEquals(listOf(batch(2)),after.map{it.id});assertEquals(setOf(image(2)),after.single().ids)
        assertEquals(before,documentJson.encodeToString(org))
    }
}
