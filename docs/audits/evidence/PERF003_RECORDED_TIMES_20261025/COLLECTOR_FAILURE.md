# Collection attempt 1, no additional test execution

2026-10-25 JST, first collect.py failed:

```
File "collect.py", line 40
  assert inputs[kind][name] in (sha256(gitBytes), sha256(gitBytesWithCRLF))
AssertionError
```

Reason: LibraryCatalogProjection.kt has mixed line endings (one CRLF among otherwise LF). The collector incorrectly assumed each working file was wholly LF or wholly CRLF. The baseline and final tests actually passed and source-before records remain unchanged. The partial collection was preserved at .local/perf003-recorded-times-20261025/collector-attempt-1; collector-failed.py retains the exact failed script. No source normalization or test rerun was performed.

Corrected proof: final raw working bytes must exactly hash to the final source-before value; reversing only the exact changed implementation block must exactly hash to baseline source-before. Both raw inputs, changing CRLF to LF only, must equal their fixed Git blobs byte-for-byte. Preserve both original raw source inputs in the evidence pack. This proves actual input identity rather than accepting a mismatching hash or removing the check. Zero added Gradle/JUnit executions; this is a collection failure, not a product test failure/pass.
