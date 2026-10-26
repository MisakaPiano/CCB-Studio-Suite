# 111程序退役与已确认工作区副本清理

2026-10-27，Asia/Tokyo。用户在核对111不再为当前运行／更新所需后，明确授权安全退役及清理本地工作区不再需要的旧文件。**COMPLETE_WITH_PRESERVED_BASELINES_AND_EVIDENCE**。本次为原UPD-CLEAN-PROTECTION-001／工作区产物生命周期任务的手工维护增量，不是新版本或自动清理规则变更。

执行源码基点为509da09e0940fcbe94ca4f72700df71e1eacac3e；日常仍为118，安装与更新代码a1202e24f7a0cba8a53f43bf035b67e2134ca2aa／映像3d0bd1f。没有改生产源码、包、tag、信任根或更新元数据，没有启动应用／真实服务／功能回归，也没有操作其他项目。

## 111：补证后删除，而非跳过保护

准确目标：`H:\CCB-Studio-Suite-App\111-1c9385afc38c`。

此前549个程序项已与保留的111构建清单一致，唯一未补证的是Uninstall.exe。此次先用保留的**原111安装器**在H盘新建的明确disposable隔离根真实NSIS解包，未启动应用。安装器SHA256为`d20ab5390227f9dbbf054e81a7b1f390fca6019eeba6b0b12578e1fc7e344a1b`；原111源码`1c9385afc38cfab8a7549cdb5287ccba3fdd4560`，manifest SHA256为`7cefba9ddcca008c507d63263639ddf3b441feb0d40a6beef2b5334946530437`。

新生成卸载器与旧目录的48921字节完全一致，SHA256为`3a5c12d831e9f7560125db89affcd499978efb54307827502db2d905a7dbed22`。因此整目录550项均有独立程序归属证明，枚举没有发现额外文件或重解析路径。未知文件并未因名称或旧版本号被直接删除。

在既有更新缓存锁与入口锁内，重新确认默认入口118、当前550程序项完整及保护记录未变后，仅迁移两处保护登记的**program字段**：

- `H:\CCB-Studio-Suite-App\ACTIVE_INSTALLATION.json`
- `H:\CCB-Studio-Suite\.local\ACTIVE_STUDIO_INSTALLATION.json`

两者从111指向118。其余所有字段逐项保持，包括`doNotCleanReferencedDirectories:true`、workspace、runtime和保留备份引用；没有删除整个记录或将保护标记设为false。原记录原字节保留于本机私有审计文件，不公开其中的备份路径。默认launch-current原字节未改。

随后使用既有`owned_files.delete_matching`逐文件核对大小／SHA256并经同句柄删除，目录只在为空时移除。结果：**111根已不存在，550文件／252504526字节退役**。当前118清单、默认入口及资料／备份保护字段复核一致；没有读取真实资料或凭据内容，不将字段保全写成私人资料全量内容审计。

这完成的是用户授权的本机历史记录迁移，不表示更新器新增了自动解除历史保护的功能。此前公开报告中“111仍保留”继续作为当时事实保留，不改写旧证据。

## 本地工作区：六份重复程序副本

三套已经完成主题检查的内部候选各有原构建payload和隔离安装副本。逐一核对保留安装器的BUILD_RECEIPT和SHA256，使用该固定安装器新建隔离NSIS输出，独立证明实际程序、INSTALL_COMPLETE和生成卸载器字节；未启动这些旧应用。仅在目标所有文件都匹配该输出且没有未知文件后退役。

| 准确目标（均在H:\CCB-Studio-Suite\.local内） | 文件 | 字节 |
| --- | ---: | ---: |
| appearance-gear-20261026\candidate\payload | 551 | 252983720 |
| appearance-gear-20261026\isolated-install | 553 | 253032721 |
| appearance-text-actions-20261026\candidate\payload | 551 | 252983864 |
| appearance-text-actions-20261026\isolated-install | 553 | 253032867 |
| token-candidate-20261026\candidate\payload | 551 | 252984155 |
| token-candidate-20261026\isolated-install | 553 | 253033154 |
| **合计** | **3312** | **1518050481** |

每套原安装器、BUILD_RECEIPT、旧源码／测试／失败证据继续保留；manifest／配置／公开测试根另存各candidate/release-inputs，PAYLOAD_RETIRED.json注明可从原固定安装器重建。保留人工操作profile／workspace／runtime，不假定旧现场内容全是垃圾。旧入口指向的已退役程序不能直接再启动；需重建隔离副本后复现，不能使用现存旧快捷方式冒充仍有安装。

此外，复用既有`workspace_artifacts`机制检查登记：109个历史disposable中87为PASS、15未封存、7为FAILED。仅处理仍有payload且已封存PASS、外部证据散列成立的5项，实际2项移除**56文件／24114字节**，另外3项为空目录收尾。没有把未封存或FAILED改成PASS后清理，也没有追溯登记任意旧目录。

## 结果、保留与失败记录

- 原有旧文件净删除 **3918项／1770579121字节，约1.65GiB**。未知文件删除0；没有递归清空H盘或回收站。
- 此次为归属补证新建的4份NSIS临时输出，另有2209文件／1011603268字节已按OWNER、独立证据及封存清单退役，**不计入旧文件净回收量**。
- 日常程序目录只剩`118-a1202e24f7a0`及独立`data`。118清单550项／252900763字节、manifest SHA256 `8af3972681326b95fa2faa97dfea07d66bb28c1e3b4a45e85ed937b65967021c`一致。
- 原107／114／115／116兼容基线、固定117／118包、复审证据、失败现场、未封存内容、用户资料／媒体／配置／凭据／插件／备份、源码、签名托管和共享依赖缓存保留。不能据此声称工作区所有历史内容均可删或已删尽。
- 初始准备错误地使用要求INSTALL_COMPLETE的已安装验证器检查未安装原始payload，安全拒绝，尚未清理任何目标。随后明确区分原始清单核对和真实NSIS输出证明，保留PREPARATION_BOUNDARY，不改产品状态或增加功能测试PASS。
- H盘可用空间只作观察，期间其他占用可能变化；回收量按逐文件删除清单计算，不以盘余量差推定全部是本次释放。

本次检查是文件归属、NSIS重建、精确迁移及事后程序完整性检查，不是新功能测试或新的Windows更新回归。JUnit0、无新构建源码／版本／发布。资产／编辑完整人工、115体验、Prompt对比、任务栏人工固定仍待验，q09 FAILED/root UNKNOWN、旧否决及独立审查状态保持。

## 可复核入口

原始清单与审计脚本：[RAW_MANIFEST](evidence/RETIRE111_WORKSPACE_CLEANUP_20261027/RAW_MANIFEST.json)。重点文件：

- UNINSTALL111_PROOF、EXACT111_RETIREMENT_PLAN、PROTECTION_MIGRATION、RETIRE111_RESULT：111独立归属及迁移／删除结果。
- WORKSPACE_PROGRAM_DUPLICATES_PLAN、PROGRAM_INVENTORY_*、PROGRAM_PROOF_*：六准确根、逐文件散列及固定安装器映射。
- EXISTING_PASS_DISPOSABLE_RESULTS、PROOF_FIXTURE_RETIREMENT、WORKSPACE_PROOF_RETIRED_*：复用生命周期及新临时输出的退役结果。
- POSTCHECK：最终当前程序／保留边界／净计数。

本机完整日志位于`H:\CCB-Studio-Suite\.local\retire111-workspace-20261027`；原保护记录仅存该处的`.private.json`，不进入公开证据。脚本含已退役路径，仅作审计材料，不是后续操作授权。后续按已有生命周期清理新成功产物；旧未封存／失败和归属不明内容仍需分别核对，不扩大本次工作范围。
