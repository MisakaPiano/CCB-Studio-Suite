import json,os,uuid,unittest
from pathlib import Path
from types import SimpleNamespace
from apply_update import wait_for_b
from studio_update import UpdateRejected
ROOT=Path(__file__).resolve().parents[2]

class VerificationWaitTests(unittest.TestCase):
    def setUp(self):
        self.folder=Path(os.environ['CCB_SYNTHETIC_TEST_ROOT'])/('wait-'+uuid.uuid4().hex);self.folder.mkdir()
        self.data=self.folder/'data';self.data.mkdir()
        self.keep=self.data/'retained.bin';self.keep.write_bytes(b'synthetic original')
        self.plan=dict(nonce='a'*64,snapshot={'synthetic':'hash'})
        self.identity=SimpleNamespace(reader=3);self.custom=dict(version=2,sourceSha='b'*40,data_version=3)
        self.ack=dict(nonce=self.plan['nonce'],version=2,sourceSha='b'*40,snapshot=self.plan['snapshot'],dataRoot=str(self.data),dataVersion=3)
        self.now=0;self.states=[];self.child=SimpleNamespace(poll=lambda:None)
    def publish(self,value=None):(self.folder/'B_VERIFIED.json').write_text(json.dumps(value or self.ack))
    def call(self,step,timeout=1800):
        try:
            return wait_for_b(self.folder,self.child,self.plan,self.identity,self.custom,self.data,
                lambda status,**kw:self.states.append((status,kw)),timeout=timeout,clock=lambda:self.now,sleep=step)
        finally:self.assertEqual(self.keep.read_bytes(),b'synthetic original')
    def test_live_verifier_after120seconds_still_validates_fresh_ack(self):
        def step(_):
            self.now+=30
            if self.now>=180:self.publish()
        self.assertEqual(self.call(step),self.ack)
        self.assertTrue(any(s[0]=='VERIFYING_B' and s[1]['verificationElapsedSeconds']>=120 for s in self.states))
    def test_foreign_nonce_program_snapshot_or_root_never_authorizes_commit(self):
        for key,value in [('nonce','c'*64),('version',8),('sourceSha','d'*40),('snapshot',{}),('dataRoot',str(self.folder)),('dataVersion',4)]:
            wrong=dict(self.ack);wrong[key]=value;self.publish(wrong)
            with self.subTest(key=key),self.assertRaises(UpdateRejected):self.call(lambda _:None)
    def test_cancel_retains_data_without_killing_program(self):
        (self.folder/'CANCEL').touch()
        with self.assertRaisesRegex(UpdateRejected,'cancelled'):self.call(lambda _:None)
    def test_b_failure_does_not_advance_or_wait_forever(self):
        (self.folder/'B_FAILED.json').write_text('{}')
        with self.assertRaisesRegex(UpdateRejected,'verification failed'):self.call(lambda _:None)
    def test_final_deadline_retains_program_for_checked_retry(self):
        def step(_):self.now+=1
        with self.assertRaisesRegex(UpdateRejected,'deadline reached'):self.call(step,timeout=2)
    def test_ack_written_as_launcher_exits_is_not_lost(self):
        def poll():self.publish();return 0
        self.child=SimpleNamespace(poll=poll)
        self.assertEqual(self.call(lambda _:None),self.ack)
