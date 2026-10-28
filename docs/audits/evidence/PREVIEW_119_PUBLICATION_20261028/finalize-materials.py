from pathlib import Path
import json,hashlib,shutil,subprocess,re
r=Path('H:/CCB-Studio-Suite');o=Path(__file__).parent;E=r/'docs/audits/evidence/PREVIEW_119_PUBLICATION_20261028'
load=lambda p:json.loads(Path(p).read_text(encoding='utf-8-sig'));sha=lambda p:hashlib.sha256(Path(p).read_bytes()).hexdigest()
state=load(o/'RELEASE_STATE.json');fixed=load(o/'FIXED_DELIVERY.json');meta=load(o/'METADATA_PUBLICATION.json');read=load(o/'FINAL_PUBLIC_READBACK.json');entry=load(o/'README_PUBLICATION.json');retired=load(o/'OWNED_FIXTURE_RETIREMENT.json');assert read['status']=='PASS' and retired['status']=='PASS'
assert sha(fixed['retainedInstaller'])==fixed['sha256'];assert not E.exists();E.mkdir(parents=True);(E/'.gitattributes').write_text('* -text\n')
def copy(p,d):
 assert p.stat().st_size<8*1024**2;d.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(p,d)
# Deliberately exclude profiles, workspaces, DPAPI data, installers, binaries and signing custody.
for p in o.iterdir():
 if p.is_file() and p.suffix in ['.json','.py','.java','.log','.txt','.md']:copy(p,E/p.name)
for name in ['public-native','signed-public-metadata']:
 for p in (o/name).rglob('*'):
  if p.is_file() and p.suffix in ['.json','.jsonl','.log','.txt']:copy(p,E/name/p.relative_to(o/name))
for folder in o.glob('metadata-attempt-*'):
 p=folder/'PREPARED.json'
 if p.exists():copy(p,E/folder.name/p.name)
review=r/'.local/project-bar-import-20261028-final';validation=load(review/'REVIEW_VALIDATION_INDEX.json')
for name in ['REVIEW_VALIDATION_INDEX.json','OWNED_TEST_FIXTURE_CLEANUP.json']:
 copy(review/name,E/'editor-review'/name)
for phase in validation['phases']:
 folder=Path(phase['path']);dest=E/'editor-review'/folder.name
 for p in folder.iterdir():
  if p.is_file() and p.suffix in ['.json','.xml','.log']:copy(p,dest/p.name)
 for sub in ['failed-source','screenshots']:
  if (folder/sub).exists():
   for p in (folder/sub).rglob('*'):
    if p.is_file() and p.suffix in ['.kt','.png']:copy(p,dest/p.relative_to(folder))
copy(r/'.local/image-window-20261028/USER_WINDOW_ACCEPTANCE.json',E/'USER_WINDOW_ACCEPTANCE.json')
for name in ['JAR_PACKAGE_VERIFICATION.json']:
 copy(r/'.local/model-bundle-20261028'/name,E/'historical-model'/name)
modelPhase=r/'.local/windows-preview/evidence/20261027T170800326125Z-bundled-automask-fixed-inputs-20261028'
for p in modelPhase.iterdir():
 if p.is_file() and p.suffix in ['.json','.xml','.log']:copy(p,E/'historical-model'/modelPhase.name/p.name)
buildPhase=Path(load(o/'PACKAGE.json')['phase'])
for n in ['summary.json','source-before.json','gradle.log']:copy(buildPhase/n,E/'app-image'/n)
sumFreed=sum(x['result']['bytesFreed'] for x in retired['fixtures']);sumFiles=sum(x['result']['removed'] for x in retired['fixtures'])
source=state['tagSource'];base='https://github.com/MisakaPiano/CCB-Studio-Suite'
report=f'''# Preview119：编辑改善阶段收尾、公开更新链与材料交接

2026-10-28 Asia/Tokyo。状态 **PUBLISHED_PUBLIC_CHAIN_VERIFIED_EDITOR_STAGE_CLOSED_WITH_SCOPE**。用户明确确认本轮三项通过，并要求收尾发布；不冒充稳定版、全项目验收或Project独立复审通过。日常安装仍未由执行者更新。

## 固定对应关系

| 对象 | 固定身份 |
| --- | --- |
| Pre-release | [v0.0.0-preview.119]({state['url']})，Release ID {state['releaseId']}，发布时间 {state['publishedAt']} |
| 应用映像输入 | [e012e2e098e20d40de97688cae376978840c3575]({base}/tree/e012e2e098e20d40de97688cae376978840c3575) |
| 来源记录／最终组装与tag | [{source}]({base}/tree/{source})；模型与编辑早期本地快照03f589c／9e3b182随祖先正常推送，不把旧“未验收”历史改写 |
| 安装包 | CCB-Studio-Suite-0.0.0-preview.119-windows-x64.exe，264,116,085 bytes |
| SHA-256 | **{fixed['sha256']}** |
| 更新元数据 | [{meta['commit']}]({base}/commit/{meta['commit']})，targets18／snapshot28／timestamp28；原root1与旧桥接不改 |
| 根SHA-256 | {read['rootSha256']} |
| main首页 | [{entry['commit']}]({base}/commit/{entry['commit']})，仅README.md，旧正文分界线后字节保留 |

四个发行附件为安装器、RELEASE_NOTES.md、THIRD_PARTY_NOTICES.zip、SHA256SUMS.txt；ID／大小／散列见[RELEASE_STATE](evidence/PREVIEW_119_PUBLICATION_20261028/RELEASE_STATE.json)。完整包公开流式下载核对通过后才签发现有三角色元数据；无根密钥创建／轮换／导出，不替换旧附件或force推送。Windows Authenticode发布者签名仍未提供。

## 实际纳入与限定人工结果

- 本轮编辑操作、三栏与队列／图层、连续工具设置、竖排、命名预设、快捷键和多种成品导出沿已批准方案收尾；性能只保留既有局部改进，不声称全机流畅验收。
- 工程栏去重，标题占中间剩余宽度，废弃完整文案及默认红色垃圾桶在最右导出左侧。收起保存并清空；归档沿用同一工程与原图、保留历史；废弃必须确认并回收可证明独占进度，生成文件另选确认，依赖原图／共享内容／已有备份保留。
- 新完整包导入不再把整包额外存成资产，只保存原图及支持的当前编辑内容；外来历史／未知字段不保留，确认摘要明确提示。外部包及已有导入资料不清理；旧源的读取／另存兼容保留。完整导出包含原图与当前内容，不含本机撤销历史、字体文件、模型、凭据和组织树，仍限64MiB。
- 默认本地AI权重实际内置，47,600,269 bytes，SHA a12ac5532e93be9dfeb96a77fc3f3647791335c9df0de9a18fcd503f7877a828。固定作者模型卡MIT声明与许可／来源随包保存，不把另一上游应用的许可UNKNOWN变成已授权。工作区不按工程复制权重；自选模型保留。
- 编辑预览和共用图像浏览宿主改独立原生窗口，最大化／还原／关闭隔离，标题栏跟随主题。

用户本次明确通过：**收起／本机列表重开保留进度；归档／历史重开；完整包导出再导入继续编辑**。前次已单独确认预览最大化／还原／关闭。其他此前确认按原记录保留，不外推资产管理、全部编辑能力、115整体体验、Prompt或独立审查。[本次原话与固定候选](evidence/PREVIEW_119_PUBLICATION_20261028/USER_EDITOR_ACCEPTANCE.json)。

## 自动与包级验证，分层不累加

1. 本轮工程栏／新导入有13唯一JUnit分阶段PASS证据；重复不累计。[逐例索引](evidence/PREVIEW_119_PUBLICATION_20261028/editor-review/REVIEW_VALIDATION_INDEX.json)保留全部7次流程状态。核心9例所处流程因桌面旧调用编译失败仍是FAILED；后修复的桌面结果分列，不篡改原失败。首轮比较重新生成对象ID的测试错误及原失败源、封面挤掉入口的截图与用户标题否决均保留。
2. 内置模型历史6唯一例、窗口原生／相关回归沿固定证据复用，不计成本轮新增13例。模型真实权重处理合成图只证明管线，非准确率验收；upstream观察器另有3个合成用例，不计编辑功能用例。
3. 实际createDistributable：应用源码e012e2e，离线严格依赖／单worker／低优先级／JDK17，sourceStable=true；这是构建成功，JUnit0，不称测试通过。
4. 119真实NSIS隔离安装返回0；551项大小／SHA逐项通过。安装后生产JAR类路径加载模型，resourceProtocol=jar、权重长度／散列及7类别通过；没有从外部模型路径偷取。三份应用JAR与用户刚试用候选完全相同，[等价映射](evidence/PREVIEW_119_PUBLICATION_20261028/APPLICATION_EQUIVALENCE.json)。
5. 公开118→119原生链如下，不用之前117→118成功替代，也没有再次要求已确认编辑人工项目。

## 公开118→119：新增一条真实隔离链

环境：本机Windows x64、同Windows账户、H盘NTFS，专用隔离HOME／USERPROFILE／APPDATA／TEMP；全为两张合成原图与两个账户引用的DPAPI合成凭据。旧118安装器仍为3540be44ca7a1fcfd583899b325abad7630e10b120d288e9f2e7dc7a1e8bd60d。

- 原118生产Main从默认入口启动、正常关闭，取合成资料／凭据快照。
- **原版118包内客户端**从公开long-term-metadata正式签名源发现并实际下载119，返回DOWNLOADED_VERIFIED；原root、版本／身份及安装器实际字节核对，无候选发现适配。
- 原118 helper绑定真实父进程、READY_TO_DRAIN、正常关闭旧窗口、真实NSIS安装119，119生产Main完成程序／资料／凭据ACK，回执COMPLETE。
- 119仍运行时旧118根消失、downloads与installing为0；备份哨兵、两张原图与两项合成凭据保持。实际清理一轮removed555／deferred0；随后重复清理removed0，成功交接回执字节未变。
- 新版正常关闭后核对合成原图／凭据，默认入口重开119并正常关闭；119包内公开检查UP_TO_DATE。没有强杀或修改日常安装。

[原始结果](evidence/PREVIEW_119_PUBLICATION_20261028/public-native/RESULT.json)、[原118公开命令](evidence/PREVIEW_119_PUBLICATION_20261028/public-native/OLD118_PUBLIC_COMMAND.json)、[交接回执](evidence/PREVIEW_119_PUBLICATION_20261028/public-native/RAW_UPDATE_RESULT.json)、[清理事件](evidence/PREVIEW_119_PUBLICATION_20261028/public-native/cleanup-events.jsonl)。旧GUI交接由隔离驱动衔接，未自动点击GUI更新按钮，不称日常GUI已升级。

## 清理、来源与保留

本轮完成后，3份自行创建的包构建／隔离安装／升级fixture先留证、验证固定119安装包另行保留，再按原owner＋逐文件manifest退役：{sumFiles}项／{sumFreed} bytes（约{sumFreed/1024**3:.2f}GiB）。未知删除0；未碰旧失败、固定基线、用户本地候选试用根、已有导入、日常安装、真实资料／凭据／插件／备份。[清理回执](evidence/PREVIEW_119_PUBLICATION_20261028/OWNED_FIXTURE_RETIREMENT.json)。固定119包留在仓库.local/preview-publication-119-20261028/delivery，仅一份保留物，不把安装包当临时垃圾。

更新器6脚本、安装器生成器与Launcher.cs相对118源码a1202e2未变化；新公开链验证其在更大模型包上的实际使用，不是又重写了清理或信任协议。原111历史人工退役及Desktop报告仍按原事实保留。本次源码冻结准备脚本参数错误、探针生成引号错误和无状态影响的占位命令拼写错误分列PREPARATION_CORRECTIONS；均未改产品结果或旧失败。

## 未完成与下一步

本轮编辑改善阶段收尾；整体编辑覆盖／资产管理完整人工、115体验／任务栏固定、Prompt对比、q09旧像素断言根因UNKNOWN、更多环境／网络盘／跨账户／跨机器凭据及长期CPU/RSS不随之完成。本轮独立审查NOT REQUESTED，旧Project结论只按其原范围保留。

下一步先迁移新对话，承接既有PERF-001～007基线确认新的性能范围；资产文件管理与真正删除、更多图像能力、Tag库按既有任务排队，Prompt继续PENDING。详见[新对话交接](../../handoff/STUDIO_NEXT_CONVERSATION_20261028.md)。日常118可由用户正常更新页升级；本次没有自动更新或读取其资料。
'''
(r/'docs/audits/PREVIEW_119_PUBLICATION_20261028.md').write_text(report,encoding='utf-8')
handoff=f'''# Studio 编辑改善收尾：新对话接续

2026-10-28，Asia/Tokyo。只在H:\\CCB-Studio-Suite继续；Desktop只读，不自动启动另一个项目。

## 已完成的本轮

Preview119已公开，tag {source}，应用映像e012e2e，安装包SHA256 {fixed['sha256']}；正式元数据{meta['commit']}，原根不变。原版118公开签名下载／旧helper真实安装／程序资料与合成凭据确认／119运行时旧根清理／重开及当前检查通过。用户刚确认工程三项通过，前次窗口三项通过，按限定范围阶段收尾。没有把Codex自测当Project独立验收，没有自动更新日常118。

[发行报告](../docs/audits/PREVIEW_119_PUBLICATION_20261028.md) · [任务清单](../docs/TASK_REGISTRY.md) · [当前状态](../docs/CURRENT_STATE.md) · [编辑实现与原失败](../docs/ux/EDITOR_ACCEPTANCE_REPLAN_20261027.md) · [本轮工程栏](../docs/ux/EDITOR_PROJECT_BAR_IMPORT_20261028.md)。历史否决与失败不要删除或改标。

## 接下来仍在计划里

|顺序／原任务|剩余事项|边界|
|---|---|---|
|下一项 · PERF-001～007|承接既有性能与资源基线，优先讨论实际卡顿、较大编辑队列／图库、切换图片和CPU／内存；具体范围及规模先确认|旧微优化工程阶段已收尾；不重复测旧通过项目，也不把本表当立即启动新测量或代码的授权|
|用户安排 · UX-RECOVERY/W2–W4|资产管理完整人工、编辑未覆盖能力及更大真实使用、115整体体验／任务栏固定|这轮已通过的工具／窗口／工程三项不用重测；逐项签收，不再笼统记编辑完全未测试|
|后续 · UX-ASSET-FS-001|直接管理本地文件与文件夹；批量重命名、复制、移动、删除；区分移出图库、回收站与永久删除原文件|当前编辑“废弃工程”只实现该工程独占进度与用户另选的所属生成物；不代表图库／所有页面已能永久删除。真实文件操作需相应确认|
|后续 · UX-EDITOR-GAP-001余项|继续按TypeSet／unified-image-tool差距确定值得借鉴的图像能力|本地检测框AI打码、默认内置模型及一键跟随工具已做；像素分割、智能擦除等不是现成功能，也不直接整体复制上游未知许可代码|
|后续 · TAG-001～005／Tag库|整理标签、分类和原始tag映射、方便调取的Tag库|概念／长期计划，详细范围和优先级待确认，未实施|
|继续等待 · Prompt对比|保留已批准布局及原始Prompt权威关系，等待用户重新启动|明确PENDING；不因编辑收尾自动开工|
|长期平台|Android及独立Server|仍是长期目标，本次Windows Preview不代表这些平台已实现|

更新收尾沿现有任务维护，不重开整套清理机制。缓存忙／空间不足的英文提示及更广异常组合、搬迁外部入口／任务栏等旧限制仍在发行报告中；有后续反馈再定范围。上游heartbeat studio按既有来源记录只观察变化，不自动采用／下载模型／改SOURCE_LOCK。

## 建议粘贴给新对话的接续指令

先读取本报告、AGENTS.md、docs/00_INDEX.md、CURRENT_STATE、TASK_REGISTRY、SOURCE_LOCK和现有活动合同，核对实际分支、HEAD、index及未提交内容。承接Preview119编辑改善的限定完成状态和所有旧失败／未验，不重做已通过人工。先与用户确认下一轮性能优化的具体范围和规模，依据PERF-001～007旧基线提出最小测量及验证顺序；本交接不自动授权新性能测试、私人资料操作、新版本发布或Desktop开发。资产／本地文件真正删除、更多图像能力和Tag库沿既有编号排队，Prompt对比继续pending。
'''
(r/'handoff/STUDIO_NEXT_CONVERSATION_20261028.md').write_text(handoff,encoding='utf-8')
head='## 2026-10-28 Preview119已公开，本轮编辑改善阶段收尾（限定签收）\n\n用户明确确认工程三项通过；119纳入累计编辑、默认内置模型、独立预览窗口、工程栏去重和新导入精简保存。源码／tag d9be9d9，映像e012e2e，安装包SHA256 b425ae40…75f1c83；正式元数据9b86ac3／targets18-snapshot28-timestamp28，原root及旧桥接不变。551安装文件、模型生产JAR读取、与试用JAR等价、原版118公开签名下载／真实helper-NSIS-生产Main／合成原图凭据保全／119运行时旧根和缓存清理／重开及UP_TO_DATE均有证据。详见docs/audits/PREVIEW_119_PUBLICATION_20261028.md；没有自动更新日常118。\n\n本轮13唯一编辑JUnit与历史模型／窗口证据分层，不累加重复；所有旧失败、用户否决、q09根因UNKNOWN及独立范围保留，不宣称全项目验收。3份本轮自有fixture2128项约1.28GiB留证退役，固定包／基线／人工资料保留。接续见handoff/STUDIO_NEXT_CONVERSATION_20261028.md：换新对话先确认性能专项范围；资产完整人工／编辑余项、115整体体验／任务栏、文件管理与真正删除、更多图像能力、Tag库仍排队；Prompt继续PENDING，不自行启动。\n\n'
for name in ['docs/00_INDEX.md','docs/CURRENT_STATE.md','docs/TASK_REGISTRY.md','tasks/UX_RECOVERY_CONTINUOUS.md','docs/rc/DAILY_USE_MANUAL_CHECKLIST.md','docs/rc/PREVIEW_UPDATE_CONTINUITY.md','docs/rc/OWNED_ARTIFACT_LIFECYCLE.md','docs/ux/UX_RECOVERY_IMPLEMENTATION_PLAN.md','tasks/REVIEW_FIXES_AND_UPDATE_CLEANUP_20261024.md']:
 p=r/name;p.write_text(head+p.read_text(encoding='utf-8-sig'),encoding='utf-8')
for name in ['tasks/PERFORMANCE_RESOURCE_PROGRAM.md','tasks/TAG_MANAGEMENT_PROGRAM.md']:
 p=r/name;p.write_text('## 2026-10-28 编辑阶段收尾后的排队状态\n\nPreview119已公开且本轮编辑改善限定签收，详见../handoff/STUDIO_NEXT_CONVERSATION_20261028.md。性能仍是下一项待确认具体范围的开发；Tag库保持规划。此处不启动测量／测试／代码或扩大真实资料权限，不重跑旧已接受基线。以下保留历史状态。\n\n'+p.read_text(encoding='utf-8-sig'),encoding='utf-8')
for name in ['docs/ux/BUNDLED_AUTOMASK_MODEL_20261028.md','docs/ux/EDITOR_IMAGE_WINDOW_20261028.md','docs/ux/EDITOR_PROJECT_BAR_IMPORT_20261028.md']:
 p=r/name;p.write_text('> 2026-10-28 发行补充：本轮相应实现已纳入Preview119（d9be9d9／SHA256 b425ae40…75f1c83），用户确认按原限定范围记录；公开118→119链通过。完整映射及所有未完成见docs/audits/PREVIEW_119_PUBLICATION_20261028.md，以下历史候选／未发布状态不改写。\n\n'+p.read_text(encoding='utf-8'),encoding='utf-8')
p=r/'docs/SOURCE_LOCK.json';v=load(p)
for item in v['sources']:
 if item['id']=='AUTOMASK_BUNDLED_MODEL':item['publicRelease']='v0.0.0-preview.119'
p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
p=r/'references/PUBLIC_SOURCE_LINKS.md';p.write_text('## 2026-10-28 公开发行补充\n\n固定模型已随实验性Preview119公开，沿原1697d5d／a12ac553来源与MIT模型卡声明；不授予unified-image-tool应用代码许可。默认模型不按工程复制，来源锁仅更新实际发布标记；upstream固定参考与观察不变。发行映射见../docs/audits/PREVIEW_119_PUBLICATION_20261028.md，以下本地／未发布是历史时点。\n\n'+p.read_text(encoding='utf-8-sig'),encoding='utf-8')
p=r/'handoff/DESKTOP_UPDATE_TECHNICAL_HANDOFF_20261028.md';p.write_text('> 2026-10-28 后续补充：最新公开Preview119，组装／tag d9be9d9、映像e012e2e、包SHA256 b425ae408e3613acac2a0000ead5f04cd02c9831a63d4b288682f5e2f75f1c83。新增编辑与内置模型；更新器6脚本、安装器生成器及Launcher.cs相对118的a1202e2无源码差异。119新增原版118公开下载／真实NSIS／生产Main／合成凭据／旧根自动退役证据，见[119发行回执](../docs/audits/PREVIEW_119_PUBLICATION_20261028.md)。因此下方118是机制固定实现基线，119不是另一套更新方案。Studio结果不代签Desktop验证，仍不授权修改Desktop、复制标识／端点／密钥或信任配置。\n\n'+p.read_text(encoding='utf-8'),encoding='utf-8')
manifest=[dict(path=p.relative_to(E).as_posix(),bytes=p.stat().st_size,sha256=sha(p)) for p in sorted(E.rglob('*')) if p.is_file()]
(E/'RAW_MANIFEST.json').write_text(json.dumps(dict(status='RETAINED',files=manifest,totalBytes=sum(x['bytes'] for x in manifest),privateData=False,realCredentials=False,privateSigningMaterial=False),indent=2))
print(json.dumps(dict(report='docs/audits/PREVIEW_119_PUBLICATION_20261028.md',handoff='handoff/STUDIO_NEXT_CONVERSATION_20261028.md',rawFiles=len(manifest),rawBytes=sum(x['bytes'] for x in manifest),retiredFiles=sumFiles,retiredBytes=sumFreed)))
