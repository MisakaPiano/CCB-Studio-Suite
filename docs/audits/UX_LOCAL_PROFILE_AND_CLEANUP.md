# Preview105 本地配置接入与空间清理

2026-10-23。用户在发布后明确批准两处临时产物清理，并要求把已保存key等数据迁到新版；随后要求清理完全不需要且可安全删除的重复构建产物。此授权覆盖本机配置/受保护凭据核验，不授权把真实资料/凭据上传，也没有真实服务调用。

## 最初原位接入（现已由独立安装替代）

已从旧快捷方式只读核对，账户/词库/图库评审长期共用 `model-accounts-review` 的workspace/runtime；后来的独立合成图库包不是这套真实配置源。新版采用原位沿用资料的方式，无需因换程序复制整库或导出明文key。

- 当前固定程序：`.local/installed/105-a416a820955a`；来自已发布105的已核验安装，547文件匹配。
- 固定入口：`.local/installed/Launch-CCB-Studio.lnk`。其名称/位置与更新器完成验证后更新的稳定入口一致，后续应用更新能把它指向新版本。
- 入口精确沿用既有workspace/runtime，启动时暂停目录自动收集。账户、共享key、独立key、图库、工程和运行配置仍保存在原位置，不建立第二套资料分叉。
- 在独占工作区锁下核对文档链、凭据引用及同一Windows用户DPAPI可读性；只做内存验证，明文立即清零释放。只发布状态布尔，不发布凭据引用、内容、指纹或真实账户数量。
- 文档和密文原字节均未改变；本轮没有真实API、未启动真人工作区界面，不代签原生105重开体验。[脱敏回执](evidence/UX_INVITATION_PREVIEW_105/LOCAL_PROFILE_CONNECTION.json)。详细本机回执与活动路径只在`.local`中。

**保留规则：** `.local/ACTIVE_STUDIO_INSTALLATION.json`现已指向下方独立安装，其程序/workspace/runtime/稳定快捷方式不能当成测试产物清理。尤其旧目录名`model-accounts-review`内保留的是迁移前真实资料备份，不代表可删除。旧仓库内启动快捷方式已标记retired，用户使用新的仓库外入口。

## 已完成的更新与初次清理

刚才获批的candidate-2/payload及update-smoke-final已清理，初次回收约480MiB；完成后的同名合成演练暂存已再清理。[首次清理清单](evidence/UX_INVITATION_PREVIEW_105/CLEANUP_APPROVED.json)。

随后复用已存在104程序，实际从公开签名渠道下载105、验证完整散列、安装到新目录并检查105显示UP_TO_DATE。合成资料及旧程序原字节不变；原生进程重启仍未测。[实际更新结果](evidence/UX_INVITATION_PREVIEW_105/LIVE_UPDATE_FINAL.json)。

## 扩大清理的执行结果

已核对80项旧程序/旧安装器/重复zip，逻辑大小约15.30GiB，清单在`.local/CLEANUP_REVIEW.md`；不含活动程序、workspace/runtime、签名保管/恢复材料、源码、审计证据和后续构建依赖缓存。文件系统链接与资料标记均作为拒绝条件。

自动审批首次拒绝了尚未执行的批量删除，理由是约80项目录递归删除范围大，需要用户对精确清单明确授权。用户随后明确回复“批准按清单删除全部80项”；再次逐项核对已确认清单、绝对路径、受保护目录、进程与文件系统链接后执行，80项均已删除。实际可用空间增加16475402240字节，约15.34GiB；清理后当时H盘约15.74GiB可用。[精确清单](evidence/UX_INVITATION_PREVIEW_105/APPROVED_CLEANUP_REVIEW.md)与[结果](evidence/UX_INVITATION_PREVIEW_105/BUILD_CLEANUP_RESULT.json)保留。当前程序、所有工作区/runtime、凭据、签名材料和依赖缓存均保留；未扩大到清单外的旧资料或其他项目。

## 用户追加：使用版移出源码仓库

用户指出不宜把日常安装放在当前工作区，并要求H盘独立新文件夹。现已完成`H:/CCB-Studio-Suite-Preview`独立安装，版本程序位于`105-a416a820955a`，资料分别在`data/workspace`和`data/runtime`；根目录固定`Launch-CCB-Studio.lnk`与更新器提交稳定入口的位置一致，实际WScript回读目标/参数匹配，自动收集暂停。

工作区4992文件、运行配置24文件合计1014949128字节逐文件复制和散列匹配，源文件再次核对未变；迁移后引用的Windows加密凭据可读，未写明文或发送请求。版本程序在资料校验后由仓库移出、547文件清单再次核验，原工作区/runtime保留为迁移前备份。仓库内旧固定快捷方式改为`.lnk.retired`，防止继续写入旧资料分叉。新入口运行不依赖源码仓库；原生105界面启动仍未测，用户离开电脑期间未发起人工测试。[独立迁移结果](evidence/UX_INVITATION_PREVIEW_105/INDEPENDENT_MIGRATION_COMPLETE.json)保留，详情只在本机MIGRATION_COMPLETE/ACTIVE_INSTALLATION文件中，无私人资料或凭据上传。

清理实际回收约15.34GiB；保留原资料备份并建立独立副本后，H盘可用约14.79GiB。此最新独立安装方案替代上文最初原位接入。

后续减少重复产物：日常使用固定安装版，普通验证优先复用现有映像/合成资料；需要冻结新候选时才完整打包。保留来源、失败/成功证据及当前/必要前版，旧二进制副本按已确认清单清理，不删除依赖缓存来换取下一轮重复下载。

## 2026-10-24 用户授权改名为 CCB-Studio-Suite-App

只读核对：旧App为209b525早期0.1.0测试包，280文件=279清单文件+清单本身，逐项bytes/SHA256匹配、无独有资料、无运行进程；Preview固定入口实际111，原活动记录105已滞后，本轮纠正为111。桌面/开始菜单没有指向这两根的额外快捷方式。无未完成更新：旧交接仅COMPLETE或FAILED_RETAINED。

用户明确要求删除旧App并将Preview改名，核对绝对路径/无reparse点及进程后执行，删除123851480字节（118.11MiB）；改名保留7365文件，其中7362份程序、资料、加密凭据与历史回执散列相同，仅根README/ACTIVE_INSTALLATION/快捷方式按新根更新。资料位于H:/CCB-Studio-Suite-App/data/workspace，runtime同级data/runtime，固定入口H:/CCB-Studio-Suite-App/Launch-CCB-Studio.lnk；当前111未擅自升级112。原备份字段未改。新App是活动安装，不能按旧测试目录删除。

新根111程序完整性验证通过，其实际捆绑运行时从原签名渠道检查到112，不下载/安装。快捷方式目标、参数及工作目录均为新根。更新器按实际程序父目录派生后续版本及固定入口，本轮没有修改签名、更新代码或历史plan中的旧路径。真实日常原生启动未测，用户资料只作文件散列核对，不解密导出凭据、不调用生成。

精确本地证据：.local/install-rename-20261024/{REVIEW,VERIFICATION,UPDATE_CHECK}.json；迁移前逐文件散列和原快捷方式在同目录保留。后续从新固定入口使用，源码仓库仍为H:/CCB-Studio-Suite；本轮无需新构建/发行。
