# 独立安装105的持续更新核验

2026-10-23，用户要求继续推进并确保刚安装版本可持续更新。工作分支5dd18b7起，当前独立安装105的应用/运行时不变；本轮只新增两项回归及维护记录，不新打包或发布空版本，不调用AI/NAI，不读用户key内容。

## 实际执行

- 直接调用`H:/CCB-Studio-Suite-Preview/105-a416a820955a/updater/python.exe`及其自带客户端，使用独立安装的现有更新缓存检查，返回UP_TO_DATE/105。没有使用源码运行时替代已安装组件。
- 实际部署工作流和维护脚本与版本化来源逐字节核对后触发现有续签任务：[run39945483509](https://github.com/MisakaPiano/CCB-Studio-Suite/actions/runs/39945483509)成功；targets4原字节不变，snapshot/timestamp由9前进到10，元数据提交7c3f255555517573aa1fb25aa721cb3d8f417003。根与105目标未变，main和旧渠道未改。[续签结果](evidence/UX_UPDATE_CONTINUITY/RENEWAL_RESULT.json)。
- 独立安装随后再次检查仍UP_TO_DATE，实际缓存已接受targets4/snapshot10/timestamp10。547程序文件仍匹配原安装清单；只更新元数据缓存，没有改工作区/凭据或其他运行配置。[汇总](evidence/UX_UPDATE_CONTINUITY/CONTINUITY_RESULT.json)。
- 以安装105中的四个实际模块为被测对象，最终45项回归执行通过，0失败/错误/跳过，22.793秒；包括签名/目标/版本校验、断线恢复、低空间拒绝、不可变安装器、并发、根轮换与续签。新增：独立根目录内data与程序分开安装；连续105/106/107合成版本共用固定快捷方式，目标更新而资料参数/字节保持不变。
- 前两次在受限执行身份下分别因系统/隔离TemporaryDirectory的私有ACL无法写入而出现5个子用例错误（2个维护测试），且首轮汇报还遇到默认GBK解码问题；失败日志保留，不改写为PASS。正常Windows用户权限、隔离临时目录和UTF-8日志复验通过。见[各次说明](evidence/UX_UPDATE_CONTINUITY/ATTEMPT_NOTES.json)及对应summary/tests.log。
- 第一次正常权限运行45项通过，但一个测试子进程未继承-B，在已安装运行环境内生成116个未列入清单的.pyc缓存。已仅移除这些生成文件（约1MiB），并为该测试的两个子进程命令补-B；最后再执行45项全部通过，随后严格安装核验547文件匹配，没有额外文件。正式客户端本身原已带-B，生产更新代码未改；[缓存清理记录](evidence/UX_UPDATE_CONTINUITY/TEST_CACHE_RESTORATION.json)与最终清单结果保留。

## 范围

未发布106，未启动真人105界面或做105→106原生重启；后者在下一份实际修复包发布时按既有合成资料流程补做。两次合成快捷方式换版不冒充实际二进制换版。上一轮真实104→105公开下载/新目录安装证据继续保留。

后续版本遵循[持续发布约束](../rc/PREVIEW_UPDATE_CONTINUITY.md)。自动续签不是永久保证：离线targets/root仍按期维护；固定快捷方式当前目标是活动版本权威，迁移时ACTIVE登记不能替代它。
