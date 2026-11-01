"""A-NR01 only: inert files, genuine publisher journals, no installer or COM."""
import hashlib,json,os,subprocess,uuid,unittest
from pathlib import Path
from types import SimpleNamespace
from unittest.mock import patch
import apply_update as au

class RecoveryIntegrityTests(unittest.TestCase):
    def setUp(self):
        self.base=Path(os.environ['CCB_SYNTHETIC_TEST_ROOT'])/('recovery-'+uuid.uuid4().hex);self.base.mkdir()
        self.results=[]
    def fixture(self,phase='committed',absent=False):
        self.root=self.base/uuid.uuid4().hex;self.root.mkdir()
        final=self.root/'new';(final/'updater').mkdir(parents=True)
        data=self.root/'data';data.mkdir();runtime=self.root/'runtime';runtime.mkdir()
        entry=dict(format=1,version=1,installed=str(self.root/'old'),data=str(data),runtime=str(runtime))
        au.atomic_json(self.root/'launch-current.json',entry)
        if not absent:
            (self.root/'CCBStudioLauncher.exe').write_bytes(b'synthetic old exe')
            (self.root/'Launch-CCB-Studio.lnk').write_bytes(b'synthetic old lnk')
        (final/'CCBStudioSuitePreview.exe').write_bytes(b'inert main never executed')
        (final/'updater/CCBStudioLauncher.exe').write_bytes(b'synthetic new exe')
        (final/'update-config.json').write_text(json.dumps(dict(version=2)))
        self.folder=self.root/'handoff';self.folder.mkdir();self.nonce='a'*64
        replace=au.os.replace
        def prepare(args,**kw):
            self.assertEqual(args[1],'--prepare-shortcut');Path(args[2]).write_bytes(b'synthetic new lnk');return SimpleNamespace(returncode=0)
        def failure(source,target):
            if phase=='failed-restored' and Path(source).name=='launcher.prepared.lnk':raise OSError('synthetic lnk failure')
            return replace(source,target)
        with patch('apply_update.subprocess.run',side_effect=prepare),patch('apply_update.os.replace',side_effect=failure):
            if phase=='failed-restored':
                with self.assertRaises(OSError):au.create_launcher(final,data,runtime,expected_current=entry,publication_admission=lambda:dict(nonce=self.nonce,folder=str(self.folder)))
            else:au.create_launcher(final,data,runtime,expected_current=entry,publication_admission=lambda:dict(nonce=self.nonce,folder=str(self.folder)))
        self.journal=next(self.root.glob('.entrypoint-*/journal.json'))
        self.assertEqual(json.loads(self.journal.read_text())['status'],'COMMITTED' if phase=='committed' else 'FAILED_RESTORED')
    def snapshot(self):
        value={}
        for p in self.root.rglob('*'):
            if p.is_symlink() or p.is_junction():value[str(p.relative_to(self.root))]='REPARSE';continue
            if p.is_file():
                with p.open('rb') as stream:value[str(p.relative_to(self.root))]=hashlib.file_digest(stream,'sha256').hexdigest()
        return value
    def check(self,case,expected='ENTRYPOINT_RECOVERY_REQUIRED'):
        before=self.snapshot();actual=au.interrupted_publication_status(self.folder,self.root,self.nonce)
        self.assertEqual(actual,expected,case);self.assertEqual(self.snapshot(),before,case)
        self.results.append(dict(case=case,status=actual,unchanged=True))
    def tearDown(self):
        (self.base/'cases.json').write_text(json.dumps(self.results,indent=2),encoding='utf-8')
    def test_intact_and_missing_or_corrupt_files_for_both_journal_states(self):
        for phase in ['committed','failed-restored']:
            self.fixture(phase);success='COMPLETE_RECEIPT_RECOVERED' if phase=='committed' else 'FAILED_RETAINED'
            self.check(phase+' intact',success)
            for name in ['CCBStudioLauncher.exe','Launch-CCB-Studio.lnk']:
                path=self.root/name;body=path.read_bytes()
                for action in ['missing','empty','different']:
                    if action=='missing':path.unlink()
                    else:path.write_bytes(b'' if action=='empty' else b'synthetic wrong bytes')
                    self.check(phase+' '+name+' '+action);path.write_bytes(body)
    def test_absent_originals_must_still_be_absent(self):
        self.fixture('failed-restored',absent=True);self.check('original entries absent','FAILED_RETAINED')
        for name in ['CCBStudioLauncher.exe','Launch-CCB-Studio.lnk']:
            path=self.root/name;path.write_bytes(b'synthetic unexpected file')
            self.check(name+' reappeared');path.unlink()
        self.check('both remain absent','FAILED_RETAINED')
    def test_hash_missing_bad_type_and_arbitrary_names_are_not_proof(self):
        for phase in ['committed','failed-restored']:
            self.fixture(phase);original=self.journal.read_bytes()
            changes=[('files missing',lambda j:j.pop('files')),('files null',lambda j:j.update(files=None)),
                ('files empty',lambda j:j.update(files={})),('arbitrary path',lambda j:j['files'].update({'../outside.exe':j['files']['CCBStudioLauncher.exe']}))]
            for name in ['CCBStudioLauncher.exe','Launch-CCB-Studio.lnk']:
                for key in ['beforeSha256','desiredSha256']:
                    changes.append((name+' missing '+key,lambda j,n=name,k=key:j['files'][n].pop(k)))
                    for bad in [True,17,[],{},'abc','A'*64]:
                        changes.append((name+' bad '+key+' '+repr(bad),lambda j,n=name,k=key,b=bad:j['files'][n].update({k:b})))
                changes.append((name+' null desired',lambda j,n=name:j['files'][n].update(desiredSha256=None)))
                changes.append((name+' extra path',lambda j,n=name:j['files'][n].update(path='../outside')))
            for case,change in changes:
                value=json.loads(original);change(value);self.journal.write_text(json.dumps(value))
                self.check(phase+' '+case);self.journal.write_bytes(original)
            self.check(phase+' valid proof restored','COMPLETE_RECEIPT_RECOVERED' if phase=='committed' else 'FAILED_RETAINED')
    def test_metadata_format_missing_oversize_and_foreign_transaction_rejected(self):
        self.fixture();original=self.journal.read_bytes()
        for body in [b'{}',b'[]',b'null',b'not-json',b' '*65537]:
            self.journal.write_bytes(body);self.check('bad journal '+str(len(body)));self.journal.write_bytes(original)
        for key,bad in [('format',True),('format',2),('commitPoint','../foreign.json'),('admission',{}),('status','PREPARING')]:
            value=json.loads(original);value[key]=bad;self.journal.write_text(json.dumps(value));self.check('journal '+key);self.journal.write_bytes(original)
        value=json.loads(original);value.pop('files');value['status']='FAILED_RESTORED'
        value['previous']=json.loads((self.root/'launch-current.json').read_text())
        self.journal.write_text(json.dumps(value));self.check('failed before file proof recorded');self.journal.write_bytes(original)
        self.journal.unlink();self.check('missing journal');self.journal.write_bytes(original)
        marker=self.folder/'PUBLICATION_ADMITTED.json';original_marker=marker.read_bytes()
        for key,bad in [('format',True),('transaction',str(self.base/('.entrypoint-'+'a'*32))),('transaction','relative'),('transaction',17),('nonce','b'*64)]:
            value=json.loads(original_marker);value[key]=bad;marker.write_text(json.dumps(value));self.check('marker '+key+' '+repr(bad));marker.write_bytes(original_marker)
        marker.write_bytes(b' '*16385);self.check('oversized marker');marker.write_bytes(original_marker)
        descriptor=self.root/'launch-current.json';body=descriptor.read_bytes();descriptor.write_bytes(b' '*16385)
        self.check('oversized descriptor');descriptor.write_bytes(body)
    def test_oversize_entry_is_rejected_without_unbounded_read(self):
        self.fixture()
        for name,limit in [('CCBStudioLauncher.exe',16*1024**2),('Launch-CCB-Studio.lnk',1024**2)]:
            path=self.root/name;original=path.read_bytes()
            with path.open('wb') as stream:stream.truncate(limit+1)
            self.check(name+' oversized');path.write_bytes(original)
        self.check('intact after own synthetic restoration','COMPLETE_RECEIPT_RECOVERED')
    def test_directory_reparse_entries_and_transaction_are_rejected(self):
        self.fixture();target=self.base/'junction-target';target.mkdir();(target/'sentinel').write_bytes(b'synthetic untouched')
        for name in ['CCBStudioLauncher.exe','Launch-CCB-Studio.lnk']:
            path=self.root/name;original=path.read_bytes();path.unlink()
            command=subprocess.run(['cmd.exe','/d','/c','mklink','/J',str(path),str(target)],capture_output=True)
            self.assertEqual(command.returncode,0,command.stderr.decode(errors='replace'));self.assertTrue(path.is_junction())
            self.check(name+' reparse directory')
            os.rmdir(path) # Only the explicitly created junction; never recurse.
            path.write_bytes(original)
        junction=self.root/('.entrypoint-'+'c'*32)
        command=subprocess.run(['cmd.exe','/d','/c','mklink','/J',str(junction),str(self.journal.parent)],capture_output=True)
        self.assertEqual(command.returncode,0)
        marker=self.folder/'PUBLICATION_ADMITTED.json';value=json.loads(marker.read_text());value['transaction']=str(junction);marker.write_text(json.dumps(value))
        self.check('reparse transaction');self.assertEqual((target/'sentinel').read_bytes(),b'synthetic untouched')
    def test_newer_descriptor_and_missing_admission_are_not_republished(self):
        self.fixture();path=self.root/'launch-current.json';value=json.loads(path.read_text());value['version']=3;path.write_text(json.dumps(value))
        self.check('newer winner unchanged')
        marker=self.folder/'PUBLICATION_ADMITTED.json';marker.unlink();self.check('no admission',None)
    def test_descriptor_root_reparse_checks_are_preserved(self):
        self.fixture();self.check('intact root control','COMPLETE_RECEIPT_RECOVERED')
        descriptor=self.root/'launch-current.json';original=descriptor.read_bytes();journal=self.journal.read_bytes()
        alias=self.root/'root-alias';target=self.root/'data'
        command=subprocess.run(['cmd.exe','/d','/c','mklink','/J',str(alias),str(target)],capture_output=True)
        self.assertEqual(command.returncode,0)
        for key in ['installed','data','runtime']:
            value=json.loads(original);value[key]=str(alias);descriptor.write_text(json.dumps(value))
            proof=json.loads(journal);proof['desired']=value;self.journal.write_text(json.dumps(proof))
            self.check('descriptor '+key+' is reparse')
            descriptor.write_bytes(original);self.journal.write_bytes(journal)
        self.check('root control restored','COMPLETE_RECEIPT_RECOVERED')
