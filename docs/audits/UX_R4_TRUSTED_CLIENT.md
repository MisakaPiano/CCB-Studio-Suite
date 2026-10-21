# UX-R4 隔离运行时与应用更新客户端（C06，部分实施）

基线151c6ec8ebd6d01bfb3345c0accf04962f0a9232。仅完成信任客户端及界面检查/确认下载接线，**不是安装、A→B或RC通过**。PRODUCT_HOLD。

应用“更新”页使用独立进程调用随包Python/TUF；固定安装配置给出产品、平台、渠道、版本和bootstrap root哈希，缓存不能替代初始信任。python-tuf负责签名、元数据版本/过期/轮换及目标字节验证；Suite只检查已认证目标的产品/平台/渠道、版本、reader范围、源码SHA、大小和说明。进程级锁保护共享缓存；空间不足提前拒绝，下载验证并落盘后才发布VERIFIED目录，失败.incomplete保留。取消/关闭排水只终止本应用拥有的更新子进程，未操作资料库或安装程序。安装接续仍缺失，UI明确区分下载与安装。

Python3.13.16官方embeddable amd64包：SHA-256与[官方发布页](https://www.python.org/downloads/release/python-31316/)一致；python.exe Authenticode状态Valid，主体Python Software Foundation，证书指纹E875769F82782E3C919984CF747D21B943E0AEBE。无系统Python/PATH/信任变更。._pth隔离环境、注册表及当前目录导入，site关闭；7个锁定wheel保留完整license/dist-info，只有cffi换为cp313，其余6个复用已校验缓存。运行时约37.5MB；[完整来源锁](../../tools/update-runtime/artifacts.json)，hash和许可证在清单。官方ZIP及cffi为本轮新增下载，HTTP元数据流量未计量；首次受限网络失败后获自动权限审查通过再下载，不把失败称成功。

实际验证：原有8项TUF测试在新Python下首次8项/13errors（Python3.13 tempfile目录在受限令牌下WinError5），未改ACL，普通用户令牌下重跑8 PASS；新增客户端7项PASS，最终运行时再次7 PASS。包括同缓存真实子进程互斥、错身份/版本/reader、篡改、断线及空间不足。测试签名只在内存、无持久私钥，无网络监听。此层用合成内存传输，未验证真实HTTP下载。

桌面首次两轮编译失败均保留，修正import/测试语法后5项定向PASS；desktop完整60项PASS（100.009秒），核心未改未重跑。更新页最初测试宿主未设置Surface造成黑字黑背景，保留失败截图，改为与工作台一致的暗色Surface后3项定向PASS，最终截图已查看。GUI使用注入合成回执，证明确认/取消/反馈，不冒充UI→真实TUF进程→安装端到端通过。打包CLI缺配置返回FAILED/FileNotFoundError已观察。JDK17.0.20.1、Gradle9.1离线严格校验、串行，无新增JVM依赖或真实API。

NSIS3.13工具链已为下一步准备：从[官方分发页](https://sourceforge.net/projects/nsis/files/NSIS%203/3.13/nsis-3.13.zip/download)取得ZIP并比对公布SHA-256 ba63dffc4410ee89193e1cb5a41989991bd77c61068da17e3156d136b7b0b3d8；首次返回HTML不是ZIP，拒绝解压并保留本地失败文件，再沿页面真实链接获取。makensis /VERSION实际v3.13，工具未Authenticode签名；来源保证是官方HTTPS与公布hash，不虚称发行签名验证。许可包含zlib/libpng、bzip2、LZMA CPL1.0，后续包需保留相应notice。没有执行安装、改系统信任或生成正式私钥。

完整安装排水、安装/启动失败恢复、数据与DPAPI凭据保全、真实A→B及其他W1–W5缺口继续实施。所有原始失败与执行清单在[证据](evidence/UX_R4_TRUSTED_CLIENT/PACKAGE_FILES.json)。Git存安全日志/截图/来源清单；下载的公共运行时与编译器保留本地构建缓存，不将二进制缓存当交付包。Drive NOT UPLOADED，无虚构文件ID。
