"""Record real unittest execution; keys remain memory-only. No OS install or trust changes."""
import hashlib
import json
import platform
import subprocess
import sys
import time
import unittest
from datetime import datetime, timezone
from pathlib import Path
import test_trust

root = Path(__file__).resolve().parents[2]
if root.name.lower() != "ccb-studio-suite":
    raise RuntimeError("Run only in the Suite workspace")
stamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S%fZ")
evidence = root / ".local/windows-rc/evidence" / (stamp + "-cp1-update-trust")
evidence.mkdir(parents=True)
test_trust.FIXTURES = root / ".local/windows-rc/update-fixtures"
test_trust.FIXTURES.mkdir(exist_ok=True)
sources = {p.relative_to(root).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest() for p in Path(__file__).parent.glob("*.py")}
start = time.monotonic()
with (evidence / "unittest.log").open("w", encoding="utf-8") as output:
    result = unittest.TextTestRunner(stream=output, verbosity=2).run(unittest.defaultTestLoader.loadTestsFromTestCase(test_trust.TrustTests))
for p, digest in sources.items():
    if hashlib.sha256((root / p).read_bytes()).hexdigest() != digest:
        raise RuntimeError("Source changed during trust tests")
summary = {"task": "WINDOWS_RC_CONTINUOUS", "checkpoint": "CP1", "scope": "Real python-tuf trust verification over synthetic in-memory transport; NOT application A-to-B installation", "status": "PASS" if result.wasSuccessful() and not result.skipped else "FAILED", "tests": result.testsRun, "failures": len(result.failures), "errors": len(result.errors), "skipped": len(result.skipped), "elapsedSeconds": time.monotonic()-start, "python": sys.version, "platform": platform.platform(), "sourceSha256": sources, "baseCommit": subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=root).decode().strip(), "applicationInstallRestartAndCredentialMigration": "NOT TESTED", "privateKeyFilesCreated": 0, "networkListeners": 0}
(evidence / "summary.json").write_text(json.dumps(summary, indent=2)+"\n", encoding="utf-8")
print((evidence / "unittest.log").read_text(encoding="utf-8"))
print(json.dumps(summary, indent=2))
print("EVIDENCE="+str(evidence))
sys.exit(0 if summary["status"] == "PASS" else 1)
