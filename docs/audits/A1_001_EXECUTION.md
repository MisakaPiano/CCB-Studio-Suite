# A1-001 — 限定数据切片实施与验证回执

2026-10-21 JST。A0-002=`PASS_WITH_SCOPE`由本轮用户明确提供；未另行读取外部Project对话。限定实施授权不含commit/push/merge/release、真实API、真实数据、A1-002或A2。

## 起点与实际范围

根`H:\CCB-Studio-Suite`；审查与实查HEAD均`6d3d2b22651f9744807925f74c6941f681bbef02`。开始main、index和工作树（含普通untracked）干净；忽略的A0参考及本地辅助文件保留。按授权建立`feature/a1-001-asset-durability`，在该分支实现，未提交HEAD仍为6d3d2b2。没有对Desktop或官方仓库执行写入/切换/构建，没有读真实图库或凭据。

只建立一个`modules/asset-library`，测试及子进程直接消费库API。采用JDK17 Java库，以便Kotlin后续调用并省去本轮无消费者的Kotlin/Compose插件与运行依赖；没有复制Desktop Prompt/请求/核心源码，没有引入sharedCore。私有v1 envelope只用于本切片的资产关系/事务，不冻结Recipe/工程/Server协议；两份业务文档作为独立opaque bytes保留。

## 实现与提交点

- `AssetStore`：显式独立根、身份标记、合作单写FileLock，拒绝已知CCB/Desktop路径和重定向目录，不回退其他根；关闭超时保留锁并拒绝新操作。
- 原始/派生资产有独立UUID，派生记录引用父资产；原始内容流式写入并校验size/SHA-256，按hash寻址，名称只作显示，不成为路径。已存在hash文件必须验证，损坏不被新输入静默替换。
- 先写事务意图、接收原始bytes、封存fingerprint和关联，再原子发布对象；不可变`records/<id>.rec`是**逻辑提交点**。随后才写可重建索引和完成标记，索引故障返回“已提交、需维护”，不伪装未提交而诱发盲重试。
- `recover`只自动完成已封存且验证通过的输入，重复执行稳定；未封存输入报告INCOMPLETE并保留，明确检查后才能选择新操作ID。部分输入不标Saved。未知/损坏意图和权威记录不被默认值覆盖。
- `ReadResult`分开Missing/Corrupt/ReadError/UnsupportedVersion；已提交记录引用的原件缺失属于Corrupt。损坏索引不删除原件，显式重建先严格扫描权威关系并保存被替换索引的原bytes。
- `AssetBackup`只快照identity/objects/records/transactions，最终manifest是备份完成标记；不含锁、索引、Secrets或无关根文件。恢复先完整校验，写到新根；中断标记阻止普通打开，同一manifest可重入，冲突目标bytes保留。恢复暂存另放`.restore-staging`。

详细API、格式限额和生命周期规则见[模块说明](../../modules/asset-library/README.md)。更新仅产出[后续设计](A1_001_UPDATER_DESIGN.md)，没有更新器实现、安装包、证书或密钥。

## 工具与依赖证据

- Windows 10 amd64；Temurin OpenJDK `17.0.20.1+1`（本机java/javac）；Gradle9.1.0复用已有安装，写缓存隔离到`.local/a1-001/gradle-home`。未改全局配置/原缓存，无Gradle分发包下载；原缓存ZIP不在本轮重新核算范围。
- Gradle Wrapper由该安装生成；wrapper JAR SHA-256=`76805e32c009c0cf0dd5d206bddc9fb22ea42e84db904b764f3047de095493f3`已与[Gradle官方checksum表](https://gradle.org/release-checksums/)匹配；distributionSha256Sum固定9.1.0-bin的`a17ddd85a26b6a7f5ddb71ff8b05fc5104c0202c6e64782429790c933686c806`。本轮测试直接使用已安装Gradle，不冒称重新下载/验证整个distribution ZIP。
- 生产外部依赖0；JUnit4.13.2（384581bytes）、Hamcrest-core1.3（45024bytes）从已有缓存只读复制到Suite临时Maven布局，固定坐标/锁文件/JAR SHA-256。下载字节0；缓存来源身份及hash见初始run证据。校验锁覆盖JAR内容，metadata校验和签名验证未启用；没有声称独立证明缓存发布者身份。
- 资源限制：Gradle单worker、单次daemon、heap384MiB/metaspace256MiB/2 processors，JUnit单fork192MiB，必要子JVM64MiB/1 processor。测试顺序执行；仅并发正确性用例使用两线程，未启动其他项目构建。峰值内存未测量，不编造资源保证。

## 执行沿革（失败与结果不抹平）

1. `01-wrapper.log`：默认缓存启动失败，native-platform.dll无法初始化；**FAILED，不是测试PASS**。
2. `02-wrapper-isolated.log`：切到Suite隔离缓存后Wrapper生成成功；只读依赖缓存第一次参数层级错误被Gradle禁用，此时没有产品测试。
3. `03-tests.log`：离线依赖解析缺少元数据，**FAILED，未运行测试**。随后仅复制两份已固定hash的测试JAR到临时仓库，避免下载/污染原缓存。
4. `04-tests-offline.log`与initial-run XML：首轮测试成功，1000资产/恢复/索引重建用例用时427915ms；这是修改前编译快照，源码在测试期间有修正，不用其结果签署最终源码。原快照逐文件源码hash未完整采集，标UNKNOWN。
5. 在首轮耗时阶段曾尝试查询自有worker以结束旧快照；CIM访问被环境拒绝，实际停止进程数0，首轮自行完成。没有更改权限或终止其他进程。
6. [第一次固定源码复测](evidence/A1_001/20261020T162815244Z/summary.json)：20 tests / 0 failures / 0 errors / 0 skipped，418.108s，源码前后hash一致。随后结束审阅发现`UUID.rec.rec`可能被错误识别成另一记录，增加严格文件名拒绝及回归；本次PASS保留为修补前快照，不代签最终代码。
7. [最终固定源码复测](evidence/A1_001/20261020T163611037Z/summary.json)：**2 suites / 21 tests / 0 failures / 0 errors / 0 skipped，PASS**；完整模块重新编译并实际运行，372.034s，测试期间8份Java源码hash不变。命令为离线脚本调用的`:asset-library:test --rerun-tasks`，single worker、strict JAR verification。最终`gradle.log`为Windows PowerShell UTF-16 BOM，保留原字节；XML/summary可直接解析。

初始失败/探索日志与XML见[evidence初始目录](evidence/A1_001/initial-run/)。最终run另存独立目录，包含gradle.log、JUnit XML、summary.json和实际sourceSha256。NO-SOURCE任务不是SKIPPED的测试用例。

## A1-D01—D07 对照

最终证据：[AssetStoreTest XML](evidence/A1_001/20261020T163611037Z/TEST-io.github.misakapiano.ccbstudio.assets.AssetStoreTest.xml)（16 tests）、[AssetBackupTest XML](evidence/A1_001/20261020T163611037Z/TEST-io.github.misakapiano.ccbstudio.assets.AssetBackupTest.xml)（5 tests）。以下PASS只针对本模块合成自动化，产品验收矩阵保持NOT TESTED，不代签人工/设备/供应商/更新安装验收。

| 入口 | 实际验证 | 最终结果 |
|---|---|---|
| A1-D01 | 有效合成PNG/JPEG及任意bytes逐字节、size/hash、独立Recipe/工程、派生父子关系，重开/独立子JVM读回 | 2 tests PASS |
| A1-D02 | 逻辑ID重放、同名异内容、内容去重保留关联、冲突输入拒绝、两线程并发归属 | 2 tests PASS |
| A1-D03 | 7个实际子JVM `halt(71)`点：INTENT/BYTES/SEALED/ASSET/RECORD/INDEX/COMPLETE；重开恢复两次一致；索引写失败已提交回执/恢复 | 2 tests PASS，含7个实际进程故障子场景 |
| A1-D04 | Missing/Corrupt/ReadError/UnsupportedVersion分离、未知/损坏bytes不覆盖、断流/注入空间权限失败、坏/缺索引重建保留原件、原件缺失、畸形意图、非规范记录名拒绝 | 5 tests PASS |
| A1-D05 | 未识别根/CCB路径拒绝、显示名不作为路径、真实Windows junction拒绝、跨进程占用/释放、排水超时保留锁且拒新操作 | 4 tests PASS |
| A1-D06 | 1/100/1000资产导入、关闭重开、恢复/索引重建；8MiB生成流最多64KiB单次读取；未声称256MiB容量或性能SLA | 1 test PASS；用例总344.549s，日志elapsedMs=344546 |
| A1-D07 | 原件/文档/未封存事务备份恢复、独立Secrets目录排除、备份中断拒绝、恢复中断阻止普通开根并可重入、坏包/冲突目标不覆盖 | 5 tests PASS |

没有单元测试FAILED或SKIPPED；前述环境/依赖启动FAILED仍保留。实际磁盘耗尽、OS ACL修改、断电和产品验收没有执行，保持NOT TESTED。

## 实际改动与未提交身份

| 范围 | 文件 |
|---|---|
| 最小构建/来源锁定 | 根settings.gradle.kts/build.gradle.kts/gradle.properties、gradlew/gradlew.bat、gradle/wrapper/、gradle/verification-metadata.xml |
| 一个资产模块 | modules/asset-library/build.gradle.kts、gradle.lockfile、README.md；main的AssetStore/StoreIO/AssetBackup/ReadResult/StoreException；test的AssetStoreTest/AssetBackupTest/StoreProcess |
| 合成执行工具 | tools/Test-AssetDurability.ps1（只运行限定模块，默认Root求值位于脚本正文） |
| 设计/证据 | 本报告、A1_001_UPDATER_DESIGN.md、evidence/A1_001/内原始日志/XML/summary/环境及文件清单 |
| 已有控制文件最小更新 | docs/00_INDEX.md、CURRENT_STATE.md、TASK_REGISTRY.md、DECISIONS.md、tasks/A1_001_IMPLEMENTATION_CANDIDATE.md |

[可审查未提交补丁](evidence/A1_001/source.patch)包含tracked diff与新实现/构建/设计/工具文件（含二进制Wrapper补丁）；执行日志/XML等证据由[精确文件状态及SHA-256清单](evidence/A1_001/WORKING_TREE.json)另列，避免补丁自包含循环。该清单绑定基准SHA、功能分支、最终测试源码hash及全部新增/修改文件；不是新的commit SHA。

原母本、A0三份报告/来源证据、SOURCE_LOCK、AGENTS、产品验收矩阵及原Verify工具均未改；三份TypeSet原件hash不变。默认Root缺陷未顺手修复。index无暂存改动，未commit/push/merge/release；本轮没有外部工程写入。

## 已知局限与停止边界

原始内容上限256MiB、单份Recipe/工程1MiB、元数据envelope3MiB、备份最多20000文件条目；无SQL索引、完整工程编辑器、真实用户备份UI、自动垃圾回收/迁移/密钥导出。数据格式为局部版本，不是长期公开交换协议。

备份白名单排除独立凭据文件/目录，不解析opaque Recipe/工程或原件中的秘密；后续调用者必须遵守凭据仅作SecretRef、不得嵌入普通文档的合同，不能把本次目录排除测试当完整DLP证明。文件ACL/原始文件系统时间和跨设备OS凭据迁移不在此切片内。

`force(true)`与原子move已在本机文件系统执行；不支持原子move则显式失败，没有静默降级。目录项断电耐久、硬件故障、OS缓存/磁盘损坏、真实磁盘耗尽/ACL变化、敌对进程路径替换/hard-link攻击和全应用所有写入者均未验证；文件锁只协调合作进程。空间/权限失败通过注入IOException/FileSystemException/AccessDeniedException测试，不冒充真实磁盘填满或OS ACL实验。

未封存且来源中断的事务保留给显式恢复；没有自动重新请求付费上游或清理未知文件。事务保留和restore scratch可能增加磁盘占用，尚无清理策略。恢复CLI/图形流程、备份加密和跨设备凭据迁移未实现。恢复报告也不意味着真实用户所有资料已被迁移验证。

Windows/Android/Server完整可信更新与真实内部A→B安装、签名/产品/渠道/过期/回滚/冻结、迁移和任务排水仍NOT TESTED；本轮不降低首版安全更新门槛。

完成本切片、最终测试与未提交diff证据后停止，交Project审查；进一步Git交付需用户另行明确授权，不能沿用A0首次推送授权。
