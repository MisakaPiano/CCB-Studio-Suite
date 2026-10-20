# CP1 — internal Windows A→B plan and exercised trust scaffold

This is the CP1 foundation, **not an application installer, actual A→B exercise or CP4 PASS**. The synthetic target payload explicitly says it is not an application. Keys used by the lab are ephemeral memory-only Ed25519 signers, never production identities, exported private keys or OS trust changes.

## Selected lab and sources

The lab uses `tuf==7.0.1` through its real `tuf.ngclient.Updater`, with a supplied immutable bootstrap root. Upstream performs role/signature, version, expiry and downloaded-byte validation; Suite does not implement a TUF serializer or substitute a signed JSON format. [Upstream client API](https://theupdateframework.readthedocs.io/en/latest/api/tuf.ngclient.updater.html), [deployment/bootstrap requirements](https://theupdateframework.readthedocs.io/en/stable/INSTALLATION.html), [version source](https://pypi.org/project/tuf/7.0.1/).

The authenticated target custom fields in this lab bind product, Windows architecture, channel, monotonically newer application version and supported data-reader range. A fake in-memory transport supplies only synthetic bytes from `updates.invalid`; it opens no listener or real provider connection. Tests include authentic download, wrong signature, expired/frozen metadata, persisted-cache rollback, changed payload, disconnect, signed wrong identity/unsupported reader, and root rotation requiring both old and new root thresholds.

The exact seven-wheel Windows/Python-3.10 lab lock is [requirements](../../tools/update-lab/requirements-win-py310.lock); artifact URL/size/SHA-256, PyPI comparison and download count are in [provenance](../audits/evidence/WINDOWS_RC/dependencies/PYTHON_UPDATE_LAB.json). Each wheel was checked against PyPI's published digest before hash-enforced offline installation into Suite `.local`. This is identity verification via TLS/PyPI, not an independent publisher-signature audit of every dependency. It does not approve the host's old Python 3.10.6 as an RC-bundled runtime.

Alternative evaluated: update4j 1.5.9 supports signed configurations/file updates, but the upstream repository is archived and its complete current security-maintenance/rollback/freshness fit was not established. No update4j binary was downloaded/adopted. [Official repository and maintenance state](https://github.com/update4j/update4j). The GitHub connector rejected a tags endpoint; the official repository and documentation were used instead. MSIX/App Installer remains an OS-install candidate; a need to alter host certificate trust is a STOP, not permission to do so.

## Required actual application exercise before any RC

1. Supply a separately reviewed production bootstrap/runtime and dependency notices; package actual Compose Studio builds A and B, not a text payload or the library test harness. Bind fixed source SHA, application/data/metadata versions, platform and internal channel. No production signing identity is implied.
2. Use an isolated Suite-owned application installation directory and independent synthetic data root. A must persist originals, full Recipe/design history, editable projects/settings and synthetic OS-protected test credentials; record before hashes/identity without exporting credential bytes or encryption material.
3. A's own UI must discover B, show version/channel/notes, obtain confirmation, download through the real trusted client, validate bytes/identity, save/close or drain all writers/jobs, hand off to the chosen installer and restart B. Test the complete application path, not only this lab or a separate script.
4. B must issue an authenticated installation/start/data-compatibility result and retain each original/document/credential. Unknown/new data formats must make old binaries refuse writes. Migration journals/snapshots retain source and all work created after upgrade; binary rollback must never silently restore an old snapshot over newer work.
5. Exercise actual bad signature/package/product/platform/channel, cancellation/disconnect, insufficient space, installer failure, drain timeout, migration interruption, failed B startup and failed restore. Include active generation/edit/save handoff with mocked providers and no blind paid retry. Keep original FAILED results.
6. Do not change system trust, use real user roots, read existing credentials or generate formal keys. If the chosen installer requires those steps, stop and record the exact environment/authorization blocker. Portable and installed layouts need their own proven handoff; file-copy assertions alone do not establish installation.

## Current boundary

The eight trust-lab tests are actual automation. Windows installation, restart, app-wide drain, credential preservation, native UI confirmation and post-upgrade work preservation are **NOT TESTED**. This scaffold satisfies only the CP1 plan/lab portion; REQ-10/REQ-11 and UPD-01–05 remain RC gates. No RC package is produced by the lab.
