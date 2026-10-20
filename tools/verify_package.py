#!/usr/bin/env python3
"""Read-only verification of the original CCB Studio Suite initiation package."""
from __future__ import annotations
import argparse
import hashlib
import json
import sys
from pathlib import Path, PurePosixPath

def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--root', type=Path, default=Path(__file__).resolve().parents[1])
    ap.add_argument('--allow-extra', action='store_true', help='Allow unlisted files; listed files must still match.')
    ns = ap.parse_args()
    root = ns.root.resolve(strict=True)
    manifest = root / 'BUNDLE_MANIFEST.json'
    try:
        data = json.loads(manifest.read_text(encoding='utf-8'))
        expected = set()
        errors = []
        for entry in data['files']:
            rel = PurePosixPath(entry['path'])
            if rel.is_absolute() or '..' in rel.parts or not rel.parts or '\\' in str(rel):
                raise ValueError('Unsafe manifest path: ' + str(rel))
            name = rel.as_posix()
            if name in expected:
                raise ValueError('Duplicate manifest path: ' + name)
            expected.add(name)
            p = root.joinpath(*rel.parts)
            # Refuse symlinks so a modified package cannot redirect checks outside its tree.
            if any(x.is_symlink() for x in [p, *p.parents] if x != root and root in x.parents):
                errors.append('SYMLINK: ' + name)
                continue
            if not p.is_file():
                errors.append('MISSING: ' + name)
                continue
            if root not in p.resolve().parents:
                errors.append('OUTSIDE ROOT: ' + name)
                continue
            raw = p.read_bytes()
            if len(raw) != entry['sizeBytes'] or hashlib.sha256(raw).hexdigest() != entry['sha256']:
                errors.append('MISMATCH: ' + name)
        excluded = set(data.get('excludedFromManifest', []))
        found = {p.relative_to(root).as_posix() for p in root.rglob('*') if p.is_file()}
        extra = found - expected - excluded
        if extra and not ns.allow_extra:
            errors.extend('EXTRA: ' + x for x in sorted(extra))
        if errors:
            print('\n'.join(errors))
            print('FAILED: original delivery package differs; do not overwrite later project edits.')
            return 1
        print(f'PASS: {len(expected)} listed files match SHA-256 and size. Product tests were not run.')
        if extra:
            print(f'NOTE: {len(extra)} extra files explicitly allowed.')
        return 0
    except (OSError, ValueError, KeyError, TypeError) as exc:
        print('ERROR: ' + str(exc), file=sys.stderr)
        return 2

if __name__ == '__main__':
    raise SystemExit(main())
