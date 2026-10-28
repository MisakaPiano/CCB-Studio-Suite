import importlib.util,unittest
from pathlib import Path
spec=importlib.util.spec_from_file_location("watch",Path(__file__).with_name("Check-Upstream.py"));watch=importlib.util.module_from_spec(spec);spec.loader.exec_module(watch)
class UpstreamTests(unittest.TestCase):
    def setup_case(self):
        old=dict(commit="a"*40,release=None,license=None)
        entry=dict(id="synthetic",repository="owner/repo",branch="main",referenceCommit="a"*40,baseline=old,pathsOfInterest=["app/core/"])
        data={"repos/owner/repo":dict(license=None),"repos/owner/repo/commits/main":dict(sha="a"*40),"repos/owner/repo/releases/latest":None}
        return dict(format=1,repositories=[entry]),dict(format=1,repositories={}),data
    def test_unchanged_and_change_cursor_do_not_adopt_reference(self):
        registry,state,data=self.setup_case();state,report=watch.check(registry,state,data.__getitem__);self.assertFalse(report["notify"])
        data["repos/owner/repo/commits/main"]["sha"]="b"*40;data["repos/owner/repo/compare/"+"a"*40+"..."+"b"*40]=dict(files=[dict(filename="app/core/detector.py")])
        state,report=watch.check(registry,state,data.__getitem__);self.assertTrue(report["notify"]);self.assertEqual(["app/core/detector.py"],report["repositories"][0]["relevantFiles"]);self.assertEqual("a"*40,registry["repositories"][0]["referenceCommit"])
        state,report=watch.check(registry,state,data.__getitem__);self.assertFalse(report["notify"])
    def test_same_tag_asset_digest_and_license_change_are_detected(self):
        registry,state,data=self.setup_case();release=dict(id=1,tag_name="v1",html_url="https://github.com/owner/repo/releases/tag/v1",published_at="synthetic",assets=[dict(name="model.onnx",size=1,digest="sha256:"+"a"*64)])
        data["repos/owner/repo/releases/latest"]=release;state,_=watch.check(registry,state,data.__getitem__)
        release["assets"][0]["digest"]="sha256:"+"b"*64;data["repos/owner/repo"]["license"]=dict(spdx_id="MIT")
        state,report=watch.check(registry,state,data.__getitem__);self.assertEqual(["release","license"],report["repositories"][0]["changes"])
    def test_failed_check_preserves_cursor_and_repeated_failure_is_quiet(self):
        registry,state,data=self.setup_case()
        def fail(path):raise TimeoutError()
        state,report=watch.check(registry,state,fail);self.assertTrue(report["notify"]);self.assertEqual("a"*40,state["repositories"]["synthetic"]["last"]["commit"])
        state,report=watch.check(registry,state,fail);self.assertFalse(report["notify"])
        state,report=watch.check(registry,state,data.__getitem__);self.assertTrue(report["notify"]);self.assertEqual("RECOVERED",report["repositories"][0]["status"])
if __name__=="__main__":unittest.main()
