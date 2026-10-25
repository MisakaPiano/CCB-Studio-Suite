from pathlib import Path
import hashlib,io,json,os,subprocess,zipfile
ROOT=Path('H:/CCB-Studio-Suite');DEST=ROOT/'docs/audits/evidence/PERFORMANCE_LATER_INCREMENT_INDEX_20261025'
def git(*args):return subprocess.check_output(['git',*args],cwd=ROOT)
def sha(b):return hashlib.sha256(b).hexdigest()
specs=[
 ('PERF003_JOB_RECORDED_TIMES_20261025','614988a0f2f6bb7f11acb5ce2dd24879929516ec','41e0111194b407f329a222b2adf4dfcb7631a93b','8ead31f82da88a387371a7d40734b7cb7c08b567e186e0f40c5a351ae869d34e',76429,3,'IMPLEMENTED_AUTO_TESTED','Job result date aggregation; imported parsing/budget/order unchanged;2resource timeouts0executions retained.'),
 ('PERF005_COLOR_SYNTAX_20261025','7e18e27bfb3713af1ddead46e3ccebbe44e6d1f5','b8b225d6951617b35b8f5e3d5b82950511681e3c','5956ce94b63d548fd2def374d0293cb29f7ccbb0da688f6062577db4021d7fd1',91403,5,'IMPLEMENTED_AUTO_TESTED','3lazy fixedRegex;functionalcolor allocationlower,HEX unchanged;3d1251f observation misreport retained/corrected.'),
 ('PERF005_VERTICAL_TOKENS_20261025','6805de754f3472da91d41d6e39adce1f758d8043','e9ae9f32498786326ae41552748db346604d97bd','de9d28a668688cd38672624267c01f27330dd4dafcdaa7df1fa07bd881841290',99994,4,'IMPLEMENTED_AUTO_TESTED','Fixed punctuation Regex/symbolSet/directquote comparisons;3source metadata fixtures notwhole typography;collector preparationfailure0extra tests retained.'),
 ('PERF005_RICH_TAG_SYNTAX_20261025','753ccac4cf81b0eb7b958b11a1e42b5578f434b1','9987d85638c38bf64641f3c19bd4836084c905a5','3bca24af68d31a77a4a9162e5ecf28eb4884ed6fad4de1580746dd5bad0e462b',87915,3,'IMPLEMENTED_AUTO_TESTED','One fixedRegex reuse;parser grammar/styles/errors/limits unchanged;plain/simple allocationunchanged andwall noise notattributed.'),
 ('PERF005_PUNCTUATION_CLASSES_20261025','838b087a08a7f81eb62d2bef2f2f5f819433e2f8','3d9abb0e10a61a230e17542a8cc22b5c1c0992e3','d026c15e213282c99b4455492d2b6550cf1a7a1208eb1e2f89eb7e0b6efc64a9',125974,3,'IMPLEMENTED_AUTO_TESTED','5fixedSets;completeverticalLayout preserved,nofontmeasurement;initialvector1FAIL/source/fixture retained,1e-9 unchanged;81420ac repeated observationmisreport corrected separately.'),
 ('PERF005_PUNCTUATION_RATIOS_20261025','e0ae1d43799957b52e0957ed0603de4edb14784e','e8378806ce950bf3d22fff1dc3e71bc7c0d95548','e97050aadfc269a36594ab3dc13348eefbab146763120546236d4cbcb7130e88',89873,3,'NOT_ADOPTED_PERFORMANCE_HOLD','Functional3PASS but8192loosewall2.63585->3.6132ms;allocationlower notadoption. Priorbaseline reused0newexecution;restored2c35296 inputs equal3d9,0restorationtests.')]
rows=[]
for directory,evidence,app,rawsha,rawsize,methods,state,limits in specs:
    rel='docs/audits/evidence/'+directory+'/'
    sourceRaw=git('show',evidence+':'+rel+'SOURCE_MAP.json');source=json.loads(sourceRaw)
    assert source['sources']['final']==app and source['finalUniqueMethods']==methods
    archive=git('show',evidence+':'+rel+'RAW_EVIDENCE.zip');assert len(archive)==rawsize and sha(archive)==rawsha
    manifestRaw=git('show',evidence+':'+rel+'RAW_MANIFEST.json');manifest=json.loads(manifestRaw)
    with zipfile.ZipFile(io.BytesIO(archive)) as z:
        assert z.testzip() is None and z.read('RAW_MANIFEST.json')==manifestRaw
        assert set(z.namelist())=={i['path'] for i in manifest['files']}|{'RAW_MANIFEST.json'}
        for item in manifest['files']:
            b=git('show',evidence+':'+rel+item['path']);assert len(b)==item['bytes'] and sha(b)==item['sha256'] and z.read(item['path'])==b
    execution=source.get('executionSource') or source['executions']['final']
    assert git('diff',app,execution,'--','modules','tools')==b''
    report=directory.replace('PERF005_PUNCTUATION_RATIOS_','PERF005_PUNCTUATION_RATIOS_EXPERIMENT_')
    reportPath='docs/audits/'+report+'.md';git('show',evidence+':'+reportPath)
    rows.append(dict(id=directory,applicationSource=app,executionSource=execution,evidenceCommit=evidence,sourceMapPath=rel+'SOURCE_MAP.json',sourceMapSha256=sha(sourceRaw),reportPath=reportPath,rawEvidencePath=rel+'RAW_EVIDENCE.zip',rawSha256=rawsha,rawBytes=rawsize,rawManifestFiles=len(manifest['files']),rawZipMembers=len(manifest['files'])+1,localFinalUniqueMethods=methods,implementationState=state,baselineSources=source['sources'],scopeAndNegativeRecords=limits,humanAndPackage='NOT TESTED',independentReview='NOT REQUESTED',newExecutionsFromThisIndex=0))
restoration='2c352961f2023464bbec0475722b78cdaa52d4ce';production='3d9abb0e10a61a230e17542a8cc22b5c1c0992e3'
assert git('diff',production,restoration,'--','modules','tools')==b''
decisionPath='docs/audits/evidence/PERF005_PUNCTUATION_RATIOS_DECISION_20261025.json';decisionRaw=git('show',specs[-1][1]+':'+decisionPath)
assert json.loads(decisionRaw)['adoption']=='NOT_ADOPTED'
packet=dict(format=1,state='PREPARED_ONLY_NOT_SENT',scope='six later historical increments after originaltwofollowons;not latestHEAD endtoend acceptance',productionBoundarySource=production,restorationSource=restoration,restorationInputsIdentical=True,rows=rows,newTestExecutionsFromIndex=0,noMethodSummation=True,noUnifiedLatestSourceOrPackageAcceptance=True,ratioAdoptionDecision=dict(path=decisionPath,evidenceCommit=specs[-1][1],sha256=sha(decisionRaw)),originalIndexesUnchanged=[dict(commit='7314ef9ac092c0056ffa6d8918472cbf8868ec93',path='handoff/PERFORMANCE_INCREMENT_REVIEW_READY_20261025.md'),dict(commit='b5e84daf63ceb3139277e6e3400d1116f354dd3b',path='handoff/PERFORMANCE_TWO_FOLLOW_ONS_REVIEW_READY_20261025.md')],excludedPending=dict(source='b049e4c7bdc292a26e5623313c1c1d91933ca9b9',record='55db06e30c65e0a8e317111931cd85d5ae2ea28e',state='PENDING_OBSERVATION',feature='document font dependencies baseline,productionunchanged;not included orclaimedtested'),unchangedLimits=['Oldfailures/userrejections/rejectedfoldertraversals retained','Operatorwrongpath priorstate/exactUTC UNKNOWN,no furtheraccess','Asset/editorUSER_NOT_TESTED,115experience/Promptcomparison PENDING','q09FAILED/rootUNKNOWN,PRODUCT_HOLD / NOT RC','Allcurrent source-onlyincrements no newpackage/tag/release/trustchain orDesktop modifications','No newProjectsend authorized;originalDesktopreport boundaryunchanged'])
assert not DEST.exists()
for p in [DEST,*DEST.parents]:
    if p.exists():assert not(os.lstat(p).st_file_attributes&0x400)
    if p==ROOT:break
assert DEST.is_relative_to(ROOT);DEST.mkdir()
data=(json.dumps(packet,indent=2)+'\n').encode();target=DEST/'REVIEW_MATRIX.json'
with target.open('xb') as f:f.write(data)
print(json.dumps(dict(rows=len(rows),matrixSha256=sha(data),newTestExecutions=0,productionBoundary=production,ratioState=rows[-1]['implementationState'],allSixRawGitBlobsAndZipCrcMembersVerified=True)))
