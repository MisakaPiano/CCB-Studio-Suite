"""Explicitly approved two-release publication. No overwrite/delete or secret output."""
import base64
import hashlib
import json
import os
import subprocess
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

folder = Path(__file__).resolve().parent
review = folder / 'publication-review'
manifest = json.loads((review / 'PUBLICATION_MANIFEST.json').read_text())
full = 'MisakaPiano/CCB-Studio-Suite-Releases'
assert manifest['repository'] == full
prefix = '/repos/' + full
class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *args, **kwargs): return None
opener = urllib.request.build_opener(NoRedirect)
env = dict(os.environ, GIT_TERMINAL_PROMPT='0', GCM_INTERACTIVE='never')
credential = subprocess.run(['git', 'credential', 'fill'], input='protocol=https\nhost=github.com\n\n', text=True, capture_output=True, env=env, check=True)
fields = dict(line.split('=', 1) for line in credential.stdout.splitlines() if '=' in line)
token = fields.get('password')
assert token
def api(path, method='GET', value=None, binary=None):
    assert path.startswith('/') and not path.startswith('//')
    host = 'uploads.github.com' if binary is not None else 'api.github.com'
    data = binary if binary is not None else (None if value is None else json.dumps(value).encode())
    headers = {'Authorization': 'Bearer ' + token, 'Accept': 'application/vnd.github+json', 'User-Agent': 'CCB-Suite-approved-channel-test', 'X-GitHub-Api-Version': '2026-03-10'}
    if data is not None:
        headers['Content-Type'] = 'application/octet-stream' if binary is not None else 'application/json'
    req = urllib.request.Request('https://' + host + path, data=data, method=method, headers=headers)
    with opener.open(req, timeout=180) as r: return json.load(r)
def optional(path):
    try: return api(path)
    except urllib.error.HTTPError as e:
        if e.code == 404: return None
        raise
receipt = {'repository': full, 'releases': [], 'status': 'IN_PROGRESS'}
def save():
    (review / 'PUBLICATION_RESULT.json').write_text(json.dumps(receipt, indent=2), encoding='utf-8')
try:
    assert api('/user')['login'].lower() == 'misakapiano'
    source_private = api('/repos/MisakaPiano/CCB-Studio-Suite')['private']
    repo = api(prefix)
    assert repo['id'] == 1429556531 and repo['private'] is False
    default = api(prefix + '/git/ref/heads/' + repo['default_branch'])['object']['sha']
    body = (review / 'TEST_RELEASE_NOTES.md').read_text(encoding='utf-8')
    for item in manifest['installers']:
        tag = item['tag']
        matches = [r for r in api(prefix + '/releases?per_page=100') if r['tag_name'] == tag]
        assert len(matches) <= 1
        release = matches[0] if matches else api(prefix + '/releases', 'POST', {'tag_name': tag, 'target_commitish': default, 'name': tag + ' — 更新链路测试（非日常版）', 'body': body, 'draft': True, 'prerelease': True, 'make_latest': 'false'})
        assert release['prerelease'] and release['body'] == body
        version = tag.rsplit('.', 1)[1]
        assets = [(item, folder / ('build-' + version) / 'studio-installer.exe')]
        assets += [(sidecar, review / sidecar['asset']) for sidecar in manifest['sidecars']]
        verified = []
        for spec, path in assets:
            data = path.read_bytes()
            assert len(data) == spec['bytes'] and hashlib.sha256(data).hexdigest() == spec['sha256']
            existing = [a for a in api(prefix + '/releases/' + str(release['id']) + '/assets?per_page=100') if a['name'] == spec['asset']]
            assert len(existing) <= 1
            asset = existing[0] if existing else api(prefix + '/releases/' + str(release['id']) + '/assets?name=' + urllib.parse.quote(spec['asset']), 'POST', binary=data)
            assert asset['state'] == 'uploaded' and asset['size'] == len(data) and asset['digest'] == 'sha256:' + spec['sha256']
            verified.append({'id': asset['id'], 'name': asset['name'], 'sha256': spec['sha256'], 'url': asset['browser_download_url']})
            print(json.dumps({'assetVerified': spec['asset']}), flush=True)
        if release['draft']:
            release = api(prefix + '/releases/' + str(release['id']), 'PATCH', {'draft': False, 'prerelease': True, 'make_latest': 'false'})
        public_assets = api(prefix + '/releases/' + str(release['id']) + '/assets?per_page=100')
        for item_verified in verified:
            actual = next(a for a in public_assets if a['id'] == item_verified['id'])
            expected_url = 'https://github.com/' + full + '/releases/download/' + tag + '/' + item_verified['name']
            assert actual['browser_download_url'] == expected_url
            item_verified['url'] = expected_url
        receipt['releases'].append({'id': release['id'], 'tag': tag, 'url': release['html_url'], 'assets': verified})
        save()
    current = json.loads((folder / 'metadata-store/CURRENT.json').read_text())
    generation = folder / 'metadata-store/generations' / current['generation']
    branch = optional(prefix + '/git/ref/heads/update-metadata')
    if branch:
        for item in manifest['metadata']:
            remote = api(prefix + '/contents/' + item['path'] + '?ref=update-metadata')
            assert hashlib.sha256(base64.b64decode(remote['content'])).hexdigest() == item['sha256']
        commit_sha = branch['object']['sha']
    else:
        tree_items = []
        for item in manifest['metadata']:
            data = (generation / Path(item['path']).name).read_bytes()
            assert hashlib.sha256(data).hexdigest() == item['sha256']
            blob = api(prefix + '/git/blobs', 'POST', {'content': base64.b64encode(data).decode(), 'encoding': 'base64'})
            tree_items.append({'path': item['path'], 'mode': '100644', 'type': 'blob', 'sha': blob['sha']})
        base_tree = api(prefix + '/git/commits/' + default)['tree']['sha']
        tree = api(prefix + '/git/trees', 'POST', {'base_tree': base_tree, 'tree': tree_items})
        commit = api(prefix + '/git/commits', 'POST', {'message': 'test: publish approved preview 100 to 101 signed metadata', 'tree': tree['sha'], 'parents': [default]})
        commit_sha = commit['sha']
        api(prefix + '/git/refs', 'POST', {'ref': 'refs/heads/update-metadata', 'sha': commit_sha})
    for item in manifest['metadata']:
        remote = api(prefix + '/contents/' + item['path'] + '?ref=' + commit_sha)
        assert hashlib.sha256(base64.b64decode(remote['content'])).hexdigest() == item['sha256']
    assert api('/repos/MisakaPiano/CCB-Studio-Suite')['private'] == source_private
    receipt.update(status='PUBLISHED_API_VERIFIED', metadataCommit=commit_sha, sourceVisibilityUnchanged=True, endToEnd='NOT_YET_TESTED')
    save()
    print(json.dumps({'status': receipt['status'], 'metadataCommit': commit_sha}), flush=True)
except Exception as e:
    receipt.update(status='FAILED_RETAINED', errorType=type(e).__name__, httpCode=getattr(e, 'code', None))
    save()
    print(json.dumps({'status': receipt['status'], 'errorType': type(e).__name__, 'httpCode': getattr(e, 'code', None)}), flush=True)
    raise SystemExit(1)
