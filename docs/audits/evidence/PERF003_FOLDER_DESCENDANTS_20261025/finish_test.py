from pathlib import Path
import subprocess
root=Path('H:/CCB-Studio-Suite');test=root/'modules/studio-core/src/test/kotlin/io/github/misakapiano/ccbstudio/core/OrganizationDescendantsPerformanceTest.kt'
path='modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/GenerationOrganization.kt'
old=subprocess.check_output(['git','show','e3989bc1f5e5f4a99caffee7419e4bb95d8f8b83:'+path],cwd=root).decode()
line=next(x for x in old.splitlines() if x.startswith('    fun descendants('))
legacy=line.replace('    fun descendants(','private fun GenerationOrganization.legacyDescendants(',1).replace('flatMap{descendants(it.id)}','flatMap{legacyDescendants(it.id)}',1)
s=test.read_text(encoding='utf-8');assert 'legacyDescendants' not in s
s=s.replace('/** Organization node',legacy+'\n\n/** Organization node',1)
s=s.replace('val expectedOrder=org.descendants(target).toList()','val expectedOrder=org.legacyDescendants(target).toList()',1)
s=s.replace('repeat(2){verify(org.descendants(target))}','repeat(2){verify(org.legacyDescendants(target));verify(org.descendants(target))}',1)
s=s.replace('repeat(4){round->','repeat(4){round->\n              for(kind in if(round%2==0)listOf("legacy","current")else listOf("current","legacy")) {',1)
s=s.replace('val actual=org.descendants(target)','val actual=if(kind=="legacy")org.legacyDescendants(target)else org.descendants(target)',1)
s=s.replace('rows+=buildJsonObject{put("context",context);','rows+=buildJsonObject{put("kind",kind);put("context",context);',1)
s=s.replace('\n            }\n          }\n        }\n        println("FOLDER_DESCENDANTS_ALLOCATION="','\n              }\n            }\n          }\n        }\n        println("FOLDER_DESCENDANTS_ALLOCATION="',1)
assert s.endswith('}\n');s=s[:-2]+(root/'.local/perf003-folder-descendants-20261025/extra-methods.txt').read_text(encoding='utf-8')+'}\n'
test.write_text(s,encoding='utf-8',newline='\n')
