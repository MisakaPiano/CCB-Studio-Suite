# UX-R2 图片方向与旧工程坐标兼容

接续固定 `66bda99309b9cc4c230498eeef60bf8b5f0314ea`，分支 feature/windows-rc-continuous，启动时本地/远程一致、index/worktree/untracked干净，同一Suite执行者。PRODUCT_HOLD，完整恢复继续。

页面新增显式coordinates：缺字段按raw-v1读取，保持旧工程JPEG原始栅格和既有文字/打码位置；新建/追加页面和无Studio扩展的TypeSet来源导入使用exif-v1。预览、导出、小人书及批次宽度读取同一解释。未知解释拒绝并保留文件，不猜测迁移。Studio交换扩展、编辑Recipe和快照包含该字段。原始文件导出不改变字节。

EXIF解析与像素映射沿用Suite已有DesktopImageEditing适配器的方法，未改只读Desktop或权威Prompt/request。只在显式exif-v1时读取方向；旧工程不因此前忽略的EXIF异常而失去原有读取能力。未引入依赖。

定向TypesetImageTest三项实际PASS：八种方向×两种字节序逐像素核对；旧序列化页保持原坐标、新建页编辑/保存/关闭重开及原件hash；未知坐标和异常方向拒绝。桌面模块编译成功不等同桌面测试。

完整回归20261021T083808277459Z-UX-R2-exif-full：115项实际PASS（core69、desktop46），0FAILED/0SKIPPED，sourceStable=true，160.774秒。Temurin17.0.20.1、Gradle9.1、离线严格验证、单worker/单fork，无下载、真实API或真实图库。原生窗口方向交互/真人视觉尚未测试；WebP/GIF/BMP、完整图库及真实应用内安全更新仍需继续，不宣布RC。

[两次实际运行与源码身份](evidence/UX_R2_IMAGE_COORDINATES/runs/) · [文件hash清单](evidence/UX_R2_IMAGE_COORDINATES/PACKAGE_FILES.json)。合成证据存Git；Drive NOT UPLOADED。
