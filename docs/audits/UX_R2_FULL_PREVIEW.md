# UX-R2 完整预览与批量输出布局

接续8efeffc2f3aeab26e7d9554e13f7e6768508ef83，唯一Suite分支执行者，PRODUCT_HOLD。

完整预览独立查看最终页面，支持上一页/下一页、左右键和Esc；本地预览游标不改编辑页、选择、快照或导出状态。书册继续用同一动态页渲染。批量导出预览与设置分区，宽屏并排，窄屏上下；预览翻页与编辑页独立。底部复制/选位置/返回始终在固定操作栏，不被说明文字推走。

最初AlertDialog默认宽度使宽屏预览仅160px高，新增定向测试FAILED；解除平台默认宽度后测试通过，但实际截图发现图片越过标题。这一中间49项PASS只代表当时断言，不代表布局正确。原始日志及问题截图保留。最终改成明确标题/受限图像viewport/底部操作三区的Dialog，加入图像不越过标题/动作边界断言，并使用真正700×800 ImageComposeScene验证窄布局，不用只改constraints的截图冒充窗口缩小。

最终定向2项PASS含四分类17文件复制回归；最终desktop完整回归49项实际PASS，0FAILED/0SKIPPED，sourceStable=true，80.618秒。core未修改，74项PASS来自前一检查点，不冒称本轮重跑。截图实际Compose离屏，原生Windows/DPI/IME/真人审美仍NOT TESTED。

JDK17.0.20.1/Gradle9.1，严格离线、单worker/单fork，无新增依赖/下载/真实API或用户图库。完整图库、APNG产品流程、原生验证及安全应用内A→B更新继续，不能称RC。

[全部六次运行及原始失败](evidence/UX_R2_FULL_PREVIEW/runs/) · [最终完整预览](evidence/UX_R2_FULL_PREVIEW/full-preview.png) · [宽布局](evidence/UX_R2_FULL_PREVIEW/wide-export-deck.png) · [窄布局](evidence/UX_R2_FULL_PREVIEW/narrow-export-deck.png)。合成证据全部存Git，Drive NOT UPLOADED。
