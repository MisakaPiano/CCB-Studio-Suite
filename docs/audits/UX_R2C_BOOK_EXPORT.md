# UX-R2c 小人书与批量文件导出工程进度

接续远程核验 `76d33634868f5cbbadeb3ceabd7b6ae8b456dc89`，工作树/index干净、同一Suite执行者。PRODUCT_HOLD 保持；这是连续实施的工程检查点，不是完整R2c或RC交付。

## 本次接线

小人书传统/清爽/复古三个源预设，参数来自固定TypeSet 1.1 `STORYBOOK_PRESETS`。Windows字体明确映射为SimSun、Microsoft YaHei、FangSong；不下载网络字体。换预设保留旁白，下一页/Ctrl+Enter继续编辑，保存/重开保留。

编辑工程进入预览与导出，选择全部/所选/当前页、四种内容（文字与打码/仅文字/仅打码/原始字节）、PNG/JPEG、ZIP；小人书可输出整册PDF。按队列顺序/编号输出，每次选择目标位置后新建独立目录。界面显示实际完成路径或失败原因。原始文件不重新编码；渲染文件明确不携带原图元数据，JPEG透明区域填白。

每个输出文件记录大小/SHA-256，完成回执写入并force后原子发布目录；失败的`.incomplete`目录保留且验证器拒绝将其识别为完成。故障钩子覆盖首图前、已写一页、发布前；无覆写旧目录或删除原件。使用既有workspace operation及AssetStore，不重写数据核心。

PDF 1.4逐页输出RGB/Flate栅格图片，页面尺寸按源jsPDF的0.75pt/px换算，逐行压缩写磁盘，未新增Gradle依赖。PDF为栅格页面，文字不可搜索，和来源canvas导出路径一致。缺字体/无可显示字形阻断渲染导出；原始字节导出仍可用。系统中文/英文两套字体名称均核对。

## 验证与失败保全

首次核心2项PASS但桌面编译FAILED（缺少共享控件import和闭合括号）；修复后编译成功，编译不计测试PASS。实际Compose小人书编辑→切换预设→连续旁白→预览→请求选择目录→整册导出→重开1项PASS；目录选择器由测试注入受控目标，系统文件对话框交互NOT TESTED。截图为离屏真实Compose，不冒充原生窗口截图。

随后新增字体门禁，首次3项中1 PASS/2 FAILED：系统返回本地化字体家族名称，英文名误判缺失。修复为两套名称并集；失败原始XML/日志保留，后续结果另记。

运行器首次参数缺少`--`分隔导致argparse拒绝，未启动Gradle/未执行测试；不计PASS。之后所有运行串行、离线严格依赖校验、单worker/单fork；Temurin17.0.20.1、Gradle9.1。没有真实API、用户图库或Desktop构建。

## 仍需继续

源队列/预设交换、完整隐私封装/metadata策略、WebP与GIF/BMP接入、批量复制/保存交互及来源比较、图库完整恢复、真实应用内安全安装更新仍未完成。字体视觉与中文IME/原生对话框由后续工程验证和集中真人验收分别记录。本轮不降低W1–W5及CP4门槛，不发布RC。

最终结果：完整运行 `20261021T074831936106Z-UX-R2c-book-full-regression` 100项实际PASS（core56/desktop44），0失败/跳过、sourceStable=true、115.979秒；随后仅JPEG质量改为来源0.95，定向3项实际PASS，完整模块未重复运行。独立pdf-lib1.17.1/JSZip3.10.1读取最新产品输出：3页尺寸900×810、900×801、900×2715.75pt，ZIP顺序/CRC/PNG字节一致PASS。来源PDF为JPEG0.92栅格，本实现无损RGB Flate，编码不同但页面尺寸合同一致。

[全部7次运行与失败](evidence/UX_R2C_BOOK_EXPORT/runs/) · [交付文件身份](evidence/UX_R2C_BOOK_EXPORT/PACKAGE_FILES.json)。合成附件均可安全存Git；本检查点Drive NOT UPLOADED，不虚构双通道回执。
