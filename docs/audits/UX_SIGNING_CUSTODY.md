# 长期签名托管与续签验证

日期：2026-10-22。基线 `14f32b32046206ff40843b9d214c24e33e151848`，分支 `feature/windows-rc-continuous`，Windows 10 19045 / Python 3.13.16。用户明确授权正式密钥、Google Drive 加密备份、独立恢复码和自动续签。

实施与保管说明见 [操作说明](../rc/LONG_TERM_SIGNING_OPERATIONS.md)。本轮无新安装包、无付费调用、无真实图库访问。

## 实际结果

- 43 项运行时测试实际 PASS，0 failure/error/skipped，12.670 秒。原始日志、逐文件源散列见 `evidence/UX_SIGNING_CUSTODY/summary.json` 和 `tests.log`。
- 密码备份和恢复码备份均 2352 字节。Drive 下载逐字节一致；恢复码下载件实际解密及四角色独立恢复通过。密码原件在创建时恢复通过，未再次请求用户输入密码。
- 密码文件 SHA-256：`bc3580c541d592ffdbaa02928c4487d20bfc538a22bf99cac5a189bd93dd0e17`。
- 恢复码文件 SHA-256：`49ec1179ea897844437343d27a707be6e462892b0aa077d321ac2533e92dba38`。
- 用户最终确认恢复码已保存并输入成功。密码、恢复码和私钥不作为提交证据；只保留公开结果。
- [GitHub 实际运行 39767014921](https://github.com/MisakaPiano/CCB-Studio-Suite-Releases/actions/runs/39767014921) 成功。工作流源码提交 `7069716381671f1cdb29f2049de3adfd8ffbafa8`。发布元数据提交 `5059fb2412eb9eecaed65b50190ccd0939966e2f`，回读签名/散列链通过，snapshot/timestamp 从 1 增至 2，targets/root 未变。
- 原测试渠道仍为 `f2ab4c5a5cecaf458e875d565062bfdf99b6dea4`；正式渠道无安装目标。

## 失败及未测项

初始密码交互 ValueError 后停止保留文件，改进重复输入提示。恢复码第一次启动因 PowerShell 嵌套引号丢失导致 SyntaxError，未执行密钥生成；改为直接执行脚本。两次恢复码不匹配后用户正确确认。初始测试沙箱临时目录权限失败，获准使用实际 Windows 用户运行后通过。首次 GitHub 工作流 YAML 第 24 行非法，失败运行 39766738685 保留；改为块式命令后重新运行成功。

跨机器恢复、自然定时触发、长期根引导安装包升级尚未测试。恢复 CLI 后加入，仅 AST 检查；所调用的加解密和保护恢复逻辑已在实际恢复与测试中运行，不把 CLI 人工交互标为 PASS。PRODUCT_HOLD / NOT RC。
