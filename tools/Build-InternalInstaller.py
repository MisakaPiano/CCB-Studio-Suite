"""Build a fresh-directory NSIS installer from an actual Compose application image.

Internal test channel only. Does not execute the installer or change system state.
Payloads never include data roots. Existing destinations are rejected, not overlaid.
"""
import argparse,hashlib,json,re,shutil,subprocess
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]

def digest(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def quote(s):
    s=str(s)
    if any(c in s for c in '$"\r\n'):raise ValueError('Unsupported NSIS path')
    return '"'+s.replace('/','\\')+'"'

def path_preflight(payload):
    # NSIS/Win32 extraction uses legacy path limits. Count UTF-16 code units,
    # matching Unicode NSIS StrLen; include our generated completion/uninstaller.
    units=lambda s:len(s.encode('utf-16-le'))//2
    files=[p.relative_to(payload).as_posix() for p in payload.rglob('*') if p.is_file()]
    files+=['INSTALL_COMPLETE','Uninstall.exe']
    directories=[p.relative_to(payload).as_posix() for p in payload.rglob('*') if p.is_dir()]
    longest_file=max(map(units,files))
    longest_directory=max([0]+list(map(units,directories)))
    return ['StrLen $0 "$INSTDIR"',f'IntOp $1 $0 + {longest_file+1}',
            'IntCmp $1 259 path_files_ok path_files_ok path_too_long','path_files_ok:',
            f'IntOp $1 $0 + {longest_directory+1}',
            'IntCmp $1 247 path_lengths_ok path_lengths_ok path_too_long','path_lengths_ok:']

def build(app,runtime,bootstrap,destination,version,metadata_url,targets_url,build_evidence):
    destination=destination.resolve()
    if not destination.is_relative_to(ROOT/'.local') or destination.exists():raise ValueError('New Suite-local destination required')
    summary=json.loads((build_evidence/'summary.json').read_text(encoding='utf-8'))
    if summary['exitCode']!=0 or not summary['sourceStable'] or ':studio-desktop:createDistributable' not in summary['arguments']:raise ValueError('No verified actual app build')
    source=json.loads((build_evidence/'source-before.json').read_text(encoding='utf-8'))
    if any(digest(ROOT/p)!=h for p,h in source.items() if p.startswith('modules/')):raise ValueError('Application sources changed since build')
    sha=summary['baseCommit']
    if not re.fullmatch('[0-9a-f]{40}',sha) or version<1:raise ValueError('Invalid identity')
    if not (app/'CCBStudioSuitePreview.exe').is_file():raise ValueError('Actual app launcher missing')
    receipt=json.loads((runtime/'RUNTIME_MANIFEST.json').read_text(encoding='utf-8'))
    for item in receipt['files']:
        p=runtime/item['path']
        if p.stat().st_size!=item['bytes'] or digest(p)!=item['sha256']:raise ValueError('Runtime changed: '+item['path'])
    destination.mkdir(parents=True)
    payload=destination/'payload';shutil.copytree(app,payload)
    shutil.copytree(runtime,payload/'updater',ignore=shutil.ignore_patterns('__pycache__'))
    shutil.copy2(bootstrap,payload/'update-root.json')
    config=dict(format=1,product='ccb-studio-suite',platform='windows-x86_64',channel='internal',version=version,displayVersion=f'0.2.{version}-internal',reader=3,sourceSha=sha,metadataUrl=metadata_url,targetsUrl=targets_url,rootSha256=digest(bootstrap),internalTest=True)
    (payload/'update-config.json').write_text(json.dumps(config,indent=2)+'\n',encoding='utf-8')
    manifest=dict(format=1,product=config['product'],platform=config['platform'],channel=config['channel'],version=version,sourceSha=sha,files=[])
    for p in sorted(payload.rglob('*')):
        if p.is_symlink():raise ValueError('Payload symlink')
        if p.is_file():manifest['files'].append(dict(path=p.relative_to(payload).as_posix(),bytes=p.stat().st_size,sha256=digest(p)))
    (payload/'INSTALL_MANIFEST.json').write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf-8')
    compiler=ROOT/'.local/windows-rc/nsis-tools/nsis-3.13/makensis.exe'
    archive=ROOT/'.local/windows-rc/updater-downloads/nsis-3.13-verified.zip'
    if digest(archive)!='ba63dffc4410ee89193e1cb5a41989991bd77c61068da17e3156d136b7b0b3d8':raise ValueError('NSIS archive identity changed')
    # Includes/stubs/plugins are executable build inputs too: verify every extracted file.
    import zipfile
    with zipfile.ZipFile(archive) as z:
        for member in z.infolist():
            if member.is_dir():continue
            installed_tool=compiler.parent.parent/member.filename
            if not installed_tool.is_file() or installed_tool.read_bytes()!=z.read(member):raise ValueError('NSIS toolchain changed: '+member.filename)
    script=['Unicode true','RequestExecutionLevel user','SetCompressor zlib','SetOverwrite off',
            f'VIProductVersion "0.2.{version}.0"','VIAddVersionKey "ProductName" "CCB Studio Suite Internal"',
            f'VIAddVersionKey "FileVersion" "0.2.{version}.0"',
            'Name "CCB Studio Suite Internal"','OutFile '+quote(destination/'studio-installer.exe'),
            f'InstallDir "$LOCALAPPDATA\\CCBStudioSuite\\applications\\{version}-{sha[:12]}"',
            'Page directory','Page instfiles','Section "Install"']
    script+=path_preflight(payload)
    script+=['IfFileExists "$INSTDIR" occupied','IfFileExists "$INSTDIR\\*.*" occupied']
    previous=None
    for p in sorted(payload.rglob('*')):
        if not p.is_file():continue
        parent=p.parent.relative_to(payload).as_posix()
        if parent!=previous:
            suffix='' if parent=='.' else '\\'+parent.replace('/','\\')
            if any(x in suffix for x in '$"\r\n'):raise ValueError('Unsafe payload path')
            script+=['SetOutPath "$INSTDIR'+suffix+'"','IfErrors failed'];previous=parent
        script+=['ClearErrors','File '+quote(p),'IfErrors failed']
    script+=['SetOutPath "$INSTDIR"','WriteUninstaller "$INSTDIR\\Uninstall.exe"','IfErrors failed',
             'FileOpen $0 "$INSTDIR\\INSTALL_COMPLETE" w','IfErrors failed',f'FileWrite $0 "{version}:{sha}"','FileClose $0','SetErrorLevel 0','Goto done',
             'occupied:','SetErrorLevel 17','Quit','failed:','SetErrorLevel 18','Quit',
             'path_too_long:','IfSilent +2','MessageBox MB_OK|MB_ICONSTOP "Installation path is too long. Choose a shorter folder. No files were installed."',
             'SetErrorLevel 19','Quit','done:','SectionEnd',
             'UninstPage uninstConfirm','UninstPage instfiles','Section "Uninstall"']
    # Remove only shipped program files; never recursively delete a directory or user data.
    for p in sorted(payload.rglob('*')):
        if p.is_file():script+=['Delete "$INSTDIR\\'+p.relative_to(payload).as_posix().replace('/','\\')+'"']
    script+=['Delete "$INSTDIR\\INSTALL_COMPLETE"','Delete "$INSTDIR\\Uninstall.exe"']
    for p in sorted((p for p in payload.rglob('*') if p.is_dir()),key=lambda p:len(p.parts),reverse=True):
        script+=['RMDir "$INSTDIR\\'+p.relative_to(payload).as_posix().replace('/','\\')+'"']
    script+=['RMDir "$INSTDIR"','SectionEnd']
    nsi=destination/'installer.nsi';nsi.write_text('\n'.join(script)+'\n',encoding='utf-8-sig')
    with (destination/'compile.log').open('wb') as log:
        result=subprocess.run([str(compiler),'/NOCONFIG',str(nsi)],stdout=log,stderr=subprocess.STDOUT)
    report=dict(task='UX-R4',status='BUILT' if result.returncode==0 else 'FAILED',sourceSha=sha,version=version,compiler='NSIS3.13',exitCode=result.returncode,files=len(manifest['files']),installed='NOT TESTED')
    if result.returncode==0:report.update(bytes=(destination/'studio-installer.exe').stat().st_size,sha256=digest(destination/'studio-installer.exe'))
    (destination/'BUILD_RECEIPT.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8');print(json.dumps(report))
    if result.returncode:raise RuntimeError('NSIS compiler failed; inspect retained compile.log')
    return destination/'studio-installer.exe'

if __name__=='__main__':
    p=argparse.ArgumentParser()
    for name in ['app','runtime','bootstrap','destination','build-evidence']:p.add_argument('--'+name,required=True,type=Path)
    p.add_argument('--version',required=True,type=int);p.add_argument('--metadata-url',required=True);p.add_argument('--targets-url',required=True)
    a=p.parse_args();build(a.app,a.runtime,a.bootstrap,a.destination,a.version,a.metadata_url,a.targets_url,a.build_evidence)
