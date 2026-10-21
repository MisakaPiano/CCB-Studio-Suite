from pathlib import Path
import hashlib,json,subprocess
r=Path.cwd();lock=json.loads((r/'modules/image-authority/SOURCE_MANIFEST.json').read_text(encoding='utf-8'))
specs=[lock['commit']+':'+f['source'] for f in lock['files']]
raw=subprocess.check_output(['git','--no-optional-locks','-C','H:/ChatChatBar-Desktop','cat-file','--batch'],input=('\n'.join(specs)+'\n').encode())
offset=0;rows=[]
for entry in lock['files']:
    end=raw.index(b'\n',offset);header=raw[offset:end].decode().split();size=int(header[2]);offset=end+1
    source=raw[offset:offset+size];offset+=size+1;working=(r/entry['path']).read_bytes()
    source_hash=hashlib.sha256(source).hexdigest();actual_hash=hashlib.sha256(working).hexdigest()
    rows.append(dict(path=entry['path'],sourceSha256=source_hash,workingSha256=actual_hash,expected=entry['sha256'],status='PASS' if source_hash==actual_hash==entry['sha256'] else 'FAILED'))
report=dict(status='PASS' if all(x['status']=='PASS' for x in rows) else 'FAILED',referenceCommit=lock['commit'],files=rows,referenceModified=False)
(r/'.local/windows-rc/authority-current.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
print(json.dumps(dict(status=report['status'],referenceCommit=lock['commit'],files=len(rows))))
raise SystemExit(0 if report['status']=='PASS' else 1)
