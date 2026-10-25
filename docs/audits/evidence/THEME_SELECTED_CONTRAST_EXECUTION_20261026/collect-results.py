"""Local-only exact evidence freeze. No push, external message, archive or fixture payload."""
from pathlib import Path
import datetime,hashlib,json,subprocess,xml.etree.ElementTree as ET
ROOT=Path('H:/CCB-Studio-Suite');OUT=Path(__file__).resolve().parent
SOURCE='c39e860252fdd96c9bec5b5d42d1abd988d3ec06'
assert Path.cwd().resolve()==ROOT.resolve() and OUT.parent==ROOT/'.local'
def emit(name,v):
    target=OUT/name;target.parent.mkdir(parents=True,exist_ok=True)
    with target.open('x',encoding='utf-8',newline='\n') as f:json.dump(v,f,indent=2,ensure_ascii=False);f.write('\n')
def copy(source,name):
    data=source.read_bytes();target=OUT/name;target.parent.mkdir(parents=True,exist_ok=True)
    with target.open('xb') as f:f.write(data)
def gitblob(ref,path):return subprocess.check_output(['git','show',ref+':'+path],cwd=ROOT)
assert not subprocess.check_output(['git','diff','--name-only',SOURCE,'HEAD','--','modules','tools']).strip()
assert not subprocess.check_output(['git','status','--porcelain','--','modules','tools']).strip()
phases=list((ROOT/'.local/windows-preview/evidence').glob('*-display-theme-selected-p2-lowmem-f-20261026'));assert len(phases)==1
run=phases[0];summary=json.loads((run/'summary.json').read_text());cases=json.loads((run/'test-cases.json').read_text(encoding='utf-8'))
assert summary['status']=='PASS' and summary['sourceStable'] and summary['tests']==3
assert summary['failures']==summary['errors']==summary['skipped']==0
assert len(cases)==3 and all(x['status']=='PASS' and x['suite'].endswith('.StudioSelectedContrastTest') for x in cases)
xmls=list(run.glob('*TEST-*.xml'));assert len(xmls)==1
xml=ET.parse(xmls[0]).getroot();assert int(xml.attrib['tests'])==3
assert {x.get('name') for x in xml.findall('testcase')}=={x['name'] for x in cases}
for attempt in ['e','f']:
    folder=ROOT/f'.local/theme-selected-contrast-lowmem-{attempt}-20261026'
    for p in folder.iterdir():
        assert p.is_file() and not p.is_symlink()
        copy(p,f'attempt-{attempt}/control/{p.name}')
for p in run.iterdir():
    assert p.is_file() and not p.is_symlink()
    copy(p,'attempt-f/run/'+p.name)
before=json.loads((run/'source-before.json').read_text())
prepared=json.loads((ROOT/'docs/audits/evidence/THEME_SELECTED_CONTRAST_P2_20261025/SOURCE_MAP.json').read_text())
mapping=[]
for x in prepared['files']:
    path=x['path'];blob=gitblob(SOURCE,path);working=(ROOT/path).read_bytes();digest=hashlib.sha256(working).hexdigest()
    assert hashlib.sha256(blob).hexdigest()==x['gitBlobSha256']
    assert digest==x['preparedWorkingSha256']
    if path in before:assert before[path]==digest
    mapping.append(dict(path=path,gitBlobSha256=hashlib.sha256(blob).hexdigest(),workingSha256=digest,
        executedRunnerBeforeSha256=before.get(path),runnerCoverage='before snapshot and sourceStable' if path in before else 'unchanged Git/prepared input and after observation; runner hash selector did not cover this filename'))
accepted='modules/studio-desktop/src/test/kotlin/io/github/misakapiano/ccbstudio/desktop/StudioAppearanceTest.kt'
assert gitblob(SOURCE,accepted)==gitblob('30b065915e376d75cc6439fac0ba55925c73f735',accepted)
emit('SOURCE_MAP.json',dict(applicationSource=SOURCE,executionHead=summary['baseCommit'],
    moduleToolGitDiffFromSource='empty before and after',sourceStable=True,
    gradleProperties='Unchanged Xmx512m/MaxMetaspace384m in source; this run overrides ONLY Gradle via exact -D argument recorded in COMMAND/summary. Test256m unchanged.',
    profile='task-local 384MiB Gradle heap/256MiB Gradle metaspace/256MiB test heap, worker1/fork1/in-process/offline/low priority/no persistent daemon',
    originalFourTestClassUnchanged=True,originalFourNotRerun=True,files=mapping,
    installer='NOT BUILT',installerVersion=None,installerSha256=None,public115='Does not contain this theme or P2',
    externalSubmission='PROHIBITED_THIS_ROUND',push='PROHIBITED_THIS_ROUND'))
memory=json.loads((ROOT/'.local/theme-selected-contrast-lowmem-f-20261026/MONITOR_RESULT.json').read_text())
samples=[json.loads(x) for x in (ROOT/'.local/theme-selected-contrast-lowmem-f-20261026/memory-samples.jsonl').read_text().splitlines()]
severe=[]
for s in samples:
    commit=s['committedBytes']>=.95*s['commitLimitBytes'] and s['availableBytes']<128*1024**2
    paging=s['availableBytes']<64*1024**2 and s['pageReadsPerSecond']>1000
    severe.append(commit or paging)
emit('RUN_MATRIX.json',dict(applicationSource=SOURCE,
    e=dict(outcome='PREPARATION_FAILED_BEFORE_GRADLE',runnerExit=1,junit=0,
           reason='Embedded Python script-directory import path was omitted; workspace_artifacts import failed. Exact runner.log retained, no fresh Gradle phase/fixtures created.'),
    f=dict(outcome='PASS',runnerExit=0,junitUniqueMethods=3,failures=0,errors=0,skipped=0,sourceStable=True,
           reason='Only bootstrap corrected to existing sys.path+runpy pattern; source/assertions/profile/test filter unchanged.',
           startedUtc=summary['startedUtc'],runnerSeconds=summary['seconds'],gradleReportedSeconds=89,cases=cases),
    oldRecords='Original A/B/C four-method evidence, P2 disk/memory zero-run records unchanged; not summed or relabeled',
    currentTask='Local automatic validation completed; previous independent P2 remains OPEN/PARTIAL_ACCEPT until separately authorized review',
    projectSendAttemptsThisRound=0,pushAttemptsThisRound=0))
emit('SUMMARY.json',dict(recordedUtc=datetime.datetime.now(datetime.timezone.utc).isoformat(),applicationSource=SOURCE,
    executionHead=summary['baseCommit'],status='LOCAL THREE METHODS PASS / NOT INDEPENDENTLY REVIEWED',
    actualUniqueMethods=3,failures=0,errors=0,skipped=0,sourceStable=True,runnerSeconds=summary['seconds'],
    initialAvailableBytes=memory['before']['availableBytes'],minAvailableBytes=memory['minAvailableBytes'],
    sampledMaxOwnedPrivateBytes=memory['sampledMaxOwnedPrivateBytes'],sampledMaxOwnedWorkingSetBytes=memory['sampledMaxOwnedWorkingSetBytes'],
    sampledMaxSystemCommittedBytes=memory['maxSystemCommittedBytes'],systemCommitChangeAtSampledPeak=memory['maxSystemCommittedBytes']-memory['before']['committedBytes'],
    minObservedCommitHeadroomBytes=min(s['commitLimitBytes']-s['committedBytes'] for s in samples),
    maxObservedPageReadsPerSecond=max(s['pageReadsPerSecond'] for s in samples),maxObservedPagesInputPerSecond=max(s['pagesInputPerSecond'] for s in samples),
    memorySamples=len(samples),monitorFailures=memory['monitorFailures'],foreignOwnerObservations=sum(len(s['foreign']) for s in samples),
    severePressureSampleCount=sum(severe),oomObserved=False,
    interpretation='One scoped run succeeded below old2GiB initial physical-free estimate; not a new universal lower minimum. Paging spikes occurred; no recorded compound severe-pressure condition/OOM. Sampled group maxima exclude helper overhead and can miss brief peaks; shared working-set pages are not unique RAM; global commit delta is not solely attributable to this job.',
    commandLineFlagLimit='Raw jvmFlags arrays are regex excerpts, can include forwarded arguments in python/cmd/launcher. Do not treat those as parsed effective JVM flags. Actual Gradle JVM19128 showed Xmx384/MaxMetaspace256; actual test JVM22732 showed Xmx256.',
    fixtureCleanup='EXPLICITLY_RETAINED; no new automatic cleanup',
    visualAcceptance='NOT TESTED; offscreen Compose pixels are automatic checks only; no Windows system-theme/live detached-window/user acceptance',
    nativePackage='NOT BUILT',independentReview='NOT SUBMITTED / HOLD BY CURRENT INSTRUCTION',externalSubmission='NONE / PROHIBITED',push='NONE / PROHIBITED'))
print(json.dumps(dict(localBundle=str(OUT),actualUniqueMethods=3,status='PASS',filesSoFar=len(list(OUT.rglob('*')))),ensure_ascii=False))
