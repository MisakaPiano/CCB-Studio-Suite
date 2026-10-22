"""Automatic installer checks only; no application launch or user-data access."""
import hashlib, json, os, subprocess, sys, time
from pathlib import Path

root = Path(__file__).resolve().parents[2]
build = root / '.local/windows-rc/fixed-twelve-3bea16b-build'
out = root / '.local/windows-rc/evidence/fixed-twelve-3bea16b-install-retry'
destination = root / '.local/windows-rc/fixed-twelve-applications/12-3bea16b7405d'
assert root == Path('H:/CCB-Studio-Suite')
assert not out.exists() and not destination.exists()
assert destination.resolve().is_relative_to(root / '.local/windows-rc')
out.mkdir(parents=True)
receipt = json.loads((build / 'BUILD_RECEIPT.json').read_text())
installer = build / 'studio-installer.exe'
digest = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
assert receipt['sourceSha'] == '3bea16b7405d1c1890d840e76ff3dee759faad5c'
assert receipt['status'] == 'BUILT' and digest(installer) == receipt['sha256']
expected = json.loads((build / 'payload/update-config.json').read_text())
sys.path.insert(0, str(root / 'tools/update-runtime'))
sys.path.insert(0, str(root / '.local/windows-rc/updater-runtime-v7/lib'))
from install_ops import verify_install

report = dict(installer=receipt, destination=str(destination), results=[], applicationLaunched=False,
              userDataAccessed=False, humanAcceptance='NOT TESTED', actualUpdate='NOT TESTED')
def save():
    (out / 'receipt.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
def check(name, target, expected_code):
    target = target.resolve()
    assert target.is_relative_to(root / '.local/windows-rc')
    start = time.monotonic()
    with (out / (name + '.log')).open('wb') as log:
        result = subprocess.run([str(installer), '/S', '/D=' + str(target)],
                                stdout=log, stderr=subprocess.STDOUT,
                                creationflags=subprocess.CREATE_NO_WINDOW, timeout=120)
    item = dict(test=name, exitCode=result.returncode, expected=expected_code,
                seconds=round(time.monotonic()-start, 3),
                status='PASS' if result.returncode==expected_code else 'FAILED')
    report['results'].append(item)
    save()
    assert result.returncode == expected_code, item
    return item

long_parent = root / '.local/windows-rc/fixed-twelve-path-guard'
assert not long_parent.exists()
check('LONG_PATH_REFUSE_BEFORE_WRITES', long_parent / ('a'*170) / ('b'*50), 19)
assert not long_parent.exists()
item = check('FRESH_INSTALL', destination, 0)
item['verification'] = verify_install(destination, expected)
save()
before = {p.relative_to(destination).as_posix():digest(p) for p in destination.rglob('*') if p.is_file()}
item = check('OCCUPIED_REFUSE', destination, 17)
item['verification'] = verify_install(destination, expected)
after = {p.relative_to(destination).as_posix():digest(p) for p in destination.rglob('*') if p.is_file()}
assert before == after
report['occupiedFilesUnchanged'] = len(before)
assert (destination / 'INSTALL_COMPLETE').read_text() == '12:' + receipt['sourceSha']
report['completionMarkerMatches'] = True
save()
print(json.dumps(report, indent=2))
