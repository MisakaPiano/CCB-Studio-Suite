"""Freeze this task's engineering inputs and zero-execution observations, no fixture/data export."""
from pathlib import Path
import datetime,hashlib,json,subprocess
root=Path('H:/CCB-Studio-Suite');assert Path.cwd().resolve()==root.resolve()
local=root/'.local/theme-selected-contrast-20261025'
out=root/'docs/audits/evidence/THEME_SELECTED_CONTRAST_P2_20261025'
source='c39e860252fdd96c9bec5b5d42d1abd988d3ec06';old='30b065915e376d75cc6439fac0ba55925c73f735'
assert subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip()==source
assert not subprocess.check_output(['git','status','--porcelain','--','modules','tools']).strip()
def write(name,data):
    assert not Path(name).is_absolute() and '..' not in Path(name).parts
    target=out/name;assert not target.exists() and not target.is_symlink()
    with target.open('xb') as f:f.write(data)
def emit(name,value):write(name,(json.dumps(value,indent=2,ensure_ascii=False)+'\n').encode('utf-8'))
def blob(ref,path):return subprocess.check_output(['git','show',ref+':'+path])
write('.gitattributes',b'* -text\n**/* -text\n')
changed=subprocess.check_output(['git','diff','--name-only',old,source,'--','modules','tools'],text=True).splitlines()
assert len(changed)==17 and all(p.startswith('modules/studio-desktop/src/') for p in changed)
inputs=['settings.gradle.kts','build.gradle.kts','gradle.properties','gradle/verification-metadata.xml']
for module in ['studio-desktop','studio-core','asset-library','image-authority']:
    inputs.append(f'modules/{module}/build.gradle.kts')
    inputs.append(f'modules/{module}/gradle.lockfile')
mapped=[]
for path in changed+inputs:
    raw=blob(source,path);working=(root/path).read_bytes()
    mapped.append(dict(path=path,gitBlobBytes=len(raw),gitBlobSha256=hashlib.sha256(raw).hexdigest(),
        preparedWorkingBytes=len(working),preparedWorkingSha256=hashlib.sha256(working).hexdigest(),
        role='changed source/test' if path in changed else 'unchanged configured build input',
        equalsOldBlob=False if path in changed else raw==blob(old,path)))
test='modules/studio-desktop/src/test/kotlin/io/github/misakapiano/ccbstudio/desktop/StudioAppearanceTest.kt'
assert blob(source,test)==blob(old,test)
assert not subprocess.check_output(['git','diff','--name-only','dce6600814e91c92e1d95432faef02e8ad09c5d4',source,'--',
    'docs/audits/evidence','docs/audits/DISPLAY_THEME_FOUR_REGRESSION_20261025.md']).strip()
emit('SOURCE_MAP.json',dict(applicationSource=source,priorAcceptedThemeSource=old,
    priorAcceptedEvidence='dce6600814e91c92e1d95432faef02e8ad09c5d4',changedFiles=17,
    mapping='Prepared source bytes only, NOT executed compiler input. Git and working bytes separately labeled (possible CRLF differences).',
    executedSource=None,buildInvocations=0,files=mapped,acceptedFourTestSourceUnchanged=True,
    oldEvidenceUnchanged=True,installerVersion=None,installerSha256=None,
    packageStatus='NOT BUILT; public Preview115 does not include this source; no build/package output mapping exists'))
write('SOURCE_DIFF.patch',subprocess.check_output(['git','diff','--binary',old,source,'--','modules','tools']))
for p in sorted(local.glob('preflight-*.json')):write(p.name,p.read_bytes())
for name in ['capture-preflight.py','capture-preflight-initial.py']:write(name,(local/name).read_bytes())
emit('RUN_MATRIX.json',dict(applicationSource=source,status='RESOURCE_NOT_ADMITTED_NO_TEST_STARTED',
    compilation='NOT COMPILED',plannedUniqueMethods=3,actualUniqueMethods=0,runnerInvocations=0,
    actualPasses=0,failures='NOT RUN (not a passing zero-failure test run)',
    plannedMethods=[
        'indicatorUsesOriginalAccentWhenReadableAndPreservesEveryStoredRgb',
        'filledBatchCoverAndLibraryControlsRenderReadableForegrounds',
        'actualRouteAndLibrarySelectionBordersStayVisibleAndClickable'],
    plannedCommand='python tools/Run-PreviewChecks.py --phase display-theme-selected-p2-20261025 --keep-fixtures -- :studio-desktop:test --tests *StudioSelectedContrastTest',
    commandExecuted=False,oldAcceptedFour='Preserved, not rerun or counted toward P2',
    reviewStatus='NOT SUBMITTED; one authorized send remains unused pending actual regression results',
    noNewFixtureCreated=True,keepFixturesWhenRun=True,packageReleaseInstallCleanupMoves=False,
    next='Fresh bounded budget/space/memory and two 30s quiet owner observations plus final preflight before invoking only the three new methods'))
emit('PREPARATION_NOTES.json',dict(productTestsExecuted=0,notes=[
    dict(stage='read-only source navigation',result='Some guessed file paths and wildcard shell paths did not exist; corrected to rg --files. Broad ignored-tree search interrupted; no deletion or process termination.'),
    dict(stage='patch preparation',result='One incomplete signature patch rejected, no edit; one all-before-write replacement count expected3 but actual4, aborted without source writes; corrected exact four observed sites.'),
    dict(stage='whitespace check',result='Source-preserved CRLF in authoritative Suite-local presentation file is flagged under default diff whitespace; cr-at-eol check passed, raw source line endings retained.'),
    dict(stage='initial resource capture',result='exit1 before observations/tests: Python3.10 lstat on an existing build class exceeded normal Windows path handling (WinError3); initial producer retained, corrected only to extended absolute path for same build subtree; no fake missing product file or test FAIL attribution.'),
    dict(stage='default contrast planning',result='Independent arithmetic only, default blue minimum standard-surface contrast approximately5.1638; not Kotlin execution or a JUnit case.')],
    exactInitialFailureUtc='UNKNOWN; observed before 2026-10-25T14:34:17.692768Z; no fabricated stderr artifact',
    failuresNotDiscarded=True))
emit('AUTHORIZATION.json',dict(userReplyUtc='2026-10-25T14:19:00Z (minute precision only)',
    provenance='Original coordinator supplied explicit human approval transcript; not inferred from Project advice',
    scope='Existing theme P2 selected contrast, smallest regression, fixed engineering-only once to original Project after ready; no other task',
    targetConversation='6adcaaef-3fac-83e8-ae57-2a524281e049',priorReviewResponse='d5d317bf-4708-4405-ad8a-87de63bd92f3',
    sendAttempts=0,notAuthorized=['package','release','install','move/delete','private data/credentials','trust changes','Desktop writes','user visual acceptance']))
write('collect-evidence.py',Path(__file__).read_bytes())
files=[]
for p in sorted(out.iterdir()):
    assert p.is_file() and not p.is_symlink()
    data=p.read_bytes();files.append(dict(path=p.name,bytes=len(data),sha256=hashlib.sha256(data).hexdigest()))
emit('MANIFEST.json',dict(recordedUtc=datetime.datetime.now(datetime.timezone.utc).isoformat(),
    basis='Exact resource observations and prepared source/input identity; no compiler/JUnit result, no user fixture payload',files=files))
print(json.dumps(dict(files=len(files)+1,bytes=sum(p.stat().st_size for p in out.iterdir()),source=source,actualJUnit=0)))
