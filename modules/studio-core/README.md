# Studio core / Preview foundation

Consumer modules use the unchanged asset-library and fixed image-authority. Workspace ownership covers strict append-only document history, independent assets, projects, generation history, exports and OS-bound secret references. Original records remain immutable; project changes are new commits. No unknown/staged/corrupt document is replaced by defaults. Interrupted jobs become UNCERTAIN and are not resent. Batch edits and order changes are one persisted undo step; missing fonts block final export.

Preview data is explicitly isolated and synthetic. `SYNTHETIC` transport returns deterministic LLM response text and MessagePack image frames through the real Designer/service; no network is reached. Ordinary rendered PNG includes a CCBStudioRecipe iTXt block; privacy rendering uses upstream PrivacyPngEncoder (fresh pixels, hidden LSB sanitization); original export retains exact bytes. Generic source metadata is not all copied into ordinary rendering; original bytes remain accessible. Full faithful external metadata roundtrip is a Preview review gap.

Secrets are separate DPAPI ciphertext, with UUID references only in account documents. Ordinary backups exclude secrets and explicitly state this. Cross-device credential recovery and CP4 app updating remain separate work. No full-app safety claim derives solely from asset-library tests.
