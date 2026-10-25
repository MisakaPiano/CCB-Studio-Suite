from pathlib import Path
import re,subprocess
root=Path('H:/CCB-Studio-Suite');local=root/'.local/perf005-punctuation-classes-20261025'
base='d82f804d73557ab4e6fa633f2655835fcc3e6d03'
prod='modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/TypesetLayout.kt'
test='modules/studio-core/src/test/kotlin/io/github/misakapiano/ccbstudio/core/VerticalPunctuationClassPerformanceTest.kt'
def git(*args):return subprocess.check_output(['git',*args],cwd=root)
assert git('rev-parse','HEAD').decode().strip()==base and git('status','--porcelain')==b''
old=git('show',base+':'+prod).decode();raw=(local/'TypesetLayout.before.kt').read_bytes();assert raw==(root/prod).read_bytes() and raw.replace(b'\r\n',b'\n')==old.encode()
cls=old[old.index('    private fun cls('):old.index('    private fun reduction(')]
fields=[];new=old
for line in cls.splitlines():
    if not line.strip().startswith('in chars('):continue
    condition,value=line.strip().split('->');expr=condition[3:];kind=value.strip('"');assert kind in ('small','close','open','middle','emph')
    name=kind+'Punctuation';assert old.count(expr)==1;new=new.replace(expr,name,1)
    fields.append('    private val '+name+' by lazy { '+expr+'.toSet() }\n')
assert len(fields)==5;new=new.replace('    private fun cls(ch:String)=',''.join(fields)+'    private fun cls(ch:String)=',1)
chars=next(x for x in old.splitlines() if 'private fun chars(' in x)
subset=old[old.index('    private fun cls('):old.index('    data class StoryLayout(')]
subset=re.sub(r'\b(Token|Cell|Line|Layout)\b',r'TypesetLayout.\1',subset)
subset=re.sub(r'\b(tokens|verticalTokens|measure)\(',r'TypesetLayout.\1(',subset)
reference='private object RecordedVerticalPunctuationLayout {\n'+chars+'\n'+subset+'}\n\n'
t=git('show',base+':'+test).decode().replace('import java.util.UUID','import java.util.UUID\nimport java.util.concurrent.Executors\nimport java.util.concurrent.TimeUnit\nimport kotlin.math.*')
t=t.replace('/** Actual vertical layout',reference+'/** Actual vertical layout',1)
t=t.replace('            repeat(2){verify(TypesetLayout.layout(item))}','            repeat(2){verify(RecordedVerticalPunctuationLayout.layout(item));verify(TypesetLayout.layout(item))}')
begin=t.index('            repeat(4){round->');end=t.index('\n        }\n        println("VERTICAL_PUNCTUATION_CLASSES=',begin)
t=t[:begin]+'''            repeat(4){round->
                for(kind in if(round%2==0)listOf("recorded","current")else listOf("current","recorded")) {
                    val thread=Thread.currentThread().id;val allocated=bean.getThreadAllocatedBytes(thread)
                    val cpu=bean.currentThreadCpuTime;val started=System.nanoTime()
                    val actual=if(kind=="recorded")RecordedVerticalPunctuationLayout.layout(item)else TypesetLayout.layout(item)
                    val elapsed=(System.nanoTime()-started)/1e6;val cpuMs=(bean.currentThreadCpuTime-cpu)/1e6
                    val allocatedBytes=bean.getThreadAllocatedBytes(thread)-allocated
                    verify(actual)
                    rows+=buildJsonObject{put("kind",kind);put("enabled",enabled);put("inputCodepoints",count);put("utf16Length",text.length);put("lines",actual.lines.size);put("serializedDocumentBytes",before.size);put("layoutSha256",hash);put("round",round);put("wallMs",elapsed);put("cpuMs",cpuMs);put("allocatedBytes",allocatedBytes)}
                }
            }'''+t[end:]
edges=r'''
    @Test fun punctuationOrderTightnessStylesWrappingAndDisabledControlKeepFullLayout() {
        val style=TypesetStyle(fontFamily="Dialog")
        val samples=listOf("", "A，中", "，,﹐、﹑。．.：:﹕；;﹔", ")）｠}｝〕〙】》»〉›」』]］〛”‟’〞〟﹚﹜﹞", "(（｟{｛〔〘【《«〈‹「『[［〚“„‘‚‛〝﹙﹛﹝\"＂'＇", "…‥—―–！!﹗？?﹖", "中文한글αβe\u0301😀𠀀%・", "... .. . \"a\" a'b", "[size=48]中[/][b]A[/]，。\n한😀")
        for(text in samples)for(tightness in listOf("standard","compact","loose"))for(width in listOf(10.0,48.0,384.0)) {
            val item=TypesetText(text=text,style=style,width=width,vertical=true,punctuationTightness=tightness,letterSpacing=3.0)
            assertTrue(item.vertical);val before=item.copy()
            for(enabled in listOf(false,true)) {
                val input=item.copy(punctuationOptimization=enabled)
                assertEquals(RecordedVerticalPunctuationLayout.layout(input),TypesetLayout.layout(input))
            }
            assertEquals(before,item)
        }
        for(size in listOf(6.0,1000.0)) {
            val item=TypesetText(text="中，(A)…😀",style=style.copy(fontSize=size),width=48.0,vertical=true,lineHeight=2.0)
            assertEquals(RecordedVerticalPunctuationLayout.layout(item),TypesetLayout.layout(item))
        }
        val disabled=TypesetLayout.layout(TypesetText(text="A，中",style=style.copy(fontSize=10.0),width=1000.0,vertical=true,punctuationOptimization=false))
        assertEquals(listOf("normal","normal","normal"),disabled.lines.single().cells.map{it.cls})
        assertEquals(listOf(12.0,12.0,12.0),disabled.lines.single().cells.map{it.advance})
    }
    @Test fun simultaneousVerticalLayoutsKeepIndependentCellsAndLineState() {
        val item=TypesetText(text="中，(A)…😀\n한。",style=TypesetStyle(fontFamily="Dialog"),width=48.0,vertical=true)
        assertTrue(item.vertical);val expected=RecordedVerticalPunctuationLayout.layout(item);val pool=Executors.newFixedThreadPool(2)
        try {
            val futures=List(2){pool.submit{repeat(50){val actual=TypesetLayout.layout(item);assertEquals(expected,actual);assertNotSame(expected.lines,actual.lines)}}}
            futures.forEach{it.get(10,TimeUnit.SECONDS)}
        }finally{pool.shutdown();assertTrue(pool.awaitTermination(10,TimeUnit.SECONDS))}
    }
'''
assert t.endswith('}\n');t=t[:-2]+edges+'}\n'
for name,content in (('TypesetLayout.candidate-v2.kt',new),('VerticalPunctuationClassPerformanceTest.final-v2.kt',t)):
    with (local/name).open('x',encoding='utf-8',newline='\n') as f:f.write(content)
print('PREPARED_ONLY source='+base+' fields='+str(len(fields))+' finalMethods='+str(t.count('@Test fun'))+' productionNotWritten=true')
