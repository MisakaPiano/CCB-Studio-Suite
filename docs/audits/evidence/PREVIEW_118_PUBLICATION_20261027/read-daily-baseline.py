from pathlib import Path
import sys,json,hashlib,datetime,shutil
ROOT=Path('H:/CCB-Studio-Suite');OUT=ROOT/'.local/preview-publication-118-20261027';APP=Path('H:/CCB-Studio-Suite-App');sys.path.insert(0,str(ROOT/'tools/update-runtime'));from install_ops import no_reparse,verify_install
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
state=json.loads((APP/'launch-current.json').read_text());assert state['version'] in (117,118)
program=no_reparse(Path(state['installed']));assert program.parent==APP
config=json.loads((program/'update-config.json').read_text());assert config['version']==state['version'] and config['channel']=='preview'
assert sha(program/'update-root.json')=='408008d80aeb2dd3844325d74f09da9d248733e220dd88c08cc6299c4a24cc7a'
assert Path(state['data'])==APP/'data/workspace' and Path(state['runtime'])==APP/'data/runtime'
protection=sha(APP/'ACTIVE_INSTALLATION.json');check=verify_install(program,config)
value=dict(status='READ_ONLY_BASELINE',atUtc=datetime.datetime.now(datetime.timezone.utc).isoformat(),version=state['version'],source=config['sourceSha'],program=str(program),dataRoot=state['data'],runtimeRoot=state['runtime'],manifestSha256=sha(program/'INSTALL_MANIFEST.json'),programVerification=check,protectionSha256=protection,hFree=shutil.disk_usage(APP).free,appDataOrCredentialContentRead=False,mutations=False)
(OUT/'DAILY_READ_ONLY_BASELINE.json').write_text(json.dumps(value,indent=2)+'\n');print(json.dumps({k:value[k] for k in ('status','version','program','hFree','appDataOrCredentialContentRead')}))
