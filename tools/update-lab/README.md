# CP1 trusted-update fixture lab

This is an actual python-tuf client test suite over an in-memory repository, not an application installer or a replacement for CP4. Synthetic signers live only in memory. Never serialize them or include keys, real credentials, caches or user data in delivery archives.

On the recorded Windows/Python 3.10 environment, from the Suite root:

```powershell
python -m venv .local/windows-rc/python
.local/windows-rc/python/Scripts/python.exe -m pip --isolated download --index-url https://pypi.org/simple --only-binary=:all: --require-hashes -r tools/update-lab/requirements-win-py310.lock --dest .local/windows-rc/wheels
.local/windows-rc/python/Scripts/python.exe -m pip --isolated install --no-index --find-links .local/windows-rc/wheels --require-hashes -r tools/update-lab/requirements-win-py310.lock
.local/windows-rc/python/Scripts/python.exe tools/update-lab/run.py
```

The pinned wheel lock is specific to this lab's Windows AMD64/Python 3.10 ABI. It does not approve the old host interpreter as a future RC runtime. Do not change global Python, pip, account, proxy or system trust settings to make it work elsewhere; record the environment and produce a separately verified lock/runtime for that target.

Raw run log/summary and disposable synthetic metadata are stored under `.local/windows-rc/`. `run.py` records pre/post source hashes, actual failures/errors/skips and the base commit. The lab opens no ports and makes no network requests after dependency installation. PyPI artifact download provenance is in `docs/audits/evidence/WINDOWS_RC/dependencies/PYTHON_UPDATE_LAB.json`.

The tested custom-field boundary authenticates product/platform/channel/version/data-reader compatibility after TUF verifies the metadata. Installation, data migration, app-wide drain, user confirmation, restart and credential preservation remain NOT TESTED here. See `docs/rc/UPDATE_A_B_PLAN.md` and the active checkpoint ledger.
