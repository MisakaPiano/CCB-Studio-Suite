"""Actual NSIS extraction boundary regression; synthetic files, Suite-local only."""
import hashlib, importlib.util, json, subprocess, uuid, zipfile
from pathlib import Path
root=Path.cwd()
spec=importlib.util.spec_from_file_location('builder',root/'tools/Build-InternalInstaller.py')
builder=importlib.util.module_from_spec(spec);spec.loader.exec_module(builder)
out=root/'.local/windows-rc/evidence'/('installer-path-guard-'+uuid.uuid4().hex)
out.mkdir(parents=True)
compiler=root/'.local/windows-rc/nsis-tools/nsis-3.13/makensis.exe'
archive=root/'.local/windows-rc/updater-downloads/nsis-3.13-verified.zip'
assert builder.digest(archive)=='ba63dffc4410ee89193e1cb5a41989991bd77c61068da17e3156d136b7b0b3d8'
with zipfile.ZipFile(archive) as z:
    for entry in z.infolist():
        if not entry.is_dir():assert (compiler.parent.parent/entry.filename).read_bytes()==z.read(entry)
payload=out/'payload';(payload/'app').mkdir(parents=True)
name='x'*76+'.jar';(payload/'app'/name).write_bytes(b'SYNTHETIC PATH BOUNDARY\n')
nsi=out/'probe.nsi';exe=out/'probe.exe'
lines=['Unicode true','RequestExecutionLevel user','OutFile '+builder.quote(exe),'Section']
lines+=builder.path_preflight(payload)
lines+=['IfFileExists "$INSTDIR" occupied','SetOutPath "$INSTDIR\\app"','File '+builder.quote(payload/'app'/name),
        'IfErrors failed','SetErrorLevel 0','Goto done','path_too_long:','SetErrorLevel 19','Quit',
        'occupied:','SetErrorLevel 17','Quit','failed:','SetErrorLevel 18','Quit','done:','SectionEnd']
nsi.write_text('\n'.join(lines)+'\n',encoding='utf-8-sig')
with (out/'compile.log').open('wb') as log:
    subprocess.run([str(compiler),'/NOCONFIG',str(nsi)],stdout=log,stderr=subprocess.STDOUT,check=True)
units=lambda s:len(str(s).encode('utf-16-le'))//2
base=root/'.local/windows-rc'/('pg-'+uuid.uuid4().hex[:8]);base.mkdir()
def destination(length,prefix):
    p=base/prefix
    return p.with_name(prefix+'x'*(length-units(p)))
max_root=259-1-units('app/'+name)
cases=[('boundary',destination(max_root,'ok-'),0),('too-long',destination(max_root+1,'long-'),19),
       ('unicode-too-long',destination(max_root+1,'\U0001f7e6-'),19),('occupied',base/'existing',17)]
(base/'existing').mkdir();(base/'existing/sentinel').write_bytes(b'PRESERVE')
results=[]
try:
    for label,target,expected in cases:
        before={p.relative_to(target).as_posix():builder.digest(p) for p in target.rglob('*') if p.is_file()} if target.exists()else None
        code=subprocess.run([str(exe),'/S','/D='+str(target)],creationflags=subprocess.CREATE_NO_WINDOW,timeout=30).returncode
        assert code==expected,(label,code,expected)
        if code==0:assert (target/'app'/name).read_bytes()==(payload/'app'/name).read_bytes()
        elif before is None:assert not target.exists(),label
        else:assert before=={p.relative_to(target).as_posix():builder.digest(p) for p in target.rglob('*') if p.is_file()}
        results.append(dict(case=label,status='PASS',exitCode=code,destinationUtf16Units=units(target)))
    status='PASS'
except Exception as e:
    status='FAILED';results.append(dict(error=str(e)));raise
finally:
    report=dict(status=status,cases=results,sourceSha256=builder.digest(root/'tools/Build-InternalInstaller.py'),
                scope='ACTUAL_NSIS_SYNTHETIC_EXTRACTION; not full application installer',compiler='NSIS 3.13',evidence=str(out))
    (out/'result.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(report))
