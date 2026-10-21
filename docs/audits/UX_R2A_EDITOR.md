# UX-R2a 编辑与排版工程检查点

接续已推送来源样本 `1140cfd56775f156ea084ad5d510221960acc165`，同一 Suite 分支与执行者；来源冻结先于原生实现。当前 **ENGINEERING_PARTIAL / PRODUCT_HOLD**，继续 R2–R4，不提交真人验收。

## 已实现与已测边界

- 正常工作台进入“编辑”，从图库所选原件建立命名工程；独立页面 ID、当前页与批次选择、基础顺序/移除、文字对象、选框/底部或右侧尺寸手柄、横竖排、轻标签富文本、字体/样式/透明度/背景边框、同工程小人书预览。
- 工程正文使用既有 AssetStore 保存不可变分块，16 MiB 文档上限、4 MiB 分块；应用 journal 只保存当前/30次撤销重做引用。资产写完再提交 journal，失败保留孤立快照；原件/数据安全核心未修改。新正文版本独立判断，旧 v1/v2 工程保留而不静默转换。
- 真实 Compose 树走通命名→输入→切竖排→自动排版完成→保存→撤销/重做→宽窄视图→关闭重开。仅测试程序外置合成图，正常产品路径没有调用合成生成器。截图是实际 Compose 组件离屏渲染，**不是原生实机截图或真人验收**。
- 中文/日文竖排替代字符、换列和 advance，与冻结源逐项一致；横排三种宽度及混合样式换行一致；小人书空/短/长旁白、空段与页码开关的行数/逐行文本/宽高一致。原生长页输出与 TypeSet 样张分别保留，不采用自生成期望代替源对照。
- 原生视觉检查发现 AWT Windows ascent 与 Canvas top baseline 相差约10px，已测 Canvas 32px 微软雅黑基线24.390625，用字体 OS/2 typo ascent/descender归一化修复，增加独立回归。依据 [Chromium GetFontBaseline](https://chromium.googlesource.com/chromium/src/+/88d7d488e5d1d8625bb2e68f015b9e881ea16120/third_party/blink/renderer/core/html/canvas/text_metrics.cc) 的语义；实际来源身份与实测值优先，不要求跨引擎抗锯齿逐像素相同。
- 字体本地导入持久化与恢复：TTF/OTF 原字节、WOFF/WOFF2 原包及解包副本分别存资产；只在 JVM 中使用，不安装到 Windows。使用 [IDWriteFactory5.UnpackFontFile](https://learn.microsoft.com/en-us/windows/win32/api/dwrite_3/nf-dwrite_3-idwritefactory5-unpackfontfile)；SDK10.0.22621只读核对 COM ABI，Windows10 15063+；输入/声明解包大小上限64MiB。该上限检查不是对任意恶意字体的完整模糊测试或进程内原生内存隔离证明。

## 实际验证

[保留的运行目录](evidence/UX_R2A_EDITOR/runs/) 包含全部本轮尝试、JUnit XML、source-before、命令及失败日志。完整 core+desktop 回归 `20261021T063813928019Z-UX-R2a-complete-module-regression`：**80项实际PASS（core40、desktop40），0 FAILED / 0 SKIPPED，sourceStable=true**。执行161.752秒。authority和asset-library源码未改，本次不将其UP-TO-DATE编译算成新测试。

千页工程、历史前后移动、陈旧revision拒绝、同根重开、最小完整备份恢复、正文hash异常、journal暂存失败保全均执行；恢复仍拒绝未知暂存文档，需要既有恢复流程，不新增自动删除/覆盖。千页测试是仓储与备份验证，不声称千页GUI性能已测。

失败记录保留：首次UI编译缺onSizeChanged导入；测试帮助函数语法错误；UI使用未配置Dispatchers.Main导致建立工程失败，改为继承现有UI dispatcher后通过；JNA Function名称被Kotlin同名接口遮蔽，显式导入后字体测试通过。首次UI截图在异步排版尚未完成时取得，后续增加“排版中/已更新”状态与渲染等待断言后重新取图，未把旧截图当完成结果。

JDK Temurin17.0.20.1 / Gradle9.1，离线、严格依赖hash、单worker、单测试fork、256MiB测试heap，无新增Gradle依赖下载。小型WOFF2测试数据来自Microsoft官方MIT示例代码，来源blob/许可随测试资源保存；OTF使用本机SourceSansPro只读文件并登记hash，不复制字体。新runner将测试resources纳入源码身份。真实API调用本连续恢复轮次仍0。

## 后续仍须完成

R2a完整对象操作/队列拖序/预设与旧工程显式迁移尚待补齐；目前打码 renderer 与工具入口仅中间实现，四效果/批次语义尚未完成源对照，**不得当作R2b交付**。同样，小人书预览已接入但整册PDF/ZIP原生导出尚待R2c完成。字体选择/混合样式/尺寸手柄原生鼠标与IME质量NOT TESTED；当前接口错误、字体丢失、导出失败的产品提示仍需后续完善。

完整图库比较、四类输出及隐私/APNG/文件剪贴板、实际安装A→B与W1–W5最终自检均继续实施，未降级更新门槛。当前未产生新RC安装包，不merge、不发布。非Git最终包尚未形成，Drive本检查点NOT UPLOADED，Git保存全部精简合成证据。

补充字体格式回归 `20261021T064157275047Z-UX-R2a-font-format-regression`：4项实际PASS，0失败/跳过，sourceStable=true，25.884秒；含本机OTF只读导入，不把此定向执行当成新版完整回归。
