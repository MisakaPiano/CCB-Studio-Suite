from pathlib import Path
import datetime, hashlib, json, subprocess

ROOT=Path('H:/CCB-Studio-Suite')
OUT=ROOT/'.local/appearance-text-actions-20261026'
DEST=ROOT/'docs/audits/evidence/APPEARANCE_TEXT_CANDIDATE_20261026'
APP='48eed2aa35d1b2bcaa86f140ac33aac9cdc286ac'
PACKAGE='b54a036fc65868836d901339f7d1e3dce24dff8a'
assert subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip()==PACKAGE
assert not subprocess.check_output(['git','diff',APP,PACKAGE,'--','modules','tools'],cwd=ROOT)

def sha(p):
    with p.open('rb') as f: return hashlib.file_digest(f,'sha256').hexdigest()
def safe(p):
    assert p.is_relative_to(ROOT) and p!=ROOT
    for q in (p,*p.parents):
        if q==ROOT: break
        if q.exists(): assert not q.is_symlink() and not (q.stat().st_file_attributes & 1024),str(q)
    return p
def write(p,value):
    safe(p)
    with p.open('x',encoding='utf-8',newline='\n') as f:
        json.dump(value,f,ensure_ascii=False,indent=2); f.write('\n')
def copy(p,name):
    q=safe(DEST/name); q.parent.mkdir(parents=True,exist_ok=True)
    with q.open('xb') as f: f.write(p.read_bytes())

now=datetime.datetime.now(datetime.timezone.utc).isoformat()
images=[
('codex-clipboard-0937dbfd-210f-41c6-902d-716896782d83.png','LIGHT FFFFFF dialog'),
('codex-clipboard-dad83c93-df90-472e-9834-628860f95c4b.png','LIGHT 000000 dialog'),
('codex-clipboard-7281d8ea-e5f2-45fd-8f1a-2b95b698f854.png','LIGHT black main window'),
('codex-clipboard-06d7efbd-9afe-4deb-b878-6b6c24123563.png','LIGHT white main window'),
('codex-clipboard-30f367f6-3339-4d75-8d7a-cb132bf1d522.png','DARK FFFFFF dialog'),
('codex-clipboard-86a29031-4400-4f1a-aa4c-32a33e464573.png','DARK 000000 dialog'),
('codex-clipboard-82ac0afe-f835-487c-a126-d031d599eea5.png','DARK black main window; Tokens low contrast remains'),
('codex-clipboard-d88ab1ff-29ec-4498-8a1f-b2c8d0f9d2b1.png','DARK white main window'),
('codex-clipboard-75f2378c-5c51-400b-9c47-30865762a73e.png','DARK default blue dialog after restore'),
('codex-clipboard-49f32799-dad1-40cc-bbd9-c7cf01f0d941.png','DARK default blue main window after close')]
manual=dict(recordedUtc=now,appSource=APP,packageSource=PACKAGE,
    status='USER_REPORTED_PASS_WITH_SCOPE',
    question='四种配色下应用颜色都生效，关闭后能重新打开，最后恢复Studio默认也生效了吗？',
    answer='都正常',
    scope=['LIGHT/DARK x 000000/FFFFFF: Apply readable and effective','Close then gear reopen','Restore Studio default'],
    visibleResult='Three dialog text actions readable in all four supplied combinations; later screenshots show default blue and dialog closed',
    screenshots=[dict(name=n,sha256=sha(Path('C:/Users/1/AppData/Local/Temp')/n),observation=o) for n,o in images],
    retryAction='Error-only Reload is covered by previous fixed one-method automatic test; native error branch NOT TESTED; no corrupt fixture introduced',
    residual=dict(id='THEME-TOKENS-CONTRAST',status='OPEN / NOT FIXED / NOT IN TEXT-ACTION PATCH',observation='DARK + black accent makes creation-page positive/negative Tokens text dark; image82ac. Separate screenshot finding, not a denial of the three confirmed dialog actions.'),
    previousDelayedAnswer='稍后检查, then corrected by user: 我点错了，你让我继续检查吧; no timeout assumed acceptance',
    previous22aaAcceptance='Four original theme checklist items USER_REPORTED_PASS only for22aa; not repeated here',
    userImagePixelsPublished=False,independentReview='NOT REQUESTED',wholeProject='PRODUCT_HOLD / NOT RC')
write(OUT/'MANUAL_TEXT_ACTION_RESULT.json',manual)
identity=json.loads((OUT/'entry-correction/APP_IDENTITY_OBSERVATION.json').read_text())
assert identity['runtimeCreated'] and not identity['isolatedFallbackCreated'] and not identity['defaultPointerCreated']
write(OUT/'entry-correction/APP_VERIFIED.json',dict(status='APP_ENTRY_ISOLATION_PASS',recordedUtc=now,
    meaning='This gate validates the isolated app entry; it is not whole-product acceptance',appSource=APP,packageSource=PACKAGE,
    probe='PROBE_VERIFIED.json',identity='APP_IDENTITY_OBSERVATION.json',manual='../MANUAL_TEXT_ACTION_RESULT.json',
    nativeCapture='FAILED: FrameArrived timed out; helper cause UNKNOWN; no tool button clicks delivered',
    manualResult=manual['status'],remaining='Tokens screenshot finding OPEN; error-only Reload native branch NOT TESTED; other legacy pending unchanged'))
mode=OUT/'entry-correction/MODE.txt'; assert mode.read_text()=='APP'
safe(mode).write_text('REVIEW',encoding='utf-8',newline='')
receipt=json.loads((OUT/'candidate/BUILD_RECEIPT.json').read_text())
assert sha(OUT/'candidate/studio-installer.exe')==receipt['sha256']
assert receipt['sha256']=='ca4cafd37285a2528090247ecc53326b5e78cafa87b569d040578639d8c10ee5'
safe(DEST); DEST.mkdir()
copy(ROOT/'docs/audits/evidence/APPEARANCE_TEXT_ACTIONS_20261026/.gitattributes','.gitattributes')
for name in ('CANDIDATE_AUTHORIZATION.json','PACKAGE_COMMAND.json','package-text-candidate.py','package.log','ISOLATED_INSTALL_COMMAND.json','ISOLATED_INSTALL_VERIFICATION.json','isolated-install.log','verify-new-candidate.py','SeedTheme.failed-v1.java.txt','SeedTheme.java','seed-compile.log','seed-compile-fixed.log','SEED_RESULT.json','MANUAL_TEXT_ACTION_RESULT.json'):
    copy(OUT/name,'control/'+name)
for name in ('BUILD_RECEIPT.json','compile.log','installer.nsi'):
    copy(OUT/'candidate'/name,'package/'+name)
for name in ('INSTALL_MANIFEST.json','update-config.json','update-root.json'):
    copy(OUT/'isolated-install'/name,'installed/'+name)
copy(OUT/'isolated-install/updater/RUNTIME_MANIFEST.json','installed/RUNTIME_MANIFEST.json')
for p in sorted((OUT/'entry-correction').iterdir()):
    if p.is_file(): copy(p,'entry/'+p.name)
write(DEST/'SOURCE_PACKAGE_MAP.json',dict(appBuildSource=APP,packageSource=PACKAGE,modulesToolsRelationship='git diff48eed..b54 -- modules tools empty; no new Gradle rerun',
    originalInputs='https://github.com/MisakaPiano/CCB-Studio-Suite/blob/'+PACKAGE+'/docs/audits/evidence/APPEARANCE_TEXT_ACTIONS_20261026/SOURCE_APP_IMAGE_MAP.json',
    inputCount=497,mainJar='studio-desktop-a667c3e9a048c3d6292aa2a6ab3d8a9.jar',mainJarSha256='1154b2cf6f961cba3d0d20b9846f9ada72703adb4d73bb55c2759301ce15fa19',
    installer=dict(path=str(OUT/'candidate/studio-installer.exe'),bytes=receipt['bytes'],sha256=receipt['sha256'],displayVersion=receipt['displayVersion'],channel='internal'),
    originalJUnit='One unique method at48eed in previous fixed bundle; no rerun or added test count in this package stage',
    buildReceipt='package/BUILD_RECEIPT.json',installation='control/ISOLATED_INSTALL_VERIFICATION.json',entry='entry/APP_VERIFIED.json',userResult='control/MANUAL_TEXT_ACTION_RESULT.json',
    release='NOT PUBLISHED / no new tag',dailyInstallation='UNCHANGED',oldEvidence='Preserved original22aa and48eed source-stage bundles without edits'))
copy(Path(__file__),'reproduction/freeze-candidate.py')
files=[dict(path=p.relative_to(DEST).as_posix(),bytes=p.stat().st_size,sha256=sha(p)) for p in sorted(DEST.rglob('*')) if p.is_file()]
write(DEST/'RAW_MANIFEST.json',dict(appSource=APP,packageSource=PACKAGE,files=files,membersIncludingManifest=len(files)+1,
    exclusions='Binaries, shortcut binary, screenshot pixels, workspace, vault, user data and credentials. Only named synthetic/control records copied.'))
print(json.dumps(dict(status='FROZEN',files=len(files)+1,manual=manual['status'],residual='THEME-TOKENS-CONTRAST OPEN',source=APP,package=PACKAGE)))
