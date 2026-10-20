"""Copy public dependency artifacts, never credentials/config, to Suite's isolated Maven cache."""
import hashlib, json, shutil
from pathlib import Path
root=Path(__file__).resolve().parents[1]
cache=Path.home()/'.gradle/caches/modules-2/files-2.1'
dest=root/'.local/windows-preview/maven'
prefixes=('org.jetbrains','androidx.','com.squareup','org.msgpack','net.java.dev.jna','junit','org.hamcrest','org.slf4j','org.sonatype','com.google.code.findbugs','com.google.errorprone','com.google.j2objc')
manifest=[]
for group in sorted(cache.iterdir()):
    if not group.name.startswith(prefixes): continue
    for f in sorted(group.glob('*/*/*/*')):
        if not f.is_file() or f.suffix not in ('.jar','.pom','.module'): continue
        art,version=f.relative_to(group).parts[:2]
        target=dest/Path(group.name.replace('.','/'))/art/version/f.name
        h=hashlib.sha256(f.read_bytes()).hexdigest()
        if target.exists() and hashlib.sha256(target.read_bytes()).hexdigest()!=h: raise RuntimeError('Ambiguous '+str(target))
        target.parent.mkdir(parents=True,exist_ok=True)
        if not target.exists(): shutil.copyfile(f,target)
        manifest.append(dict(group=group.name,artifact=art,version=version,file=f.name,size=f.stat().st_size,sha256=h,source='existing public Gradle artifact cache; no settings or credentials copied'))
(root/'.local/windows-preview/cache-seed.json').write_text(json.dumps(manifest,indent=2)+'\n')
print(json.dumps(dict(files=len(manifest),bytes=sum(x['size'] for x in manifest),networkDownloadBytes=0)))
