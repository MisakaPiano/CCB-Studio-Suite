import json,hashlib,os,uuid,unittest,importlib.util
from pathlib import Path
from unittest.mock import patch
from internal_installer_inputs import validate_sources,validate_image
ROOT=Path(__file__).resolve().parents[2]

class InternalInputLockTests(unittest.TestCase):
    def setUp(self):
        self.base=Path(os.environ['CCB_SYNTHETIC_TEST_ROOT'])/('input-lock-'+uuid.uuid4().hex);self.base.mkdir()
    def test_source_inventory_handles_unicode_and_rejects_omission_or_changed_bytes(self):
        f=self.base/'tools/工具.py';f.parent.mkdir();f.write_bytes(b'original')
        lock=self.base/'lock.json'
        value=dict(format=1,kind='INTERNAL_SOURCE_SNAPSHOT',baseCommit='a'*40,files={'tools/工具.py':hashlib.sha256(f.read_bytes()).hexdigest()})
        lock.write_text(json.dumps(value))
        def git(args,**kw):
            return 'a'*40 if args[1]=='rev-parse' else 'tools/工具.py\0'.encode('utf-8')
        with patch('internal_installer_inputs.subprocess.check_output',side_effect=git):
            self.assertEqual(validate_sources(self.base,lock),value)
            value['files']={};lock.write_text(json.dumps(value))
            with self.assertRaises(ValueError):validate_sources(self.base,lock)
            value['files']={'tools/工具.py':'0'*64};lock.write_text(json.dumps(value))
            with self.assertRaises(ValueError):validate_sources(self.base,lock)
    def test_frozen_image_rejects_changed_or_extra_program_files(self):
        app=self.base/'app';app.mkdir();exe=app/'CCBStudioSuitePreview.exe';exe.write_bytes(b'synthetic')
        lock=self.base/'image.json';value=dict(format=1,kind='VERIFIED_APPLICATION_IMAGE',sourceSha='b'*40,
            files={exe.name:dict(bytes=9,sha256=hashlib.sha256(b'synthetic').hexdigest())})
        lock.write_text(json.dumps(value));self.assertEqual(validate_image(app,lock),value)
        extra=app/'unexpected.dll';extra.write_bytes(b'unknown')
        with self.assertRaises(ValueError):validate_image(app,lock)
        # Restore only this synthetic unknown filename via a new clean test image,
        # without cleanup of the retained failure sample.
        app2=self.base/'app2';app2.mkdir();(app2/exe.name).write_bytes(b'wrongdata')
        with self.assertRaises(ValueError):validate_image(app2,lock)
    def test_public_profile_cannot_use_uncommitted_internal_input_lock(self):
        spec=importlib.util.spec_from_file_location('builder_lock',ROOT/'tools/Build-InternalInstaller.py');module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
        dest=ROOT/'.local'/('reject-public-'+uuid.uuid4().hex)
        profile=dict(channel='preview',displayVersion='0.0.0-preview.120',githubRepository='MisakaPiano/CCB-Studio-Suite',metadataBranch='long-term-metadata')
        with self.assertRaisesRegex(ValueError,'internal-only'):
            module.build(Path('unused'),Path('unused'),Path('unused'),dest,120,
                'https://raw.githubusercontent.com/MisakaPiano/CCB-Studio-Suite/long-term-metadata/preview/windows-x86_64/metadata/',
                'https://github.com/MisakaPiano/CCB-Studio-Suite/releases/download/',Path('unused'),
                profile=profile,internal_source_lock=Path('unused'),frozen_image_manifest=Path('unused'))
        self.assertFalse(dest.exists())
