# 启动诊断Project限定接受与整包归档边界

2026-10-25 Asia/Tokyo，已直接读取[原Project最终消息](https://chatgpt.com/c/6adcaaef-3fac-83e8-ae57-2a524281e049?messageId=dace7591-239a-4eb0-982d-e918bc23c6c0)完整正文及结尾。会话工具确认消息身份/已完成状态，但正文为content-reference占位；原对话可见页面补足全文，已无等待完整答复的连接提示。本文件记录Project裁定，不把Codex自检当独立验收。

**固定源码8a757a6121c219dd942358d25c7a3707bc1fa8db、报告/证据31d38532b48e7a94e182f14a90bfbc955d44095b：本轮限定独立复审PASS_WITH_SCOPE。**没有要求修改生产代码的确定缺陷，无需重做已接受旧基线。此轮只收尾审查/归档，不接续任何新细分测量或开发。

|层次|Project最终状态|
|---|---|
|测量探针与阶段定义|PASS_WITH_SCOPE|
|固定源码与关键原始证据|PASS_WITH_SCOPE|
|两次原生启动诊断执行|各自固定测试宿主PASS|
|本轮独立复审|PASS_WITH_SCOPE|
|确定产品瓶颈/优化效果|UNKNOWN / 未建立|
|用户人工、安装包集成|NOT TESTED|
|PERF-002/007整项完成|未关闭|
|完整ZIP独立保全核验|NOT VERIFIED — TOOL_ACCESS_LIMIT；归档补充后仍未完成|

## 必须保留的限定

- **阶段窗口不覆盖整个录制**：Project复算还有44条NativeMethodSample、169条ClassLoad位于明确分组窗口之外。本轮又只从既有ANALYSIS复算该差数，未启动JVM或新测量。WToolkit.eventLoop可能等待，不等于忙CPU；控制器5条SHA2样本不证明校验冗余，不可据此取消校验。
- 4347/3588ms为不同模式、各1个新JVM测试宿主的就绪条件，不是提速对照。组合提交、内容绘制、showing、DWM是不同边界；CPU/类加载/JIT可并行/嵌套，不能强行加总。Window/图形区间只是后续候选，不是单一产品根因。
- Project独立取得原始JFR及gzip并核对SHA，另12份关键文本/JSON的字节和散列吻合。五类敏感事件0；JFR内建CheckPoint/Metadata不属于额外启用的应用采样类别。
- 五份相关源码身份获核对，已有Gradle文本CRLF/Git LF的表示区别保留；466个工程输入与1728个运行类路径文件是交付记录，Project未从用户Windows盘重新逐项获取全部二进制。
- 首次Python导入失败、源码断言失败、Desktop繁忙推迟保留原状态，最后2次成功不覆盖它们。48资产ID但1个PNG SHA、编译0 JUnit、setup不计测量、原件/禁止发送/关闭/退出检查及失败夹具均保留原范围。
- 完整Main/包、OS冷缓存、DWM实际呈现、物理IO、长期资源/真实图库及用户体验未验。原P2/PERF-003/旧原生基线接受保持其原范围；q09 FAILED/根因UNKNOWN、资产/编辑USER_NOT_TESTED、115体验/Prompt对比PENDING、PRODUCT_HOLD / NOT RC保持。

## 原始ZIP的访问核对与归档补充

Project最终裁定明确：受当前GitHub文件接口大小上限限制，**没有独立重算整个1,233,904字节ZIP，也未独立逐项解压全56成员**。已独立读到的关键材料足以支撑工程限定接受；整包未签收不是本轮要求重跑诊断或改生产的缺陷。不能把交付侧CRC/SHA声明转写成Project已做整包检查。

本次在相同已批准材料/相同原Project范围内，核对原包后只补一次归档访问说明，没有新载荷、重新压缩、Library上传或权限变化：

- 固定commit：`31d38532b48e7a94e182f14a90bfbc955d44095b`。
- 路径：`docs/audits/evidence/STARTUP_DIAG_20261025/RAW_EVIDENCE.zip`。
- [官方原字节直链](https://raw.githubusercontent.com/MisakaPiano/CCB-Studio-Suite/31d38532b48e7a94e182f14a90bfbc955d44095b/docs/audits/evidence/STARTUP_DIAG_20261025/RAW_EVIDENCE.zip)；[官方仓库Download raw对象页](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/31d38532b48e7a94e182f14a90bfbc955d44095b/docs/audits/evidence/STARTUP_DIAG_20261025/RAW_EVIDENCE.zip)。后者是页面，不能把HTML当ZIP；应直接取得前者二进制，避开受限文件摘要接口。
- Git blob SHA-1：`2932921b12d4699b72b6a466052059d692af41c1`；大小1,233,904字节；SHA-256 `a5eb28eb597763a306e558e006270ab9e8b58c3e6aa66929af1bbdba4a7fb99f`。
- **交付侧实际公开读取：PASS。**UTC2026-10-24 20:39:20.087860–20:39:22.809724，只1次GET，无凭据，HTTP200/application/zip/Content-Length1233904。收到字节与本机原包、固定Git对象完全相同；CRC通过，55项manifest清单+manifest共56成员，逐成员大小/SHA及Git原字节吻合。见[完整执行记录](../../handoff/evidence/STARTUP_DIAG_PROJECT_REVIEW_20261025/PUBLIC_ARCHIVE_CHECK.json)。此读取是归档保全核对，不是新诊断或产品测试。

原工程交审请求只发送1次；本次[归档补充载荷](../../handoff/evidence/STARTUP_DIAG_PROJECT_REVIEW_20261025/ARCHIVE_REQUEST.json)另明确只作一次可读性/归档核验，不重开工程审查。补充送达与Project是否独立取得整包，见[实时交审/归档回执](../../handoff/STARTUP_DIAG_PROJECT_REVIEW_DISPATCH_20261025.md)，不从交付侧公开下载成功推断对方工具已可读。

### 单次归档补充的实际回复

补充请求`7e227615-ea7d-4e6e-a0b6-b39c0108f0b2`于UTC2026-10-24 20:40:26.796落地，1633字符由会话工具全文回读一致，没有重发。[Project归档回复](https://chatgpt.com/c/6adcaaef-3fac-83e8-ae57-2a524281e049?messageId=4647f364-283a-4a15-8e5b-b28da1823e2c)已通过原页面完整读到，工具正文仍是占位；[结构化回执](../../handoff/evidence/STARTUP_DIAG_PROJECT_REVIEW_20261025/ARCHIVE_RESULT.json)分列结果。

Project报告直接下载失败，当前执行容器不能解析raw.githubusercontent.com域名。因此完整字节、整包SHA-256/Git blob SHA-1及全部56成员均NOT VERIFIED，归档状态**NOT VERIFIED — TOOL_ACCESS_LIMIT**；交付侧整包记录只作REPORTED_PASS。Project明确没有证据说明GitHub文件损坏、链接失效或交付错误；先前JFR/gzip/关键文本/源码的独立核验与工程PASS_WITH_SCOPE不失效，不要求重开发或重跑。

本轮登记/归档补充已完成，停止重试和新交审。若以后要求完整整包独立签收，尚需决定一种对Project环境可访问的原字节读取/传输方式；涉及Library上传或改变共享须先明确批准，当前未执行。工程限定接受保持；下一项诊断/优化方向与启动时机仍须后续明确决定。本轮停在审查收尾检查点，不发版、不动日常安装/资料/凭据或暂停扫描，不付费调用、不改信任链/生产代码，不启动Desktop。
