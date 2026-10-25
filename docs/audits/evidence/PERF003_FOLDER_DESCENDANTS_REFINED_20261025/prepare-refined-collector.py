from pathlib import Path
root=Path('H:/CCB-Studio-Suite');local=root/'.local/perf003-folder-descendants-20261025'
s=(local/'collect.py').read_text(encoding='utf-8')
s=s.replace('PERF003_FOLDER_DESCENDANTS_20261025','PERF003_FOLDER_DESCENDANTS_REFINED_20261025',1).replace('01020b1f82bff95dfacdb75f6318b3d2d258c626','0564cc48fe75a4aecec6e8b0137b762a89f8bc2c')
s=s.replace(".glob('*-perf003-folder-descendants-'+kind+'-20261025')",".glob('*-perf003-folder-descendants-'+('refined-final' if kind=='final' else kind)+'-20261025')")
s=s.replace('Final two warmups per mode/four alternating same-JVM rounds','Final twenty warmups per mode/four alternating same-JVM rounds')
s=s.replace('Final01020b1 three unique methodsPASS','Refined0564cc4 three unique methodsPASS').replace('Actual paired old/new four rounds with two warmups/mode','Actual paired old/new four rounds with twenty warmups/mode')
s=s.replace('Single linked result replaces per-node flatten and set merging;original folder scan/child-order/DFS recursion remains.','Single linked result replaces per-node flatten and set merging;original child filter is restored before recursion,original scan/child-order/DFS remains. Previous01020b1 slower two-warmup cohort and9512e88 diagnostic are retained separately;this source is0564cc4. Low-warm comparison pending,separate from functional PASS.')
s=s.replace("'finish_test.py'):","'finish_test.py','COLLECTOR_PREPARATION_FAILURE.md','prepare-refined-collector.py'):")
s=s.replace('finalUniqueMethods=3,baselineRepeatedMethodNotAdded=True,','finalUniqueMethods=3,baselineRepeatedMethodNotAdded=True,refinedFrom="01020b1,negative cohort7314ef9 and warm diagnostic e1f4f2a retained",lowWarmCrossCheck="PENDING / separate evidence",')
target=local/'collect-refined.py';assert not target.exists();target.write_text(s,encoding='utf-8')
