package io.github.misakapiano.ccbstudio.core

data class TypesetOrganizationChange(val before:GenerationOrganization,val after:GenerationOrganization)
/** Folder edits and project lifecycle share one journal publication, never two partial commits. */
object TypesetOrganizationChanges {
    fun remove(org:GenerationOrganization,id:String):GenerationOrganization {
        val n=org.folders.single{it.id==id}
        return org.copy(folders=org.folders.filterNot{it.id==id}.map{if(it.parent==id)it.copy(parent=n.parent)else it},batches=org.batches.map{if(it.parent==id)it.copy(parent=n.parent)else it}).also{it.validate()}
    }
    fun resolve(w:StudioWorkspace,change:TypesetOrganizationChange?):GenerationOrganization {
        if(change==null)return w.state.generationOrganization
        require(w.state.generationOrganization==change.before){"组织已改变，请重新打开窗口"}
        change.after.validate()
        require(change.after.currentManual==change.before.currentManual)
        require(change.after.batches.map{it.copy(parent=null)}==change.before.batches.map{it.copy(parent=null)}){"本操作只允许调整组织"}
        return change.after
    }
    fun relocate(extras:TypesetEditorExtras,before:GenerationOrganization,after:GenerationOrganization):TypesetEditorExtras {
        fun parent(id:String?):String? {var next=id;val seen=mutableSetOf<String>();while(next!=null&&after.folders.none{it.id==next}){require(seen.add(next));next=before.folders.firstOrNull{it.id==next}?.parent};return next}
        return extras.copy(projectRecords=extras.projectRecords.map{it.copy(parent=parent(it.parent))})
    }
}
