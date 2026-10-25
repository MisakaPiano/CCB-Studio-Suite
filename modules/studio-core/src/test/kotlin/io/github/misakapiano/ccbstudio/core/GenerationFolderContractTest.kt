package io.github.misakapiano.ccbstudio.core

import kotlinx.serialization.encodeToString
import java.util.UUID
import kotlin.test.*

/** Folder selection relies on the existing admitted graph and its depth-first order. */
class GenerationFolderContractTest {
    private fun folder(index:Int)=UUID(6L,index.toLong()+1).toString()
    @Test fun depthFirstOrderUnknownRootsAndReparentedSnapshotsStayIndependent() {
        val org=GenerationOrganization(folders=listOf(
            GenerationFolder(id=folder(4),name="B child",parent=folder(2)),
            GenerationFolder(id=folder(2),name="B",parent=folder(0)),
            GenerationFolder(id=folder(5),name="unrelated"),
            GenerationFolder(id=folder(3),name="A child",parent=folder(1)),
            GenerationFolder(id=folder(0),name="root"),
            GenerationFolder(id=folder(1),name="A",parent=folder(0))))
        org.validate();val before=documentJson.encodeToString(org)
        assertEquals(listOf(0,2,4,1,3).map(::folder),org.descendants(folder(0)).toList())
        assertEquals(listOf(2,4).map(::folder),org.descendants(folder(2)).toList())
        assertEquals(setOf(folder(5)),org.descendants(folder(5)))
        assertEquals(setOf(folder(999)),org.descendants(folder(999)))
        assertEquals(setOf("unknown"),org.descendants("unknown"))
        assertEquals(setOf("unknown"),GenerationOrganization().descendants("unknown"))
        val moved=org.copy(folders=org.folders.map{if(it.id==folder(4))it.copy(parent=folder(1))else it});moved.validate()
        assertEquals(listOf(0,2,1,4,3).map(::folder),moved.descendants(folder(0)).toList())
        assertEquals(listOf(0,2,4,1,3).map(::folder),org.descendants(folder(0)).toList())
        assertEquals(before,documentJson.encodeToString(org))
    }
    @Test fun originalAdmissionStillRejectsInvalidGraphSizeAndDepth() {
        val root=GenerationFolder(id=folder(0),name="root")
        val invalid=listOf(
            GenerationOrganization(folders=listOf(root,root.copy(name="duplicate"))),
            GenerationOrganization(folders=listOf(root.copy(id="not-a-folder-id"))),
            GenerationOrganization(folders=listOf(root.copy(name=""))),
            GenerationOrganization(folders=listOf(root.copy(kind="unsupported"))),
            GenerationOrganization(folders=listOf(root.copy(parent=folder(99)))),
            GenerationOrganization(folders=listOf(root.copy(parent=folder(1)),GenerationFolder(id=folder(1),name="cycle",parent=folder(0)))),
            GenerationOrganization(folders=List(33){i->GenerationFolder(id=folder(i),name="deep$i",parent=if(i==0)null else folder(i-1))}),
            GenerationOrganization(folders=List(1001){i->GenerationFolder(id=folder(i),name="many$i")}))
        invalid.forEach{assertFails{it.validate()}}
        val maximum=GenerationOrganization(folders=List(32){i->GenerationFolder(id=folder(i),name="allowed$i",parent=if(i==0)null else folder(i-1))})
        maximum.validate();assertEquals(List(32,::folder),maximum.descendants(folder(0)).toList())
    }
}
