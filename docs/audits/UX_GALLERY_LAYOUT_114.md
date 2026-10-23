# Preview114 — 图库布局、分割线与全页菜单

## 授权与更正范围

用户已批准图库“批次→分组→图片”和虚拟相册布局，随后拒绝113实际组织树的偏差。113技术59项通过、公开更新链检查等事实保留，**样式未通过用户验收**。本轮继续原授权实施并沿现有Preview渠道发布，不重新设计替代已确认demo。Prompt对比继续PENDING。

最新要求将收起/展开统一放到组织树分割线上，悬浮显示操作方向、点击切换，拖动仍能改宽；移除顶部不相干的文件夹展开按钮和树头“收起”按钮。逐页自查下拉文字，修复按钮13sp而Material菜单默认subtitle1为16sp、CompactChoice列表14sp的差异。

## 实现

- 组织树保留160dp默认宽度、130–320dp拖宽；常驻全部资产及计数、生成来源/批次、自定义组织/虚拟相册，按demo缩进及当前选中背景。新建相册为树头图标；仅当前批次展开子分组。
- 批次根显示批次卡片，逐层进入分组、图片；排序改变封面后仍保留卡片组织身份。虚拟相册保留子相册和直接关联图片，原有工具/筛选/导入/详情/回收站入口保留。
- 分割线中央把手为同一收起/展开入口，含方向图标、提示、键盘按钮语义；整个分割线（包括把手）可拖宽。
- 各页Material下拉经同一13sp菜单/菜单项控件，说明弹层继承13sp；CompactChoice按钮与列表统一13sp。保留原选中、禁用、键盘/焦点和弹层定位规则。

## 实际验证与过程记录

20张纯合成PNG、2批次/5分组/3虚拟相册，与demo组织规模相同；旧升级探针的1张无来源图不再用作样式核对。实际Compose渲染截图位于 `modules/studio-desktop/build/gallery-layout-114/`，合成资料路径另存fixture.json。

逐页源码入口审计记录 `.local/windows-rc/gallery-layout-114/MENU_AUDIT.json`；实际展开检查创作模型、AI设计历史、图库来源/标签匹配/操作/导入、编辑工程、收集类型、回收站类型、账户视觉模型、批次添加、APNG添加。读取实际TextLayoutResult核对菜单13sp，不能将源码审计当所有实际操作已通过。

首轮编译因TooltipArea实验API缺OptIn失败；首轮测试因测试返回Path（JUnit要求void）及中央按钮阻挡拖宽失败，均保留证据。修复后实际2项通过。扩展检查发现排序换封面导致组织卡片身份丢失，已改为全部成员稳定映射并核对实际卡片名称/计数及指针进入。逐页扩展测试的合成账户provider错误、单击等待双击判断、返回图库尚未渲染即查找节点等测试问题也已修正；未把失败的整轮计为PASS。各页菜单均已产生实际渲染截图，最终冻结回归、安装、签名发布及实际旧客户端更新尚未完成，不预先标PASS。

按用户既有清理授权，仅删除113 `candidate/payload`、`smoke-113`两个已核验清单且无运行进程的重复程序目录，回收505174745字节。保留113已验证安装、公开安装器、审计证据及全部用户资料；精确记录 `.local/windows-rc/gallery-organization-113/REDUNDANT_PAYLOAD_CLEANUP.json`。

私人数据/真实生成调用：0。日常安装不自动切换；不改Desktop、main或原信任根。

## 冻结执行与发行完成

固定应用/组装 `f59b0c25a2eb9d28ad2392d53e8cce1a95857a99` 已推送，73项实际执行全部PASS，0失败/错误/跳过，源码稳定；createDistributable实际完成。范围含图库/相册/组织/指针与键盘、20图逐页菜单、批次429及更新/资料保留；最终证据 `.local/windows-preview/evidence/20261023T210910441011Z-gallery-layout-114-final-regression`。菜单源审计22文件，实际页面展开及TextLayoutResult字号断言执行通过；[合成图库](evidence/UX_GALLERY_LAYOUT_114/batch-root.png)、[分组](evidence/UX_GALLERY_LAYOUT_114/batch-groups.png)、[分割线悬浮](evidence/UX_GALLERY_LAYOUT_114/divider-hover.png)、[来源下拉](evidence/UX_GALLERY_LAYOUT_114/source-menu.png)为真实Compose界面渲染，非重画的概念。

[Preview114](https://github.com/MisakaPiano/CCB-Studio-Suite/releases/tag/v0.0.0-preview.114)及原签名渠道已发布。元数据 `7504ac1f78535f847c5efa356054be1be54cac74`，版本 {'targets': 13, 'snapshot': 19, 'timestamp': 19}；根/main不变。安装器168633807字节，SHA256 `82464978a84decaada263459374450251ec21f070e33047e3f6647395aef9971`；550安装文件核验、重复安装拒绝且文件不变，实际版权材料随包核验。

107原装运行时从正常公开地址检查并下载114，原装helper执行新目录安装及测试固定入口发布。实际114原生首帧B回执与旧合成资料快照一致；114再查已是最新。10份旧合成资料、550旧程序文件保留。仅A父进程身份/排水替为已关闭的合成父进程，**不代签日常真人更新或人工样式验收**。首次CDN旧目标若出现，保留尝试并等正常渠道114后再继续。完整本地记录 `.local/windows-rc/gallery-layout-114/LIVE_UPDATE.json`、`NATIVE_STARTUP_CHECK.json`、`publication/PUBLICATION_RESULT.json`；公开精简证据见 [RELEASE_EVIDENCE.json](evidence/UX_GALLERY_LAYOUT_114/RELEASE_EVIDENCE.json)。

逐页范围说明：本次AI设计历史为空列表，只实际验证展开/关闭；非空文字字号由同一CompactChoice在创作、编辑、账户页的实际布局断言覆盖，不冒称已有设计历史数据验收。更新页当前无下拉菜单。其余静态菜单入口经22文件源码审计，共用菜单字体约束；不把有限合成例子外推为所有真实内容、系统文件对话框或右键菜单的穷尽验收。

两次原生测试窗口在B回执及当前版本验证后均核对PID、完整路径和合成资料参数关闭；CloseMainWindow未找到普通主窗口，随后仅终止本轮合成测试进程，不冒称原生优雅退出通过。该限制不等于既有维护/排水自动测试失败。

发布及实际升级通过后，继续按既有安全清理授权核验114两个重复程序目录 `candidate/payload`、`smoke-114` 的发行文件清单、精确根及无运行进程后删除，回收505201199字节；保留实际已验证114程序、安装器、许可和全部证据。合计本轮113/114重复程序回收1010375944字节。记录 `.local/windows-rc/gallery-layout-114/REDUNDANT_PAYLOAD_CLEANUP.json`，H盘余约848MiB；不清理日常安装或用户数据。

日常App、私人资料和凭据未改，真实调用0，Prompt对比PENDING。下一步由用户从原固定入口“更新”获取114；不用重导入或重新生图。113技术通过与布局拒绝分别记录，不把本轮自动通过当用户认可。
