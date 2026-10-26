from pathlib import Path
import json,hashlib,zipfile,shutil
ROOT=Path('H:/CCB-Studio-Suite');OUT=ROOT/'.local/preview-publication-118-20261027';C=Path('C:/CCB-Studio-Suite-Test-118-20261027/b');SOURCE='a1202e24f7a0cba8a53f43bf035b67e2134ca2aa';SHA='3540be44ca7a1fcfd583899b325abad7630e10b120d288e9f2e7dc7a1e8bd60d'
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
notes='''# Preview118 — 更新交接与旧程序目录收尾

实验性 Windows x64 邀请 Preview，尚非稳定版或全项目验收。

- 修正更新进程、新程序及清理进程继承旧工作目录的问题；兼容旧117 helper，避免新版仍运行时旧空目录被持续占用。
- 成功确认程序、资料和凭据状态、切换默认入口后，立即按所属证明清理废弃程序和下载；不等待7天。仅在获准退役的程序根中受控收尾空子目录；不删除未知文件，不遍历受保护目录或目录联接。
- 占用、权限或遍历限额失败会安全延后并重试；清理失败不会改写更新的成功/失败，也不会强行关闭用户窗口。
- 固定候选已经实际执行原始117程序/旧helper到118的NSIS安装、正常关闭及重启；118运行时旧117根自动移除，下载/解包0，重复清理0，两张合成原图及两项引用的合成凭据保留。候选阶段使用本地发现适配，正式公开签名发现/下载链须在发布后另行验证。

未知或自行改动的文件、配置、插件、源码、图库、备份及历史受保护安装继续保留，包含受保护的空目录；遇此情况旧根可能保留。不会为了目录消失而删除用户内容。

仍待验：资产管理/编辑完整人工验收、115体验反馈、Prompt对比、实际任务栏固定；q09历史失败的根因仍UNKNOWN。独立复审NOT REQUESTED。本轮只覆盖本机同一Windows账户的隔离升级；更多Windows环境、网络盘、跨账户/跨机器凭据迁移和所有异常组合未由本例验证。保留现有安装路径长度限制。Windows Authenticode发布者签名尚未提供，TUF更新签名不能代替Windows发布者认证。

固定源码：a1202e24f7a0cba8a53f43bf035b67e2134ca2aa
应用映像输入：3d0bd1f02c630d1e312d7469dbd71f29efe0bec1
安装器SHA-256：3540be44ca7a1fcfd583899b325abad7630e10b120d288e9f2e7dc7a1e8bd60d
候选证据：https://github.com/MisakaPiano/CCB-Studio-Suite/blob/ee46f20bccf34fa5268cb6b39e0d180793d9ebb4/docs/audits/PREVIEW_118_CANDIDATE_20261027.md
'''
(C/'RELEASE_NOTES.md').write_text(notes,encoding='utf-8')
new=json.loads((C/'payload/INSTALL_MANIFEST.json').read_text());old=json.loads((ROOT/'.local/relocation-117-20261027/candidate-preview-117/release-inputs/INSTALL_MANIFEST.json').read_text())
def external(m):
 return {e['path']:e['sha256'] for e in m['files'] if (e['path'].startswith('app/') and e['path'].endswith('.jar') and not Path(e['path']).name.startswith(('studio-','asset-library-','image-authority-'))) or e['path'].startswith('runtime/legal/') or (e['path'].startswith('updater/') and any(s in e['path'].lower() for s in ('license','notice','copyright')))}
# Build notices from the actual current package. Do not copy stale component metadata.
oldzip=ROOT/'.local/relocation-117-20261027/candidate-preview-117/THIRD_PARTY_NOTICES.zip'
assert sha(oldzip)=='c96a425a18f8b75d2d8939b595fa6809c432b398a9a62000484a8eb300de7b07'
components=[e for e in new['files'] if e['path'].startswith('app/') and e['path'].endswith('.jar')]
entries={}
for item in components:
 jar=C/'payload'/item['path'];assert sha(jar)==item['sha256']
 with zipfile.ZipFile(jar) as archive:
  for name in archive.namelist():
   if name.endswith('/') or not any(word in Path(name).name.lower() for word in ('license','notice','copyright','copying')):continue
   assert '..' not in Path(name).parts and not name.startswith(('/', '\\'))
   entries['jar-notices/'+jar.name+'/'+name]=archive.read(name)
for item in new['files']:
 name=item['path'];lower=name.lower()
 if name.startswith('runtime/legal/') or (name.startswith('updater/') and any(word in lower for word in ('license','notice','copyright','copying'))):
  entries['package-notices/'+name]=(C/'payload'/name).read_bytes()
with zipfile.ZipFile(oldzip) as archive:
 for name in archive.namelist():
  if name.startswith('additional/'):
   assert '..' not in Path(name).parts
   entries[name]=archive.read(name)
entries['COMPONENTS.json']=(json.dumps(dict(source=SOURCE,version='0.0.0-preview.118',components=components),indent=2)+'\n').encode()
entries['README.txt']=b'Third-party notices for Preview 118, generated from the actual fixed payload. Original embedded notices and runtime legal files remain in the installation. Additional pinned Skia/Skiko notices retained. COMPONENTS.json records the current JVM component hashes.\n'
with zipfile.ZipFile(C/'THIRD_PARTY_NOTICES.zip','x',compression=zipfile.ZIP_DEFLATED) as target:
 for name,data in sorted(entries.items()):target.writestr(name,data)
(OUT/'NOTICES_GENERATION.json').write_text(json.dumps(dict(status='GENERATED_FROM_FIXED118_PAYLOAD',files=len(entries),oldArchiveAdditionalOnly=True,currentComponentHashes=True,installerChanged=False,preparationGuard='Prior exact-path comparison rejected a renamed metadata-only Skiko packaging stub; no stale component table was reused'),indent=2)+'\n')
name='CCB-Studio-Suite-0.0.0-preview.118-windows-x64.exe';(C/'SHA256SUMS.txt').write_text(SHA+'  '+name+'\n'+sha(C/'THIRD_PARTY_NOTICES.zip')+'  THIRD_PARTY_NOTICES.zip\n'+sha(C/'RELEASE_NOTES.md')+'  RELEASE_NOTES.md\n',encoding='utf-8')
assets={name:dict(localPath=str(C/'studio-installer.exe'),bytes=(C/'studio-installer.exe').stat().st_size,sha256=SHA)}
for f in ('RELEASE_NOTES.md','THIRD_PARTY_NOTICES.zip','SHA256SUMS.txt'):
 p=C/f;assets[f]=dict(localPath=str(p),bytes=p.stat().st_size,sha256=sha(p));shutil.copy2(p,OUT/f)
pre=json.loads((OUT/'READ_ONLY_PREFLIGHT.json').read_text());pre.update(status='PREFLIGHT_PASS',assets=assets,noticesGeneratedFromFixedPayload=True,noticesCurrentComponentHashes=True);(OUT/'PREFLIGHT.json').write_text(json.dumps(pre,indent=2)+'\n');print(json.dumps(dict(status='ASSETS_PREPARED',assets={k:{f:v[f] for f in ('bytes','sha256')} for k,v in assets.items()})))
