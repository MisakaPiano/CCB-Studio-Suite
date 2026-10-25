from pathlib import Path
import subprocess
root=Path('H:/CCB-Studio-Suite');test=root/'modules/studio-core/src/test/kotlin/io/github/misakapiano/ccbstudio/core/LibraryRecordedTimesPerformanceTest.kt'
path='modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/LibraryCatalogProjection.kt'
old=subprocess.check_output(['git','show','e66aeaeecbca6a6ccbc9b4ec5e70a97a4dde8945:'+path],cwd=root).decode()
legacy=old[old.index('fun libraryRecordedTimes('):old.index('\nfun libraryDateMatches(')].replace('fun libraryRecordedTimes(','private fun legacyRecordedTimes(',1)
s=test.read_text(encoding='utf-8');assert 'legacyRecordedTimes' not in s
s=s.replace('/** Catalog metadata',legacy+'\n\n/** Catalog metadata',1)
s=s.replace('verify(libraryRecordedTimes(assets,emptyList()))','repeat(2){verify(legacyRecordedTimes(assets,emptyList()));verify(libraryRecordedTimes(assets,emptyList()))}',1)
s=s.replace('repeat(4){round->','repeat(4){round->\n              for(kind in if(round%2==0)listOf("legacy","current")else listOf("current","legacy")) {',1)
s=s.replace('val actual=libraryRecordedTimes(assets,emptyList())','val actual=if(kind=="legacy")legacyRecordedTimes(assets,emptyList())else libraryRecordedTimes(assets,emptyList())',1)
s=s.replace('rows+=buildJsonObject {put("assets",count);','rows+=buildJsonObject {put("kind",kind);put("assets",count);',1)
s=s.replace('\n            }\n        }\n        println("RECORDED_TIMES_ALLOCATION="','\n              }\n            }\n        }\n        println("RECORDED_TIMES_ALLOCATION="',1)
assert s.endswith('}\n');s=s[:-2]+(root/'.local/perf003-recorded-times-20261025/extra-methods.txt').read_text(encoding='utf-8')+'}\n'
test.write_text(s,encoding='utf-8',newline='\n')
