from pathlib import Path
import hashlib,json
ROOT=Path('H:/CCB-Studio-Suite');OUT=Path(__file__).resolve().parent;ENTRY=OUT/'entry-correction'
assert OUT==ROOT/'.local/token-candidate-20261026' and not ENTRY.exists()
INSTALL=OUT/'isolated-install';verified=json.loads((OUT/'ISOLATED_INSTALL_VERIFICATION.json').read_text());assert verified['status']=='PASS'
def sha(p):
    with p.open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()
jar=next((INSTALL/'app').glob('studio-desktop-*.jar'))
ENTRY.mkdir()
lock=dict(appCode='26ef38857be510636dffdb5fcbd59a8d70c85550',buildPackageHead='0f11fdd6b6d281987dd7c449d623a74b8b74b8f0',
          mainJar=jar.name,mainJarSha256=sha(jar),nativeExeSha256=sha(INSTALL/'CCBStudioSuitePreview.exe'),installerSha256=verified['installerSha256'],
          gateMeaning='Only fixed package files and actual shortcut probe verified. Native/user acceptance NOT TESTED.')
(ENTRY/'PACKAGE_LOCK.json').write_text(json.dumps(lock,indent=2)+'\n',encoding='utf-8')
old=(ROOT/'.local/appearance-text-actions-20261026/entry-correction/Launch-Candidate.ps1').read_text()
old=old.replace('appearance-text-actions-20261026','token-candidate-20261026')
old=old.replace("$taskMode -notin @('PROBE','APP','REVIEW')","$taskMode -notin @('PROBE','READY')")
old=old.replace('studio-desktop-a667c3e9a048c3d6292aa2a6ab3d8a9.jar',jar.name)
old=old.replace('1154b2cf6f961cba3d0d20b9846f9ada72703adb4d73bb55c2759301ce15fa19',lock['mainJarSha256'])
old=old.replace('494f137e9fa8cb07b7f1d19ed6cc3e6dfac4afd3df16397390d30f1aee58541f',lock['nativeExeSha256'])
line=" if($taskMode -eq 'REVIEW' -and -not(Test-Path -LiteralPath (Join-Path $taskEntry 'APP_VERIFIED.json'))){throw 'Candidate app entry has not been verified'}"
assert line in old;old=old.replace(line,' # READY is package/probe verified only; native and user checks remain pending.')
old=old.replace("if($taskMode -eq 'REVIEW')","if($taskMode -eq 'READY')")
assert 'APP_VERIFIED' not in old and 'appearance-text-actions' not in old
(ENTRY/'Launch-Candidate.ps1').write_text(old,encoding='utf-8',newline='\n')
probe=(ROOT/'.local/appearance-text-actions-20261026/entry-correction/Probe.ps1').read_text().replace('appearance-text-actions-20261026','token-candidate-20261026')
(ENTRY/'Probe.ps1').write_text(probe,encoding='utf-8',newline='\n')
(ENTRY/'MODE.txt').write_text('PROBE',encoding='utf-8',newline='')
seed=(ROOT/'.local/appearance-text-actions-20261026/SeedTheme.java').read_text()
assert 'StudioThemeMode.LIGHT' in seed and '0xFFFFFF' in seed
seed=seed.replace('appearance-text-actions-20261026','token-candidate-20261026').replace('StudioThemeMode.LIGHT','StudioThemeMode.DARK').replace('0xFFFFFF','0x000000').replace('LIGHT FFFFFF','DARK 000000')
(OUT/'SeedTheme.java').write_text(seed,encoding='utf-8',newline='\n')
print(json.dumps(dict(status='ENTRY_PREPARED_NOT_LAUNCHED',jar=jar.name,mainJarSha256=lock['mainJarSha256'])))
