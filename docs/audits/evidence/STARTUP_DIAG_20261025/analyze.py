import collections,datetime,gzip,json,re,sys
from pathlib import Path
run=Path(sys.argv[1])
def stamp(value):return datetime.datetime.fromisoformat(value.replace('Z','+00:00')).timestamp()
def duration(value):
    match=re.fullmatch(r'PT([\d.]+)S',value)
    if not match:raise ValueError('Unexpected JFR duration '+value)
    return float(match[1])*1000
def method(frame):
    value=frame.get('method') or {};kind=value.get('type') or {}
    return kind.get('name','?').replace('/','.')+'.'+value.get('name','?')
analysis={}
for kind in ('timeline-only','minimum-jfr'):
    phase=run/kind
    events=[json.loads(line) for line in (phase/'events.jsonl').read_text().splitlines()]
    by={event['phase']:event for event in events};started=json.loads((phase/'STARTED.json').read_text())
    pairs=[('brand','brand-start','brand-ready'),('workspace','workspace-open-start','workspace-opened'),('application-entry','workspace-opened','application-first-composition'),('http-construction','http-construction-start','http-construction-ready'),('controller','controller-construction-start','controller-opened'),('native-window','window-call-start','native-window-first-composition'),('window-content-to-workbench','native-window-first-composition','workbench-call-start'),('workbench-composition','workbench-call-start','workbench-first-composition-committed')]
    rows=[dict(stage=name,wallMs=by[b]['elapsedMs']-by[a]['elapsedMs'],processCpuMs=(by[b]['processCpuNs']-by[a]['processCpuNs'])/1e6) for name,a,b in pairs]
    draw=by['first-native-draw'];commit=by['workbench-first-composition-committed']
    rows.append(dict(stage='commit-to-first-completed-content-draw',wallMs=draw['firstContentDrawMs']-commit['elapsedMs'],processCpuMs=None))
    rows.append(dict(stage='completed-draw-to-showing-confirmation',wallMs=draw['elapsedMs']-draw['firstContentDrawMs'],processCpuMs=None))
    result=dict(parentLaunchToMainNoteMs=by['main-enter']['epochMillis']-started['epochSeconds']*1000,parentLaunchToWindowReadyMs=draw['epochMillis']-started['epochSeconds']*1000,processLifetimeMs=json.loads((phase/'EXIT_OBSERVATION.json').read_text())['elapsedMs'],stages=rows,pid=started['pid'])
    if kind=='minimum-jfr':
        with gzip.open(phase/'JFR_EVENTS.json.gz','rt',encoding='utf-8') as source:recording=json.load(source)['recording']['events']
        counts=collections.Counter(e['type'] for e in recording)
        bounds=[('parent-to-main',started['epochSeconds'],stamp(by['main-enter']['utc']))]+[(name,stamp(by[a]['utc']),stamp(by[b]['utc'])) for name,a,b in pairs]
        content_time=stamp(draw['utc'])-(draw['elapsedMs']-draw['firstContentDrawMs'])/1000
        bounds += [('commit-to-content-draw',stamp(commit['utc']),content_time),('draw-to-ready',content_time,stamp(draw['utc'])),('settle',stamp(draw['utc']),stamp(by['startup-diagnostic-settled']['utc'])),('checks-and-close',stamp(by['startup-diagnostic-settled']['utc']),stamp(by['PASS']['utc']))]
        windows={name:dict(executionSamples=0,nativeSamples=0,threadCounts=collections.Counter(),topLeafMethods=collections.Counter(),classLoads=0,classLoadInclusiveMs=0,compilations=0,compilationInclusiveMs=0,gcEvents=0) for name,_,_ in bounds}
        load_long=[];compile_long=[]
        for event in recording:
            val=event['values'];moment=stamp(val['startTime']);typ=event['type']
            bucket=next((windows[name] for name,a,b in bounds if a<=moment<b),None)
            if typ in ('jdk.ExecutionSample','jdk.NativeMethodSample'):
                if bucket is not None:
                    bucket['executionSamples' if typ=='jdk.ExecutionSample' else 'nativeSamples']+=1
                    thread=val.get('sampledThread') or {};bucket['threadCounts'][thread.get('javaName',thread.get('osName','?'))]+=1
                    frames=(val.get('stackTrace') or {}).get('frames') or []
                    bucket['topLeafMethods'][method(frames[0]) if frames else 'NO_STACK']+=1
            elif typ=='jdk.ClassLoad':
                ms=duration(val['duration']);name=(val.get('loadedClass') or {}).get('name','?')
                load_long.append(dict(ms=ms,name=name,startTime=val['startTime']))
                if bucket is not None:bucket['classLoads']+=1;bucket['classLoadInclusiveMs']+=ms
            elif typ=='jdk.Compilation':
                ms=duration(val['duration']);compile_long.append(dict(ms=ms,method=method(dict(method=val.get('method'))),startTime=val['startTime']))
                if bucket is not None:bucket['compilations']+=1;bucket['compilationInclusiveMs']+=ms
            elif typ=='jdk.GarbageCollection' and bucket is not None:bucket['gcEvents']+=1
        for bucket in windows.values():
            bucket['topLeafMethods']=bucket['topLeafMethods'].most_common(12);bucket['threadCounts']=dict(bucket['threadCounts'])
        result.update(jfrEventCounts=dict(counts),jfrWindows=windows,longestClassLoads=sorted(load_long,key=lambda x:x['ms'],reverse=True)[:15],longestCompilations=sorted(compile_long,key=lambda x:x['ms'],reverse=True)[:15])
    analysis[kind]=result
output=dict(results=analysis,limits=['Only one session per mode, no statistical confidence or pre/post product comparison','JFR bootstrap affects parent-to-main; sampling not exclusive CPU or causal proof','Class-load/compilation event durations may overlap/nest and do not sum to wall time','Phase timestamps include management/file-note observer cost; content draw is not Windows compositor presentation','48 asset IDs, one unique PNG hash; no OS cache flush/full Main/packaged startup/private data/real provider calls','No repeated accepted gallery baseline or JUnit tests'])
(run/'ANALYSIS.json').write_text(json.dumps(output,indent=2)+'\n',encoding='utf-8')
print(json.dumps(dict(results={k:{key:value for key,value in v.items() if key in ('parentLaunchToMainNoteMs','parentLaunchToWindowReadyMs','processLifetimeMs','stages','jfrEventCounts','jfrWindows')} for k,v in analysis.items()}),indent=2))
