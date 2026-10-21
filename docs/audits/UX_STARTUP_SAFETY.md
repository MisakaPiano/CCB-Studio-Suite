# 启动拒绝、迁移中断与真实安装故障

UX_RECOVERY_CONTINUOUS，feature/windows-rc-continuous，基线 de2a6d33b959c2c1a7001ad3a7d30b10be5c1ba3。PRODUCT_HOLD / NOT RC。

## 实际修复

- 不支持的工作区标记在创建 `.owner.lock` 前拒绝，锁内仍重复核验。旧安装 B 的合成根探针发现额外创建空锁文件，原有文件没有改变；保留这次 FAILED，不能将其算成目录完全不变。新核心/启动回归断言所有条目及文件散列完全不变。
- 启动失败提供原因与下一步，避免仅显示 IllegalArgumentException。未知格式、更新确认格式不符、已有迁移副本、取消、目录占用及待恢复状态分别提示；不把任意异常消息中的私人路径/请求直接展示。更新交接仍严格验证，不绕过原安全门禁。
- NSIS 在写入前检查最终文件及目录 UTF-16 路径长度；过长返回 19，不创建目标。旧安装包曾在 263 字符路径漏装一个依赖，安装后完整性检查拒绝并保留旧版本。这次真实失败保留。四个真实 NSIS 小载荷案例（边界成功、过长、含非 BMP 字符过长、已占用）实际 PASS；尚不能替代新完整应用包安装验证。

## 执行与边界

迁移子 JVM 在 SOURCE_LOCKED / MIGRATION_COPIED / MIGRATION_COMMITTED / MIGRATION_VALIDATED / MIGRATION_PUBLISHED 五个边界 Runtime.halt(73)。父进程确认原目录指纹不变、未发布目标可重新迁移、已发布目标不覆盖，原件保全，运行中请求重开转 UNCERTAIN 且不重发。首次测试编译因局部变量遮蔽 java 包失败，修正后定向 1 PASS、核心 126 PASS；随后根标记前置检查定向 13 PASS、核心+桌面 218 PASS。最新提示修正定向 3 PASS、桌面 92 PASS（222.144 秒、0 failures/errors/skipped、sourceStable=true）。无跳过测试；历史失败原样保存。

真实固定 A（251dc34950990b0878e1910d16ed289e9400db8d）/B（0ae42d9b5e6dec3fd796b28a8f92fca507cccbdc）的补充进程探针：

1. 实际 NSIS 拒绝已有目录，exit 17，哨兵文件未变，PASS。
2. 实际 A 保持打开，更新 helper 等待排水 120 秒后 FAILED_RETAINED，A 存活、根锁仍占用、资料指纹不变、未启动安装，PASS。测试结束仅清理自身启动的空闲 A 进程；不是 updater 强制关闭。
3. 长路径首次实际安装后校验 FAILED，B 未启动；测试预期 B_FAILED 文件而未找到，夹具也 FAILED。保留所有回执，没有将安装失败偷换为 B 启动失败。
4. 短隔离路径实际 NSIS 安装 B；夹具故意将合成根标记改为未来格式，并结束自身 A 作为故障注入。实际 B 拒绝启动（旧包显示 IllegalArgumentException），helper FAILED_RETAINED，旧 A 的 546 个文件、故障注入后的资料快照、旧入口哨兵不变，没有 B_VERIFIED 或迁移副本，PASS。用户截图中的错误窗对应此探针。哨兵不是可执行快捷方式；不据此声称旧快捷方式启动通过。夹具直接传交接计划，也不冒充 GUI 安装确认或正常排水验证。

JDK Temurin 17.0.20.1，Gradle 9.1.0 离线、严格依赖、单 worker；Python 3.13.16/TUF 7.0.1 为已核验本地 v7，NSIS 3.13 工具链逐文件与固定归档核对。依赖下载 0，真实 API 0，全部资料/凭据均为隔离合成夹具。未上传受保护凭据文件、原图库、测试密钥或安装 payload；仅白名单回执/日志、脚本与散列。

后续继续固定新安装包、原生窗口/备份恢复路径及 W1–W5 剩余门槛；本检查点不宣布 RC。精简原始证据 Git 交付；Drive 本次 NOT UPLOADED。
