"""Create review-only archives from a verified checkpoint; never read credentials or user roots."""
import argparse, hashlib, json, re, subprocess, zipfile, xml.etree.ElementTree as ET
from pathlib import Path

p=argparse.ArgumentParser();p.add_argument('--checkpoint',required=True);a=p.parse_args()
root=Path(__file__).resolve().parents[1];local=root/'.local/windows-preview'
head=subprocess.check_output(['git','rev-parse','HEAD'],cwd=root,text=True).strip()
assert head==a.checkpoint and re.fullmatch('[0-9a-f]{40}',head)
app=root/'modules/studio-desktop/build/compose/binaries/main/app/CCBStudioSuitePreview'
assert (app/'CCBStudioSuitePreview.exe').is_file()
out=local/'delivery'/head[:12];out.mkdir(parents=True,exist_ok=False)
digest=lambda b:hashlib.sha256(b).hexdigest()
secret=re.compile(rb'(?:pst-|sk-)[A-Za-z0-9_-]{24,}|-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----')
def inspect(name,data):
    assert not secret.search(data), 'Credential-shaped content: '+name
    assert not any(x.lower() in name.lower() for x in ['.dpapi','.ccbsecret','.keystore','.p12','.pfx']),name
def readme():
    return f'''# CCB Studio Suite Windows Preview 0.1.0

Checkpoint: {head}
Branch: feature/windows-rc-continuous
Gate: PREVIEW_GATE_WITH_GAPS. Internal synthetic-data review only; not RC or installer.

Unzip the entire archive to a new directory. Run CCBStudioSuitePreview/CCBStudioSuitePreview.exe.
The bundled JDK17 runtime requires no separate Java installation. Windows x64 only.
No administrator rights, installation, trust-setting change or credentials are needed.
Default data root: %LOCALAPPDATA%/CCBStudioSuitePreview/synthetic-v1 (independent of Desktop).
For a fresh review workspace, use: CCBStudioSuitePreview.exe --synthetic-root <new-absolute-directory>
Use built-in synthetic samples. The UI is explicitly SYNTHETIC and will not make paid calls.
Do not import a real personal library or use this as a long-term storage release.

Review: docs/PREVIEW_REVIEW_ITEMS.md. Modern titlebar uses blue CCB icon, native window controls,
generation/edit/history tabs, compact toolbars, collapsible library and parameter sections.
Visual/mouse/high-DPI/IME acceptance remains NOT TESTED. Real account UI onboarding, full live
Designer roundtrip, rich typography, full metadata export and CP4 real A-to-B update remain gaps.
Normal backup excludes credentials. Restore creates a new directory; no in-place migration.

MANIFEST.json contains every shipped file's size/SHA256; SBOM.json maps packaged dependencies
to locally resolved Maven metadata. Original JAR notices and runtime/legal remain included.
Source: https://github.com/MisakaPiano/CCB-Studio-Suite/tree/{head}
'''.encode()

payload={'README.txt':readme()}
for f in sorted(app.rglob('*')):
    if f.is_file():
        assert not f.is_symlink()
        name='CCBStudioSuitePreview/'+f.relative_to(app).as_posix();b=f.read_bytes();inspect(name,b)
        if f.suffix=='.jar':
            with zipfile.ZipFile(f) as z:
                assert not any('LiveProviderProbe' in n or 'LiveValidation' in n for n in z.namelist()),'Test entry point packaged'
                for n in z.namelist():
                    if not n.endswith('/') and f.name.startswith(('studio-','image-authority-','asset-library-')):inspect(name+'!'+n,z.read(n))
        payload[name]=b

pom_index={}
for base in [local/'maven',local/'gradle-home/caches/modules-2/files-2.1']:
    for f in base.rglob('*.pom'):
        try:
            doc=ET.fromstring(f.read_bytes());ns={'m':'http://maven.apache.org/POM/4.0.0'}
            def value(tag):return doc.findtext('m:'+tag,namespaces=ns) or doc.findtext('m:parent/m:'+tag,namespaces=ns)
            group,artifact,version=value('groupId'),value('artifactId'),value('version')
            if all([group,artifact,version]):pom_index[f'{artifact}-{version}']=(group,artifact,version,doc,f)
        except ET.ParseError:continue
components=[]
for f in sorted((app/'app').glob('*.jar')):
    base=re.sub(r'-[a-f0-9]{20,40}$','',f.stem);component=dict(file=f.name,bytes=f.stat().st_size,sha256=digest(f.read_bytes()))
    if base in pom_index:
        group,artifact,version,doc,pom=pom_index[base]
        component.update(coordinates=f'{group}:{artifact}:{version}',licenses=[dict(name=x.findtext('m:name',namespaces=ns),url=x.findtext('m:url',namespaces=ns)) for x in doc.findall('m:licenses/m:license',ns)])
        payload[f'licenses/maven/{group}/{artifact}/{version}/pom.xml']=pom.read_bytes()
    else:
        assert base in ['studio-desktop','studio-core','image-authority','asset-library'],'Unmapped dependency '+base
        component.update(sourceCommit=head,coordinates='Suite:'+base)
    components.append(component)
payload['SBOM.json']=json.dumps(dict(checkpoint=head,components=components,runtime='Eclipse Temurin 17.0.20.1+1; licenses in runtime/legal',dependencySources=['https://repo.maven.apache.org/maven2/','https://maven.google.com/','https://plugins.gradle.org/m2/','read-only existing public-artifact cache seeded into isolated Suite cache'],verification='Gradle strict SHA256 + locks. Bootstrap hashes pin downloaded bytes; not independent publisher signature verification.'),indent=2).encode()
for f in sorted((root/'docs/rc/licenses').rglob('*')):
    if f.is_file():payload['licenses/'+f.name]=f.read_bytes()
for rel in ['docs/rc/PREVIEW_REVIEW_ITEMS.md','modules/image-authority/SOURCE_MANIFEST.json','modules/studio-desktop/WINDOW_CHROME_SOURCE.json','modules/studio-desktop/src/main/resources/brand/PROVENANCE.json']:
    payload['docs/'+Path(rel).name]=(root/rel).read_bytes()

def archive(kind,files):
    for name,b in files.items():inspect(name,b)
    manifest=dict(task='WINDOWS_PREVIEW_GATE',checkpoint=head,kind=kind,files=[dict(path=n,bytes=len(b),sha256=digest(b)) for n,b in sorted(files.items())])
    manifest_bytes=(json.dumps(manifest,indent=2)+'\n').encode()
    destination=out/f'CCBStudioSuite_{head[:12]}_{kind}.zip'
    with zipfile.ZipFile(destination,'x',zipfile.ZIP_DEFLATED,compresslevel=6) as z:
        for name,b in sorted(files.items()):z.writestr(name,b)
        z.writestr('MANIFEST.json',manifest_bytes)
    with zipfile.ZipFile(destination) as z:
        assert z.testzip() is None
        for item in manifest['files']:
            b=z.read(item['path']);assert len(b)==item['bytes'] and digest(b)==item['sha256']
    (out/(kind+'-manifest.json')).write_bytes(manifest_bytes)
    return dict(checkpoint=head,kind=kind,path=str(destination),name=destination.name,bytes=destination.stat().st_size,sha256=digest(destination.read_bytes()),manifestFiles=len(manifest['files']),localArchiveVerification='PASS')

receipts=[archive('windows-x64-preview',payload)]
evidence={}
for base in [root/'docs/audits/evidence/WINDOWS_PREVIEW',local/'ui-evidence']:
    prefix='published' if base.parts[-1]=='WINDOWS_PREVIEW' else 'raw-ui'
    for f in sorted(base.rglob('*')):
        if f.is_file():evidence[prefix+'/'+f.relative_to(base).as_posix()]=f.read_bytes()
for f in sorted(local.glob('authority-*.log')):evidence['initial-failures/'+f.name]=f.read_bytes()
cup=local/'live-validation/synthetic-live-workspace/assets/objects/4d3674e9e8f5d30820defc393228a3a0baa83222e6849c0f5a7526de478f3225.blob'
assert digest(cup.read_bytes())==cup.stem
evidence['authorized-synthetic-result/ceramic-cup.png']=cup.read_bytes()
receipts.append(archive('raw-evidence',evidence))
(out/'LOCAL_ARTIFACTS.json').write_text(json.dumps(receipts,indent=2)+'\n')
print(json.dumps(receipts,indent=2))
