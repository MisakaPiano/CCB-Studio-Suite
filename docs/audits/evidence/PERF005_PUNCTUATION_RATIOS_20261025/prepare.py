from pathlib import Path
import re,subprocess
root=Path('H:/CCB-Studio-Suite');local=root/'.local/perf005-punctuation-ratios-20261025'
base='838b087a08a7f81eb62d2bef2f2f5f819433e2f8'
prod='modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/TypesetLayout.kt'
prior='modules/studio-core/src/test/kotlin/io/github/misakapiano/ccbstudio/core/VerticalPunctuationClassPerformanceTest.kt'
def git(*args):return subprocess.check_output(['git',*args],cwd=root)
assert git('rev-parse','HEAD').decode().strip()==base and git('status','--porcelain')==b''
old=git('show',base+':'+prod).decode();raw=(local/'TypesetLayout.before.kt').read_bytes();assert raw==(root/prod).read_bytes() and raw.replace(b'\r\n',b'\n')==old.encode()
assert git('show','3d9abb0e10a61a230e17542a8cc22b5c1c0992e3:'+prod).decode()==old
entries=[('compactPunctuationRatios','doubleArrayOf(.20,.18,.28,.10,.10,.28,.22,.08)'),('loosePunctuationRatios','doubleArrayOf(.08,.06,.12,.04,.04,.12,.10,.02)'),('standardPunctuationRatios','doubleArrayOf(.14,.12,.20,.07,.07,.20,.16,.05)')]
new=old
for name,expr in entries:assert old.count(expr)==1;new=new.replace(expr,name,1)
new=new.replace('    private fun reduction(',''.join('    private val '+name+' by lazy { '+expr+' }\n' for name,expr in entries)+'    private fun reduction(',1)
chars=next(x for x in old.splitlines() if 'private fun chars(' in x)
subset=old[old.index('    private val smallPunctuation '):old.index('    data class StoryLayout(')]
subset=re.sub(r'\b(Token|Cell|Line|Layout)\b',r'TypesetLayout.\1',subset);subset=re.sub(r'\b(tokens|verticalTokens|measure)\(',r'TypesetLayout.\1(',subset)
reference='private object RecordedVerticalRatioLayout {\n'+chars+'\n'+subset+'}\n'
t=git('show',base+':'+prior).decode();begin=t.index('private object RecordedVerticalPunctuationLayout {');end=t.index('\n/** Actual vertical layout')
t=t[:begin]+reference+t[end:]
t=t.replace('RecordedVerticalPunctuationLayout','RecordedVerticalRatioLayout').replace('class VerticalPunctuationClassPerformanceTest','class VerticalPunctuationRatioPerformanceTest').replace('measuredVerticalLayoutKeepsClassesAdvancesWrappingAndDocument','measuredVerticalRatiosKeepCompleteLayoutAndAdmittedDocument').replace('punctuationOrderTightnessStylesWrappingAndDisabledControlKeepFullLayout','ratioTablesStylesTightnessWrappingAndFallbackKeepCompleteLayout').replace('simultaneousVerticalLayoutsKeepIndependentCellsAndLineState','simultaneousRatioProfilesKeepTablesAndLineStateIndependent').replace('VERTICAL_PUNCTUATION_CLASSES=','VERTICAL_PUNCTUATION_RATIOS=')
t=t.replace('for(count in listOf(128,1024,8192))for(enabled in listOf(false,true)) {','for(count in listOf(128,1024,8192))for(profile in listOf("disabled","standard","compact","loose")) {\n            val enabled=profile!="disabled";val tightness=if(enabled)profile else "standard"')
t=t.replace('vertical=true,punctuationOptimization=enabled)','vertical=true,punctuationOptimization=enabled,punctuationTightness=tightness)',1)
t=t.replace('put("kind",kind);put("enabled",enabled);','put("kind",kind);put("profile",profile);put("enabled",enabled);',1)
extra=r'''
        for((tightness,advances) in listOf("standard" to listOf(12.0,10.6,10.8),"compact" to listOf(12.0,10.0,10.2),"loose" to listOf(12.0,11.2,11.4),"unknown-helper" to listOf(12.0,10.6,10.8))) {
            val item=TypesetText(text="A，中",style=style.copy(fontSize=10.0),width=1000.0,vertical=true,punctuationTightness=tightness)
            val actual=TypesetLayout.layout(item);assertEquals(RecordedVerticalRatioLayout.layout(item),actual)
            advances.zip(actual.lines.single().cells).forEach{(expected,cell)->assertEquals(expected,cell.advance,1e-9)}
        }
'''
marker='        val disabled=TypesetLayout.layout(';assert t.count(marker)==1;t=t.replace(marker,extra+marker,1)
begin=t.index('        assertTrue(item.vertical);val expected=RecordedVerticalRatioLayout.layout(item);val pool=')
end=t.index('\n        }finally{pool.shutdown();assertTrue(pool.awaitTermination(10,TimeUnit.SECONDS))}',begin)
t=t[:begin]+'''        assertTrue(item.vertical);val profiles=listOf("standard","compact","loose")
        val expected=profiles.associateWith{RecordedVerticalRatioLayout.layout(item.copy(punctuationTightness=it))};val pool=Executors.newFixedThreadPool(2)
        try {
            val futures=List(2){worker->pool.submit{repeat(50){round->val profile=profiles[(round+worker)%profiles.size];val actual=TypesetLayout.layout(item.copy(punctuationTightness=profile));assertEquals(expected.getValue(profile),actual);assertNotSame(expected.getValue(profile).lines,actual.lines)}}}
            futures.forEach{it.get(10,TimeUnit.SECONDS)}'''+t[end:]
for name,content in (('TypesetLayout.candidate.kt',new),('VerticalPunctuationRatioPerformanceTest.kt',t)):
    with (local/name).open('x',encoding='utf-8',newline='\n') as f:f.write(content)
print('PREPARED_ONLY fields3/uses3 fulloldreference exact;newmethods='+str(t.count('@Test fun'))+' precedingbaseline3d9 reused,no newbaselineexecution')
