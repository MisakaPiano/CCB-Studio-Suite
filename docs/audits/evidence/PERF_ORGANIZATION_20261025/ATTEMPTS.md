# Attempts and counts

- First direct embedded-Python runner invocation failed to import workspace_artifacts before Gradle. A wrapper inserts the tools path; no production change was made for this launcher issue.
- First Gradle attempt was interrupted before test execution when Desktop compilation appeared. Log and source input hashes retained; NOT TESTED, not PASS. Its unsealed fixtures retained, no historical cleanup.
- Serial baseline: 1 method PASS at 69f53d3, unchanged production from e4d3d5c. The 9 measurements inside it are scenarios, not extra tests. Desktop started another build before it completed; use this for scale discovery, not a quiet-machine wall-time claim.
- Final: 9 unique methods PASS at 98666fb, 0 failures/errors/skips. Includes the baseline method again, so never add 1+9. Four paired timing rounds and inner assertions are not additional test cases. No JVM/UI/native update regressions unrelated to changed grouping were repeated.
- Legacy comparison source extraction initially used the Python Windows default codec and failed before writing; corrected to UTF-8. This is fixture preparation, not a product test failure.
- Evidence collection first stopped on the absent ambient-process JSON (an empty PowerShell pipeline had created no file); resumed its own incomplete stage, recording the observation limitation explicitly. No fixed manifest/archive existed or was replaced.
- No new performance independent review or user acceptance. Project's prior P2 closure covers cleanup source only and is not performance approval. q09 old FAILED/UNKNOWN and user rejections retained elsewhere; no new q09 run.
