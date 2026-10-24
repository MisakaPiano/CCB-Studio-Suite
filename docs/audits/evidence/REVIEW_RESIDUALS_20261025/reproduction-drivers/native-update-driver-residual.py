import ctypes,hashlib,json,os,shutil,subprocess,sys,time
from pathlib import Path
from ctypes import wintypes
ROOT=Path(__file__).resolve().parents[2]
LAB=Path(json.loads((ROOT/'.local/review-fixes-cleanup-20261024/native-lab.json').read_text())['lab'])
sys.path.insert(0,str(ROOT/'tools/update-runtime'))
from apply_update import create_launcher
from install_ops import atomic_json as _atomic_json,verify_install
from residue_cleanup import enroll_program
evidence_retries=[]
def atomic_json(path,value):
    for attempt in range(6):
        try:return _atomic_json(path,value)
        except PermissionError as error:
            if getattr(error,"winerror",None) not in (32,33) or attempt==5:raise
            evidence_retries.append(dict(file=path.name,attempt=attempt+1,winerror=error.winerror))
            time.sleep(.1*(attempt+1))

def digest(p):
    with p.open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()
def load(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def note(s):print(s,flush=True)
report=dict(status='INCOMPLETE',sourceSha=load(LAB/'candidate/BUILD_RECEIPT.json')['sourceSha'],dailyInstallationModified=False,realProviderCalls=0,normalHumanGuiUpdate='NOT TESTED',scope='Real native A with test-only driver invokes production Workbench drain, prepareUpdateHandoff and close; literal loopback TUF; real native B first-frame verification')
try:
    receipt=load(LAB/'candidate/BUILD_RECEIPT.json');candidate=LAB/'candidate';source=receipt['sourceSha']
    a=LAB/'programs'/('1-'+source[:12]);shutil.copytree(candidate/'payload',a)
    config=load(a/'update-config.json');config.update(version=1,displayVersion='0.2.1-internal');atomic_json(a/'update-config.json',config)
    probe=ROOT/'modules/studio-desktop/build/libs/studio-desktop-review-update-probe.jar';shutil.copyfile(probe,a/'app/review-update-probe.jar')
    cfg=a/'app/CCBStudioSuitePreview.cfg';text=cfg.read_text();text=text.replace('app.mainclass=io.github.misakapiano.ccbstudio.desktop.MainKt','app.mainclass=io.github.misakapiano.ccbstudio.desktop.ReviewUpdateNativeA\napp.classpath=$APPDIR\\review-update-probe.jar');cfg.write_text(text)
    manifest=load(a/'INSTALL_MANIFEST.json');manifest.update(version=1,files=[])
    for p in sorted(a.rglob('*')):
        if p.is_file() and p.name not in ('INSTALL_MANIFEST.json','INSTALL_COMPLETE'):
            manifest['files'].append(dict(path=p.relative_to(a).as_posix(),bytes=p.stat().st_size,sha256=digest(p)))
    atomic_json(a/'INSTALL_MANIFEST.json',manifest);(a/'INSTALL_COMPLETE').write_text('1:'+source)
    expected={k:config[k] for k in ('product','platform','channel','version','sourceSha')};verification=verify_install(a,expected)
    atomic_json(LAB/'A_INPUT_MAP.json',dict(kind='TEST_ONLY_NATIVE_A',productionSource=source,main='ReviewUpdateNativeA (test JAR only)',probeJarSha256=digest(probe),manifestSha256=digest(a/'INSTALL_MANIFEST.json'),executableSha256=digest(a/'CCBStudioSuitePreview.exe'),verification=verification,publicRelease=False))
    create_launcher(a,LAB/'data',LAB/'runtime')
    # A is an explicitly owned synthetic native fixture, not an unproven legacy
    # user installation. This receipt models an A installed by the new installer.
    enroll_program(LAB/'runtime/updates',a.parent,a,expected,receipt['sha256'],LAB/'runtime/updates/install-A-fixture.json')
    (a/'UNKNOWN_USER_SENTINEL.txt').write_bytes(b'unknown files must survive owned retirement')
    atomic_json(LAB/'repository/publish.json',dict(build=str(candidate)))
    deadline=time.monotonic()+20
    while not (LAB/'repository/PUBLISHED.json').exists():
        if time.monotonic()>deadline:raise RuntimeError('Loopback publication timeout')
        time.sleep(.1)
    with (LAB/'a-process.log').open('wb') as log:
        process=subprocess.Popen([str(a/'CCBStudioSuitePreview.exe'),str(LAB)],cwd=a,stdout=log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
    atomic_json(LAB/'A_STARTED.json',dict(pid=process.pid,executable=str(a/'CCBStudioSuitePreview.exe')));note('A_STARTED '+str(process.pid))
    deadline=time.monotonic()+180
    while not (LAB/'A_HANDOFF_READY.json').exists():
        if (LAB/'A_FAILED.json').exists():raise RuntimeError('A failed: '+(LAB/'A_FAILED.json').read_text())
        if process.poll() is not None:raise RuntimeError('A exited before handoff: '+(LAB/'a-process.log').read_text(errors='replace')[-1500:])
        if time.monotonic()>deadline:raise RuntimeError('A handoff timeout')
        time.sleep(.2)
    ready=load(LAB/'A_HANDOFF_READY.json');plan_path=Path(ready['plan']);plan=load(plan_path);folder=plan_path.parent
    assert ready['parentPid']==plan['parentPid']==load(LAB/'A_DRAIN_WAITING.json')['pid']
    assert Path(load(LAB/'A_DRAIN_WAITING.json')['command'])==a/'CCBStudioSuitePreview.exe'
    assert load(LAB/'A_DRAIN_WAITING.json')['generationSends']==1
    assert process.wait(timeout=20)==0
    assert (LAB/'A_NORMAL_EXIT_READY.json').exists();note('A_NORMAL_EXIT')
    deadline=time.monotonic()+150
    while True:
        value=load(folder/'result.json') if (folder/'result.json').exists() else {}
        if value.get('status')=='FAILED_RETAINED':raise RuntimeError('B handoff failed: '+str(value.get('reason')))
        if value.get('status')=='COMPLETE':break
        if time.monotonic()>deadline:raise RuntimeError('B completion timeout')
        time.sleep(.2)
    ack=load(folder/'B_VERIFIED.json');assert ack['snapshot']==ready['snapshot']==plan['snapshot']
    creds=ack['snapshot']['credentials'];assert len(creds)==2 and creds[0].get('sha256') and not creds[0].get('status')
    assert len(ack['snapshot']['assets'])>=3
    b=Path(value['installed']);assert load(b/'update-config.json')['sourceSha']==source
    assert Path(load(a.parent/'launch-current.json')['installed'])==b
    atomic_json(LAB/'A_TO_B_RESULT.json',dict(helper=value,b=ack,a=ready,parentImageAndDrain='REAL_NATIVE_PROCESS_NO_PARENT_STUB',syntheticCredentialCount=len(creds),credentialSnapshotMatched=True));note('B_COMPLETE '+str(b))
    deadline=time.monotonic()+45
    while (a/'CCBStudioSuitePreview.exe').exists() or any((LAB/'runtime/updates/downloads').rglob('studio-installer.exe')):
        if time.monotonic()>deadline:raise RuntimeError('Post-COMPLETE owned program/download cleanup did not finish')
        time.sleep(.25)
    assert (a/'UNKNOWN_USER_SENTINEL.txt').read_bytes()==b'unknown files must survive owned retirement'
    atomic_json(LAB/'CLEANUP_ASSERTIONS.json',dict(status='PASS',obsoleteProgramExeRemoved=True,ownedDownloadRemoved=True,unknownFileRetained=True,successCondition='COMPLETE before cleanup',waitDays=0))
    # Send the normal close message only to windows owned by this exact B image.
    user=ctypes.WinDLL('user32',use_last_error=True);kernel=ctypes.WinDLL('kernel32',use_last_error=True)
    kernel.OpenProcess.argtypes=[wintypes.DWORD,wintypes.BOOL,wintypes.DWORD];kernel.OpenProcess.restype=wintypes.HANDLE;kernel.CloseHandle.argtypes=[wintypes.HANDLE]
    kernel.QueryFullProcessImageNameW.argtypes=[wintypes.HANDLE,wintypes.DWORD,wintypes.LPWSTR,ctypes.POINTER(wintypes.DWORD)]
    user.GetWindowThreadProcessId.argtypes=[wintypes.HWND,ctypes.POINTER(wintypes.DWORD)];user.PostMessageW.argtypes=[wintypes.HWND,wintypes.UINT,wintypes.WPARAM,wintypes.LPARAM]
    user.IsWindowVisible.argtypes=[wintypes.HWND];user.GetClassNameW.argtypes=[wintypes.HWND,wintypes.LPWSTR,ctypes.c_int]
    closed=[];callback_type=ctypes.WINFUNCTYPE(wintypes.BOOL,wintypes.HWND,wintypes.LPARAM)
    def callback(hwnd,_):
        klass=ctypes.create_unicode_buffer(256);user.GetClassNameW(hwnd,klass,len(klass))
        if not user.IsWindowVisible(hwnd) or klass.value!='SunAwtFrame':return True
        pid=wintypes.DWORD();user.GetWindowThreadProcessId(hwnd,ctypes.byref(pid));h=kernel.OpenProcess(0x1000,False,pid.value)
        if not h:return True
        try:
            text=ctypes.create_unicode_buffer(32768);length=wintypes.DWORD(len(text))
            if kernel.QueryFullProcessImageNameW(h,0,text,ctypes.byref(length)) and Path(text.value)==b/'CCBStudioSuitePreview.exe':
                if user.PostMessageW(hwnd,0x10,0,0):closed.append(dict(pid=pid.value,window=int(hwnd)))
        finally:kernel.CloseHandle(h)
        return True
    cb=callback_type(callback);user.EnumWindows.argtypes=[callback_type,wintypes.LPARAM]
    deadline=time.monotonic()+15
    while not closed and time.monotonic()<deadline:user.EnumWindows(cb,0);time.sleep(.2)
    atomic_json(LAB/'B_NORMAL_CLOSE_REQUEST.json',dict(method='WM_CLOSE only to visible main SunAwtFrame owned by exact B executable; hidden toolkit windows excluded',windows=closed))
    kernel.WaitForSingleObject.argtypes=[wintypes.HANDLE,wintypes.DWORD];kernel.WaitForSingleObject.restype=wintypes.DWORD
    exits=[]
    for pid in {x['pid'] for x in closed}:
        h=kernel.OpenProcess(0x100000|0x1000,False,pid)
        if not h:exits.append(dict(pid=pid,alreadyExited=True));continue
        try:exits.append(dict(pid=pid,waitResult=kernel.WaitForSingleObject(h,30000)))
        finally:kernel.CloseHandle(h)
    atomic_json(LAB/'B_EXIT_OBSERVATION.json',dict(exits=exits))
    assert closed and all(x.get('alreadyExited') or x.get('waitResult')==0 for x in exits),'B visible window did not close normally'
    report.update(status='PASS',aPid=ready['parentPid'],launcherShimPid=process.pid,aExitCode=process.returncode,aDrain='PRODUCTION_WORKBENCH_WAITED_FOR_SLOW_RESPONSE',aParent='REAL_WINDOWS_IMAGE_BOUND_HANDLE',bVerification='PROGRAM_DATA_AND_CREDENTIALS_MATCH',credentials=len(creds),generationSends=ready['generationSends'],assets=len(ack['snapshot']['assets']),installerSha256=receipt['sha256'],installerBytes=receipt['bytes'],bClose='VISIBLE_MAIN_WINDOW_CLOSED_NORMALLY',ownedCleanup='IMMEDIATE_COMPLETE_NO_WAIT_DAYS',aInputMap='A_INPUT_MAP.json',release='NOT_PUBLISHED')
except BaseException as e:
    report.update(status='FAILED',reason=type(e).__name__,detail=str(e)[:2000]);raise
finally:
    report['testEvidenceWriteRetries']=evidence_retries
    atomic_json(LAB/'NATIVE_UPDATE_SUMMARY.json',report)
    # Test windows belong to the driver even on assertion failure. Preserve all
    # evidence first and close only the exact visible synthetic B main window.
    if (LAB/'candidate/BUILD_RECEIPT.json').is_file():
        try:subprocess.run([sys.executable,'-B',str(ROOT/'.local/review-fixes-cleanup-20261024/close-native-lab.py')],timeout=40,check=True)
        except Exception as close_error:atomic_json(LAB/'WINDOW_CLEANUP_ERROR.json',dict(reason=type(close_error).__name__))
    (LAB/'repository/STOP').write_text('Stop isolated test server normally')
    note(json.dumps(report,indent=2))
