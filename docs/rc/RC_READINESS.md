# Windows RC readiness — RC BLOCKED

Contract: [active snapshot](../../tasks/WINDOWS_RC_CONTINUOUS.md). Current critical STOP: [authority rights/artifact boundary](DEPENDENCY_ESCALATION.md). No Windows application package or real application A→B update is delivered. An evidence ZIP is not an installer.

| Required first-version behavior | Actual scope/result |
|---|---|
| REQ-01 persistent gallery/reopen/backup/export | Asset-library synthetic automation only; no Windows gallery UI acceptance |
| REQ-02 standalone NAI generation | BLOCKED by authoritative-core rights/artifact boundary |
| REQ-03 AI Prompt design/revise/history | BLOCKED by the same single-authority dependency |
| REQ-04 named LLM/NAI secure profiles | NOT IMPLEMENTED / NOT TESTED |
| REQ-05 visible/frozen real provider/account/model | NOT IMPLEMENTED / NOT TESTED |
| REQ-06 drag/drop/import/metadata UI | NOT IMPLEMENTED / NOT TESTED |
| REQ-07 full Recipe/Prompt comparison | NOT IMPLEMENTED / NOT TESTED |
| REQ-08 persistent non-destructive editing/batch/undo | NOT IMPLEMENTED / NOT TESTED |
| REQ-09 originals/projects/exports separated | Library byte/relationship separation tested; full editing/export workflow NOT TESTED |
| REQ-10 full in-app secure update | CP1 real TUF trust lab only; application discovery/confirmation/install/restart NOT TESTED |
| REQ-11 upgrade data/credential preservation | NOT TESTED; no application or OS-protected credential migration exercised |
| REQ-12 traceable adopted CCB authority | Exact source probe recorded; adoption rights/artifact dependency BLOCKED |

CP1 new maintenance tests supplement inherited durability tests, not user acceptance. Unknown/corrupt/future/unique data remains explicitly refused/preserved. The old product acceptance matrix and all A0/A1/R1 evidence remain unchanged. Historical FAILED/SKIPPED/UNKNOWN are retained, not converted into new PASS.

Not tested: arbitrary power loss/directory-entry durability, real disk exhaustion/OS ACL changes, adversarial hard-link/TOCTOU, full Windows UI/lifecycle, real user libraries/credentials/providers, actual install/update, Android/Server and peak memory. These do not become PASS from mocked transport or JVM file tests.

Concentrated human acceptance is deferred until an actual RC exists. The eventual single-session checklist must cover clean install/upgrade identity, isolated data roots, restart, import/metadata/full Recipe compare, AI design/revision and frozen accounts, original-byte preservation, persistent editing/batch/undo/export and normal/privacy difference, verified backup/restore, in-app A→B confirmation/failure preservation and supported credential continuity. It cannot substitute for missing automated product gates, and paid providers still need separate authorization.
