# CROSS PROJECT REGISTRY

本表记录候选/来源/状态，不能覆盖目标工程的CURRENT或替代其验收。

| ID | 内容 | 方向 | 当前 | 目标证据 |
|---|---|---|---|---|
| X-001 | 图像请求/Prompt Designer共享 | Desktop→Suite | CANDIDATE，A0已审D精确对象；未抽取/采用 | A0源码表；Desktop需独立窄模块审查/发布 |
| X-002 | 原生批量嵌字打码 | Suite→Desktop | 需求候选，未实现 | 独立通用模块/接口/验收 |
| X-003 | 多API档案和请求身份 | 共同合同、分别接入 | CANDIDATE，D-044已核验NOT IMPLEMENTED | A0 GAP-06/07/09；直连与管理员Server凭据分离 |
| X-004 | 安全应用内更新 | Suite→Desktop候选 | 未实现 | 安全链/平台证据；不改Desktop原门槛 |
| X-005 | 图库保全/恢复经验 | 双向 | 待实际证据 | 具体缺陷/修复/可复用边界 |
| X-006 | Android图像适配 | 官方CCB→Suite | CANDIDATE，O原生图像/凭据与U更新器选读；未采用 | A0源码表；真实Android/安装链NOT TESTED |

2026-10-21 A0依据：[精确来源与symbol](audits/A0_001_REUSE_AUDIT.md)、[差距与候选](audits/A0_001_DEPENDENCY_AND_GAPS.md)。本表只登记Suite候选，不修改Desktop CURRENT、排期、baseline或验收。

状态流：CANDIDATE → SOURCE_READY → TARGET_REVIEW → TARGET_IMPLEMENTED → TARGET_VALIDATED → ADOPTED。拒绝/延期另记理由，不自动升级。

每项后续补来源repo/commit/路径、目标任务、所有者、允许范围、契约变化、测试、采用版本。不能把参考文件存在视为已采纳。
