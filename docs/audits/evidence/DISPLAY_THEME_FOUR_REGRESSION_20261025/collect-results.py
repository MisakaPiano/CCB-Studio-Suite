from pathlib import Path
import json,hashlib,datetime,subprocess,xml.etree.ElementTree as ET

ROOT=Path('H:/CCB-Studio-Suite')
OUT=ROOT/'docs/audits/evidence/DISPLAY_THEME_FOUR_REGRESSION_20261025'
SOURCE='30b065915e376d75cc6439fac0ba55925c73f735'
INITIAL='42146d3705f5cbc9ed392735f9c4727f8a8f67d7'
EXPECTED={
 'damagedLatestAppearanceBlocksWritesAndRetainsItsRevisionAndOriginal',
 'strictSettingsRejectFutureMalformedAndOutOfRangeValuesInsteadOfReplacingThem',
 'modesAndColorStylesHaveReadableForegroundsWithoutChangingTheExistingDefault',
 'saveUsesLatestMetadataAndReopensWithoutChangingOriginalImageBytes',
}

def git(*args):
    return subprocess.check_output(['git',*args],cwd=ROOT)

def load(path):
    return json.loads(path.read_text(encoding='utf-8-sig'))

def guarded(path):
    path.relative_to(ROOT)
    current=path
    while current!=ROOT:
        if current.exists() and getattr(current.lstat(),'st_file_attributes',0)&0x400:
            raise RuntimeError('Reparse input/output')
        current=current.parent
    return path

def copy(source,target):
    guarded(source);guarded(target)
    assert source.is_file()
    with target.open('xb') as stream:stream.write(source.read_bytes())

def write(name,value):
    with guarded(OUT/name).open('x',encoding='utf8',newline='') as stream:
        json.dump(value,stream,indent=2);stream.write('\n')

assert git('rev-parse','HEAD').decode().strip()==SOURCE
assert not git('status','--porcelain').decode().strip()
for attempt in ('b','c'):
    for area in ('control','run'):
        assert not list((OUT/f'attempt-{attempt}'/area).iterdir())

runs={}
for attempt,phase in [('b','20261025T125230731187Z-display-theme-resume-b-20261025'),
                      ('c','20261025T125548826733Z-display-theme-resume-c-20261025')]:
    folder=ROOT/'.local/windows-preview/evidence'/phase
    summary=load(folder/'summary.json');cases=load(folder/'test-cases.json')
    fixture=load(folder/'fixture-cleanup.json')
    assert summary['sourceStable'] is True
    assert summary['arguments'][-3:]==[':studio-desktop:test','--tests','*StudioAppearanceTest']
    assert fixture['status']=='EXPLICITLY_RETAINED'
    fixture_root=guarded(Path(fixture['root']))
    fixture_root.relative_to(ROOT/'.local/disposable')
    assert fixture_root.is_dir()
    if attempt=='b':
        assert summary['exitCode']==1 and summary['status']=='FAILED' and summary['tests']==0 and cases==[]
        assert summary['baseCommit']=='4bce9ad0ed81e7d2401a352805b5bebe7e9ab25e'
        log=(folder/'gradle.log').read_text(encoding='utf8')
        assert 'warningelse' in log and 'accentelse' in log
    else:
        assert summary['baseCommit']==SOURCE and summary['exitCode']==0 and summary['status']=='PASS'
        assert summary['tests']==4 and summary['failures']==summary['errors']==summary['skipped']==0
        assert len(cases)==4 and {row['name'] for row in cases}==EXPECTED
        assert all(row['status']=='PASS' for row in cases)
        xmls=list(folder.glob('studio-desktop-TEST-*.xml'));assert len(xmls)==1
        suite=ET.parse(xmls[0]).getroot()
        assert int(suite.attrib['tests'])==4 and {c.attrib['name'] for c in suite.findall('testcase')}==EXPECTED
    for file in folder.iterdir():
        if file.is_file():copy(file,OUT/f'attempt-{attempt}'/'run'/file.name)
    control=ROOT/f'.local/studio-display-theme-resume-{attempt}-20261025'
    for name in ('PLAN.json','preflight.json','run-gate.ps1','gate-status.json','gate-observations.jsonl','runner.log'):
        copy(control/name,OUT/f'attempt-{attempt}'/'control'/name)
    gate=load(control/'gate-status.json')
    assert gate['runnerInvoked'] is True and gate['details']['exitCode']==summary['exitCode']
    runs[attempt]={'phase':phase,'applicationSource':INITIAL if attempt=='b' else SOURCE,
        'actualExecutionCommit':summary['baseCommit'],'outcome':'COMPILE FAILED BEFORE JUNIT' if attempt=='b' else 'FOUR UNIQUE METHODS PASS',
        'startedUtc':summary['startedUtc'],'secondsIncludingBuild':summary['seconds'],
        'actualMethods':summary['tests'],'cases':cases,'fixtures':fixture,'autoCleanup':'DISABLED / EXPLICITLY_RETAINED'}

prefix='modules/studio-desktop/src/main/kotlin/io/github/misakapiano/ccbstudio/desktop/'
changes={prefix+'BatchWorkspace.kt':('warningelse','warning else'),prefix+'TypesetQueueWorkspace.kt':('accentelse','accent else')}
assert set(git('diff','--name-only',INITIAL,SOURCE,'--','modules','tools').decode().splitlines())==set(changes)
for name,(old,new) in changes.items():
    before=git('show',INITIAL+':'+name);after=git('show',SOURCE+':'+name)
    assert before.count(old.encode())==1 and after==before.replace(old.encode(),new.encode(),1)
test='modules/studio-desktop/src/test/kotlin/io/github/misakapiano/ccbstudio/desktop/StudioAppearanceTest.kt'
assert git('show',INITIAL+':'+test)==git('show',SOURCE+':'+test)
snapshot=load(ROOT/'.local/windows-preview/evidence'/runs['c']['phase']/'source-before.json')
assert snapshot[test]==hashlib.sha256(git('show',SOURCE+':'+test)).hexdigest()

files=git('diff','--name-only','db46098445d1c1064ee77104fe320268cc6d0f93',SOURCE,'--','modules','tools').decode().splitlines()
source_map={'applicationSource':SOURCE,'initialThemeSource':INITIAL,'beforeThemePerformanceSource':'db46098445d1c1064ee77104fe320268cc6d0f93',
 'onlyChangeAfterInitialTheme':'Exactly one whitespace separator in each of two if/else palette expressions',
 'testSourceUnchangedFromInitial':True,'executedTestSourceSha256':snapshot[test],
 'inputMapping':'Raw source-before snapshots are working-byte SHA256. Git blob SHA256 below is separately labeled; line-ending normalization may differ. sourceStable is the runner result, not a fabricated source-after file.',
 'files':[], 'packageVersion':None,'packageSha256':None,'packageStatus':'NOT BUILT / NOT VERIFIED',
 'releaseStatus':'UNCHANGED; no version, tag, signing or update metadata change',
 'realProvider':'NOT TESTED / NOT CALLED','humanAcceptance':'NOT TESTED','independentReview':'NOT REQUESTED',
 'environment':'Windows/PowerShell; Gradle 9.1.0 wrapper; JDK17 configured in build, exact runtime version not separately captured; raw arguments record offline strict dependencies, low priority, one worker, in-process compiler. No extra JVM invocation to collect environment.'}
for name in files:
    raw=git('show',SOURCE+':'+name)
    source_map['files'].append({'path':name,'gitBlobSha256':hashlib.sha256(raw).hexdigest(),'executedWorkingByteSha256':snapshot.get(name)})
write('SOURCE_MAP.json',source_map)
write('RUN_MATRIX.json',{'recordedUtc':datetime.datetime.now(datetime.timezone.utc).isoformat(),
 'authorization':'Human 2026-10-25 12:46 UTC (21:46 JST) agreement relayed with transcript by original coordinator; original four tests only after fresh resource admission.',
 'priorA':{'fixedEvidenceCommit':'4bce9ad0ed81e7d2401a352805b5bebe7e9ab25e','path':'docs/audits/evidence/DISPLAY_THEME_RESOURCE_GATE_20261025_A','outcome':'RESOURCE TIMEOUT; RUNNER NOT INVOKED; 0 METHODS','changed':False},
 'attempts':runs,'finalUniqueMethods':4,'counting':'B compile failed before tests; C four unique methods only. Do not add variants, XML rows, old baselines or zero-test resource attempts.',
 'actualNativeUiOrPackageAcceptance':'NOT TESTED','protectionCase':'UPD-CLEAN-PROTECTION-001 remains REGISTERED / STATIC ONLY; not executed',
 'noActions':'No package, release, install, real archival/move/delete, automatic cleanup, real credentials or private data access. B/C synthetic fixture roots retained.'})
with (OUT/'LEXICAL_FIX.patch').open('xb') as stream:stream.write(git('diff','--no-ext-diff',INITIAL,SOURCE,'--',*changes))
copy(Path(__file__),OUT/'collect-results.py')
with (OUT/'.gitattributes').open('x',encoding='utf8',newline='') as stream:stream.write('* -text\n**/* -text\n')
write('MANIFEST.json',{'files':[{'path':p.relative_to(OUT).as_posix(),'bytes':p.stat().st_size,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()} for p in sorted(OUT.rglob('*')) if p.is_file()],
 'basis':'Exact task-owned raw copies, explicit input/source mapping, no user data or fixture payloads included.'})

for attempt in ('b','c'):
    p=ROOT/f'.local/studio-display-theme-resume-{attempt}-20261025/CONTINUATION.json';s=load(p)
    s['updatedUtc']=datetime.datetime.now(datetime.timezone.utc).isoformat();s['activeExecSessionId']=None
    s['status']='COMPILE FAILED / actualJUnit0 / retained' if attempt=='b' else 'FINAL FOUR UNIQUE METHODS PASS / 0 failures errors skipped / retained fixtures'
    s['evidence']='docs/audits/evidence/DISPLAY_THEME_FOUR_REGRESSION_20261025'
    s['nextAction']='Current authorization complete after evidence/docs; no rerun of passing four, no package/release/install/cleanup/protection case or additional features. UI/user/internal package remain unverified.'
    p.write_text(json.dumps(s,indent=2)+'\n',encoding='utf8',newline='')
p=ROOT/'.local/studio-display-theme-20261025/CONTINUATION.json';s=load(p)
s['updatedUtc']=datetime.datetime.now(datetime.timezone.utc).isoformat();s['activeExecSessionId']=None
s['previousApplicationSource']=s.get('applicationSource');s['applicationSource']=SOURCE;s['status']='Four original theme JVM methods PASS on corrected source; original A deferral and B compile failure preserved; current scope stops before package/UI acceptance'
s['actualMethodsExecuted']=4;s['evidence']='docs/audits/evidence/DISPLAY_THEME_FOUR_REGRESSION_20261025';s['nextAction']='No active Studio job; do not repeat four methods or expand current scope. No packaging/install/release/automatic cleanup/real data. Original queue remains, current 12:46 UTC permission only four regressions.'
p.write_text(json.dumps(s,indent=2)+'\n',encoding='utf8',newline='')
print(json.dumps({'finalUniqueMethods':4,'bActualMethods':0,'rawFiles':len(list(OUT.rglob('*'))),'source':SOURCE,'fixtureCleanup':'EXPLICITLY RETAINED'}))
