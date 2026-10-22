## 日常版条件核对 checkpoint（2026-10-22）

[剩余条件](rc/FIRST_DAILY_USE_READINESS.md)、[返回后的人工清单](rc/DAILY_USE_MANUAL_CHECKLIST.md)、[自动补验](audits/UX_DAILY_USE_READINESS.md)已保存。最新45项更新/恢复回归通过；103安装547文件和332模块源码核对、105声明匹配、两张合成图片及接收目录准备完成。用户离开电脑且要求保留checkpoint：不开始人工测试/真实API，不重复已通过102→103。下一步用户回来做图库/编辑/导出组，实际提供者最后另确认范围。PRODUCT_HOLD / NOT RC。
## 长期签名102→103完整升级通过（2026-10-22）

[实际记录](audits/UX_LONGTERM_UPGRADE.md)：经用户明确授权发布102/103测试包，长期根真实GitHub应用内下载、安装重启、资料核验及再次检查最新通过；A/B各547文件、10条合成资产与状态快照一致，原备份未变。用户确认并提交103最新截图。44项回归通过，发布目标后的续签工作流成功并回读验证。[Desktop参考报告](rc/WINDOWS_UPDATE_REFERENCE.md)已补充长期渠道来源。无需追加人工操作；首个日常版产品验收、自然定时触发和跨机恢复未完成，PRODUCT_HOLD / NOT RC。
## 长期签名、加密备份与续签部署（2026-10-22）

[操作说明](rc/LONG_TERM_SIGNING_OPERATIONS.md)与[验证记录](audits/UX_SIGNING_CUSTODY.md)：正式四角色密钥已创建，Drive 两份加密备份已下载校验、恢复码路径实际恢复通过，用户确认独立恢复码已保存。43 项测试通过；GitHub 续签工作流实际运行成功，远端签名链回读通过。正式渠道为空，尚无新根安装包；自然定时触发和跨机器恢复未测。PRODUCT_HOLD / NOT RC。以下旧记录按时间保留。
## 真实 GitHub 应用内升级已验证（2026-10-22）

[Desktop 参考报告](rc/WINDOWS_UPDATE_REFERENCE.md)：经用户授权发布测试包后，真实公开 GitHub 100→101 下载、安装、重启、再次检查最新版本完成。A/B 各547文件、10条合成资产与整体状态快照核验通过；原备份未变，原本缺失的凭据仍未配置。人工截图及 COMPLETE 交接证据完整保留；首次夹具误用备份目录导致 FOREIGN_ROOT 的失败也已记录。长期密钥/续签尚未部署，本批是一次性测试信任；PRODUCT_HOLD / NOT RC。

## GitHub 渠道实现与本地安装验证（2026-10-22）

[实施记录](audits/UX_GITHUB_CHANNEL.md)：39 项实际通过，公共 GitHub 传输、共用安装前复核、preview 打包和持久元数据准备已实现；实际 A/B 安装器通过合成传输下载/安装，旧程序与合成资料未变。真实 GitHub 发布/重启尚未验证，正式密钥未建立；不提供完整成功的 Desktop 报告。用户已授权并完成公开发行仓库仅 README 创建；已回读核验，无 Release/安装包，源码仓库权限未变。PRODUCT_HOLD / NOT RC。

## 长期更新渠道方案（2026-10-22）

[具体方案](rc/LONG_TERM_UPDATE_CHANNEL_PLAN.md)：只读参考官方 Android v1.4.6 GitHub 发布/下载流程，建议独立发行仓库的公开 preview、不主动推广；保留真正私有备选。列明现有客户端传输适配、持久签名/续签、发布顺序和验收。仅方案，未创建密钥或外部发布；用户已重新表示可配合人工测试，优先人工截图。PRODUCT_HOLD / NOT RC。

## 固定 0.2.12 内部包与长期使用条件（2026-10-22）

[实际记录](audits/UX_FIXED_TWELVE.md)：3bea16b 最新界面完整桌面 115 项实际 PASS；新固定内部包构建、546 文件安装核验及路径/覆盖拒绝通过，失败与中断尝试保留。本轮没有启动应用或开展人工测试。[长期使用版剩余条件](rc/FIRST_DAILY_USE_READINESS.md)明确集中体验验收及持久更新渠道仍未完成；PRODUCT_HOLD / NOT RC。

## 图库/编辑清晰度与设置沿用（2026-10-22）

[实际记录](audits/UX_EDITOR_CLARITY.md)：按用户反馈收起次要操作与长说明，复现并修复连续嵌字样式回退，提供快捷命名预设；自动回归与源 APNG 接线核验记录保留。[单张 PNG 文件粘贴](audits/UX_NATIVE_CLIPBOARD_ELEVEN.md)经用户及字节核对通过，不能代替多文件/APNG。用户要求本轮改好告知即可，暂不开展新人工测试；PRODUCT_HOLD / NOT RC。

## 角色展开样式与实机输入记录（2026-10-22）

[实际记录](audits/UX_ROLE_HEADER.md)：按用户反馈将角色折叠移至标题左侧，并澄清位置重置含义；两项相关回归实际 PASS。用户确认安装版 11 中文输入/切页保留和位置 X/Y/重置可用；剪贴板接收端尚未测，继续新固定包，PRODUCT_HOLD / NOT RC。

## 固定 0.2.11 安装验证（2026-10-22）

[实际记录](audits/UX_FIXED_ELEVEN.md)：a50f428 固定包实际安装、546 文件核验及路径/已有目录拒绝通过，独立合成窗口已启动并收到用户截图。各模块最近执行与当前源码逐一匹配；更新过期追踪描述，原生输入/剪贴板与集中验收仍待验证，PRODUCT_HOLD / NOT RC。

## 角色位置与账号额度修正（2026-10-22）

[实际记录](audits/UX_CREATION_CONTROLS.md)：复现并修正角色位置控件重叠、保存选用 NAI 账号后沿用旧余额；两次实际失败保留，定向 2 项及完整桌面 114 项实际 PASS。实际安装代码拒绝未来资料格式，原生错误弹窗仍未测；继续新固定包及原生门槛，PRODUCT_HOLD / NOT RC。

## 恢复工作区实际更新与角色流程（2026-10-22）

[固定 9→10 实际更新](audits/UX_RESTORED_UPDATE_ACTUAL.md)：用户确认重启，B 完成记录与 10 条资产/先前缺失凭据状态一致，546 个安装文件核验通过。[角色流程](audits/UX_ROLE_WORKFLOW.md)：实际工作台启停/重排至请求及重开定向通过，完整桌面 112 项实际 PASS。固定安装包未替换，继续其余门槛，PRODUCT_HOLD / NOT RC。

## 恢复工作区更新修复（2026-10-22）

[实际记录](audits/UX_RESTORED_UPDATE.md)：原工作区 7→8 实际更新及再次检查最新版本通过；恢复窗口缺失凭据导致的安装前失败已复现并定向修复。可读凭据丢失仍拒绝，继续完整回归和新固定包验证，PRODUCT_HOLD / NOT RC。

## 安装版备份恢复第二窗口（2026-10-22）

[实际记录](audits/UX_NATIVE_RESTORE.md)：用户完成 0.2.7 备份/恢复新目录/打开第二窗口；61 个文件三方散列、文档链及凭据排除只读验证通过。原窗口保留，继续固定 7→8 更新，PRODUCT_HOLD / NOT RC。

## 原生选择框安装版观察（2026-10-22）

[实际交付](audits/UX_NATIVE_INSTALLED.md)：固定源码 08733d5 的 0.2.7 安装/文件验证通过，用户截图确认真实 Windows 原生导入框；Drive 分卷与证据已上传并回读。取消返回已由用户确认；恢复第二窗口及 0.2.8 更新继续，PRODUCT_HOLD / NOT RC。

## Windows 原生选择与分步恢复（2026-10-22）

[实际记录](audits/UX_NATIVE_PICKERS.md)：固定 Desktop 原生选择器适配全部入口；恢复来源/位置分步及错误来源提前提示。定向 16 项、desktop 完整 108 项实际 PASS；首次编译失败和旧安装版 MISSING 保留。原生手选与固定包验证继续，PRODUCT_HOLD / NOT RC。

## 字体回退与导出反馈（2026-10-22）

[实际记录](audits/UX_GLYPH_EXPORT_FEEDBACK.md)：修复小人书测量/绘制字体不一致、导出进度重绘阻塞及冷字体载入；停止/明确重试/复制失败文件保留已验证。core 129 项与最终 desktop 95 项分别实际 PASS，三轮失败保留。继续固定包与原生验证，PRODUCT_HOLD / NOT RC。

## Recipe 往返与小人书批次验证（2026-10-22）

[实际记录](audits/UX_FLOW_COMPLETION.md)：修复合法大 Recipe 导出后无法导入、勾选队列页切走当前样式来源；批次撤销/重开及四模块完整 351 项实际 PASS。18 项动作映射补齐，原生验证与产品验收仍单列，PRODUCT_HOLD / NOT RC。

## 启动与真实安装故障验证（2026-10-22）

[实际记录](audits/UX_STARTUP_SAFETY.md)：未知格式无新增文件、迁移五边界进程中断、实际排水超时与 B 启动失败保全；长路径故障已保留并补写入前门禁。218 项核心/桌面回归及最新 92 项桌面回归实际 PASS。继续新安装包与剩余门槛，PRODUCT_HOLD / NOT RC。

## 连续生成及更新进程验证（2026-10-22）

[实际记录](audits/UX_CONTINUOUS_PROCESS.md)：连续批次/停止/部分结果/重开不重发定向 PASS，桌面完整 91 项 PASS；真实 CLI/HTTP 更新故障 8 项 PASS，历史失败保留。继续安装/迁移故障与固定包验证，PRODUCT_HOLD / NOT RC。

## 新工作台备份恢复进度（2026-10-22）

[实际记录](audits/UX_LIBRARY_BACKUP.md)：接入已有安全核心，恢复到新目录、缺凭据/目录重连提示及直接打开入口；3 项定向、90 项桌面完整实际 PASS，失败保留。安装版第二窗口与集中真人验收未签；继续完整工程门槛，PRODUCT_HOLD / NOT RC。

## 实际更新与界面反馈修正（2026-10-22）

[实际记录](audits/UX_UPDATE_FEEDBACK.md)：用户操作真实 A→B，格式 2→3 独立复制与 10 条资产/1 个合成凭据核验 COMPLETE；同版本误报失败、下载进度/完成反馈及 Desktop 顶栏图形已修正。Python 23 项、定向 7 项、桌面完整 87 项实际 PASS，历史失败保留。继续完整门槛，PRODUCT_HOLD / NOT RC。

## 格式 3 复制迁移进度（2026-10-22）

[实际记录](audits/UX_FORMAT_THREE_MIGRATION.md)：独立副本迁移、启动确认/签名目标绑定及真实旧 A 读取拒绝已验证；完整 208 项实际PASS，旧备份锁初始化失败及修正保留。A安装包已封存，继续B打包和真实2→3应用内更新，PRODUCT_HOLD / NOT RC。

## 更新数据格式交接进度（2026-10-22）

[实际记录](audits/UX_UPDATE_DATA_BRIDGE.md)：签名目标输出格式、独立迁移目录与确认绑定已接线；6项定向、桌面完整81项及Python20项实际PASS，权限/编码失败保留。当前仍为数据格式2，继续固定A包与格式3复制迁移/真实更新，PRODUCT_HOLD / NOT RC。

## 大资产恢复修复进度（2026-10-22）

[实际记录](audits/UX_LARGE_ASSET_RECOVERY.md)：33 MiB合成归档实际复现恢复失败并修复；流式校验保留原件/损坏拒绝，12项定向、完整核心及维护120项实际PASS。继续数据能力与实际更新门槛，PRODUCT_HOLD / NOT RC。

## 可恢复资产移除进度（2026-10-22）

[实际记录](audits/UX_ASSET_ARCHIVE.md)：图片/Prompt/整册回收站、恢复与统一标签选用已接线；5项核心、2项桌面定向和完整196项实际PASS，首次测试编译失败保留。继续大资产恢复及数据读取/更新门槛，PRODUCT_HOLD / NOT RC。

## TypeSet 颜色兼容进度（2026-10-22）

[实际记录](audits/UX_TYPESET_COLORS.md)：162组独立源颜色样本、富文本/透明色及非法值恢复已验证；7项定向、完整189项实际PASS，历史失败保留。继续资产维护与数据读取/更新门槛，PRODUCT_HOLD / NOT RC。

## 设计应用与图片来源进度（2026-10-22）

[实际记录](audits/UX_DESIGN_PROVENANCE.md)：明确候选应用→最终请求、撤销/重做来源及编辑图回查源图已接线；10项定向、完整186项实际PASS，首次复制入口测试失败保留。继续剩余TypeSet/资产与更新门槛，PRODUCT_HOLD / NOT RC。

## TypeSet 队列操作进度（2026-10-22）

[实际记录](audits/UX_TYPESET_QUEUE.md)：按序号/右键移动、长队列边缘滚动、编辑后缩略预览与侧栏拖动已接入；最终完整180项实际PASS，全部失败保留。继续设计请求关联和剩余工程门槛，PRODUCT_HOLD / NOT RC。

## Desktop 相册与参照结构进度（2026-10-22）

[实际记录](audits/UX_HISTORY_REFERENCE.md)：固定Desktop源码的分组/筛选/选择策略已接入统一资产；Prompt与参数切换、角色对齐和摘取保留。13项定向、完整175项实际PASS；继续TypeSet和更新门槛，PRODUCT_HOLD / NOT RC。

## 自动资产收集进度（2026-10-22）

[实际记录](audits/UX_COLLECTIONS.md)：专用导入路径、多目录自动收集、恢复后重连门禁及维护排水已接线。最终完整166项实际PASS，初始化错误与历史超时证据保留；继续 Desktop 相册源码继承、TypeSet与更新门槛，PRODUCT_HOLD / NOT RC。

## 资产目录与多 Prompt 参照进度（2026-10-22）

[实际记录](audits/UX_LIBRARY_PROMPTS.md)：独立Prompt/标签别名/目录手动同步、图片Prompt预览及多来源角色拖动对齐已接线。核心91实际PASS、最终桌面68实际PASS，首轮完整回归1失败和修正保留；自动收集及完整恢复继续，PRODUCT_HOLD / NOT RC。

## 统一资产回用进度（2026-10-22）

[实际记录](audits/UX_SHARED_ASSETS.md)：编辑图片/PDF/ZIP资产与来源快照接线，跨引导/编辑选用保存重开；完整152项实际PASS。继续目录入口/Prompt资产/别名标签及原UX恢复，PRODUCT_HOLD。

## 创作页源码继承修正（2026-10-21）

[实际记录](audits/UX_CREATION_LAYOUT.md)：直接继承源图标/固定栏/缩略条并恢复连续编辑及窄窗摘要；desktop 完整64项实际PASS，首轮编译失败保留。产品恢复继续，PRODUCT_HOLD/NOT RC。

## UX-R4 实际A→B进度（2026-10-21）

[实际回执](audits/UX_R4_ACTUAL_AB.md)：磁盘阻塞已解除；真实A窗口下载/确认→NSIS安装→B启动/数据与测试凭据指纹核对COMPLETE。更新后继续编辑重开1项PASS，Drive证据已上传。故障覆盖与剩余UX继续，PRODUCT_HOLD/NOT RC。

## UX-R4 实际更新空间阻塞（2026-10-21）

[实际回执](audits/UX_R4_AB_SPACE_BLOCK.md)：A应用可信检查成功，下载因H盘剩余约147MiB被安全拦截。B未安装/重启，RC BLOCKED；已保留失败和精确清理候选，等待空间解除。

## UX-R4 维护排水与交接进度（2026-10-21）

[执行记录](audits/UX_R4_HANDOFF.md)：desktop完整62项、随后7项定向、Python更新组件18项实际PASS。实际A→B尚未执行；继续固定包演练，PRODUCT_HOLD。

## UX-R4 实际安装器进度（2026-10-21）

[实际记录](audits/UX_R4_INITIAL_INSTALLER.md)：真实NSIS安装544个文件核验、拒绝覆盖；新Python8项PASS。原生窗口存在但截图超时，视觉未验证；尚未串联A→B，继续实施，PRODUCT_HOLD。

## UX-R4 可信客户端进度（2026-10-21）

[实际记录](audits/UX_R4_TRUSTED_CLIENT.md)：隔离Python/TUF和检查/确认下载界面接线；desktop完整60项、最终主题3项、TUF8项/客户端7项实际PASS，失败保留。安装/重启/数据保全未完成，继续R4及剩余UX，PRODUCT_HOLD。

## UX-R2 长页PDF兼容（2026-10-21）

[实际记录](audits/UX_R2_LONG_PDF.md)：完整142项实际PASS，长页经Poppler/PDFium独立尺寸及像素核验；保留UserUnit方案兼容失败和验证脚本错误。完整恢复与R4继续，PRODUCT_HOLD。

## UX-R3 APNG进度（2026-10-21）

[实际记录](audits/UX_R3_APNG.md)：桌面57项完整PASS，最后确认状态修正后5项定向PASS；伪装/还原、批次文件交付、GIF时序、原件/副本重开和静态确认已测。失败记录保留；继续完整恢复和R4，PRODUCT_HOLD。

## UX-R3 导入进度（2026-10-21）

[实际记录](audits/UX_R3_INGRESS.md)：54项桌面完整实际PASS，核心未改未重跑；导入逐项回执、文件原字节/位图来源、取消文字粘贴拦截和重开已测。系统原生拖放/剪贴板未签；继续APNG和剩余恢复，PRODUCT_HOLD。

## UX-R3 选择应用进度（2026-10-21）

[实际记录](audits/UX_R3_RECIPE_SELECTION.md)：完整136项实际PASS；确认字段应用、取消、草稿冲突保护、撤销/重做和重开已测，初始编译失败保留。继续导入/图库及完整R4，PRODUCT_HOLD。

## UX-R3 PNG字段进度（2026-10-21）

[实际记录](audits/UX_R3_PNG_FIELDS.md)：完整132项实际PASS；最终页面可读性修正后4项定向PASS。PNG未知字段单图查看/两图比较、复制及勾选保留已验证，历史失败保留；继续完整R3/R4，PRODUCT_HOLD。

## UX-R2 预览可靠性进度（2026-10-21）

[实际记录](audits/UX_R2_PREVIEW_RELIABILITY.md)：完整工作台路径连续两次52项实际PASS；字体冷缓存和最大队列空间已测，独立挂载弹窗的历史超时及根因UNKNOWN保留。继续R3与安全更新，PRODUCT_HOLD。

## UX-R3 图库与比较进度（2026-10-21）

[实际记录](audits/UX_R3_GALLERY.md)：完整126项实际PASS；随后边界/GUI定向4项PASS，编译失败保留。图库筛选、比较/缩放/复制及确认回用保存重开已测；完整R3和安全更新继续，PRODUCT_HOLD。

## UX-R2 完整预览进度（2026-10-21）

[实际记录](audits/UX_R2_FULL_PREVIEW.md)：desktop49项完整实际PASS；完整预览键盘/状态隔离、宽窄输出布局及复制回归通过，初始失败与问题截图保留。core未改，继续恢复，PRODUCT_HOLD。

## UX-R2 预设消费进度（2026-10-21）

[实际记录](audits/UX_R2_PREFERENCE_CONSUMERS.md)：122项完整实际PASS；导出/队列/故事/连续打码默认已接入真实消费者，保存重开与备份恢复通过。继续完整预览、图库和安全更新，PRODUCT_HOLD。

## UX-R2 格式兼容进度（2026-10-21）

[实际记录](audits/UX_R2_IMAGE_FORMATS.md)：120项完整实际PASS，保留首轮GUI测试FAILED。WebP/GIF/BMP原件、首帧/默认图编辑、WebP/原格式导出与重开已验证；完整偏好/图库/更新继续，PRODUCT_HOLD。

## UX-R2 图片坐标兼容进度（2026-10-21）

[实际记录](audits/UX_R2_IMAGE_COORDINATES.md)：115项完整实际PASS。旧工程保留raw坐标，新工程记录EXIF方向，原件不变；格式/图库/应用内安全更新继续，PRODUCT_HOLD。

## UX-R2 四分类导出与Recipe恢复进度（2026-10-21）

[执行记录](audits/UX_R2_EXPORT_METADATA.md)：112项完整实际PASS；四分类完整序列、PNG元数据/隐写保留、隐私清除、隔离剪贴板及Recipe确认/撤销已测。保留编译失败，继续格式/图库/更新，PRODUCT_HOLD。

## UX-R2 工程与预设交换进度（2026-10-21）

[执行记录](audits/UX_R2_SOURCE_EXCHANGE.md)：107项完整实际PASS；源程序导出→Studio编辑→源程序回读、独立导入/取消、预设合并/替换、原件及备份恢复已验证。继续完整导出/图库/更新，PRODUCT_HOLD。

## UX-R2c 小人书与文件导出进度（2026-10-21）

[执行记录](audits/UX_R2C_BOOK_EXPORT.md)：完整100项实际PASS，随后JPEG质量定向3项PASS；整册PNG/ZIP/PDF、目录选择接线、故障保全和重开已测。继续源交换/完整导出/图库/更新，PRODUCT_HOLD。

## UX-R2预设与默认进度（2026-10-21）

[执行记录](audits/UX_R2A_PRESETS.md)：跨工程文字预设、四槽打码/启动默认、源新建文字限制已接入；完整core53+desktop43共96实际PASS。保留全部历史失败，PRODUCT_HOLD；继续源文件交换、完整导出、图库与安全更新。

## UX-R2对象与队列进度（2026-10-21）

[执行与界面证据](audits/UX_R2A_OBJECTS_QUEUE.md)：core50+desktop43共93项实际PASS；对象锁定/层序、工程另存/删除、Ctrl/Shift多选拖序及重开已测。七次运行含失败保留；PRODUCT_HOLD，继续预设/交换/完整导出与R3–R4。

## UX-R2批次与打码进度（2026-10-21）

[来源比较与执行记录](audits/UX_R2B_MASKS.md)：12组源图对照、真实Compose指针/保存/重开已测；修复防抖保存取消误报。最新desktop完整42 PASS，core46实际PASS来自前次运行且源码未变。失败保留，PRODUCT_HOLD，继续完整R2–R4。

## UX-R2a 原生排版工程进度（2026-10-21）

[实际执行证据](audits/UX_R2A_EDITOR.md)：core+desktop完整80项PASS，后续字体格式定向4项PASS；10次构建/测试尝试及失败保留。来源样本对照、工程保存/重开、字体与备份恢复已测；完整编辑/批量/整册导出继续实施，原生视觉和真人验收NOT TESTED，PRODUCT_HOLD。

[TypeSet来源样本冻结](audits/TYPESET_SOURCE_REFERENCE.md)：16项输出及独立PDF/ZIP核验，原件启动失败与测试副本差异明示；继续R2原生实现，非产品PASS。

[UX-R1b创作/回用检查点](audits/UX_R1B_CREATION.md)：39 desktop实际PASS；保留17次构建/测试运行。继续完整UX恢复，非RC。

# 当前执行：UX恢复连续实施

[正式授权合同](../tasks/UX_RECOVERY_CONTINUOUS.md) · [源样本/Q09补正](ux/TYPESET_REFERENCE_VALIDATION.md) · [75项实现证据台账](ux/ACTION_IMPLEMENTATION.json)。UX-R0审查通过不等于产品通过，当前PRODUCT HOLD。

[UX-R1a工作台工程检查点](audits/UX_R1A_WORKBENCH.md)：真实应用接线/本地HTTP流程，23项desktop新测试PASS；原生截图超时单独记NOT TESTED，继续R1b。

[UX-R1b引导/Q09中间检查点](audits/UX_R1B_GUIDANCE.md)：155项新执行PASS，R1b仍在实施；未达到产品验收。

# 当前审阅入口：UX-R0 产品恢复合同

**PRODUCT HOLD / NOT ACCEPTED。** Issue #4最新完整继承要求下，已交付[整体Project审阅入口](ux/UX_R0_PROJECT_REVIEW.md)、[来源差距矩阵](ux/UX_R0_PARITY_MATRIX.md)、[逐动作继承合同](ux/UX_R0_ACTION_TRACE.md)、[GUI工作流/宽窄结构](ux/UX_R0_WORKFLOW_AND_LAYOUT.md)、[完整实施方案](ux/UX_RECOVERY_IMPLEMENTATION_PLAN.md)。[当前任务](../tasks/UX_R0_PRODUCT_RECOVERY_AUDIT.md)与[最新Issue范围](../tasks/UX_R0_ISSUE4_FULL_ADOPTION_ADDENDUM.md)。本轮仅文档，Project批准后再实现；下面均保留历史。

# 00 — 文档入口

**当前用户验收结论：USER_REVIEW_FAILED。见[首轮反馈与源码定位](rc/PREVIEW_USER_REVIEW_1.md)。下面Preview交付入口保留历史，不代表用户通过。**

**最新入口：[Windows Preview Gate完整交付](rc/PREVIEW_PROJECT_REVIEW.md) · [集中真人验收](rc/PREVIEW_REVIEW_ITEMS.md) · [GitHub/Drive回执](audits/evidence/WINDOWS_PREVIEW/DELIVERY.json)。状态PREVIEW_GATE_WITH_GAPS；实现209b5252已推送，待集中review，历史RC BLOCKED不可替代当前状态。**

## 每次任务首读

1. [当前状态](CURRENT_STATE.md)
2. [任务登记](TASK_REGISTRY.md)
3. [来源锁](SOURCE_LOCK.json)
4. 当前 [Windows RC连续合同](../tasks/WINDOWS_RC_CONTINUOUS.md)与[checkpoint恢复台账](rc/CHECKPOINTS.md)；A1/R1原合同和证据保留历史，不从main重做

## 产品与工程合同

| 文件 | 用途 |
|---|---|
| [01章程](01_PROJECT_CHARTER.md) | 定位/项目边界 |
| [02需求](02_PRODUCT_REQUIREMENTS.md) | 平台/模式/首版 |
| [03架构](03_ARCHITECTURE_AND_REUSE.md) | 共享/适配/无环 |
| [04数据](04_DATA_SAFETY.md) | 原图/工程/恢复 |
| [05更新](05_UPDATE_AND_RELEASE.md) | 首版更新/发行 |
| [06服务端](06_SERVER_SECURITY.md) | 队列/撤销/授权 |
| [07来源](07_SOURCE_MAP.md) | 精确入口与证据 |
| [08协作](08_GIT_AND_COLLABORATION.md) | Git/工作区/反哺 |
| [09路线](09_ROADMAP.md) | 切片和退出标准 |
| [10验收](10_ACCEPTANCE_MATRIX.md) | 测试与证据分类 |
| [决定](DECISIONS.md) | 初始约束/待设计 |
| [跨项目](CROSS_PROJECT_REGISTRY.md) | 候选与目标验收 |

## 初始化和交接

[使用入口](../START_HERE_CN.md) · [Project指令](../instructions/PROJECT_INSTRUCTIONS.md) · [Codex开场](../handoff/CODEX_START_MESSAGE.md) · [新Project接手](../handoff/NEW_PROJECT_OPENING.md) · [Desktop协作说明](../handoff/DESKTOP_COLLABORATION_NOTE.md) · [参考文件](../references/README.md) · [打包边界](../PACKAGING_NOTES.md)。

[合并母本](../CCB_STUDIO_SUITE_INIT_20261020.md)用于初始上传/引继，是有日期快照。后续以Git的分文件CURRENT和当前任务维护；不要求同步改写历史母本。

源码未实施前不要因这些文件存在宣称功能PASS。

## A0-001 历史交付（A0-002 已 PASS_WITH_SCOPE）

[精确源码审计](audits/A0_001_REUSE_AUDIT.md) · [依赖、差距与变更回执](audits/A0_001_DEPENDENCY_AND_GAPS.md) · [来源及验证证据](audits/A0_001_SOURCE_EVIDENCE.json) · [A1-001任务（本轮限定授权）](../tasks/A1_001_IMPLEMENTATION_CANDIDATE.md)。A0没有产品实现或测试验收。

[Project审阅交接入口](../handoff/PROJECT_A0_REVIEW.md)：用户在A0交付后追加授权上传Git；原报告中的无commit/push是审计结束时的历史事实，当前交付使用Git实际commit定位。

## A1-001（2026-10-21，实施/合成验证完成，待审查）

用户提供A0-002结论PASS_WITH_SCOPE，允许从6d3d2b2在独立功能分支执行数据切片；实施时授权不含Git提交/推送；用户随后要求远程交付，本次追加Git交付入口见[Project A1审阅交接](../handoff/PROJECT_A1_REVIEW.md)。见[实际实施/21项测试回执](audits/A1_001_EXECUTION.md)、[未提交补丁](audits/evidence/A1_001/source.patch)、[文件状态与SHA清单](audits/evidence/A1_001/WORKING_TREE.json)、[模块合同与运行方式](../modules/asset-library/README.md)、[更新器后续设计](audits/A1_001_UPDATER_DESIGN.md)。A0报告和来源JSON保留原始快照，产品验收矩阵不提前改PASS。

## A1-001-R1（2026-10-21，针对性安全补强）

原A1@3c17b4c9审查PASS_WITH_SCOPE由用户提供；本轮只修复目录完整性和备份/关闭排水竞争。见[R1 Project审阅入口](audits/A1_001_R1_PROJECT_REVIEW.md)、[真实实施/失败沿革/恢复语义](audits/A1_001_R1_EXECUTION.md)、[本轮27项实际结果](audits/evidence/A1_001_R1/FINAL_TEST_CASES.json)、[源码身份](audits/evidence/A1_001_R1/SOURCE_IDENTITY.json)。GitHub/Drive交付以实际回执为准；不改旧证据或产品验收矩阵，不自动继续更新器/其他阶段。

R1实际交付：[GitHub/Drive回执](audits/evidence/A1_001_R1/DELIVERY.json)及[固定实现和附件入口](audits/A1_001_R1_PROJECT_REVIEW.md)。实现a9ac4712已远程核验；Drive ZIP原字节回读hash一致，后续提交仅补交付记录。

## Windows RC 连续任务（2026-10-21）

已实际读取[#1正式Gate](https://github.com/MisakaPiano/CCB-Studio-Suite/issues/1#issuecomment-6296885017)、[#2完整合同与R1审查](https://github.com/MisakaPiano/CCB-Studio-Suite/issues/2)。从R1@27dbd5d5独立分支继续；[RC状态](rc/RC_READINESS.md)、[CP台账](rc/CHECKPOINTS.md)、[更新A→B计划/真实TUF合成lab边界](rc/UPDATE_A_B_PLAN.md)、[权威依赖STOP](rc/DEPENDENCY_ESCALATION.md)、[合同原始快照](audits/evidence/WINDOWS_RC/CONTRACT_SOURCE.json)。不改历史证据，不把CP1库/信任lab称为RC。

CP1 [实际执行/环境/测试与STOP回执](rc/CP1_EXECUTION.md)：31项完整JVM回归和8项真实TUF合成信任验证已执行；整体RC BLOCKED，未产出安装包。

Windows连续任务实际远程交付：[RC BLOCKED/CP1固定审阅入口](rc/PROJECT_REVIEW.md)、[机器恢复台账](rc/CHECKPOINTS.json)、[GitHub/Drive实际回执](audits/evidence/WINDOWS_RC/DELIVERY.json)。

## Windows Preview resume / Issue #3

Latest Project correction rescinds generalized source-rights / missing-SDK STOP. Same owner resumed from 86760bc0; historical RC BLOCKED evidence remains. Current work is IN PROGRESS toward Preview Gate, with actual 69 authority + 8 Studio core synthetic tests PASS. See [Preview progress](rc/PREVIEW_PROGRESS.md) and [Issue #3](https://github.com/MisakaPiano/CCB-Studio-Suite/issues/3). Real API use is additionally authorized at <=12 attempts/rolling hour and >=300s interval; no real calls at this checkpoint. Not RC/Preview acceptance.

当前执行入口：[Preview任务](../tasks/WINDOWS_PREVIEW_GATE.md)、[P2进度](rc/PREVIEW_P2_EXECUTION.md)、[集中待验项](rc/PREVIEW_REVIEW_ITEMS.md)。历史 CP1 STOP 已纠偏；请以 CURRENT STATE 页首和后续 Preview Gate 回执为准。
