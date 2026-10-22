# 首个长期使用预览版：GitHub 更新渠道方案

2026-10-22，方案阶段。本次实际起点为 `4bf16df55f27bccd699b51ed9d6cb42a3fa284a3`，生产源码检查点为 `3bea16b7405d1c1890d840e76ff3dee759faad5c`。本文件不表示已经实现或发布。PRODUCT_HOLD / NOT RC 保持。

## 分发选择

用户先选择“自己使用，之后公开”、无现成服务器、GitHub 私有渠道；随后提出首版不宣传、说明能力边界即可。据此推荐 **公开 preview、先由本人使用、不主动推广**，取消首版下载登录需求。公开可下载与真正私有是两种选择；不宣传不能保证无人发现。这里推荐公开分发，不把讨论视作创建公开仓库、改变现有仓库权限或发布安装包的授权。

首版无需承诺零缺陷。发布说明列明已验证范围、已知问题、外部服务依赖、资料备份与反馈方法；不承诺未测试的兼容性或维护时限。说明能力范围不能替代资料保全与更新校验，本方案也不判断免责条款的法律效力。

采用独立发行仓库（拟名 `MisakaPiano/CCB-Studio-Suite-Releases`，尚未创建），不改变现有源码仓库可见性。只上传安装包、可信更新元数据、版本说明、许可证/应附声明及校验信息。公开前核对打包内容与第三方分发要求，不上传测试工作区、凭据、用户图片或内部日志。

若仍要求真正私有，保留同一架构但增加私有资产下载授权；最小只读权限、系统保护存储、禁止写入 URL/日志/备份。该分支工作暂不列为推荐首版的必要条件。

## 官方安卓版可参考的实际做法

只读核对官方 `SaltyFishOTL/ChatChatBar` 的 v1.4.6，固定提交 `9841e2f216ec6de5f19ecbcdd53e9a0c0ee3ad3d`。未构建、运行或修改官方项目；这不是变更 Suite 的来源依赖锁。

- [release.yml](https://github.com/SaltyFishOTL/ChatChatBar/blob/9841e2f216ec6de5f19ecbcdd53e9a0c0ee3ad3d/.github/workflows/release.yml)：手动触发，指定版本和说明，使用 Secrets 中的 Android 签名材料构建 APK，更新版本并创建 GitHub Release。该版本 JVM 测试为可选输入，不能照搬为 Suite 的免测依据。
- [AppUpdateChecker.kt](https://github.com/SaltyFishOTL/ChatChatBar/blob/9841e2f216ec6de5f19ecbcdd53e9a0c0ee3ad3d/app/app/src/main/java/com/example/chatbar/domain/update/AppUpdateChecker.kt)：查询 Releases，有 Atom/其他发现回退，过滤草稿和预发布，解析 APK 资产。Suite 的 preview 需要明确接收预发布，不能直接复制其筛选。
- [AppUpdateManager.kt](https://github.com/SaltyFishOTL/ChatChatBar/blob/9841e2f216ec6de5f19ecbcdd53e9a0c0ee3ad3d/app/app/src/main/java/com/example/chatbar/domain/update/AppUpdateManager.kt)：应用缓存下载、进度/取消，检查有效 APK、包名、版本名和递增 versionCode，再调用系统安装入口；安装权限不满足时打开相应设置。

可复用的是“GitHub 托管、版本发布、应用内下载、用户确认安装”的产品流程。Windows 没有这段 Android 安装接线；沿用 Suite 已有独立安装器和保存/重启/资料确认链，不退回只提供网页链接。

## 最小持久渠道设计

| 项目 | 具体安排（待实现） |
|---|---|
| 渠道 | `preview`；首个版本建议 `0.3.0-preview.1`，内部递增编号统一分配并校验，不依赖字符串排序 |
| 安装包 | 独立发行仓库的版本 Release；每版本唯一 tag 和资产，不覆盖已发布字节 |
| 版本发现 | 签名的 TUF 元数据决定可安装版本；GitHub latest/列表仅用于传输或展示，不作为信任依据 |
| 元数据 | 发行仓库独立 `update-metadata` 分支，路径 `preview/windows-x86_64/metadata/`；保留版本化元数据与根轮换文件 |
| 传输 | 公共 HTTPS；无需购买域名/服务器。下载失败可重试，已安装应用仍可离线打开本地资料 |
| 安装 | 延续保存、排水、独立安装、新程序启动与资料核验后才报告完成；不强杀写入者 |
| 未来公开推广 | 已为公开 preview 时只需推广，不必更换根密钥或搬工作区；stable 以后显式选择，不自动切换 |

现有客户端固定逻辑目标为 `studio-installer.exe`，且检查下载与安装交接各自创建 TUF Updater；GitHub 的版本资产 URL 不能仅换一个 base URL 就直接接上。实现时增加共用 GitHub 传输/目标定位模块：

1. 元数据从固定仓库、固定渠道路径读取，保留 TUF 自身的版本/过期/根轮换验证。
2. 已签名目标的 custom 中包含发行 tag、资产名或 ID；在签名验证后才用于定位安装包。固定允许的仓库与 HTTPS 主机，拒绝任意外部 URL；重定向限界并核对最终文件长度和 SHA-256。
3. `studio_update.py` 下载与 `apply_update.py` 安装前重检共用实现和相同配置。确认后元数据变化不得静默安装不同包。
4. preview 不依赖 GitHub `latest` 是否包含预发布；无网络、限流、资产缺失显示可理解错误，不触发资料变更。

## 密钥和维护

复用现有 TUF 库，不另写签名算法。当前 internal 发布器每次生成内存临时密钥且一天过期，只用于测试，不能直接用于本渠道。

首版建议离线保管根与发行目标签名材料，分别备份在加密介质；不写入源码或普通工作区备份。根轮换与备份恢复先用测试钥匙演练。时间戳/快照使用独立维护密钥，根密钥不放 GitHub Actions。Windows 程序签名证书与 TUF 更新签名是不同事项，不能把后者称为已消除系统信誉提示；本轮不购买证书或创建正式密钥。

拟定维护参数：根 1 年、targets 90 天、snapshot 14 天、timestamp 7 天；每日续签 timestamp，必要时同步续签 snapshot，targets 每月检查并续签，根到期前 180 天开始提醒。参数属于方案，需故障测试后定稿；尚未创建定时任务或存入 Secrets。持续无新版本时也需续签，而不是假设上次安装包一直存在就能永久检查更新。

元数据过期时拒绝安装更新但不禁止使用已有本地作品；补签恢复后应能再检查。丢失或泄露密钥按验证过的轮换/人工重新引导流程处理，不靠删缓存绕过验证。不要向用户承诺自动化任务永远不会失败。

## 每次发布顺序

1. 固定源码 SHA、递增版本、渠道、资料格式、包清单和已知问题；运行对应回归。
2. 构建唯一安装包并记录散列，验证新目录安装、保存重开及升级路径。
3. 准备 Release 草稿，上传并回读核对完整资产；确认可分发内容和版本说明。
4. 经发布授权公开版本资产，再签署并发布指向该资产的元数据。先有可取的包，后有更新指针；不能反向发布。
5. 元数据串行发布，先版本化文件、最后 timestamp；保留旧版本化文件，使不同缓存时刻可重试而不混用。跨多个 HTTP 请求不存在天然事务，不声称一次 Git 提交即可免除并发/缓存测试。
6. 从安装版经真实 HTTPS 完成一次检查、下载、安装、重启及资料核验；失败保留旧程序和资料。错误版本优先发布更高版本修复，不让旧程序写入新格式资料。

首版采用人工触发发布即可。暂不增加分批灰度、后台统计、账号系统或自建服务器。

## 具体实现与验证范围

| 工作 | 现状与下一动作 |
|---|---|
| 通用 preview 构建配置 | `tools/Build-InternalInstaller.py` 目前硬编码 internal；增加正式渠道配置校验，不改内部测试语义 |
| GitHub 传输 | 更新 `tools/update-runtime/studio_update.py` 与 `apply_update.py` 的共用接线；验证仓库限制、预发布、重定向、限流、网络中断与安装前重检 |
| 持久发布器 | 新增保存版本状态、串行锁、dry-run、签名输入与只发布完整资产的工具；不将正式私钥写入报告 |
| 维护流程 | 测试密钥演练续签、长时间离线后恢复、逐版本根轮换、过期拒绝、丢失备份恢复；再准备维护任务定义 |
| 首次从 internal 进入 preview | 受控安装新的 preview 引导包并选择/验证工作区；不可直接替换 internal 信任文件。先用合成资料演练，不擅自迁移个人资料 |
| 用户确认 | 最新固定包集中日常流程确认、真实渠道 A→B 一次升级。用户已表示可随时人工测试，画面优先人工截图；准备明确步骤后再请求 |

本轮仅方案与来源核对，无构建、程序运行、人工测试、付费调用、正式密钥创建或外部发布。既有测试成绩不重标为新渠道通过。首版门槛仍按 [已有准备条件](FIRST_DAILY_USE_READINESS.md) 和 [更新合同](../05_UPDATE_AND_RELEASE.md)，普通体验缺口可列已知问题；资料损坏、错误覆盖、凭据泄露等不能用预览标签掩盖。

下一具体动作：实现可用合成签名元数据测试的公共 GitHub 传输适配与通用 preview 构建配置；真实发布之前再形成可审阅的包、版本说明、仓库/权限及密钥保管清单。

## 来源与核对范围

- [GitHub Releases](https://docs.github.com/en/repositories/releasing-projects-on-github/about-releases)：有仓库读取权限者可访问 Release；单资产小于 2 GiB，现有约 160 MiB 包可容纳。
- [Release 资产 API](https://docs.github.com/en/rest/releases/assets#get-a-release-asset)：二进制响应可为直接内容或重定向，私有授权与公共匿名读取分开处理。
- [TUF 规范](https://theupdateframework.github.io/specification/v1.0.36/)：签名、版本/到期检查、根轮换和一致快照；以上周期是项目建议，不是规范强制值。
- Android 三文件散列分别为：Checker `82aa934d9dad45e66d65a6ddd5a74a84c04e136d27bc31d4f3dd497b47836eb5`；Manager `609e99abeeb5fd2a59bf311d8432036fc034fba9066ce4acbc26fe82cae455eb`；workflow `bb9c64507ec9c756a959f87db88f4f49baf1126468364e64f8771ffb1b2e6fa8`。只审阅，不作为 Android 实机验收。
