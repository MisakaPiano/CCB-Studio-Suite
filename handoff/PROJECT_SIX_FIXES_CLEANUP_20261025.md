# 定向复审交接：六项缺陷与所属产物清理

请从原Project的“阶段复审结论”继续，独立判断六个问题是否关闭。**PRODUCT_HOLD / NOT RC保持；本次并非全项目验收。**

固定源码`da2ae810542c79b0049b74148566a423fb3e1bc3`；JVM构建输入`28d09f52ea8954f7f713744af892fc50f53f3887`，应用/原生启动器输入至最终源码逐文件散列相同。最终内部安装器168923897字节、SHA256 `8b33fd65aa0d4a08d0d2d1e6b6a1614ae1e35b166eba4d8e1224abf43d49deff`，未公开发布。115公开源码ff145d0、tag/安装器与原证据2489844均未改变。

先读[本轮实现、验证与限制](../docs/audits/REVIEW_FIXES_AND_CLEANUP_20261025.md)，再直接抽查[固定映射](../docs/audits/evidence/REVIEW_FIXES_CLEANUP_20261025/FIXED_SOURCE_PACKAGE_MAP.json)、[原始逐例结果索引](../docs/audits/evidence/REVIEW_FIXES_CLEANUP_20261025/RUN_INDEX.json)、[差异](https://github.com/MisakaPiano/CCB-Studio-Suite/compare/2489844984e2ad13f2f0c9d614702d37a7e1d615...da2ae810542c79b0049b74148566a423fb3e1bc3)和原始ZIP。38 JVM与49 Python分列，不与中间轮次/旧版累加。旧失败、用户否决以及115体验/Prompt对比PENDING继续保留。

六项：恢复副本不能抢默认入口；发送许可前暂停/结束原子检查；关闭/更新先禁新发送再收完在途；排队账户版本/secretRef冻结；Prompt目录采集中断幂等；手动累计与归档容量一致。真实A native PID与Windows image-bound句柄、生产排水调用、合成DPAPI凭据、TUF回环/NSIS/真实B回执、立即清理和窗口正常退出都有原始证据；注意A是独立测试main驱动生产路径，非原封115二进制或真人完整GUI更新。最终helper已COMPLETE，但驱动写CLEANUP_ASSERTIONS时因Windows共享占用FAILED，保留原状；只读事后8项断言及完整临时签名链核验PASS，不能混写为最终驱动无异常PASS。请独立判断这份证据的限定适用范围。

清理新增范围同时复审：成功确认前保留旧程序；COMPLETE后立即退役可信所属旧文件/下载，用户否决的7天保留已取消。未知/修改文件、原图、数据、凭据、插件、源码与备份保护；Windows句柄删除及长路径/联接/权限/占用/中断/重复用例。首次接管旧程序不按文件名删除无散列证明的卸载器。工作区新测试先留证再清理自己的临时根；[历史目录清单](../docs/audits/HISTORICAL_CLEANUP_REVIEW_20261025.md)只读，未授权删除。

请按“实现完成／自动测试通过／用户手动确认／独立审查接受／仍未验证”分别给出限定结论；若材料不足写UNKNOWN。不要代签115人工反馈、要求重做已有人工、取消Preview标记、改信任链/日常资料、调用真实服务或自动发版。新增独立意见请附具体源码/证据和下一步条件，保留原阶段结论。
