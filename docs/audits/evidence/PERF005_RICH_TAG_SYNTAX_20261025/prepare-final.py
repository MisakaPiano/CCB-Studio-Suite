from pathlib import Path
import re,subprocess
root=Path('H:/CCB-Studio-Suite');local=root/'.local/perf005-rich-tag-syntax-20261025'
base='73259d51414ccd95d9a2f911c845084d00215bc2'
prod='modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/TypesetLayout.kt'
test='modules/studio-core/src/test/kotlin/io/github/misakapiano/ccbstudio/core/RichTagSyntaxPerformanceTest.kt'
def git(*args):return subprocess.check_output(['git',*args],cwd=root)
assert git('rev-parse','HEAD').decode().strip()==base and git('status','--porcelain')==b''
old=git('show',base+':'+prod).decode();assert (local/'TypesetLayout.before.kt').read_bytes()==(root/prod).read_bytes()
assert (local/'TypesetLayout.before.kt').read_bytes().replace(b'\r\n',b'\n')==old.encode()
expr=r'Regex("^([a-zA-Z_]+)\\s*=\\s*(.+)$")';assert old.count(expr)==1
new=old.replace(expr,'richTagSyntax',1).replace('    private fun patch(tag:String,s:TypesetStyle):TypesetStyle? {','    private val richTagSyntax by lazy { '+expr+' }\n    private fun patch(tag:String,s:TypesetStyle):TypesetStyle? {',1)
subset=old[old.index('    fun tokens('):old.index('    private val force=')]
reference='private object RecordedRichTagTokens {\n'+re.sub(r'\bToken\b','TypesetLayout.Token',subset)+'}\n\n'
t=git('show',base+':'+test).decode()
t=t.replace('import java.util.UUID','import java.util.UUID\nimport java.util.concurrent.Executors\nimport java.util.concurrent.TimeUnit')
t=t.replace('/** Actual rich text',reference+'/** Actual rich text',1)
t=t.replace('            repeat(2){verify(TypesetLayout.tokens(text,base))}','            repeat(2){verify(RecordedRichTagTokens.tokens(text,base));verify(TypesetLayout.tokens(text,base))}')
begin=t.index('            repeat(4){round->');end=t.index('\n        }\n        println("RICH_TAG_TOKENS=',begin)
t=t[:begin]+'''            repeat(4){round->
                for(kind in if(round%2==0)listOf("recorded","current")else listOf("current","recorded")) {
                    val thread=Thread.currentThread().id;val allocated=bean.getThreadAllocatedBytes(thread)
                    val cpu=bean.currentThreadCpuTime;val started=System.nanoTime()
                    val actual=if(kind=="recorded")RecordedRichTagTokens.tokens(text,base)else TypesetLayout.tokens(text,base)
                    val elapsed=(System.nanoTime()-started)/1e6;val cpuMs=(bean.currentThreadCpuTime-cpu)/1e6
                    val allocatedBytes=bean.getThreadAllocatedBytes(thread)-allocated
                    verify(actual)
                    rows+=buildJsonObject{put("kind",kind);put("context",context);put("units",count);put("utf16Length",text.length);put("outputTokens",actual.size);put("serializedDocumentBytes",before.size);put("tokenOrderSha256",hash);put("round",round);put("wallMs",elapsed);put("cpuMs",cpuMs);put("allocatedBytes",allocatedBytes)}
                }
            }'''+t[end:]
edges=r'''
    @Test fun aliasesQuotedMalformedTagsNumericRulesAndLimitsKeepRecordedResults() {
        val base=TypesetStyle(fontFamily="Dialog",fontSize=31.0,fill="#123456",italic=true)
        val samples=listOf("", "[[b]中😀", "[unknown=7]X", "[size=48", "[/]X[/]", "[B][i][u][del]X[/][/][/][/]Y", "[plain]X[/]Y", "[ f = 'Example, serif' ]X[/][fontfamily=\"Dialog\"]Y[/]", "[size=0]X[/][size=NaN]Y[/][fs=-1]Z[/][fs=9999]Q[/]", "[weight=-5]A[/][w=9999]B[/][ow=Infinity]C[/][outlinewidth=200]D[/]", "[c=bad-color]A[/][fill=rgb(1,2,3)]B[/][outline=#abc]C[/][strokewidth=2]D[/]", "[size=\n48]X[/][size=]Y[/][sïze=48]Z[/]", "中文한글αβe\u0301😀𠀀\uD800")
        for(text in samples){val expected=RecordedRichTagTokens.tokens(text,base);repeat(3){assertEquals(expected,TypesetLayout.tokens(text,base))}}
        assertEquals("[b]中😀",TypesetLayout.tokens("[[b]中😀",base).joinToString(""){it.ch})
        assertEquals("[unknown=7]X",TypesetLayout.tokens("[unknown=7]X",base).joinToString(""){it.ch})
        assertEquals(listOf(31.0,31.0,6.0,1000.0),TypesetLayout.tokens("[size=0]X[/][size=NaN]Y[/][fs=-1]Z[/][fs=9999]Q[/]",base).map{it.style.fontSize})
        assertEquals("bad-color",TypesetLayout.tokens("[color=bad-color]X[/]",base).single().style.fill)
        assertEquals("z".repeat(256),TypesetLayout.tokens("[font=${"z".repeat(300)}]X[/]",base).single().style.fontFamily)
        val boundary="[b]".repeat(999)+"X"+"[/]".repeat(999)
        assertEquals(RecordedRichTagTokens.tokens(boundary,base),TypesetLayout.tokens(boundary,base))
        assertEquals(100000,TypesetLayout.tokens("X".repeat(100000),base).size)
        for(text in listOf("[b]".repeat(1000)+"X","X".repeat(100001))) {
            val oldError=assertFails{RecordedRichTagTokens.tokens(text,base)};val newError=assertFails{TypesetLayout.tokens(text,base)}
            assertEquals(oldError.javaClass,newError.javaClass);assertEquals(oldError.message,newError.message)
        }
        assertEquals(base,TypesetLayout.tokens("X",base).single().style)
        repeat(3){assertEquals(base,TypesetLayout.tokens("X",base).single().style)}
    }
    @Test fun simultaneousTagParsingKeepsIndependentStyleStacksAndResults() {
        val base=TypesetStyle(fontFamily="Dialog");val text="[size=48]中[/][color=#123456]A😀[/]B"
        val expected=RecordedRichTagTokens.tokens(text,base);val pool=Executors.newFixedThreadPool(2)
        try {
            val futures=List(2){pool.submit{repeat(100){val actual=TypesetLayout.tokens(text,base);assertEquals(expected,actual);assertNotSame(expected,actual);assertEquals(base,TypesetLayout.tokens("X",base).single().style)}}}
            futures.forEach{it.get(10,TimeUnit.SECONDS)}
        }finally{pool.shutdown();assertTrue(pool.awaitTermination(10,TimeUnit.SECONDS))}
    }
'''
assert t.endswith('}\n');t=t[:-2]+edges+'}\n'
for name,content in (('TypesetLayout.candidate.kt',new),('RichTagSyntaxPerformanceTest.final.kt',t)):
    with (local/name).open('x',encoding='utf-8',newline='\n') as f:f.write(content)
print('PREPARED_ONLY source='+base+' finalMethods='+str(t.count('@Test fun'))+' productionNotWritten=true')
