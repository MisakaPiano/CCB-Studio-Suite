# Preview118：公开发行、日常升级与所属清理回执

2026-10-27，Asia/Tokyo。按用户确认发布固定118，先验公开117→118链，再由用户操作日常程序完成升级。**PUBLISHED_PUBLIC_CHAIN_VERIFIED_DAILY118_CONFIRMED**。本次没有重建安装器或扩展功能测试；不是稳定版、全项目验收或Project独立复审通过。

## 固定对应关系

| 对象 | 固定值 |
| --- | --- |
| Pre-release | [v0.0.0-preview.118](https://github.com/MisakaPiano/CCB-Studio-Suite/releases/tag/v0.0.0-preview.118)，Release ID 424811525 |
| 发布时间 | 2026-10-27 06:54:02 JST／2026-10-26 21:54:02 UTC |
| tag／最终组装与更新脚本 | [a1202e24f7a0cba8a53f43bf035b67e2134ca2aa](https://github.com/MisakaPiano/CCB-Studio-Suite/tree/a1202e24f7a0cba8a53f43bf035b67e2134ca2aa) |
| 应用映像构建输入 | 3d0bd1f02c630d1e312d7469dbd71f29efe0bec1；与候选相同，不重编译 |
| 候选固定证据 | [ee46f20bccf34fa5268cb6b39e0d180793d9ebb4](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/ee46f20bccf34fa5268cb6b39e0d180793d9ebb4/docs/audits/PREVIEW_118_CANDIDATE_20261027.md) |
| 安装包 | CCB-Studio-Suite-0.0.0-preview.118-windows-x64.exe，169006742 bytes，asset 679593008 |
| 安装包SHA-256 | **3540be44ca7a1fcfd583899b325abad7630e10b120d288e9f2e7dc7a1e8bd60d** |
| 正式元数据提交 | [9ddec977eab81725fa36c99e2628413118c90958](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/9ddec977eab81725fa36c99e2628413118c90958)，parent ad7ed68caa2fe99ac20bbc77aeec76281f0ead74 |
| 元数据版本 | targets17／snapshot26／timestamp26，root1不变 |
| 正式根SHA-256 | 408008d80aeb2dd3844325d74f09da9d248733e220dd88c08cc6299c4a24cc7a |
| main首页提交 | [3db4dda450e0184e0298cd7eb01f9dc781e9cedc](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/3db4dda450e0184e0298cd7eb01f9dc781e9cedc)，只有README.md |

四附件为安装器、RELEASE_NOTES.md、THIRD_PARTY_NOTICES.zip、SHA256SUMS.txt，实际ID／大小／散列见[RELEASE_STATE](evidence/PREVIEW_118_PUBLICATION_20261027/RELEASE_STATE.json)。全包公开流式下载校验后才签发现有targets/snapshot/timestamp；无根签名、创建／轮换密钥、信任配置变更、force推送或旧附件替换。Windows Authenticode发布者签名仍未提供。

为复用既有发布工具，从同一固定安装器在获准C盘根真实解包，550项与原release-inputs相符。该临时构建视图不代表另做安装包。第三方声明由固定118包的现有通知重新汇总，并保留既有额外Skia／Skiko通知；未复用旧115组件表。安装器原字节未变。

## 公开117→118：本轮新增一条原生升级链

环境：本机Windows x64、同一Windows账户、C盘NTFS，独立USERPROFILE／APPDATA／TEMP。仅合成原图、合成账户及DPAPI合成凭据，无实际供应商调用。旧117安装器SHA为bec5f78735b6cd4c517c7c31c3d5bd499ed97a0d565e75718bc90c38fb0089c9。

1. 原117生产Main启动；在旧117真实类上创建两张合成原图、两个账户引用的合成凭据，正常关闭后取得快照。
2. **未修改的117包内客户端**从公开正式TUF通道发现118并实际下载，DOWNLOADED_VERIFIED，字节与批准包一致。没有候选阶段的create_updater／authorize_download适配。
3. 再启动旧117，等启动检查释放缓存锁后，以真实父进程绑定交接；原117 helper进入READY_TO_DRAIN，正常关闭旧窗口；真实NSIS安装118，生产Main完成资料及合成凭据ACK，回执COMPLETE。
4. 118仍运行时，旧117根已消失；自动清理最终删除553个所属程序项及2个下载项，下载和installing目录均0。备份哨兵、两张原图、两个合成凭据保留。
5. 对同一安装执行118自带清理，COMPLETE／removed0／deferred0，原交接回执字节不变。从默认入口重开118、资料检查和正常关闭分别通过；新118包内公开检查返回UP_TO_DATE／118。

实际命令见[OLD117_PUBLIC_COMMAND](evidence/PREVIEW_118_PUBLICATION_20261027/public-native/OLD117_PUBLIC_COMMAND.json)，原生驱动见同证据目录verify-public-native.py与resume-public-native.py，命令入口是117包内studio_update.py的`--package <旧包> --cache <隔离缓存> --download`、原117 apply_update.py的`--handoff <新合成交接>`，随后118包内residue_cleanup.py及studio_update.py。不新增JUnit或累计旧已接受用例。

这是公开客户端下载／原生helper／真实NSIS／生产Main链。旧GUI安装前交接由隔离驱动衔接，**没有自动点击旧GUI更新按钮**；日常GUI案例分列如下，不能互相冒充。

## 保留的失败与执行边界

- 准备时既有GitHub适配器拒绝仓库根API端点，改用连接器确认main；该阶段未发生公开写入。原脚本／PREPARATION_FAILURE保留。
- 初始通知清单对 renamed metadata-only Skiko stub的精确路径比较失败；随后按当前实际通知重汇总，没有替换或改造固定安装器。原驱动与NOTICES_GENERATION保留。
- 公开原生测试在旧根消失后立即断言下载目录为空，**检查早于同一异步清理轮的下载移除**，驱动失败。原B运行观察、原交接COMPLETE与清理事件保留；原B之后已不在，退出原因NOT_INDEPENDENTLY_DIAGNOSED，未追认为正常关闭。
- 后续从**同一已完成升级**的默认入口重开118，等待旧根及下载两项完成，再补做重复清理／资料检查／正常重开关闭／公开当前检查；没有再次升级重算通过。PUBLIC_NATIVE_EARLY_ASSERTION明确记录原失败。RESULT的publicRelease:false指隔离驱动自身不执行发布，不表示118未公开。
- 一次生成首页脚本时的本机默认文本编码错误发生在脚本生成前，无外部写入，改用显式UTF-8；不作为产品失败或新测试。

此前候选的两次旧根失败、准备失败、7项旧兼容／搬迁基线、主题人工和用户否决均按原固定报告保留，不重跑、不叠加成本轮成功次数。

## 日常GUI升级及用户新反馈

用户先遇到缓存占用，再遇到暂存空间不足。只读核对确认缓存锁之后已释放；117客户端下载检查要求`169006742×3+64MiB=574129090 bytes`，当时H盘约558MB，实际不满足门槛。没有删除client.lock、强杀进程或降低空间检查。

将仓库保留的原117安装器临时移存用户已批准的C测试根，复制后逐字节散列确认，再移除H盘那份临时存储位置以腾空间；保留位置记录。公开118固定包此前也临时移存在该根。**两包最终都已按原SHA归还原H盘位置**，固定基线没有丢失。

电脑控制画面持续超时，未盲点界面；用户通过正常日常更新页下载、安装及重启，并明确回复“已完成，显示118”。随后截图显示当前118及底部更新完成，又在收尾期间遇到缓存占用提示；清理退出后用户再检查，截图显示**已是最新版本**。升级成功与后续检查占用分开记录，未把占用误认作升级回滚。

[日常只读核验](evidence/PREVIEW_118_PUBLICATION_20261027/DAILY_UPDATE_VERIFICATION.json)：550程序项一致、原信任根及资料／runtime位置不变，回执COMPLETE／B_PROGRAM_DATA_AND_CREDENTIAL_SNAPSHOT_MATCH／dataModifiedByInstaller:false；118进程运行时旧117根已自动移除，下载0、installing0，111与原保护记录仍在。执行者**未读取真实图库、私人凭据或私有快照**；资料／凭据内容结论来自应用原成功确认，不是独立审计私人数据。

日常额外用户验收只覆盖本次GUI升级、118显示及更新检查恢复。资产／编辑完整人工、115体验、Prompt对比和任务栏人工固定仍待验。缓存忙／空间不足目前直接显示英文失败文案的体验问题仍存在，本轮只处理实际阻止、留证，不声称已改代码或另发版本。

## 111保护与清理边界

当前默认运行和后续更新不依赖111。遗留ACTIVE_INSTALLATION仍引用111并禁止清理，且同时保护实际资料／runtime／备份；本轮未解除或改写该记录，不能以清理旧程序为由关闭整份保护。

用户追问后只读比对：111 manifest与保留的111构建基线一致，549个程序项／252455605 bytes一致，没有发现保护子目录或reparse；另有Uninstall.exe／48921 bytes尚未补齐独立所属证明。**旧程序可规划退役，但整目录尚未获本次解除保护／删除授权**。详细清单留本地PROTECTED111_READ_ONLY.json；本报告只公开汇总，不读取备份内容，不追认实际记录迁移完成。

本次自身产物退役：[PUBLIC_FIXTURE_RETIREMENT](evidence/PREVIEW_118_PUBLICATION_20261027/PUBLIC_FIXTURE_RETIREMENT.json)，**1260文件／852190688 bytes**，C授权测试根已移除，1个Windows生成的内部junction核对目标仍在根内后非递归解除，未遍历链接；未知文件删除0。固定118／原117安装器已归还，历史证据、原失败、固定基线、日常118、资料、111保护及备份保留。

## 入口及复核索引

main README顶部下载指向实际118，明确Assets/windows-x64.exe与Source code区别，实验性邀请Preview与已知待验保持。只修改下载区，原正文分界线后字节保留；旧Releases迁移链接及桥接通道不改。Releases侧栏只读观察开启，Pre-release保持；沿原手工同步流程，无新自动化。

原始证据见[RAW_MANIFEST](evidence/PREVIEW_118_PUBLICATION_20261027/RAW_MANIFEST.json)：

- RELEASE_STATE、PUBLIC_INSTALLER_BYTE_VERIFICATION：附件ID及全包实际公开下载散列。
- METADATA_PUBLICATION、signed-public-metadata、FINAL_PUBLIC_READBACK：元数据固定提交、签名与散列关系、正式根及首页复核。
- public-native/RESULT、RAW_UPDATE_RESULT、cleanup-events.jsonl、repeat-cleanup.log：公开原生升级与清理逐项结果；失败记录另列。
- DAILY_UPDATE_VERIFICATION、USER_UPDATE_CONFIRMATION：日常程序与用户升级确认，非全面功能验收。
- FIXED_INSTALLERS_RESTORED、PUBLIC_FIXTURE_RETIREMENT_MANIFEST与退役回执：保留物及本轮测试产物清理边界。

脚本仅为审计复现材料，路径含已退役夹具，不能直接重放为日常操作授权。本次文档提交与上述源码／tag／元数据／首页提交分开。独立审查仍NOT REQUESTED，q09 FAILED/root UNKNOWN、原失败／用户否决保持；不启动性能、Tag、界面重做或Desktop开发。
