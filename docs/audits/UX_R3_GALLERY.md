# UX-R3 图库与结构化两图比较检查点

基于7b2a7a232186798576bd6ef21372b1330aa7a60f，唯一Suite执行者，feature/windows-rc-continuous。本检查点为ENGINEERING_PARTIAL，PRODUCT_HOLD不变。

图库接入自适应缩略图、文件名/历史Prompt查询、来源/已有任务日期筛选、Ctrl/Shift选择、全选/全不选/反选及隐藏选择计数。导入旧图没有日期时明确显示未记录。两图比较固定A/B身份，显示原件SHA；锁定/独立缩放、平移、适应两图、完整结构字段搜索、只看差异、单字段/完整Recipe复制、长字段查看。JSON Pointer区分字段名里的斜杠与结构路径，missing/null/空字符串/数组顺序分别处理。关联请求按现有hash校验展开，不更改权威Prompt或请求语义。

参数选择入口进入既有权威metadata/历史复用确认流程，确认更新草稿并保存，原始Recipe及图片字节不变。不会把任意未知JSON字段直接拼到请求中。

## 实际验证

完整core76+desktop50共126项PASS，0失败/错误/跳过，sourceStable=true，175.409秒。随后截图检查发现搜索标签重叠，改用独立标题；补充解析前深度检查及50000字段上限，LIMIT_EXCEEDED区别于CORRUPT。最后定向core3+desktop1共4项实际PASS，68.410秒；不把之前126称作最终源码再次完整执行。两次编译失败原样保留；纯编译成功0测试记NOT TESTED。

真实Compose离屏GUI测试覆盖筛选→两图比较→真实鼠标滚轮锁定/独立缩放→复制字段（隔离剪贴板消费者）→参数复用确认→落盘/重开→原件与Recipe不变。无Recipe图仍显示并标MISSING。最终截图人工检查搜索文本不重叠。合成图片中的文字裁切来自原128×96测试图，非真人素材或视觉验收。

JDK Temurin17.0.20.1、Gradle9.1，严格离线、串行单worker与单测试fork；依赖沿用既有缓存，无新增依赖或下载，真实API调用0。资产安全核心、Desktop/upstream无修改。

## 未完成范围

R3尚未完成：导入PNG未知元数据完整字段展示、逐字段草稿应用、图库相册/筛选持久化、拖放/图片剪贴板、窄窗比较与A/B大图快捷键仍需补齐。原生Windows视觉/DPI/IME及真实提供者端到端NOT TESTED。完整安全应用内A→B更新仍是RC门槛，本检查点不是RC/产品通过。

[七次运行及失败原始证据](evidence/UX_R3_GALLERY/runs/) · [图库截图](evidence/UX_R3_GALLERY/filtered-grid.png) · [比较截图](evidence/UX_R3_GALLERY/compare-fields.png) · [大小/SHA-256清单](evidence/UX_R3_GALLERY/PACKAGE_FILES.json)。可安全公开的合成证据全部纳入Git；本检查点Drive NOT UPLOADED，无独立Drive附件或虚构文件ID。
