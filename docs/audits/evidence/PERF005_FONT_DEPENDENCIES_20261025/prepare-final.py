from pathlib import Path
import subprocess
root=Path('H:/CCB-Studio-Suite');local=root/'.local/perf005-font-dependencies-20261025'
base='b049e4c7bdc292a26e5623313c1c1d91933ca9b9'
prod='modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/TypesetFonts.kt'
test='modules/studio-core/src/test/kotlin/io/github/misakapiano/ccbstudio/core/DocumentFontDependenciesPerformanceTest.kt'
def git(*args):return subprocess.check_output(['git',*args],cwd=root)
assert git('rev-parse','HEAD').decode().strip()==base and git('status','--porcelain')==b''
old=git('show',base+':'+prod).decode();raw=(local/'TypesetFonts.before.kt').read_bytes();assert raw==(root/prod).read_bytes() and raw.replace(b'\r\n',b'\n')==old.encode()
begin=old.index('    fun documentFonts(');end=old.index('    fun verifyRenderable(',begin);function=old[begin:end]
newFunction='''    fun documentFonts(document:TypesetDocument):List<String> {
        val out=ArrayList<String>()
        document.pages.forEach { p->
            out.add(p.story.fontFamily)
            p.texts.forEach { t->
                out.add(t.style.fontFamily)
                TypesetLayout.tokens(t.text,t.style).forEach { out.add(it.style.fontFamily) }
            }
        }
        document.presets.forEach { out.add(it.text.style.fontFamily) }
        return out
    }
'''
new=old[:begin]+newFunction+old[end:]
reference='private object RecordedDocumentFontDependencies {\n'+function+'}\n\n'
t=git('show',base+':'+test).decode().replace('import java.util.UUID','import java.util.UUID\nimport java.util.concurrent.Executors\nimport java.util.concurrent.TimeUnit')
t=t.replace('/** Font family strings',reference+'/** Font family strings',1)
t=t.replace('            repeat(2){verify(TypesetFonts.documentFonts(doc))}','            repeat(2){verify(RecordedDocumentFontDependencies.documentFonts(doc));verify(TypesetFonts.documentFonts(doc))}')
begin=t.index('            repeat(4){round->');end=t.index('\n        }\n        println("DOCUMENT_FONT_DEPENDENCIES=',begin)
t=t[:begin]+'''            repeat(4){round->
                for(kind in if(round%2==0)listOf("recorded","current")else listOf("current","recorded")) {
                    val thread=Thread.currentThread().id;val allocated=bean.getThreadAllocatedBytes(thread)
                    val cpu=bean.currentThreadCpuTime;val started=System.nanoTime()
                    val actual=if(kind=="recorded")RecordedDocumentFontDependencies.documentFonts(doc)else TypesetFonts.documentFonts(doc)
                    val elapsed=(System.nanoTime()-started)/1e6;val cpuMs=(bean.currentThreadCpuTime-cpu)/1e6
                    val allocatedBytes=bean.getThreadAllocatedBytes(thread)-allocated
                    verify(actual)
                    rows+=buildJsonObject{put("kind",kind);put("pages",pages);put("texts",pages*8);put("context",context);put("fontReferences",actual.size);put("serializedDocumentBytes",before.size);put("fontOrderSha256",hash);put("round",round);put("wallMs",elapsed);put("cpuMs",cpuMs);put("allocatedBytes",allocatedBytes)}
                }
            }'''+t[end:]
edges=r'''
    @Test fun emptyHiddenTextPresetsDuplicatesAndParserErrorsKeepDependencySemantics() {
        val alias="ccb-font:"+UUID(34L,1L)
        val doc=TypesetDocument(pages=listOf(TypesetPage(assetId=UUID(35L,1L).toString(),name="All text included",story=StoryStyle(fontFamily=alias),texts=listOf(TypesetText(text="",style=TypesetStyle(fontFamily="EmptyBase")),TypesetText(text="[font='Inline']A[/]B",visible=false,locked=true,style=TypesetStyle(fontFamily=alias)),TypesetText(text="[unknown=9]中文😀[[",style=TypesetStyle(fontFamily="Dialog"))))),presets=listOf(TypesetPreset(name="Only base is used",text=TypesetText(text="[font=PresetInline]X[/]",style=TypesetStyle(fontFamily="PresetBase")))))
        doc.validate();val before=documentJson.encodeToString(doc).toByteArray()
        val expected=RecordedDocumentFontDependencies.documentFonts(doc);assertEquals(expected,TypesetFonts.documentFonts(doc))
        assertEquals(listOf(alias,"EmptyBase",alias,"Inline",alias),expected.take(5));assertEquals("PresetBase",expected.last());assertTrue("PresetInline" !in expected)
        val mutableCopy=TypesetFonts.documentFonts(doc).toMutableList();mutableCopy.clear();assertEquals(expected,TypesetFonts.documentFonts(doc))
        assertContentEquals(before,documentJson.encodeToString(doc).toByteArray())
        assertEquals(emptyList(),TypesetFonts.documentFonts(TypesetDocument()))
        assertEquals(listOf("PresetBase"),TypesetFonts.documentFonts(TypesetDocument(presets=doc.presets)))
        for(text in listOf("[b]".repeat(1000)+"X","X".repeat(100001))) {
            val invalid=doc.copy(pages=listOf(doc.pages.single().copy(texts=listOf(TypesetText(text=text)))))
            val oldError=assertFails{RecordedDocumentFontDependencies.documentFonts(invalid)};val newError=assertFails{TypesetFonts.documentFonts(invalid)}
            assertEquals(oldError.javaClass,newError.javaClass);assertEquals(oldError.message,newError.message)
        }
        // Original helper reads preset base only, including malformed preset text; no admission claim.
        val invalidPreset=TypesetDocument(presets=listOf(doc.presets.single().copy(text=doc.presets.single().text.copy(text="X".repeat(100001)))))
        assertEquals(listOf("PresetBase"),TypesetFonts.documentFonts(invalidPreset));assertEquals(RecordedDocumentFontDependencies.documentFonts(invalidPreset),TypesetFonts.documentFonts(invalidPreset))
    }
    @Test fun simultaneousDependencyListsKeepOrderAndCallerDocumentIndependent() {
        val doc=TypesetDocument(pages=listOf(TypesetPage(assetId=UUID(36L,1L).toString(),name="Metadata only",texts=listOf(TypesetText(text="[font=Inline]中😀[/]A",style=TypesetStyle(fontFamily="Dialog"))))))
        val expected=RecordedDocumentFontDependencies.documentFonts(doc);val before=documentJson.encodeToString(doc).toByteArray();val pool=Executors.newFixedThreadPool(2)
        try {
            val futures=List(2){pool.submit{repeat(50){val actual=TypesetFonts.documentFonts(doc);assertEquals(expected,actual);assertNotSame(expected,actual);assertContentEquals(before,documentJson.encodeToString(doc).toByteArray())}}}
            futures.forEach{it.get(10,TimeUnit.SECONDS)}
        }finally{pool.shutdown();assertTrue(pool.awaitTermination(10,TimeUnit.SECONDS))}
    }
'''
assert t.endswith('}\n');t=t[:-2]+edges+'}\n'
for name,content in (('TypesetFonts.candidate.kt',new),('DocumentFontDependenciesPerformanceTest.final.kt',t)):
    with (local/name).open('x',encoding='utf-8',newline='\n') as f:f.write(content)
print('PREPARED_ONLY functionbody aggregation only;methods='+str(t.count('@Test fun'))+' productionNotWritten=true')
