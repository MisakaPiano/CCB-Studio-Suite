# Codex 独立任务：CCB Studio Suite 初次 Git 提交与 GitHub 推送

**任务 ID：BOOTSTRAP-GIT-001｜执行时点：A0-001 报告完成、用户已审核提交范围之后。**

> 本文件是给 Codex 的任务书。**由用户明确发送本任务后，才授权按本任务的界限执行 commit/push。** 它不是 A0 的默认授权，也不授权创建/公开 GitHub 仓库。

## 当前目标

将 `H:\CCB-Studio-Suite` 已有的已审阅立项文件和 A0 只读审计成果，在独立 Git 仓库 `main` 形成一个可追溯提交，并推送到**用户事先在 GitHub 创建且已确认身份的目标远程仓库**。

仅允许当前 Suite 工程的必要 Git 初始化、明确路径的首次 commit 和一次正常 push。不得改动 CCB Desktop、官方 CCB 或 TypeSet Forge 参考原件。

## 起始检查（全部必须做）

1. 读取 `AGENTS.md`、`docs/CURRENT_STATE.md`、`tasks/A0_001_BOOTSTRAP_AND_AUDIT.md`、A0 实际产物和 `SETUP_THREE_WORKSPACES_CN.md`。
2. 核对 `Get-Location` 对应 `H:\CCB-Studio-Suite`，不是嵌套工程，不是 `H:\ChatChatBar-Desktop`；记录当前 branch/HEAD、worktree/index/untracked。
3. 核对已经有真实 A0 结果，未完成时不虚构其完成；仅做用户允许的立项资料提交需更正 commit 信息。
4. 核对 GitHub 远程地址预期为 `https://github.com/MisakaPiano/CCB-Studio-Suite.git`；若 Owner 不同或仓库已存在内容/发生推进，**停止写入并报告，不覆盖、不强推**。
5. 如尚无本地 `.git` 且已确认是独立目录，可以 `git init -b main`；若 Git root 是父目录或其他项目，停止。
6. 若已存在 Git commit，**不要擅自创建第二个“初始提交”**。给出当前 HEAD 与远程分支关系，按现有历史审查是否能安全 fast-forward 推送。

## 安全提交流程

- 检查 `.gitignore` 是否确实排除 `.reference-assets/`、`.reference-repos/`、`.secrets/`、`.env*`、`data/`、`backups/` 等。
- 不采用无范围的 `git add .` 或 `git add -A`；仅显式允许 `.gitattributes`、`.gitignore`、`AGENTS.md`、`README.md`、`START_HERE_CN.md`、`SETUP_THREE_WORKSPACES_CN.md`、`PACKAGING_NOTES.md`、`CCB_STUDIO_SUITE_INIT_20261020.md` 以及审查过的 `docs/`、`handoff/`、`instructions/`、`references/`、`tasks/`、`tools/` 具体路径。
- 审阅 `git diff --cached --name-only`、`git diff --cached --stat`、`git diff --cached --check`、staged 文件关键内容与潜在 Secrets；如无法确认合法来源或敏感性，**停止，不推送**。
- 禁止提交 TypeSet HTML/README/CHANGELOG 原件、浏览器缓存、真实图片、API Token、密码、私钥或签名证书。
- `BUNDLE_MANIFEST.json`、`SHA256SUMS.txt` 是初始交付校验记录，已按 `.gitignore` 排除，不要为做成“零未跟踪文件”而提交它们或删除文件。
- 不擅自新增覆盖混合来源源码的统一许可证。
- 在提交范围通过后才 `git commit`；若 A0 结果已经存在，可用 `docs(studio): bootstrap governance and record A0 audit`。不能谎称未完成的审计已完成。
- `origin` 未设置时只添加经确认的目标；存在时先检验，**不删除或重定向未知 origin**。
- 只允许正常 `git push -u origin main`。如遭拒绝，停止，不使用 `--force`、`reset --hard`、`rebase` 或自动合并未知远程提交。

## 严格禁止

- 不创建 GitHub 远程仓库、不更改其 public/private、不自动添加 GitHub Secrets 或发布 Release。
- 不访问私人用户图库、SecretStore、Windows Credential Manager 或其他应用的真实数据。
- 不运行 Gradle、Android 模拟器、安装器或付费 API 请求。
- 不执行 A1，不修改产品 Prompt，不吸收上游更新。
- 不提交或修改 `H:\ChatChatBar-Desktop` 任何内容。

## 验证与回报

成功后报告：

```text
本地路径：
仓库 root：
当前 branch：
本地 commit SHA：
GitHub origin URL：
远程 main SHA（若可读取）：
提交文件列表／数量：
.reference-assets 是否保持未跟踪且被忽略：
真实用户数据／Secrets 是否未提交：
本地 git status：
推送是否真实成功：
未确认或未完成事项：
```

`git push` 或网络鉴权失败时只报告事实，保留本地提交，不称同步完成，不反复重试或改凭据。可将真实 SHA 和仓库 URL 带回新的 CCB Studio Suite ChatGPT Project 做独立审查。完成后停止。
