# 01 — 立项章程

资料日期：2026-10-20。性质：来自立项对话的需求与初始组织方案；不是生产实现或发布许可。

## 产品定位

CCB Studio Suite 是 Windows/Android 原生图像创作套件，包含 LLM 辅助设计/修改 Prompt、NovelAI 生图、可靠本地图库、完整生成参数和元数据管理、多 Prompt/多图对比、非破坏图片编辑、批量打码嵌字、本人 API 直连、私有服务器任务执行与多终端访问，以及从首个实际使用版本起的安全应用内更新。

它不是完整聊天应用，也不是删除 CCB Desktop 若干页面后的改名产品。主运行环境不依赖浏览器、WebView、localStorage 或 IndexedDB。

**服务器原始设计（用户确认）：** Studio Server 由部署者集中配置并保护上游生图 API 凭据；经过授权的 Windows/Android 等客户端发送完整生成 Recipe，不需要各自提供 NovelAI Token。服务器持久排队、集中调度与限制请求频率、允许撤销尚未发出的任务，并将原始结果及 Metadata 归属回传。可选 BYOK 是未来扩展，不得强制取代管理员集中执行模式。供应商服务条款和实际上线风险另行评估，不反向删除此产品能力。

## 项目关系

| 来源/项目 | 责任 |
|---|---|
| 官方 SaltyFishOTL/ChatChatBar | 被选定复用的官方行为、Prompt、请求及 Android 实现来源 |
| MisakaPiano/ChatChatBar-Desktop | 官方桌面移植、已验证共享核心、Windows Studio/平台经验 |
| TypeSet Forge 1.1 | 批量嵌字打码、队列、导出工作流及算法参考 |
| CCB Studio Suite | 独立产品体验、资产库、编辑工程、服务器、跨端协议及升级合同 |

权威按功能领域划分，不能把多个来源混成一个总上游。正确编辑参考是 TypeSet Forge，不是 CCB 制卡器；旧 CCB Editor Reference Pack 只有出现明确相关任务时才作为次级参考。

## 不改变的边界

本项目不自动改变 CCB Desktop 的 P8—P18、正式 upstream baseline、Prompt、数据合同或验收；不把未合入 feature 整条当作稳定依赖；不向官方 upstream 写入；不自动读取、迁移或清理真实图库/聊天数据；不自动创建公开仓库或发布。

## 用户价值与首要承诺

原图、编辑工程、Prompt 和已接收任务不是可随意清除的缓存。工程承诺是不静默覆盖、不以默认值替换损坏数据、不因升级或测试版而清档，失败保留原始内容，并建立可验证备份/恢复。

这不是对硬件损坏、用户手动删除或任何断电场景的绝对无损保证。未测试的耐久性边界必须披露。

## 明确的非目标

首版不复制完整聊天/WorldBook/Memory/RAG/Fish 产品，不自建 NovelAI 模型推理服务，不为了复用而整体重构 CCB 为 KMP，不在初始化阶段创建通用平台或大量无消费者抽象。
