"""Two real Windows jpackage cwd cases, not a full signed/NSIS update test.

Supply a NEW workspace_artifacts OWNER root, retained baseline app-image and JDK.
The legacy call models the old helper's inherited cwd; fixed call uses production
start_b. Synthetic terminal receipt is supplied by this driver, not real data ACK.
"""
import argparse, hashlib, json, os, shutil, subprocess, sys, time, traceback
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
sys.path[:0]=[str(ROOT/'tools'),str(ROOT/'tools/update-runtime')]
import workspace_artifacts as artifacts
from apply_update import start_b,start_cleanup,ParentProcess
from install_ops import atomic_json,verify_install
from residue_cleanup import enroll_program,run

def wait(predicate,message,seconds=20):
    deadline=time.monotonic()+seconds
    while not predicate():
        if time.monotonic()>deadline:raise RuntimeError(message)
        time.sleep(.05)

def digest(path):return hashlib.sha256(path.read_bytes()).hexdigest()

def main():
    p=argparse.ArgumentParser();p.add_argument('--owner',required=True,type=Path);p.add_argument('--baseline',required=True,type=Path);p.add_argument('--jdk',required=True,type=Path);p.add_argument('--out',required=True,type=Path);a=p.parse_args()
    owner,_=artifacts.owner(a.owner);work=owner/'payload';out=artifacts.safe(a.out);out.mkdir(exist_ok=False)
    if any(work.iterdir()):raise ValueError('Fresh payload required')
    env=dict(os.environ)
    for key in ('USERPROFILE','HOME','APPDATA','LOCALAPPDATA','TEMP','TMP'):
        folder=work/key;folder.mkdir();env[key]=str(folder)
    env['JAVA_TOOL_OPTIONS']='-Duser.home='+env['HOME']
    source=ROOT/'modules/studio-desktop/src/main/java/io/github/misakapiano/ccbstudio/desktop/UpdateLaunchDirectory.java'
    jna=next((a.baseline/'app').glob('jna-5.*.jar'));classes=work/'classes';classes.mkdir()
    build=[str(a.jdk/'bin/javac.exe'),'-encoding','UTF-8','--release','17','-cp',str(jna),'-d',str(classes),str(source),str(ROOT/'tools/update-runtime/fixtures/LaunchDirectoryProbe.java')]
    with (out/'javac.log').open('xb') as log:subprocess.run(build,stdout=log,stderr=subprocess.STDOUT,env=env,check=True,timeout=30)
    jar=work/'probe.jar';subprocess.run([str(a.jdk/'bin/jar.exe'),'--create','--file',str(jar),'-C',str(classes),'.'],env=env,check=True,timeout=20,capture_output=True)
    worker_source=work/'Worker.cs';worker_source.write_text('using System;using System.IO;class Worker{static int Main(string[] a){for(int i=0;i+1<a.Length;i++)if(a[i]=="--cache")File.WriteAllText(Path.Combine(a[i+1],"worker-cwd.txt"),Environment.CurrentDirectory);return 0;}}')
    worker=work/'worker.exe'
    subprocess.run([r'C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe','/nologo','/target:exe','/out:'+str(worker),str(worker_source)],check=True,capture_output=True,timeout=30)
    results=[]
    for index,label in enumerate(('legacy-inherited-cwd','fixed-explicit-cwd')):
        case=work/str(index);root=case/'programs';data=case/'data';runtime=case/'r';cache=runtime/'updates';folder=cache/'handoffs/probe'
        for directory in (root,data,folder):directory.mkdir(parents=True)
        (data/'synthetic-original.bin').write_bytes(b'unchanged synthetic original')
        old=root/('1-'+'a'*12);current=root/('2-'+'b'*12)
        for directory in (old,current):(directory/'app').mkdir(parents=True)
        (old/'CCBStudioSuitePreview.exe').write_bytes(b'old synthetic program');(old/'app/main.jar').write_bytes(b'old synthetic jar')
        shutil.copy2(a.baseline/'CCBStudioSuitePreview.exe',current/'CCBStudioSuitePreview.exe')
        shutil.copytree(a.baseline/'runtime',current/'runtime');shutil.copy2(jna,current/'app/jna.jar');shutil.copy2(jar,current/'app/probe.jar')
        (current/'app/CCBStudioSuitePreview.cfg').write_text('[Application]\napp.mainclass=LaunchDirectoryProbe\napp.classpath=$APPDIR\\probe.jar\napp.classpath=$APPDIR\\jna.jar\n[JavaOptions]\njava-options=-Xmx64m\njava-options=-XX:ActiveProcessorCount=2\n')
        (current/'updater/lib').mkdir(parents=True);shutil.copy2(worker,current/'updater/pythonw.exe');(current/'updater/lib/residue_cleanup.py').write_text('# native cwd worker probe only')
        identities=[]
        installer_hash=hashlib.sha256(b'synthetic installer').hexdigest()
        for version,letter,directory in ((1,'a',old),(2,'b',current)):
            expected=dict(product='ccb-studio-suite',platform='windows-x86_64',channel='internal',version=version,sourceSha=letter*40);identities.append(expected)
            files=[dict(path=f.relative_to(directory).as_posix(),bytes=f.stat().st_size,sha256=digest(f)) for f in directory.rglob('*') if f.is_file()]
            atomic_json(directory/'INSTALL_MANIFEST.json',dict(format=1,**expected,files=files));(directory/'INSTALL_COMPLETE').write_text(f'{version}:{letter*40}')
            enroll_program(cache,root,directory,expected,installer_hash,cache/'install.json',now=1000)
        journal=cache/'install.json';atomic_json(journal,dict(status='INSTALLED_NOT_STARTED',target=str(current),expected=identities[1],verification=verify_install(current,identities[1])))
        plan=folder/'plan.json';atomic_json(plan,dict(format=1,nonce='b'*64,installed=str(old),cache=str(cache),approved=dict(**identities[1],sha256=installer_hash)))
        receipt=dict(status='STARTING_B',nonce='b'*64,installed=str(current),installJournal=str(journal),verification='B_PROGRAM_DATA_AND_CREDENTIAL_SNAPSHOT_MATCH')
        atomic_json(folder/'result.json',receipt);atomic_json(root/'launch-current.json',dict(installed=str(current),data=str(data),runtime=str(runtime)))
        handle=None;child=None;row=dict(case=label,status='FAILED')
        try:
            if index==0:
                with (folder/'b-stdout.log').open('wb') as stdout,(folder/'b-stderr.log').open('wb') as stderr:
                    child=subprocess.Popen([str(current/'CCBStudioSuitePreview.exe'),'--studio-root',str(data),'--runtime-root',str(runtime),'--update-handoff',str(plan)],cwd=old,env=env,stdout=stdout,stderr=stderr,creationflags=subprocess.CREATE_NO_WINDOW)
            else:
                # Child inherits no private environment; use isolated variables for this call.
                prior=dict(os.environ);os.environ.clear();os.environ.update(env)
                try:child=start_b(current,data,runtime,plan)
                finally:os.environ.clear();os.environ.update(prior)
            wait(lambda:(folder/'READY').exists(),'Native B did not become ready')
            lines=(folder/'READY').read_text().splitlines();pid=int(lines[0]);handle=ParentProcess(pid,current/'CCBStudioSuitePreview.exe')
            assert Path(lines[1])==current
            time.sleep(.3);assert child.poll() is None,'Original host exited before terminal helper receipt'
            receipt['status']='COMPLETE';atomic_json(folder/'result.json',receipt);before=(folder/'result.json').read_bytes()
            if index==0:assert child.wait(timeout=15)==0
            value=run(current,cache,data,runtime,now=1001)
            assert value['status']=='COMPLETE' and not old.exists() and not handle.exited()
            assert (folder/'result.json').read_bytes()==before
            assert (data/'synthetic-original.bin').read_bytes()==b'unchanged synthetic original'
            worker_child=start_cleanup(current,cache,data,runtime);assert worker_child.wait(timeout=10)==0
            assert Path((cache/'worker-cwd.txt').read_text())==current
            assert run(current,cache,data,runtime,now=1002)['removed']==0
            row.update(status='PASS',newVmPid=pid,newCwd=lines[1],oldRootAbsentWhileBRunning=True,cleanupWorkerCwd=str(current),cleanup=value,receiptUnchanged=True,repeatRemoved=0)
        except Exception:row['error']=traceback.format_exc();raise
        finally:
            (folder/'STOP').write_text('Normal synthetic shutdown')
            if handle:
                wait(handle.exited,'Native probe did not stop normally',15);handle.close()
            if child:child.wait(timeout=15)
            row['normalShutdown']=True;results.append(row)
            for name in ('b-stdout.log','b-stderr.log'):
                if (folder/name).exists():shutil.copy2(folder/name,out/(label+'-'+name))
            (out/'cases.json').write_text(json.dumps(results,indent=2)+'\n')
    (out/'inputs.json').write_text(json.dumps(dict(javaSourceSha256=digest(source),baselineNativeExeSha256=digest(a.baseline/'CCBStudioSuitePreview.exe'),jnaSha256=digest(jna),commands=[build],scope='NATIVE_PROCESS_DIRECTORY_ONLY_NO_TUF_NSIS_OR_REAL_DATA'),indent=2)+'\n')
    print(json.dumps(results,indent=2))

if __name__=='__main__':main()
