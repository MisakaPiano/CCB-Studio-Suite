# Preparation attempt, no test execution

2026-10-25 JST, first invocation of finish_test.py failed before writing the test file:

```
File "finish_test.py", line 16
  (root/'.local/perf003-tag-index-20261025/extra-methods.txt').read_text()
UnicodeDecodeError: 'gbk' codec can't decode byte 0xad in position 280: illegal multibyte sequence
```

The repository test remained exactly at baseline c82b463; production candidate was not executed. Cause: local Python used default Windows GBK to read the UTF-8 draft containing a synthetic Chinese tag. Corrected all draft/test read and write calls to explicit UTF-8, then re-applied preparation once. This is a preparation failure with zero Gradle/JUnit executions, not a failed/passed product test. No prior failure or assertion removed.
