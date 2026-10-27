package io.github.misakapiano.ccbstudio.assets;

import java.io.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Callable;
import static java.nio.file.StandardOpenOption.*;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;

/** Cooperative single-writer store. A committed records/<UUID>.rec is the logical commit point. */
public final class AssetStore implements AutoCloseable {
    public enum Kind { ORIGINAL, DERIVED }
    public enum Io { READ, WRITE, MOVE }
    public enum Point { INTENT, BYTES, SEALED, ASSET, RECORD, INDEX, COMPLETE,
        BACKUP_REGISTERED, BACKUP_FILE, BACKUP_MANIFEST, RESTORE_FILE, RESTORE_COMPLETE,
        MAINTENANCE_INTENT, MAINTENANCE_MOVED, MAINTENANCE_COMPLETE }
    public interface Faults {
        default void before(Io operation, Path path) throws IOException {}
        default void after(Point point, Path path) throws IOException {}
    }
    public static final Faults NONE = new Faults() {};

    public record Request(UUID id, String name, Kind kind, UUID parentId, byte[] recipe, byte[] project) {
        public Request {
            Objects.requireNonNull(id); Objects.requireNonNull(name); Objects.requireNonNull(kind);
            Objects.requireNonNull(recipe); Objects.requireNonNull(project);
            if (name.length() > 512 || recipe.length > 1024 * 1024 || project.length > 1024 * 1024)
                throw new IllegalArgumentException("Name or opaque document exceeds slice limit");
            if ((kind == Kind.ORIGINAL) != (parentId == null) || id.equals(parentId))
                throw new IllegalArgumentException("Original has no parent; derived requires a different parent");
            recipe = recipe.clone(); project = project.clone();
        }
        @Override public byte[] recipe() { return recipe.clone(); }
        @Override public byte[] project() { return project.clone(); }
    }
    public record Asset(Request request, long size, String sha256) {
        public Asset {
            Objects.requireNonNull(request);
            if (size < 0 || size > StoreIO.MAX_ASSET || sha256 == null || !sha256.matches("[0-9a-f]{64}"))
                throw new IllegalArgumentException("Invalid fingerprint");
        }
    }
    public record Receipt(Asset asset, boolean indexReady, String maintenanceDetail) {}
    public record Recovery(UUID id, String state, String detail) {}

    final Path root;
    final StoreIO io;
    private final FileChannel channel;
    private final FileLock ownership;
    private final Object gate = new Object();
    private final Object serial = new Object();
    private int active;
    private boolean draining, closed;

    public static AssetStore open(Path root) throws IOException { return open(root, NONE); }
    public static AssetStore open(Path root, Faults faults) throws IOException { return open(root, faults, false); }
    static AssetStore open(Path supplied, Faults faults, boolean restoring) throws IOException {
        Path root = supplied.toAbsolutePath().normalize();
        rejectForeignRoot(root);
        StoreIO.directory(root);
        StoreIO io = new StoreIO(faults);
        var preflight = io.read(root.resolve("suite-root.rec"), "root");
        if (preflight.status() == ReadResult.Status.MISSING
                && StoreIO.entries(root, "*").stream().anyMatch(p -> !p.getFileName().toString().equals(".owner.lock")))
            throw new StoreException("FOREIGN_ROOT", "Nonempty unrecognized root refused before lock creation");
        if (preflight.status() != ReadResult.Status.MISSING) preflight.require();
        Path lockPath = root.resolve(".owner.lock");
        StoreIO.regular(lockPath);
        FileChannel channel = FileChannel.open(lockPath, CREATE, WRITE, NOFOLLOW_LINKS);
        FileLock lock = null;
        try {
            try { lock = channel.tryLock(); }
            catch (OverlappingFileLockException busy) { throw new StoreException("IN_USE", "Root already owned in this JVM"); }
            if (lock == null) throw new StoreException("IN_USE", "Root already owned by another process");
            var marker = io.read(root.resolve("suite-root.rec"), "root");
            if (marker.status() == ReadResult.Status.MISSING) {
                if (StoreIO.entries(root, "*").stream().anyMatch(p -> !p.getFileName().toString().equals(".owner.lock")))
                    throw new StoreException("FOREIGN_ROOT", "Nonempty unrecognized root is retained and refused");
                io.immutable(root.resolve("suite-root.rec"), "root", "CCBStudioSuite/assets/a1-001".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            } else if (!Arrays.equals(marker.require(), "CCBStudioSuite/assets/a1-001".getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
                throw new StoreException("FOREIGN_ROOT", "Different root identity");
            }
            if (!restoring) {
                var intent = io.read(root.resolve("restore-intent.rec"), "restore-intent");
                if (intent.status() != ReadResult.Status.MISSING) {
                    byte[] identity = intent.require();
                    var done = io.read(root.resolve("restore-done.rec"), "restore-done");
                    if (done.status() != ReadResult.Status.VALID || !Arrays.equals(identity, done.value()))
                        throw new StoreException("RESTORE_INCOMPLETE", "Resume the explicit restore; ordinary writes are blocked");
                }
            }
            for (String directory : List.of("objects", "records", "transactions", "indexes")) StoreIO.directory(root.resolve(directory));
            return new AssetStore(root, io, channel, lock);
        } catch (IOException | RuntimeException bad) {
            if (lock != null) lock.release();
            channel.close(); throw bad;
        }
    }
    private AssetStore(Path root, StoreIO io, FileChannel channel, FileLock lock) {
        this.root = root; this.io = io; this.channel = channel; this.ownership = lock;
    }
    static void rejectForeignRoot(Path root) throws IOException {
        for (Path component : root) {
            String name = component.toString().toLowerCase(Locale.ROOT);
            if (Set.of("chatchatbar-desktop", "chatchatbardesktop", "chatbar", "chatchatbar").contains(name))
                throw new StoreException("FOREIGN_ROOT", "CCB/Desktop root names are not Suite roots");
        }
    }
    <T> T operation(Callable<T> body) throws IOException {
        synchronized (gate) {
            if (draining || closed) throw new StoreException("CLOSING", "Store is draining or closed");
            // Registration covers both queued and running IO until the outer body settles.
            // Already admitted work uses internal helpers, not a new public admission during drain.
            active++;
        }
        try {
            synchronized (serial) {
                try { return body.call(); }
                catch (IOException | RuntimeException e) { throw e; }
                catch (Exception e) { throw new IOException(e); }
            }
        } finally { synchronized (gate) { active--; gate.notifyAll(); } }
    }
    public Path root() { return root; }
    Path recordPath(UUID id) { return root.resolve("records").resolve(id + ".rec"); }
    Path objectPath(Asset asset) { return root.resolve("objects").resolve(asset.sha256() + ".blob"); }
    private Path transaction(UUID id) { return root.resolve("transactions").resolve(id.toString()); }

    public Receipt put(Request request, InputStream input) throws IOException {
        Objects.requireNonNull(request); Objects.requireNonNull(input);
        return operation(() -> {
            if (EditorSnapshotRetirement.retired(this, request.id())) throw new StoreException("CONFLICT", "Explicitly discarded editor identity cannot be reused");
            var existing = readInternal(request.id());
            if (existing.status() != ReadResult.Status.MISSING)
                return replay(existing.require(), request, input);
            Path tx = transaction(request.id());
            if (Files.exists(tx, NOFOLLOW_LINKS)) {
                recoverOne(request.id());
                var recovered = readInternal(request.id());
                if (recovered.status() == ReadResult.Status.VALID) return replay(recovered.value(), request, input);
                if (recovered.status() != ReadResult.Status.MISSING) recovered.require();
                throw new StoreException("RECOVERY_REQUIRED", "Unsealed transaction retained; inspect its staged bytes before choosing a new operation ID");
            }
            if (request.parentId() != null) readInternal(request.parentId()).require();
            StoreIO.directory(tx);
            io.immutable(tx.resolve("request.rec"), "request", encodeRequest(request));
            io.faults.after(Point.INTENT, tx);
            var fingerprint = io.copyNew(input, tx.resolve("payload.part"));
            io.faults.after(Point.BYTES, tx);
            var asset = new Asset(request, fingerprint.size(), fingerprint.sha256());
            io.immutable(tx.resolve("seal.rec"), "asset", encodeAsset(asset));
            io.faults.after(Point.SEALED, tx);
            return finish(asset, tx);
        });
    }
    private Receipt replay(Asset asset, Request request, InputStream input) throws IOException {
        var fingerprint = StoreIO.transfer(input, OutputStream.nullOutputStream());
        if (!sameRequest(asset.request(), request) || asset.size() != fingerprint.size() || !asset.sha256().equals(fingerprint.sha256()))
            throw new StoreException("CONFLICT", "Operation ID already owns different input or association");
        return new Receipt(asset, indexInternal(request.id()).status() == ReadResult.Status.VALID, "Idempotent existing commit");
    }
    private Receipt finish(Asset asset, Path tx) throws IOException {
        if (asset.request().parentId() != null) readInternal(asset.request().parentId()).require();
        Path object = objectPath(asset);
        if (StoreIO.regular(object)) verifyFile(object, asset).require();
        else {
            verifyFile(tx.resolve("payload.part"), asset).require();
            io.publish(tx.resolve("payload.part"), object, false);
        }
        io.faults.after(Point.ASSET, tx);
        io.immutable(recordPath(asset.request().id()), "asset", encodeAsset(asset));
        // This immutable relationship record is the commit point. Index failure cannot uncommit it.
        try {
            io.faults.after(Point.RECORD, tx);
            io.cache(root.resolve("indexes").resolve(asset.request().id() + ".idx"), "index", encodeAsset(asset));
            io.faults.after(Point.INDEX, tx);
            io.immutable(tx.resolve("complete.rec"), "complete", asset.sha256().getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            io.faults.after(Point.COMPLETE, tx);
            return new Receipt(asset, true, "");
        } catch (IOException maintenance) {
            readInternal(asset.request().id()).require();
            return new Receipt(asset, false, "Committed; index/completion maintenance requires recovery");
        }
    }
    private ReadResult<Asset> verifyFile(Path path, Asset asset) {
        try {
            var f = io.fingerprint(path);
            return f.size() == asset.size() && f.sha256().equals(asset.sha256()) ? ReadResult.valid(asset)
                : ReadResult.error(ReadResult.Status.CORRUPT, "Original size/hash differs; file retained");
        } catch (NoSuchFileException missing) { return ReadResult.error(ReadResult.Status.MISSING, "Referenced original is absent"); }
        catch (IOException | SecurityException bad) { return ReadResult.error(ReadResult.Status.READ_ERROR, "Original cannot be read; retained"); }
    }
    ReadResult<Asset> decodeAssetFile(Path path, String type) {
        var result = io.read(path, type);
        if (result.status() != ReadResult.Status.VALID) return ReadResult.error(result.status(), result.detail());
        try { return ReadResult.valid(decodeAsset(result.value())); }
        catch (IOException | IllegalArgumentException bad) { return ReadResult.error(ReadResult.Status.CORRUPT, "Malformed asset relationship"); }
    }
    ReadResult<Asset> readInternal(UUID id) {
        var result = decodeAssetFile(recordPath(id), "asset");
        if (result.status() != ReadResult.Status.VALID) return result;
        if (!result.value().request().id().equals(id)) return ReadResult.error(ReadResult.Status.CORRUPT, "Record filename/identity mismatch");
        var verified = verifyFile(objectPath(result.value()), result.value());
        if (verified.status() == ReadResult.Status.MISSING)
            return ReadResult.error(ReadResult.Status.CORRUPT, "Committed original missing; no replacement created");
        return verified;
    }
    public ReadResult<Asset> read(UUID id) throws IOException { return operation(() -> readInternal(id)); }
    public void export(UUID id, OutputStream target) throws IOException {
        operation(() -> {
            Asset asset = readInternal(id).require();
            io.faults.before(Io.READ, objectPath(asset));
            try (var in = Files.newInputStream(objectPath(asset), READ, NOFOLLOW_LINKS)) {
                var f = StoreIO.transfer(in, target);
                if (f.size() != asset.size() || !f.sha256().equals(asset.sha256()))
                    throw new StoreException("CORRUPT", "Export checksum failed; caller must not publish its partial output");
            }
            return null;
        });
    }
    private ReadResult<Asset> indexInternal(UUID id) {
        var cached = decodeAssetFile(root.resolve("indexes").resolve(id + ".idx"), "index");
        if (cached.status() != ReadResult.Status.VALID) return cached;
        var authority = readInternal(id);
        if (authority.status() != ReadResult.Status.VALID) return authority;
        return sameAsset(cached.value(), authority.value()) ? cached
                : ReadResult.error(ReadResult.Status.CORRUPT, "Index differs from committed authority");
    }
    public ReadResult<Asset> readIndex(UUID id) throws IOException { return operation(() -> indexInternal(id)); }
    public List<Asset> catalog() throws IOException { return operation(this::catalogInternal); }
    /** Select committed metadata by project, verifying every relationship envelope/identity and
     * every selected payload. This is not a whole-store payload integrity audit; catalog/backup
     * retain that stronger contract. No mutable index is trusted for project selection. */
    public List<Asset> catalogProject(byte[] project) throws IOException {
        byte[] wanted = Objects.requireNonNull(project).clone();
        return operation(() -> catalogInternal(wanted));
    }
    // Only inside an admitted operation; backup must retain its original lifetime registration.
    List<Asset> catalogInternal() throws IOException {
        return catalogInternal(null);
    }
    private List<Asset> catalogInternal(byte[] project) throws IOException {
        if (!Thread.holdsLock(serial)) throw new IllegalStateException("Catalog requires an admitted serialized operation");
        List<Asset> result = new ArrayList<>();
        for (Path record : StoreIO.entries(root.resolve("records"), "*")) {
            String name = record.getFileName().toString();
            if (name.startsWith(".pending-")) {
                canonicalRecordId(name.substring(".pending-".length()));
                // This is the writer's staging-name format, not proof of ownership or a commit.
                // Truncated, unreadable and future envelopes keep their distinct failure states.
                decodeAssetFile(record, "asset").require();
                throw new StoreException("RECOVERY_REQUIRED", "Pending record publication retained: " + name
                        + "; explicit inspection required before claiming a complete catalog");
            }
            if (!name.endsWith(".rec")) throw new StoreException("CORRUPT", "Unknown record entry retained: " + name);
            UUID id = canonicalRecordId(name.substring(0, name.length() - ".rec".length()));
            if (project == null) result.add(readInternal(id).require());
            else {
                Asset candidate = decodeAssetFile(record, "asset").require();
                if (!candidate.request().id().equals(id)) throw new StoreException("CORRUPT", "Record filename/identity mismatch");
                if (Arrays.equals(candidate.request().project(), project)) result.add(readInternal(id).require());
            }
        }
        return List.copyOf(result);
    }
    private static UUID canonicalRecordId(String name) throws StoreException {
        try {
            UUID id = UUID.fromString(name);
            if (name.equals(id.toString())) return id;
        } catch (IllegalArgumentException invalid) { /* Retain unknown or noncanonical entries. */ }
        throw new StoreException("CORRUPT", "Unknown or noncanonical record filename retained: " + name);
    }
    public int rebuildIndex() throws IOException {
        return operation(() -> {
            var assets = catalogInternal(); // Never rebuild from a silently partial scan.
            for (Asset asset : assets) io.cache(root.resolve("indexes").resolve(asset.request().id() + ".idx"), "index", encodeAsset(asset));
            return assets.size();
        });
    }
    public List<Recovery> recover() throws IOException {
        return operation(() -> {
            List<Recovery> report = new ArrayList<>();
            for (Path tx : StoreIO.entries(root.resolve("transactions"), "*")) {
                try { report.add(recoverOne(UUID.fromString(tx.getFileName().toString()))); }
                catch (IllegalArgumentException bad) { throw new StoreException("CORRUPT", "Unknown transaction directory retained"); }
            }
            return List.copyOf(report);
        });
    }
    private Recovery recoverOne(UUID id) {
        Path tx = transaction(id);
        try {
            if (EditorSnapshotRetirement.retired(this,id)) return new Recovery(id,"EDITOR_RETIRED","Explicit editor deletion; transaction is not replayed");
            StoreIO.safeChain(tx);
            var request = io.read(tx.resolve("request.rec"), "request");
            if (request.status() != ReadResult.Status.VALID) return new Recovery(id, request.status().name(), request.detail());
            Request original;
            try { original = decodeRequest(request.value()); }
            catch (IOException | IllegalArgumentException malformed) {
                return new Recovery(id, "CORRUPT", "Malformed intent retained");
            }
            if (!original.id().equals(id)) return new Recovery(id, "CORRUPT", "Transaction ID mismatch");
            var seal = decodeAssetFile(tx.resolve("seal.rec"), "asset");
            if (seal.status() == ReadResult.Status.MISSING) return new Recovery(id, "INCOMPLETE", "Unsealed bytes retained for explicit recovery; not Saved");
            if (seal.status() != ReadResult.Status.VALID) return new Recovery(id, seal.status().name(), seal.detail());
            if (!sameRequest(original, seal.value().request())) return new Recovery(id, "CORRUPT", "Intent/seal mismatch");
            // Immutable commit conflicts/unknown versions are preserved, never overwritten.
            var existing = readInternal(id);
            if (existing.status() != ReadResult.Status.MISSING && existing.status() != ReadResult.Status.VALID)
                return new Recovery(id, existing.status().name(), existing.detail());
            Receipt receipt = finish(seal.value(), tx);
            return new Recovery(id, receipt.indexReady() ? "COMPLETED" : "COMMITTED_MAINTENANCE_REQUIRED", receipt.maintenanceDetail());
        } catch (StoreException failure) { return new Recovery(id, failure.code(), failure.getMessage()); }
        catch (IOException | IllegalArgumentException failure) { return new Recovery(id, "READ_ERROR", "Recovery failed; all existing bytes retained"); }
    }
    public boolean close(Duration timeout) throws IOException {
        long deadline = System.nanoTime() + timeout.toNanos();
        synchronized (gate) {
            draining = true;
            if (closed) return true;
            while (active > 0) {
                long left = deadline - System.nanoTime();
                if (left <= 0) return false; // Keep lock and draining state on timeout.
                try { gate.wait(Math.max(1, left / 1_000_000)); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); return false; }
            }
            ownership.release(); channel.close(); closed = true; return true;
        }
    }
    @Override public void close() throws IOException {
        if (!close(Duration.ofSeconds(30))) throw new StoreException("DRAIN_TIMEOUT", "Root ownership retained until active IO settles");
    }
    static boolean sameRequest(Request a, Request b) {
        return a.id().equals(b.id()) && a.name().equals(b.name()) && a.kind() == b.kind()
            && Objects.equals(a.parentId(), b.parentId()) && Arrays.equals(a.recipe(), b.recipe()) && Arrays.equals(a.project(), b.project());
    }
    static boolean sameAsset(Asset a, Asset b) { return sameRequest(a.request(), b.request()) && a.size() == b.size() && a.sha256().equals(b.sha256()); }
    static byte[] encodeRequest(Request request) throws IOException {
        var bytes = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(bytes)) { writeRequest(out, request); }
        return bytes.toByteArray();
    }
    private static void writeRequest(DataOutputStream out, Request r) throws IOException {
        out.writeUTF(r.id().toString()); out.writeUTF(r.name()); out.writeUTF(r.kind().name());
        out.writeUTF(r.parentId() == null ? "" : r.parentId().toString());
        for (byte[] bytes : List.of(r.recipe(), r.project())) { out.writeInt(bytes.length); out.write(bytes); }
    }
    static Request decodeRequest(byte[] bytes) throws IOException {
        try (var in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            Request request = readRequest(in); if (in.read() != -1) throw new IOException("Unknown request fields"); return request;
        }
    }
    private static Request readRequest(DataInputStream in) throws IOException {
        UUID id = UUID.fromString(in.readUTF()); String name = in.readUTF(); Kind kind = Kind.valueOf(in.readUTF());
        String parent = in.readUTF(); byte[][] documents = new byte[2][];
        for (int i = 0; i < 2; i++) {
            int size = in.readInt(); if (size < 0 || size > 1024 * 1024) throw new IOException("Document size invalid");
            documents[i] = in.readNBytes(size); if (documents[i].length != size) throw new EOFException();
        }
        return new Request(id, name, kind, parent.isEmpty() ? null : UUID.fromString(parent), documents[0], documents[1]);
    }
    static byte[] encodeAsset(Asset asset) throws IOException {
        var bytes = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(bytes)) { writeRequest(out, asset.request()); out.writeLong(asset.size()); out.writeUTF(asset.sha256()); }
        return bytes.toByteArray();
    }
    static Asset decodeAsset(byte[] bytes) throws IOException {
        try (var in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            var asset = new Asset(readRequest(in), in.readLong(), in.readUTF());
            if (in.read() != -1) throw new IOException("Unknown relationship fields"); return asset;
        }
    }
}
