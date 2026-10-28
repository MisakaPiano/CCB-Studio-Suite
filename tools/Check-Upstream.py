"""Read-only public GitHub observation; never checks out, downloads assets or adopts code."""
import copy, datetime, hashlib, json, os, re, subprocess, sys, uuid, urllib.error, urllib.parse, urllib.request
from pathlib import Path

def github(path):
    request=urllib.request.Request("https://api.github.com/"+path,headers={"User-Agent":"CCB-Studio-Suite-upstream-observer","Accept":"application/vnd.github+json"})
    try:
        with urllib.request.urlopen(request,timeout=20) as response:
            if urllib.parse.urlsplit(response.url).hostname!="api.github.com":raise ValueError("Unexpected API redirect")
            body=response.read(4*1024*1024+1)
            if len(body)>4*1024*1024:raise ValueError("API response exceeds bound")
            return json.loads(body)
    except urllib.error.HTTPError as error:
        if error.code==404 and path.endswith("/releases/latest"):return None
        raise
    except urllib.error.URLError:
        if os.name!="nt":raise
        # Use Windows' existing HTTPS trust stack if the bundled Python runtime cannot connect.
        # Certificate verification stays enabled; no trust store or system setting is changed.
        url="https://api.github.com/"+path
        quoted="'"+url.replace("'","''")+"'"
        script="\n".join(["$ErrorActionPreference='Stop'", "$ProgressPreference='SilentlyContinue'", "[Console]::OutputEncoding=[System.Text.UTF8Encoding]::new()", "$uri="+quoted, "try { (Invoke-WebRequest -UseBasicParsing -TimeoutSec 20 -Uri $uri -Headers @{'User-Agent'='CCB-Studio-Suite-upstream-observer'}).Content } catch { if ($uri.EndsWith('/releases/latest') -and $_.Exception.Response.StatusCode.value__ -eq 404) { 'null' } else { throw } }"])
        result=subprocess.run(["powershell.exe","-NoLogo","-NoProfile","-NonInteractive","-Command",script],stdout=subprocess.PIPE,stderr=subprocess.PIPE,timeout=30,creationflags=subprocess.CREATE_NO_WINDOW)
        if result.returncode:raise RuntimeError("Windows HTTPS observation failed; baseline retained")
        if len(result.stdout)>4*1024*1024:raise ValueError("API response exceeds bound")
        return json.loads(result.stdout.decode("utf-8-sig"))

def observe(entry,fetch):
    repo=entry["repository"];branch=entry["branch"]
    if not re.fullmatch(r"[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+",repo) or not re.fullmatch(r"[A-Za-z0-9_./-]+",branch):raise ValueError("Invalid upstream identity")
    base="repos/"+repo
    metadata=fetch(base);head=fetch(base+"/commits/"+urllib.parse.quote(branch,safe=""));release=fetch(base+"/releases/latest")
    commit=head["sha"]
    if not re.fullmatch("[0-9a-f]{40}",commit):raise ValueError("Invalid commit SHA")
    latest=None if release is None else dict(tag=release["tag_name"],id=release["id"],url=release["html_url"],publishedAt=release["published_at"],assets=sorted([dict(name=a["name"],size=a["size"],digest=a.get("digest")) for a in release.get("assets",[])],key=lambda a:a["name"]))
    return dict(commit=commit,release=latest,license=(metadata.get("license") or {}).get("spdx_id"))

def check(registry,state,fetch=github):
    if registry.get("format")!=1 or state.get("format")!=1:raise ValueError("Unknown upstream record format; retained")
    result=copy.deepcopy(state);reports=[]
    for entry in registry["repositories"]:
        key=entry["id"];previous=state.get("repositories",{}).get(key,{})
        before=previous.get("last",entry["baseline"])
        try:
            current=observe(entry,fetch)
            changes=[name for name in ("commit","release","license") if before.get(name)!=current.get(name)]
            paths=[];partial=False
            if "commit" in changes:
                if not re.fullmatch("[0-9a-f]{40}",before["commit"]):raise ValueError("Invalid prior commit")
                comparison=fetch("repos/"+entry["repository"]+"/compare/"+before["commit"]+"..."+current["commit"])
                files=comparison.get("files",[]);partial=len(files)>=300
                paths=[f["filename"] for f in files if any(f["filename"].startswith(p) if p.endswith("/") else f["filename"]==p for p in entry.get("pathsOfInterest",[]))]
            recovered=bool(previous.get("error"));result.setdefault("repositories",{})[key]=dict(last=current,error=None)
            reports.append(dict(id=key,repository=entry["repository"],status="CHANGED" if changes else "RECOVERED" if recovered else "UNCHANGED",notify=bool(changes or recovered),changes=changes,observation=current,relevantFiles=paths,partialComparison=partial,referenceCommitUnchanged=entry["referenceCommit"]))
        except Exception as error:
            code=type(error).__name__+(":"+str(error.code) if isinstance(error,urllib.error.HTTPError) else "")
            result.setdefault("repositories",{})[key]=dict(previous,last=before,error=code)
            reports.append(dict(id=key,repository=entry["repository"],status="ERROR",notify=previous.get("error")!=code,error=code,lastObservationRetained=True))
    return result,dict(checkedUtc=datetime.datetime.now(datetime.timezone.utc).isoformat(),notify=any(r["notify"] for r in reports),repositories=reports,adoption="NONE")

def safe(path,root):
    path=path.absolute()
    if not path.is_relative_to(root):raise ValueError("Monitor output outside Suite retained")
    for p in [path,*path.parents]:
        if p.is_symlink() or p.is_junction():raise ValueError("Linked monitor path retained")
    return path

def write(path,value):
    pending=path.with_name(path.name+"."+uuid.uuid4().hex+".pending")
    with pending.open("x",encoding="utf-8") as f:json.dump(value,f,ensure_ascii=False,indent=2);f.write("\n");f.flush();os.fsync(f.fileno())
    os.replace(pending,path)

def main():
    if sys.version_info<(3,12):raise RuntimeError("Use the configured Python 3.13 runtime")
    root=Path(__file__).resolve().parents[1];assert (root/"AGENTS.md").is_file()
    registry=json.loads((root/"references/UPSTREAM_TRACKING.json").read_text(encoding="utf-8"))
    folder=safe(root/".local/upstream-monitor",root);folder.mkdir(parents=True,exist_ok=True)
    statefile=safe(folder/"state.json",root);reportfile=safe(folder/"latest.json",root)
    lock=safe(folder/"operation.lock",root).open("a+b")
    try:
        lock.seek(0)
        if os.name=="nt":
            import msvcrt
            if lock.read(1)==b"":lock.write(b"1");lock.flush()
            lock.seek(0);msvcrt.locking(lock.fileno(),msvcrt.LK_NBLCK,1)
        else:
            import fcntl
            fcntl.flock(lock.fileno(),fcntl.LOCK_EX|fcntl.LOCK_NB)
        state=json.loads(statefile.read_text(encoding="utf-8")) if statefile.exists() else dict(format=1,repositories={})
        next_state,report=check(registry,state)
        # Persist a notifying report before advancing the observation cursor. A failed write cannot lose the change.
        if report["notify"]:
            history=safe(folder/"changes",root);history.mkdir(exist_ok=True)
            key=hashlib.sha256(json.dumps(report,sort_keys=True).encode()).hexdigest()[:16]
            write(safe(history/(datetime.datetime.now(datetime.timezone.utc).strftime("%Y%m%dT%H%M%S")+"-"+key+".json"),root),report)
        write(reportfile,report);write(statefile,next_state)
        print(json.dumps(report,ensure_ascii=False,indent=2))
        return 1 if any(r["status"]=="ERROR" for r in report["repositories"]) else 0
    finally:lock.close()

if __name__=="__main__":raise SystemExit(main())
