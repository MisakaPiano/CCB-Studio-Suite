"""Explicit internal-only input locks. Public/default packaging remains commit-gated."""
from pathlib import Path,PurePosixPath
import hashlib,json,re,subprocess
def digest(path):return hashlib.sha256(path.read_bytes()).hexdigest()

SOURCE_PATHS=['modules','tools','gradle','gradlew','gradlew.bat','build.gradle.kts','settings.gradle.kts','gradle.properties']
PACKAGING_REQUIRED=['tools/Build-InternalInstaller.py','tools/Prepare-UpdaterRuntime.py','tools/installer_repair_ui.py',
    'tools/internal_installer_inputs.py','tools/windows-launcher/Launcher.cs','modules/studio-desktop/src/main/resources/brand/ccb-blue.ico',
    *['tools/update-runtime/'+name+'.py' for name in ('studio_update','github_transport','apply_update','install_ops','repair_install','residue_cleanup','owned_files')]]

def validate_committed_sources(root):
    """All package source areas, including newly imported generators and helpers.
    Non-source inputs (runtime wheels/NSIS/bootstrap/app image) retain their
    separate manifest/archive/build-evidence verification in the builder.
    """
    changed=subprocess.check_output(['git','diff','HEAD','--name-only','-z','--',*SOURCE_PATHS],cwd=root)
    untracked=subprocess.check_output(['git','ls-files','--others','--exclude-standard','-z','--',*SOURCE_PATHS],cwd=root)
    if changed or untracked:raise ValueError('Commit package inputs before building a fixed installer (modified or untracked)')
    # Mandatory executable/generator inputs must be tracked, even if ignored.
    subprocess.check_output(['git','ls-files','--error-unmatch','--',*PACKAGING_REQUIRED],cwd=root,stderr=subprocess.STDOUT)
    return subprocess.check_output(['git','rev-parse','HEAD'],cwd=root,text=True).strip()
def safe_relative(root,name):
    path=PurePosixPath(name)
    if not name or path.as_posix()!=name or path.is_absolute() or any(p in ('.','..') for p in path.parts) or '\\' in name or ':' in name:
        raise ValueError('Unsafe locked input path')
    result=root.joinpath(*path.parts)
    for p in [result,*result.parents]:
        if p.is_symlink() or p.is_junction():raise ValueError('Linked input refused')
        if p==root:break
    return result
def source_files(root):
    names=subprocess.check_output(['git','ls-files','-z','--cached','--others','--exclude-standard','--',
        *SOURCE_PATHS],cwd=root).decode('utf-8').split('\0')
    return sorted(n for n in names if n and safe_relative(root,n).is_file())
def validate_sources(root,path):
    value=json.loads(path.read_text(encoding='utf-8'))
    head=subprocess.check_output(['git','rev-parse','HEAD'],cwd=root,text=True).strip()
    if value.get('format')!=1 or value.get('kind')!='INTERNAL_SOURCE_SNAPSHOT' or value.get('baseCommit')!=head:
        raise ValueError('Wrong internal source snapshot identity')
    expected=value.get('files',{})
    if set(expected)!=set(source_files(root)):raise ValueError('Internal snapshot must cover the complete package-source set')
    for name,hashed in expected.items():
        if not re.fullmatch('[0-9a-f]{64}',hashed) or digest(safe_relative(root,name))!=hashed:raise ValueError('Internal source input changed: '+name)
    return value
def validate_image(app,path):
    value=json.loads(path.read_text(encoding='utf-8'))
    if value.get('format')!=1 or value.get('kind')!='VERIFIED_APPLICATION_IMAGE' or not re.fullmatch('[0-9a-f]{40}',value.get('sourceSha','')):
        raise ValueError('Wrong frozen app-image identity')
    expected=value.get('files',{})
    actual={p.relative_to(app).as_posix() for p in app.rglob('*') if p.is_file()}
    if set(expected)!=actual or 'CCBStudioSuitePreview.exe' not in expected:raise ValueError('Frozen app-image inventory differs')
    for name,item in expected.items():
        p=safe_relative(app,name)
        if p.stat().st_size!=item['bytes'] or digest(p)!=item['sha256']:raise ValueError('Frozen app image changed: '+name)
    return value
