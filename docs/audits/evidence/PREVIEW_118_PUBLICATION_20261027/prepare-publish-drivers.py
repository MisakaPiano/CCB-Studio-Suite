from pathlib import Path
ROOT=Path('H:/CCB-Studio-Suite');OUT=ROOT/'.local/preview-publication-118-20261027'
for name in ('publish-release.py','publish-metadata.py'):
 s=(ROOT/'.local/preview-publication-117-20261027'/name).read_text(encoding='utf-8-sig')
 s=s.replace("OUT=ROOT/'.local/preview-publication-117-20261027';C=ROOT/'.local/relocation-117-20261027/candidate-preview-117'","OUT=ROOT/'.local/preview-publication-118-20261027';C=Path('C:/CCB-Studio-Suite-Test-118-20261027/b')")
 s=s.replace('bec5f78735b6cd4c517c7c31c3d5bd499ed97a0d565e75718bc90c38fb0089c9','3540be44ca7a1fcfd583899b325abad7630e10b120d288e9f2e7dc7a1e8bd60d').replace('b9b240a248d428ec158c341b8c127969fd54cb64','a1202e24f7a0cba8a53f43bf035b67e2134ca2aa').replace('preview.117','preview.118').replace('Preview117','Preview118')
 s=s.replace('assert version==116','assert version==117').replace('releaseVersion=117','releaseVersion=118')
 assert "OUT=ROOT/'.local/preview-publication-118-20261027'" in s and "C=Path('C:/CCB-Studio-Suite-Test-118-20261027/b')" in s
 (OUT/name).write_text(s,encoding='utf-8')
