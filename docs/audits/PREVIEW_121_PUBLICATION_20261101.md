# Preview121 发布及公开更新链 — 2026-11-01起始轮

用户在远程Windows交接准备中明确要求“最新版没发布先发布”；本轮先发布，远程包随后准备。不将发布授权扩大为日用再次替换或其他项目功能。完整访问权限实际生效；原工作区feature/windows-rc-continuous、HEAD7e15bc23ce54e9f2cb20b480f0f27ac9984f867d、index空及全部未提交修改保留，未reset/clean/stash。CURRENT_STATE/TASK_REGISTRY等共同协调文档未改。

## 固定身份

- [Preview121](https://github.com/MisakaPiano/CCB-Studio-Suite/releases/tag/v0.0.0-preview.121)，公开邀请prerelease，非稳定版。固定应用源码/tag e6cbe7ef6313c1389d48501d948c2d99763f1e56 / v0.0.0-preview.121，基于已公开120的8fccd113，23个Suite modules/tools差异；独立分支codex/preview121-single-original、工作树.local/r121。文本换行归一化逐项记录，不冒原工作区逐字节相同。
- 安装器CCB-Studio-Suite-0.0.0-preview.121-windows-x64.exe，263165596B，SHA256 4722287ddc74549df340be98f6779eedf8dbcee650d9df55f017dd92571988f6。GitHub发行430657445，四附件的实际size/digest与本地一致；说明、第三方声明与SHA256SUMS已附。发布后没有替换安装器附件。
- 新目录NSIS真实安装exit0；552文件347401589B，清单SHA256 fbe40d04d177ba675a64068f8f27cbf6869166f93f3ac4e75ff6ac52f23d5987。包内桌面1154/core431/image-authority854/assets26类与本次实际编译输出全部逐字节一致，不用旧增量候选代替新包身份。
- 固定证据[evidence/PREVIEW_121_PUBLICATION_20261101/MANIFEST.json](evidence/PREVIEW_121_PUBLICATION_20261101/MANIFEST.json)不含私人图库、Prompt、环境变量、签名密钥、原始vault或凭据快照。

## 范围与有限正确性

新图库导入不再额外保存imports图片；库内按内容散列保留原件，目录/相册使用资产ID引用。外部源文件仍保留，不把源文件与库内原件两份说成整个硬盘只剩一个文件。新事务完整提交并复核后退役自身重复暂存；历史副本仅经显式、逐项证明和引用迁移后清理，不启动扫删。未来格式、冲突、取消恢复与原请求身份规则保持。

文件夹一次导入的单个确认框选择递归、保留目录相册；不自动开启持续收集。自动收集默认页简化、按文件夹暂停/继续/检查/停止，停止保留已入库资料。内部维护包新增明确离线模式，不联网、不伪装UP_TO_DATE，前端显示离线维护说明；公开121配置仍为preview/正式HTTPS来源、无offline标记，原信任根/验证规则不变。

此前9个存储唯一方法、4个新导入/收集唯一方法、旧人工通过及历史失败维持原受限结论，没有重测整套。此次新增更新入口后端3方法既有PASS，逻辑未变；独立发布源码的1个新增UI方法实际PASS/sourceStable，组件图已查看。构建包含编译/JUnit/app-image共1207.629秒，是构建时间，不是性能结果。实际新包另外仅做12图/共享相册引用装配核对，0账户/Job/extra imports；不是新造性能基线。

## 公开120→121同一轮核验

全新.local/u121合成根。原公开120安装器/原包内客户端通过原正式TUF根发现、下载并校验公开121，未使用发现适配/跳过签名；原120 helper执行真实NSIS，121生产Main回执COMPLETE / B_PROGRAM_DATA_AND_CREDENTIAL_SNAPSHOT_MATCH。两张微型合成原图与两条合成凭据引用保持，资料/runtime路径不变、备份保留，0真实Provider。Sky仅对精确隔离窗口做正常关闭，未强杀。

原观察脚本在更新已COMPLETE后等待旧根完全消失，90秒超时：实际合成更新状态窗口仍显示“更新入口已提交”并占用旧启动器，另见驱动生成的空NVIDIA Corporation/umdlogs目录。源程序按策略延后，而非更新失败。正常关闭该合成完成提示后，用已安装121自带清理器继续同一受控清理，COMPLETE/removed1/deferred0，旧120目录在B仍运行时移除，下载/安装暂存均0。没有为根消失手工删文件，也没有重装/重下/重跑整轮求绿；只有关闭后剩余检查用resume驱动继续。Sky索引点击geometry不可用后重新观察，改用正常Alt+F4，属于工具输入问题，不冒产品按钮失效。

121随后正常关闭，离线实际重读两原图/两合成凭据通过；121包内客户端公开检查UP_TO_DATE。原默认入口重开及重复清理幂等旧通过未重测，不标作本次执行。全部准备失败、清理观察超时和同轮继续证据保留。

签名元数据提交7141141ba125337a38a496ffffd8498a441dcacf；targets20、snapshot35、timestamp35。仅已有targets/snapshot/timestamp角色内存签名，root字节SHA256 408008d80aeb2dd3844325d74f09da9d248733e220dd88c08cc6299c4a24cc7a不变，不创建/导出新密钥、不改legacy、不force。公开发行和更新链核验与整页性能/用户验收分开。

## 失败、限制与下一步

独立构建首次缺原已采用的本地Maven副本，0JUnit执行/sourceStable，补229文件189083802B原副本后strict检查继续，不改锁或校验。打包先发现新工作树因继承准备临时GIT_INDEX_FILE而索引为空，源码文件不变；仅给该精确自有工作树read-tree HEAD，原根索引不动，无重编译。随后缺固定NSIS工具/归档，完整部分payload保留candidate-attempt1-retained，补既有锁定工具后完成。公开驱动首次旧版本日期路径错误发生于实际更新前，修正复用原120安装；上述清理超时发生于更新COMPLETE后，未改生产包。原R2方法Path返回导致0发现、runner参数失败、其他全部历史FAILED/UNKNOWN/用户否决不改写。

**用户真实图库仍报告严重切页和预览卡顿。整体性能UNCONFIRMED/HOLD；没有千图、长期CPU/RSS、独立Project或全产品验收。** 用户另有Windows电脑，下一具体动作交付固定121源码/程序/合成输入/执行和回传材料，不上传私人资料，不在本机重建性能基线。Prompt对比、创作复用/拖放/右键、查看器排序等仍pending。日用当前维护安装本轮未替换；Windows Authenticode发布者签名仍未提供。
