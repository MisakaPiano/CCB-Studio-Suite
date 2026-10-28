# 内置默认识图模型：本地实现与打包核验边界

2026-10-28 Asia/Tokyo。沿现有UX-EDITOR-GAP-001／编辑改善任务，用户改定顺序：最新更新安装方案先共享Desktop，之后模型内置，最后再回UI。Desktop交接仅文档提交`e74f7a2769595259a32d546e165c1f00107a895e`已公开并发给其Codex；用户已明确授权两份未验收本地快照；编辑快照9e3b182已本地冻结，模型快照随本文件提交。尚未推送源码或发布。编辑窗口最新反馈为“仍有很多问题”，不标人工通过。

## 实现

- 固定权重`nsfw-anime-medium-x1280.onnx`，47,600,269字节，SHA256 `a12ac5532e93be9dfeb96a77fc3f3647791335c9df0de9a18fcd503f7877a828`。与既有C隔离目录模型及作者Hugging Face固定LFS对象一致，不采用另一份未知权重。
- 作者仓库`01miku/anime-nsfw-segm-yolo26@1697d5d1827b6a818b350b44bf3ec27f08837a2a`；模型卡标注MIT，无单独LICENSE文件。资源包含原卡、来源／散列清单和许可文本，不虚构作者版权行，也不把模型声明视为unified-image-tool应用代码的许可。
- 放在`modules/studio-desktop/src/main/resources/models/automask/`。`BundledAutoMaskModel.kt`从自身classpath读包内资源，核对长度／SHA，再由既有独立ONNX适配器解析，缺失或不一致明确报错，不改用未知文件。只在AI功能需要时加载，不在启动时下载或扫描用户路径。
- 无配置时直接使用内置模型，默认排除面部类别，跟随当前打码工具；无需首次找模型文件或保存配置才能使用。设置页可切回内置，也保留本地自选模型。已有自选配置优先，坏自选资源不静默替换。
- `TypesetAutoMaskResources.BUNDLED_ID`为固定资源身份；工作区只保存身份／散列／参数，不按工作区或工程复制47.6MB权重。自选模型仍用既有托管资产、按SHA去重和备份策略。用户先前保存的模型不会自动被删除。
- 保留现有设置格式，不新增全局资料版本或更新协议。内置身份由程序解析，自选身份由资料库解析；预设交换仍不携带本地模型／配置，不自动激活外来模型。

## 固定输入与验证

基点`e74f7a2769595259a32d546e165c1f00107a895e`仅包含已共享文档，不冒充模型或累计编辑源码已提交。当前77个累计变更源输入在`.local/windows-preview/evidence/20261027T170800326125Z-bundled-automask-fixed-inputs-20261028/FIXED_SOURCE_MAP.json`；这是整个未提交编辑增量，非本轮新增77文件。`.gitattributes`另加ONNX二进制规则，来源记录位于`docs/SOURCE_LOCK.json`及`references/PUBLIC_SOURCE_LINKS.md`。

Windows本机、JDK17、真实Windows ONNX Runtime CPU、离线严格依赖、单worker低优先级。现有`Run-EditorChecks-C.py`执行：核心`TypesetAutoMaskSettingsTest`，桌面`BundledAutoMaskModelTest`、`TypesetAutoMaskOneClickTest`、`TypesetInteractionRevisionTest.saveAiSettings*`，以及`:studio-desktop:jar`。**最终6唯一用例PASS，0失败／跳过，sourceStable=true，102.815秒；不与前次重复累计。**

覆盖：实际模型身份和类别、损坏／截断拒绝；真实模型在合成图上首次无文件选择器识别；内置配置保存、重开／备份恢复而无权重资产副本；既有自选模型、跨页一键、取消／坏资源／过时快照、可撤销／不重复和配置弹窗。此处真实权重处理合成图片只验证管线，不是模型准确率验收；没有读取私人图片、凭据或调用真实服务。

首次`20261027T170443639136Z-bundled-automask-default-20261028`虽6功能结果PASS，但执行者在构建运行中删除测试里一个未使用变量，sourceStable=false，整体**FAILED，不计最终通过**。`SOURCE_MUTATION.json`及可按散列还原的前后测试源、日志／XML／失败fixture保留；没有用构建成功掩盖输入变动。

最后又启动独立无界面的Java探针，用**生产JAR类路径**（非main编译输出目录）实际加载内置模型，返回`resourceProtocol=jar`、正确47,600,269字节／SHA及7类别；不使用C目录模型或文件选择器。生产JAR内确认模型、MODEL_NOTICE、MIT和model.json完整，测试探针类不在生产JAR中。

- 桌面JAR：49,383,927字节，SHA256 `ba9299fc8af5cd73294c145d8a21765888ea580225eccda801fccc90c7953383`。
- 原始命令、依赖散列、探针JAR及日志：`.local/model-bundle-20261028/JAR_PACKAGE_VERIFICATION.json`、`jar-probe.args`、stdout／stderr。
- 成功自有合成fixture435项／905,323字节已外部留证后按owner退役；失败、固定基线、人工审阅资料保持。源模型本身是打包输入，不是临时缓存，不在这次清理中删除。

## 2026-10-28 本地固定安装包与隔离安装核验

用户明确允许的两份未验收快照已本地提交：编辑`9e3b1827bea979f198a5bcf896ece6b7a012e851`，模型`03f589c313b1aef324b39e4eb697bbbe747fb305`。未推送代码。后者既是本次应用映像源码，也是NSIS组装输入；这不是新Preview发行，沿既有内部包身份`0.2.2-internal`。

本地安装器264,078,553字节，SHA-256 `6fbd4bb019bb6ed282591c055c8d906052a9b792da7154d836a5498e20296427`。正常NSIS隔离安装返回0，551项安装清单逐文件大小／SHA核对通过；从安装后生产JAR类路径执行独立测试探针，返回包内47,600,269字节、正确模型SHA、7类别、`resourceProtocol=jar`，测试类不在生产JAR里。测试未使用外部模型路径，也未读取私人图片／凭据。

`.local/model-bundle-20261028/local-package/`保留`CANDIDATE.json`、`BUILD_OWNER.json`、`INSTALL_OWNER.json`、`ISOLATED_INSTALL_VERIFICATION.json`、`installed-probe.args`与原始stdout／stderr；`delivery/`保留固定安装器、构建回执和安装清单。映像证据为`.local/windows-preview/evidence/20261027T172657725153Z-bundled-model-app-image-cached-wix-20261028/`，离线严格依赖构建成功，sourceStable=true；这项是构建核验，不计为新增JUnit用例。

首次映像生成因隔离Gradle缓存缺`wix311.zip`在离线检查失败，第二次诊断明确同因；两份失败日志均保留。沿既有本机缓存复制同一WiX归档到本轮C隔离Gradle缓存，源／目标SHA同为`2c1888d5d1dba377fc7fa14444cf556963747ff9a0a289a3599cf09da03b9e2e`，未联网下载／跳过构建保护／改变发布流程。之后正常构建成功。

**安装器／新隔离安装后模型读取已通过；升级后的模型读取、这个固定包的原生GUI试用仍未验证。** 未发布、未更新日常118，用户现有审阅窗口未被替换。最新窗口控制修复是在此固定包之后的独立工作区增量，不假称包含在03f589c包内。

## 历史待核验状态（已由上述本地包证据更新）

**程序JAR已含模型且默认读取已实测；新NSIS安装器／隔离安装／升级后的模型读取尚未核验。** 当前用户看到的窗口仍是上次候选，不含这次新构建；日常118没有改动，不能以此文档声称模型已交付到已安装118。

现有`tools/Build-InternalInstaller.py`在生成固定安装器前要求包输入已提交（报错`Commit package inputs before building a fixed installer`）。当前仍有用户未验收的编辑源码，不能绕过该保护或把文档HEAD冒充构建源码，也不能只提交模型却忽略它依赖的未提交编辑输入。用户已明确允许现有编辑工作与模型变更分别保留为未验收本地快照，仅本地提交，不推送、不发布、不更新日常安装。现按此授权继续正常安装器和隔离核验；不绕过现有打包保护。

UI继续等待用户问题清单；模型安装包级核验优先于下一轮UI修改。原资产完整人工、115体验、Prompt对比、旧q09／UNKNOWN／独立范围和用户否决保持。模型精度、更多图片／机器性能、像素分割和橡皮擦不在本轮新验收。

最新用户报告预览最大化仅铺满应用、二次点击与图标状态异常、关闭可能退出整个应用；下一轮UI先处理独立窗口生命周期、真实最大化／还原、统一浏览器和跟随主题标题栏。工程栏先合并重复入口、再按实际关系分组和区分主次，不照搬文件／编辑／视图名称或把全部按钮机械塞入下拉。此反馈不反向改写旧自动PASS，也不当作全部工程管理已验收。
