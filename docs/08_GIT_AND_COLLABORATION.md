# 08 — Git、工作区与双向协作

## 独立仓库

拟议 `MisakaPiano/CCB-Studio-Suite`，独立普通仓库而非整体 Fork Desktop。可公开，不强制私有；“源码可见性”“私人部署”“运行时数据保密”分开。建项本身不创建或公开远程仓库。

本地 `H:\CCB-Studio-Suite` 与 `H:\ChatChatBar-Desktop` 平级。Studio任务不切换、reset、clean、stash或构建正在工作的Desktop目录。

## 分支

`main` 稳定集成；`feature/*` 功能；`integration/*` 联调；`sync/*` 范围明确的来源升级；`release/*` 发布准备。平台和运行模式通过模块/配置，不用长期android/private/network分支。

不同应用可独立发布，配套协议/数据兼容范围清楚。不因Server修复强制Windows同时发版。

## 只读参考

优先精确commit读取。需独立参考副本放在忽略的 `.reference-repos/`，记录大小和保留策略。不修改邻接工程；默认分支不代表Desktop主线，必须显式选 `desktop` 或精确SHA。

不把全部旧Project Sources复制进新项目；按来源地图读取。构建、参考、截图、发行包需保留规则，不无限堆积。

## 提交和授权

初始A0允许必要本地git init，但不自动commit/push/merge/创建远程/修改可见性。逐路径stage；参考资产、用户数据、私密Prompt、Token和签名材料排除。没有授权记录不得新增统一许可证覆盖混合来源。

现有Desktop授权不被本项目否定；新Suite所采用范围独立核对。不把仓库Public当作任意再分发许可，也不以Private替代许可核对。

## 反哺流程

候选 → 来源实现/测试 → 目标项目审查 → 目标实施 → 目标验证 → 精确采用版本。

登记见 [CROSS_PROJECT_REGISTRY](CROSS_PROJECT_REGISTRY.md)。Suite登记只陈述候选/来源事实，目标工程的CURRENT与验收仍由目标项目维护；不能一份跨项目表覆盖双方批准权。

一个任务一个主执行者。只读跨项目参考不等于可写两个仓库。并行任务不得改同一工作树同一文件。
