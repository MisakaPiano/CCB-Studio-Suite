# Existing PERF-003 folder batch predicate work, preparation only

Production as of92f8f0e is byte-equivalent (modules/tools excluding tests) to completedfa58a50. Traversal candidates01020b1/0564cc4 were NOT ADOPTED after mixed low-warm latency;all raw cohorts retained. Do not tune the original recursive helper again or relabel rejected trials as fixes.

Baseline974e100 adds a pure adapter of the current filter predicates.32/128/256 admitted nodes,64/256/512 batches,root/leaf,archivedOnlyfalse/true,shuffled batch order,unplaced parents,one synthetic image UUID/group per batch,no payloads. Full StudioState.validate plus<=4MiB and byte equality outside timing. Expected IDs computed independently from fixture index rules,not traversal. Selected baseline also runs the two retained GenerationFolderContractTest methods;no accepted batch9/startup/full suite.

If supported,minimal new selection method on GenerationOrganization:

```
fun batchesInSubtree(id:String?,archivedOnly:Boolean=false):List<GenerationBatch> {
  val eligible=if(archivedOnly)batches.filter{it.archivedAt!=null}else batches
  if(eligible.isEmpty())return emptyList()
  val scope=descendants(id!!)
  return eligible.filter{it.parent in scope}
}
```

Same original descendants,no new cache/index/schema/admission. Exactly one subtree evaluation per nonempty eligible query. The nullable ID intentionally preserves existing lazy short-circuit:old archived predicate never evaluates `g.organizationId!!` when there are no eligible batches. Empty/no-archived with null ID must remain empty;nonempty eligible null still throws NullPointerException as before. Do not replace with requireNotNull/change exception policy. Unknown nonnull root stays old behavior. Returned filtered list preserves input order and cannot mutate source batch list.

Call-site scope (only after actual baseline):

- GalleryWorkspace organizationIds:organizations root retains original direct archived filter (no subtree). Folder branch uses method with nullable g.organizationId and archivedOnly=true. Do not eagerly apply `!!` at argument construction.
- GalleryOrganization OrganizationFolders,organizationFolders folder cards,and organizationOpen breadcrumb folder path:method uses defaultarchivedOnly=false as the current expressions do. Do not "fix" their archive-policy difference in a performance task. Leave direct-parent archived batch cards,image projection/index,path labels,IDs/selection/remember keys/layout intact.

Tests after implementation:new core helper paired with unchanged recorded predicate plus order/archive/null-shortcircuit/empty/unknown/reparented-state invariants;new pure desktop folder-card projection test (no Workbench/native window/clipboard/provider),only newly selected methods. Existing accepted batch image index and old9methods stay untouched. Run core/desktop tasks with separate explicit filters;do not put filters only after last task and accidentally run fullcore. All fixtures synthetic metadata only;no actual gallery/credentials/service/paused collection/daily app/release/security/Desktop changes. New Project materials prepared only.
