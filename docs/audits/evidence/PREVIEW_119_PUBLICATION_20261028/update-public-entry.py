from pathlib import Path
import sys,json,base64,hashlib
r=Path('H:/CCB-Studio-Suite');o=Path(__file__).parent;sys.path.insert(0,str(r/'.local/preview-publication-116-20261026'))
from github_session import Session,REPO
s=Session();result=json.loads((o/'PUBLIC_CHANNEL_VERIFICATION.json').read_text());assert result['status']=='PASS' and result['version']==119
state=json.loads((o/'RELEASE_STATE.json').read_text());live=s.api('GET','/repos/'+REPO+'/releases/'+str(state['releaseId']));assert not live['draft'] and live['prerelease']
marker='<!-- public119-verification-20261028 -->'
if marker not in live['body']:
 extra='\n\n'+marker+'\n### 发布后公开更新链核验\n\n2026-10-28：原版118客户端通过原正式信任根发现并下载公开119，旧118 helper执行真实NSIS隔离安装，119生产程序确认两张合成原图及两个账户引用的合成凭据。119仍运行时旧118根、下载及解包清理完成；重复清理无变化，默认入口重开正常，119公开检查返回已是最新版。未使用候选发现适配、未强杀、未访问或更新日常安装。详细原始结果与失败保留见源码分支的 docs/audits/PREVIEW_119_PUBLICATION_20261028.md。\n'
 live=s.api('PATCH','/repos/'+REPO+'/releases/'+str(state['releaseId']),dict(body=live['body']+extra))
 (o/'RELEASE_BODY_UPDATE.json').write_text(json.dumps(dict(status='PASS',releaseId=live['id'],marker=marker,preReleaseRetained=live['prerelease']),indent=2))
file=s.api('GET','/repos/'+REPO+'/contents/README.md?ref=main');old=base64.b64decode(file['content']).decode('utf-8');assert old.startswith('<!-- suite-download-entry-20261024 -->')
suffix=old[old.index('<!-- release-migration-20261023 -->'):]
prefix='''<!-- suite-download-entry-20261024 -->
> [!IMPORTANT]
> ## [Windows 预览版下载 — 0.0.0-preview.119](https://github.com/MisakaPiano/CCB-Studio-Suite/releases/tag/v0.0.0-preview.119) · [全部版本](https://github.com/MisakaPiano/CCB-Studio-Suite/releases)
> 在发行页 **Assets** 下载 **`CCB-Studio-Suite-0.0.0-preview.119-windows-x64.exe`**。**Source code (zip / tar.gz) 是源码，不是 Windows 安装器。**
>
> 这是**实验性邀请 Preview，尚非稳定版或全项目验收版**。119纳入编辑改善、工程收起／归档／完整包、独立预览窗口及内置本地AI识别。原版118→119公开签名发现、下载及真实NSIS隔离升级已核验。

当前主要限制：

- 本轮收起／重开、归档／历史重开、完整包导出再导入，以及此前预览窗口三步已获限定人工确认；资产管理和编辑剩余覆盖、115整体体验／任务栏固定、Prompt对比仍待验。旧q09像素断言失败根因仍UNKNOWN，不宣称Project独立验收通过。
- AI当前为本地检测框打码，非像素分割或智能擦除；准确率需用户检查。内置模型使安装包约252MiB，更新需要相应暂存空间。完整工程交换包仍限64MiB，字体文件／模型／凭据与本机撤销历史不包含在内。
- 组织和归档不复制原图；新导入只保留继续编辑所需内容，不额外留整包，已有导入资料不自动清理。文件管理与真正删除原文件的更广能力仍待后续实施。
- 更新／关闭排水、搬迁及所属清理沿用原机制；未知、自行修改和受保护内容保留，占用或权限失败安全重试。外部入口搬迁后可能需要重建，路径长度限制仍存在。
- 当前验证是本机同Windows账户的隔离案例；更多机器、网络盘、跨账户／跨机器凭据、长期CPU／RSS与大规模队列并未由本轮签收。Windows Authenticode发布者签名尚未提供；TUF签名不能替代发布者认证。Android及独立Server尚未实施。

[当前状态](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/feature/windows-rc-continuous/docs/CURRENT_STATE.md) · [本轮发行证据](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/feature/windows-rc-continuous/docs/audits/PREVIEW_119_PUBLICATION_20261028.md) · [首页同步流程](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/feature/windows-rc-continuous/docs/rc/PREVIEW_UPDATE_CONTINUITY.md)

---

'''
new=prefix+suffix;(o/'PUBLIC_MAIN_README_BEFORE.txt').write_text(old,encoding='utf-8');(o/'PUBLIC_MAIN_README_119.txt').write_text(new,encoding='utf-8')
if new!=old:
 published=s.api('PUT','/repos/'+REPO+'/contents/README.md',dict(message='Link Preview119 editor improvement release and retained validation limits',content=base64.b64encode(new.encode()).decode(),sha=file['sha'],branch='main'))
 commit=published['commit']['sha'];detail=s.api('GET','/repos/'+REPO+'/commits/'+commit);assert [x['filename'] for x in detail['files']]==['README.md']
else:commit=None
read=s.api('GET','/repos/'+REPO+'/contents/README.md?ref=main');assert base64.b64decode(read['content'])==new.encode()
(o/'README_PUBLICATION.json').write_text(json.dumps(dict(status='PASS',commit=commit,files=['README.md'],suffixPreserved=True,downloadPage=state['url'],preReleaseRetained=True,readmeSha256=hashlib.sha256(new.encode()).hexdigest(),legacyChannelModified=False),indent=2));print('README_AND_RELEASE_BODY_UPDATED')
