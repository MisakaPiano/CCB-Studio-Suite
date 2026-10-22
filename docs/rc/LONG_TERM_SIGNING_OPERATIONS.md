# 长期签名、备份与续签

2026-10-22：正式四角色签名密钥、两条加密恢复路径和 GitHub 续签任务已部署。此处的签名用于应用更新元数据，不是 Windows Authenticode 证书。

## 保管与恢复

[私人 Google Drive 备份目录](https://drive.google.com/drive/folders/1CBc-ViywavAVMC1ZzuCfxI44BlKL1I4H) 已回读确认只有所有者权限。

- `CCB-Suite-signing-backup-v1.enc`：使用设置的备份密码恢复。
- `CCB-Suite-recovery-code-backup-v1.enc`：使用独立恢复码恢复，不需要备份密码。用户已保存并重新输入验证。

两份文件均为 AES-256-GCM 加密，Scrypt 派生密钥。密码或恢复码只需保留一种即可使用对应文件；两者均遗失且本机密钥也丢失时，不能从密文恢复。密码和恢复码未上传网盘、未进入源码或聊天。本机私钥及恢复码受当前 Windows 用户 DPAPI 保护，位于 `.local/signing-custody-v1`；直接复制 DPAPI 文件不等于跨机器备份。

需要恢复时，下载对应 `.enc` 文件，使用项目的 `tools/update-runtime/restore_signing_backup.py`，指定 `--backup` 和一个不存在的 `--destination` 目录。恢复码方式额外指定 `--recovery-code`。使用具备本项目锁定依赖的 Python，按本机隐藏提示输入密码或恢复码，不放入命令参数。成功后四把私钥在新 Windows 账号下重新 DPAPI 保护；随后按公开根散列核对身份。该入口不会覆盖已有目录。

验证范围：密码备份创建时已解密恢复；Drive 下载后与该文件逐字节一致。恢复码备份从 Drive 下载后已实际解密、验证四把签名密钥并恢复到独立目录。不同 Windows 机器及恢复 CLI 人工操作尚未测试。

## 自动续签

[续签工作流](https://github.com/MisakaPiano/CCB-Studio-Suite-Releases/actions/workflows/renew-long-term.yml) 每天 UTC 03:23（日本时间 12:23）运行，也可手动 Run workflow。首次手动触发已成功执行同一工作流，日历定时触发尚未到时验证。

GitHub Secrets 仅保存 snapshot/timestamp 两把在线密钥。root/targets 私钥留在本机和加密备份。在线任务不能签发新安装包，只能延长现有目录的 snapshot（14 天）和 timestamp（7 天），且验证固定根、所有签名、版本与文件散列后才以普通非强制提交发布。

根 SHA-256：`408008d80aeb2dd3844325d74f09da9d248733e220dd88c08cc6299c4a24cc7a`。

当前 targets 到期：2027-01-20 10:54:30 UTC；root 到期：2027-10-22 10:47:53 UTC。任一离线元数据剩余不足 7 天，在线续签会失败并要求离线维护。应在 2027-01-13 前安排 targets 续签。自动续签不等于永远无需维护，工作流成功记录也不保证 GitHub 将来的调度和通知送达。

## 当前发布边界

正式根放在独立 `long-term-metadata` 分支。2026-10-22 用户进一步授权102/103两个隔离升级测试包，targets现已指向103；这两份包固定长期根，但仍不是日常版。原一次性测试 `update-metadata` 分支保持不变。实际升级进展见 [102→103记录](../audits/UX_LONGTERM_UPGRADE.md)；不能把已丢弃旧测试私钥的100/101版直接当成正式信任链。产品仍为 PRODUCT_HOLD / NOT RC。
