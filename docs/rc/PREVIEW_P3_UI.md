# Preview P3 — 现代 Windows 工作台

继承已推送 P2 `57c909bbadfe134530922ffa19d693d553325139`，本检查点不重做 A1/R1/CP1。直接用户要求：参考最新 CCB Desktop 改造整个 UI，所有 Suite 窗口统一蓝色 CCB 标识。

## 改动与来源

- 44dp 自绘顶栏：蓝色小象、生成/编辑/记录、账户、最小化/最大化/还原/关闭。原生 Windows 控制桥仅作用于本窗口，关闭仍进入 Studio 排水保存；保留 Windows 原生缩放、系统菜单和 Snap 行为的实现入口。
- 紧凑操作栏：资料库开关、导入、新工程、撤销重做、备份/恢复、导出。主区域使用深灰蓝语义配色、独立画布、图库缩略图与筛选、底部页面条、可折叠负向 Prompt/角色区。窄窗口切为单面板；窗口默认1260×830。
- 原始资产、编辑工程、撤销顺序、导出与恢复语义保持原合同。图库上下拖动、按钮排序和批量图层保留。无新增真实 API 请求，UI 明示合成模式。
- 视觉参考为 Desktop 已提交 `ab3d06abcaca2df8b21c734841d7c4e744099f51`。两个窗口桥文件按原字节复用；`.gitattributes -text` 保留其原行尾，清单见 `modules/studio-desktop/WINDOW_CHROME_SOURCE.json`。没有读取/覆盖 Desktop 并行未提交工作，没有构建 Desktop。
- 图像 Prompt/Designer/NAI 权威仍为 `5ef95428421f7c693b010ac88f51d470804ab07a` 的85文件闭包。两种来源身份分别登记，不以最新 UI SHA 偷换请求语义。
- 蓝色 logo 由已授权固定 CCB 原图使用 imagegen 衍生，ICO 仅做多分辨率容器编码；原图和输出的 hash、生成提示见 `brand/PROVENANCE.json`。不声称生成结果逐像素保形，细节列真人验收。

## 实际验证及失败保全

`modern-chrome-first` 首轮编译 FAILED：Compose 嵌套作用域不能隐式访问外层 maxWidth；0项测试实际执行。已改为外层明确计算宽度标志。原始失败日志不覆盖。

`modern-chrome-fixed` 实际6项窗口测试 PASS，0失败/错误/跳过；新增 JNA 编译依赖与 JUnit 配置离线解锁后写回依赖锁。涵盖交互区域不可拖拽、DPI缩放后的按钮命中、最大化边缘、关闭回调、负坐标显示器和未测量布局。并成功生成独立 app image。最终 strict/offline 复核另见 modern-final 原始回执。

实际桌面会话启动自检窗口：121次绘制、原生最大化→还原→最小化→还原成功；两图建工程、批量两层、撤销/重做、排序、合成设计/修改/生成、三种导出、备份全部完成。通过 `DesktopWindowCommand.CLOSE` 进入正常关闭，进程退出码0；没有强杀。再次启动完成2次绘制并正常退出0，状态 SHA-256 `67a56983e77178f4a656cca2fd069b3529de58e4adcf8626aead13b92d6b7a84` 与三份原件身份完全一致。JSON/退出回执见 evidence/WINDOWS_PREVIEW/ui。

这些是应用自身绘制/控制器/native-command 自检，不是鼠标屏幕自动化。既有 Computer Use 截图/坐标方法仍 ENVIRONMENT_UNAVAILABLE，未以其他截屏手段冒充成功。用户首张旧版截图与全部失败/退出沿革保留，真人外观和鼠标验收 NOT TESTED。

## 环境与范围

Temurin JDK17.0.20.1+1，Gradle9.1.0，Kotlin2.3.20，Compose1.10.3，JNA5.18.1。串行单 worker、2CPU、512MiB build heap；本次 UI 依赖/测试全部使用已有隔离缓存离线解析。原P2完整84项（69权威+15核心）实际结果及源码hash保留；本次只重跑受影响的完整桌面测试模块，不把未执行的历史31项资产测试改称本轮PASS。

最终包是 Windows x64 内部 Preview 的可直接运行 ZIP（含运行时），不是安装器、签名发行或 RC。CP4 应用内 A→B 安装/迁移/凭据保全尚未实施验收。真实账户 UI 接线、完整 live Designer、多供应商、富文本排版、任意原始 metadata 全量复制等缺口在集中清单明确列出。

## 交付索引完整性修复

P3 提交前检查发现原有 `data/` 忽略规则误匹配 `modules/image-authority/src/main/kotlin/com/example/chatbar/data/`，导致 P1/P2 已推送树缺少17份清单内源码。那些文件一直在本地实际构建/测试输入中，但旧检查点单独检出不能完整复现；先前仅工作区 hash 检查不足以证明 Git 闭包，现明确纠正。P3 只给此精确源码目录增加例外，逐文件敏感扫描并核验与5ef95428及清单完全一致，再显式纳入 Git。用户资料 data/ 忽略仍有效。最终索引检查覆盖所有85份权威来源和135份最终测试输入，不把旧树的不完整性隐藏。

`git diff --check` 对原始 CRLF 日志/保留行尾发出空白警告，以及 Main.kt 多一个 EOF 空行；它不是执行测试失败。原始证据及原字节来源不为消除格式警告而重写。
