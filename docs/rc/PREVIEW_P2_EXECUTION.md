# Preview P2: runnable UI and recovery engineering

Base P1: 7d6bc80b61199e3ee9647127fea9b1e634ebeb9c. Implementation identity is the containing engineering commit and each run's full source-before.json. Work remains IN PROGRESS until actual package/UI smoke and final review receipt.

- Independent Compose Desktop consumer compiles. Gallery imports, fixed-authority synthetic Designer/revision/NAI, account identity, full Recipe/metadata, persistent edit layers, batch export, order/undo/redo, backup and restore-to-new-root are wired.
- Core run `20261020T195607582282Z-recovery-ui`: 15 actual tests, no failures/errors/skips; strict/offline resolution, source stable. Includes real child-process halt at pre/post document commit, another-process root conflict, snapshot corruption/unknown/incomplete rejection, restore staging retention and fresh retry. Full authority regression follows final package checkpoint.
- Restore validates every manifest size/hash and copied document references, stages under a unique sibling, and publishes only into a new absent directory without replace. Interrupted staging stays retained; restart/retry uses a fresh stage, not in-place migration. Ordinary backup excludes OS-bound credentials. This does not claim arbitrary power-cut or full-app update durability.
- Low-resource build: Temurin JDK17.0.20.1, Gradle9.1.0, Kotlin2.3.20, Compose1.10.3, 1 worker, 2 active CPUs, 512MiB build heap / 384MiB metaspace. Kotlin plugin loading centralized at root after the initial duplicate-loader warning; unchanged upstream ModelRequestException warning retained.

## Authorized real service evidence

Latest direct user authorization permits stored Desktop credentials read-only and at most 12 attempts per rolling hour, at least 300 seconds apart across services. Pasted fallback credentials were neither materialized nor needed. No Desktop writes or real user images/Prompt inputs occurred. Exactly two network requests were dispatched, both using a synthetic ceramic-cup request:

1. SiliconFlow Qwen/Qwen3-8B: HTTP200, parsed response length123, response hash in live receipt; one LLM transport probe, not full live Designer acceptance.
2. NovelAI: HTTP200, 27 intermediate frames, one final 232709-byte PNG; original persisted and verified; SHA256 4d3674e9e8f5d30820defc393228a3a0baa83222e6849c0f5a7526de478f3225. Actual request start was 546767ms after LLM, exceeding 300000ms. No retries.

The live probes are explicit test-classpath runners, excluded from the packaged UI. UI remains explicitly SYNTHETIC; live account onboarding/full live Designer workflow is an implementation gap, not PASS. Credentials are unprotected only in memory and not logged, backed up, committed, or shipped. Budget reservation is durable and fail-closed for corrupt/partial history or clock rollback. Real receipts are authoritative; generic run summaries' `realProvider: NOT TESTED` field is a conservative runner default and is not the live result. Likewise earlier zero-JUnit `status: PASS` only meant command success; those originals remain, and the runner now emits COMMAND_SUCCESS for zero-test commands.

## Retained failures and limits

Prior offline/cache/PowerShell failures remain unchanged. One NAI invocation failed at argparse because the `--` separator was missing; exit2 before Gradle/network, then corrected once. This is a launcher failure, not a provider failure or consumed attempt. First staging whitespace warning applied to exact upstream EOF and raw log trailing spaces; those source/evidence bytes were intentionally retained, not reformatted. No test failures have been erased.

Preview UI usability, high-DPI/IME/font quality and long-term data/update acceptance remain separate. See PREVIEW_REVIEW_ITEMS.md. No RC/install-update result is inferred from these checks.

## Native-window observation / automation boundary

First two sandbox launches created JVM state but no targetable window. Third interactive-desktop launch exposed the exact Preview title. Computer Use screenshot failed with `FrameArrived timed out: timed out waiting on channel`; refreshed selection/activation retry failed with `window capture timed out`. Accessibility exposed only window chrome; click failed with `coordinate input geometry is unavailable`. These methods are marked ENVIRONMENT_UNAVAILABLE, not UI PASS. The user supplied a screenshot showing the actual rendered Preview, preserved as user-first-launch.png. It confirms initial visible launch, not mouse workflows or normal exit. The screenshot also revealed black text on dark background; the content-color inheritance is being corrected.

The executor stopped only its own three verified-path initial test process pairs to rebuild. The user correctly clarified that the executor, not the user, closed the window. Those forced terminations do not count as graceful drain/close tests. Subsequent instrumented application self-check must separately prove drawing/controller workflows, normal close and restart. No extra real API requests occurred.

## Completed engineering checks and subsequent UI request

Final P2 regression run 20261020T201211375696Z-final-regression: **84 tests (69 fixed-authority + 15 core), 0 failures/errors/skips**, strict offline dependencies, all relevant tasks rerun, stable source hashes. Original export now uses PNG/JPEG extension matching original signature and still preserves bytes. Preview app image built successfully. The earlier app-internal selfcheck completed 233 draws, synthetic design/revision/generation, two-page editing/undo/redo/order, three export categories and backup; application exited itself with code0. Reopen exited0 with identical state hash and all3 original hashes. These are controller/draw/restart observations, not successful screen-control or human acceptance.

User then requested the whole UI follow **latest CCB Desktop** style, especially a modern title bar with controls. This is active follow-up work; P2 is a pushed engineering checkpoint, not final Preview delivery. Fixed visual reference commit ab3d06abcaca2df8b21c734841d7c4e744099f51 is recorded separately in UI_STYLE_SOURCE.json. It does not alter the 5ef95428 Prompt/request authority. Blue logo and dark-text correction are already included. Final package/review receipt will follow the updated UI selfcheck.
