from pathlib import Path
ROOT=Path('H:/CCB-Studio-Suite');OUT=ROOT/'.local/preview-publication-118-20261027';old=ROOT/'.local/preview-candidate-118-20261027'
s=(old/'integrate-r3.py').read_text(encoding='utf-8-sig')
s=s.replace("OUT=ROOT/'.local/preview-candidate-118-20261027'","OUT=ROOT/'.local/preview-publication-118-20261027'").replace("Path('C:/CCB-Studio-Suite-Test-118-20261027/r3')","Path('C:/CCB-Studio-Suite-Test-118-20261027/p')").replace("EVIDENCE=OUT/'integration-r3'","EVIDENCE=OUT/'public-native'")
s=s.replace("assert not CROSS.exists();CROSS.mkdir();", "assert load(OUT/'METADATA_PUBLICATION.json')['releaseVersion']==118\nassert not CROSS.exists();CROSS.mkdir();")
s=s.replace("OUT/'candidate-r3/BUILD_RECEIPT.json'","Path('C:/CCB-Studio-Suite-Test-118-20261027/b/BUILD_RECEIPT.json')").replace("OUT/'candidate-r3/payload/INSTALL_MANIFEST.json'","Path('C:/CCB-Studio-Suite-Test-118-20261027/b/payload/INSTALL_MANIFEST.json')").replace("OUT/'candidate-r3/payload/update-root.json'","Path('C:/CCB-Studio-Suite-Test-118-20261027/b/payload/update-root.json')")
a=s.index("installerFolder=cache/'downloads'");b=s.index("command([root/'CCBStudioLauncher.exe'],'active117-entry.log')")
block='''newReceipt=load(Path('C:/CCB-Studio-Suite-Test-118-20261027/b/BUILD_RECEIPT.json'))
publicCommand=[old/'updater/python.exe','-B',old/'updater/lib/studio_update.py','--package',old,'--cache',cache,'--download']
write(EVIDENCE/'OLD117_PUBLIC_COMMAND.json',dict(arguments=list(map(str,publicCommand)),noAdapter=True,rootSha256=sha(old/'update-root.json')))
command(publicCommand,'old117-public-download.log',timeout=240)
download=load(EVIDENCE/'old117-public-download.log');assert download['status']=='DOWNLOADED_VERIFIED' and download['version']==118 and download['sha256']==newReceipt['sha256']
installer=Path(download['installer']);assert installer.is_relative_to(cache/'downloads') and sha(installer)==newReceipt['sha256'];installerFolder=installer.parent
write(EVIDENCE/'OLD117_PUBLIC_DOWNLOAD.json',dict(status='PASS',result=download,publicSignedDiscovery=True,realPublicBytes=True,sourceAdapter=False))
custom=dict(product='ccb-studio-suite',platform='windows-x86_64',channel=download['channel'],version=download['version'],minimum_reader=download['minimumReader'],maximum_reader=download['maximumReader'],data_version=download['dataVersion'],sourceSha=download['sourceSha'],displayVersion=download['displayVersion'],notes=download['notes'],installedBytes=download['installedBytes'])
'''
s=s[:a]+block+s[b:]
s=s.replace("[str(old/'updater/pythonw.exe'),'-B',str(OUT/'old-helper-adapter.py'),str(plan),str(targetFile)]","[str(old/'updater/pythonw.exe'),'-B',str(old/'updater/lib/apply_update.py'),'--handoff',str(plan)]")
s=s.replace("discovery='ISOLATED_LOCAL_CANDIDATE_ADAPTER_NOT_PUBLIC_TUF'","discovery='REAL_PUBLIC_SIGNED_TUF_117_TO_118_NO_ADAPTER'")
marker="print(json.dumps(value,indent=2),flush=True)"
extra='''currentCache=CROSS/'current-check'
command([current/'updater/python.exe','-B',current/'updater/lib/studio_update.py','--package',current,'--cache',currentCache],'current118-public.log',timeout=100)
currentResult=load(EVIDENCE/'current118-public.log');assert currentResult['status']=='UP_TO_DATE' and currentResult['version']==118 and currentResult['sha256']==newReceipt['sha256']
write(EVIDENCE/'CURRENT118_PUBLIC.json',dict(status='PASS',result=currentResult))
raw=EVIDENCE/'signed-metadata';raw.mkdir()
for label,directory in [('old117',cache/'metadata'),('current118',currentCache/'metadata')]:
 target=raw/label;target.mkdir()
 for path in directory.glob('*.json'):shutil.copy2(path,target/path.name)
value['public117DownloadAndSignature']='PASS';value['public118Current']='UP_TO_DATE';value['discoveryAdapterUsed']=False;write(EVIDENCE/'RESULT.json',value)
write(OUT/'PUBLIC_CHANNEL_VERIFICATION.json',value)
print(json.dumps(value,indent=2),flush=True)'''
s=s.replace(marker,extra)
assert "old-helper-adapter.py" not in s and "LOCAL_CANDIDATE.json" not in s
(OUT/'verify-public-native.py').write_text(s,encoding='utf-8');(OUT/'SeedCandidate118.java').write_bytes((old/'SeedCandidate118.java').read_bytes())
print('Public signed real117/native118 verification driver prepared; no execution before metadata publication')
