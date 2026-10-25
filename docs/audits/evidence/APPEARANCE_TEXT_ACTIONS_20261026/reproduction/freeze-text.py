from pathlib import Path
import hashlib,json,subprocess,zipfile
ROOT=Path('H:/CCB-Studio-Suite');OUT=ROOT/'.local/appearance-text-actions-20261026';SOURCE='48eed2aa35d1b2bcaa86f140ac33aac9cdc286ac'
DEST=ROOT/'docs/audits/evidence/APPEARANCE_TEXT_ACTIONS_20261026';DEST.mkdir()
def sha(p):
 with p.open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()
def write(p,v):
 with p.open('x',encoding='utf-8') as f:json.dump(v,f,indent=2);f.write('\n')
def copy(p,name):
 q=DEST/name;q.parent.mkdir(parents=True,exist_ok=True);q.write_bytes(p.read_bytes())
build=ROOT/'.local/windows-preview/evidence/20261025T215119192236Z-appearance-text-actions-20261026'
inputs=json.loads((build/'source-before.json').read_text());mapped=[]
for name,digest in inputs.items():
 p=ROOT/name;assert sha(p)==digest,name
 raw=p.read_bytes();git=subprocess.check_output(['git','show',SOURCE+':'+name]);assert raw.replace(b'\r\n',b'\n')==git.replace(b'\r\n',b'\n'),name
 mapped.append(dict(path=name,buildSha256=digest,gitSha256=hashlib.sha256(git).hexdigest(),relationship='EXACT' if raw==git else 'CRLF_LF_ONLY'))
app=ROOT/'modules/studio-desktop/build/compose/binaries/main/app/CCBStudioSuitePreview';jars=list((app/'app').glob('studio-desktop-*.jar'));assert len(jars)==1
classes=ROOT/'modules/studio-desktop/build/classes/kotlin/main';matching=[]
with zipfile.ZipFile(jars[0]) as z:
 for p in classes.rglob('StudioAppearanceKt*.class'):
  rel=p.relative_to(classes).as_posix();assert p.read_bytes()==z.read(rel);matching.append(dict(path=rel,sha256=sha(p)))
 assert matching and not any('StudioAppearanceTextActionTest' in n for n in z.namelist())
write(DEST/'SOURCE_APP_IMAGE_MAP.json',dict(source=SOURCE,baseManualCandidate='22aa8c55ec4b05db29acb443d85717e80eb9d24a',inputs=mapped,appImage=str(app),mainJar=jars[0].name,mainJarSha256=sha(jars[0]),matchingAppearanceClasses=matching,appImageFiles=[dict(path=p.relative_to(app).as_posix(),bytes=p.stat().st_size,sha256=sha(p)) for p in sorted(app.rglob('*')) if p.is_file()],nsisInstaller='NOT BUILT for this text-only increment',userRetest='NOT REQUESTED / NOT TESTED',independentReview='NOT REQUESTED',currentUserApp='Old validated22aa candidate retained and not replaced',publicRelease=False))
copy(ROOT/'handoff/evidence/THEME_P2_PROJECT_REVIEW_20261026/.gitattributes','.gitattributes')
for p in build.iterdir():
 if p.is_file():copy(p,'run/'+p.name)
for name in ('run-text-actions.py','PLAN.json','COMMAND.json','RUNNER_IDENTITY.json','runner.log','memory-samples.jsonl','MONITOR_RESULT.json'):copy(OUT/name,'control/'+name)
write(DEST/'FINDING.json',dict(id='THEME-TEXT-ACTION-CONTRAST',sourceImage='codex-clipboard-b8079402-db48-4547-b17c-13d65022f1ed.png',oldSource='22aa8c55ec4b05db29acb443d85717e80eb9d24a',observed='White accent on light dialog caused unreadable Apply/Reset/Close text; retry same inherited default',fix='Four theme-dialog TextButtons use existing derived selectionInk. Raw primary/RGB/format and original callbacks unchanged. No generic72 TextButton migration.',automated='1 unique method actual production components, pixels>=4.5 readable foreground on both modes and black/white accents, all4click callbacks, original primary preserved',scope='Source/offscreen consumer only; no new NSIS/native/user/Project acceptance claimed',manualStatement='User prior four theme checklist all passed retained for22aa source; this screenshot finding separately fixed',userImagesPublished=False))
copy(Path(__file__),'reproduction/freeze-text.py')
files=[dict(path=p.relative_to(DEST).as_posix(),bytes=p.stat().st_size,sha256=sha(p)) for p in sorted(DEST.rglob('*')) if p.is_file()]
write(DEST/'RAW_MANIFEST.json',dict(source=SOURCE,files=files,membersIncludingManifest=len(files)+1,bytesExcludingManifest=sum(x['bytes'] for x in files),excluded='User screenshot bytes, binaries, private workspace, vault, credentials.'))
print(json.dumps(dict(status='FROZEN',source=SOURCE,inputs=len(mapped),appearanceClasses=len(matching),files=len(files)+1,appMainJarSha256=sha(jars[0]))),flush=True)