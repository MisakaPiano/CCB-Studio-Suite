"""Integrity check for inventory IDs, references, evidence paths and fixed source coverage."""
import json
from pathlib import Path
root=Path(__file__).resolve().parents[2]
folder=Path(__file__).resolve().parent
coverage=json.loads((folder/'COVERAGE.json').read_text(encoding='utf-8'))
counts={}
for kind,c in coverage['sources'].items():
    value=json.loads((folder/(kind+'-source-ledger.json')).read_text(encoding='utf-8'))
    rows=value['entries']
    assert value['commit']==c['commit']
    assert len(rows)==c['sourceFiles']
    assert len({r['id'] for r in rows})==len(rows)
    assert len({r['path'] for r in rows})==len(rows)
    assert sum(len(r['functions']) for r in rows)==c['functions']
    for r in rows:
        assert len(r['sha256'])==64 and len(r['blob'])==40
        assert all(x['line']>0 for x in r['functions']+r['uiLabels'])
        assert all((root/p).exists() for p in r['suiteTargets']+r['evidence'])
    counts[kind]=len(rows)
suite=json.loads((folder/'suite-added-capabilities.json').read_text(encoding='utf-8'))
assert len(suite['features'])==25
assert len({r['id'] for r in suite['features']})==25
assert len({r['id'] for r in suite['relatedInheritedActions']})==75
for r in suite['features']:
    assert all((root/p).exists() for p in r['suiteTargets']+r['evidence'])
    if r['implementationStatus']!='NOT_IMPLEMENTED':assert r['suiteTargets'],r['id']
print(json.dumps(dict(status='INVENTORY_INTEGRITY_PASS',sources=counts,suiteFeatures=25,inheritedActions=75)))
