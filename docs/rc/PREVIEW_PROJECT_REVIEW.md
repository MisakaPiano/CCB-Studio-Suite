# Windows Preview Gate — PREVIEW_GATE_WITH_GAPS

2026-10-21 JST，依据 Issue #3 / #2 及本会话直接追加授权交付。现代 UI 和蓝色 CCB 标识已实施，真实包启动/窗口命令/主要合成工作流/安全退出/重启均有证据。现在停止工程推进，等待集中真人 Preview review。不是 RC，也不是正式发行。

## 固定入口与双通道交付

- 开发分支：`feature/windows-rc-continuous`。最终实现固定提交：[`209b5252e06ea55b6cdf970da131b330a407e882`](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/209b5252e06ea55b6cdf970da131b330a407e882)。本回执所在后续提交只维护交付与控制记录，不改变包的实现输入。
- [Windows x64 Preview ZIP](https://drive.google.com/file/d/1OfnmWokv_Dl68IWczaA1K80vgCYaigSE/view)：61,399,607 bytes，含 JDK 运行时，无须另外安装 Java；完整解压后运行 `CCBStudioSuitePreview/CCBStudioSuitePreview.exe`。
- [原始证据 ZIP](https://drive.google.com/file/d/1yPCUX4CR_2qenXwVnvJxa9cRY9QyJ7Fa/view)：560,219 bytes，包含原始运行日志、旧屏幕观察、合成真实供应商输出与校验清单。
- 两个文件均位于用户指定 Drive 文件夹，上传后按原字节回读，大小/SHA-256/ZIP CRC/全部 manifest 条目一致。未更改共享权限或配置 OAuth，未上传凭据。包279项、证据155项；最终“解压包实际启动/重启”的追加回执在 Git 的 ui/package-* 文件中，不伪称它们已包含在较早封存的证据 ZIP。
- [机器交付回执](../audits/evidence/WINDOWS_PREVIEW/DELIVERY.json)、[包清单](../audits/evidence/WINDOWS_PREVIEW/PACKAGE_MANIFEST.json)、[SBOM与依赖声明](../audits/evidence/WINDOWS_PREVIEW/PACKAGE_SBOM.json)、[证据清单](../audits/evidence/WINDOWS_PREVIEW/RAW_EVIDENCE_MANIFEST.json)。

```text
Preview ZIP SHA-256
0ac4e63be314c513f90000ffee22a1e285e46a3975fbc554fa726fdac9683ec6

Evidence ZIP SHA-256
303ddf33a939f5782b3cf37080b1bb2ef8436382d7601fc25a56e1255a0f7320
```

## 本次实际变化

P1建立固定权威闭包与 Studio 持久工作流；P2接入独立 Windows 界面、恢复/导出/编辑和蓝色标识；P3按最新 Desktop 风格现代化整个工作台：自绘顶栏与原生窗口控制、生成/编辑/记录导航、操作工具栏、图库缩略图/筛选/收起、底部页面条、画布缩放、参数折叠及窄窗口布局。详细路径与来源见 [P3记录](PREVIEW_P3_UI.md)、[P2记录](PREVIEW_P2_EXECUTION.md)、[P1记录](PREVIEW_PROGRESS.md)。

图像权威固定 `5ef95428421f7c693b010ac88f51d470804ab07a`，85份原字节文件；视觉/两个窗口控制文件固定 `ab3d06abcaca2df8b21c734841d7c4e744099f51`。没有改动 Desktop、upstream、历史母本或真实图库。

交付校验纠正了旧 `data/` 忽略规则误排除17份权威包源码的问题：P1/P2远程树独立检出曾不完整，本地实际测试输入并未缺失。P3精确放行该源码目录后补齐，85份权威文件、两个窗口文件已逐一核对 Git index 和来源 hash；全部135份最终测试输入都能从 Git 还原，12份仅有明确的 CRLF/LF 文本归一化差别。见 [索引验证](../audits/evidence/WINDOWS_PREVIEW/P3_INDEX_VERIFICATION.json)。所有59份受保护历史文件保持原字节。

## 实际执行矩阵

| 范围 | 状态 | 证据与界限 |
|---|---|---|
| 权威图像核心 | PASS，69项实际测试 | P2 final-regression；源码随后未改，保留完整 JUnit/golden；不是重跑计数 |
| Studio核心 | PASS，15项实际测试 | 同次P2回归；原图/编辑/导出、DPAPI合成往返、限额、真实子进程崩溃/锁、恢复拒绝异常；源码未改 |
| 新桌面模块完整测试 | PASS，6项 | P3 modern-final，0失败/错误/跳过，strict/offline且稳定源码；首轮编译失败日志保留 |
| 资产模块 | HISTORICAL PASS，31项 | CP1当时实际执行，本轮未改/未重跑，不代签新的执行结果 |
| 可运行包 | PASS（工程） | 最终ZIP279项校验；解压包实际绘制133次；原生max/restore/min/restore、合成工作流与退出0 |
| 最终包重启 | PASS（工程） | 第二次正常退出0，状态与三份原件身份一致；非任意断电或应用升级测试 |
| 硅基流动/NovelAI | LIMITED ACTUAL PASS | 各1次HTTP200；共2次实际请求，间隔546767ms；NAI27中间帧+1最终PNG。完整live Designer/多供应商仍NOT TESTED |
| 屏幕控制 | ENVIRONMENT_UNAVAILABLE | 截图超时与坐标不可用；没有声称鼠标或视觉自动验收成功。旧用户截图保留 |
| 真人UI、DPI、IME、Snap、排版 | NOT TESTED / UNKNOWN | 集中清单逐项验收；原生命令自动化不替代真人效果 |
| 实际安装器与安全应用内A→B | NOT IMPLEMENTED / NOT TESTED | CP4完整合同保留；本交付不是RC、安装更新或凭据迁移PASS |

环境：Temurin17.0.20.1+1、Gradle9.1.0、Kotlin2.3.20、Compose1.10.3；单worker、2CPU、512MiB build heap、384MiB metaspace。使用Suite隔离Gradle/Maven缓存，初始从既有公共依赖缓存只读复制224项/171,287,006bytes，缺少依赖与元数据从Maven Central、Google Maven、Gradle Plugin Portal补取；原始日志/校验锁保留。Compose第一次打包曾下载官方WiX3.11.2工具ZIP，未运行安装器。P3全部离线。已保留JAR内声明、JDK legal、Skiko/Skia许可证及POM来源；完整公开发行原生第三方归属审计仍属RC准备范围。

## 已知缺口与下一步

1. 当前UI明确使用合成账户/transport；真实账户录入和完整live Designer未接线。两次受控真实探针不是完整产品验收，探针入口不在分发包内。
2. 竖排为基础列布局，富文本/复杂标点混排未完备；普通渲染保留CCBStudioRecipe但不复制任意原件metadata。原始导出保持完整字节；隐私输出仍需检查视觉遮盖效果。
3. 鼠标与外观/DPI/IME等需要真人；原始截图、应用内绘制和命令测试各自标明证据边界。
4. Preview只允许隔离合成资料；首个长期使用版本的完整安全更新、真实A→B安装/恢复/迁移/凭据保全未完成，不降级合同门槛。

下一项仅为 [一次集中真人验收清单](PREVIEW_REVIEW_ITEMS.md)。完成后由用户/Project统一决定修补与后续RC范围；本会话不会自动进入CP4、合并main或发布。
