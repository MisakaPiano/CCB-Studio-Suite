# 原生选择框安装版观察与双通道交付

任务 UX_RECOVERY_CONTINUOUS；固定源码 `08733d5dcc97f47bc1bf813475c505a58391a4c7`，分支 `feature/windows-rc-continuous`。PRODUCT_HOLD / NOT RC。

## 固定安装包

|版本|文件大小 bytes|SHA-256|实际状态|
|---|---:|---|---|
|0.2.7-internal|167303853|a5064c48a986b397364c432c9f380664dfb2f4e35427c00365e93383be7e7908|已构建、实际安装、546 个安装文件校验通过；过长路径写入前拒绝、已有目标拒绝且文件保全实际 PASS|
|0.2.8-internal|167303848|fe73b343512739a978abd10ef0b17007bb4e4d8a6d7fb6bf3d872c8a7febef7b|同源码构建，发布到本机隔离更新测试库；本版本尚未安装/更新演练，不标 PASS|

Gradle 两次实际 createDistributable 成功分别 20.029 / 16.401 秒，属于构建命令成功（测试数 0），不是新测试 PASS。源码修复回归为前一记录的定向 16 项及完整 desktop 108 项实际执行。安装包以独立目录封存，没有覆盖之前版本。

## 安装版原生观察

版本 7 使用来自旧已验证 A→B 的独立合成资料副本（72 文件 / 50804 bytes），持有来源锁复制，原件和随机测试 DPAPI 凭据逐文件散列一致；不包含用户真实图库或真实 API 凭据。资料使用 ab-data-5，旧 ab-data-4 保留。

用户误关第一次打开的新版后明确要求重开。已重新打开同一安装目录、同一测试工作区；实际进程 20352，窗口 857632。用户从创作页点击“导入图片”提供截图，实际显示 Windows Common Item Dialog 的地址栏、搜索、左侧导航、文件列表及 Open/Cancel。由此确认固定安装版真实弹框；不是仅源码或模拟场景。

截图显示用户个人目录名，因此仅保留本地，不上传公开 Git 或 Drive；提交的是观察结论与图片身份。电脑捕获在新版本仍超时，不能声称自动捕获成功。手动选文件、多选落盘、取消后焦点、原生恢复选择和第二工作区打开尚未完成现场验证；这些项目仍 NOT TESTED。用户窗口保持打开，不因验证而自动关闭。

## Google Drive 实际回执

原整包上传返回 HTTP 413：167303853 bytes 超过连接器 104857600 bytes 上限。保留失败；未修改账号/共享权限。采用两个低于上限的分卷，并提供先校验各卷、再重组并核对完整 SHA-256 的 PowerShell 脚本。脚本实际运行成功，不覆盖已有文件，不执行安装包。

- [分卷 01](https://drive.google.com/file/d/1qVH1-GvSjcBc_pj1gqgPQK1EbqPSAQQv/view?usp=drivesdk)：83886080 bytes，SHA-256 `52807cdc5588c1cc808f938e7472bbff3d602c8ccebc829ceccce1382da9ea63`。
- [分卷 02](https://drive.google.com/file/d/12OFZci2CZS1CEKT8yqnMef4ftoqNZNZD/view?usp=drivesdk)：83417773 bytes，SHA-256 `a09d69ee7d5382897f3e2d19fc7d75ac9b6f07ac82814d2acb955e09343b9e9a`。
- [重组说明与校验脚本](https://drive.google.com/file/d/1nQpHo9TuzISV8-GlpKLMUZgiunuov9_8/view?usp=drivesdk)：1996 bytes，SHA-256 `a6705dd70761ebc76ccb4064a7b23f4283c46ce03a94cfec95fa4c19e0b671b4`。
- [完整构建和安装证据](https://drive.google.com/file/d/1tqD1lZ56-orPvXiv-4EOb9rVX8uVVawi/view?usp=drivesdk)：139471 bytes，SHA-256 `37dd466aaa36e5f098356bf8925a727f89c85f9cbac6f05019e8222153d22ef7`。

以上实际上传成功，逐个 metadata 回读确认 ID、名称、大小及指定父文件夹。连接器未返回远端散列；上述 SHA-256 为本地上传文件散列，完整重组也只声明本地实际验证。未导出私钥、凭据、数据根、私人 Prompt/图库或 TypeSet 原件。0.2.8 尚未上传 Drive。

将两个分卷和说明 ZIP 下载到同一新目录，解压说明 ZIP，运行其中 Reassemble-Installer.ps1，可得到精确 0.2.7 安装包。此包供内部测试，尚不是最终 Windows RC；不把本记录当成 W1–W5 产品验收通过。
