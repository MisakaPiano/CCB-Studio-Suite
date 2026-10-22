# 长期签名渠道安装包升级验证

2026-10-22，用户要求继续完整升级验证，并告知约20分钟后暂离。当前工作区 `H:\CCB-Studio-Suite`，分支 `feature/windows-rc-continuous`，开始 HEAD `1b326bf373377bfdfc3847a7c900fb322f526f0a`，index/worktree 干净。

## 已实际完成

安装配置新增受限 `metadataBranch`，允许既有 `update-metadata` 和独立 `long-term-metadata`；配置、传输和打包的端点必须完全匹配。缺省保留旧行为，拒绝跨渠道 URL 和任意分支。44项运行时回归实际通过，源码稳定，0失败/错误/跳过。实现提交 `bebc78c6f36f3c026c362b48a335cc5a7b28f0d8`。

复用源码未变且构建证据有效的 `3bea16b` 应用镜像，重新组装更新器和长期根。两份包 `0.0.0-preview.102` / `.103` 各167322788字节，547个清单文件。没有重跑 Gradle/桌面回归，不把原镜像来源误写成本轮新构建。安装器无 Authenticode。

- 102 SHA-256：`2ba6fe873fc0a44a7d498f85e12438f10e2bfe96b2f8d85a3694c0b45d516416`。
- 103 SHA-256：`cd354d0abfca8d76e58a3d7edd768468e7024eb3f2fea17412b0a291bce891fc`。
- 两份安装均在独立目录实际执行并完整核验。103重复安装返回17，原目录全部文件字节不变。
- 102通过已安装产品 StudioRestore 接口恢复既有合成备份到 `longterm-real-102-103/restored-workspace`，随后启动窗口供人工操作。没有将备份直接当作工作区，没有读真实用户图库。
- 本地准备 targets版本2、snapshot/timestamp版本3，根固定为长期根。原测试100/101渠道保持不变。

## 尚待完成

用户明确允许后，102/103已作为 prerelease 发布，附测试说明和版权记录；GitHub 文件digest/大小、元数据内容均回读核验。签名目标发布提交 `f8584ac37eadd0fc315b6a5dab9a3129204cc2c1`。实际102更新器通过公开HTTPS读到103候选，没有替换网络响应。源码仓库权限未变。

公开下载102包与已安装A包逐字节一致。102最初由本地同字节安装器安装，不能表述为“从下载副本重新安装”。103将由应用从真实GitHub下载。

有安装目标后的续签工作流 [39769472899](https://github.com/MisakaPiano/CCB-Studio-Suite-Releases/actions/runs/39769472899) 实际成功，提交 `9cf9c2197abd8598d09a40124581e9a9f94f4f74`。回读根与targets字节不变，snapshot/timestamp递增至4，完整签名/散列/版本/有效期链通过；自然定时触发尚未到时验证。

用户已确认完成安装重启并再次检查最新版本，截图明确显示正确的 `longterm-real-102-103/restored-workspace` 与 `0.0.0-preview.103`。交接 `64e0fd3b-febb-4eae-90ba-9f273dd31bd9` 为 COMPLETE / B_PROGRAM_DATA_AND_CREDENTIAL_SNAPSHOT_MATCH。

独立核验 A/B 各547文件（251092053字节）、B安装器散列、10条资产字节和整体状态快照一致；原合成备份未变，安装器没有修改资料。原本缺失的一项凭据仍为 MISSING_BEFORE_UPDATE，本次不证明真实可读凭据的保全。资料格式3→3，无新迁移测试。用户不再需要继续保留窗口或追加人工操作。

结论：本次长期根102→103真实GitHub应用内升级链路 PASS；自然定时触发、跨机器恢复及日常版完整产品验收仍待后续，PRODUCT_HOLD / NOT RC。证据见 [实际结果](evidence/UX_LONGTERM_UPGRADE/actual/ACTUAL_RESULT.json)。下一动作是整理首个日常使用版的剩余产品验收与发行条件，不能自动将本测试包改为正式版。
