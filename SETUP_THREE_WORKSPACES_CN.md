# CCB Studio Suite — GitHub / Windows Codex / ChatGPT Project 一次建项操作手册

**文档身份：SETUP-ADDENDUM-001**  
**编写日期：2026-10-21**  
**范围：补齐“如何实际创建三个项目”的操作流程；只增加说明，不修改既有立项合同。**

## 0. 核验结论与使用前提

已核对交付包：

- 原始 ZIP：`CCB_Studio_Suite_INIT_1_0_20261020.zip`（含 `CCB-Studio-Suite/` 顶层文件夹、`AGENTS.md`、Project Instructions、A0 任务书和 TypeSet Forge 参考）；
- 服务器原始设计补丁：`CCB_Studio_Suite_INIT_1_0_Server_Design_Patch_20261020.zip`（13 个替换文件）；
- 本增量包：`CCB_Studio_Suite_SETUP_ADDENDUM_20261021.zip`（只新增本说明和首次 Git 发布任务书）。

**应当先解压原始包，再覆盖服务器修正补丁，最后加入本增量包。**

这三份提供完整的立项资料、ChatGPT Project 指令、Windows Codex A0 工作入口和 Git 初始化/远程发布步骤；并不包含已经创建的 `.git`、实际 GitHub 仓库、ChatGPT Project、生产应用或平台证书。三项需要按下面步骤在你自己的账号／电脑上完成。

现有 Desktop、官方 CCB 均不因此被修改。不要直接执行旧母本中的“创建文件”指令来覆盖现成的分文件内容。

## 1. 把三份压缩包组合为同一个 Windows 目录

### 第一次解压时

在资源管理器里打开三个 ZIP，**全部解压到 `H:\`，而不是解压到 `H:\CCB-Studio-Suite\`**；每个 ZIP 内已经带 `CCB-Studio-Suite/` 这一层。

1. 解压原始 `CCB_Studio_Suite_INIT_1_0_20261020.zip` 到 `H:\`。
2. 解压 `CCB_Studio_Suite_INIT_1_0_Server_Design_Patch_20261020.zip` 到 `H:\`。只覆盖补丁中列出的 13 个文件。
3. 解压 `CCB_Studio_Suite_SETUP_ADDENDUM_20261021.zip` 到 `H:\`。此增量包只增加文件，不覆盖现有项目文件。

结果必须是：

```text
H:\
├── ChatChatBar-Desktop\            ← 原项目；不动
└── CCB-Studio-Suite\               ← 新项目
    ├── START_HERE_CN.md
    ├── SETUP_THREE_WORKSPACES_CN.md  ← 本说明
    ├── AGENTS.md
    ├── docs\
    ├── handoff\
    │   └── GITHUB_INITIAL_PUBLISH_AFTER_A0.md
    ├── instructions\
    ├── tasks\
    └── .reference-assets\           ← 参考素材，Git 忽略
```

如果 `H:\CCB-Studio-Suite` **已经存在并且被你或 Codex 修改过**，不要直接选“全部覆盖”。先检查实际改动、仅补入缺失文件；服务器补丁与已有人工改动发生冲突时，按文件比对而不是覆盖。

### 检查原始包＋补丁内容

在新项目根目录打开 PowerShell：

```powershell
Set-Location 'H:\CCB-Studio-Suite'
Get-ChildItem -Force | Select-Object Name
powershell.exe -NoProfile -File .\tools\Verify-Package.ps1 -AllowExtraFiles
```

`-AllowExtraFiles` 是因为本增量包会新增文件。正常结果是 `PASS: 36 files match SHA-256 and size`。**它证明文件包完整，不代表任何软件功能通过测试。**

如果早已对原立项文件做过合法修改，原始校验不再通过是预期可能性；不要为了校验绿灯覆盖当前有效改动。可以让 Codex 仅解释差异。

## 2. 先创建 GitHub 远程空仓库（无需 Codex）

1. 登录 GitHub；打开 https://github.com/new ，或页面右上角 `+` → **New repository**。
2. **Owner**：`MisakaPiano`，若账号实际名称不同则用真实 Owner，不要凭历史记录硬设。
3. **Repository name**：`CCB-Studio-Suite`。
4. **Description**（可选）：`Native Windows/Android AI image studio and private task server`。
5. **Visibility**：你可选择 **Public**（与现有 CCB Desktop 的公开定位一致）或 Private；源码可见性不等于部署方式，也不等于取得第三方代码再分发授权。初始公开前需审查提交内容和素材许可。
6. **不要**选 `Add a README file`、`.gitignore` 模板或 `Choose a license`；这些文件已经在本地，空仓库可避免无意义的历史冲突。
7. 点击 **Create repository**。
8. 复制 GitHub 提供的 HTTPS 地址；若 Owner 如上，预期形式是：

   `https://github.com/MisakaPiano/CCB-Studio-Suite.git`

> 创建空仓库不会自动把 `H:\CCB-Studio-Suite` 的文件上传。首次提交和推送是后面的单独步骤。如果 GitHub 提示仓库名已存在，先确认是不是自己的目标仓库；不要覆盖已存在仓库或向错误仓库推送。

官方依据：https://docs.github.com/en/migrations/importing-source-code/using-the-command-line-to-import-source-code/adding-locally-hosted-code-to-github

## 3. 建立 ChatGPT Project（与 Codex 和 GitHub 是不同的空间）

1. 打开 ChatGPT 网页（或桌面端 ChatGPT 视图），在左侧找到 **新建项目 / New project**。
2. 名称填写：**CCB Studio Suite**。
3. 打开项目右上 `…` → **Project settings／项目设置** → **Project instructions／项目指令**。
4. 从你的本地包 `H:\CCB-Studio-Suite\instructions\PROJECT_INSTRUCTIONS.md` 复制**全文**，粘贴为项目指令并保存。注意必须是服务器修正补丁覆盖后的版本；其中应明确“服务器管理员集中配置上游 API、授权客户端无需 NAI Token；BYOK 不强制”。
5. 在 Project 的**项目资料 / Files / Add files** 中，先上传下列文件（按当前本地路径）：

   ```text
   H:\CCB-Studio-Suite\CCB_STUDIO_SUITE_INIT_20261020.md
   H:\CCB-Studio-Suite\docs\00_INDEX.md
   H:\CCB-Studio-Suite\docs\CURRENT_STATE.md
   H:\CCB-Studio-Suite\docs\TASK_REGISTRY.md
   H:\CCB-Studio-Suite\docs\SOURCE_LOCK.json
   ```

   **这 5 个文件足够冷启动。** 如果计划允许且需要增强索引，可以再添加：

   ```text
   H:\CCB-Studio-Suite\docs\CROSS_PROJECT_REGISTRY.md
   H:\CCB-Studio-Suite\docs\DECISIONS.md
   H:\CCB-Studio-Suite\docs\06_SERVER_SECURITY.md
   ```

6. **不需要上传整个 ZIP**，也不必把每一份分文件全部重复上传。ZIP 中的 `.reference-assets` 原始 HTML 先留本地给 Codex 定向审计；上传前应核对授权和隐私。
7. 在新 Project 新建一条对话，将 `handoff/NEW_PROJECT_OPENING.md` 中的**消息正文**粘贴发送。不要把标题或“复制下面正文”这种使用说明当作执行命令。
8. 等 GitHub 首次推送完成后，把仓库 URL `https://github.com/<你的owner>/CCB-Studio-Suite` 告知该 Project，并要求它按精确 commit 审阅 Git CURRENT；如 GitHub 连接不可用，报告未核验，不虚构实时读取。

**权限关系**：ChatGPT Project 本身不是你 Windows 的 Git 工作目录，项目资料不是自动双向同步的 Git 仓库。以后 Git 里的 CURRENT 是实时权威，上传的初始母本只保留有日期快照身份。只有需要时更新少量 Project 入口文件，不向 Project 重复堆积每次构建文件。

官方依据：https://help.openai.com/zh-hans-cn/articles/10169521-projects-in-chatgpt

## 4. Windows Codex 项目的实际创建方式

**Codex 本地项目的关键是选中一个真实工作目录**，它不等于 ChatGPT Project，也不需要先 Fork Desktop。

1. 打开电脑上的 ChatGPT / Codex 桌面应用，切换到 **Codex** 视图。
2. 通过 **打开文件夹 / Open folder / Add project（具体按钮可能随客户端版本不同）**，选择：

   `H:\CCB-Studio-Suite`

3. 确认显示的项目根目录**不是** `H:\ChatChatBar-Desktop`，也不是 `H:\CCB-Studio-Suite\CCB-Studio-Suite` 这种双重嵌套。
4. 在该工作区发新任务时，使用 `handoff/CODEX_START_MESSAGE.md` 里的**开场消息正文**。此消息会让 Codex 按 `AGENTS.md`、`docs/*` 和 `tasks/A0_001_BOOTSTRAP_AND_AUDIT.md` 做 A0。
5. A0 允许在新的独立目录执行本地 `git init -b main`（若尚未初始化），但**原 A0 明确禁止 `git commit`、`git push`、创建 GitHub 仓库、改其他仓库**。这不是文件包缺陷，而是对首次操作的安全边界。
6. 查看 A0 报告的真实 `Git` 状态、受审源码、变更路径和停止点；**不要让 Codex 自动接着实施 A1 或继续修改 Desktop**。

打开新目录会形成与现有 Desktop 分离的 Codex 工作区／任务上下文；不是在 Desktop 仓库里加一个长期功能分支。若 Codex 页面仍显示 Desktop，先切回正确目录再发送任务。

官方背景：https://help.openai.com/en/articles/20001275-chatgpt-work-and-codex

## 5. A0 完成后，把真实成果首次提交并推送到 GitHub

### 推荐：给 Codex 一个**单独的 Git 任务**

A0 结束、你确认文件内容后，**另开一个同工作目录 Codex 任务**，直接把下面文件的完整任务正文交给它：

`handoff/GITHUB_INITIAL_PUBLISH_AFTER_A0.md`

这会给 Codex 一次明确限定的 commit/push 授权，与 A0 的只读审计授权不混淆。它在推送前必须核对 staged 文件和 GitHub 远程身份，不能自行创建公开仓库或提交 `.reference-assets`。

### 手动完成时的 PowerShell 路径

以下是**你手动执行的替代方案**。如果交给 Codex 做，**不要同时手动再执行相同提交**。

```powershell
Set-Location 'H:\CCB-Studio-Suite'

# 如果 A0 还没有初始化 Git，先确认确实不在其他 Git 工作树内，
# 再手动运行：git init -b main

git status --short
git branch --show-current

git check-ignore -v .reference-assets/typeset-forge-1.1/TypesetForge_1.1.html

# 只提交已知的项目管理目录，不用 git add .
git add -- .gitattributes .gitignore AGENTS.md README.md START_HERE_CN.md `
  PACKAGING_NOTES.md CCB_STUDIO_SUITE_INIT_20261020.md `
  SETUP_THREE_WORKSPACES_CN.md docs/ handoff/ instructions/ references/ tasks/ tools/

# 审查实际将被提交的内容
git diff --cached --name-only
git diff --cached --check
git ls-files --cached -- .reference-assets/ .reference-repos/ .secrets/ data/ backups/
```

**必须确认上面的 `git ls-files ...` 没有列出任何私人参考、密钥或用户数据，而且 `git diff --cached --name-only` 里只有你期望的项目文件。** 如果发现不该提交的文件，先停止并查明；不要靠 `reset --hard` 或清目录解决。

然后才执行：

```powershell
git commit -m "docs(studio): bootstrap project governance and A0 audit"

# origin 尚未存在时：
git remote add origin https://github.com/MisakaPiano/CCB-Studio-Suite.git

# 如果 origin 已存在，先用 git remote -v 检查，不再 add 第二次。
git remote -v
git push -u origin main
```

推送后验证：

```powershell
git status --short
git rev-parse HEAD
git remote -v
git ls-files -- .reference-assets/
```

你应在 GitHub 页面看到 `README.md`、`AGENTS.md`、`docs/`、`handoff/`、`tasks/`；**不应看到 `.reference-assets` 下的 TypeSet 原始 HTML**。若还未运行 A0，首次提交描述应改成“initiation documents”，不要谎称已经包含 A0 审计成果。

若 Git 询问登录，使用 GitHub 的正常浏览器认证或已配置的凭据管理流程；**不要把 PAT 或密码贴进 ChatGPT、提交消息或脚本文件。**

## 6. 三者的交接和验收

创建完成后应分别核对：

| 工作位置 | 正确状态 | 可怎样验证 |
| --- | --- | --- |
| GitHub | 目标仓库存在，有 `main` 和首次 commit，未提交参考与秘密文件 | 浏览器查看仓库／commit；本地 `git remote -v` |
| Windows Codex | 工作目录是 `H:\CCB-Studio-Suite`，不是 Desktop；A0 只读审计完成或正在进行 | 查看工作区路径、A0 输出、`git status` |
| ChatGPT Project | 名为 `CCB Studio Suite`，项目指令采用服务器修正版，五份初始资料可读取 | 打开 Project settings／Sources，发送开场消息检查 |

如果 GitHub 仓库只有空仓库页面，**说明远程已创建但首次推送尚未完成**，不能说完整同步。  
如果 Codex 已运行 A0 但没有 commit，这是原任务安全边界，不是工作失败。  
如果 ChatGPT 能读上传文件但看不到 GitHub HEAD，应标记“实时 Git 未核验”，不要以初始 SOURCE_LOCK 的观察代替当前代码。

### 后续跨项目桥接

新仓库有真实 URL、首次 SHA 和 A0 结论后，再把 `handoff/DESKTOP_COLLABORATION_NOTE.md` 发到原 CCB Desktop Project。它只建立只读关联，不授权原 Desktop Codex 自动改代码、同步 upstream 或改变 P8—P18。

## 7. 常见错误定位

**错误 1：解压后目录是 `H:\CCB-Studio-Suite\CCB-Studio-Suite`。** 说明额外套了一层，把内层真正项目移到预期根；移动之前确认外层没有另外的用户数据或 Git 仓库。

**错误 2：补丁覆盖到了旧项目或未覆盖到。** 检查新目录 `docs/06_SERVER_SECURITY.md` 是否说明服务器管理员管理上游 API，授权客户端不强制自带 NAI Token。不是的话先不要启动 A0，修正补丁位置。

**错误 3：GitHub 显示已有 README 引发 push 拒绝。** 一般是创建远程时勾选了 README、license 或 gitignore。不要强推；先确认远程不是已使用仓库，按真实历史做安全合并或在未使用的空仓库上重新建项。

**错误 4：`git init` 说已有仓库。** 先 `git rev-parse --show-toplevel`；若输出是新项目根目录，继续；若输出是 Desktop 或其他父目录，停止，不要污染既有仓库。

**错误 5：Codex 说它不能 push。** 原 A0 明确禁止，这是预期行为。A0 完成后另发 `handoff/GITHUB_INITIAL_PUBLISH_AFTER_A0.md`，不要求它违背之前任务边界。

**错误 6：项目资料的旧 SHA 与实时 Git 不同。** 初始母本和 SOURCE_LOCK 是有日期的来源观察；实时仓库前进是正常现象。准确记录新 SHA 与差异，不通过静默重写历史抹掉旧观察。

**错误 7：校验器提示 EXTRA。** 本增量包只新增说明文件，应用 `-AllowExtraFiles`；不要为了消除 EXTRA 删除使用者自己的文件。

**错误 8：GitHub 鉴权失败。** 先完成官方登录流程，别关闭现有 Desktop 的 Git 凭据或把私钥写入 README。

---

**最终判断：**原包提供产品与技术合同，服务器补丁恢复管理员集中 API 模式，本增量包补足 GitHub 创建和首次安全推送、Windows Codex 工作区绑定、ChatGPT Project 初始化三个操作缺口。三份组合后，足以完成“正式建项和启动 A0”的准备；**仍不意味着产品代码已经完成或版本适配已经验收。**
