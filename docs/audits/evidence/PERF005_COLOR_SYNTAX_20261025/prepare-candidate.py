from pathlib import Path
import hashlib,json,subprocess
ROOT=Path('H:/CCB-Studio-Suite');LOCAL=ROOT/'.local/perf005-color-syntax-20261025'
name='modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/TypesetColors.kt'
raw=(ROOT/name).read_bytes();before=(LOCAL/'TypesetColors.before.kt').read_bytes();assert raw==before
fixed=subprocess.check_output(['git','show','HEAD:'+name],cwd=ROOT)
assert raw.replace(b'\r\n',b'\n')==fixed
old=fixed.decode();new=old
expressions=[('functionSyntax',r'Regex("([a-z]+)\\((.*)\\)",RegexOption.DOT_MATCHES_ALL)'),('whitespaceSyntax',r'Regex("\\s+")'),('numberSyntax',r'Regex("[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:e[+-]?[0-9]+)?")')]
for key,expr in expressions:
    assert old.count(expr)==1
    new=new.replace(expr,key,1)
fields=''.join('    private val '+key+' by lazy { '+expr+' }\n' for key,expr in expressions)
new=new.replace('object TypesetColors {\n','object TypesetColors {\n'+fields,1)
(LOCAL/'TypesetColors.candidate.kt').write_text(new,encoding='utf-8')
(LOCAL/'candidate-source-check.json').write_text(json.dumps(dict(status='DRAFT_ONLY_NOT_APPLIED_OR_EXECUTED',beforeRawSha256=hashlib.sha256(raw).hexdigest(),beforeGitSha256=hashlib.sha256(fixed).hexdigest(),proposedTextSha256=hashlib.sha256(new.encode()).hexdigest(),changes='3 exact syntax expression replacements plus3 lazy immutable fixed pattern fields;no input/result cache'),indent=2)+'\n',encoding='utf-8')
print('Draft only;fixed regex patterns/options unchanged,modules/tools untouched,no test execution')
