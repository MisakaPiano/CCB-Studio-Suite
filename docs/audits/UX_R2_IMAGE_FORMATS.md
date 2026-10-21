# UX-R2 图片格式与静态编辑闭环

接续已推送并核验的d1cb386788bf233928e0c608d2ce5d1501738f81。PRODUCT_HOLD，继续完整恢复；不合并main、不发布。

## 实现与来源

导入保全PNG/JPEG/WebP/GIF/BMP原始字节。解码在分配栅格前检查32MiB输入和12,582,912像素预算。GIF检查逻辑画布与第一帧偏移，越界拒绝；WebP使用现有Studio已锁定的Skiko0.9.37.4解码器。旧PNG/JPEG坐标解释仍遵循前一检查点，没有自动迁移旧工程。

编辑输出可选PNG/JPEG/WebP/原格式，原格式跟随PNG/JPEG/WebP，其余按TypeSet来源getExportSpecForEntry行为转PNG。WebP质量95；JPEG白底质量95；原件分类无论选择何种编码均逐字节保全。源交换文件正确记录实际mime/扩展名。PNG来源元数据只从PNG提取，WebP/JPEG重新编码不宣称保全来源元数据。

GIF、动态WebP仅编辑首帧；APNG使用PNG默认静态图（可能是独立封面，不冒称一定是动画首帧）。界面与导出预览明确说明，原件分类保留全部动画字节；未实现时间轴/逐帧编辑，也不作这类声明。

核心新增直接Skiko依赖，仅复用桌面现有版本及Windows x64运行时；传递jbr-api1.5.0也已有缓存。离线严格verification运行，更新core编译/运行/测试锁，没有下载、没有更改verification hash或Desktop/upstream。其他平台运行时未测试，仍属后续平台任务。

## 实际验证

定向6项PASS（格式3+方向3）；新增合成双帧GIF/WebP/APNG后格式4项PASS。合成样本用Pillow生成纯红/纯蓝12×8两帧，无用户图片或外部参考原件。

Compose真实UI路径：WebP导入→命名编辑工程→添加文字→预览→选择WEBP→目录适配→实际编码/回执核验→保存关闭重开，1项实际PASS。首轮脚本错误查找“PNG”而实际控件为“图片格式 · PNG ▾”，1项FAILED保留，修正定位后通过；没有跳过步骤。

截图为离屏Compose实际画面，非原生窗口认证。实际系统文件选择器/剪贴板/真人视觉NOT TESTED；当前导出对话框仍较长，需整体交互整理，不因格式测试通过宣布W4完成。

完整模块回归：120项实际PASS（core73/desktop47），0FAILED/0SKIPPED，sourceStable=true，202.057秒。Temurin17.0.20.1、Gradle9.1，单worker/单fork，严格离线。真实API0次。

[全部运行含失败](evidence/UX_R2_IMAGE_FORMATS/runs/) · [实际Compose截图](evidence/UX_R2_IMAGE_FORMATS/webp-export-preview.png) · [文件身份](evidence/UX_R2_IMAGE_FORMATS/PACKAGE_FILES.json)。本次均为合成Git证据，Drive NOT UPLOADED。
