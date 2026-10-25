from pathlib import Path
import hashlib, subprocess

root=Path('H:/CCB-Studio-Suite')
local=root/'.local/perf005-vertical-tokens-20261025'
prod='modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/TypesetLayout.kt'
test='modules/studio-core/src/test/kotlin/io/github/misakapiano/ccbstudio/core/VerticalTokenProjectionPerformanceTest.kt'
base='c6efb5e36827311da9e919521723a0dd7fce3f97'
def git(*args):return subprocess.check_output(['git',*args],cwd=root)
assert git('rev-parse','HEAD').decode().strip()==base
assert git('status','--porcelain')==b''
before=(local/'TypesetLayout.before.kt').read_bytes()
assert before==(root/prod).read_bytes()
old=git('show',base+':'+prod).decode('utf-8')
assert before.replace(b'\r\n',b'\n')==old.encode()
new=old
double=r'ch in setOf("\"","＂")'
single='ch in setOf("\'","＇")'
regex=r'Regex("\\p{P}")'
symbols=r'chars("/\\／＼~～〜=＝+＋*＊&＆@＠#＃%％^＾<>＜＞|｜")'
for expr,replacement in ((double,r'ch=="\"" || ch=="＂"'),(single,'ch=="\'" || ch=="＇"'),(regex,'verticalPunctuation'),(symbols,'rotationSymbols')):
    assert new.count(expr)==1,(expr,new.count(expr))
    new=new.replace(expr,replacement,1)
marker='    private fun presentation(ch:String)='
assert new.count(marker)==1
new=new.replace(marker,'    private val verticalPunctuation by lazy { '+regex+' }\n    private val rotationSymbols by lazy { '+symbols+'.toSet() }\n'+marker,1)

chars=next(x for x in old.splitlines() if 'private fun chars(' in x)
constants=old[old.index('    private val force='):old.index('    fun upright(')]
projection=old[old.index('    fun verticalTokens('):old.index('    private fun cls(')]
projection=projection.replace('List<Token>','List<TypesetLayout.Token>').replace('mutableListOf<Token>','mutableListOf<TypesetLayout.Token>')
reference='private object RecordedVerticalProjection {\n'+chars+'\n'+constants+projection+'}\n\n'
t=git('show',base+':'+test).decode('utf-8')
t=t.replace('import java.util.UUID','import java.util.UUID\nimport java.util.concurrent.Executors\nimport java.util.concurrent.TimeUnit')
t=t.replace('/** Actual character projection',reference+'/** Actual character projection',1)
t=t.replace('            repeat(2){verify(TypesetLayout.verticalTokens(input))}', '            repeat(2){verify(RecordedVerticalProjection.verticalTokens(input));verify(TypesetLayout.verticalTokens(input))}')
begin=t.index('            repeat(4){round->')
end=t.index('\n        }\n        println("VERTICAL_TOKEN_PROJECTION=',begin)
t=t[:begin]+'''            repeat(4){round->
                for(kind in if(round%2==0)listOf("recorded","current")else listOf("current","recorded")) {
                    val thread=Thread.currentThread().id;val allocated=bean.getThreadAllocatedBytes(thread)
                    val cpu=bean.currentThreadCpuTime;val started=System.nanoTime()
                    val actual=if(kind=="recorded")RecordedVerticalProjection.verticalTokens(input)else TypesetLayout.verticalTokens(input)
                    val elapsed=(System.nanoTime()-started)/1e6;val cpuMs=(bean.currentThreadCpuTime-cpu)/1e6
                    val allocatedBytes=bean.getThreadAllocatedBytes(thread)-allocated
                    verify(actual)
                    rows+=buildJsonObject{put("kind",kind);put("context",context);put("inputTokens",input.size);put("utf16Length",text.length);put("outputTokens",actual.size);put("serializedDocumentBytes",before.size);put("tokenOrderSha256",hash);put("round",round);put("wallMs",elapsed);put("cpuMs",cpuMs);put("allocatedBytes",allocatedBytes)}
                }
            }'''+t[end:]
edges=r'''
    @Test fun unicodeStylesQuoteStateAndInvalidHelpersKeepRecordedSemantics() {
        val style=TypesetStyle(fontFamily="Dialog",fontSize=31.0,fill="#123456",italic=true)
        val samples=listOf("", "\"＂\"＂ '＇'＇", "can't a＇b '中'", ". .. ... .... .....", "︐︵︶・･·‧•※々〃ゝゞヽヾ﹅﹆", "/\\／＼~～〜=＝+＋*＊&＆@＠#＃%％^＾<>＜＞|｜", "中文한글αβ😀e\u0301𠀀\n\t", "[b]A[/][size=42]中[/]...")
        for(text in samples) {
            val input=TypesetLayout.tokens(text,style);val snapshot=input.toList()
            val expected=RecordedVerticalProjection.verticalTokens(input)
            repeat(3){assertEquals(expected,TypesetLayout.verticalTokens(input));assertEquals(snapshot,input)}
        }
        for(quote in listOf("\"","＂","'","＇"))repeat(3) {
            assertEquals(listOf(if(quote=="\""||quote=="＂")"﹃"else"﹁"),TypesetLayout.verticalTokens(TypesetLayout.tokens(quote,style)).map{it.display})
        }
        val decorated=listOf(TypesetLayout.Token("中",style,"custom",rotate=true,forced=true,alnum=true),TypesetLayout.Token("%",style.copy(fontSize=47.0),"mark",forced=true))
        assertEquals(RecordedVerticalProjection.verticalTokens(decorated),TypesetLayout.verticalTokens(decorated))
        // Empty helper tokens are not admitted document input; preserve the old exception too.
        val invalid=listOf(TypesetLayout.Token("",style))
        val oldError=assertFails{RecordedVerticalProjection.verticalTokens(invalid)}
        val newError=assertFails{TypesetLayout.verticalTokens(invalid)}
        assertEquals(oldError.javaClass,newError.javaClass);assertEquals(oldError.message,newError.message)
    }
    @Test fun simultaneousProjectionKeepsPerCallQuoteStateAndFreshResults() {
        val input=TypesetLayout.tokens("\"中文😀...\" 'a' a'b α%・",TypesetStyle(fontFamily="Dialog"))
        val snapshot=input.toList();val expected=RecordedVerticalProjection.verticalTokens(input)
        val pool=Executors.newFixedThreadPool(2)
        try {
            val futures=List(2){pool.submit{repeat(100){val actual=TypesetLayout.verticalTokens(input);assertEquals(expected,actual);assertNotSame(expected,actual);assertEquals(snapshot,input)}}}
            futures.forEach{it.get(10,TimeUnit.SECONDS)}
        }finally{pool.shutdown();assertTrue(pool.awaitTermination(10,TimeUnit.SECONDS))}
    }
'''
assert t.endswith('}\n');t=t[:-2]+edges+'}\n'
for name,content in (('TypesetLayout.candidate.kt',new),('VerticalTokenProjectionPerformanceTest.final.kt',t)):
    with (local/name).open('x',encoding='utf-8',newline='\n') as f:f.write(content)
print('PREPARED_ONLY productionNotWritten=true baseline='+base+' candidateSha256='+hashlib.sha256(new.encode()).hexdigest()+' finalTestMethods='+str(t.count('@Test fun')))
