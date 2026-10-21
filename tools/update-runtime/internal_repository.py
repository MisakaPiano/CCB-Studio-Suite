"""Internal A-to-B transport only: loopback server, memory-only disposable keys.

Publishes only a supplied verified build and public metadata. No private key export,
no TLS/system trust changes, and no access to application data or credentials.
"""
import argparse,hashlib,json,sys,threading,time
from datetime import datetime,timedelta,timezone
from http.server import BaseHTTPRequestHandler,HTTPServer
from pathlib import Path
from securesystemslib.signer import CryptoSigner
from tuf.api.metadata import Metadata,Root,Targets,Snapshot,Timestamp,TargetFile,MetaFile

def serve(folder):
    suite=Path(__file__).resolve().parents[2]
    folder=folder.resolve()
    if not folder.is_relative_to(suite/'.local') or folder.exists():raise ValueError('Fresh Suite-local evidence directory required')
    folder.mkdir(parents=True)
    signers={role:CryptoSigner.generate_ed25519() for role in ['root','targets','snapshot','timestamp']}
    expiry=datetime.now(timezone.utc)+timedelta(days=1)
    root=Metadata(Root(expires=expiry,consistent_snapshot=False))
    for role,signer in signers.items():root.signed.add_key(signer.public_key,role)
    root.sign(signers['root']);(folder/'root.json').write_bytes(root.to_bytes())
    published={};installer=None
    class Handler(BaseHTTPRequestHandler):
        def log_message(self,*args):pass
        def do_GET(self):
            with (folder/'requests.jsonl').open('a',encoding='utf-8') as log:log.write(json.dumps({'time':time.time(),'path':self.path})+'\n')
            if self.path in published:
                data=published[self.path];self.send_response(200);self.send_header('Content-Length',str(len(data)));self.end_headers();self.wfile.write(data)
            elif self.path=='/targets/studio-installer.exe' and installer:
                self.send_response(200);self.send_header('Content-Length',str(installer.stat().st_size));self.end_headers()
                with installer.open('rb') as stream:
                    while chunk:=stream.read(1024*1024):self.wfile.write(chunk)
            else:self.send_error(404)
    server=HTTPServer(('127.0.0.1',0),Handler)
    (folder/'server.json').write_text(json.dumps({'host':'127.0.0.1','port':server.server_port,'privateKeys':'MEMORY_ONLY','rootSha256':hashlib.sha256(root.to_bytes()).hexdigest()}),encoding='utf-8')
    threading.Thread(target=server.serve_forever,daemon=True).start()
    try:
        while not (folder/'STOP').exists():
            request=folder/'publish.json'
            if request.exists() and installer is None:
                value=json.loads(request.read_text(encoding='utf-8'));candidate=Path(value['build']).resolve()
                if not candidate.is_relative_to(suite/'.local'):raise ValueError('Build outside isolated Suite directory')
                receipt=json.loads((candidate/'BUILD_RECEIPT.json').read_text(encoding='utf-8'))
                file=candidate/'studio-installer.exe'
                if receipt['status']!='BUILT' or hashlib.file_digest(file.open('rb'),'sha256').hexdigest()!=receipt['sha256']:raise ValueError('Build receipt mismatch')
                manifest=json.loads((candidate/'payload/INSTALL_MANIFEST.json').read_text(encoding='utf-8'))
                custom=dict(product='ccb-studio-suite',platform='windows-x86_64',channel='internal',version=receipt['version'],minimum_reader=2,maximum_reader=2,sourceSha=receipt['sourceSha'],displayVersion=f"0.2.{receipt['version']}-internal",notes='隔离内部真实应用更新；保留原件、工程、历史和测试凭据。',installedBytes=sum(x['bytes'] for x in manifest['files']))
                target=TargetFile.from_file('studio-installer.exe',str(file),['sha256']);target.unrecognized_fields['custom']=custom
                targets=Metadata(Targets(version=2,expires=expiry));targets.signed.targets['studio-installer.exe']=target;targets.sign(signers['targets'])
                snapshot=Metadata(Snapshot(version=2,expires=expiry));snapshot.signed.meta['targets.json']=MetaFile.from_data(2,targets.to_bytes(),['sha256']);snapshot.sign(signers['snapshot'])
                timestamp=Metadata(Timestamp(version=2,expires=expiry));timestamp.signed.snapshot_meta=MetaFile.from_data(2,snapshot.to_bytes(),['sha256']);timestamp.sign(signers['timestamp'])
                for name,metadata in [('targets',targets),('snapshot',snapshot),('timestamp',timestamp)]:
                    data=metadata.to_bytes();(folder/(name+'.json')).write_bytes(data);published['/metadata/'+name+'.json']=data
                installer=file
                (folder/'PUBLISHED.json').write_text(json.dumps(custom,ensure_ascii=False,indent=2),encoding='utf-8')
            time.sleep(.2)
    finally:server.shutdown();server.server_close()

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--folder',required=True,type=Path);serve(p.parse_args().folder)
