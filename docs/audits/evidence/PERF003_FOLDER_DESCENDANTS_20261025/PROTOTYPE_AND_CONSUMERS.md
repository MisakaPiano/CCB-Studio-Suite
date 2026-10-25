# Folder subtree investigation: historical preparation notes

The notes below were written before the baseline. After actual shape evidence,option2 was selected as application01020b1 and its three-method final check PASS. Option1 was not implemented;option3 consumer multiplicity remains open. Original preparation text is retained below as historical context,not current implementation status.

Production GenerationOrganization.descendants remains unchanged. First root-only gate at f6d680c was cancelled before any Gradle/JUnit execution (no phase directory); e3989bc expands the same baseline method to root / two-node subtree / leaf / missing root, each at32/256/1000 validated nodes. Cancellation is0 executions,not a test failure/pass.

Current consumers (read-only at e3989bc,unchanged Desktop files):

- GalleryWorkspace.kt folder scope: archived batch filter calls descendants for every eligible batch; organizations root short-circuits it.
- GalleryOrganization.kt OrganizationFolders: each folder's batch filter repeatedly calls descendants for every batch.
- organizationFolders folder cards and organizationOpen path scopes have the same repeated membership predicate.

This is separate from the already accepted GalleryBatchIndex image projection. Do not rerun/reopen its nine methods or startup baseline. No layout/feature/queue/archive edits made.

Candidate tradeoffs to evaluate before choosing:

1. Build parent adjacency once per descendants call, then DFS into one ordered result. Reduces large-root scans but can allocate/compute a whole1000-node index for a leaf or two-node subtree. The caller may repeat that per batch,so a root-only benchmark is insufficient.
2. Keep original scan semantics but accumulate directly in one ordered set,without per-level flattened lists and merged sets. No whole-tree adjacency allocation for a leaf;still repeated node scans. Preserve DFS order and unknown-root singleton,do not add a visited guard which silently accepts invalid cycles. Complete existing validate must remain.
3. Hoist the unchanged descendants result once outside each batch-filter invocation. Targets actual multiplicity and can help root and leaf;requires a separately frozen caller scope/equivalence tests,without changing archived-only rules/batch order/origin image projection/layout. Do not assume all call sites have the same archive predicate. No implementation yet.

No candidate is selected or claimed fixed without actual shape results. The baseline executes full StudioState.validate outside traversal timing,checks <=4MiB and metadata bytes after every call. Original public helper does not itself validate;validation admission must not be removed/moved or represented as new helper behavior. Invalid structures are outside the measured traversal and remain refused by existing validate.

Desktop first:current one-shot gate waits for existing Desktop worker. No force kill/security change/real data/new package/release/Project send. Baseline NOT TESTED until actual records. Fixed previous reports/failures/user rejections/USER_NOT_TESTED/115/Prompt/q09/PRODUCT_HOLD remain.
