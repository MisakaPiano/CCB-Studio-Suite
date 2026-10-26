from pathlib import Path
ROOT=Path('H:/CCB-Studio-Suite');OUT=ROOT/'.local/preview-publication-118-20261027';s=(OUT/'prepare-assets.py').read_text(encoding='utf-8-sig')
a=s.index('assert external(old)==external(new)');b=s.index("name='CCB-Studio-Suite-0.0.0-preview.118-windows-x64.exe'")
replacement='''# Build notices from the actual current package. Do not copy stale component metadata.
oldzip=ROOT/'.local/relocation-117-20261027/candidate-preview-117/THIRD_PARTY_NOTICES.zip'
assert sha(oldzip)=='c96a425a18f8b75d2d8939b595fa6809c432b398a9a62000484a8eb300de7b07'
components=[e for e in new['files'] if e['path'].startswith('app/') and e['path'].endswith('.jar')]
entries={}
for item in components:
 jar=C/'payload'/item['path'];assert sha(jar)==item['sha256']
 with zipfile.ZipFile(jar) as archive:
  for name in archive.namelist():
   if name.endswith('/') or not any(word in Path(name).name.lower() for word in ('license','notice','copyright','copying')):continue
   assert '..' not in Path(name).parts and not name.startswith(('/', '\\\\'))
   entries['jar-notices/'+jar.name+'/'+name]=archive.read(name)
for item in new['files']:
 name=item['path'];lower=name.lower()
 if name.startswith('runtime/legal/') or (name.startswith('updater/') and any(word in lower for word in ('license','notice','copyright','copying'))):
  entries['package-notices/'+name]=(C/'payload'/name).read_bytes()
with zipfile.ZipFile(oldzip) as archive:
 for name in archive.namelist():
  if name.startswith('additional/'):
   assert '..' not in Path(name).parts
   entries[name]=archive.read(name)
entries['COMPONENTS.json']=(json.dumps(dict(source=SOURCE,version='0.0.0-preview.118',components=components),indent=2)+'\\n').encode()
entries['README.txt']=b'Third-party notices for Preview 118, generated from the actual fixed payload. Original embedded notices and runtime legal files remain in the installation. Additional pinned Skia/Skiko notices retained. COMPONENTS.json records the current JVM component hashes.\\n'
with zipfile.ZipFile(C/'THIRD_PARTY_NOTICES.zip','x',compression=zipfile.ZIP_DEFLATED) as target:
 for name,data in sorted(entries.items()):target.writestr(name,data)
(OUT/'NOTICES_GENERATION.json').write_text(json.dumps(dict(status='GENERATED_FROM_FIXED118_PAYLOAD',files=len(entries),oldArchiveAdditionalOnly=True,currentComponentHashes=True,installerChanged=False,preparationGuard='Prior exact-path comparison rejected a renamed metadata-only Skiko packaging stub; no stale component table was reused'),indent=2)+'\\n')
'''
s=s[:a]+replacement+s[b:];s=s.replace('thirdPartyLicenseTextsUnchanged=True','noticesGeneratedFromFixedPayload=True');(OUT/'prepare-assets-r2.py').write_text(s,encoding='utf-8')
