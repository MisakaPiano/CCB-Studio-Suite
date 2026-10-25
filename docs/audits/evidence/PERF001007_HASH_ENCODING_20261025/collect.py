from pathlib import Path
import hashlib,json,shutil,statistics,subprocess,xml.etree.ElementTree as ET,zipfile
ROOT=Path('H:/CCB-Studio-Suite');DEST=ROOT/'docs/audits/evidence/PERF001007_HASH_ENCODING_20261025'
assert not DEST.exists();DEST.mkdir(parents=True)
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def write(p,v):p.write_text(json.dumps(v,indent=2)+'\n',encoding='utf-8')
sources={'baseline':'2559eb030bcda1d34e07802e79f704d3b8e0f9b1','final':'8f4597ddb8278e25a1ab90dafabae4774755e853'};counts={};cleanup=[];metrics={};inputs={}
for kind in ('baseline','final'):
    candidates=list((ROOT/'.local/windows-preview/evidence').glob('*-perf001007-hash-encoding-'+kind+'-20261025'));assert len(candidates)==1;src=candidates[0]
    for p in src.iterdir():
        if p.is_file():
            target=DEST/'runs'/kind/p.name;target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(p,target)
    summary=json.loads((src/'summary.json').read_text());assert summary['sourceStable'] and summary['status']=='PASS'
    if kind=='baseline':assert summary['baseCommit']==sources[kind]
    else:assert subprocess.check_output(['git','diff',sources[kind],summary['baseCommit'],'--','modules','tools'],cwd=ROOT)==b''
    assert(summary['tests'],summary['failures'],summary['errors'],summary['skipped'])==((2 if kind=='baseline' else 7),0,0,0)
    counts[kind]={k:summary[k] for k in ('tests','failures','errors','skipped','sourceStable','baseCommit','seconds','startedUtc')}
    clean=json.loads((src/'fixture-cleanup.json').read_text());assert clean['status']=='COMPLETE';cleanup.append(clean)
    inputs[kind]=json.loads((src/'source-before.json').read_text())
    output=ET.parse(next(src.glob('*HashEncodingPerformanceTest.xml'))).getroot().findtext('system-out');rows=json.loads(next(x for x in output.splitlines() if x.startswith('HASH_ENCODING_ALLOCATION=')).split('=',1)[1]);write(DEST/(kind+'-samples.json'),rows)
    metrics[kind]=[]
    for size in sorted({r['payloadBytes'] for r in rows}):
        for mode in sorted({r.get('kind','actualLegacy') for r in rows if r['payloadBytes']==size}):
            selected=[r for r in rows if r['payloadBytes']==size and r.get('kind','actualLegacy')==mode]
            metrics[kind].append(dict(payloadBytes=size,kind=mode,callsPerSample=64,medians={k:statistics.median(r[k] for r in selected) for k in ('wallMs','cpuMs','allocatedBytes')}))
prefix='modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/'
oldM=subprocess.check_output(['git','show',sources['baseline']+':'+prefix+'Models.kt'],cwd=ROOT).decode();newM=subprocess.check_output(['git','show',sources['final']+':'+prefix+'Models.kt'],cwd=ROOT).decode()
oldLine=next(x for x in oldM.splitlines() if x.startswith('fun sha256('));newLine=next(x for x in newM.splitlines() if x.startswith('fun sha256('));assert oldM.replace(oldLine,newLine,1)==newM
oldF=subprocess.check_output(['git','show',sources['baseline']+':'+prefix+'FileIntegrity.kt'],cwd=ROOT).decode();newF=subprocess.check_output(['git','show',sources['final']+':'+prefix+'FileIntegrity.kt'],cwd=ROOT).decode()
assert oldF.replace('digest.digest().joinToString(""){"%02x".format(it)}','java.util.HexFormat.of().formatHex(digest.digest())')==newF
testPath='modules/studio-core/src/test/kotlin/io/github/misakapiano/ccbstudio/core/HashEncodingPerformanceTest.kt';test=subprocess.check_output(['git','show',sources['final']+':'+testPath],cwd=ROOT).decode()
legacy=next(x for x in test.splitlines() if x.startswith('private fun legacySha256('));assert legacy.replace('private fun legacySha256(','fun sha256(',1)==oldLine
identities=[]
for kind in ('baseline','final'):
    for name in [prefix+'Models.kt',prefix+'FileIntegrity.kt',testPath,'tools/Run-PreviewChecks.py']:
        fixed=subprocess.check_output(['git','show',sources[kind]+':'+name],cwd=ROOT)
        assert inputs[kind][name] in (hashlib.sha256(fixed).hexdigest(),hashlib.sha256(fixed.replace(b'\n',b'\r\n')).hexdigest())
        identities.append(dict(run=kind,path=name,testInputSha256=inputs[kind][name],gitSha256=hashlib.sha256(fixed).hexdigest()))
unchanged=['modules/asset-library','modules/image-authority','modules/studio-desktop','tools',prefix+'StateJournal.kt',prefix+'StudioWorkspace.kt']
assert subprocess.check_output(['git','diff',sources['baseline'],sources['final'],'--',*unchanged],cwd=ROOT)==b''
write(DEST/'SOURCE_MAP.json',dict(sources=sources,executionSource=counts['final']['baseCommit'],executionModulesAndToolsEqualApplication=True,identities=identities,unchangedPaths=unchanged,productionOnlyTwoHexTextExpressions=True,legacyFunctionExact=True,sha256AlgorithmInputsAndValidationUnchanged=True,finalUniqueMethods=7,baselineRepeatedMethodsNotAdded=True,release='NONE',independentReview='NOT REQUESTED',humanAndPackage='NOT TESTED'))
write(DEST/'ANALYSIS.json',dict(medians=metrics,counts=counts,finalUniqueMethods=7,cleanupFiles=sum(c['removed'] for c in cleanup),cleanupBytes=sum(c['bytesFreed'] for c in cleanup),limits=['64 calls/sample,one warmup5calls,4same-JVM alternating legacy/current rounds per0/1KiB/64KiB input;not64unique tests','All256byte values compared under6 explicit Locales with no default change;empty/abc fixed SHA vectors','Two isolated constructed v2/v3 envelopes with old digest function,not an actual published installer/update path','No keys/signatures/trust roots/algorithm/input bytes/validation changed;only equivalent lowercase hex representation','Microbenchmark thread allocation not complete UI/RSS/physicalIO/real-library or universal speed acceptance','Single worker/low priority/2processors/256MiB,Desktop gate owner checks not continuous quiet proof']))
deferred=ROOT/'.local/perf001007-hash-encoding-20261025/resource-deferrals.jsonl'
if deferred.exists():shutil.copyfile(deferred,DEST/'resource-deferrals.jsonl')
shutil.copyfile(Path(__file__),DEST/'collect.py')
(DEST/'ATTEMPTS_AND_LIMITS.md').write_text('''# SHA digest text formatting

Baseline2559eb0:2 methods PASS actual sha256 API and all256byte values across ROOT/US/China/Japan/Arabic/Turkish formatters without changing JVM default;known empty/abc vectors. Final8f4597d:7 unique PASS,0fail/error/skip,source stable.4 hash methods(actual paired allocation/canonical outputs,byte/Locale/vectors,old-digest synthetic2/3document envelopes/full previous/body hashes and corruption refusal,file fingerprint/size/budget/source preservation)+3existing chain/unknown backup/bounded fingerprint cases. Internal256values/6Locales/64calls/rounds/versions not separate tests;baseline repeated methods not added.

Only core sha256's and fingerprintFile's digest-to-string expressions replace per-byte Formatter with Java17HexFormat already used by unchanged JavaAssetStore. SHA-256 algorithm/provider selection,input bytes,full digest length,lowercase ASCII result,all actual file reads,budgets,path safety/error categories remain. Entire Models and FileIntegrity equal outside these two expressions;legacy reference character-exact except function signature/private. No shared mutable MessageDigest,caching/skipped checks,request/Prompt logic/data schema,signature/key/TUF/update protocol/config/metadata/installer/version/tag change.

Actual old API baseline retained;final same-JVM alternating4 legacy/current batches each64calls at0/1KiB/64KiB inputs.5warmup calls each,batch results verified outside counter,measurement before JSON/assertions;new per-call allocation bound2KiB. Bare string-encoding/crypto helper measurement,not end-to-end application/large media/RSS or allWindows speed proof. CPU timer sub-tick0 not zeroCPU. Old manually constructed digest envelopes are isolated schema fixtures,not actual old installer upgrade/native credentials evidence.

Desktop busy deferred final,one gated run low priority/single worker2processor256MiB,launch boundary empty not continuous quiet host proof. No unrelated process termination/security/global Locale change/private data/credentials/paid calls/user source scan,new package/release/daily install or Project payload. Successful own fixtures retired after logs,earlier real failures/old evidence/baseline/backups/unknown content retained. No new failure in this increment. Assets/editor USER_NOT_TESTED,115/Prompt PENDING,q09 UNKNOWN and PRODUCT_HOLD / NOT RC remain;independent review not requested,new materials only prepared.
''',encoding='utf-8')
(DEST/'.gitattributes').write_bytes(b'* -text\n**/* -text\n')
items=[dict(path=p.relative_to(DEST).as_posix(),bytes=p.stat().st_size,sha256=sha(p)) for p in sorted(DEST.rglob('*')) if p.is_file()]
write(DEST/'RAW_MANIFEST.json',dict(format=1,files=items));archive=DEST/'RAW_EVIDENCE.zip'
with zipfile.ZipFile(archive,'x',compression=zipfile.ZIP_DEFLATED) as z:
    for p in sorted(DEST.rglob('*')):
        if p.is_file() and p!=archive:z.write(p,p.relative_to(DEST).as_posix())
with zipfile.ZipFile(archive) as z:
    assert z.testzip() is None
    for item in items:assert hashlib.sha256(z.read(item['path'])).hexdigest()==item['sha256']
print(json.dumps(dict(sources=sources,zipBytes=archive.stat().st_size,zipSha256=sha(archive),manifestFiles=len(items),zipMembers=len(items)+1,medians=metrics,cleanupFiles=sum(c['removed'] for c in cleanup),cleanupBytes=sum(c['bytesFreed'] for c in cleanup)),indent=2))
