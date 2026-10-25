from pathlib import Path
import subprocess
root=Path('H:/CCB-Studio-Suite');test=root/'modules/studio-core/src/test/kotlin/io/github/misakapiano/ccbstudio/core/TagSuggestionsPerformanceTest.kt'
path='modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/LibraryTagIndex.kt'
old=subprocess.check_output(['git','show','f996e4ed48529af204ab6e6be9ef0ff5cf19c4bd:'+path],cwd=root).decode()
legacy=old[old.index('    fun suggestions('):old.index('\n}\nfun matchesLibraryTags(')].replace('    fun suggestions(text:String):List<String> =','private fun legacySuggestions(counts:Map<String,Int>,text:String):List<String> =',1)
s=test.read_text(encoding='utf-8');assert 'legacySuggestions' not in s
s=s.replace('/** Management-label suggestion',legacy+'\n\n/** Management-label suggestion',1)
s=s.replace('repeat(2){verify(index.suggestions(query))}','repeat(2){verify(legacySuggestions(index.counts,query));verify(index.suggestions(query))}',1)
s=s.replace('repeat(4){round->','repeat(4){round->\n              for(kind in if(round%2==0)listOf("legacy","current")else listOf("current","legacy")) {',1)
s=s.replace('val actual=index.suggestions(query)','val actual=if(kind=="legacy")legacySuggestions(index.counts,query)else index.suggestions(query)',1)
s=s.replace('rows+=buildJsonObject{put("assets",count);','rows+=buildJsonObject{put("kind",kind);put("assets",count);',1)
s=s.replace('\n            }\n        }\n        println("TAG_SUGGESTIONS_ALLOCATION="','\n              }\n            }\n        }\n        println("TAG_SUGGESTIONS_ALLOCATION="',1)
assert s.endswith('}\n');s=s[:-2]+(root/'.local/perf003-tag-suggestions-20261025/extra-methods.txt').read_text(encoding='utf-8')+'}\n'
test.write_text(s,encoding='utf-8',newline='\n')
