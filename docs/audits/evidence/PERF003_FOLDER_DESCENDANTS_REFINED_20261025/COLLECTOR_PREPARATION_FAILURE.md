# Refined collector preparation attempt, zero tests

2026-10-25 JST, inline Python command generation used backslash-escaped double quotes inside a PowerShell double-quoted argument. PowerShell parsed it before executing Python and stopped with:

```
ParserError: You must provide a value expression following the '+' operator.
```

No Python preparation,source edit,collector output or Gradle/JUnit execution occurred in that command. Corrected by saving a standalone UTF-8 preparation script through the file tool,then invoking it with a plain path. Original candidate/diagnostic/test evidence unchanged. This is a shell preparation error,not a product test FAIL/PASS.
