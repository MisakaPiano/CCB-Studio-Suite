"""One public GET of the already approved frozen ZIP. No new diagnostic execution."""
import datetime,hashlib,io,json,subprocess,urllib.request,zipfile
from pathlib import Path
ROOT=Path('H:/CCB-Studio-Suite')
OUT=ROOT/'handoff/evidence/STARTUP_DIAG_PROJECT_REVIEW_20261025/PUBLIC_ARCHIVE_CHECK.json'
assert not OUT.exists(),'Keep the first readability attempt; never overwrite/retry silently'
COMMIT='31d38532b48e7a94e182f14a90bfbc955d44095b'
PATH='docs/audits/evidence/STARTUP_DIAG_20261025/RAW_EVIDENCE.zip'
URL='https://raw.githubusercontent.com/MisakaPiano/CCB-Studio-Suite/'+COMMIT+'/'+PATH
EXPECTED='a5eb28eb597763a306e558e006270ab9e8b58c3e6aa66929af1bbdba4a7fb99f'
def utc():return datetime.datetime.now(datetime.timezone.utc).isoformat()
record=dict(startedUtc=utc(),url=URL,commit=COMMIT,path=PATH,publicGetAttempts=1,credentialsSupplied=False,scope='Same authorized frozen archive; deliverer verification, not independent Project receipt')
try:
    request=urllib.request.Request(URL,headers={'User-Agent':'Studio-Frozen-Archive-Verification/1.0'})
    with urllib.request.urlopen(request,timeout=30) as response:
        data=response.read(4*1024*1024)
        record.update(httpStatus=response.status,finalUrl=response.url,contentType=response.headers.get('Content-Type'),contentLengthHeader=response.headers.get('Content-Length'))
    actual=hashlib.sha256(data).hexdigest();assert len(data)==1233904 and actual==EXPECTED
    assert data==(ROOT/PATH).read_bytes()
    assert data==subprocess.check_output(['git','show',COMMIT+':'+PATH],cwd=ROOT)
    blob=hashlib.sha1(b'blob '+str(len(data)).encode()+b'\0'+data).hexdigest()
    assert blob=='2932921b12d4699b72b6a466052059d692af41c1'
    with zipfile.ZipFile(io.BytesIO(data)) as archive:
        assert archive.testzip() is None
        manifest=json.loads(archive.read('RAW_MANIFEST.json'));items=manifest['files']
        assert len(items)==55 and len(archive.namelist())==56
        assert len(set(archive.namelist()))==56
        assert set(archive.namelist())=={i['path'] for i in items}|{'RAW_MANIFEST.json'}
        for item in items:
            content=archive.read(item['path'])
            assert len(content)==item['bytes'] and hashlib.sha256(content).hexdigest()==item['sha256']
            fixed=subprocess.check_output(['git','show',COMMIT+':'+str(Path(PATH).parent/item['path']).replace('\\','/')],cwd=ROOT)
            assert fixed==content
    analysis=json.loads((ROOT/Path(PATH).parent/'runs/run-b4de1224/ANALYSIS.json').read_text(encoding='utf-8'))['results']['minimum-jfr']
    windows=analysis['jfrWindows'].values()
    outsideNative=analysis['jfrEventCounts']['jdk.NativeMethodSample']-sum(w['nativeSamples'] for w in windows)
    outsideClasses=analysis['jfrEventCounts']['jdk.ClassLoad']-sum(w['classLoads'] for w in windows)
    assert (outsideNative,outsideClasses)==(44,169)
    record.update(status='PASS',bytes=len(data),sha256=actual,gitBlobSha1=blob,manifestFiles=55,zipMembers=56,crcAndManifestVerified=True,publicBytesEqualLocalAndFixedGit=True,allManifestMembersEqualFixedGit=True,outsideStageWindows=dict(NativeMethodSample=outsideNative,ClassLoad=outsideClasses),archiveModified=False,independentProjectArchiveReceipt='NOT YET OBTAINED')
except BaseException as error:
    record.update(status='FAILED_OR_UNREADABLE',error=repr(error))
    raise
finally:
    record['finishedUtc']=utc();OUT.write_text(json.dumps(record,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(record,indent=2))
