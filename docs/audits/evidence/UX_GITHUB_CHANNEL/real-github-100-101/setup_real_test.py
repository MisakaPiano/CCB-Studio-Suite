import hashlib
import json
import shutil
import subprocess
import sys
import urllib.request
from pathlib import Path

workspace = Path('H:/CCB-Studio-Suite')
sys.path.insert(0, str(workspace / 'tools/update-runtime'))
from install_ops import verify_install
folder = Path(__file__).resolve().parent
run = workspace / '.local/windows-rc/github-real-100-101'
run.mkdir(exist_ok=True)
spec = json.loads((folder / 'publication-review/PUBLICATION_MANIFEST.json').read_text())['installers'][0]
url = 'https://github.com/MisakaPiano/CCB-Studio-Suite-Releases/releases/download/' + spec['tag'] + '/' + spec['asset']
exe = run / spec['asset']
if not exe.exists():
    stage = run / (spec['asset'] + '.partial')
    assert not stage.exists(), 'Retain partial download for inspection'
    with urllib.request.urlopen(url, timeout=90) as source, stage.open('xb') as target:
        assert source.status == 200
        shutil.copyfileobj(source, target, 1024 * 1024)
    assert stage.stat().st_size == spec['bytes']
    assert hashlib.sha256(stage.read_bytes()).hexdigest() == spec['sha256']
    stage.rename(exe)
assert hashlib.sha256(exe.read_bytes()).hexdigest() == spec['sha256']
app = run / 'applications/100-A'
assert not app.exists(), 'Existing install retained; do not rerun setup'
result = subprocess.run([str(exe), '/S', '/D=' + str(app)], creationflags=subprocess.CREATE_NO_WINDOW, timeout=120)
assert result.returncode == 0
verified = verify_install(app, json.loads((app / 'update-config.json').read_text()))
source = workspace / '.local/windows-rc/ab-data-5/backups/11d26e91-ed10-44e7-8b75-c18ea34fb14b'
data = run / 'synthetic-workspace'
assert not data.exists()
before = {p.relative_to(source).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest() for p in source.rglob('*') if p.is_file()}
shutil.copytree(source, data)
after = {p.relative_to(data).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest() for p in data.rglob('*') if p.is_file()}
assert before == after
runtime = run / 'runtime'
runtime.mkdir()
report = {'status': 'REAL_PUBLIC_HTTPS_A_DOWNLOAD_INSTALL_VERIFIED', 'downloadUrl': url, 'sha256': spec['sha256'], 'bytes': spec['bytes'], 'installed': verified, 'app': str(app), 'dataRoot': str(data), 'runtimeRoot': str(runtime), 'fixtureKind': 'OFFLINE_SYNTHETIC_BACKUP_COPY_NOT_RESTORE_TEST', 'fixtureSource': str(source), 'fixtureFileHashes': before, 'credentials': 'BACKUP_EXCLUDES_CREDENTIALS; MISSING_REFERENCE_EXPECTED', 'restart': 'NOT_YET_TESTED'}
(run / 'SETUP.json').write_text(json.dumps(report, indent=2), encoding='utf-8')
print(json.dumps({k: v for k, v in report.items() if k != 'fixtureFileHashes'}), flush=True)
