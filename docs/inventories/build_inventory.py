"""Read-only fixed Git object inventory. Does not fetch, checkout, or read user data."""
import hashlib, json, re, subprocess
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
OUT=Path(__file__).resolve().parent
REFERENCE=Path('H:/ChatChatBar-Desktop')
SOURCES={
 'official':('SaltyFishOTL/ChatChatBar','979cb505958dffb1b6d64a8de657434f198ffa96'),
 'desktop':('MisakaPiano/ChatChatBar-Desktop','5ef95428421f7c693b010ac88f51d470804ab07a'),
 'desktop-development':('MisakaPiano/ChatChatBar-Desktop','c4098e08bec84372911a5a8bc38c2cfa72f98978')}

def git(*args):return subprocess.check_output(['git','-C',str(REFERENCE),*args])
def source_files(commit):
    entries=[]
    for entry in git('ls-tree','-rz','--full-tree',commit).split(b'\0'):
        if not entry:continue
        metadata,path=entry.split(b'\t',1);path=path.decode()
        if path.endswith('.kt') and '/src/' in path and not re.search(r'/src/(?:test|androidTest|desktopTest|commonTest)',path):
            entries.append((metadata.split()[2].decode(),path))
    proc=subprocess.run(['git','-C',str(REFERENCE),'cat-file','--batch'],input=('\n'.join(x[0] for x in entries)+'\n').encode(),stdout=subprocess.PIPE,check=True)
    data=proc.stdout;pos=0;result=[]
    for blob,path in entries:
        end=data.index(b'\n',pos);header=data[pos:end].split();length=int(header[2]);start=end+1;raw=data[start:start+length];pos=start+length+1
        text=raw.decode('utf-8',errors='replace')
        funcs=[{'line':i,'name':m.group(1)} for i,line in enumerate(text.splitlines(),1) if (m:=re.search(r'\bfun\s+(?:[\w<>?.]+\.)?(\w+)\s*\(',line))]
        labels=[{'line':i,'label':m.group(1)} for i,line in enumerate(text.splitlines(),1) for m in re.finditer(r'(?:Text|CbText|SectionTitle|StudioAction|label|title|contentDescription)\s*(?:\(|=)\s*"([^"\n]{1,180})"',line)]
        image=bool(re.search(r'image|novelai|picture|photo|apng|avatar|cover|screenshot|crop|图片|图像|生图|相册|头像|封面|截图|打码|反推',path+'\n'+text,re.I))
        result.append(dict(path=path,blob=blob,sha256=hashlib.sha256(raw).hexdigest(),imageRelated=image,functions=funcs,uiLabels=labels))
    return result

def write(name,value):(OUT/name).write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
suite_sources=[p for p in (ROOT/'modules').rglob('*.kt') if '/src/main/' in p.as_posix() and '/build/' not in p.as_posix()]
suite_by_name={p.name:p.relative_to(ROOT).as_posix() for p in suite_sources}
actions=json.loads((ROOT/'docs/ux/ACTION_IMPLEMENTATION.json').read_text(encoding='utf-8'))['actions']

def mapping(path):
    name=Path(path).name
    if name=='DesktopImageTools.kt':
        return 'PARTIAL_REQUIRES_ACTION_REVIEW',[suite_by_name[n] for n in ['DesktopImageTools.kt','GalleryAnimation.kt','DisguiseCovers.kt']],['docs/audits/UX_EDITOR_CLARITY.md','docs/audits/UX_APNG_COVERS.md']
    if name in ['SharedDanbooruCatalogUpdateChecker.kt','NovelAiRankedIndexBuilder.kt','DesktopNovelAiCatalogUpdate.kt','DesktopNovelAiCatalog.kt'] and name in suite_by_name:
        return 'IMPLEMENTED_REQUIRES_HUMAN_REVIEW',[suite_by_name[name]],['docs/audits/UX_NAI_SETTINGS.md','docs/audits/UX_NAI_BACKGROUND.md']
    if name in suite_by_name:return 'SOURCE_PRESENT_UNVERIFIED',[suite_by_name[name]],[]
    groups=[
      ('ImageViewer|ViewerPresentation', ['StudioAssetViewer.kt'],['UX_NAI_BACKGROUND']),
      ('NovelAiStudio|StudioControls|StudioCharacters|StudioPrompt|StudioHistory|DesignConversation|NovelAiGuidance|NovelAiImage', ['StudioApplication.kt','StudioVision.kt'],['UX_R1A_WORKBENCH','UX_ROLE_WORKFLOW','UX_CREATION_CONTROLS']),
      ('ImageTools|Apng|Disguise', ['GalleryAnimation.kt','StudioApplication.kt'],['UX_EDITOR_CLARITY']),
      ('ImagePostProcess', ['StudioApplication.kt'],['UX_R1B_POSTPROCESS']),
      ('ImageMetadata|Stealth', ['StudioImagePrompt.kt'],['UX_R1B_METADATA']),
      ('ModelSettings|ModelDiscovery|ModelEdit|ModelManage', ['StudioModelAccounts.kt','StudioApplication.kt'],['UX_MODEL_ACCOUNTS','UX_VISION_CREDENTIALS']),
      ('ImageUnderstanding', ['StudioVision.kt'],['UX_VISION_CREDENTIALS'])]
    for pattern,names,evidence in groups:
        if re.search(pattern,name,re.I):
            targets=[suite_by_name[n] for n in names if n in suite_by_name]
            return 'PARTIAL_REQUIRES_ACTION_REVIEW',targets,[f'docs/audits/{e}.md' for e in evidence if (ROOT/f'docs/audits/{e}.md').exists()]
    return 'NOT_MAPPED',[],[]

def ledger(kind,files,repo,commit):
    rows=[]
    for f in files:
        status,targets,evidence=mapping(f['path'])
        rows.append(dict(id=kind.upper()+'-'+hashlib.sha256(f['path'].encode()).hexdigest()[:12],**f,implementationStatus=status,suiteTargets=targets,evidence=evidence,humanAcceptance='NOT_SIGNED_OFF_FOR_WHOLE_SOURCE_UNIT'))
    return dict(kind=kind,repository=repo,commit=commit,date='2026-10-23',granularity='source unit + every detected Kotlin function and literal UI label; not a semantic proof of exhaustive user workflows',entries=rows)

def table(name,title,items,description):
    lines=['# '+title,'',description,'','状态：`PARTIAL_REQUIRES_ACTION_REVIEW`=已定位适配入口，逐动作仍需核实；`SOURCE_PRESENT_UNVERIFIED`=存在同名来源代码，不代表用户能用；`NOT_MAPPED`=未找到已核实Suite对应项。所有整文件人工状态均未签收，具体通过范围以专项证据为准。','','|ID|来源单元 / 功能入口|Suite状态|Suite对应 / 证据|','|---|---|---|---|']
    groups=[
      ('模型/API/默认项/视觉关联','Model|Embedding|ConnectionTest|ApiKey'),
      ('NAI生成/尺寸/采样/额度/连续任务','NovelAi|Generation|Anlas|StudioControls'),
      ('AI设计/修改/反推/候选应用','Design|Reverse|Understanding'),
      ('Prompt编辑/角色位置/角色卡图像词','Prompt|StudioCharacters|CharacterCover|Appearance'),
      ('基图/图生图/蒙版/Vibe/精准参考','Guidance|Inpaint|Vibe|ReferenceImage'),
      ('增强/放大/重新生成','PostProcess|Regeneration|Upscale|Enhance'),
      ('图库/历史/相册/结果查看','History|Gallery|ImageViewer|Preview|Album'),
      ('元数据/隐写/参数回用/隐私清理','Metadata|Stealth|ImageTools|ImageFormat|Privacy'),
      ('APNG伪装/还原/动画','Apng|Disguise|Animation|Gif'),
      ('图片编辑/裁剪/绘制/旋转','Editing|Crop|Canvas|Drawing'),
      ('聊天图片/附件/截图/背景','ChatImage|Screenshot|Background|Attachment'),
      ('聊天/会话/流式消息/会话设置','Chat|Session|Message'),
      ('角色管理/角色编辑/角色资源','Character|Card'),
      ('世界书/格式卡/提示词检查','WorldBook|Format|Inspector'),
      ('语音/Fish/音频播放','Fish|Audio|Voice|Speech|Playback'),
      ('备份/恢复/存储/数据根切换','Backup|Restore|Storage|Data|Snapshot|Persistence'),
      ('窗口/平台/文件选择/剪贴板/更新','Window|Chrome|Dialog|Clipboard|Update|Bootstrap|Launcher'),
      ('其他来源支撑与尚未分类入口','.*')]
    buckets={label:[] for label,_ in groups}
    for r in items:
        for label,pattern in groups:
            if re.search(pattern,Path(r['path']).name,re.I):buckets[label].append(r);break
    summary=['## 可读能力分组','','分组用于导航，不把整组自动标为完成；详细动作、行号与控件标签在对应JSON。每个来源单元恰好落入一个组，交叉能力通过来源详情查看。','','|能力组|来源单元数|未映射单元数|','|---|---|---|']
    for label,bucket in buckets.items():
        if bucket:summary.append(f'|{label}|{len(bucket)}|{sum(r["implementationStatus"]=="NOT_MAPPED" for r in bucket)}|')
    lines[4:4]=summary+['','## 逐单元实现与证据','','']
    for r in items:
        repo,commit=SOURCES[r['sourceKind']]
        src=f'https://github.com/{repo}/blob/{commit}/{r["path"]}'
        funcs=', '.join(x['name'] for x in r['functions'][:6]) or '声明/支撑逻辑'
        targets='<br>'.join(r['suiteTargets']+r['evidence']) or '待逐项核对；不据名称猜测已实现'
        lines.append(f'|{r["id"]}|[{Path(r["path"]).name}]({src})<br>{funcs}|{r["implementationStatus"]}|{targets}|')
    (OUT/name).write_text('\n'.join(lines)+'\n',encoding='utf-8')

loaded={}
for kind,(repo,commit) in SOURCES.items():
    files=source_files(commit)
    if kind.startswith('desktop'):files=[f for f in files if f['path'].startswith('app/desktopApp/') or f['path'].startswith('app/sharedCore/')]
    value=ledger(kind,files,repo,commit)
    for r in value['entries']:r['sourceKind']=kind
    loaded[kind]=value
    write(kind+'-source-ledger.json',value)

official=loaded['official']['entries']
table('CCB_IMAGE_CAPABILITIES.md','CCB本体图片能力来源与实现清单',[r for r in official if r['imageRelated']],
      '固定官方提交979cb505（1.4.6观察来源），不改变已有请求权威版本。采用路径和内容宽匹配：生成、识图、反推、图像工具、元数据、聊天图片/截图、头像封面背景及相关存储。为避免静默遗漏，官方全部生产Kotlin文件（包括不匹配项）也完整保存在official-source-ledger.json；不匹配项并非已证明无图片能力。JSON附函数、控件文字、行号和散列，供逐入口复核。')
desktop=loaded['desktop']['entries']
dev=loaded['desktop-development']['entries']
base={x['path']:x['sha256'] for x in desktop}
delta=[r for r in dev if base.get(r['path'])!=r['sha256']]
write('desktop-deleted-source-units.json',[r for r in desktop if r['path'] not in {v['path'] for v in dev}])
table('CCBDESKTOP_CAPABILITIES.md','CCBdesktop全产品来源与Suite实现清单',desktop+delta,
      '全量枚举固定集成提交5ef95428的desktopApp与sharedCore生产Kotlin源单元；并单独纳入固定开发快照c4098e08新增或改变的单元（不是自动采用开发分支）。覆盖聊天、角色/世界书、模型、图片工作室、数据/备份、平台窗口及该开发快照中的语音等能力。完整开发快照另有desktop-development-source-ledger.json；被删除单元可从两份路径差异追踪。列入清单不等于Suite必须移植完整聊天产品，也不代表Suite已有这些功能。')

# Explicit Suite enhancements; no fabricated completion from source filenames alone.
features=[
 ('资产原件字节保留及独立工程/导出','StudioWorkspace.kt','UX_DAILY_USE_READINESS','IMPLEMENTED_AUTO_HUMAN_PARTIAL'),
 ('统一持久图库、别名与标签','GalleryWorkspace.kt','UX_LIBRARY_PROMPTS','IMPLEMENTED_HUMAN_PENDING'),
 ('多收集目录、变更版本、暂停恢复与重新授权','LibraryDirectoryMonitor.kt','UX_COLLECTION_RESPONSIVENESS','IMPLEMENTED_RECOVERY_AND_SMALL_SYNC_HUMAN_SCOPED_PASS_BULK_PENDING'),
 ('独立Prompt资产与AI候选/图片/文字主动加入参照','DesignPromptReference.kt','UX_NAI_BACKGROUND','IMPLEMENTED_AUTO_DEDICATED_HUMAN_PENDING'),
 ('多图Recipe比较、角色展示位置对齐、字段摘取','PromptReferenceBoard.kt','UX_LIBRARY_PROMPTS','IMPLEMENTED_AUTO_DEDICATED_HUMAN_PENDING'),
 ('语言/视觉/NAI分类、生效状态、列表与默认选择','StudioModelAccounts.kt','UX_ACCOUNT_ROLES','IMPLEMENTED_CONFIGURATION_AND_ACTIVE_LAYOUT_HUMAN_SCOPED_PASS'),
 ('独立视觉模型与共用默认凭据','StudioModelAccounts.kt','UX_VISION_CREDENTIALS','IMPLEMENTED_CONFIGURATION_HUMAN_PASS_ACTUAL_VISION_PENDING'),
 ('模型用途提示与不兼容用途拦截','ModelPurpose.kt','UX_MODEL_PURPOSE','IMPLEMENTED_AUTO_HUMAN_SCOPED_PASS'),
 ('生成任务不确定状态与禁止盲目重发','CreationSession.kt','UX_DAILY_USE_READINESS','IMPLEMENTED_HUMAN_PENDING'),
 ('设计、应用草稿、最终生成的来源链','DesignProvenance.kt','UX_DAILY_USE_READINESS','IMPLEMENTED_HUMAN_PENDING'),
 ('原生多工程图片编辑与历史','TypesetWorkspace.kt','UX_EDITOR_CLARITY','IMPLEMENTED_HUMAN_PENDING'),
 ('连续嵌字样式与别名预设','TypesetWorkspace.kt','UX_EDITOR_CLARITY','IMPLEMENTED_HUMAN_PENDING'),
 ('批量交付及多文件剪贴板','ExportClipboard.kt','UX_NATIVE_CLIPBOARD_ELEVEN','IMPLEMENTED_SINGLE_PNG_HUMAN_PASS_BATCH_PENDING'),
 ('资产回收及工程引用保全','AssetArchive.kt','UX_DAILY_USE_READINESS','IMPLEMENTED_HUMAN_PENDING'),
 ('资料损坏拒绝默认覆盖及单工作区所有权','StudioWorkspace.kt','UX_DAILY_USE_READINESS','IMPLEMENTED_AUTO_HUMAN_PARTIAL'),
 ('完整备份、恢复新目录及凭据排除','StudioRestore.kt','UX_NATIVE_RESTORE','IMPLEMENTED_HUMAN_SCOPED_PASS'),
 ('资料格式升级建立副本','StudioFormatMigration.kt','UX_RESTORED_UPDATE','IMPLEMENTED_AUTO_HUMAN_PARTIAL'),
 ('Windows原生窗口、文件选择及拖放','StudioApplication.kt','UX_FIXED_ELEVEN','IMPLEMENTED_HUMAN_PARTIAL'),
 ('安全GitHub应用内下载、安装及重启','StudioUpdateHandoff.kt','UX_LONGTERM_UPGRADE','IMPLEMENTED_HUMAN_SCOPED_PASS'),
 ('项目Release迁移与旧渠道兼容','StudioUpdates.kt','UX_RELEASE_MIGRATION','IMPLEMENTED_HUMAN_SCOPED_PASS'),
 ('长期签名、加密云备份和独立恢复码','tools/update-runtime','UX_SIGNING_CUSTODY','IMPLEMENTED_RECOVERY_SCOPED_PASS'),
 ('新旧渠道自动续签及离线targets维护','tools/update-runtime','UX_RENEWAL_PREFLIGHT','IMPLEMENTED_MANUAL_RUN_PASS_NATURAL_SCHEDULE_PENDING'),
 ('Suite Android完整独立创作','NOT_IMPLEMENTED','', 'NOT_IMPLEMENTED'),
 ('Suite Server管理员上游凭据及多用户队列','NOT_IMPLEMENTED','', 'NOT_IMPLEMENTED'),
 ('跨Windows/Android/Server资产与任务协议','NOT_IMPLEMENTED','','NOT_IMPLEMENTED'),
 ('词库独立后台任务、可见进度与就地取消','NaiSettings.kt','UX_NAI_BACKGROUND','IMPLEMENTED_AUTO_NEW_BACKGROUND_UI_HUMAN_PENDING'),
 ('AI设计独立可调窗口和共享未发送输入','StudioApplication.kt','UX_NAI_BACKGROUND','IMPLEMENTED_AUTO_WINDOW_HUMAN_SCOPED_PASS'),
 ('单一生图模型、动态费用和单机本地停止','NaiSettings.kt','UX_NAI_BACKGROUND','IMPLEMENTED_AUTO_MODEL_AND_COST_LAYOUT_HUMAN_SCOPED_PASS'),
 ('图片查看器悬浮翻页、右键及直达编辑/APNG','StudioAssetViewer.kt','UX_NAI_BACKGROUND','IMPLEMENTED_AUTO_VIEWER_ENTRY_HUMAN_SCOPED_PASS_APNG_DELIVERY_PENDING'),
 ('可视多套封面库、独立管理窗口、默认恢复与自动预览','DisguiseCoverManager.kt','UX_COVER_LIBRARY_GALLERY','IMPLEMENTED_COVER_LIBRARY_HUMAN_SCOPED_PASS_PREVIEW_AND_DELIVERY_PENDING'),
 ('图库来源目录折叠、层级偏好及可见滚动条','GalleryHistory.kt','UX_COVER_LIBRARY_GALLERY','IMPLEMENTED_DIRECTORY_GROUPING_AND_SCROLLBARS_HUMAN_SCOPED_PASS')]
rows=[]
for i,(label,target,audit,status) in enumerate(features,1):
    if target.endswith('.kt'):
        resolved=[p.relative_to(ROOT).as_posix() for p in suite_sources if p.name==target]
        if not resolved:
            # Keep a gap visible instead of inventing a path or dropping the feature.
            resolved=[];status='TARGET_REQUIRES_RECONCILIATION / '+status
    else:resolved=[target] if (ROOT/target).exists() else []
    evidence=[f'docs/audits/{audit}.md'] if audit and (ROOT/f'docs/audits/{audit}.md').exists() else []
    rows.append(dict(id=f'SUITE-{i:03}',capability=label,implementationStatus=status,suiteTargets=resolved,evidence=evidence))
write('suite-added-capabilities.json',dict(date='2026-10-23',features=rows,relatedInheritedActions=actions,scopeNote='增强/平台适配/运维与继承项有交叉；TypeSet继承动作仍按75项来源合同单列，不冒称全为Suite原创。'))
lines=['# Suite新增与扩展能力实现清单','','按图像工作室基础之上的资产/工程/交付、平台适配、运维和跨端规划列出。与继承能力有交叉，不能把原有TypeSet排版或CCB生图算法标成原创。测试结果链接只证明其限定范围；尚未回填证据与未实现明确保留。','','|ID|能力|实现/验证状态|对应文件与证据|','|---|---|---|---|']
for r in rows:lines.append('|'+ '|'.join([r['id'],r['capability'],r['implementationStatus'],'<br>'.join(r['suiteTargets']+r['evidence']) or '未实现/待核对'])+'|')
(OUT/'SUITE_ADDED_CAPABILITIES.md').write_text('\n'.join(lines)+'\n',encoding='utf-8')
write('COVERAGE.json',dict(sources={k:dict(commit=v['commit'],sourceFiles=len(v['entries']),functions=sum(len(r['functions']) for r in v['entries']),labels=sum(len(r['uiLabels']) for r in v['entries'])) for k,v in loaded.items()},officialImageCandidates=sum(r['imageRelated'] for r in official),desktopDevelopmentChangedOrAdded=len(delta),suiteEnhancements=len(rows),suiteActionIds=len(actions),notMapped={k:sum(r['implementationStatus']=='NOT_MAPPED' for r in v['entries']) for k,v in loaded.items()},limitations=['文件与函数枚举完整不等于用户工作流语义全量验证','名称/内容图片候选宽匹配可能误报；未匹配源文件仍完整保留','固定版本之外的新上游能力未盘点；当前Desktop外部活动分支不作为权威','未知实现/人工状态保留，不能把源码同名视作移植完成']))
print((OUT/'COVERAGE.json').read_text(encoding='utf-8'))
