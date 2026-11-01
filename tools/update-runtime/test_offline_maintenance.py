"""Explicit local maintenance never fetches metadata or claims UP_TO_DATE."""
import hashlib,json,os,tempfile,unittest
from pathlib import Path
from unittest.mock import patch
from studio_update import Installed,UpdateRejected,execute

class OfflineMaintenanceTest(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory(dir=os.environ['CCB_OFFLINE_TEST_ROOT'])
        self.addCleanup(self.temp.cleanup)
        self.root=Path(self.temp.name);self.package=self.root/'program';self.package.mkdir()
        bootstrap=b'synthetic-bootstrap-no-network'
        (self.package/'update-root.json').write_bytes(bootstrap)
        self.config=dict(format=1,product='ccb-studio-suite',platform='windows-x86_64',version=120,reader=3,
            channel='internal',internalTest=True,updateMode='offline-maintenance',sourceSha='a'*40,
            rootSha256=hashlib.sha256(bootstrap).hexdigest(),metadataUrl='https://example.invalid/metadata/',targetsUrl='https://example.invalid/targets/')

    def load(self):
        (self.package/'update-config.json').write_text(json.dumps(self.config),encoding='utf-8')
        return Installed.load(self.package)

    def test_check_and_download_return_local_without_network_cache_or_progress(self):
        installed=self.load();cache=self.root/'updates';progress=[]
        with patch('studio_update.create_updater',side_effect=AssertionError('Network path reached')):
            for download in (False,True):
                self.assertEqual({'status':'LOCAL_MAINTENANCE'},execute(installed,cache,download=download,progress=progress.append))
        self.assertFalse(cache.exists());self.assertEqual([],progress)

    def test_offline_mode_cannot_silence_preview_or_implicit_internal_packages(self):
        for change in ({'channel':'preview'},{'internalTest':False},{'updateMode':'unknown'}):
            previous=self.config.copy();self.config.update(change)
            with self.assertRaises(UpdateRejected):self.load()
            self.config=previous

    def test_package_without_marker_still_reaches_normal_update_verification(self):
        self.config.pop('updateMode');installed=self.load();self.assertFalse(installed.offline_maintenance)
        with patch('studio_update.create_updater',side_effect=RuntimeError('ONLINE_VERIFICATION_REACHED')):
            with self.assertRaisesRegex(RuntimeError,'ONLINE_VERIFICATION_REACHED'):
                execute(installed,self.root/'updates')

if __name__=='__main__':unittest.main()
