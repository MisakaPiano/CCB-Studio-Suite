import json,sys
from pathlib import Path
HERE=Path(__file__).resolve().parent;sys.path.insert(0,str(HERE))
import cleanup as c
p=HERE/'new-synthetic-delete-validation.bin';assert not p.exists();p.write_bytes(b'explicit synthetic deletion validation')
s=p.stat();item=dict(bytes=s.st_size,modified100=s.st_mtime_ns//100+116444736000000000)
with c.held_parents(p):result=c.delete_file(p,item)
assert result=='REMOVED' and not p.exists(),result
print(json.dumps(dict(syntheticHandleAndMetadataCheck=result)))
