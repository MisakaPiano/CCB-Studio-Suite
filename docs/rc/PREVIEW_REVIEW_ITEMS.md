# Preview 集中真人验收清单

本轮结论为 PREVIEW_GATE_WITH_GAPS。先将交付 ZIP 完整解压到新目录，运行 CCBStudioSuitePreview/CCBStudioSuitePreview.exe；运行环境已随包附带。使用“+ 样例”创建合成资料。默认独立数据根为 `%LOCALAPPDATA%/CCBStudioSuitePreview/synthetic-v1`，也可用 `--synthetic-root <新的绝对目录>` 指定。不要导入真实图库或凭据。

一次集中完成以下操作并记录“通过 / 有问题 / 未测”即可，不需要逐切片批准：

1. 查看蓝色 CCB 小象、顶栏生成/编辑/记录导航、导入/撤销/导出按钮；拖动空白顶栏、双击、最小化/最大化/还原、四边缩放与 Windows Snap。检查 100%/150%/200% 缩放、窄窗口、中文输入法与键盘焦点。
2. 创建两份样例，选择后建工程；在资料库缩略图上拖动上下排列，或使用上/下移按钮；切换缩略图、隐藏/显示资料库，检查画布缩放和长文本滚动。
3. 编辑页在图像上拖绘矩形、画笔、圈选与文字；检查操作应用于已选工程页面，撤销/重做、隐藏图层、横竖排文字及缺失字体提示。字号、标点、混合中西文和主观布局重点留意见下表。
4. 生成页使用明确标注的合成账户设计/继续修改并生成合成结果；记录页查看原始 Metadata、完整 Recipe 和两图差异。此 UI 暂不接入真实账户，不会消耗付费额度。
5. 分别导出原件、普通 PNG+Recipe、隐私 PNG。确认原件未变、导出另存；备份后关闭再开，检查图层/顺序；通过“备份与工作区”菜单恢复到新目录，保留旧工作区。
6. 关闭应用。集中反馈外观/交互问题和下列功能缺口是否阻碍下一步 RC；CP4 真正 A→B 安装更新仍需后续工程和独立证据。

This list collects human/uncertain observations, not automatic approval. Preview uses an isolated synthetic root. No long-term real-library claim or CP4 RC gate is made.

| Item | Reproducible review | Current evidence boundary |
|---|---|---|
| Overall layout and language | Start 0.1.0, resize to compact/full screen, compare dark contrast, Chinese labels and collapsible Prompt sections | Modern Desktop-style UI implemented; human preference NOT TESTED |
| IME / high DPI | Enter Chinese/Japanese, change Windows scaling, keyboard tab navigation | Actual device/human NOT TESTED until a recorded run |
| Vertical typography | Create TEXT layer with CJK, punctuation and mixed Latin; toggle vertical | Basic column layout only; typographic rules / mixed-font rich text remain review gaps |
| Missing font | Enter an unavailable font and attempt export | Automated preview warning and export refusal; user chooses replacement, no silent complete claim |
| Gesture choices | Drag rectangle/brush/lasso and inspect batch targets; undo/redo | Geometry regression does not sign subjective usability |
| Ordinary metadata export | Compare original raw file with rendered PNG+CCBStudioRecipe and privacy PNG | Original bytes preserved; ordinary rendering does not copy every arbitrary original metadata field |
| Privacy rendering | Inspect masks/text and compare fresh encoded output | Upstream synthetic chunk/LSB tests; no guarantee that mosaic makes visual content unrecognizable |
| Provider quality | Review authorized synthetic-cup real-call result separately from deterministic fixtures | One successful request is not complete multi-provider acceptance |
| UI account mode | Named synthetic profiles and frozen identity; DPAPI synthetic roundtrip separately tested | Real-account UI onboarding and full live Designer roundtrip are pending integration review |
| Recovery and backup | Open a fresh synthetic root, create project, backup, reopen | Asset and document checks are evidence; arbitrary power loss and full-app upgrade are not |
| CP4 | Real signed A→B discovery/download/installation/restart/migration | NOT TESTED; Preview is not first long-term release |

| Blue CCB brand | Inspect title bar/taskbar, native import/error dialogs and package icon | User-requested imagegen blue variant of fixed CCB logo; shape fidelity/small-size appearance await human review |
| Native screen automation | Capture and mouse geometry failed; do not repeat as acceptance | ENVIRONMENT_UNAVAILABLE; user supplied first actual window screenshot, app instrumentation reported separately |

| Native title bar and Snap | Drag blank area, double-click, use caption controls, resize edges and inspect Snap on a high-DPI monitor | Six geometry/command tests PASS; actual app native maximize/restore/minimize/restore PASS; mouse/Snap/human behavior NOT TESTED |

Automated selfcheck and restart passed with unchanged state/original hashes. No missing measurement is relabeled FAILED and no blocked safety issue is deferred as cosmetic.
