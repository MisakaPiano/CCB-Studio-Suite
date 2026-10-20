# 07 — 来源地图与精确读取

**日期锚点：2026-10-20。此表沿用上一轮立项会话的读取记录；本次打包没有重新查询 GitHub。** 当前 HEAD 可已前进，A0 用 live 查询核对必要增量。

| 来源 | 已记录版本/身份 | 用途与限制 |
|---|---|---|
| Desktop 已集成线 | `5ef95428421f7c693b010ac88f51d470804ab07a` (`desktop`) | 首次复用审计锚点，不等于已经成功抽取 |
| Desktop 声明 validated upstream | `5e76a9cb841736bbbf3499a2e35e5789af4c5ca8` / 1.4.1 | 官方语义参考；选择性图像移植另读 compatibility |
| Desktop 活动 feature | `c4098e08bec84372911a5a8bc38c2cfa72f98978` | 需求/开发参考，不整分支导入 |
| 官方 observed master | `979cb505958dffb1b6d64a8de657434f198ffa96` | 新变化观察，不自动升为依赖 |
| TypeSet Forge | 1.1 HTML、说明书、更新说明 | 本包原始字节及SHA-256已核对，功能未执行验收 |

活动 feature 相对更早的 `c280dca127ca246d03d301c5fa8af8a78b69fd20` 已有后续提交，这是上一轮记录的时间差，不自动扩大 Suite 采用范围。

## Desktop 首读

```text
docs/desktop/00_PROJECT_SOURCE_MAP.md
docs/desktop/10_UPSTREAM_BASELINE.json
docs/desktop/21_CURRENT_STATE.md
docs/desktop/13_FEATURE_PARITY.md
docs/desktop/14_UPSTREAM_COMPAT.md
docs/desktop/92_PHASE7_POST_MERGE_CLOSEOUT.md
```

按任务再读 `55_PHASE7_DEFERRED_OWNER_MAP.md`、`64_PHASE7_SCOPE_OWNER_UPDATE.md`。`146_MULTI_API_ACCOUNT_PROFILES_CONTRACT.md`/D-044 从活动 feature 的精确版本读取，它是未实施需求，不假设在旧稳定desktop存在。

## 官方定向入口

`.agents/skills/chatbar-image-generation-runtime/SKILL.md` 在已知基线可读；再按其真实入口读取 Prompt Designer、Studio Models/Repository、ImageService、metadata、guidance、history、Android平台适配与测试。其他Skill先确认存在，不臆造路径。

## TypeSet Forge

[参考说明](../references/README.md)和[原始文件清单](../references/REFERENCE_ASSET_MANIFEST.json)记录文件名映射、大小与SHA-256。HTML静态审计，不运行、不加载外部字体/脚本、不读取浏览器资料。

## 验证口径

本包哈希证明取得的文件字节身份，不证明它就是用户本机最后修改版本、不代表运行验收、不授予再分发许可。旧聊天观察、真实源码与正式采用状态严格分开。

所有来源采纳记录采用：来源 → 精确身份 → 功能 → 共享/适配/仅参考 → 差异 → 测试 → 后续同步责任。
