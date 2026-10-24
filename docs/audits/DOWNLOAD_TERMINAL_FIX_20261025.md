# 下载引用终态分类：Project剩余P2定向修复

2026-10-25 Asia/Tokyo。用户要求读取原Project最新结论，有本轮范围内遗留就继续，先不发布。开始于授权Suite根、`feature/windows-rc-continuous@134dd47ae86a4ab5dbef579b61e19458dece2579`，index/worktree为空。没有扩大UI或Desktop开发，没有重做人工验收，也未追加历史目录删除。

## Project实际裁定

Project对2484858源码/134dd47交付限定关闭未知卸载器P1、helper默认入口、VIBE维护门、同ID策略保留快照四个残余，接受冻结旧凭据补证；原第2/5/6项沿用已确认范围。自动清理还剩下载引用终态分类P2，不能整体通过。见[审查原文](evidence/DOWNLOAD_TERMINAL_20261025/PROJECT_FINDINGS.md)。这不是全项目或公开115验收。

## 根因与最小改动

旧分类先看到COMPLETE、匹配nonce、verification标记就不再保留下载引用，随后遇到非默认程序根便提前跳过，未校验installed、installJournal、journal存在性与目标身份。FAILED_RETAINED也未绑定本plan nonce。因此另一根的不完整“终态”仍可能借已有成功记录释放同一下载。

固定实现源码：[f6598d39298c06ec5644a131e484e814f77b6f45](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/f6598d39298c06ec5644a131e484e814f77b6f45)。生产改动只有`tools/update-runtime/residue_cleanup.py`：

- 在程序根资格筛选之前验证整个cache中每个交接的终态；不完整或不可证明的引用先进入保留集合。
- COMPLETE须匹配plan nonce和verification，含绝对installed/journal路径；journal须位于本cache且可读，安装状态必须是INSTALLED_NOT_STARTED；安装目标、产品/平台/渠道、版本、源码身份与plan及新目录名一致，且有有效的manifest验证记录。
- FAILED_RETAINED只有nonce匹配才视为本交接的终态。不能证明的回执继续保护准确下载目录；非默认根也不能绕过此检查。
- 合法COMPLETE或同nonce失败释放其引用后，原有成功目标仍可立即清理；不恢复七天、额外冷启动或改更新状态。无需扫描另一资料根或读取其资料/凭据；已退役的历史程序可以通过保留的安装journal验证。

对比[最小源码差异](https://github.com/MisakaPiano/CCB-Studio-Suite/compare/134dd47ae86a4ab5dbef579b61e19458dece2579...f6598d39298c06ec5644a131e484e814f77b6f45)。另一改动文件为`test_residue_cleanup.py`：新增5个方法，并把原来两个省略安装状态、一个省略nonce的“合法终态”夹具补成实际helper一直产生的形态；新增相反用例明确验证缺字段必须保留，没有放松断言。

## 实际隔离验证

使用同一Windows环境和已验证的本地updater Python，真实Windows锁/句柄删除原语，仅合成程序和资料。`run-focused.py`以新OWNER隔离根运行，无真实服务、日常安装、签名或凭据访问。

旧实现先复现：5个新增方法中4个失败、1个合法终态对照通过；13个子场景断言失败，错误0。安装器实际字节与登记散列一致；资料哨兵保留。此FAILED及夹具保留，不改为PASS。

最终执行 **24个唯一清理用例PASS，失败/错误/跳过0**，包含5个新增方法：缺installed/journal、journal不存在、FAILED nonce错误或缺失、journal目标/身份/验证/状态异常、合法终态立即清理及重复执行。方法内部子场景另记，不累加成额外用例；首轮24项与此前18 JVM/56 Python均不加到本次24项。原有未知文件、占用、权限、路径/联接、中断及重复清理也在这24项中。

最终源码和运行输入散列稳定，24个资料哨兵一致；成功测试payload先外部留证再自动清理，636文件/277202字节，延后0。仅清自身本轮登记根，失败基线夹具保留，没有运行历史清理脚本。

实际命令：`<verified-updater-python> -B .local/review-terminal-20261025/run-focused.py final`。运行器副本、逐方法/子场景状态、原始日志、命令、环境及源码散列见[证据目录](evidence/DOWNLOAD_TERMINAL_20261025/SOURCE_AND_ARTIFACT_MAP.json)。运行器从Suite `.local/review-terminal-20261025`执行；证据中的历史脚本不是新的执行授权。

## 源码与包身份必须分开

**本轮没有重打包。现有内部EXE不包含此P2修复。**

- 新源码为f6598d3，应用modules与原生启动器输入未改；没有重复Gradle/JVM/原生更新演练。
- 旧内部包仍为248485824e1d6d2201ceedf7d14ff66e251d344d，168926729字节，SHA-256 `2dc31cb8badfe4ed5cb02c8a99523d81dc05f65a874aeb49ac3299048f701238`。本轮重新核对本地EXE未变，不能给它改贴新源码标签。
- 公开仍是Preview115/ff145d0，本轮无release/tag/版本号/元数据/信任链写入，也未更新日常安装。
- 三份旧固定RAW ZIP及原始失败字节保持，映射与保留散列见SOURCE_AND_ARTIFACT_MAP。上一轮清单计数更正为126条清单文件（含.gitattributes）＋manifest＝127个ZIP成员，不为计数重跑产品测试。

本次实现和定向测试完成，新P2独立复审仍PENDING；PRODUCT_HOLD / NOT RC保持。q09画布像素断言仍FAILED、根因UNKNOWN；115命名归档/组织树/任务栏体验、Prompt对比继续分别PENDING。未验证当前新清理器的实际打包/原生运行、原封115完整升级、真实服务、跨用户凭据或完整灾难矩阵；此前较窄原生与人工结论不扩张。

下一动作仅交回原Project复审此P2的终态分类和合成证据；接受之后是否打包或发布仍按用户后续授权，不自动发版。
