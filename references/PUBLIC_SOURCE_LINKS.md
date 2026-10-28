## 2026-10-28：内置识图模型来源（本地实现，未发布）

- 固定权重与模型页：[01miku/anime-nsfw-segm-yolo26@1697d5d](https://huggingface.co/01miku/anime-nsfw-segm-yolo26/tree/1697d5d1827b6a818b350b44bf3ec27f08837a2a)。模型卡明确标注MIT，单独LICENSE文件未提供；原卡、声明和标准许可文本随资源保留，不发明上游版权行。
- `nsfw-anime-medium-x1280.onnx`，47,600,269字节，SHA256 `a12ac5532e93be9dfeb96a77fc3f3647791335c9df0de9a18fcd503f7877a828`，与固定LFS对象及本轮已有模型逐字节身份一致；无下载或采用新权重。
- 程序资源内置并默认加载，不按工程复制、不运行时联网找模型；自选模型仍沿原托管资产保存。模型声明只覆盖此权重，不把unified-image-tool仓库license UNKNOWN写成已取得其代码许可；旧记录保留历史时点。
- 这不是新发行或新模型准确率验收；安装器产物仍待后续固定候选核验。来源元数据读取不涉及私人图片／凭据，UI重整暂停。

# 可追溯来源入口

这些链接来自前一轮立项资料与已知精确源码路径。本次只文件整理，未重新联网验证链接内容或最新产品政策；A0使用时按任务核对。

## 精确Git身份

- Desktop集成审计锚点：https://github.com/MisakaPiano/ChatChatBar-Desktop/tree/5ef95428421f7c693b010ac88f51d470804ab07a
- Desktop活动需求参考：https://github.com/MisakaPiano/ChatChatBar-Desktop/tree/c4098e08bec84372911a5a8bc38c2cfa72f98978
- 官方已声明基线：https://github.com/SaltyFishOTL/ChatChatBar/tree/5e76a9cb841736bbbf3499a2e35e5789af4c5ca8
- 官方观察提交：https://github.com/SaltyFishOTL/ChatChatBar/commit/979cb505958dffb1b6d64a8de657434f198ffa96
- 官方图像Skill：https://github.com/SaltyFishOTL/ChatChatBar/blob/5e76a9cb841736bbbf3499a2e35e5789af4c5ca8/.agents/skills/chatbar-image-generation-runtime/SKILL.md
- 多账户需求合同：https://github.com/MisakaPiano/ChatChatBar-Desktop/blob/c4098e08bec84372911a5a8bc38c2cfa72f98978/docs/desktop/146_MULTI_API_ACCOUNT_PROFILES_CONTRACT.md

## 设计审查用官方文档

- Gradle Composite Builds：https://docs.gradle.org/current/userguide/composite_builds.html
- Android PackageInstaller：https://developer.android.com/reference/android/content/pm/PackageInstaller
- Android应用签名：https://developer.android.com/studio/publish/app-signing
- TUF安全模型：https://theupdateframework.io/docs/security/
- GitHub仓库授权说明：https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/licensing-a-repository
- NovelAI条款：https://novelai.net/terms
- NovelAI API文档入口：https://api.novelai.net/docs/
- Tailscale机器分享：https://tailscale.com/docs/features/sharing

外部文档是可变化来源，不能从这些入口推导固定“安全封号频率”、永远有效的API参数或已获服务器托管许可。

## 2026-10-27：unified-image-tool 图像处理参考

- 仓库：https://github.com/duxingz/unified-image-tool
- 固定参考提交：https://github.com/duxingz/unified-image-tool/tree/ed42f1f459dde621086f7763c36a46d768d091cb
- 本次核对默认分支main仍为该提交，最新发布：https://github.com/duxingz/unified-image-tool/releases/tag/v1.22.2
- 用途：本地ONNX识别、自动/手动打码、图像处理及交互参考；Studio当前为独立适配的检测框方案，非全部上游功能移植。
- 追踪表：[UPSTREAM_TRACKING.json](UPSTREAM_TRACKING.json)，关注识别、效果、图像管线、UI、模型说明、依赖和许可；变化须独立审查，不自动采用。
- GitHub当前license为null，代码/模型再分发许可未确认；用户本地选择的模型不随Studio公共包发布。

### 检查方式与维护边界

用户已确认沿Studio仓库的来源记录追踪。`docs/SOURCE_LOCK.json`固定采用参考身份；`UPSTREAM_TRACKING.json`保存观察基线，变化不能自动覆盖来源锁。`tools/Check-Upstream.py`只读公开GitHub元数据，比较默认分支提交、release及同名资产的size/digest、许可；相关文件差异仅供后续判断，不自动下载/运行/合并。无token读取，无新增git remote。

手动检查（仓库根目录，使用已配置Python 3.13）：

```powershell
& 'C:\Users\1\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe' -X utf8 tools/Check-Upstream.py
```

最新报告在`.local/upstream-monitor/latest.json`，最后成功观察在`state.json`，变化/新错误/恢复历史在`changes/`；未变化不增加历史记录。错误保留基线，不冒充无更新；相同错误不反复提醒。仅内核文件锁，不因旧PID锁阻塞后续检查；输出拒绝符号链接/目录联接。Python网络失败时用Windows现有HTTPS验证栈只读查询，不关闭证书校验或修改系统信任。首次URLError及随后RECOVERED报告均保留。

已建立本聊天的heartbeat **studio / Studio 上游变更检查**，每天09:00（Asia/Tokyo）；仅新变化或新的检查故障/恢复提醒，未变化安静。创建后已读回ACTIVE配置，**尚未观察首次定时触发**，不把手工执行当调度验收。固定参考ed42f1f、v1.22.2及license UNKNOWN保持。3个合成检查用例实际PASS，原始日志和真实公开检查位于`.local/editor-native-20261027/one-click-ai-full-review/upstream-evidence/`；不是上游功能、模型准确率或独立审查通过。
