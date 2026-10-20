# STOP — CP2 authoritative image-core dependency

Source-specific evidence: [CORE_DEPENDENCY_GATE.json](../audits/evidence/WINDOWS_RC/CORE_DEPENDENCY_GATE.json). Authority: [formal Gate, boundary 5 and STOP conditions](https://github.com/MisakaPiano/CCB-Studio-Suite/issues/1#issuecomment-6296885017), [Issue #2 CP2](https://github.com/MisakaPiano/CCB-Studio-Suite/issues/2).

The selected reference is `MisakaPiano/ChatChatBar-Desktop@5ef95428421f7c693b010ac88f51d470804ab07a`. Read-only targeted checks found no LICENSE/COPYING/NOTICE filename or README rights statement at that commit, and no published narrow-artifact identity in the provided module/build contract. This is an **unresolved required evidence gap**, not a legal conclusion that no one can grant permission. Ownership of the GitHub fork is not treated as a blanket grant of all upstream/resources' rights.

`app/sharedCore/build.gradle.kts` exposes the whole Kotlin core, not a frozen standalone image-authority SDK. `domain/prompt/NovelAiPromptAuthority.kt` still imports CharacterCard/ChatMessage/PlaceholderRenderer; `domain/image/NovelAiPromptDesigner.kt` additionally imports ModelConfig/chat transports/image-understanding and serialization. These are the specific supplied boundaries already identified by A0 and now rechecked, not a new whole-repository audit.

Under the active Gate, Suite may not repair this by copying literals/serializers into a second implementation, changing fallback, including the live Desktop checkout, or writing Desktop to extract/publish it. A synthetic adapter does not turn this dependency into a proven authoritative implementation. CP2 GEN-01/02/REQ-02/03/12 therefore cannot be marked GREEN, and no Windows RC can be claimed.

## Concrete independent target contract to unblock

Target owner/project: CCB Desktop, separately authorized; Suite does not execute that task here.

1. Identify a lawful, fixed narrow distribution/source closure for the selected Prompt designer/revision authority, NAI request/model/guidance/frame semantics and required metadata DTO/transport ports. Record exact source commit, full paths, transitive closure and all code/resource rights/notices. Alternatively provide explicit rights and a reviewed Suite-local exact-source boundary without changing Desktop.
2. Preserve the original single Prompt/request/fallback authority; separate UI/storage/account ports without editing literals or changing defaults/error/cancellation semantics. Do not publish the complete Desktop app as a cyclic dependency.
3. Supply stable coordinates or a reproducible exact-source artifact with SHA-256, API surface, golden request/Prompt/fallback equivalence tests, and real executed test evidence. Avoid private prompts/tokens or unlicensed catalog/font/reference data in fixtures.
4. Suite then consumes this artifact through a narrow generation adapter, freezes real provider/account/model identity per job and executes mock/loopback parity/failure tests. Real paid tests remain separately authorized.

## Execution consequence

The affected CP2 production dependency is stopped. Independent CP1 preservation/maintenance and the genuine TUF trust lab continue to their verified checkpoint; this is not a request for routine Slice approval. CP3/CP4 application work depends on an unpassed CP2/product foundation and is not silently skipped into GREEN. Overall status is **RC BLOCKED**, pending the external rights/artifact boundary. Any further independently authorized work can resume from the pushed checkpoint without replacing R1 or repeating A0. No cross-project message, code write, formal key or permission change is performed by this document.
