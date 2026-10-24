# 发布入口与 Project 阶段独立复审交接

2026-10-24 Asia/Tokyo，用户直接授权。本轮为文档任务，接续点 `feature/windows-rc-continuous@4c0f35deaba980cbc068b8ac3968bc730a952f8d`；开始时 index/worktree/untracked 均为空。

## 允许范围

- 只读核验两仓库的指令、默认分支、实际最新公开 Preview、首页 Releases 和历史 Project 审查。
- 主库 `MisakaPiano/CCB-Studio-Suite` 默认分支仅修改 README；旧 `MisakaPiano/CCB-Studio-Suite-Releases` 默认分支仅修改 README。用户已明确批准这两项文档提交及推送，取代历史“不得写 main”在本次文档范围的限制，不合并 feature 应用源码。
- 当前持续分支维护阶段交接、现有发布流程说明、最少必要状态及合成证据；允许文档提交/推送。
- 用只读公开元数据和本地既有安装器散列固定应用身份；不重新执行构建或创作服务，不访问日常用户资料/真实凭据。

## 明确排除

不发新版本、不改安装包、tag、版本号、发布状态、更新协议、密钥/签名/TUF 元数据；不新增自动化或触发现有续签；不改 Desktop/upstream、不发送其他聊天或 Project 消息。旧发布与桥接通道保留。用户正在试用115，不将“准备试用”写为通过。

## 交付与验收边界

追溯最后可核实的外部 Project 审查记录及其限定身份；保留失败/否决/未知项。实现、自测、用户确认、独立审查和未验证分列；交接不等于 Project 的新裁决。只打包白名单文本/合成运行日志，不包含真实资料、凭据、恢复码、签名私钥或整套工作区。

完成后核对公开 README/链接、远端提交差异及两库非文档分支/tag/release 不变。只报告实际结果，由原 Project 独立复审。
