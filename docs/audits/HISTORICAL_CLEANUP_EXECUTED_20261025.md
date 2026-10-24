# 已授权历史残留清理实际回执

2026-10-25 Asia/Tokyo。用户已直接授权清理清单中确认的旧测试程序、旧共享测试内容和日常目录中不再需要的程序残留；复审证据、固定基线、日常当前安装、备份及未知归属保留。本次是精确清单的人工清理，与尚未发布的新更新器自动机制分开。

仅20个明确根，逐文件核对原计划身份后按句柄删除，目录只在空时移除；未递归清理整个H盘，没有清空回收站。共享测试根中名称表明备份、迁移或证据的子树不遍历、不删除；归属未知、重解析点和不安全名称保留。清日常残留时取得更新缓存锁并重新确认默认入口，未调用新自动清理机制。

实际移除 **236530** 文件、**8506611341** 字节（**7.92 GiB**），延后0项；1086个明确保留项均仍存在。当前115完整程序清单复核通过，12项受保护锚点散列一致；活动记录仍保护的111、原活动备份、107/114/115固定测试基线保留。用户资料和私人凭据内容未读取，不能把排除范围与入口散列检查写成全量私人资料逐字节验证。

| 准确根 | 移除文件 | 回收字节 | 保留条目 |
|---|---:|---:|---:|
| `H:\CCB-Studio-Suite\.local\windows-rc\performance-106\smoke-106` | 550 | 252177541 | 0 |
| `H:\CCB-Studio-Suite\.local\windows-rc\batch-109\smoke-109` | 550 | 252496316 | 0 |
| `H:\CCB-Studio-Suite\.local\windows-rc\update-repair-110\smoke-110` | 550 | 252498502 | 0 |
| `H:\CCB-Studio-Suite\.local\windows-rc\batch-parity-111\smoke-111` | 550 | 252504526 | 0 |
| `H:\CCB-Studio-Suite\.local\windows-rc\batch-history-112\smoke-112` | 550 | 252523469 | 0 |
| `H:\CCB-Studio-Suite\.local\windows-preview\tests` | 81063 | 3117641767 | 918 |
| `H:\CCB-Studio-Suite\.local\windows-preview\desktop-tests` | 149951 | 1516452845 | 168 |
| `H:\CCB-Studio-Suite-App\105-a416a820955a` | 550 | 252165408 | 0 |
| `H:\CCB-Studio-Suite-App\107-5907779fad12` | 550 | 252182991 | 0 |
| `H:\CCB-Studio-Suite-App\109-2093ae5cb858` | 550 | 252496316 | 0 |
| `H:\CCB-Studio-Suite-App\112-acf71fc1acb2` | 550 | 252523469 | 0 |
| `H:\CCB-Studio-Suite-App\114-f59b0c25a2eb` | 550 | 252625080 | 0 |
| `H:\CCB-Studio-Suite-App\data\runtime\updates\downloads\55f29ce4-a8f1-46ee-8ad6-4ab5603c7d1c` | 2 | 168634431 | 0 |
| `H:\CCB-Studio-Suite-App\data\runtime\updates\downloads\6a364160-365e-4bd6-b8fd-d3c92e9c5f56` | 2 | 168519172 | 0 |
| `H:\CCB-Studio-Suite-App\data\runtime\updates\downloads\6ed417d6-2848-4e04-81de-0d6ecd55f89f` | 2 | 168509900 | 0 |
| `H:\CCB-Studio-Suite-App\data\runtime\updates\downloads\9e0a48ce-c1fa-45a3-bc81-1d5cf65ab58d` | 2 | 168212176 | 0 |
| `H:\CCB-Studio-Suite-App\data\runtime\updates\downloads\ad964293-d37e-467b-a2a4-5c5dae56f88d` | 2 | 168536656 | 0 |
| `H:\CCB-Studio-Suite-App\data\runtime\updates\downloads\d5014e2a-6d75-4b4d-bd2d-6054f6990677` | 2 | 168890976 | 0 |
| `H:\CCB-Studio-Suite-App\data\runtime\updates\downloads\e279470e-7160-4bcc-9561-7f5c29d0bff0` | 2 | 168509900 | 0 |
| `H:\CCB-Studio-Suite-App\data\runtime\updates\downloads\fd0da5a1-d945-4786-839b-0a0a64bc80a2` | 2 | 168509900 | 0 |

两个共享测试根因为保留备份和证据而仍存在；这不代表可以进一步一键清空。上述程序以外的旧目录和早期失败现场没有获得本次精确归属证明，未删除。111不能绕过既有保护记录自动当垃圾处理。成功清理后无需继续使用旧105/107/109/112/114二进制回滚；用户当前115、备份和迁移原件仍保留。

完整逐文件计划、删除日志、精确源归属和事后检查见 [证据目录](evidence/HISTORICAL_CLEANUP_EXECUTED_20261025/POSTCHECK.json)、同目录PLAN.json.gz、REMOVALS.jsonl.gz、RESULT.json。清理脚本仅是本次历史操作的证据，不是后续执行授权；不可直接重放。
