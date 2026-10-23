# Preview 106 性能与资源优化

2026-10-23，用户要求自动验证后通过既有渠道发布修复，Prompt设计保持PENDING；暂不安排人工操作。当前状态：Preview106已发布并接入既有签名渠道，首轮5项、最终52项实际自动回归PASS；105实际公开下载/新目录安装/106当前检查PASS。原生一键重启未测，不安排当前人工操作。

## 问题和改动

图库卡片原来调用与大图相同的DesktopOwnedImage：静态解码目标1600，APNG/GIF等会为每张卡片运行播放循环。Suite侧图库卡片、回收站列表和共用资产选择器现使用静态256px缩略图，队列共用此加载器；大图、对照和主动播放保持原实现。没有改动113份锁定的Desktop权威文件、Prompt或请求逻辑。

缩略图是可丢弃的视觉派生物：每个工作区独立8MiB解码像素LRU，最多两个解码，合并同ID并发请求；最后一个订阅离开则取消，错误不缓存。进入关闭/更新维护时取消并等待完成、清空缓存，更新前检失败恢复可再次加载。所有冷读取仍通过workspace.bytes原有校验，导出/复制/回执不使用缩略图。内存缓存命中不重新扫描原文件；不把显示缓存作为完整性证据。

8MiB约束仅针对缓存引用的像素，不是进程RSS或所有可见卡片的总占用。JPEG/PNG正常路径支持下采样；已有WebP后备解码可能临时解码原尺寸，之后保留栅格强制≤256px；没有重写解码器或宣称临时峰值全部解决。

## 实测边界

Temurin17.0.20.1，Windows报告Windows10，测试JVM限制2逻辑处理器/256MiB堆；24个合成PNG卡片ID，1536×2048，每图370985字节。相同内存字节输入、同进程串行测量，因此排除了磁盘导入/校验与完整界面延迟。旧基线仅调用原1600目标解码及ImageBitmap转换，未包含旧PNG再编码往返或动画循环，属于保守对照。

首轮旧冷1714ms/进程CPU2828ms/保留像素75497472字节，新冷270ms/CPU546ms/4718592字节；新暖十次访问<1ms计时分辨率、0新增读取/解码，不能解释为所有场景0耗时。未测进程RSS、私人图库、整机空闲CPU或人眼滚动手感，PERF002/003/007仍开放。

五项测试实际通过：相同图请求合并与LRU、并发上限/排水/失败重试、离开取消和后备栅格上限、APNG封面/GIF/WebP静态读取与原件保留、24卡片对照。首次新测试因JUnit要求void返回失败，修复显式Unit后通过；失败和通过的日志、源码散列均保留在[evidence](evidence/PERFORMANCE_106)。锁定113文件再次逐字节核验PASS。

后续：运行受影响图库/动画/资产/更新回归，冻结106安装器，发布后用105实际验证公开签名渠道发现、下载和新目录安装；自动回归不代替原生用户验收。

最终回归：源码e08429e7ed57bb822d4680ba0e552ff649fecaa8，52 tests、0 failures/errors/skipped，175.231秒，sourceStable=true；createDistributable实际完成。覆盖图库选择/目录/标签/排序/动画/导入、回收站、备份、收集取消排水、更新进度与恢复后凭据快照。最终同场景旧冷1353ms/CPU1640ms、新冷253ms/CPU484ms，保留像素仍72→4.5MiB，暖十次0额外读取，波动原样保留，不选择性覆盖首轮。

## 实际发行与更新

[Preview106](https://github.com/MisakaPiano/CCB-Studio-Suite/releases/tag/v0.0.0-preview.106)已发布。应用源码e08429e7ed57bb822d4680ba0e552ff649fecaa8，固定组装/标签839205b8499f9a5c68cd8ef96785efbb991ce7cb；安装器168206116字节，SHA256 `7bdf66de5dbf1da572fffbe3a3a9521fd5b565fb74e86239e0d58e0e6e44680f`。547随包文件新目录安装核验通过，同目录重复安装返回17且已有文件不变；许可从实际payload重新提取/核对，100条notice归档。

沿用原公开信任根408008d80aeb2dd3844325d74f09da9d248733e220dd88c08cc6299c4a24cc7a与既有本机受托管密钥；目标5/snapshot11/timestamp11，元数据提交7932f34c5b608f9b2b75b503c30dcfe050845109。先上传并核验GitHub公开安装器digest/bytes及说明许可，再更新签名目标，未覆盖105/tag、未改main、未改旧桥接渠道或Desktop。

使用已安装105程序的精确拷贝（仅程序，无用户资料），实际运行其随包Python及四个更新模块，公开网络发现106、完整下载校验、新目录安装及106 UP_TO_DATE全部PASS；247个合成资料文件、550旧安装文件不变。用户的独立安装 `H:/CCB-Studio-Suite-Preview`、原件、账户和密钥未被此次性能演练改动；现有固定入口仍105，之后从应用“更新”获取106，不另复制人工评审包。

原生UI演练：仅启动本轮新建的隔离合成105实例（24图，无账户、收集暂停）。computer-use两次捕获分别报 `FrameArrived timed out: timed out waiting on channel`、`window capture timed out: timed out waiting on channel`。按技能恢复规则重选窗口后重试一次，仍失败则停止UI操作；校验PID/精确exe路径后仅结束本轮合成进程，保留演练资料。未假称原生一键保存/重启通过，未要求离开电脑的用户操作。实际CLI安装通过不代签该UI验收。详见[原始发布/安装/公开更新回执](evidence/PERFORMANCE_106/release)。

本轮主要文件：`StaticThumbnails.kt`、`QueueThumbnails.kt`、`GalleryWorkspace.kt`、`LibraryArchiveWorkspace.kt`、`SharedAssetPicker.kt`、`StudioApplication.kt`与`StaticThumbnailTest.kt`；专项/任务记录同步。下一具体性能项：使用合成大库建立PERF003搜索/排序/分组的规模基线，并继续补PERF002/007实际启动和进程资源采样；Prompt新设计PENDING，当前原生更新交互未验单独保留。
