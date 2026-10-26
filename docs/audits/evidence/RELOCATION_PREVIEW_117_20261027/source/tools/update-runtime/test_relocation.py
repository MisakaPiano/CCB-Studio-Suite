"""New move-specific safety checks; native real-volume cases are separate evidence."""
import json,os,shutil,unittest
from pathlib import Path
from install_ops import atomic_json
from residue_cleanup import run,relocation_projection
from studio_update import UpdateRejected
from test_residue_cleanup import ResidueCleanupTests


class RelocationCleanupTests(ResidueCleanupTests):
    # Only explicitly selected methods below are run, not inherited historical cases.
    def prepare_contained_profile(self):
        old_data=self.data;old_runtime=self.runtime
        self.data=self.root/'data/workspace';self.data.parent.mkdir()
        self.runtime=self.root/'data/runtime'
        shutil.move(old_data,self.data);shutil.move(old_runtime,self.runtime)
        def replace(v):
            if isinstance(v,dict):return {k:replace(x) for k,x in v.items()}
            if isinstance(v,list):return list(map(replace,v))
            if isinstance(v,str):
                for a,b in ((old_data,self.data),(old_runtime,self.runtime)):
                    if v==str(a) or v.startswith(str(a)+os.sep):return str(b)+v[len(str(a)):]
            return v
        for p in self.root.rglob('*.json'):atomic_json(p,replace(json.loads(p.read_text())))
        self.cache=self.runtime/'updates';self.folder=self.cache/'handoffs/fixture'

    def relocate(self):
        origin=self.root;destination=self.base/'renamed'
        before={p.relative_to(origin):p.read_bytes() for p in (self.folder/'plan.json',self.folder/'result.json',self.cache/'install-current.json')}
        shutil.move(origin,destination);self.root=destination
        self.data=destination/'data/workspace';self.runtime=destination/'data/runtime';self.cache=self.runtime/'updates'
        self.old=destination/self.old.name;self.current=destination/self.current.name;self.folder=self.cache/'handoffs/fixture'
        atomic_json(destination/'launch-current.json',dict(installed=str(self.current),data=str(self.data),runtime=str(self.runtime),previousRoots=[str(origin)]))
        return before

    def test_relocated_complete_cleans_owned_bytes_preserves_unknown_and_protected(self):
        self.prepare_contained_profile()
        protected,expected=self.program(111,'d')
        atomic_json(self.root/'ACTIVE_INSTALLATION.json',dict(program=str(protected),workspace=str(self.data)))
        (self.old/'unknown.txt').write_bytes(b'not updater owned')
        before=self.relocate();protection=(self.root/'ACTIVE_INSTALLATION.json').read_bytes()
        value=self.cleanup();self.assertEqual(value['status'],'COMPLETE');self.assertGreater(value['removed'],0)
        self.assertEqual((self.old/'unknown.txt').read_bytes(),b'not updater owned')
        self.assertTrue((self.root/protected.name/'retired.dll').is_file())
        for name,content in before.items():self.assertEqual((self.root/name).read_bytes(),content)
        self.assertEqual((self.root/'ACTIVE_INSTALLATION.json').read_bytes(),protection)
        self.assertEqual(self.cleanup()['removed'],0)

    def test_relocated_incomplete_handoff_retains_predecessor(self):
        self.prepare_contained_profile();atomic_json(self.folder/'result.json',dict(status='WAITING_FOR_B'))
        self.relocate();self.assertEqual(self.cleanup()['status'],'RETAINED_NO_COMPLETE_HANDOFF')
        self.assertTrue((self.old/'retired.dll').is_file())

    def test_relocated_protection_record_still_blocks_old_program(self):
        self.prepare_contained_profile()
        atomic_json(self.root/'ACTIVE_INSTALLATION.json',dict(program=str(self.old)))
        self.relocate();self.cleanup();self.assertTrue((self.old/'retired.dll').is_file())

    def test_relocation_history_rejects_drive_root_overlap_and_ambiguity(self):
        for origins in ([str(self.root.anchor)],[str(self.root.parent)],[str(self.root/'child')],['relative'],['x']*17):
            with self.assertRaises(UpdateRejected):relocation_projection(self.root,dict(previousRoots=origins))
        project=relocation_projection(self.root,dict(previousRoots=[str(self.base/'old'),str(self.base/'old/nested')]))
        with self.assertRaises(UpdateRejected):project(str(self.base/'old/nested/file'))

    def test_relocated_receipt_does_not_relax_modified_file_hash(self):
        self.prepare_contained_profile();self.relocate()
        changed=self.old/'retired.dll';changed.write_bytes(b'manually changed original')
        self.cleanup();self.assertEqual(changed.read_bytes(),b'manually changed original')
