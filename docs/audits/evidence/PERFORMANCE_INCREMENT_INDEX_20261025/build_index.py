from pathlib import Path
import hashlib,json,re,subprocess
root=Path('H:/CCB-Studio-Suite');ids=json.loads((root/'.local/perf-review-summary-20261025/SOURCE_IDS.json').read_text(encoding='utf-8'))
rows=[
 ('图库原件导出','accdc59','c2ae633','PERF005_ORIGINAL_EXPORT_20261025',5),
 ('共享复制/排版原件','5bfa51d','0e1b3cd','PERF005_SHARED_ORIGINAL_COPY_20261025',12),
 ('受管目录副本','3086895','2ba1f02','PERF001005_INGRESS_COPY_20261025',4),
 ('复制回执摘要','ec75083','aa2ca0a','PERF001005_RECEIPT_DIGEST_20261025',16),
 ('旧编辑原件导出','5a91e99','4fd0202','PERF005_EDITOR_ORIGINAL_20261025',5),
 ('采集输入摘要','cb819b4','334f6ac','PERF006_COLLECTION_HASH_20261025',5),
 ('公共有界读取','27e5621','cd2e3f4','PERF001005_BOUNDED_READ_20261025',17),
 ('完整资产字节返回','66ed23c','53a6de4','PERF001005_WORKSPACE_BYTES_20261025',7),
 ('Journal提交正文','6dfadc0','1b6e3b6','PERF001007_JOURNAL_COMMIT_20261025',12),
 ('目录/副本组合观察','6dfadc0','a0b65a5','PERF001006_INTEGRATION_20261025',3),
 ('摘要等价hex编码','8f4597d','a21444e','PERF001007_HASH_ENCODING_20261025',7),
 ('虚拟相册成员并集','1553d8b','ff5998b','PERF003_VIRTUAL_MEMBERS_20261025',5),
 ('管理标签构造计数','490c596','a080970','PERF003_TAG_INDEX_20261025',4),
 ('记录日期元数据副本','492f8cc','3bda776','PERF003_RECORDED_TIMES_20261025',4),
 ('目录相册消费者副本','605f89e','7905fd1','PERF003_DIRECTORY_COPY_20261025',2),
 ('标签建议自然键','fa58a50','cda1c0f','PERF003_TAG_SUGGESTIONS_20261025',4)]
def blob(commit,path):return subprocess.check_output(['git','show',commit+':'+path],cwd=root)
matrix=[];rendered=[]
for title,app,ev,report,count in rows:
    reportPath='docs/audits/'+report+'.md';text=blob(ids[ev],reportPath).decode()
    folders=list(dict.fromkeys('docs/audits/evidence/'+x for x in re.findall(r'evidence/([^/\s)]+)/',text)))
    candidates=[]
    for folder in folders:
        result=subprocess.run(['git','show',ids[ev]+':'+folder+'/SOURCE_MAP.json'],cwd=root,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        if result.returncode==0 and ids[app] in result.stdout.decode():candidates.append((folder,result.stdout))
    assert len(candidates)==1,(app,'Expected one report-linked source map containing exact application SHA',folders)
    folder,sourceMap=candidates[0];archive=blob(ids[ev],folder+'/RAW_EVIDENCE.zip');json.loads(sourceMap)
    item=dict(scope=title,applicationSource=ids[app],evidenceCommit=ids[ev],fixedReport=reportPath,sourceMap=folder+'/SOURCE_MAP.json',sourceMapSha256=hashlib.sha256(sourceMap).hexdigest(),rawArchive=folder+'/RAW_EVIDENCE.zip',rawArchiveBytes=len(archive),rawArchiveSha256=hashlib.sha256(archive).hexdigest(),finalUniqueMethodsReported=count,countsAreFromFixedReportNotNewExecutions=True,independentReview='NOT REQUESTED',userAndPackage='NOT TESTED')
    matrix.append(item)
    rendered.append(f'|{title}|[{app}](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/{ids[app]})|[{ev}固定报告](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/{ids[ev]}/{reportPath})|{count}，仅该源码报告范围|')
base=subprocess.check_output(['git','rev-parse','--verify','f5b008e'],cwd=root,text=True).strip()
data=dict(format=1,preparedOnly=True,sent=False,applicationBoundary=ids['fa58a50'],historicalStart=base,diffUrl='https://github.com/MisakaPiano/CCB-Studio-Suite/compare/'+base+'...'+ids['fa58a50'],rows=matrix,testCountTotal='NOT COMPUTED;overlapping methods/source revisions must not be added',pendingFolderBaselineExcluded='OUT OF SCOPE:e3989bc test source and subsequent folder investigation;no implementation conclusion included,see current task record;root-only f6 gate0executions',package='NONE;no package mapping or release for this performance boundary',oldDesktopUpdateReport='Unchanged fixed update-scope document,not expanded by this index')
out=root/'docs/audits/evidence/PERFORMANCE_INCREMENT_INDEX_20261025';assert not out.exists();out.mkdir(parents=True)
(out/'.gitattributes').write_bytes(b'* -text\n**/* -text\n')
(out/'REVIEW_MATRIX.json').write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8',newline='\n')
(out/'build_index.py').write_bytes(Path(__file__).read_bytes())
(out/'PREPARATION_FAILURE.md').write_text('First index builder assumed an explicit SOURCE_MAP/RAW_MANIFEST Markdown link;the original export report links ANALYSIS/runs/ZIP instead. Assertion stopped before output creation,0 tests executed. Changed discovery to report-linked evidence folders and require exactly one source map containing the full expected application SHA. No existing report/source/evidence modified;all raw ZIP hashes computed from their fixed Git evidence commits. Failed script retained in local build-index-failed.py.\n',encoding='utf-8')
(root/'.local/perf-review-summary-20261025/MATRIX_ROWS.md').write_text('\n'.join(rendered)+'\n',encoding='utf-8')
print(json.dumps(dict(rows=len(rows),matrixSha256=hashlib.sha256((out/'REVIEW_MATRIX.json').read_bytes()).hexdigest(),diffUrl=data['diffUrl'],application=ids['fa58a50']),indent=2))
