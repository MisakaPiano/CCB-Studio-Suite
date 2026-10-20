"""Synthetic repository, memory-only ephemeral signers, and no network listeners."""
import tempfile
import unittest
from datetime import datetime, timedelta, timezone
from pathlib import Path
from urllib.parse import urlparse

from securesystemslib.signer import CryptoSigner
from tuf.api import exceptions
from tuf.api.metadata import Metadata, Root, Targets, Snapshot, Timestamp, TargetFile, MetaFile
from tuf.ngclient import Updater
from tuf.ngclient.fetcher import FetcherInterface
from candidate import ExpectedIdentity, CandidateRejected, select_candidate

FIXTURES = None


class MemoryFetcher(FetcherInterface):
    def __init__(self, files):
        self.files = files
        self.disconnect = False

    def _fetch(self, url):
        if self.disconnect:
            raise exceptions.DownloadError("Synthetic disconnected transport")
        parsed = urlparse(url)
        if parsed.netloc != "updates.invalid":
            raise AssertionError("Fixture transport must never access a real provider")
        path = parsed.path.lstrip("/")
        if path not in self.files:
            raise exceptions.DownloadHTTPError("Synthetic 404", 404)
        data = self.files[path]
        for offset in range(0, len(data), 64):
            yield data[offset:offset + 64]


class Repository:
    def __init__(self):
        self.signers = {role: CryptoSigner.generate_ed25519() for role in ("root", "targets", "snapshot", "timestamp")}
        self.files = {}
        self.fetcher = MemoryFetcher(self.files)
        self.expiry = datetime.now(timezone.utc) + timedelta(days=1)
        root = Metadata(Root(expires=self.expiry, consistent_snapshot=False))
        for role, signer in self.signers.items():
            root.signed.add_key(signer.public_key, role)
        root.sign(self.signers["root"])
        self.bootstrap = root.to_bytes()  # Only public keys, never signer/private-key serialization.
        self.publish()

    def publish(self, version=2, custom=None, expired=False, bad_signature=False):
        expiry = datetime.now(timezone.utc) - timedelta(days=1) if expired else self.expiry
        data = b"SYNTHETIC B PAYLOAD: NOT AN APPLICATION INSTALLER"
        target = TargetFile.from_data("studio.zip", data, ["sha256"])
        target.unrecognized_fields["custom"] = custom or {"product": "ccb-studio-suite", "platform": "windows-x86_64", "channel": "internal", "version": version, "minimum_reader": 1, "maximum_reader": 1}
        targets = Metadata(Targets(version=version, expires=self.expiry))
        targets.signed.targets["studio.zip"] = target
        targets.sign(CryptoSigner.generate_ed25519() if bad_signature else self.signers["targets"])
        target_bytes = targets.to_bytes()
        snapshot = Metadata(Snapshot(version=version, expires=self.expiry))
        snapshot.signed.meta["targets.json"] = MetaFile.from_data(version, target_bytes, ["sha256"])
        snapshot.sign(self.signers["snapshot"])
        snapshot_bytes = snapshot.to_bytes()
        timestamp = Metadata(Timestamp(version=version, expires=expiry))
        timestamp.signed.snapshot_meta = MetaFile.from_data(version, snapshot_bytes, ["sha256"])
        timestamp.sign(self.signers["timestamp"])
        self.files.update({"metadata/timestamp.json": timestamp.to_bytes(), "metadata/snapshot.json": snapshot_bytes, "metadata/targets.json": target_bytes, "targets/studio.zip": data})

    def client(self, path):
        (path / "metadata").mkdir(exist_ok=True)
        (path / "targets").mkdir(exist_ok=True)
        return Updater(str(path / "metadata"), "https://updates.invalid/metadata/", str(path / "targets"), "https://updates.invalid/targets/", fetcher=self.fetcher, bootstrap=self.bootstrap)


class TrustTests(unittest.TestCase):
    def setUp(self):
        self.path = Path(tempfile.mkdtemp(dir=FIXTURES))
        self.repo = Repository()
        self.identity = ExpectedIdentity("ccb-studio-suite", "windows-x86_64", "internal", 1, 1)

    def test_authenticated_target_download_has_exact_bytes(self):
        client = self.repo.client(self.path)
        target = select_candidate(client, "studio.zip", self.identity)
        downloaded = client.download_target(target)
        self.assertEqual(Path(downloaded).read_bytes(), self.repo.files["targets/studio.zip"])

    def test_wrong_target_signature_is_rejected(self):
        self.repo.publish(bad_signature=True)
        with self.assertRaises(exceptions.RepositoryError):
            select_candidate(self.repo.client(self.path), "studio.zip", self.identity)

    def test_expired_metadata_freeze_is_rejected(self):
        self.repo.publish(expired=True)
        with self.assertRaises(exceptions.ExpiredMetadataError):
            self.repo.client(self.path).refresh()

    def test_cached_metadata_cannot_roll_back(self):
        self.repo.client(self.path).refresh()
        self.repo.publish(version=1)
        with self.assertRaises(exceptions.BadVersionNumberError):
            self.repo.client(self.path).refresh()

    def test_tampered_payload_is_not_accepted_as_a_download(self):
        client = self.repo.client(self.path)
        info = select_candidate(client, "studio.zip", self.identity)
        self.repo.files["targets/studio.zip"] = b"X" * info.length
        with self.assertRaises(exceptions.LengthOrHashMismatchError):
            client.download_target(info)
        self.assertFalse((self.path / "targets/studio.zip").exists())

    def test_disconnected_transport_has_no_successful_payload(self):
        self.repo.fetcher.disconnect = True
        with self.assertRaises(exceptions.DownloadError):
            select_candidate(self.repo.client(self.path), "studio.zip", self.identity)
        self.assertEqual(list((self.path / "targets").iterdir()), [])

    def test_signed_wrong_identity_and_unsupported_reader_are_rejected(self):
        original = {"product": "ccb-studio-suite", "platform": "windows-x86_64", "channel": "internal", "version": 2, "minimum_reader": 1, "maximum_reader": 1}
        for field, value in (("product", "other-product"), ("platform", "android"), ("channel", "public"), ("minimum_reader", 2), ("version", 1), ("maximum_reader", "unknown")):
            with self.subTest(field=field):
                path = Path(tempfile.mkdtemp(dir=FIXTURES))
                self.repo.publish(custom={**original, field: value})
                with self.assertRaises(CandidateRejected):
                    select_candidate(self.repo.client(path), "studio.zip", self.identity)
                self.assertEqual(list((path / "targets").iterdir()), [])

    def test_rotated_root_needs_old_and_new_signatures(self):
        new = CryptoSigner.generate_ed25519()
        rotated = Metadata(Root(version=2, expires=self.repo.expiry, consistent_snapshot=False))
        for role, signer in self.repo.signers.items():
            rotated.signed.add_key(new.public_key if role == "root" else signer.public_key, role)
        rotated.sign(self.repo.signers["root"])
        self.repo.files["metadata/2.root.json"] = rotated.to_bytes()
        with self.assertRaises(exceptions.RepositoryError):
            self.repo.client(self.path).refresh()
        rotated.sign(new, append=True)
        self.repo.files["metadata/2.root.json"] = rotated.to_bytes()
        self.repo.client(self.path).refresh()
        self.assertEqual(Metadata.from_file(str(self.path / "metadata/root.json")).signed.version, 2)
