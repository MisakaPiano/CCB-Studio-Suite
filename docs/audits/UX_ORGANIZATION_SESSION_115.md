# Preview115：批次组织、当前手动会话和固定启动入口

2026-10-24，Preview115已发布到既有预发行和长期签名渠道；公开旧客户端升级及115当前检查已通过。用户已确认最新交互概念并要求实施，Prompt对比继续PENDING。

## 已接入的范围

新增持久化组织目录（Project/Chapter/普通文件夹）和批次元数据；手动、自动批次均显示创建时间，整批保存询问名称和上级组织。手动当前会话独立于整个资产库，重开保留；成功归档后清空当前栏，取消或失败不清空。逐张结果仍即时保存，不逐张弹命名框。旧来源未知的图不猜测补进新手动会话。

归档仅保存名称、关联与分组引用，不移动、复制或重命名原图。原始生成回执和Seed不改写。历史自动批次显式重新打开为暂停的新副本；保留已完成图的原引用，原始来源仍指向原批次。须先归档当前活动批次，不能绕过429冷却。组织调整有冲突检查的撤销/重做。

图库保留原有导航、导入/粘贴、筛选、标签、详情、八个工具及回收站入口。左树新增项目/章节、手动/自动来源分区，三角展开与名称导航分开，初始折叠；长树有可见滚动条。全部资产展开投影跟随当前相册折叠方式；把手常驻并保留拖宽。共用菜单13sp/32dp单行，长文字自然换行，保留禁用/焦点/悬浮。批次归档移至独立批次管理，去掉重复暂停命令，开始使用Play图标，运行设置直接打开表单。

固定启动器 `CCBStudioLauncher.exe` 放在安装根，读取原子提交的 `launch-current.json`。更新验证完成后才切换；较旧程序不能覆盖较新入口。桌面与开始菜单快捷方式都指向固定启动器；窗口和快捷方式使用 `MisakaPiano.CCBStudioSuite.Preview`，并设置重新启动命令和图标。更新页提供创建入口命令。旧的版本目录固定图标需用户取消并重新固定一次；不修改Windows固定列表。参考 [Microsoft AppUserModelID](https://learn.microsoft.com/en-us/windows/win32/shell/appids) 和 [RelaunchIconResource](https://learn.microsoft.com/en-us/windows/win32/properties/props-system-appusermodel-relaunchiconresource)。

启动器由本机.NET Framework编译器从本仓库C#源码构建，随安装清单验证；无需给用户另装Java/Python。命令参数正确保留Unicode和空格，拒绝被改动的目标程序、重解析路径及覆盖无关快捷方式。同一稳定入口保留图标和应用标识；实际用户固定效果仍需在Windows界面验收。

## 实际验证与失败记录

- 早先15项共用菜单/把手检查已执行，但不作为本次新功能完成证据。
- `20261024T115635536423Z-organization-session-compile`：因发现另一Desktop构建运行而主动停止本轮已核实Suite进程，0项执行，FAILED，不归因为代码通过。
- `20261024T120336495036Z-organization-session-first-regression`：实际160项核心检查通过，整体FAILED，新增界面测试有语法错误，桌面测试未执行。命令中的过滤只应用到最后任务，导致核心范围过大；后续已分任务缩小。仅清理这一轮创建的179个UUID合成资料目录，回收266970095字节，详见`.local/organization-session-first-fixture-cleanup.json`。
- `20261024T121338755714Z-organization-session-targeted`：16项实际执行PASS，失败/错误/跳过0，源码稳定，覆盖命名归档、重开/来源、历史副本、429、实际图库和会话界面。
- `20261024T122005021341Z-organization-session-ui-polish`：9项实际执行，8通过/1失败；真实归档表单与组织树通过，更新界面因追加可选回调改变尾随lambda绑定失败，已修正参数顺序并补两入口事件检查，后续复验待记。
- C#入口首轮脚本编码/编译路径、回执读取编码、已有快捷方式只读COM对象问题均保留失败过程，已修正。最新`.local/windows-rc/entrypoint-115-273d3fee046b4e6c992be8a15d3e9474/RESULT.json` PASS：同一启动器选择114/115合成目标、中文/空格资料路径、快捷方式更新、拒绝无关文件、真实隐藏测试窗口AppID及重启命令、目标篡改拒绝；不创建日常桌面/开始菜单文件，不代签实际任务栏固定。

真实生成0；旧共享权威源码不改，日常安装/资料不自动切换。本节为中途记录；最终包/签名/公开升级证据见下方，真实日常用户更新和新样式人工验收仍待反馈。

## 资源边界

H盘可用空间从约430MiB降至约32MiB，不能安全进行打包/安装演练。四份仓库内退役110–113合成程序已逐文件核验且无使用进程，精确清理计划`.local/retired-110-113-cleanup-plan.json`（1010138350字节）已展示给用户，用户明确回复“删除这四份旧测试程序”；再次核验文件清单、散列、路径及无使用进程后已删除，释放1010138350字节，H盘可用1052037120字节。结果`.local/retired-110-113-cleanup-result.json`。保留107验证基线、114及全部发行证据，排除日常`H:/CCB-Studio-Suite-App`。现有资源规则要求精确授权，不能擅自删其他程序、缓存或用户资料。

更新界面回调兼容修正：20261024T123112296523Z-organization-session-update-repair，6项实际执行PASS/0失败/0跳过，sourceStable=true。创建桌面/开始菜单按钮触发各自入口事件，未触发安装。

内嵌组织流程首轮运行中断且无完整结果，记INCOMPLETE；重跑20261024T125115578207Z-organization-session-inline-retry实际9项，8通过1失败，新增测试选择了底层Prompt输入框，未选择组织名称输入框，已修正定位。新增独立窗口归档弹窗归属；安装/交接Python13项实际PASS，RUNTIME_TESTS.json，非真人更新。

20261024T125409576127Z-organization-session-dialogs因新增测试局部函数语法错误未执行测试（FAILED/0）；修正后20261024T125623375390Z-organization-session-dialogs-repair实际3项PASS，sourceStable=true：内嵌新建组织、取消保留名称、默认选中新章节、归档清空、来源不变、组织撤销/重做、独立窗口历史弹窗归属及批次运行设置。实际1320×880归档/图库树、960批次截图目视检查；新的运行设置无重复暂停入口，保存后的时间戳保留。

首次冻结1de9b3e回归20261024T125822835355Z-organization-session-115-final-regression实际46项：45通过/1失败/0错误/0跳过。旧连续生图测试仍定位已被三段生成方式替代的Switch，未找到控件，在发送请求前失败；改为点击实际“连续生成”入口，保留原有分次/停止/残片保存全部断言。该次未创建发行映像，不用于发布通过凭证。

## 固定发行与最后公开验证

应用与组装源码 `ff145d051bae286ab3e27cadd41195f8bd4c4e18`（实现1de9b3e，追加旧测试定位修正ff145d0）。最终冻结 `20261024T130159887808Z-organization-session-115-final-regression`：46项实际执行，0失败/错误/跳过，sourceStable=true；实际createDistributable成功。另有13项安装/交接Python检查通过，原生C#合成目标与窗口属性验证通过。

安装包168890283字节，SHA-256 `356e749bd346af4a1d419a0d8b48b1e27d2314e6fce5763fa0933917247862dc`；551个实际安装文件全部核验，重复安装返回17且文件散列不变。真实115原生进程在首帧后签出匹配107合成资料快照的B_VERIFIED；预发布启动探针没有运行完整父进程排水/安装helper，不代签完整用户更新。关闭仅限该探针，CloseMainWindow未提供可关闭窗口，随后精确身份核验终止该合成进程，不能称正常关闭通过。

[115预发行](https://github.com/MisakaPiano/CCB-Studio-Suite/releases/tag/v0.0.0-preview.115)公开4个发行文件，服务端资产散列/字节数均一致；100份许可证材料。长期元数据提交 `60c527f55ced1f8a8e897ce957b49a66c9f83f53`，targets14/snapshot21/timestamp21；原信任根SHA-256 `408008d80aeb2dd3844325d74f09da9d248733e220dd88c08cc6299c4a24cc7a`不变，main保持36abfd8c11d0360a443e33e6ead84b78685b7541。刚发布后普通公开检查仍返回114，保留首次结果；尚不能记为115公开客户端检查通过。

本轮两份新临时副本的精确计划`.local/windows-rc/organization-session-115/TEMPORARY_CLEANUP_PLAN.json`，共505742629字节，逐文件与本次发行清单匹配，无使用进程。用户已回复“核验完成后删除这两份临时副本”；待公开核验完毕才执行，不删除115安装包、107/114基线及证据，不涉及日常App。此批准与先前110–113四份旧测试程序清理分别记录。

持久化简要证据见 `docs/audits/evidence/ORGANIZATION_SESSION_115/`。原始截图在 `modules/studio-desktop/build/organization-115/`；真实服务及任务栏人工固定效果仍NOT TESTED，新样式等待用户反馈。

## 最终结果

普通公开地址最初短暂返回114；等待缓存同步后原107模块实际发现、完整下载并验证115，未改下载地址或绕过信任。原107 helper完成新目录安装，真实115程序首帧后返回匹配B回执；115安装运行时执行UP_TO_DATE/115检查通过。探针使用的107和115 CPython二进制/标准库散列完全相同，加载的是原107客户端与helper模块。父进程已退出/排水为明确替身，不能代签真人完整更新。10份原合成资料和550个旧程序文件散列不变。

新版在旧helper COMPLETE后写入固定启动器和选择记录，实际快捷方式目标为固定CCBStudioLauncher.exe，AppID匹配；再次解析确实选择115和本轮资料/runtime路径。实际Java测试进程MainWindowHandle未观察到可读主窗，不把它算作任务栏真人固定或真实窗口属性验收；先前隐藏测试窗口的原生属性检查通过。

公开测试进程已在匹配进程路径/合成资料参数后关闭，非正常退出验收。两份用户精确批准的新临时副本在最终PASS之后再次检查全部文件/散列/无使用进程后删除，共505742629字节。源清单和update-config另存candidate/retained-payload-metadata；安装器及107、114、公开验证后的115基线、全部资料证据保留。H盘最终可用776531968字节，超过本次下载保护阈值573779713字节；日常App不自动切换。

本轮源码31文件（生产适配、核心模型、测试、打包及记录）；主要入口GenerationOrganization.kt、GenerationManagement.kt、GalleryOrganization.kt、DesktopEntrypoints.kt、tools/windows-launcher/Launcher.cs。最终操作为沿已授权分支提交实现/证据并推送；main、Desktop/upstream未改。真实生成0，Prompt对比继续PENDING。
