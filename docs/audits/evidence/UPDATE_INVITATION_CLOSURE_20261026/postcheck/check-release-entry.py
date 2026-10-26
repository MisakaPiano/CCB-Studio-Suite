from pathlib import Path
import base64,datetime,hashlib,json,sys,urllib.request
ROOT=Path('H:/CCB-Studio-Suite');OUT=ROOT/'.local/update-chain-20261026'
def get(path):
 request=urllib.request.Request('https://api.github.com/'+path,headers={'User-Agent':'CCB-Studio-Suite-readonly-review','Accept':'application/vnd.github+json'})
 with urllib.request.urlopen(request,timeout=25) as response:return json.load(response)
report=dict(atUtc=datetime.datetime.now(datetime.timezone.utc).isoformat(),status='INCOMPLETE',mutations=0,credentialsUsed=False)
try:
 repo=get('repos/MisakaPiano/CCB-Studio-Suite');branch=repo['default_branch']
 releases=[r for r in get('repos/MisakaPiano/CCB-Studio-Suite/releases?per_page=20') if not r['draft'] and r['prerelease']]
 latest=max(releases,key=lambda r:r['published_at']);report['latest']=dict(tag=latest['tag_name'],url=latest['html_url'],preRelease=latest['prerelease'],publishedAt=latest['published_at'],assets=[dict(name=a['name'],bytes=a['size'],digest=a.get('digest')) for a in latest['assets']])
 file=get('repos/MisakaPiano/CCB-Studio-Suite/contents/README.md?ref='+branch);text=base64.b64decode(file['content']).decode('utf-8')
 (OUT/'PUBLIC_MAIN_README.txt').write_text(text,encoding='utf-8')
 report['mainReadme']=dict(branch=branch,blob=file['sha'],sha256=hashlib.sha256(text.encode()).hexdigest(),latestReleasePageLinked=latest['html_url'] in text,allVersionsLinked='https://github.com/MisakaPiano/CCB-Studio-Suite/releases' in text,assetsGuidance='Assets' in text and 'windows-x64.exe' in text,sourceCodeWarning='Source code' in text,experimentalPreview='Preview' in text)
 old=get('repos/MisakaPiano/CCB-Studio-Suite-Releases/contents/README.md');oldtext=base64.b64decode(old['content']).decode('utf-8');(OUT/'PUBLIC_OLD_README.txt').write_text(oldtext,encoding='utf-8')
 report['oldReadme']=dict(blob=old['sha'],newMainReleasesLinked='https://github.com/MisakaPiano/CCB-Studio-Suite/releases' in oldtext)
 report['status']='PASS' if all(report['mainReadme'][key] for key in ('latestReleasePageLinked','allVersionsLinked','assetsGuidance','sourceCodeWarning','experimentalPreview')) and report['oldReadme']['newMainReleasesLinked'] else 'GAP'
except Exception as error:report.update(status='FAILED',error=type(error).__name__,detail=str(error)[:500])
(OUT/'RELEASE_ENTRY_CHECK.json').write_text(json.dumps(report,indent=2),encoding='utf-8');print(json.dumps(report,indent=2))
