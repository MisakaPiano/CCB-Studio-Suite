// Exact organizationFolders algorithm from 69f53d3; test-only comparison.
package io.github.misakapiano.ccbstudio.desktop
import io.github.misakapiano.ccbstudio.core.*

internal fun legacyOrganizationFolders(g:GalleryViewState,albums:List<VirtualAlbum>,p:GalleryHistoryProjection,org:GenerationOrganization=GenerationOrganization()):List<OrganizationFolder> = when(g.organizationKind){
    "organizations","folder"->{val parent=g.organizationId.takeIf{g.organizationKind=="folder"};org.folders.filter{it.parent==parent}.map{f->OrganizationFolder("folder",f.id,f.name,org.batches.filter{it.parent in org.descendants(f.id)}.flatMap{generationBatchImages(org,it.id,p)}.toSet())}+org.batches.filter{it.parent==parent&&it.archivedAt!=null}.map{b->OrganizationFolder("batch",b.id,b.title,generationBatchImages(org,b.id,p))}}
    "batches","type"->(p.batchOrigins.values.map{it.batchId}+org.batches.map{it.id}).distinct().filter{id->g.organizationKind!="type"||(org.batches.find{it.id==id}?.kind?:"auto")==g.organizationId}.map{id->OrganizationFolder("batch",id,org.batches.find{it.id==id}?.title?:p.batchOrigins.values.first{it.batchId==id}.batchName.ifBlank{"批次 ${id.take(8)}"},generationBatchImages(org,id,p))}
    "batch"->org.batches.find{it.id==g.organizationId}?.groups?.takeIf{it.isNotEmpty()}?.map{OrganizationFolder("members",it.id,it.name,it.images.toSet())}?:p.batchOrigins.filterValues{it.batchId==g.organizationId}.entries.groupBy{it.value.groupId}.map{(id,entries)->OrganizationFolder("group",id,entries.first().value.groupName,entries.map{it.key}.toSet())}
    "album","albums"->albums.filter{it.parent==(if(g.organizationKind=="album")g.organizationId else null)}.map{OrganizationFolder("album",it.id,it.name,VirtualAlbums.members(albums,it.id))}
    else->emptyList()
}
