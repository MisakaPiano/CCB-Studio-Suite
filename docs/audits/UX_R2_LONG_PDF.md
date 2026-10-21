# UX-R2 长页PDF兼容补强（T20 / F整册输出）

起点 d9d965e4081bebe22d907a26b9ad1dfb129cfeed。保留现有小人书、原件与资产安全实现，仅修正PDF版本/页面表达并增加独立阅读器验证。PRODUCT_HOLD，非RC。

旧PDF 1.4直写坐标可能超过传统14400pt实现范围。首个PDF 1.6 UserUnit方案虽通过4项模块测试，但本机Poppler 26.07.0与PDFium 156.0.8076.0均忽略缩放，200×20539px长页变成100×10270px；该方案实际FAILED，不交付。失败PDF、hash和阅读器测量均保留。最终采用PDF 2.0直接1/72英寸坐标；不截断、缩小或分拆旁白。PDF 2.0取消页面尺寸上限的来源：[PDF Association](https://pdfa.org/how-big-can-your-pdf-page-be/)。阅读器仍有实现限制，UI明示超长页需要支持大页面的阅读器，不宣称所有旧阅读器兼容。

实际三轮Gradle：最初定向4 PASS（不能代表阅读器通过）；修正后定向4 PASS；最终core+desktop完整142 PASS，0失败/错误/跳过，185.767秒，sourceStable。JDK17.0.20.1、Gradle9.1，offline、strict依赖校验、单worker。所有原始运行在runs内。验证脚本初次误用PDFium对象context manager导致TypeError，已修复，失败JSON保留。

最终重新执行独立工具：pypdf6.10.0解析页数、尺寸和嵌入RGB原样；Poppler96dpi产生200×289及200×20539像素两页；PDFium禁用图像平滑后逐像素RGB完全相同。Poppler默认平滑导致RGB不完全相同，明确记录false，不能冒充像素相等。长页首尾裁片已人工查看，Row0001至Row1700及页脚保留。最终PDF SHA-256：01f736eff31b0fa6ea0ce5b56bf07086d785218463149e45c5a2c65494f166d2。既有3页整册PDF/ZIP另外通过pdf-lib1.17.1/JSZip3.10.1独立验证。

Python/Poppler/PDFium来自Codex现有依赖运行时，无新下载。全部输入人工合成；无真实API/图库/凭据。原生Windows阅读器、打印、真人排版质量、最大100000px极限未测试；两种已测阅读器不代替所有阅读器验收。安全应用内A→B仍未完成。

[原始证据与大小/hash](evidence/UX_R2_LONG_PDF/PACKAGE_FILES.json)；全部安全合成附件在Git，Drive NOT UPLOADED，无单独非Git附件。后续继续剩余排版/图库统一工作流与R4，未合并或发布。
