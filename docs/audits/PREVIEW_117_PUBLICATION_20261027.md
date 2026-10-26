# Preview117：公开发行与更新链回执

2026-10-27，Asia/Tokyo。用户明确确认发布固定117并核验公开更新链，**不自动更新日常116**。本轮不重建安装器，也没有重跑候选已接受的搬迁测试。

## 固定对应关系

| 对象 | 实际固定值 |
| --- | --- |
| 公开Pre-release | [v0.0.0-preview.117](https://github.com/MisakaPiano/CCB-Studio-Suite/releases/tag/v0.0.0-preview.117)，Release ID 424487169 |
| 发布时间 | 2026-10-27 01:23:40 JST（2026-10-26 16:23:40 UTC） |
| tag／组装源码 | **b9b240a248d428ec158c341b8c127969fd54cb64**，包含71bac6f及搬迁修正 |
| 实际应用映像构建 | c601d50a0b57de765353c6f0e600ddc2e9807738 |
| 候选及搬迁证据提交 | [3d4a97f7c6303bf7c81798abbabfdc7057312a36](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/3d4a97f7c6303bf7c81798abbabfdc7057312a36/docs/audits/RELOCATION_PREVIEW_117_20261027.md) |
| 安装包 | CCB-Studio-Suite-0.0.0-preview.117-windows-x64.exe，169,002,371字节，asset 678734160 |
| 安装包SHA-256 | **bec5f78735b6cd4c517c7c31c3d5bd499ed97a0d565e75718bc90c38fb0089c9**，与用户确认的候选完全一致 |
| 正式更新元数据 | [ad7ed68caa2fe99ac20bbc77aeec76281f0ead74](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/ad7ed68caa2fe99ac20bbc77aeec76281f0ead74)，parent 59412d5ab296563ccc556e7cfb1523bcf9b0cb3e |
| 元数据版本 | targets16／snapshot25／timestamp25；正式根仍为1 |
| 根SHA-256 | 408008d80aeb2dd3844325d74f09da9d248733e220dd88c08cc6299c4a24cc7a，未改变 |
| 默认分支README | [ec99e7cd9dfc8acf92f4284133ff6f2a03f91c3d](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/ec99e7cd9dfc8acf92f4284133ff6f2a03f91c3d)，只修改README.md的下载入口及限制说明 |

实际四个附件为安装器、RELEASE_NOTES.md、THIRD_PARTY_NOTICES.zip、SHA256SUMS.txt。逐项上传后核对GitHub的大小和digest，再公开；之后又实际流式下载公开安装器核对全包散列。原116附件未替换，旧Releases仓库、桥接通道及正式根未变。Windows Authenticode发布者签名仍未提供，不把TUF签名当作Windows发布者认证。

## 实际公开链验证

执行环境为本机Windows；复用用户准确批准的C盘Suite隔离测试根，并在留证后退役。日常安装、图库和私人凭据未访问。

1. 使用保留的**真实116包内客户端及Python运行时**，固定来源9839bce／根408008d8…24cc7a。6个旧更新模块先与116安装清单核对；没有用当前源码伪装旧客户端。
2. 旧客户端实际通过公开正式通道验证签名并发现117，返回DOWNLOADED_VERIFIED；公开下载169,002,371字节及SHA-256与批准包一致。
3. 使用旧116安装助手和该可信目标，执行真实NSIS隔离安装；实际返回`final,journal,record`，状态为**INSTALLED_NOT_STARTED**。550项／252,893,352字节验证通过，安装清单与固定117候选相同，合成资料哨兵字节不变。
4. 执行这份刚安装的117包内客户端，实际返回**UP_TO_DATE／117／相同安装包散列**。
5. 最终重新读取Release、四附件、正式元数据、tag绑定及main README；根签名委托、targets/snapshot/timestamp的散列关系和目标117再次核对通过。

这是一条**公开客户端／下载／安装助手链**，旧父目录是明确标记的合成定位锚点，**没有启动公开116旧GUI或本次117 GUI，没有伪造A→B完成回执**。候选原生搬迁及合成凭据证据继续按原固定范围引用；本例不能当作用户日常GUI升级、所有Windows环境或Project独立验收。公开链新增一例，不与旧7项或候选3类搬迁重复累计。

## 入口、证据和清理

默认分支已通过连接的GitHub再次确认是main。首页顶部“Windows预览版下载”指向117，并明确在Assets下载windows-x64.exe、Source code不是安装器；保留原文分界线之后的正文。Releases区块只读观察仍开启，未为展示而取消Pre-release。旧Releases README迁移链接仍有效，未修改旧库。

发布页正文增加“发布后的公开更新链核验”补记，说明候选说明中的“尚未发布验证”属于当时记录；**原RELEASE_NOTES附件、安装器及签名目标内容未改写**。后续首页同步继续沿既有流程，不新增自动化。

可复核原始记录位于[证据清单](evidence/PREVIEW_117_PUBLICATION_20261027/RAW_MANIFEST.json)：

- `PREFLIGHT.json`、`CREATE_RELEASE_REQUEST.json`、`RELEASE_STATE.json`及四份UPLOAD_REQUEST：确认来源、请求与实际附件ID／散列。
- `METADATA_PUBLICATION.json`、`metadata-attempt-1/PREPARED.json`、`signed-public-metadata/*`：只签发既有targets/snapshot/timestamp角色，5个元数据文件变更，根不变、不force、不导出私钥。
- `OLD116_PUBLIC_COMMAND.json`、`OLD116_PUBLIC_DOWNLOAD.json`、`old116-public.log`：实际旧客户端命令和公开下载结果。
- `PUBLIC_ISOLATED_INSTALL.json`、`public-raw/install-*.json`、`PUBLIC117_CURRENT_CHECK.json`、`PUBLIC_CHANNEL_VERIFICATION.json`：真实安装状态及117当前版本检查。
- `README_PUBLICATION.json`、`RELEASE_BODY_UPDATE.json`、`FINAL_PUBLIC_READBACK.json`：默认分支、只改README的提交、公开页面及附件复核。
- `PUBLIC_FIXTURE_RETIREMENT_MANIFEST.json`及`PUBLIC_FIXTURE_RETIREMENT.json`：准确所属清单，573个新测试文件／约422MB已逐文件安全退役，C盘根已移除。固定117安装包及payload、旧固定基线、日常安装、111保护与备份保留。

`freeze-final.py`首次比较时误将Windows本地CRLF文本捕获与远端LF字节直接比较，诊断断言失败。保留原脚本和`FREEZE_DRIVER_FAILURE.json`；改用明确的文本行尾规范化后通过，**没有重下安装器或重做安装**，也未改变公开文件或信任配置。这不是更新失败，不删除原记录冒充零失败。

## 当前结论与待验

**PUBLISHED_PUBLIC_CHAIN_VERIFIED_DAILY_NOT_UPDATED。**公开117已发布并通过上述隔离公开链验证；本轮没有代用户更新日常安装。资产／编辑完整人工验收、115体验、Prompt对比、用户实际任务栏重新固定继续待验；任务栏自动追踪搬迁未验。独立复审NOT REQUESTED，q09旧失败及UNKNOWN根因、旧否决与其他未完成状态保留。

继续保留候选报告的长路径、外置资料、跨账户／跨机器和更多Windows环境限制。清理仍只处理明确所属文件；未知／自改、用户资料、插件、备份及受保护111不删除。

资源观察：结束时H盘可用约335MB；若日常升级尚需新下载，按169,002,371字节下载＋252,893,352字节安装＋64MiB安全余量，约需489MB新增空间。**本轮没有因此删除受保护基线或扩大清理范围**；实际日常升级前应先核对缓存和可用空间，不能据本次隔离成功承诺当前H盘足够。
