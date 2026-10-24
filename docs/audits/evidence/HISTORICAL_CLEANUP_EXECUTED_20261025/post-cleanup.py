import gzip,hashlib,json,os,runpy,shutil,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2];LOCAL=Path(__file__).resolve().parent
ns=runpy.run_path(str(LOCAL/'cleanup.py'),run_name='manual_postcheck')
out=ROOT/'docs/audits/evidence/HISTORICAL_CLEANUP_EXECUTED_20261025';out.mkdir(parents=True,exist_ok=True)
result=ns['read'](LOCAL/'RESULT.json')
with gzip.open(LOCAL/'PLAN.json.gz','rt',encoding='utf-8') as f:plan=json.load(f)
assert result['protectedAnchorsUnchanged'] and len(result['results'])==20
assert all(not x['deferred'] for x in result['results'])
active=ns['read'](ns['APP']/'launch-current.json');assert active==plan['active']
config=ns['read'](Path(active['installed'])/'update-config.json')
verification=ns['verify_install'](Path(active['installed']),{k:config[k] for k in ('product','platform','channel','version','sourceSha')})
assert active['version']==115
assert all(ns['digest'](Path(a['path']))==a['sha256'] for a in plan['anchors'])
retained=[(Path(entry['root']),r) for entry in plan['roots'] for r in entry['retained']]
assert all(os.path.lexists(ns['io_path'](root/r['path'])) for root,r in retained)
assert all(p.exists() for p in (ns['APP']/'111-1c9385afc38c',ROOT/'.local/windows-rc/model-accounts-review/workspace',ROOT/'.local/windows-rc/model-accounts-review/runtime'))
for name in ('smoke-107','114-f59b0c25a2eb','115-ff145d051bae'):
    assert (ROOT/'.local/windows-rc/performance-107'/name/'INSTALL_MANIFEST.json').is_file()
post=dict(status='PASS',filesRemoved=result['filesRemoved'],bytesFreed=result['bytesFreed'],gibFreed=result['bytesFreed']/1024**3,roots=20,deferred=0,retainedEntryExistenceChecks=len(retained),protectedAnchors=plan['anchors'],dailyCurrentVersion=115,dailyProgramVerification=verification,protected111AndBackupsAndBaselinesPresent=True,userDataOrPrivateCredentialContentsRead=False,preservationScope='Excluded all protected roots and backup/evidence subtrees; no full private-data byte inventory or credential read',automatedUpdaterUsedForDailyCleanup=False,recycleBinCleared=False,helperSha256=ns['digest'](LOCAL/'cleanup.py'))
ns['write'](out/'POSTCHECK.json',post)
for name in ('PLAN_SUMMARY.json','PLAN.json.gz','REMOVALS.jsonl.gz','RESULT.json','cleanup.py','validate-helper.py'):
    shutil.copyfile(LOCAL/name,out/name)
rows='\n'.join('| `'+r['root']+'` | '+str(r['removed'])+' | '+str(r['bytesFreed'])+' | '+str(len(r['protectedEntries']))+' |' for r in result['results'])
doc='''# 已授权历史残留清理实际回执

2026-10-25 Asia/Tokyo。用户已直接授权清理清单中确认的旧测试程序、旧共享测试内容和日常目录中不再需要的程序残留；复审证据、固定基线、日常当前安装、备份及未知归属保留。本次是精确清单的人工清理，与尚未发布的新更新器自动机制分开。

仅20个明确根，逐文件核对原计划身份后按句柄删除，目录只在空时移除；未递归清理整个H盘，没有清空回收站。共享测试根中名称表明备份、迁移或证据的子树不遍历、不删除；归属未知、重解析点和不安全名称保留。清日常残留时取得更新缓存锁并重新确认默认入口，未调用新自动清理机制。

实际移除 **FILES** 文件、**BYTES** 字节（**GIB GiB**），延后0项；1086个明确保留项均仍存在。当前115完整程序清单复核通过，12项受保护锚点散列一致；活动记录仍保护的111、原活动备份、107/114/115固定测试基线保留。用户资料和私人凭据内容未读取，不能把排除范围与入口散列检查写成全量私人资料逐字节验证。

| 准确根 | 移除文件 | 回收字节 | 保留条目 |
|---|---:|---:|---:|
ROWS

两个共享测试根因为保留备份和证据而仍存在；这不代表可以进一步一键清空。上述程序以外的旧目录和早期失败现场没有获得本次精确归属证明，未删除。111不能绕过既有保护记录自动当垃圾处理。成功清理后无需继续使用旧105/107/109/112/114二进制回滚；用户当前115、备份和迁移原件仍保留。

完整逐文件计划、删除日志、精确源归属和事后检查见 [证据目录](evidence/HISTORICAL_CLEANUP_EXECUTED_20261025/POSTCHECK.json)、同目录PLAN.json.gz、REMOVALS.jsonl.gz、RESULT.json。清理脚本仅是本次历史操作的证据，不是后续执行授权；不可直接重放。
'''.replace('FILES',str(result['filesRemoved'])).replace('BYTES',str(result['bytesFreed'])).replace('GIB',f"{post['gibFreed']:.2f}").replace('ROWS',rows)
(ROOT/'docs/audits/HISTORICAL_CLEANUP_EXECUTED_20261025.md').write_text(doc,encoding='utf-8')
print(json.dumps({k:v for k,v in post.items() if k not in ('protectedAnchors',)},indent=2))
