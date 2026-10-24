# 历史残留只读清单（尚未删除）

此清单与新版机制开发分开交付，**不构成删除授权**。只检查明确的活动安装及Suite仓库测试目录，不清理整个H盘、不清空回收站。归属不明、权限不足或记录未对上时保留；不把目录名当成充分归属证明。

## 已确认的旧合成程序副本

下表均通过本地BUILD_RECEIPT源码身份、安装manifest与现有程序逐文件核验，未发现清单外文件（允许的NSIS生成文件除外）；进程检查无对应程序运行。删除只影响这些合成演练程序，不影响日常App或日常回退；会失去对应演练的展开副本，所以仍需你确认准确路径。安装包、固定基线、原始日志不在这些子目录内。

| 准确路径 | 字节 | 来源版本/源码 |
|---|---:|---|
|`H:\CCB-Studio-Suite\.local\windows-rc\performance-106\smoke-106`|252177541|106 / `839205b8499f`|
|`H:\CCB-Studio-Suite\.local\windows-rc\batch-109\smoke-109`|252496316|109 / `2093ae5cb858`|
|`H:\CCB-Studio-Suite\.local\windows-rc\update-repair-110\smoke-110`|252498502|110 / `c9e46074ec19`|
|`H:\CCB-Studio-Suite\.local\windows-rc\batch-parity-111\smoke-111`|252504526|111 / `1c9385afc38c`|
|`H:\CCB-Studio-Suite\.local\windows-rc\batch-history-112\smoke-112`|252523469|112 / `acf71fc1acb2`|

源码与manifest散列、来源记录路径见[确认记录](evidence/REVIEW_FIXES_CLEANUP_20261025/observations/CONFIRMED_WORKSPACE_PROGRAM_CANDIDATES.json)。未通过严格安装形态核验的构建payload/108 smoke仍标NOT_CONFIRMED，不推定损坏或垃圾，也不包含在上表。

## 旧共享测试内容

| 准确路径 | 字节 | 文件数 | 来源依据/影响 |
|---|---:|---:|---|
|`H:\CCB-Studio-Suite\.local\windows-preview\tests`|4108545212|97250|Gradle原固定合成测试根；元数据完整计数且跳过重解析点。新测试已用独立根；清除将移除旧用例资料/备份现场，须确认，证据日志另保留。|
|`H:\CCB-Studio-Suite\.local\windows-preview\desktop-tests`|1524402699|154693|Gradle原固定合成测试根；元数据完整计数且跳过重解析点。新测试已用独立根；清除将移除旧用例资料/备份现场，须确认，证据日志另保留。|

两根共5,632,947,911字节（约5.25GiB）。不是日常图库；不包括`.local/windows-preview/live-validation`或活动备份。旧失败/人工用例是否还需保留物理现场，交由Project复审确认，不将自动通过当作删除理由。

## 其他已列目录：待核验，不是整目录删除清单

下表只列元数据；完整[69项只读记录](evidence/REVIEW_FIXES_CLEANUP_20261025/observations/WORKSPACE_HISTORICAL_ENRICHED.json)保留此前部分统计及权限错误。含原始证据、备份、合成凭据或旧安装包的父目录不能整体删除。

| 准确路径 | 已统计字节 | 统计状态 |
|---|---:|---|
|`H:\CCB-Studio-Suite\.local\windows-preview\selfcheck-1`|690915|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-preview\selfcheck-modern-1`|690905|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-preview\png-fields-before-surface`|25131|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-preview\preview-layout-before-fix`|315131|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-preview\long-pdf-userunit-failure`|285798|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\ab-applications-1`|0|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\ab-applications-2`|1585|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\ab-applications-3`|1627|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\ab-applications-4`|0|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\ab-applications-5`|1585|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\ab-applications-6`|1610|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\ab-build-a-1`|127230|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\ab-build-a-2`|127232|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\ab-build-a-3`|127234|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\ab-build-a-4`|127608|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\ab-build-a-5`|127612|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\ab-build-a-6`|127614|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\ab-build-b-1`|127230|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\ab-build-b-2`|127230|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\ab-build-b-3`|127230|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\ab-build-b-4`|127610|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\ab-build-b-5`|127614|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\ab-build-b-6`|127615|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\ab-data-1`|189715|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\ab-data-2`|270676|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\ab-data-3`|86820|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\ab-data-3-format3-49cf5fbbe6ff`|50804|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\ab-data-3-probe-copy`|37955|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\ab-data-3-restore-after`|30547|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\ab-data-3-restore-before`|30547|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\ab-data-3-restore-migrated-after`|35215|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\ab-data-4`|130712|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\ab-data-5`|147983|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\ab-data-6-restored`|68852|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\ab-repository-1`|15582|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\ab-repository-2`|13820|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\ab-repository-3`|6837|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\ab-repository-4`|4382|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\ab-repository-5`|5342|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\ab-repository-6`|4633|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\ab-repository-7`|4056|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\ab-repository-8`|1716|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\ab-runtime-1`|5538|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\ab-runtime-2`|166548190|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\ab-runtime-3`|167216665|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\ab-runtime-5`|334630037|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\ab-runtime-6`|167326773|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\entrypoint-115-273d3fee046b4e6c992be8a15d3e9474`|129819|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\entrypoint-115-3c917256734a46b191f78cf9a9e8185e`|127592|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\entrypoint-115-47569f2c4eb74daaa1c2c277d9db01a1`|129793|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\entrypoint-115-61113906ea7a472a99c2c2873bddb9ab`|0|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\entrypoint-115-63442bf9108a4fe3be645b16acd81213`|129281|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\entrypoint-115-c1a8407c26154d67a5bf6b7cdce1c74c`|0|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\entrypoint-115-c85434b47cd142a28a03cb76885f42c0`|120315|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\handoff-fixtures`|78741|不完整/权限不足，保留|
|`H:\CCB-Studio-Suite\.local\windows-rc\install-ops-fixtures`|171760|不完整/权限不足，保留|
|`H:\CCB-Studio-Suite\.local\windows-rc\residue-cleanup-fixtures`|189798|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\runtime-client-fixtures`|20418625|不完整/权限不足，保留|
|`H:\CCB-Studio-Suite\.local\windows-rc\update-fixtures`|59035|不完整/权限不足，保留|
|`H:\CCB-Studio-Suite\.local\windows-rc\batch-109`|1178460675|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\batch-history-112`|842526333|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\batch-parity-111`|1056378708|不完整/权限不足，保留|
|`H:\CCB-Studio-Suite\.local\windows-rc\gallery-organization-113`|337722966|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\update-repair-110`|842729832|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\performance-106`|672845874|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\performance-108`|673966677|元数据计数完成；归属/保留用途仍需核对|
|`H:\CCB-Studio-Suite\.local\windows-rc\review-update-native`|2237822176|元数据计数完成；归属/保留用途仍需核对|

## 日常安装历史（只读）

日常固定入口实际115；活动保护记录仍引用111，不能自动绕过。下列程序均未删除；旧二进制删除会移除该版本地回退/演练能力，即使它不含用户资料，也须确认。

| 准确路径 | 字节 | 结论 |
|---|---:|---|
|`H:\CCB-Studio-Suite-App\105-a416a820955a`|252165408|历史候选；见原记录manifest/journal依据，不自动删除|
|`H:\CCB-Studio-Suite-App\107-5907779fad12`|252182991|历史候选；见原记录manifest/journal依据，不自动删除|
|`H:\CCB-Studio-Suite-App\109-2093ae5cb858`|252496316|历史候选；见原记录manifest/journal依据，不自动删除|
|`H:\CCB-Studio-Suite-App\111-1c9385afc38c`|252504526|保留：活动保护记录引用|
|`H:\CCB-Studio-Suite-App\112-acf71fc1acb2`|252523469|历史候选；见原记录manifest/journal依据，不自动删除|
|`H:\CCB-Studio-Suite-App\114-f59b0c25a2eb`|252625080|历史候选；见原记录manifest/journal依据，不自动删除|
|`H:\CCB-Studio-Suite-App\115-ff145d051bae`|252895803|保留：当前程序|

更新下载8目录共1,348,323,111字节，准确路径、大小、来源版本及记录见[安装只读记录](evidence/REVIEW_FIXES_CLEANUP_20261025/observations/HISTORICAL_RESIDUE_READ_ONLY.json)。不删除交接/失败回执。

## 明确保留

- `.local/ACTIVE_STUDIO_INSTALLATION.json`及日常根的保护记录；`model-accounts-review/workspace`和`runtime`备份。
- `performance-107/smoke-107`、`114-f59b0c25a2eb`、`115-ff145d051bae`基线；114/115安装器及全部原始复审包。
- 源码、依赖构建缓存、签名托管目录、私人资料、图库、配置、凭据和人工备份。
- 尚待复审的失败现场；两条挂起测试进程仅在用户精确授权后结束，失败与线程栈仍保留。

本轮只自动清理新建且明确登记为一次性、已留证的成功测试内容；旧目录一律未删除。下一步是你确认具体清单，再执行逐项清理，不能把上表所有父目录一键清空。
