# Preview116 正式渠道候选：等待用户确认

2026-10-26 Asia/Tokyo。按用户本轮明确指令，纳入已完成的更新、关闭、所属清理和主题修复，只完成必要打包／隔离安装／发行前身份核验。**READY_FOR_USER_CONFIRMATION_NOT_PUBLISHED**。没有新公开Release/tag、正式签名元数据、源码提交／推送或日常安装改动。确认前不发布、不更新日常安装。

## 固定候选

|项目|固定值|
|---|---|
|版本／渠道|`0.0.0-preview.116` ／ `preview`，拟保留Pre-release|
|拟tag|`v0.0.0-preview.116`，尚未创建|
|公开最新观察|2026-10-26 13:19:36 UTC重新读取release与tag，最新115且Pre-release；116为未使用的下一递增版本。后续发布前必须再次核对，不能覆盖竞争产生的同tag／版本|
|组装源码|`9839bce20ccdf4460e0aa42befcb070c1a6a235f`|
|实际应用映像构建|`0f11fdd6b6d281987dd7c449d623a74b8b74b8f0`；应用代码对26ef388及当前源码一致|
|安装器|168,998,814字节；SHA-256 **`8fee16c386f1da7a247b0f8a739b067687ab6712f9ad0e749fff106e0648966e`**|
|本地唯一安装器|[candidate/studio-installer.exe](H:/CCB-Studio-Suite/.local/preview-candidate-116-20261026/candidate/studio-installer.exe)|
|拟发布附件名|`CCB-Studio-Suite-0.0.0-preview.116-windows-x64.exe`，发布时改附件名不改字节；本地构建器保留约定的`studio-installer.exe`，不额外堆积同一安装器副本|
|数据读取版本|3；目标最低读取器2、最高3，不变更数据版本协议|
|正式更新根|SHA-256 `408008d80aeb2dd3844325d74f09da9d248733e220dd88c08cc6299c4a24cc7a`，与只读115基线同字节|
|正式渠道端点|`MisakaPiano/CCB-Studio-Suite` ／ `long-term-metadata` ／ `preview/windows-x86_64`，`internalTest=false`|

[构建回执](evidence/PREVIEW_116_CANDIDATE_20261026/package/BUILD_RECEIPT.json)、[实际源码输入及身份](evidence/PREVIEW_116_CANDIDATE_20261026/SOURCE_INPUTS.json)、[40份原始证据清单](evidence/PREVIEW_116_CANDIDATE_20261026/RAW_MANIFEST.json)。清单SHA-256为`cdc6eec95226620effe29b30fb32acfa56443b969bd33bada03a45aa37bdc625`。本报告、原任务本地记录与新回归尚未提交，不能冒充已公开的固定文档提交；生产实现来源仍为上列既有固定源码。

## 纳入内容及证据复用

- 更新／关闭先关发送门、收完在途结果、保存并排水；发送前暂停／结束原子检查，排队账户冻结、恢复副本不接管默认入口，Prompt采集中断幂等恢复、手动批次累计与归档容量一致。
- 安装成功且新版程序／资料／凭据确认后，立即退役具有可信归属的程序和下载文件；占用／权限失败安全重试，未知、自改文件及资料／插件／备份／活动保护记录保留。不增加7天保留期，也不把历史111保护解除当作新版清理的一部分。
- 齿轮命中、深浅／系统主题、弹窗按钮与选中态可读性、深色黑强调色Tokens修复；同时包含已完成的局部性能工程增量。

复用[本轮更新收尾固定证据](UPDATE_INVITATION_CLOSURE_20261026.md)的3个保护组合及1次内部原生A→B（3合成资产、2合成DPAPI凭据、生产排水、真实安装／B确认、成功即清理、正常关闭）。主题与Tokens沿用各自固定源码／包／用户回复的限定接受，不要求重复人工。当前116主JAR与该内部演练及Tokens候选相同，6个更新脚本与源码及旧证据相同；[复用身份映射](evidence/PREVIEW_116_CANDIDATE_20261026/REUSED_EVIDENCE_MAPPING.json)只建立适用关系，不算116新增功能测试或新的Project接受。

## 本次实际发行前核验

|检查|实际结果及边界|
|---|---|
|版本／不可变来源|PASS：公开release/tag重新读取；116递增；固定组装及应用构建输入，生产源码无改动|
|真实NSIS打包|PASS：退出0，Preview profile；不执行Gradle或重编译已固定应用|
|隔离安装|PASS：真实执行该安装器到新OWNER根，退出0；550清单项逐文件匹配|
|生产代码／运行时|PASS：2242生产类逐字节一致，326运行时文件匹配；6更新脚本匹配；test-only原生探针及Tokens测试类未进入产品JAR|
|正式更新身份|PASS：`Installed.load`实际读取新安装配置；116／preview／reader3／正式根及固定仓库端点一致，`internalTest=false`|
|发行目标绑定|PASS：现有`target_from_build`绑定安装器真实长度／散列／源码／版本及拟GitHub附件；现有传输器接受该目标位置格式。**仅本地未签名草案**，没有读取签名私钥或生成正式TUF元数据|
|发行材料|PASS：说明、散列清单、目标草案和资产映射齐全；100项第三方声明ZIP与实际公开115附件digest一致，依赖锁／验证元数据／运行时依赖清单无增量|
|公开116发现／下载／升级|**PENDING**：须用户确认发布后以实际签名渠道核验；不能用本地配置检查代签此项|
|116人工／独立复审|**NOT TESTED ／ NOT REQUESTED**，不新增通过声明|

[安装验证](evidence/PREVIEW_116_CANDIDATE_20261026/control/INSTALL_VERIFICATION.json)、[实际命令](evidence/PREVIEW_116_CANDIDATE_20261026/control/PACKAGE_COMMAND.json)、[安装命令](evidence/PREVIEW_116_CANDIDATE_20261026/control/INSTALL_COMMAND.json)、[目标草案](evidence/PREVIEW_116_CANDIDATE_20261026/control/UNSIGNED_TARGET_PROPOSAL.json)。本轮新增Gradle／JUnit／Python功能回归／原生UI启动均为0，不累加既有接受结果。

运行144.362秒、143资源样本；物理可用最低657,690,624字节，H盘最低521,777,152字节。[资源／执行回执](evidence/PREVIEW_116_CANDIDATE_20261026/control/RUN_RESULT.json)。打包前核实无Java／jpackage／NSIS重负载owner。采用既有`--consume-app-image`，只将准确的Suite临时构建映像同盘移至候选payload，原220文件与Tokens候选逐字节一致；不重复拷贝整个映像，不删除旧固定候选／测试基线。[移动前清单](evidence/PREVIEW_116_CANDIDATE_20261026/control/APP_IMAGE_BEFORE.json)、[差异为空](evidence/PREVIEW_116_CANDIDATE_20261026/control/APP_IMAGE_REFERENCE_DIFF.json)。

发行材料辅助脚本首轮把发布专用`release_repository`误当作随客户端捆绑模块，发生`ModuleNotFoundError`；只补充固定源码工具目录的导入位置，在新输出前完成修正。安装器未重建、测试未重跑、无签名或发布操作。[首轮记录及原脚本](evidence/PREVIEW_116_CANDIDATE_20261026/control/MATERIALS_PREPARATION_FAILURE.json)。两条既有NSIS缺少FileDescription／LegalCopyright警告保留，不将其抹去或扩展本轮版本资源开发范围。

成功后已外部留证，通过既有OWNER封存机制清理本次隔离安装及编译临时根：**553文件、253,033,391字节**；保留候选安装器、候选payload、材料、日志和散列。[退役回执](evidence/PREVIEW_116_CANDIDATE_20261026/control/INSTALL_FIXTURE_RETIREMENT.json)。未启动候选UI，没有留下本次应用窗口，不涉及历史保护111、日常安装、用户图库或备份。

## 发行材料及已知待验

- [候选发行说明](H:/CCB-Studio-Suite/.local/preview-candidate-116-20261026/RELEASE_NOTES.md)、[拟发布散列清单](H:/CCB-Studio-Suite/.local/preview-candidate-116-20261026/SHA256SUMS.txt)、[本地文件与拟附件名映射](H:/CCB-Studio-Suite/.local/preview-candidate-116-20261026/PUBLICATION_MANIFEST.json)。散列清单使用拟发布的Windows附件名，本地实际文件路径由映射明确，不要求复制一份大安装器。
- **资产管理／编辑完整人工验收、115体验、Prompt对比分别保持待验／PENDING。**不以此候选的文件验证或既有局部人工代签整组功能。
- q09编辑历史像素断言FAILED／root UNKNOWN，跨机器恢复、真实服务和更多Windows环境无本轮新验证；新包独立审查NOT REQUESTED。旧失败、用户否决及未完成状态保留。
- 仍为实验邀请Preview，非稳定版或全项目RC。Windows Authenticode发布者签名尚无；TUF信任根一致不代表Windows发布者认证。
- 保护111、未知或自改历史文件继续保留；未实施真实活动记录迁移，不承诺清空所有旧目录。不扩大历史清理或绕过备份保护。

## 用户确认后的顺序（本轮未执行）

1. 重新核对公开版本/tag及更新元数据父版本，保持此固定116安装器；若发生版本竞争，停止而非覆盖或偷偷换包。
2. 沿现有流程将同散列附件、说明及校验／声明材料发布为Pre-release，回读公开附件字节与身份；不合并应用到main，不取消Pre-release。
3. 使用现有受托管签名流程绑定该确切目标，普通前进提交正式更新元数据，保留根与旧桥接通道；随后验证旧客户端的实际签名发现／下载和隔离安装，更新默认分支下载入口。
4. 以上发行后核验通过才按用户授权更新日常安装，沿原保全／排水／启动确认流程留证；保护活动资料和备份，不解除111保护。

当前仅向用户交付这一固定候选供确认；没有自动执行上述发布或日常更新。
