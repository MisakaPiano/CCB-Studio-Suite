# NAI账户、Danbooru更新和V5 Opus体力（2026-10-23）

后续人工回执：用户确认真实词库完成、再次检查最新、中文注释和候选补全可见；实际切换V5后体力正确。最初负面反馈经补充截图澄清，限定范围通过。后台等待、单一模型入口及费用按钮后续改动见[UX_NAI_BACKGROUND.md](UX_NAI_BACKGROUND.md)，不能据旧版本回执冒签新界面。

基线为Suite `f75cfc459ce6464d62099a9bb5a4ea18165ea8cf`，沿用 `feature/windows-rc-continuous` 连续授权。用户新截图要求核对NAI配置、词库更新、中文翻译和V5体力。仅在Suite修改；Desktop/官方来源只读。未读取真实工作区、API Key或用户图片；用户截图只作为界面问题线索，不纳入仓库证据。无真实生成请求，无新公开安装包。

## 核实及修改

- NAI账户原来沿用通用语言模型编辑器，展示固定接口和手填模型ID，创作页账户菜单也显示账户中遗留的模型。实际请求始终按创作草稿的selectedModel执行，因此可能显示V4.5而本次实际用V5。账户页现只管理账户名称/凭据，固定服务以文字表示；按钮为“保存并选用此账户”。创作页和账户摘要显示本次实际生图模型。既有资料结构与请求语义未变。
- 新建创作默认值和本次明确选择分开显示。用户明确指定的本次模型不因修改默认值被覆盖，沿用既有resolvedDraft权威；不是重写模型分辨逻辑。
- 中文注释已实装：`NovelAiPromptTranslationService`使用本地Danbooru翻译及ECDICT离线词典，在原Prompt下显示注释，不修改原文，不调用语言模型。这不是任意整段中英互译服务。原先缺少用户能操作的词库更新入口。
- 新增“创作设置”：检查Danbooru词库、用户确认下载并校验、取消、本地中文注释和复制时忽略画风。沿用既有固定来源的GitHub检查器和桌面不可变词库发布器：固定提交URL、Git blob SHA-1/大小/SQLite结构验证、先构建补全索引，再原子切换active-catalog.json；旧版本保留，失败/取消不换词库。刷新翻译服务及注释异步键，使已有Prompt在词库更新后重新注释。
- 账户解析器已有有效Opus资格、V5百分比/用尽状态和约可生成张数。现在实际选择V5时显示体力及进度条；未知/非有效Opus/非有限数值不画伪满格。仅显示服务结果，不修改费用/资格算法。切换账户仍清空旧额度，刷新仍经过既有请求预算。
- 用户截图的“请求预算等待268秒”属于已实现的请求间隔门槛；不能因此判定网络卡住。未绕过预算或自行重发。

## 来源

固定Desktop `5ef95428421f7c693b010ac88f51d470804ab07a` 三个生产文件（检查器、共享索引构建器、桌面安装器）按原字节复用，路径和SHA-256见[evidence/UX_NAI_SETTINGS/SOURCE.json](evidence/UX_NAI_SETTINGS/SOURCE.json)。同版本`DesktopNovelAiCatalog.kt`的已验证manifest读取路径恢复到Suite适配中；对应CatalogUpdateTest作为测试基线，另加取消/错误数据验证。V5显示是Suite平台UI接线，未改权威NovelAiAccountService/成本估算/Prompt算法。外部活动HEAD仅作只读布局参考，不成为整分支采用授权。

## 验证状态

首次20261022T200110445254Z-nai-settings-initial在编译阶段发现缺少共享索引构建器，0项测试执行，原始失败保留。按相同固定来源补齐依赖并更新113项权威文件manifest后，20261022T200418356651Z-nai-settings-dependency-complete实际30项通过，0失败/错误/跳过，235.446秒，源码稳定。范围包括完整工作台、账户职责、角色卡默认值、Prompt输入/注释以及新增NAI/词库更新验证；不是全量桌面测试。

新增验证使用合成SQLite和拦截传输：校验失败不切换、索引生成及激活、重开继续使用新词库、损坏manifest不被覆盖、取消/非法SQLite保留旧指针且清理临时文件、固定commit下载及blobSHA判别、无Authorization头。实际随包词库可给blue sky/cloud产生中文注释，原Prompt保持。UI验证旧账户模型值不干扰实际V5显示、NAI表单不含手填ID/地址、仅V5显示体力、默认值不覆盖显式选择，以及未知/NaN/非Opus/用尽/49%数据投影。

收尾把费用类别改为中文，并为设置页加滚动以容纳更新进度/错误；20261022T200831672893Z-nai-settings-final-display最终7项定向测试实际通过，0失败/错误/跳过，85.011秒，源码稳定，重新构建本地包成功。随后将两项辅助源列入复现工具显式种子并实际重放，113项路径和SHA-256完全一致；未改生产字节，见AUTHORITY_REPRODUCTION.json。不把编译或合成测试升级为用户验收。实际网络词库更新、真实V5额度显示及本轮新界面人工测试尚未验证。PRODUCT_HOLD / NOT RC。


## 固定本地包与人工下一步

新包`.local/windows-rc/nai-settings-review/application`共220文件，逐项SHA-256与构建输出核对，见PACKAGE_FILES.json。最终构建源码快照与当前modules字节一致。快捷方式`.local/windows-rc/nai-settings-review/Start-nai-settings-review.lnk`显式传入原model-accounts-review工作区和runtime，参数已与启动解析器及之前正常快捷方式核对，未自行启动第二个工作区实例、未读取/复制用户资料，见SHORTCUT.json。

人工：关闭旧测试窗口后从新快捷方式打开；确认NAI账户仅配置名称/凭据，顶部实际模型与创作生成设置一致；V5时能看到体力状态，可主动刷新账户额度但不生成；创作设置中检查Danbooru，发现新版本才下载并校验，确认完成/重开后中文注释仍有效。未知体力需区分尚未刷新、未有效Opus和服务未返回数值。合成原文blue sky/cloud可用于看注释，不必提交私人Prompt。真实网络/额度和这些新界面尚待用户回执；此前三类账户布局的限定人工通过仍保留。
