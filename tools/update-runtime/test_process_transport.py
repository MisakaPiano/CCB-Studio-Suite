"""Actual updater CLI/HTTP faults with a fixed internal installer, never execute it.

Only literal loopback and fresh Suite-local fixtures. Test signing keys remain in
memory. This is process/transport evidence, not GUI install/restart acceptance.
"""
import hashlib,json,os,socket,subprocess,sys,threading,time,unittest,uuid
from datetime import datetime,timedelta,timezone
from http.server import BaseHTTPRequestHandler,ThreadingHTTPServer
from pathlib import Path
from securesystemslib.signer import CryptoSigner
from tuf.api.metadata import Metadata,Root,Targets,Snapshot,Timestamp,TargetFile,MetaFile

ROOT=Path(__file__).resolve().parents[2]
OUT=None
BUILD=None

def digest(path):
    with path.open('rb') as stream:return hashlib.file_digest(stream,'sha256').hexdigest()

class ProcessTransportTests(unittest.TestCase):
    def setUp(self):
        self.path=OUT/(self._testMethodName+'-'+uuid.uuid4().hex[:8]);self.path.mkdir()
        self.package=self.path/'package';self.package.mkdir()
        self.cache=self.path/'cache';self.cache.mkdir()
        self.requests=[];self.mode='normal';self.target_started=threading.Event();self.release=threading.Event()
        self.signers={role:CryptoSigner.generate_ed25519() for role in ['root','targets','snapshot','timestamp']}
        self.expiry=datetime.now(timezone.utc)+timedelta(days=1)
        root=Metadata(Root(expires=self.expiry,consistent_snapshot=False))
        for role,signer in self.signers.items():root.signed.add_key(signer.public_key,role)
        root.sign(self.signers['root']);self.bootstrap=root.to_bytes();self.files={}
        fixture=self
        class Handler(BaseHTTPRequestHandler):
            def log_message(self,*args):pass
            def do_GET(self):
                fixture.requests.append(self.path)
                try:
                    if self.path in fixture.files:
                        value=fixture.files[self.path];self.send_response(200);self.send_header('Content-Length',str(len(value)));self.end_headers();self.wfile.write(value)
                    elif self.path=='/targets/studio-installer.exe':
                        source=BUILD/'studio-installer.exe';self.send_response(200);self.send_header('Content-Length',str(source.stat().st_size));self.end_headers()
                        fixture.target_started.set()
                        with source.open('rb') as stream:
                            first=True
                            while chunk:=stream.read(256*1024):
                                if first and fixture.mode=='tamper':chunk=bytes([chunk[0]^1])+chunk[1:]
                                self.wfile.write(chunk);self.wfile.flush()
                                if first and fixture.mode=='truncate':self.close_connection=True;return
                                if first and fixture.mode=='pause':
                                    if not fixture.release.wait(30):return
                                first=False
                    else:self.send_error(404)
                except (BrokenPipeError,ConnectionResetError,ConnectionAbortedError):pass
        self.server=ThreadingHTTPServer(('127.0.0.1',0),Handler)
        self.thread=threading.Thread(target=self.server.serve_forever,daemon=True);self.thread.start()
        self.config=dict(format=1,product='ccb-studio-suite',platform='windows-x86_64',channel='internal',version=3,reader=2,sourceSha='a'*40,
            metadataUrl=f'http://127.0.0.1:{self.server.server_port}/metadata/',targetsUrl=f'http://127.0.0.1:{self.server.server_port}/targets/',rootSha256=hashlib.sha256(self.bootstrap).hexdigest(),internalTest=True)
        (self.package/'update-root.json').write_bytes(self.bootstrap);self.write_config();self.publish()
    def tearDown(self):
        self.release.set();self.server.shutdown();self.server.server_close();self.thread.join(2)
        (self.path/'requests.json').write_text(json.dumps(self.requests,indent=2),encoding='utf-8')
    def write_config(self):
        (self.package/'update-config.json').write_text(json.dumps(self.config),encoding='utf-8')
    def publish(self,version=2,changes=None,bad_signature=False,expired=False):
        receipt=json.loads((BUILD/'BUILD_RECEIPT.json').read_text(encoding='utf-8'))
        custom=dict(product='ccb-studio-suite',platform='windows-x86_64',channel='internal',version=receipt['version'],minimum_reader=2,maximum_reader=3,data_version=3,sourceSha=receipt['sourceSha'],displayVersion='Internal process fixture',notes='Synthetic test repository',installedBytes=300000000)
        custom.update(changes or {})
        target=TargetFile.from_file('studio-installer.exe',str(BUILD/'studio-installer.exe'),['sha256']);target.unrecognized_fields['custom']=custom
        targets=Metadata(Targets(version=version,expires=self.expiry));targets.signed.targets[target.path]=target
        targets.sign(CryptoSigner.generate_ed25519() if bad_signature else self.signers['targets'])
        snapshot=Metadata(Snapshot(version=version,expires=self.expiry));snapshot.signed.meta['targets.json']=MetaFile.from_data(version,targets.to_bytes(),['sha256']);snapshot.sign(self.signers['snapshot'])
        timestamp=Metadata(Timestamp(version=version,expires=datetime.now(timezone.utc)-timedelta(days=1) if expired else self.expiry));timestamp.signed.snapshot_meta=MetaFile.from_data(version,snapshot.to_bytes(),['sha256']);timestamp.sign(self.signers['timestamp'])
        self.files={f'/metadata/{name}.json':value.to_bytes() for name,value in [('targets',targets),('snapshot',snapshot),('timestamp',timestamp)]}
        # Keep only public, signed metadata in evidence; never serialize signers.
        for name,value in self.files.items():(self.path/(str(version)+'-'+name.rsplit('/',1)[1])).write_bytes(value)
    def command(self,download=True):
        return [sys.executable,str(Path(__file__).with_name('studio_update.py')),'--package',str(self.package),'--cache',str(self.cache)]+(['--download'] if download else [])
    def run_cli(self,download=True):
        env=dict(os.environ);env['NO_PROXY']='127.0.0.1';env['no_proxy']='127.0.0.1'
        result=subprocess.run(self.command(download),capture_output=True,env=env,timeout=45,creationflags=subprocess.CREATE_NO_WINDOW)
        identity=str(len(list(self.path.glob('cli-*.json'))))
        value=json.loads(result.stdout.decode('utf-8'));(self.path/f'cli-{identity}.json').write_text(json.dumps(dict(exitCode=result.returncode,result=value,stderr=result.stderr.decode('utf-8',errors='replace')),indent=2),encoding='utf-8')
        return result.returncode,value
    def rejected(self):
        code,result=self.run_cli();self.assertNotEqual(code,0);self.assertEqual(result['status'],'FAILED')
        self.assertFalse(list(self.cache.rglob('VERIFIED.json')));self.assertFalse(list(self.cache.glob('install-*.json')))
        return result
    def test_unreachable_loopback_does_not_download(self):
        with socket.socket() as closed:closed.bind(('127.0.0.1',0));port=closed.getsockname()[1]
        self.config['metadataUrl']=f'http://127.0.0.1:{port}/metadata/';self.write_config();self.rejected();self.assertFalse(self.requests)
    def test_bad_signature_is_rejected_before_payload(self):
        self.publish(bad_signature=True);self.rejected();self.assertNotIn('/targets/studio-installer.exe',self.requests)
    def test_expired_metadata_is_rejected_before_payload(self):
        self.publish(expired=True);self.rejected();self.assertNotIn('/targets/studio-installer.exe',self.requests)
    def test_wrong_platform_is_rejected_before_payload(self):
        self.publish(changes={'platform':'android'});self.rejected();self.assertNotIn('/targets/studio-installer.exe',self.requests)
    def test_cached_metadata_rollback_is_rejected(self):
        code,result=self.run_cli(False);self.assertEqual(code,0);self.assertEqual(result['status'],'AVAILABLE')
        self.publish(version=1);self.rejected();self.assertNotIn('/targets/studio-installer.exe',self.requests)
    def test_tampered_real_installer_has_no_completion(self):
        self.mode='tamper';self.rejected();self.assertTrue(self.target_started.is_set())
    def test_disconnect_then_explicit_retry_verifies_exact_installer(self):
        self.mode='truncate';self.rejected();failed=list((self.cache/'downloads').glob('*.incomplete'));self.assertTrue(failed)
        self.mode='normal';code,result=self.run_cli();self.assertEqual(code,0);self.assertEqual(result['status'],'DOWNLOADED_VERIFIED')
        self.assertEqual(digest(Path(result['installer'])),digest(BUILD/'studio-installer.exe'))
        self.assertTrue(all(p.exists() for p in failed));self.assertFalse(list(self.cache.glob('install-*.json')))
    def test_killed_download_process_releases_cache_and_never_completes(self):
        self.mode='pause';env=dict(os.environ);env['NO_PROXY']='127.0.0.1'
        with (self.path/'cancel-stdout.log').open('wb') as stdout,(self.path/'cancel-stderr.log').open('wb') as stderr:
            child=subprocess.Popen(self.command(),stdout=stdout,stderr=stderr,env=env,creationflags=subprocess.CREATE_NO_WINDOW)
            try:
                self.assertTrue(self.target_started.wait(15));child.terminate();child.wait(10)
                self.assertFalse(list(self.cache.rglob('VERIFIED.json')))
            finally:
                self.release.set()
                if child.poll() is None:child.kill();child.wait(10)
        self.mode='normal';code,result=self.run_cli(False);self.assertEqual(code,0);self.assertEqual(result['status'],'AVAILABLE')
        self.assertEqual(self.requests.count('/targets/studio-installer.exe'),1)

if __name__=='__main__':
    import argparse,platform
    parser=argparse.ArgumentParser();parser.add_argument('--build',required=True,type=Path);args=parser.parse_args();BUILD=args.build.resolve()
    if not BUILD.is_relative_to(ROOT/'.local'):raise ValueError('Only Suite-local internal fixture builds')
    receipt=json.loads((BUILD/'BUILD_RECEIPT.json').read_text(encoding='utf-8'))
    if receipt['status']!='BUILT' or digest(BUILD/'studio-installer.exe')!=receipt['sha256']:raise ValueError('Fixed installer identity mismatch')
    OUT=ROOT/'.local/windows-rc/evidence'/(datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')+'-updater-process-transport');OUT.mkdir(parents=True)
    paths=list(Path(__file__).parent.glob('*.py'));hashes=lambda:{p.relative_to(ROOT).as_posix():digest(p) for p in paths};before=hashes();started=time.monotonic()
    with (OUT/'tests.log').open('w',encoding='utf-8') as log:result=unittest.TextTestRunner(stream=log,verbosity=2).run(unittest.defaultTestLoader.loadTestsFromTestCase(ProcessTransportTests))
    stable=before==hashes();report=dict(status='PASS' if result.wasSuccessful() and stable and not result.skipped else 'FAILED',tests=result.testsRun,failures=len(result.failures),errors=len(result.errors),skipped=len(result.skipped),seconds=time.monotonic()-started,sourceStable=stable,sourceHashes=before,python=sys.version,platform=platform.platform(),installer=receipt,baseCommit=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),actualInstallerExecution='NOT TESTED',realApiCalls=0)
    (OUT/'summary.json').write_text(json.dumps(report,indent=2),encoding='utf-8');print('EVIDENCE='+str(OUT));print((OUT/'tests.log').read_text(encoding='utf-8'));print(json.dumps(report,indent=2))
    raise SystemExit(0 if report['status']=='PASS' else 1)
