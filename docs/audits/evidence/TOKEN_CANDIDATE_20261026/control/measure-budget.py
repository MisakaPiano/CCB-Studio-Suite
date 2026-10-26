from pathlib import Path
import os,stat,json,datetime
ROOT=Path('H:/CCB-Studio-Suite');OUT=Path(__file__).resolve().parent
def size(p):
    todo=[p];total=count=0
    while todo:
        q=todo.pop();s=q.lstat();assert not s.st_file_attributes & stat.FILE_ATTRIBUTE_REPARSE_POINT
        if stat.S_ISDIR(s.st_mode):
            with os.scandir(q) as entries:todo.extend(Path(x.path) for x in entries)
        else:assert stat.S_ISREG(s.st_mode);total+=s.st_size;count+=1
        assert count<20000
    return dict(path=str(p),bytes=total,files=count)
app=size(ROOT/'modules/studio-desktop/build/compose/binaries/main/app/CCBStudioSuitePreview')
runtime=size(ROOT/'.local/candidate-review-20261026/updater-runtime')
compiled=[]
for module in ('asset-library','image-authority','studio-core','studio-desktop'):
    p=ROOT/'modules'/module/'build/classes'/('java' if module=='asset-library' else 'kotlin')/'main';compiled.append(size(p))
previous=(ROOT/'.local/appearance-text-actions-20261026/candidate/studio-installer.exe').stat().st_size
payload=app['bytes']+runtime['bytes']+2*1024**2
estimate=2*app['bytes']+2*payload+(previous*12+9)//10+2*sum(x['bytes'] for x in compiled)+256*1024**2
v=dict(atUtc=datetime.datetime.now(datetime.timezone.utc).isoformat(),app=app,runtime=runtime,compiled=compiled,
    previousInstallerBytes=previous,hBudgetBytes=estimate,cBudgetBytes=805306368,
    calculation='Two full app-image buffers + full payload and isolated installation + previous NSIS size x1.2 + twice all compiled main classes +256MiB safety; existing unrelated module/test/old-candidate output is retained, not duplicated by this app-image-only invocation.',
    reasonForRefinement='No tests or full module output copies are requested. Earlier runner double-counted whole existing build trees including unrelated outputs. This narrower budget is based on measured files and explicitly allows the new copies retained by this task; no deletion/consumption of old image/candidates.',
    notActualPeakMeasurement=True,physicalRamGate='No 2GiB rule; actual Available/commit/overlap and configured JVM limits separately evaluated.')
with (OUT/'BUILD_BUDGET.json').open('x',encoding='utf-8') as f:json.dump(v,f,indent=2);f.write('\n')
print(json.dumps(v),flush=True)
