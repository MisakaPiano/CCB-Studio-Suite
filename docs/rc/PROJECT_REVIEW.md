# Windows RC Continuous — RC BLOCKED / CP1 checkpoint

No Windows RC installer or actual application A→B update is delivered. This is a tested, resumable CP1 data-maintenance/trust-foundation checkpoint. CP2 is stopped at the explicitly approved source/authority boundary, not a routine Slice approval request.

Baseline: `27dbd5d578f364afb529fc33944386c0c107f0da`. Branch: `feature/windows-rc-continuous`. Read this page at the final fixed commit reported by Codex; the source-bound implementation checkpoint and platform receipts will be recorded after actual push/upload.

1. [CP1 execution, exact actual results/environment and STOP](CP1_EXECUTION.md).
2. [Checkpoint/resume ledger](CHECKPOINTS.md), [active task contract](../../tasks/WINDOWS_RC_CONTINUOUS.md), [formal Gate and all issue comments snapshot](../audits/evidence/WINDOWS_RC/CONTRACT_SOURCE.json).
3. [All 31 final JVM cases](../audits/evidence/WINDOWS_RC/CP1_TEST_CASES.json), [11 Java + 3 Python source identities and counts](../audits/evidence/WINDOWS_RC/CP1_SOURCE_IDENTITY.json), [readable original-run derivatives](../audits/evidence/WINDOWS_RC/runs).
4. [Data module/API](../../modules/asset-library/README.md), [maintenance source](../../modules/asset-library/src/main/java/io/github/misakapiano/ccbstudio/assets/AssetMaintenance.java), [real process-crash maintenance regression](../../modules/asset-library/src/test/java/io/github/misakapiano/ccbstudio/assets/AssetMaintenanceTest.java).
5. [Actual TUF lab and reproducible dependency lock](../../tools/update-lab/README.md), [future real application A→B gate](UPDATE_A_B_PLAN.md).
6. [Exact-source CP2 blocker and independent target remedy](DEPENDENCY_ESCALATION.md), [all REQ-01–12 actual readiness states](RC_READINESS.md).
7. [59 unchanged historical evidence files](../audits/evidence/WINDOWS_RC/HISTORICAL_EVIDENCE.json) and [startup/owner observation](../audits/evidence/WINDOWS_RC/START_BASELINE.json).

Actual final validation: 31 JVM tests and 8 real TUF synthetic-trust tests, 0 failures/errors/skips. New maintenance targeted tests: 4 PASS. These do not prove application UI, paid generation, credential storage/migration, full editing or application update. Historical failures remain unchanged.

Required next input: a reviewed, fixed authoritative Desktop image-core artifact/source closure with explicit Suite reuse/distribution rights/notices and equivalence vectors, or a lawful independently approved exact-source boundary. Do not reconstruct Prompt/request/fallback logic, write Desktop, merge main or produce a release to bypass this STOP. Resume from the pushed checkpoint once the dependency is resolved; do not repeat A0 or discard R1.
