package io.github.misakapiano.ccbstudio.core

import kotlinx.serialization.json.*

data class RecipeField(val kind:String,val value:String)
data class RecipeInspection(val status:String,val fields:Map<String,RecipeField>,val raw:String?)
data class RecipeDifference(val path:String,val a:RecipeField?,val b:RecipeField?) {val different get()=a!=b}
object RecipeComparison {
    private class InspectionLimit:RuntimeException()
    // Bound nesting before the recursive JSON parser runs. Brackets inside strings are data.
    private fun checkNesting(raw:String) {
        var depth=0;var quoted=false;var escaped=false
        for(c in raw){
            if(quoted){if(escaped)escaped=false else if(c=='\\')escaped=true else if(c=='"')quoted=false}
            else when(c){'"'->quoted=true;'[','{'->{depth++;if(depth>128)throw InspectionLimit()};']','}'->depth--}
        }
    }
    /** JSON Pointer paths preserve literal dots, slashes and brackets; missing is never JSON null. */
    fun inspect(raw:String?):RecipeInspection {
        if(raw.isNullOrBlank())return RecipeInspection("MISSING",emptyMap(),raw)
        if(raw.length>64*1024*1024)return RecipeInspection("TOO_LARGE",emptyMap(),raw)
        return try {
            checkNesting(raw)
            val root=Json.parseToJsonElement(raw);val fields=linkedMapOf<String,RecipeField>()
            fun walk(value:JsonElement,path:String,depth:Int){if(depth>128 || fields.size>=50000)throw InspectionLimit()
                fields[path]=when(value){is JsonObject->RecipeField("object","${value.size} fields");is JsonArray->RecipeField("array","${value.size} items");JsonNull->RecipeField("null","null");is JsonPrimitive->RecipeField(if(value.isString)"string"else"literal",value.toString())}
                when(value){is JsonObject->value.forEach{(k,v)->walk(v,path+"/"+k.replace("~","~0").replace("/","~1"),depth+1)};is JsonArray->value.forEachIndexed{i,v->walk(v,"$path/$i",depth+1)};else->Unit}
            }
            walk(root,"",0);val version=((root as? JsonObject)?.get("version") as? JsonPrimitive)?.intOrNull
            RecipeInspection(if(version!=null && version !in 1..3)"UNSUPPORTED_VERSION"else"READABLE",fields,raw)
        }catch(e:InspectionLimit){RecipeInspection("LIMIT_EXCEEDED",emptyMap(),raw)}catch(e:Exception){RecipeInspection("CORRUPT",emptyMap(),raw)}
    }
    fun rows(a:RecipeInspection,b:RecipeInspection):List<RecipeDifference> =(a.fields.keys+b.fields.keys).sorted().distinct().map{RecipeDifference(it,a.fields[it],b.fields[it])}
}
