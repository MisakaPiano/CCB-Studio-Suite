"""Prepare durable public metadata locally. No network, key creation or key export.

Signers are supplied by the operator's signing adapter. Only public metadata is
persisted. Each prepared generation is immutable; CURRENT is the local commit
point. Publishing that generation to GitHub is a separate authorized operation.
"""
from datetime import datetime, timedelta, timezone
import hashlib
import json
import os
from pathlib import Path
from types import SimpleNamespace
import uuid
from tuf.api.metadata import Metadata, Targets, Snapshot, Timestamp, MetaFile, TargetFile
from studio_update import Installed, inspect, exclusive, UpdateRejected
from github_transport import GitHubFetcher
from install_ops import atomic_json, no_reparse


def verify(root, role, metadata):
    root.signed.verify_delegate(role, metadata.signed_bytes, metadata.signatures)


def current(store):
    path=store/'CURRENT.json'
    if not path.exists():return None,{}
    receipt=json.loads(path.read_text(encoding='utf-8'))
    generation=receipt['generation']
    if not isinstance(generation,str) or len(generation)!=32 or any(c not in '0123456789abcdef' for c in generation):
        raise ValueError('Invalid metadata generation')
    folder=no_reparse(store/'generations'/generation)
    files={}
    for name,digest in receipt['files'].items():
        if Path(name).name!=name or '/' in name or '\\' in name:raise ValueError('Invalid metadata name')
        data=(folder/name).read_bytes()
        if hashlib.sha256(data).hexdigest()!=digest:raise ValueError('Prepared metadata changed')
        files[name]=data
    return receipt,files


def prepare(store, bootstrap, repository, channel, signers, target=None, now=None, dry_run=False, rotated_root=None):
    """New target needs targets signer; ordinary renewal needs only online roles.

    Caller pins bootstrap out of band. Root rotation must be sequential and
    signed by both old and new root thresholds. Nothing here uploads files.
    """
    store=no_reparse(Path(store).resolve())
    if store.exists() and not store.is_dir():raise ValueError('Metadata store must be a directory')
    # Windows lock also protects version allocation across publisher processes.
    with exclusive(store):
        previous,files=current(store)
        pinned=Metadata.from_bytes(bootstrap)
        verify(pinned,'root',pinned)
        if previous and files.get(f'{pinned.signed.version}.root.json')!=bootstrap:
            raise ValueError('Publisher bootstrap identity changed')
        root=pinned
        for version in range(pinned.signed.version+1, (previous or {}).get('rootVersion',pinned.signed.version)+1):
            next_root=Metadata.from_bytes(files[f'{version}.root.json'])
            verify(root,'root',next_root);verify(next_root,'root',next_root)
            root=next_root
        if rotated_root is not None:
            next_root=Metadata.from_bytes(rotated_root)
            if next_root.signed.version!=root.signed.version+1:raise ValueError('Root rotation must be sequential')
            verify(root,'root',next_root);verify(next_root,'root',next_root)
            root=next_root
        now=now or datetime.now(timezone.utc)
        if root.signed.is_expired(now):raise ValueError('Signing root has expired')
        old={name:Metadata.from_bytes(files[name+'.json']) for name in ('targets','snapshot','timestamp')} if previous else {}
        if previous:
            if previous['repository']!=repository or previous['channel']!=channel:raise ValueError('Metadata store identity changed')
            # Validate prior public state under its original root, before accepting changes.
            prior_root=Metadata.from_bytes(files[f"{previous['rootVersion']}.root.json"])
            for name,metadata in old.items():verify(prior_root,name,metadata)
            old['snapshot'].signed.meta['targets.json'].verify_length_and_hashes(files['targets.json'])
            old['timestamp'].signed.snapshot_meta.verify_length_and_hashes(files['snapshot.json'])
        if target is None:
            if not previous:raise ValueError('Initial publication needs a target')
            targets=old['targets']
        else:
            identity=Installed(1,3,channel,'','',bootstrap)
            inspect(SimpleNamespace(get_targetinfo=lambda _:target),identity)
            GitHubFetcher(repository,channel).authorize(target)
            if previous:
                prior=old['targets'].signed.targets['studio-installer.exe']
                before=prior.unrecognized_fields['custom']['version'];after=target.unrecognized_fields['custom']['version']
                if after<before or (after==before and target.to_dict()!=prior.to_dict()):
                    raise ValueError('Release version reused with different content or rolled back')
            targets=Metadata(Targets(version=old['targets'].signed.version+1 if previous else 1,expires=now+timedelta(days=90)))
            targets.signed.targets['studio-installer.exe']=target
            targets.sign(signers['targets'])
        verify(root,'targets',targets)
        if targets.signed.expires<=now+timedelta(days=7):raise ValueError('Offline target renewal required before online maintenance')
        snapshot=Metadata(Snapshot(version=old['snapshot'].signed.version+1 if previous else 1,
                                   expires=min(now+timedelta(days=14),targets.signed.expires)))
        snapshot.signed.meta['targets.json']=MetaFile.from_data(targets.signed.version,targets.to_bytes(),['sha256'])
        snapshot.sign(signers['snapshot']);verify(root,'snapshot',snapshot)
        timestamp=Metadata(Timestamp(version=old['timestamp'].signed.version+1 if previous else 1,
                                     expires=min(now+timedelta(days=7),snapshot.signed.expires)))
        timestamp.signed.snapshot_meta=MetaFile.from_data(snapshot.signed.version,snapshot.to_bytes(),['sha256'])
        timestamp.sign(signers['timestamp']);verify(root,'timestamp',timestamp)
        files[f'{pinned.signed.version}.root.json']=bootstrap
        files[f'{root.signed.version}.root.json']=root.to_bytes()
        for name,metadata in [('targets',targets),('snapshot',snapshot),('timestamp',timestamp)]:
            files[name+'.json']=metadata.to_bytes()
            if name!='timestamp':files[f'{metadata.signed.version}.{name}.json']=metadata.to_bytes()
        receipt=dict(generation=uuid.uuid4().hex,repository=repository,channel=channel,rootVersion=root.signed.version,
                     timestampVersion=timestamp.signed.version,releaseVersion=targets.signed.targets['studio-installer.exe'].unrecognized_fields['custom']['version'],
                     files={name:hashlib.sha256(data).hexdigest() for name,data in sorted(files.items())},
                     status='PREPARED_NOT_PUBLISHED',publishLast='timestamp.json')
        if dry_run:return receipt
        folder=store/'generations'/receipt['generation'];folder.mkdir(parents=True)
        for name,data in files.items():
            with (folder/name).open('xb') as stream:stream.write(data);stream.flush();os.fsync(stream.fileno())
        atomic_json(store/'CURRENT.json',receipt)
        return receipt


def target_from_build(build, notes, tag, asset):
    """Bind the signed target to a completed build, not an arbitrary file path."""
    build=Path(build)
    receipt=json.loads((build/'BUILD_RECEIPT.json').read_text(encoding='utf-8'))
    config=json.loads((build/'payload/update-config.json').read_text(encoding='utf-8'))
    manifest=json.loads((build/'payload/INSTALL_MANIFEST.json').read_text(encoding='utf-8'))
    target=TargetFile.from_file('studio-installer.exe',str(build/'studio-installer.exe'),['sha256'])
    if receipt['status']!='BUILT' or target.hashes['sha256']!=receipt['sha256'] or target.length!=receipt['bytes']:
        raise ValueError('Build payload does not match receipt')
    for key in ('sourceSha','version','channel'):
        if receipt[key]!=config[key] or manifest[key]!=config[key]:raise ValueError('Build identity mismatch')
    target.unrecognized_fields['custom']=dict(product=config['product'],platform=config['platform'],channel=config['channel'],
        version=config['version'],displayVersion=config['displayVersion'],sourceSha=config['sourceSha'],minimum_reader=2,
        maximum_reader=config['reader'],data_version=config['reader'],notes=notes,installedBytes=sum(f['bytes'] for f in manifest['files']),
        github=dict(tag=tag,asset=asset))
    return target
