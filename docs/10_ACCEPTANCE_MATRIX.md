# 10 — 验收矩阵

**全部为待执行标准；文件包校验不等于产品验收。**

| ID | 必须验证 | 阻塞范围 | 当前 |
|---|---|---|---|
| DATA-01 | 导入/生成原图字节、哈希与来源保全 | 首版 | NOT TESTED |
| DATA-02 | 编辑保存、重启重开、批量状态恢复 | 首版 | NOT TESTED |
| DATA-03 | 写入中断、空间不足、索引与文件不一致 | 首版 | NOT TESTED |
| DATA-04 | 损坏/未知数据不被默认值覆盖 | 首版 | NOT TESTED |
| DATA-05 | 备份恢复成功及恢复失败保全 | 首版 | NOT TESTED |
| DATA-06 | 与Desktop数据根、锁和凭据隔离 | 首版 | NOT TESTED |
| GEN-01 | AI设计可应用、修改、撤回并追溯 | 首版 | NOT TESTED |
| GEN-02 | 最终发送参数与Recipe记录对应 | 首版 | NOT TESTED |
| GEN-03 | 实际平台/账户/模型显示正确且任务中冻结 | 首版 | NOT TESTED |
| GEN-04 | 失败/取消/429/结果不确定不产生无界重试 | 首版 | NOT TESTED |
| GEN-05 | 凭据不入metadata/日志/工程/普通导出 | 首版 | NOT TESTED |
| EDIT-01 | 文字/打码/变换不覆盖唯一原图 | 首版 | NOT TESTED |
| EDIT-02 | 多选/批量/排序与撤销边界 | 首版 | NOT TESTED |
| EDIT-03 | 横竖排、中日文输入、缩放与触控 | 对应平台 | NOT TESTED |
| EDIT-04 | 普通与隐私导出的元数据边界 | 首版 | NOT TESTED |
| UX-01 | 拖入、查看Prompt、比较完整Recipe | 首版 | NOT TESTED |
| UX-02 | 长Prompt、窄窗口、高DPI、键盘可达 | Windows首版 | NOT TESTED |
| UPD-01 | 内部A→B真实应用内更新 | 首版 | NOT TESTED |
| UPD-02 | 坏包/签名/渠道/产品/平台错误拒绝 | 首版 | NOT TESTED |
| UPD-03 | 取消/断网/空间不足保留当前数据 | 首版 | NOT TESTED |
| UPD-04 | 编辑/生成中更新安全交接 | 首版 | NOT TESTED |
| UPD-05 | 迁移中断与程序回退兼容性限制 | 首版 | NOT TESTED |
| AND-01 | 独立应用身份和签名下覆盖升级保留数据 | Android首版 | NOT TESTED |
| AND-02 | 后台/回收/分享/文件入口/离线编辑 | Android首版 | NOT TESTED |
| NET-01 | 重复提交和网络重试只形成一个逻辑任务 | Server首版 | NOT TESTED |
| NET-02 | 撤销与发送竞争，未发送任务确实不发送 | Server首版 | NOT TESTED |
| NET-03 | 重启不盲重发不确定付费请求 | Server首版 | NOT TESTED |
| NET-04 | 同用户跨设备与工程版本冲突 | 多端 | NOT TESTED |
| NET-05 | 管理员配置的上游凭据仅在 Server 内使用；授权 Windows/Android 客户端不提供 NAI Token 仍可提交 Recipe、取回原图/Metadata | Server首版 | NOT TESTED |
| NET-06 | 多用户共享管理员选择的上游执行账户时，任务归属与队列、限速、撤销互不串线；BYOK 不构成默认前置 | 多用户 | NOT TESTED |
| SEC-01 | A不能访问B任务/资产/工程 | 多用户 | NOT TESTED |
| SEC-02 | 上传预算、路径穿越、解压/解码防护 | Server首版 | NOT TESTED |
| SEC-03 | 用户/设备撤销后权限失效 | 多用户 | NOT TESTED |
| SEC-04 | 管理员与普通用户更新权限隔离 | Server首版 | NOT TESTED |
| SEC-05 | 客户端/其他用户无权读服务器管理员上游 Token；图像回传、任务查询及诊断不泄露凭据 | Server首版 | NOT TESTED |
| COMP-01 | 新旧client/server协议兼容或明确拒绝 | 多端 | NOT TESTED |
| REUSE-01 | 共享模块无环、来源与采用版本可追溯 | 所有阶段 | NOT TESTED |

证据至少记录源码SHA、执行环境、自动化/人工、合成/真实供应商、结果、原始证据入口和未覆盖项。

源码存在、测试代码存在、编译成功、测试执行、人工通过、集成、发布不能互相替代。保留FAILED、SKIPPED、NOT TESTED、UNKNOWN及旧失败。真实付费调用另行授权，不为“绿色”擅自请求。

里程碑以交付范围判断，不通过减少测试或静默删除需求来闭合。
