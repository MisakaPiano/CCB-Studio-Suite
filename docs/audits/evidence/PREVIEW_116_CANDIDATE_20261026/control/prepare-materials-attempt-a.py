from pathlib import Path
import hashlib,json,sys,zipfile
ROOT=Path('H:/CCB-Studio-Suite');OUT=ROOT/'.local/preview-candidate-116-20261026';BUILD=OUT/'candidate';plan=json.loads((OUT/'PLAN.json').read_text());verified=json.loads((OUT/'INSTALL_VERIFICATION.json').read_text());assert verified['status']=='PASS'
sys.path.insert(0,str(BUILD/'payload/updater/lib'))
from release_repository import target_from_build
from github_transport import GitHubFetcher
notes=(OUT/'RELEASE_NOTES.md').read_text(encoding='utf-8')
target=target_from_build(BUILD,notes,plan['proposedTag'],plan['installerName'])
assert target.hashes['sha256']==verified['installerSha256'] and target.length==verified['installerBytes']
fetcher=GitHubFetcher('MisakaPiano/CCB-Studio-Suite','preview',metadata_branch='long-term-metadata');fetcher.authorize(target)
assert target.unrecognized_fields['custom']['version']==116 and target.unrecognized_fields['custom']['sourceSha']==plan['source']
assert target.unrecognized_fields['custom']['github']['asset']==plan['installerName']
proposal=dict(status='UNSIGNED_LOCAL_PROPOSAL_NOT_PUBLISHED',targetPath=target.path,target=target.to_dict(),authorizedLocation=fetcher.authorized,formalMetadataModified=False,signingKeysRead=False)
(OUT/'UNSIGNED_TARGET_PROPOSAL.json').write_text(json.dumps(proposal,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def sha(p):
 with p.open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()
with zipfile.ZipFile(OUT/'THIRD_PARTY_NOTICES.zip') as archive:
 assert archive.testzip() is None;notices=len(archive.namelist())
assets={plan['installerName']:BUILD/'studio-installer.exe','RELEASE_NOTES.md':OUT/'RELEASE_NOTES.md','THIRD_PARTY_NOTICES.zip':OUT/'THIRD_PARTY_NOTICES.zip'}
sums=''.join(sha(path)+'  '+name+'\n' for name,path in assets.items())
(OUT/'SHA256SUMS.txt').write_text(sums,encoding='ascii')
manifest=dict(status='READY_FOR_USER_CONFIRMATION_NOT_PUBLISHED',sourceSha=plan['source'],appBuildSourceSha=plan['appBuildSource'],version=116,displayVersion=plan['displayVersion'],proposedTag=plan['proposedTag'],tagCreated=False,installer=json.loads((BUILD/'BUILD_RECEIPT.json').read_text()),installationVerification='INSTALL_VERIFICATION.json',targetProposal='UNSIGNED_TARGET_PROPOSAL.json',assets={name:dict(localPath=str(path),bytes=path.stat().st_size,sha256=sha(path)) for name,path in assets.items()},checksums=dict(path=str(OUT/'SHA256SUMS.txt'),sha256=sha(OUT/'SHA256SUMS.txt')),noticesEntries=notices,formalRootSha256=plan['formalRootSha256'],newTests=0,reusedEvidence='docs/audits/UPDATE_INVITATION_CLOSURE_20261026.md; TOKEN_CANDIDATE_20261026.md and original scoped Project judgments',pending=['Full asset/editor human acceptance','User115 experience','Prompt comparison','Public116 signed discovery/download after publication','Daily installation after user confirmation and publication verification','Independent review of new package NOT REQUESTED','q09 historical FAILED/root UNKNOWN; cross-machine restore and other Windows environments unverified'],userConfirmationRequired=True,dailyInstallationModified=False,published=False,signingKeysRead=False)
(OUT/'PUBLICATION_MANIFEST.json').write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf-8')
(OUT/'README.zh-CN.txt').write_text('Preview116候选，等待用户确认；未发布，未更新日常安装。\n本地唯一安装器：candidate/studio-installer.exe。拟发布时使用PUBLICATION_MANIFEST.json中的Windows附件名，不改变文件字节。\n公开签名元数据尚未生成；UNSIGNED_TARGET_PROPOSAL.json只是本地身份/散列绑定草案。\n不要将本地候选安装器直接用于覆盖日常目录。确认后沿原发布流程先核对渠道，再更新日常安装。\n完整结果见INSTALL_VERIFICATION.json；发行范围及待验项见RELEASE_NOTES.md。\n',encoding='utf-8')
print(json.dumps(dict(status=manifest['status'],installerSha256=target.hashes['sha256'],noticesEntries=notices,signingKeysRead=False,published=False)))
