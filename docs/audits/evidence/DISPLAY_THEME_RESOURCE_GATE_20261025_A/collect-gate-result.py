from pathlib import Path
import json,hashlib,datetime,subprocess
root=Path('H:/CCB-Studio-Suite')
local=root/'.local/studio-display-theme-20261025'
out=root/'docs/audits/evidence/DISPLAY_THEME_RESOURCE_GATE_20261025_A'
assert not list(out.iterdir()),'Unexpected existing output; no overwrite'
state=json.loads((local/'gate-status.json').read_text(encoding='utf-8-sig'))
assert state['status']=='RESOURCE_GATE_TIMEOUT_NO_TEST_STARTED' and state['runnerInvoked'] is False and state['details']['actualTestsStarted']==0
assert (local/'gate-runner.log').stat().st_size==0
assert not list((root/'.local/windows-preview/evidence').glob('*-display-theme-20261025'))
source='42146d3705f5cbc9ed392735f9c4727f8a8f67d7'
assert not subprocess.check_output(['git','diff','--name-only',source,'HEAD','--','modules','tools'],cwd=root,text=True).strip()
for name in ['RESOURCE_RECHECK_A.json','wait-and-run-theme.ps1','gate-status.json','gate-observations.jsonl','gate-runner.log','collect-gate-result.py']:
    with (out/name).open('xb') as f:f.write((local/name).read_bytes())
with (out/'.gitattributes').open('x',encoding='utf8',newline='') as f:f.write('* -text\n')
failure={'type':'LookupError','message':'unknown encoding: utf8-sig','stage':'Inline collector, first read of gate-status.json before any evidence file copy','evidenceFilesCreatedBeforeFailure':0,'originalProducer':'Inline tool invocation retained in conversation; no saved first-producer file','correction':'Use Python encoding name utf-8-sig; resume this empty, task-created destination with CreateNew writes','actualJUnitExecutionsAdded':0,'gateRerun':False}
with (out/'COLLECTOR_FIRST_FAILURE.json').open('x',encoding='utf8',newline='') as f:json.dump(failure,f,indent=2);f.write('\n')
result={'recordedUtc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'applicationSource':source,'executionDocsHead':'1c3c9d9e6ba1a35e29c462657a0eba888e0ced22','phase':'display-theme-20261025','gateOutcome':state['status'],'runnerInvoked':False,'actualJUnitExecutions':0,'compileStatus':'NOT COMPILED','testStatus':'NOT TESTED; not PASS/FAIL/SKIPPED','runnerLogBytes':0,'freshRunnerEvidenceDirectories':0,'packageStatus':'NOT BUILT','protectionCase':{'id':'UPD-CLEAN-PROTECTION-001','docsCommit':'57876f186bf1b6066e047d2898d4515dfab8ae38','status':'REGISTERED / STATIC ONLY / NOT EXECUTED'},'finalObservationRecordedFromPrecedingToolOutput':{'observedUtc':'2026-10-25T11:11:27.5017227Z','hFreeBytes':3090264064,'cFreeBytes':9589731328,'owners':[{'pid':47608,'owner':'DESKTOP'}]},'budgetBytes':{'H':2194033190,'C':805306368},'decision':'Space floor satisfied at observed snapshots. Thirty-minute gate did not obtain two quiet admissions. Desktop owner remains; do not loop retry without changed resource evidence. No termination or background Studio work remains.','humanAcceptance':'NOT TESTED','independentReview':'NOT REQUESTED','realAppOrDataAccess':'NONE','cleanupOrCrossVolumeUse':'NONE','collectorFirstAttempt':'FAILED before copies; see COLLECTOR_FIRST_FAILURE.json, no new tests/gate rerun','preserved':'Original source, raw results, failures, denied decisions, unknowns, 115/Prompt PENDING, assets/editor USER_NOT_TESTED, PRODUCT_HOLD/NOT RC.'}
with (out/'RESULT.json').open('x',encoding='utf8',newline='') as f:json.dump(result,f,indent=2);f.write('\n')
manifest={'recordedUtc':result['recordedUtc'],'basis':'Exact copies of this task-owned gate files plus result/failure transcription; no test execution','files':[{'path':p.name,'bytes':p.stat().st_size,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()} for p in sorted(out.iterdir()) if p.is_file()]}
with (out/'MANIFEST.json').open('x',encoding='utf8',newline='') as f:json.dump(manifest,f,indent=2);f.write('\n')
p=local/'CONTINUATION.json'
s=json.loads(p.read_text(encoding='utf8'))
s['updatedUtc']=result['recordedUtc'];s['activeExecSessionId']=None;s['status']='RESOURCE_GATE_TIMEOUT_NO_TEST_STARTED; Desktop admission blocked; space snapshots meet unchanged floor';s['actualMethodsExecuted']=0;s['gateOutcomeEvidence']='docs/audits/evidence/DISPLAY_THEME_RESOURCE_GATE_20261025_A/RESULT.json';s['nextAction']='No active Studio job. Await changed Desktop resource evidence; only then fresh target-volume peak and owner preflight before the same four new theme tests. Do not repeat unchanged gates/source checks or invent more work. No package before theme validation; protection conflict remains static-only until internal acceptance. No real App, cleanup, new I: use, release or daily install changes.'
p.write_text(json.dumps(s,indent=2)+'\n',encoding='utf8',newline='')
print(json.dumps({'gate':state['status'],'actualJUnitExecutions':0,'rawEvidenceFiles':len(manifest['files'])+1,'evidenceBytes':sum(p.stat().st_size for p in out.iterdir() if p.is_file())}))
