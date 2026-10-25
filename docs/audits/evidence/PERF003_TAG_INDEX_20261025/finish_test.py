from pathlib import Path
import subprocess
root=Path('H:/CCB-Studio-Suite');test=root/'modules/studio-core/src/test/kotlin/io/github/misakapiano/ccbstudio/core/LibraryTagIndexPerformanceTest.kt'
path='modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/LibraryTagIndex.kt'
old=subprocess.check_output(['git','show','c82b463ba06f00f0251458f724fcc924622b1b35:'+path],cwd=root).decode()
legacy=old[old.index('class LibraryTagIndex('):old.index('\nfun matchesLibraryTags(')].replace('class LibraryTagIndex(','private class LegacyLibraryTagIndex(',1)
s=test.read_text(encoding='utf-8');assert 'LegacyLibraryTagIndex' not in s
s=s.replace('/** Scoped management',legacy+'\n\n/** Scoped management',1)
s=s.replace('fun verify(index:LibraryTagIndex) {\n                assertEquals(count+1,index.assetCount);assertEquals(expected,index.counts)\n                assertEquals((0 until 64).map{"tag$it"},index.counts.keys.toList())','fun verify(assetCount:Int,counts:Map<String,Int>) {\n                assertEquals(count+1,assetCount);assertEquals(expected,counts)\n                assertEquals((0 until 64).map{"tag$it"},counts.keys.toList())',1)
s=s.replace('verify(LibraryTagIndex(scope,labels))','repeat(2){val legacy=LegacyLibraryTagIndex(scope,labels);verify(legacy.assetCount,legacy.counts);val current=LibraryTagIndex(scope,labels);verify(current.assetCount,current.counts)}',1)
s=s.replace('repeat(4){round->','repeat(4){round->\n              for(kind in if(round%2==0)listOf("legacy","current")else listOf("current","legacy")) {',1)
s=s.replace('val actual=LibraryTagIndex(scope,labels)','val actual=if(kind=="legacy")LegacyLibraryTagIndex(scope,labels)else LibraryTagIndex(scope,labels)',1)
s=s.replace('verify(actual)','when(actual){is LegacyLibraryTagIndex->verify(actual.assetCount,actual.counts);is LibraryTagIndex->verify(actual.assetCount,actual.counts);else->error("Unknown measurement mode")}',1)
s=s.replace('rows+=buildJsonObject {put("uniqueLabeledAssets",count);','rows+=buildJsonObject {put("kind",kind);put("uniqueLabeledAssets",count);',1)
s=s.replace('\n            }\n        }\n        println("TAG_INDEX_ALLOCATION="','\n              }\n            }\n        }\n        println("TAG_INDEX_ALLOCATION="',1)
assert s.endswith('}\n');s=s[:-2]+(root/'.local/perf003-tag-index-20261025/extra-methods.txt').read_text(encoding='utf-8')+'}\n'
test.write_text(s,encoding='utf-8',newline='\n')
