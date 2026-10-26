from pathlib import Path
import base64,json,hashlib,sys,re,urllib.request
ROOT=Path('H:/CCB-Studio-Suite');OUT=ROOT/'.local/preview-publication-118-20261027'
sys.path.insert(0,str(ROOT/'.local/preview-publication-116-20261026'))
from github_session import Session,REPO
def write(name,value): (OUT/name).write_text(json.dumps(value,indent=2)+'\n',encoding='utf-8')
assert json.loads((OUT/'PUBLIC_CHANNEL_VERIFICATION.json').read_text(encoding='utf-8'))['status']=='PASS'
s=Session();release=json.loads((OUT/'RELEASE_STATE.json').read_text(encoding='utf-8'))
live=s.api('GET','/repos/'+REPO+'/releases/'+str(release['releaseId']))
assert live['prerelease'] and not live['draft']
marker='<!-- public118-verification-20261027 -->';body=live['body']
extra='''

<!-- public118-verification-20261027 -->
### 发布后的公开更新链核验

2026-10-27已补验：未修改的117包内客户端通过原正式信任根发现并下载公开118，安装器字节与散列一致；旧117 helper执行真实NSIS隔离安装，生产程序确认资料与两个合成凭据。118运行时旧117根自动退役、下载及解包无残留、重复清理无变化，118检查返回已是最新版。未使用候选发现适配。

一次过早检查下载目录的测试断言失败已保留；等待同一轮异步清理完成后续验，未重复升级累加通过。失败驱动结束后原B进程已不在，退出原因未独立诊断；后续同一安装从默认入口重开、资料复核及正常关闭分别留证。

日常安装仍待正常界面升级；不能替代用户功能验收。[固定候选与旧失败](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/ee46f20bccf34fa5268cb6b39e0d180793d9ebb4/docs/audits/PREVIEW_118_CANDIDATE_20261027.md)。上文发行说明中的发布前待验是历史阶段记录，此补记补充公开核验，附件及签名目标保持原字节。资产／编辑完整人工、115体验、Prompt对比及任务栏人工固定待验，q09 FAILED/root UNKNOWN保留，非全项目或Project独立验收。
'''
if marker not in body:
 result=s.api('PATCH','/repos/'+REPO+'/releases/'+str(release['releaseId']),dict(body=body+extra))
 assert result['body']==body+extra and result['prerelease']
 write('RELEASE_BODY_UPDATE.json',dict(status='PASS',releaseId=release['releaseId'],priorBodySha256=hashlib.sha256(body.encode()).hexdigest(),newBodySha256=hashlib.sha256((body+extra).encode()).hexdigest(),onlyBodyChanged=True,attachmentsUnchanged=True))
file=s.api('GET','/repos/'+REPO+'/contents/README.md?ref=main')
old=base64.b64decode(file['content']).decode('utf-8');assert old.startswith('<!-- suite-download-entry-20261024 -->')
suffix=old[old.index('<!-- release-migration-20261023 -->'):]
prefix='''<!-- suite-download-entry-20261024 -->
> [!IMPORTANT]
> ## [Windows 预览版下载 — 0.0.0-preview.118](https://github.com/MisakaPiano/CCB-Studio-Suite/releases/tag/v0.0.0-preview.118) · [全部版本](https://github.com/MisakaPiano/CCB-Studio-Suite/releases)
> 在发行页 **Assets** 下载 **`CCB-Studio-Suite-0.0.0-preview.118-windows-x64.exe`**。**Source code (zip / tar.gz) 是源码，不是 Windows 安装器。**
>
> 这是**实验性邀请 Preview，尚非稳定版或完整 RC 验收版**。118补齐旧117升级后的程序目录占用与空目录收尾；公开签名发现／下载及真实NSIS隔离升级已核验，新版运行时旧根自动退役。自动验证不能代替完整人工验收。

当前主要限制：

- 资产管理／编辑完整人工验收、115体验反馈和Prompt对比分别待验；q09编辑历史像素断言失败的根因仍UNKNOWN。
- 安装盘需留足暂存空间；117客户端下载本包前要求约548MiB空闲。检查／下载／清理共用缓存锁，重叠操作时安全拒绝，应等待当前操作结束再重试。
- 搬迁前正常关闭程序，携带完整目录，从新位置CCBStudioLauncher.exe启动可重建外部入口。Windows任务栏自动跟随搬迁／用户重新固定尚未验收；外置资料不会自动搬运，过长路径可能安全拒绝。
- 当前原生升级和搬迁仅在本机同一Windows账户的C/H NTFS卷验证；跨机器、换账户、网络盘、更多Windows环境及真实供应商调用未由本轮验证。
- 清理只处理有归属证明的废弃程序／缓存和获准退役根内安全空目录；受保护111、未知／自改文件、用户资料、插件、备份及受保护空目录均保留。持续占用或权限失败仍可能保留旧目录并安全重试。
- Windows Authenticode发布者签名尚未提供，TUF更新签名不能代替发布者认证。Android与BYOK Server尚未实施，当前下载为Windows x64。

[固定候选与旧失败](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/ee46f20bccf34fa5268cb6b39e0d180793d9ebb4/docs/audits/PREVIEW_118_CANDIDATE_20261027.md) · [当前状态](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/feature/windows-rc-continuous/docs/CURRENT_STATE.md) · [首页链接同步流程](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/feature/windows-rc-continuous/docs/rc/PREVIEW_UPDATE_CONTINUITY.md#首页下载入口同步)

---

'''
new=prefix+suffix
(OUT/'PUBLIC_MAIN_README_BEFORE.txt').write_text(old,encoding='utf-8');(OUT/'PUBLIC_MAIN_README_118.txt').write_text(new,encoding='utf-8')
result=s.api('PUT','/repos/'+REPO+'/contents/README.md',dict(message='Document Preview118 download and verified update boundaries',content=base64.b64encode(new.encode()).decode(),sha=file['sha'],branch='main'))
commit=result['commit']['sha'];detail=s.api('GET','/repos/'+REPO+'/commits/'+commit);assert [x['filename'] for x in detail['files']]==['README.md']
read=s.api('GET','/repos/'+REPO+'/contents/README.md?ref=main');actual=base64.b64decode(read['content']);assert actual==new.encode()
def public(url):
 with urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'Suite118-public-entry-readback'}),timeout=30) as response:return response.read()
legacy=public('https://raw.githubusercontent.com/MisakaPiano/CCB-Studio-Suite-Releases/main/README.md');assert b'https://github.com/MisakaPiano/CCB-Studio-Suite/releases' in legacy
html=public('https://github.com/MisakaPiano/CCB-Studio-Suite').decode('utf-8');position=html.find('"sidebarAbout"');fragment=html[position:position+50000] if position>=0 else ''
record=dict(status='PASS',defaultBranch='main',commit=commit,files=['README.md'],beforeBlob=file['sha'],afterBlob=read['sha'],readbackSha256=hashlib.sha256(actual).hexdigest(),suffixPreserved=True,downloadPage=release['url'],legacyMigrationLinkPresent=True,legacyModified=False,releasesSidebarObserved=bool(re.search(r'"releases"\s*:',fragment)),sidebarConfigurationChanged=False,preReleaseRemainsTrue=True)
write('README_PUBLICATION.json',record);print(json.dumps(record))
