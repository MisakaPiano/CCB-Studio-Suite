"""Read-only assertion of persisted update and cleanup facts; no rerun or relabel."""
import hashlib,json,time
from pathlib import Path
from tuf.api.metadata import Metadata
ROOT=Path(__file__).resolve().parents[2]
LAB=Path(json.loads((ROOT/'.local/review-fixes-cleanup-20261024/native-lab.json').read_text())['lab'])
def read(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def digest(p):
    with p.open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()
receipt=read(LAB/'candidate/BUILD_RECEIPT.json');ready=read(LAB/'A_HANDOFF_READY.json');folder=Path(ready['plan']).parent;plan=read(folder/'plan.json');result=read(folder/'result.json');ack=read(folder/'B_VERIFIED.json')
checks=[]
def check(name,condition):assert condition,name;checks.append(dict(name=name,status='PASS'))
check('same_fixed_source_and_installer',receipt['sourceSha']=='da2ae810542c79b0049b74148566a423fb3e1bc3' and digest(LAB/'candidate/studio-installer.exe')==receipt['sha256'])
check('real_A_image_bound_and_normal_exit_after_drain',ready['parentPid']==plan['parentPid']==read(LAB/'A_DRAIN_WAITING.json')['pid'] and (LAB/'A_NORMAL_EXIT_READY.json').is_file() and ready['generationSends']==1)
check('helper_COMPLETE_and_B_snapshot',result['status']=='COMPLETE' and ack['snapshot']==plan['snapshot']==ready['snapshot'])
check('three_assets_one_decryptable_synthetic_credential',len(ack['snapshot']['assets'])==3 and len(ack['snapshot']['credentials'])==1 and ack['snapshot']['credentials'][0].get('sha256') and not ack['snapshot']['credentials'][0].get('status'))
a=Path(plan['installed']);b=Path(result['installed'])
check('owned_old_program_and_downloads_retired',not (a/'CCBStudioSuitePreview.exe').exists() and not list((LAB/'runtime/updates/downloads').rglob('studio-installer.exe')))
check('unknown_file_and_current_program_retained',(a/'UNKNOWN_USER_SENTINEL.txt').read_bytes()==b'unknown files must survive owned retirement' and (b/'CCBStudioSuitePreview.exe').is_file())
check('B_visible_window_closed_and_process_exit',any(x.get('alreadyExited') or x.get('waitResult')==0 for x in read(LAB/'NORMAL_CLOSE_EXIT.json')))
repo=LAB/'repository';root=Metadata.from_file(str(repo/'root.json'))
for role in ('root','targets','snapshot','timestamp'):
    metadata=Metadata.from_file(str(repo/(role+'.json')))
    root.signed.verify_delegate(role,metadata.signed_bytes,metadata.signatures)
targets=Metadata.from_file(str(repo/'targets.json'));snapshot=Metadata.from_file(str(repo/'snapshot.json'));timestamp=Metadata.from_file(str(repo/'timestamp.json'))
snapshot.signed.meta['targets.json'].verify_length_and_hashes((repo/'targets.json').read_bytes());timestamp.signed.snapshot_meta.verify_length_and_hashes((repo/'snapshot.json').read_bytes())
target=targets.signed.targets['studio-installer.exe'];target.verify_length_and_hashes((LAB/'candidate/studio-installer.exe').read_bytes())
check('public_ephemeral_TUF_signatures_links_and_target_bytes',digest(repo/'root.json')==read(b/'update-config.json')['rootSha256'] and target.hashes['sha256']==receipt['sha256'])
report=dict(status='POSTCHECK_PASS_DRIVER_FAILURE_PRESERVED',sourceSha=receipt['sourceSha'],installerSha256=receipt['sha256'],checks=checks,driverStatus=read(LAB/'NATIVE_UPDATE_SUMMARY.json')['status'],driverFailure='Evidence-output atomic replacement sharing violation; actual helper result was COMPLETE. Not a claim of exception-free driver success.',dailyInstallationModified=False,realProviderCalls=0)
out=LAB/'FINAL_NATIVE_POSTCHECK.json'
for attempt in range(5):
    try:out.write_text(json.dumps(report,indent=2)+'\n');break
    except PermissionError:
        if attempt==4:raise
        time.sleep(.2)
print(json.dumps(report,indent=2))
