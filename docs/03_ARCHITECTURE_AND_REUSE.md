# 03 — 架构与代码复用

## 初始方向

Windows 优先 Kotlin/JVM、Compose Desktop、JDK 17；Android 为原生应用，复用适用纯 Kotlin/JVM 逻辑和平台无关合同。不要求先将整个 CCB/Desktop 迁成 Kotlin Multiplatform。

具体 Kotlin/Compose/AGP/SDK 版本由 A0 对照被采用源码冻结，不复制旧快照版本号。

## 逻辑职责

```text
apps/studio-windows
apps/studio-android
server/studio-server
modules/studio-domain
modules/image-editing
modules/asset-library
modules/remote-protocol
modules/update-contract
platform/windows
platform/android
```

这是职责划分，不要求初始化时创建全部空模块。只有真实消费者需要时建立模块与构建配置。

## 依赖与复用优先级

1. 直接依赖经过审计且版本固定的共享模块。
2. 在原权威项目中另立小范围无语义漂移的共享抽取任务。
3. 原项目暂不适合改动时，受控引入精确源码，记录路径、commit、身份、授权、差异、同步与测试策略。
4. 确有平台或产品差异时实现窄适配。

不得无来源复制一套 Prompt、请求构造、WorldBook 或 metadata 解释后分别维护。

本地 Composite Build 是候选，不让正式发行依赖某开发者未提交邻接工作树。发行绑定精确依赖版本和可重现来源。

## Android 的直接参考

文件选择/分享、生命周期、系统安装、平台安全存储可直接参考官方 Android CCB 的适用源码与平台文档。共享请求语义则只选定一套版本；不能让 Windows/Android/Server 各追各的最新实现又宣称一致。

Suite 可比 Desktop 更早采用某项已审核的图像更新，但不得自动提升 Desktop compatibility；反之亦然。

## 无环的双向反哺

允许：CCB 图像请求核心被 Desktop/Suite 消费；通用图片编辑核心被 Suite/Desktop 消费。

禁止：完整 Desktop 应用依赖完整 Suite 应用，而 Suite 又依赖完整 Desktop。通用编辑组件不得依赖服务器、完整聊天容器或 Suite 独有持久化实现。

不急于新建第三个共享仓库。模块可以由任一现有仓库发布；每项能力只有一个维护来源，依赖方向有向无环。

## 接口边界候选（未冻结实现或 schema）

- 生成后端：本人直连使用本机选择的供应商凭据；远程任务后端将完整 Recipe 交给服务器，**服务器以管理员统一配置的上游凭据执行**，不要求远端客户端提供 NAI Token；可选 BYOK 不成为默认依赖；
- 图像资产：不可变原始字节、受控派生物、可重建缓存；
- 账户解析：区分客户端/直连供应商账户、服务器管理员的供应商账户、Studio Server 用户/设备身份，以及各自的安全凭据引用；
- 编辑工程：平台无关操作/层次 + 平台渲染适配；
- 平台能力：文件选择、分享、通知、凭据、安装与生命周期；
- 服务端协议：任务/资产标识，不接受任意本机路径或命令。

以上只是 A0 审计边界，不批准数据库、正式格式或所有类名。
