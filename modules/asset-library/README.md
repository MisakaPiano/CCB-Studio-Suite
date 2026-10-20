# Asset durability slice

JDK 17 library with no production dependencies. The current consumers are the synthetic import/export, recovery and backup tests, including independent child JVMs. No UI, image codec, CCB Prompt/request or Server code is included. Java keeps this first slice small and callable from future Kotlin consumers; Android compatibility is not tested.

`AssetStore.open(explicitRoot)` owns a cooperative process lock. It only initializes an empty root, requires its own identity marker afterwards, rejects known CCB/Desktop path components and redirected directory paths, and never selects a fallback data root. Do not use a real user library for this experimental slice.

`put(Request, InputStream)` streams at most 256 MiB without decoding. A request contains a stable UUID, opaque display name, original/derived kind, optional parent asset UUID, and separate opaque Recipe/project bytes (at most 1 MiB each). Names never become paths. Derived bytes are a separate asset; same content may share an already verified object while retaining independent associations. Reusing an operation UUID with changed content or associations fails explicitly. Unsealed interrupted requests require explicit recovery handling and cannot be silently replaced by retry.

The private v1 envelope has a bounded payload, type, version and SHA-256; it is not a final Recipe, project or remote protocol schema. Unknown or corrupt envelopes are retained and refused. `ReadResult` separates Missing, Corrupt, ReadError and UnsupportedVersion. Recipe/project bytes are not parsed, migrated or reset.

Commit sequence:

1. Durable transaction request (`transactions/<id>/request.rec`).
2. Stream input to `payload.part`, force file, then write a sealed size/hash/association record.
3. Publish the verified content-addressed `objects/<sha256>.blob` using atomic rename; never overwrite existing mismatched bytes.
4. Publish immutable `records/<id>.rec` — the logical commit point.
5. Update a reconstructible per-record index and completion marker. Failure here returns a committed receipt requiring maintenance, not an uncommitted result.

Recovery explicitly enumerates transaction outcomes, resumes only sealed validated input, and is repeatable. Unsealed bytes remain visible as INCOMPLETE. Records are authoritative; index corruption cannot delete originals. Explicit index rebuilding preserves replaced index bytes under `indexes/observed`. There is no automatic garbage collection or history pruning.

R1 catalog integrity: catalog/rebuild enumerate every immediate entry in `records`, not an extension-filtered subset. Canonical `<uuid>.rec` entries require a valid relationship and original. Unknown names fail with CORRUPT; unreadable entries and future envelopes retain READ_ERROR/UNSUPPORTED_VERSION. A canonical `.pending-<uuid>` with a valid asset envelope reports RECOVERY_REQUIRED, while damaged/unreadable/future pending envelopes retain their specific failures. A staging-shaped name is not proof of ownership or a committed record. Catalog never returns a partial success; rebuild validates the whole scan before writing indexes. All anomalies are retained, and healthy assets remain individually readable/exportable. Transaction recovery can complete an interrupted publication, but does not delete the old staging candidate or clear the catalog warning. Explicit staging inspection/disposition is future maintenance work; there is no automatic promotion or cleanup.

File contents use `FileChannel.force(true)` and publication requires atomic moves. Unsupported atomic moves fail visibly. This is not an arbitrary-power-loss guarantee: directory-entry durability, hardware flush behavior, hostile concurrent path replacement and hard-link attacks are not proven. The lock coordinates cooperating processes, not an OS-wide security boundary.

`close(Duration)` blocks new operations, drains already registered IO and releases ownership only when settled. Timeout or interruption keeps ownership and draining state; call close again after pending work finishes. Do not call close from inside an active callback. `export` reports verification failure and its caller must not publish a partially written output.

R1 backup admission: a registered backup validates the catalog using the internal serialized helper under its existing operation registration. It does not re-enter the public admission gate when close begins draining. New public operations remain refused, and a timed-out close retains the root lock until the backup completes or fails. BACKUP_REGISTERED is a synthetic fault/barrier point before catalog inspection; it does not change the on-disk format. The final manifest plus complete checksum validation remain the success condition, not merely starting or copying part of a backup.

`AssetBackup.create` snapshots only identity/originals/relationships/transaction artifacts, with a final manifest as completion marker; caches, secrets and unrelated root files are excluded. Restore verifies the complete manifest before writing a new root; interrupted restore blocks ordinary opens and can resume only against the same manifest. Different existing target bytes are never replaced. Restore scratch copies are retained separately in `.restore-staging`; backup manifests are checksums, not publisher authentication or encrypted backups. This is a local synthetic recovery API, not a user-facing backup UI or updater.

The backup allowlist excludes separate credential files/directories; it does not inspect secrets embedded by a caller inside opaque Recipe/project bytes or originals. Callers must never place credentials there. File ACLs, original filesystem timestamps and OS-bound secret migration are outside this slice.

Run the bounded tests from the Suite root:

```powershell
powershell.exe -NoProfile -File .\tools\Test-AssetDurability.ps1 -Root H:\CCB-Studio-Suite
```

The offline helper uses an existing Gradle 9.1.0 and two pinned cached test JARs, copies them into `.local/a1-001/test-maven`, and isolates Gradle writes/fixtures under `.local/a1-001`. It never edits the source cache. Pass explicit `-GradleCommand` and `-TestArtifactCache` paths on another machine. Standard Gradle builds can resolve the same coordinates from Maven Central without that optional local-repository argument. Test results include real JUnit XML, logs and source hashes; absence of a runtime/cache is a failure, not a fake PASS.
