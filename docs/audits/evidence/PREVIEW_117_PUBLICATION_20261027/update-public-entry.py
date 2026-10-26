from pathlib import Path
import base64,datetime,hashlib,json,re,sys,urllib.request
ROOT=Path('H:/CCB-Studio-Suite');OUT=ROOT/'.local/preview-publication-117-20261027';sys.path.insert(0,str(ROOT/'.local/preview-publication-116-20261026'))
from github_session import Session,REPO
assert json.loads((OUT/'PUBLIC_CHANNEL_VERIFICATION.json').read_text())['status']=='PASS'
s=Session();release=json.loads((OUT/'RELEASE_STATE.json').read_text());live=s.api('GET','/repos/'+REPO+'/releases/'+str(release['releaseId']))
assert live['prerelease'] and not live['draft'];body=live['body'];marker='<!-- public117-verification-20261027 -->'
if marker not in body:
 extra='''

<!-- public117-verification-20261027 -->
### 发布后的公开更新链核验

2026-10-27已补验：保留的116客户端通过原有信任根实际发现并下载公开117，包散列一致；旧helper执行真实NSIS隔离安装后，117客户端检查返回已是最新版。上文“尚未发布验证”是候选阶段记录，现由本补记更新；原附件和签名目标内容保持原字节。

此公开链检查使用合成资料和命令行客户端，不能当作用户日常GUI升级验收；日常116未自动更新。候选的真实原生搬迁、资料与合成凭据验证另见[固定报告](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/3d4a97f7c6303bf7c81798abbabfdc7057312a36/docs/audits/RELOCATION_PREVIEW_117_20261027.md)。资产／编辑完整人工验收、115体验、Prompt对比及任务栏自动跟随仍保持待验，非全项目验收。
'''
 result=s.api('PATCH','/repos/'+REPO+'/releases/'+str(release['releaseId']),dict(body=body+extra))
 assert result['body']==body+extra and result['prerelease']
 (OUT/'RELEASE_BODY_UPDATE.json').write_text(json.dumps(dict(status='PASS',releaseId=release['releaseId'],priorBodySha256=hashlib.sha256(body.encode()).hexdigest(),newBodySha256=hashlib.sha256((body+extra).encode()).hexdigest(),onlyBodyChanged=True,attachmentsUnchanged=True),indent=2)+'\n')
file=s.api('GET','/repos/'+REPO+'/contents/README.md?ref=main');old=base64.b64decode(file['content']).decode('utf-8');assert old.startswith('<!-- suite-download-entry-20261024 -->')
boundary=old.index('<!-- release-migration-20261023 -->');suffix=old[boundary:]
prefix='''<!-- suite-download-entry-20261024 -->
> [!IMPORTANT]
> ## [Windows 预览版下载 — 0.0.0-preview.117](https://github.com/MisakaPiano/CCB-Studio-Suite/releases/tag/v0.0.0-preview.117) · [全部版本](https://github.com/MisakaPiano/CCB-Studio-Suite/releases)
> 在发行页 **Assets** 下载 **`CCB-Studio-Suite-0.0.0-preview.117-windows-x64.exe`**。**Source code (zip / tar.gz) 是源码，不是 Windows 安装器。**
>
> 这是**实验性邀请 Preview，尚非稳定版或完整 RC 验收版**。117纳入目录改名／移动后的入口与所属清理修复；本机真实搬迁、公开签名发现／下载及隔离安装已核验，自动验证不能代替完整人工验收。

当前主要限制：

- 资产管理／编辑完整人工验收、115体验反馈和Prompt对比分别待验；q09编辑历史像素断言失败的根因仍UNKNOWN。
- 搬迁前正常关闭程序，携带完整目录；从新位置的CCBStudioLauncher.exe启动后可重建外部入口。Windows任务栏自动追踪搬迁／用户重新固定尚未验收，外置资料不会自动搬运；过长路径可能被安装器安全拒绝。
- 真实搬迁仅覆盖本机同一Windows账户的C/H NTFS卷；跨机器恢复、换账户、网络盘、更多Windows环境和真实供应商调用未由本轮验证。
- 清理只处理有归属证明的废弃程序和缓存，受保护111、未知／自改文件、用户资料、插件和备份保留。Windows Authenticode发布者签名尚未提供，TUF更新签名不能代替Windows发布者认证。
- Android与BYOK Server尚未实施；当前可下载交付为Windows x64。

[搬迁与固定候选验证](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/3d4a97f7c6303bf7c81798abbabfdc7057312a36/docs/audits/RELOCATION_PREVIEW_117_20261027.md) · [当前状态](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/feature/windows-rc-continuous/docs/CURRENT_STATE.md) · [首页链接同步流程](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/feature/windows-rc-continuous/docs/rc/PREVIEW_UPDATE_CONTINUITY.md#首页下载入口同步)

---

'''
new=prefix+suffix;(OUT/'PUBLIC_MAIN_README_BEFORE.txt').write_text(old,encoding='utf-8');(OUT/'PUBLIC_MAIN_README_117.txt').write_text(new,encoding='utf-8')
result=s.api('PUT','/repos/'+REPO+'/contents/README.md',dict(message='Document Preview117 download and verified relocation boundaries',content=base64.b64encode(new.encode()).decode(),sha=file['sha'],branch='main'))
commit=result['commit']['sha'];detail=s.api('GET','/repos/'+REPO+'/commits/'+commit);assert [x['filename'] for x in detail['files']]==['README.md']
read=s.api('GET','/repos/'+REPO+'/contents/README.md?ref=main');actual=base64.b64decode(read['content']);assert actual==new.encode()
def public(url):
 with urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'Suite117-public-entry-readback'}),timeout=30) as response:return response.read()
legacy=public('https://raw.githubusercontent.com/MisakaPiano/CCB-Studio-Suite-Releases/main/README.md');assert b'https://github.com/MisakaPiano/CCB-Studio-Suite/releases' in legacy
html=public('https://github.com/MisakaPiano/CCB-Studio-Suite').decode('utf-8');position=html.find('"sidebarAbout"');fragment=html[position:position+50000] if position>=0 else ''
record=dict(status='PASS',defaultBranch='main',defaultBranchVerifiedWithConnectedGitHub=True,commit=commit,files=['README.md'],beforeBlob=file['sha'],afterBlob=read['sha'],readbackSha256=hashlib.sha256(actual).hexdigest(),suffixPreserved=True,downloadPage=release['url'],legacyMigrationLinkPresent=True,legacyModified=False,releasesSidebarObserved=bool(re.search(r'"releases"\s*:',fragment)),sidebarConfigurationChanged=False,preReleaseRemainsTrue=True)
(OUT/'README_PUBLICATION.json').write_text(json.dumps(record,indent=2)+'\n');print(json.dumps(record),flush=True)
