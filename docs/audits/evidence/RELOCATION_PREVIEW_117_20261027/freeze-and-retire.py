from pathlib import Path
import hashlib,json,shutil,subprocess,sys
ROOT=Path('H:/CCB-Studio-Suite');OUT=ROOT/'.local/relocation-117-20261027';CROSS=Path('C:/Users/1/AppData/Local/Temp/CCB-Studio-Suite-Relocation-20261027');EVIDENCE=ROOT/'docs/audits/evidence/RELOCATION_PREVIEW_117_20261027'
sys.path.insert(0,str(ROOT/'tools/update-runtime'));from owned_files import delete_matching
def sha(p):
 with p.open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()
def write(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(v,indent=2)+'\n')
assert json.loads((OUT/'NATIVE_CASES_RESULT_FINAL.json').read_text())['status']=='PASS'
assert json.loads((OUT/'FORMAL_CANDIDATE_VERIFICATION.json').read_text())['status']=='PASS'
candidate=OUT/'candidate-preview-117';receipt=json.loads((candidate/'BUILD_RECEIPT.json').read_text());assert sha(candidate/'studio-installer.exe')==receipt['sha256']
notes='''# Preview117 — 目录搬迁后的入口、资料定位与更新清理

实验性 Windows x64 邀请 Preview，尚非稳定版或全项目验收。

- 纳入71bac6f：旧缓存路径记录不再阻塞其他有完整成功证明的更新清理；兼容旧客户端下载收据缺失，仍只接管精确路径和经校验字节。
- 关闭程序后，将整个安装目录连同内部资料一起改名或移动，可从新位置的CCBStudioLauncher.exe启动。核对同一程序身份及现有资料后才更新路径，不创建空资料来替代缺失资料。
- 保留旧更新记录原字节，在新位置投影其所属及保护引用；成功确认后清理所属旧程序和下载，未知或自改文件、受保护安装及备份保留。
- 已补同盘改名、同盘移动、H→C真实跨盘三种原生更新链验证，各使用3项合成资产、2项合成凭据；正式117包另完成隔离安装及连续改名、移动、C→H搬迁启动核验。旧7项回归仅复用，不代替这些实测或重复累计。

搬迁时先正常关闭程序并携带完整目录。外部资料路径不会自动搬走；资料缺失或目录联接会拒绝重定位。搬迁后原桌面、开始菜单或任务栏入口可能失效：从新位置启动后，用更新页重建桌面和开始菜单入口；任务栏重新固定未作人工验收，不能声称Windows会自动追踪跨盘搬迁。当前NSIS仍有完整路径长度限制，过长路径会安全拒绝；本轮保留了该失败记录。仅验证同一Windows账户及本机C/H的NTFS卷，未验证跨机器、换账户或网络盘。

仍待验：资产管理／编辑完整人工验收、115体验反馈、Prompt对比分别保留；q09旧失败及其UNKNOWN根因不改。独立复审NOT REQUESTED。正式117公开签名发现／下载尚未发布验证；本轮不改变信任根，不提供Windows Authenticode签名，不更新日常安装。
'''
(candidate/'RELEASE_NOTES.md').write_text(notes,encoding='utf-8')
notice=ROOT/'.local/preview-candidate-116-20261026/THIRD_PARTY_NOTICES.zip'
if not notice.exists():notice=ROOT/'.local/preview-candidate-116-20261026/candidate/THIRD_PARTY_NOTICES.zip'
assert notice.exists() and sha(notice)=='c96a425a18f8b75d2d8939b595fa6809c432b398a9a62000484a8eb300de7b07'
shutil.copyfile(notice,candidate/'THIRD_PARTY_NOTICES.zip')
name='CCB-Studio-Suite-0.0.0-preview.117-windows-x64.exe'
(candidate/'SHA256SUMS.txt').write_text(receipt['sha256']+'  '+name+'\n'+sha(candidate/'THIRD_PARTY_NOTICES.zip')+'  THIRD_PARTY_NOTICES.zip\n'+sha(candidate/'RELEASE_NOTES.md')+'  RELEASE_NOTES.md\n',encoding='utf-8')
write(OUT/'CANDIDATE_READY.json',dict(status='READY_FOR_USER_CONFIRMATION_NOT_PUBLISHED',version=117,displayVersion='0.0.0-preview.117',source=receipt['sourceSha'],appBuildSource=receipt['appBuildSourceSha'],nativeLauncherSourceSha256=receipt['nativeLauncherSourceSha256'],installer=str(candidate/'studio-installer.exe'),assetName=name,bytes=receipt['bytes'],sha256=receipt['sha256'],realRelocationCases=3,formalCandidateVerified=True,oldSevenReusedOnly=True,formalRootUnchanged=True,dailyInstallationModified=False,pending=['public117 signed discovery/download','independent review','full asset/editor human acceptance','115 experience','Prompt comparison','actual user taskbar pin after move']))

# Freeze only generated evidence/logs and public metadata, never profile/vault/image bytes.
assert not EVIDENCE.exists();EVIDENCE.mkdir(parents=True);(EVIDENCE/'.gitattributes').write_text('* -text\n')
for p in OUT.iterdir():
 if p.is_file() and p.suffix in ('.json','.log','.py'):shutil.copyfile(p,EVIDENCE/p.name)
for folder in ('native','native-r2','native-r3','formal-package','repository','repository-v2'):
 for p in (OUT/folder).rglob('*'):
  if p.is_file() and p.suffix in ('.json','.jsonl','.log'):
   dest=EVIDENCE/folder/p.relative_to(OUT/folder);dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(p,dest)
for folder in ('candidate-internal','candidate-v2','candidate-v3','candidate-preview-117'):
 for name in ('BUILD_RECEIPT.json','installer.nsi','compile.log','RELEASE_NOTES.md','SHA256SUMS.txt'):
  p=OUT/folder/name
  if p.exists():dest=EVIDENCE/folder/name;dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(p,dest)
for rel in ('payload/INSTALL_MANIFEST.json','payload/update-config.json','payload/update-root.json','payload/updater/RUNTIME_MANIFEST.json'):
 dest=EVIDENCE/'final-package'/rel;dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(candidate/rel,dest)
for folder in (ROOT/'.local/windows-preview/evidence').glob('*-relocation-117-*-20261027'):
 for p in folder.iterdir():
  if p.is_file():dest=EVIDENCE/'build'/folder.name/p.name;dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(p,dest)
sources=['tools/windows-launcher/Launcher.cs','tools/update-runtime/residue_cleanup.py','tools/update-runtime/apply_update.py','tools/update-runtime/test_relocation.py','tools/Build-InternalInstaller.py','modules/studio-desktop/src/main/kotlin/io/github/misakapiano/ccbstudio/desktop/DesktopEntrypoints.kt','modules/studio-desktop/src/test/kotlin/io/github/misakapiano/ccbstudio/desktop/RelocationNativeA.kt']
mapping=[]
for name in sources:
 p=ROOT/name;dest=EVIDENCE/'source'/name;dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(p,dest)
 blob=subprocess.check_output(['git','rev-parse','HEAD:'+name],cwd=ROOT,text=True).strip();mapping.append(dict(path=name,windowsSha256=sha(p),gitBlob=blob,sourceCommit=receipt['sourceSha']))
write(EVIDENCE/'SOURCE_MAPPING.json',mapping)

def retire(folder,label):
 folder=Path(folder);assert folder.is_relative_to(OUT) or folder==CROSS
 files=[];dirs=[]
 for p in folder.rglob('*'):
  assert not p.is_symlink() and not p.is_junction()
  if p.is_file():files.append(dict(path=p.relative_to(folder).as_posix(),bytes=p.stat().st_size,sha256=sha(p)))
  elif p.is_dir():dirs.append(p)
 proof=EVIDENCE/'retirement'/(label+'-MANIFEST.json');write(proof,dict(root=str(folder),files=files,kind='THIS_TURN_EXPLICIT_SYNTHETIC_ROOT',containsUserData=False))
 removed=0
 for item in files:
  result=delete_matching(folder/item['path'],item)
  if result!='REMOVED':raise RuntimeError('Owned retirement safely deferred: '+result)
  removed+=1
 for p in sorted(dirs,key=lambda p:len(p.parts),reverse=True):p.rmdir()
 folder.rmdir();write(EVIDENCE/'retirement'/(label+'-RESULT.json'),dict(status='COMPLETE',files=removed,bytes=sum(x['bytes'] for x in files),priorFailureEvidenceRetained=True));return removed

# C root was absent before this task and explicitly approved by the user.
owner=json.loads((CROSS/'OWNER.json').read_text());assert owner==dict(kind='AUTHORIZED_SUITE_SYNTHETIC_RELOCATION',suite=str(ROOT),task='relocation-117-20261027',dailyData=False)
for p in [CROSS,*CROSS.parents]:assert not p.is_symlink() and not p.is_junction()
retired=retire(CROSS,'approved-cross-volume-root')
for folder in ('launcher-fixtures','shortcut-lock-fixture','synthetic','updater-runtime'):
 p=OUT/folder
 if p.exists():retired+=retire(p,folder)
# Only empty ancestors of the two recorded, already-retired H fixtures.
known=[json.loads((OUT/'native-r3/cross-volume-H-to-C/REAL_MOVE.json').read_text())['source'],json.loads((OUT/'FORMAL_CANDIDATE_VERIFICATION.json').read_text())['moves'][-1]['destination']]
for value in known:
 path=Path(value)
 for parent in path.parents:
  if parent==ROOT/'.local/disposable':break
  assert parent.is_relative_to(ROOT/'.local/disposable')
  if parent.exists():parent.rmdir()
write(EVIDENCE/'FINAL_SCOPE.json',dict(status='CANDIDATE_READY_WITH_KNOWN_LIMITS',source=receipt['sourceSha'],installerSha256=receipt['sha256'],crossRootGone=not CROSS.exists(),retiredFiles=retired,kept='Fixed Preview117 installer/payload and raw evidence; existing baselines and daily installation untouched',published=False))
files=[dict(path=p.relative_to(EVIDENCE).as_posix(),bytes=p.stat().st_size,sha256=sha(p)) for p in sorted(EVIDENCE.rglob('*')) if p.is_file()]
write(EVIDENCE/'RAW_MANIFEST.json',dict(format=1,files=files))
print(json.dumps(dict(status='PASS',evidence=str(EVIDENCE),evidenceFiles=len(files),retiredFiles=retired,installerSha256=receipt['sha256'])),flush=True)
