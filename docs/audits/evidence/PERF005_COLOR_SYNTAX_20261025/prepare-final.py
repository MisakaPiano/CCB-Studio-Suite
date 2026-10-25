from pathlib import Path
import subprocess
ROOT=Path('H:/CCB-Studio-Suite');LOCAL=ROOT/'.local/perf005-color-syntax-20261025'
BASE='1a007ab11f7fa50520d57387d2000e3d2f1cebf9'
PROD='modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/TypesetColors.kt'
TEST='modules/studio-core/src/test/kotlin/io/github/misakapiano/ccbstudio/core/TypesetColorSyntaxPerformanceTest.kt'
def git(*args):return subprocess.check_output(['git',*args],cwd=ROOT)
assert git('diff',BASE,'HEAD','--','modules','tools')==b'' and git('diff','HEAD','--','modules','tools')==b''
old=git('show',BASE+':'+PROD).decode();t=git('show',BASE+':'+TEST).decode()
ref=old[old.index('object TypesetColors {'):].replace('object TypesetColors {','private object RecordedTypesetSyntaxColors {',1).rstrip()
t=t.replace('import java.util.UUID','import java.util.UUID\nimport java.util.Locale\nimport java.awt.Color\nimport kotlin.math.*',1)
t=t.replace('/** Actual parser calls',ref+'\n\n/** Actual parser calls',1)
assert 'private object RecordedTypesetSyntaxColors' in t
t=t.replace('            repeat(2){verify(colors.map{TypesetColors.parse(it)})}','            repeat(2){verify(colors.map{RecordedTypesetSyntaxColors.parse(it)});verify(colors.map{TypesetColors.parse(it)})}',1)
t=t.replace('            repeat(4){round->','            repeat(4){round->\n              for(mode in if(round%2==0)listOf("recorded","current")else listOf("current","recorded")) {',1)
t=t.replace('                val actual=colors.map{TypesetColors.parse(it)}','                val actual=colors.map{if(mode=="recorded")RecordedTypesetSyntaxColors.parse(it)else TypesetColors.parse(it)}',1)
t=t.replace('rows+=buildJsonObject{put("context",kind);','rows+=buildJsonObject{put("kind",mode);put("context",kind);',1)
t=t.replace('            }\n        }\n        println("TYPESET_COLOR_SYNTAX="','              }\n            }\n        }\n        println("TYPESET_COLOR_SYNTAX="',1)
methods='''    @Test fun exactArgbAndInvalidOutcomeMatchRecordedParserIncludingTrackedSourceCases() {
        val tracked=documentJson.parseToJsonElement(requireNotNull(javaClass.getResource("/typeset/source-colors.json")).readText()).jsonObject.getValue("cases").jsonArray
        assertEquals(162,tracked.size)
        val extra=listOf(" RED ","#1234","transparent","rgb(255 0 0 / .5)","hsl(120 100% 50%)","rgb(\\n1 2\\n3 / .5\\n)","hwb(2turn 10% 20% / .4)","RGB(1e2 2e1 3e0 / 25%)")
        val invalid=listOf("#12","not-a-color","rgb(1,2)","rgb(NaN 0 0)","hsl(0 50 50)","rgba(1,2,3,4,5)","rgb(1e999 0 0)","rgb(1 2 3 4)","rgb(1,2,3 / .5)","hwb(0,10%,20%)","color(display-p3 1 0 0)","oklch(50% .2 30)","x".repeat(257))
        fun outcome(parser:(String)->Color,text:String):Pair<Int?,Pair<String,String?>?> = try{parser(text).rgb to null}catch(e:IllegalArgumentException){null to (e.javaClass.name to e.message)}
        for(value in tracked.map{it.jsonObject.getValue("value").jsonPrimitive.content}+extra+invalid)
            assertEquals(outcome(RecordedTypesetSyntaxColors::parse,value),outcome(TypesetColors::parse,value),value)
        invalid.forEach{assertFailsWith<IllegalArgumentException>{TypesetColors.parse(it)}}
        assertEquals(Color(17,34,51,68).rgb,TypesetColors.parse("#1234").rgb)
        assertEquals(0,TypesetColors.parse("transparent").rgb)
        assertEquals(Color(255,0,0).rgb,TypesetColors.parse(" RED ").rgb)
        assertEquals(Color(255,0,0,128).rgb,TypesetColors.parse("rgb(255 0 0 / .5)").rgb)
        assertEquals(Color(0,255,0).rgb,TypesetColors.parse("hsl(120 100% 50%)").rgb)
    }
    @Test fun concurrentParserReuseKeepsIndependentMatchAndColorResults() {
        val inputs=listOf("rgba(240,12,30,.5)","hsl(210 50% 40% / 70%)","hwb(2turn 10% 20% / .4)","rgb(5e1 1e2 2e2 / .8)","#1234","rebeccapurple","transparent")
        val expected=inputs.associateWith{RecordedTypesetSyntaxColors.parse(it).rgb}
        val pool=java.util.concurrent.Executors.newFixedThreadPool(2)
        try {
            val futures=(0..1).map{worker->pool.submit(java.util.concurrent.Callable {
                repeat(100){round->for(offset in inputs.indices){val value=inputs[(offset+round+worker)%inputs.size];assertEquals(expected.getValue(value),TypesetColors.parse(value).rgb)}}
                true
            })}
            futures.forEach{assertTrue(it.get(20,java.util.concurrent.TimeUnit.SECONDS))}
        } finally {pool.shutdown();assertTrue(pool.awaitTermination(10,java.util.concurrent.TimeUnit.SECONDS))}
    }
'''
assert t.endswith('}\n');t=t[:-2]+methods+'}\n'
target=LOCAL/'TypesetColorSyntaxPerformanceTest.final.kt';assert not target.exists();target.write_text(t,encoding='utf-8')
print('Prepared final test only under authorized localroot;no module writes or test execution')
