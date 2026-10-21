package io.github.misakapiano.ccbstudio.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

@Serializable data class PromptReferenceState(val ids:List<String> = emptyList(),val roleOrders:Map<String,List<String>> = emptyMap())
data class PromptReferenceRow(val key:String,val label:String,val cells:Map<String,PromptSection>)
/** Comparison presentation only: never mutates prompt assets, draft roles or requests. */
class PromptReferences(private val library:LibraryRepository) {
    fun load():PromptReferenceState=library.load().preferences["promptReferences"]?.let{documentJson.decodeFromString<PromptReferenceState>(it)}?:PromptReferenceState()
    private fun save(s:PromptReferenceState)=library.update{it.copy(preferences=it.preferences+("promptReferences" to documentJson.encodeToString(s)))}
    fun add(id:String){val prompt=library.prompt(id);require(prompt.text.isNotBlank() || prompt.sections.any{it.text.isNotBlank()});val s=load();if(id !in s.ids){require(s.ids.size<16){"A reference board supports up to 16 sources"};save(s.copy(ids=s.ids+id))}}
    fun remove(id:String){val s=load();save(s.copy(ids=s.ids-id,roleOrders=s.roleOrders-id))}
    fun sections(id:String):List<PromptSection> {val p=library.prompt(id);return (if(p.sections.isEmpty())listOf(PromptSection(id="body",label="Prompt",text=p.text))else p.sections).filter{it.text.isNotBlank()}}
    fun roles(id:String):List<String> {val ids=sections(id).mapNotNull{it.roleId}.distinct();return (load().roleOrders[id].orEmpty().filter{it in ids}+ids).distinct()}
    fun moveRole(id:String,role:String,before:String?) {val s=load();require(id in s.ids);val order=roles(id);require(role in order && (before==null || before in order));if(role==before)return
        val next=order-role;val index=before?.let{next.indexOf(it)}?:next.size;save(s.copy(roleOrders=s.roleOrders+(id to (next.take(index)+role+next.drop(index)))))
    }
    fun comparisonValues(id:String)=sections(id).flatMap{it.text.split(Regex(",|\\r?\\n"))}.map{it.trim()}.filter{it.isNotEmpty()}
    /** Row alignment is independent of text height. No synthetic blank Prompt is inserted. */
    fun rows():List<PromptReferenceRow> {
        val ids=load().ids;val all=ids.associateWith{sections(it)};val out=mutableListOf<PromptReferenceRow>()
        all.values.flatten().filter{it.roleId==null}.map{it.label}.distinct().forEach{label->
            out+=PromptReferenceRow("global:$label",label,ids.mapNotNull{id->all.getValue(id).firstOrNull{it.roleId==null && it.label==label}?.let{id to it}}.toMap())
        }
        val ordered=ids.associateWith{roles(it)}
        repeat(ordered.values.maxOfOrNull{it.size}?:0){index->
            val groups=ids.associateWith{id->all.getValue(id).filter{it.roleId!=null && it.roleId==ordered.getValue(id).getOrNull(index)}}
            groups.values.flatten().map{it.label}.distinct().forEach{label->
                out+=PromptReferenceRow("role:$index:$label","角色位置 ${index+1} · $label",ids.mapNotNull{id->groups.getValue(id).firstOrNull{it.label==label}?.let{id to it}}.toMap())
            }
        }
        return out
    }
}
