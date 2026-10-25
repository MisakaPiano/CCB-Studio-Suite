from pathlib import Path
import hashlib,json,subprocess,zipfile,io
ROOT=Path('H:/CCB-Studio-Suite')
DEST=ROOT/'docs/audits/evidence/PERFORMANCE_TWO_FOLLOW_ONS_20261025'
DOC=ROOT/'handoff/PERFORMANCE_TWO_FOLLOW_ONS_REVIEW_READY_20261025.md'
ORIGINAL='7314ef9ac092c0056ffa6d8918472cbf8868ec93'
BASE='fa58a50c44a592df0f0c8745ba773c244d7442d9'
APP='52e0bf05a5844f85c59d3bb79a71b7f2c3182858'
def sha(b):return hashlib.sha256(b).hexdigest()
def git(*args):return subprocess.check_output(['git',*args],cwd=ROOT)
def blob(commit,path):return git('show',commit+':'+path)
def fixed(commit,path):return 'https://github.com/MisakaPiano/CCB-Studio-Suite/blob/'+commit+'/'+path
matrixPath='docs/audits/evidence/PERFORMANCE_INCREMENT_INDEX_20261025/REVIEW_MATRIX.json'
original=blob(ORIGINAL,matrixPath)
assert sha(original)=='cc69b9a92313c81aadde911ae2122d379cf36855924f440e3e1d986704932ca1'
assert len(json.loads(original)['rows'])==16 and json.loads(original)['applicationBoundary']==BASE
rows=[]
for scope,app,evidence,stem,report,expectedSize,expectedSha in [
    ('文件夹批次筛选','1256c64ddd4291f585be4ff8d086f98a404a972b','37e434606f10930d5169961bb33b0dd282bbee6a','PERF003_FOLDER_BATCH_SELECTION_20261025','PERF003_FOLDER_BATCH_SELECTION_20261025.md',116842,'a5a089a2e5e1059320aac57b10f3ab8e9e88d063bfe1af20c96c1a1d83b118f7'),
    ('标签相册默认配方','52e0bf05a5844f85c59d3bb79a71b7f2c3182858','74dfb15a764d47d618d658bdf5d7e07047b4a9bc','PERF003_TAG_ALBUM_DEFAULTS_20261025','PERF003_TAG_ALBUM_DEFAULTS_20261025.md',76259,'c9ac75067185b9112282b3ddf75ae00720d49d6f248607eae7bd153c27628385')]:
    folder='docs/audits/evidence/'+stem
    mapping=blob(evidence,folder+'/SOURCE_MAP.json');parsed=json.loads(mapping);assert parsed['sources']['final']==app
    summary=json.loads(blob(evidence,folder+'/runs/final/summary.json'))
    assert(summary['tests'],summary['failures'],summary['errors'],summary['skipped'])==(4,0,0,0)
    assert summary['sourceStable'] and summary['baseCommit']==app
    raw=blob(evidence,folder+'/RAW_EVIDENCE.zip');assert len(raw)==expectedSize and sha(raw)==expectedSha
    with zipfile.ZipFile(io.BytesIO(raw)) as z:
        assert z.testzip() is None
        manifest=json.loads(z.read('RAW_MANIFEST.json'))
        for item in manifest['files']:assert sha(z.read(item['path']))==item['sha256']
    reportPath='docs/audits/'+report;reportBytes=blob(evidence,reportPath);assert app.encode() in reportBytes
    rows.append(dict(scope=scope,applicationSource=app,evidenceCommit=evidence,report=reportPath,fixedReport=fixed(evidence,reportPath),sourceMap=folder+'/SOURCE_MAP.json',sourceMapSha256=sha(mapping),rawArchive=folder+'/RAW_EVIDENCE.zip',rawArchiveBytes=len(raw),rawArchiveSha256=sha(raw),finalUniqueMethodsReported=4,resultsFromFixedHistoricalRunNotNewExecution=True,independentReview='NOT REQUESTED',humanAndPackage='NOT TESTED'))
changed=git('diff','--name-only',BASE,APP,'--','modules','tools').decode().splitlines()
production=[p for p in changed if '/src/test/' not in p]
assert set(production)=={'modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/GenerationOrganization.kt','modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/LibraryCatalogProjection.kt','modules/studio-desktop/src/main/kotlin/io/github/misakapiano/ccbstudio/desktop/GalleryOrganization.kt','modules/studio-desktop/src/main/kotlin/io/github/misakapiano/ccbstudio/desktop/GalleryWorkspace.kt'}
assert git('diff',BASE,APP,'--','modules/image-authority')==b''
org=production[production.index('modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/GenerationOrganization.kt')]
assert next(x for x in blob(BASE,org).splitlines() if x.startswith(b'    fun descendants('))==next(x for x in blob(APP,org).splitlines() if x.startswith(b'    fun descendants('))
auditCommit='50de30fdd15275e7b5d1226ec04d954ede1d5f11';auditPath='docs/audits/OPERATOR_PREPARATION_PATH_ERROR_20261025.md';audit=blob(auditCommit,auditPath)
v=dict(format=1,preparedOnly=True,sent=False,applicationBoundary=APP,oldIndex=dict(commit=ORIGINAL,applicationBoundary=BASE,matrix=matrixPath,matrixSha256=sha(original),historicalRows=16,notRewrittenOrResent=True),rows=rows,newExecutionsByIndex=0,noSumOrSingleLatestSourceEndToEndAcceptance=True,changedModuleAndToolPaths=changed,productionPaths=production,authorityUnchanged=True,rejectedTraversalNotPresent=True,operatorAudit=dict(commit=auditCommit,path=auditPath,sha256=sha(audit),priorFileExistenceAndContents='UNKNOWN',exactEventUtc='UNKNOWN;bounded02:47:52..02:54:55UTC',notProductTestResult=True),excludedPending=dict(jobDateBaseline='3d7369963ec942fed58b1a1963d2c5d9ccc397e4 / NOT EXECUTED;two resource gates each0executions',colorParser='LOCAL DRAFT ONLY / NOT APPLIED OR EXECUTED'),package='NONE',release='NONE',human='NOT TESTED',independent='NOT REQUESTED',assetsAndEditor='USER_NOT_TESTED',preview115Experience='PENDING',promptComparison='PENDING',q09='FAILED/root UNKNOWN',product='PRODUCT_HOLD / NOT RC')
assert not DEST.exists() and not DOC.exists();DEST.mkdir(parents=False)
data=(json.dumps(v,ensure_ascii=True,indent=2)+'\n').encode();(DEST/'FOLLOW_ON_MATRIX.json').write_bytes(data)
(DEST/'.gitattributes').write_bytes(b'* -text\n**/* -text\n');(DEST/'build_index.py').write_bytes(Path(__file__).read_bytes())
text='''# 性能复审补充：仅两段，尚未发送

2026-10-25 JST。PREPARED ONLY / NOT SENT；新载荷没有外发批准或独立结论。本补充不替换、不重发[原16项固定索引](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/7314ef9ac092c0056ffa6d8918472cbf8868ec93/handoff/PERFORMANCE_INCREMENT_REVIEW_READY_20261025.md)，其fa58a50边界和原包保持。本补充只固定之后两项已完成增量，应用边界[52e0bf05a5844f85c59d3bb79a71b7f2c3182858](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/52e0bf05a5844f85c59d3bb79a71b7f2c3182858)，[相对fa58a50差异](https://github.com/MisakaPiano/CCB-Studio-Suite/compare/fa58a50c44a592df0f0c8745ba773c244d7442d9...52e0bf05a5844f85c59d3bb79a71b7f2c3182858)。

|增量|实际应用/执行|固定报告与原始证据|自动结果|
|---|---|---|---|
'''
for row in rows:text+='|'+row['scope']+'|'+row['applicationSource']+'|['+row['evidenceCommit'][:7]+'报告]('+row['fixedReport']+')|该源码4唯一方法PASS，非新执行/不相加|\n'
text+='''
文件夹筛选只把每批次重复求子树变成每筛选一次；原递归、完整校验、归档策略差异、null空候选短路、批次顺序/来源/索引/布局不变。256节点512批次根配对201.039→0.486ms、161761928→322720B仅局部元数据；不代签整页/RSS。标签相册只调用内按原canonical构造复用不可变默认recipe，entry/图像/时钟/ID/key保持、权威Prompt/请求未改；分配少约68%～73%，最大配对墙钟4.590→4.845ms略慢，原结果保留，**不称提速**。

[机器映射](../docs/audits/evidence/PERFORMANCE_TWO_FOLLOW_ONS_20261025/FOLLOW_ON_MATRIX.json)，SHA256 `MATRIX_SHA`。从固定Git重算两份SOURCE_MAP、原始ZIP大小/SHA/CRC/清单项，并核对原16项矩阵SHA；**索引不产生新测试执行、总数或最新52e0bf0单一端到端验收**，各方法仅对应其各自源码/实际输入。生产相对fa58a50只有4文件，权威模块未改；旧遍历方法已恢复，旧试验仍未采用。

|状态类别|本补充实际范围|
|---|---|
|实现完成|上述两段源码已固定，非整专项完成|
|自动执行通过|1256c64与52e0bf0各自最终4方法，基线/内部轮次另列，不累加|
|用户手动确认|无新增；资产/编辑USER_NOT_TESTED，115体验/Prompt比较分别PENDING|
|独立审查通过|新增量NOT REQUESTED；旧批次/原生/启动PASS_WITH_SCOPE不外推|
|仍未验证|本源码整包/原生升级/日常安装/真实大库、完整UI、峰值堆/GC/RSS/长期/物理IO/所有Windows；无包/tag/安装器SHA或更新元数据新映射|

## 必须保留的边界

- [遍历试验全部候选/负面样本/恢复](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/124bd4e0998772ce4b68f1e72cfb6f1b9059230d/docs/audits/PERF003_FOLDER_TRAVERSAL_EXPERIMENT_20261025.md)仍NOT ADOPTED；两预热大根慢样本、预热改变方向与根因UNKNOWN不改判。新一次求值不能抹掉旧失败。
- [执行者路径错误及后续约束](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/50de30fdd15275e7b5d1226ec04d954ede1d5f11/docs/audits/OPERATOR_PREPARATION_PATH_ERROR_20261025.md)另列：既有记录是补丁目标误填，先前文件存在性/内容和精确误写UTC UNKNOWN。执行纪律与产品测试分开，不能声称全程无越界、确定没有覆盖或无影响；不再访问该路径追查。该说明不回写旧源码/原始包，也不是产品测试PASS/FAIL。
- 作业日期基线3d73699尚未执行；两轮资源门各0执行，第三轮等待Desktop，不属于本补充通过结果。颜色解析仅本地草稿，未写入modules/未测，不算实现。已接受旧批次9/启动不重跑，旧失败/用户否决/q09 FAILED-root UNKNOWN及PRODUCT_HOLD / NOT RC保持。
- Desktop资源优先、边界采样不是持续静默或CPU归因；原16项中ZIP独立签收TOOL_ACCESS_LIMIT等限制不因本补充消失。旧下载清理P2/六项修复固定对象与原Desktop技术报告范围不扩大；未触发Desktop开发、真实账户/服务/资料、暂停扫描、签名/信任链、安装/发布或新分享。

后续如用户明确批准此具体补充载荷，Project应先核固定源/原字节/SOURCE_MAP与逐例、4处文件夹筛选的归档/顺序/null等价、canonical默认值的可共享性及每entry时钟，再独立裁定本补充。报告完成不等于全项目/用户/Windows或Desktop验收；当前不发送。
'''
DOC.write_text(text.replace('MATRIX_SHA',sha(data)),encoding='utf-8')
print(json.dumps(dict(status='PREPARED_ONLY_NOT_SENT',applicationBoundary=APP,matrixSha256=sha(data),rows=2,newExecutions=0,rawArchives=[dict(bytes=r['rawArchiveBytes'],sha256=r['rawArchiveSha256']) for r in rows]),indent=2))
