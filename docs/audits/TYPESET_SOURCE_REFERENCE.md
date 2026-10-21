# TypeSet 1.1 源行为样本冻结

基于 Suite 63ca0065a1ba1027821d9979c7eaecb3d6b44963；2026-10-21。此检查点只有参考执行工具和证据，尚无 Studio 原生排版等效结论。连续进入 R2，不是产品验收停止点。

## 来源与执行差异

只读 HTML 200005 bytes，SHA-256 `892ea08f0070bdfd264b8895d38f6239f32f436b68a74ab42a2ef3336e28bb63`。原文件在隔离 Edge 131.0.2903.112 实际启动失败：DEFAULT_PREFS 在 STORYBOOK_PRESETS 初始化前引用它，触发 TDZ ReferenceError。[未修改原件的运行回执](evidence/TYPESET_REFERENCE/unmodified-startup.json)与截图保留。

`tools/Capture-TypeSetReference.cjs` 只在内存测试副本把 DEFAULT_PREFS 整块移动到 STORYBOOK_PRESETS 后；布局、绘制、换行、竖排和导出算法不改。另将源声明的 jsPDF 2.5.2 / JSZip 3.10.1 动态导入替换为固定本地 UMD，暴露原函数供重复输入。完整差异及修改副本 hash 见 manifest。不得将这次运行描述为未修改原件可直接启动。

隔离临时浏览器配置，1400×950、DPR1，页面网络请求全部阻断；不使用用户浏览器/账号。外部 Google Fonts 未加载。显式 FontFace CCBRef→本机 Microsoft YaHei、CCBRefSerif→SimSun，加载已确认；Windows 字体文件 hash 登记但不复制字体。粗体/斜体可能由浏览器合成，原生跨引擎比较必须披露。三款小人书样式的字体显式映射为此固定字体，不冒称已取得 Google 字体效果。

测试依赖从 jsdelivr 固定版本路径实际下载至 ignored `.local/typeset/deps`；首次受限网络连接失败，授权下载后成功。精确大小/hash 见 manifest；未引入产品依赖或产品 CDN。首次浏览器受限启动 EPERM 后使用隔离启动权限成功。未运行 Gradle、真实 API 或用户图库。

## 实际取得及核验

[冻结 manifest](evidence/TYPESET_REFERENCE/source-output/manifest.json) 包含16项：中文/日文竖排、横/竖混合富文本、三种框宽换行、长短/空白旁白与页码开关、三种样式应用、整册 PDF/ZIP。输入、逐字布局/几何、原源画布 PNG、源界面截图和导出均冻结；HTML、字体、依赖库不上传。

[独立容器核验](evidence/TYPESET_REFERENCE/source-output/CONTAINER_VERIFICATION.json) 实际 PASS：pdf-lib 解读3页，宽均900pt，页高810/810/1272pt，对应源1200×1080/1080/1696像素；ZIP CRC、3页顺序、PNG尺寸及与源单页PNG逐字节相同。工具不调用源渲染器推导期望。源 PDF 本身把页面栅格化，不是可选择文字的矢量 PDF。

保留验证过程的失败事实：最初独立脚本尝试 Python fitz，但环境没有该包，未安装；改用已有 pdf-lib。首次 ZIP 验证错误地把目录项计入3页断言而失败；修正验证器过滤目录项后通过，未改源输出。前一次较少样本的捕获仍留在 `.local/typeset/runs/2026-10-21T06-02-13-195Z`，本次冻结采用扩展后的06:04:26运行。

人工体验：NOT TESTED。已检查源长页视觉样张，空段、旁白、图像及编号可见；这不是用户审阅。Studio 原生实现比较：NOT TESTED，下一步以这些固定输出验证，不自行生成期望替代来源。已知源混合大字号可能越过以基础字号推导的框边界，须记录差异并避免导出裁切。保留原件与数据安全核心。
