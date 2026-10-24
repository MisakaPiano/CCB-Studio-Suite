import json,statistics
from pathlib import Path
ROOT=Path('H:/CCB-Studio-Suite/.local/native-perf-20261025')
success=[p for p in ROOT.glob('run-*') if (p/'SUMMARY.json').exists() and json.loads((p/'SUMMARY.json').read_text()).get('status')=='PASS']
assert success,'No successful native pair recorded; do not invent completed measurements'
run=max(success,key=lambda p:p.stat().st_mtime)
results={}
for kind in ['process-cold','warm-reopen']:
    folder=run/kind;events=[json.loads(l) for l in (folder/'events.jsonl').read_text().splitlines()];by={e['phase']:e for e in events}
    samples=json.loads((folder/'PROCESS_SAMPLES.json').read_text());epoch=json.loads((folder/'STARTED.json').read_text())['epochSeconds']
    def nearest(event):return min(samples,key=lambda s:abs(s['elapsedMs']-(event['epochMillis']-epoch*1000)))
    def phase(a,b):
        left,right=by[a],by[b];l,r=nearest(left),nearest(right)
        return dict(wallMs=right['elapsedMs']-left['elapsedMs'],cpuMs=(right['processCpuNs']-left['processCpuNs'])/1e6,processIoReadBytes=r['io']['readBytes']-l['io']['readBytes'],processIoWriteBytes=r['io']['writeBytes']-l['io']['writeBytes'])
    cycles=[e['readyMs'] for e in events if e['phase'].startswith('gallery-cycle-')]
    results[kind]=dict(startup=json.loads((folder/'RESULT.json').read_text()),workspaceOpenStageMs=by['workspace-opened']['elapsedMs']-by['main-enter']['elapsedMs'],controllerOpenStageMs=by['controller-opened']['elapsedMs']-by['workspace-opened']['elapsedMs'],firstGalleryReadyMs=by['first-gallery-ready']['readyMs'],galleryCyclesMs=cycles,galleryCycleMedianMs=statistics.median(cycles),creationIdle=phase('first-native-draw','creation-idle-5s'),galleryIdle=phase('first-gallery-ready','gallery-idle-10s'),sampledProcessIoLast=samples[-1]['io'],frames=events[-1]['nativeDraws'],closedThumbnailCounts=dict(pending=by['controller-closed']['pendingThumbnails'],retainedBytes=by['controller-closed']['retainedThumbnailBytes']),totalProcessCpuMs=by['PASS']['processCpuNs']/1e6)
report=dict(run=str(run),results=results,notes=['Workspace open stage includes initial brand setup; controller stage includes application composition bootstrap, not exclusive method profiling','Window readiness joins completed native content draw and showing state; gallery includes two explicit test-only frame barriers; 20ms readiness polling','Sampled process IO is not physical disk activity; snapshots are ~100ms apart; heap peak is max event sample, not a continuous JVM peak','Two sessions are not statistical population or long-duration leak validation; OS file caches not flushed; packaged launcher and full Main startup not measured'])
(run/'ANALYSIS.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report,indent=2))
