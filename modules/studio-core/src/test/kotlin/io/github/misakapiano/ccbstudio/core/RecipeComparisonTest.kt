package io.github.misakapiano.ccbstudio.core
import kotlin.test.*
import kotlinx.serialization.json.*
class RecipeComparisonTest {
    @Test fun limitsPreserveRawAndDoNotTreatValidComplexDataAsCorruption() {
        val deep="[".repeat(10000)+"0"+"]".repeat(10000)
        assertEquals("LIMIT_EXCEEDED",RecipeComparison.inspect(deep).status)
        assertEquals(deep,RecipeComparison.inspect(deep).raw)
        assertEquals("LIMIT_EXCEEDED",RecipeComparison.inspect("["+List(50000){"0"}.joinToString(",")+"]").status)
        val quoted=JsonObject(mapOf("text" to JsonPrimitive("[".repeat(10000)+"\\\""))).toString()
        assertEquals("READABLE",RecipeComparison.inspect(quoted).status)
        assertEquals("READABLE",RecipeComparison.inspect("""{"version":{"vendor":"opaque"}}""").status)
    }
    @Test fun missingNullEmptyUnknownAndStructuredPathsAreDistinct() {
        val a=RecipeComparison.inspect("""{"a.b":null,"a":{"b":""},"x/y~z":[1,2],"empty":{}}""")
        val b=RecipeComparison.inspect("""{"a":{"b":null},"x/y~z":[2,1],"empty":[]}""")
        val rows=RecipeComparison.rows(a,b).associateBy{it.path}
        assertNull(rows.getValue("/a.b").b);assertEquals("null",rows.getValue("/a.b").a!!.kind)
        assertEquals("string",rows.getValue("/a/b").a!!.kind);assertTrue(rows.getValue("/a/b").different)
        assertEquals("1",rows.getValue("/x~1y~0z/0").a!!.value);assertTrue(rows.getValue("/empty").different)
        assertEquals("MISSING",RecipeComparison.inspect(null).status);assertEquals("CORRUPT",RecipeComparison.inspect("{").status)
        assertEquals("UNSUPPORTED_VERSION",RecipeComparison.inspect("""{"version":999,"opaque":true}""").status)
    }
    @Test fun exactRowsIncludeEqualValuesAndArrayOrder() {
        val raw="""{"negative":"","characters":[{"center":{"x":0.1}},{"center":{"x":0.9}}]}"""
        val a=RecipeComparison.inspect(raw);assertTrue(RecipeComparison.rows(a,a).all{!it.different});assertTrue(a.fields.containsKey("/characters/1/center/x"));assertEquals(raw,a.raw)
    }
}
