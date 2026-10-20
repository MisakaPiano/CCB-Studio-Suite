package io.github.misakapiano.ccbstudio.assets;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Explicit, byte-preserving reconciliation of proven duplicate staging records. No delete API. */
public final class AssetMaintenance {
    private AssetMaintenance() {}
    public enum Category { COMMITTED, PENDING, UNKNOWN_NAME }
    public record Entry(String name, Category category, ReadResult.Status status,
                        long size, String sha256, UUID assetId, String detail) {}
    public record Reconciliation(UUID id, String sourceName, String sha256, String state) {}
    private record Intent(String name, long size, String sha256, UUID assetId) {}

    public static List<Entry> inspect(AssetStore store) throws IOException {
        return store.operation(() -> {
            List<Entry> result = new ArrayList<>();
            for (Path path : StoreIO.entries(store.root.resolve("records"), "*")) {
                String name = path.getFileName().toString();
                UUID id = canonical(name.endsWith(".rec") ? name.substring(0, name.length()-4) : "");
                UUID stagingId = name.startsWith(".pending-") ? canonical(name.substring(9)) : null;
                Category category = id != null ? Category.COMMITTED : stagingId != null ? Category.PENDING : Category.UNKNOWN_NAME;
                var decoded = id != null ? store.readInternal(id) : store.decodeAssetFile(path, "asset");
                ReadResult.Status status = category == Category.UNKNOWN_NAME ? ReadResult.Status.CORRUPT : decoded.status();
                long size = -1; String hash = "";
                try { var f = store.io.fingerprint(path); size = f.size(); hash = f.sha256(); }
                catch (IOException denied) { status = ReadResult.Status.READ_ERROR; }
                result.add(new Entry(name, category, status, size, hash,
                        decoded.status() == ReadResult.Status.VALID ? decoded.value().request().id() : null,
                        category == Category.UNKNOWN_NAME ? "Unknown entry retained; no automatic disposition" : decoded.detail()));
            }
            return List.copyOf(result);
        });
    }

    /** Move only a valid staging candidate already duplicated by a verified committed record.
     * Caller supplies the fingerprint returned by inspect; stale inspection never authorizes new bytes. */
    public static Reconciliation reconcilePending(AssetStore store, String name, long size, String sha256) throws IOException {
        return store.operation(() -> {
            if (!name.startsWith(".pending-") || canonical(name.substring(9)) == null)
                throw new StoreException("REFUSED", "Only canonical staging candidates can be reconciled");
            if (size < 0 || sha256 == null || !sha256.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("Fingerprint required");
            Path source = store.root.resolve("records").resolve(name);
            var asset = store.decodeAssetFile(source, "asset").require();
            Intent intent = new Intent(name, size, sha256, asset.request().id());
            verifyCandidate(store, source, intent);
            UUID id = UUID.randomUUID(); Path journal = journals(store).resolve(id.toString());
            store.io.immutable(journal.resolve("intent.rec"), "reconcile-intent", encode(intent));
            store.io.faults.after(AssetStore.Point.MAINTENANCE_INTENT, journal);
            return finish(store, id, intent);
        });
    }

    /** Re-enter durable intents; unknown/missing/corrupt versions are refused and preserved. */
    public static List<Reconciliation> recover(AssetStore store) throws IOException {
        return store.operation(() -> {
            if (!Files.exists(journals(store), LinkOption.NOFOLLOW_LINKS)) return List.of();
            List<Reconciliation> results = new ArrayList<>();
            for (Path path : StoreIO.entries(journals(store), "*")) {
                UUID id = canonical(path.getFileName().toString());
                if (id == null) throw new StoreException("CORRUPT", "Unknown maintenance entry retained");
                Intent intent = decode(store.io.read(path.resolve("intent.rec"), "reconcile-intent").require());
                results.add(finish(store, id, intent));
            }
            return List.copyOf(results);
        });
    }

    private static Reconciliation finish(AssetStore store, UUID id, Intent intent) throws IOException {
        Path journal = journals(store).resolve(id.toString());
        Path source = store.root.resolve("records").resolve(intent.name());
        Path held = journal.resolve("candidate.rec");
        boolean sourceExists = StoreIO.regular(source), heldExists = StoreIO.regular(held);
        if (sourceExists && heldExists) throw new StoreException("CONFLICT", "Both staging locations exist; all bytes retained");
        if (!sourceExists && !heldExists) throw new StoreException("MISSING", "Reconciliation bytes missing; no default created");
        verifyCandidate(store, heldExists ? held : source, intent);
        if (!heldExists) store.io.publish(source, held, false);
        store.io.faults.after(AssetStore.Point.MAINTENANCE_MOVED, journal);
        verifyCandidate(store, held, intent);
        store.io.immutable(journal.resolve("complete.rec"), "reconcile-complete", encode(intent));
        store.io.faults.after(AssetStore.Point.MAINTENANCE_COMPLETE, journal);
        return new Reconciliation(id, intent.name(), intent.sha256(), "PRESERVED_DUPLICATE");
    }
    private static void verifyCandidate(AssetStore store, Path path, Intent intent) throws IOException {
        var f = store.io.fingerprint(path);
        if (f.size() != intent.size() || !f.sha256().equals(intent.sha256()))
            throw new StoreException("CONFLICT", "Staging fingerprint changed; retained");
        var candidate = store.decodeAssetFile(path, "asset").require();
        var committed = store.readInternal(intent.assetId()).require();
        if (!candidate.request().id().equals(intent.assetId()) || !AssetStore.sameAsset(candidate, committed))
            throw new StoreException("CONFLICT", "Not a proven duplicate of a verified commit; retained");
    }
    private static Path journals(AssetStore store) { return store.root.resolve("maintenance/reconciliations"); }
    private static UUID canonical(String value) {
        try { UUID id = UUID.fromString(value); return id.toString().equals(value) ? id : null; }
        catch (IllegalArgumentException invalid) { return null; }
    }
    private static byte[] encode(Intent i) throws IOException {
        var bytes = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(bytes)) {
            out.writeUTF(i.name()); out.writeLong(i.size()); out.writeUTF(i.sha256()); out.writeUTF(i.assetId().toString());
        }
        return bytes.toByteArray();
    }
    private static Intent decode(byte[] bytes) throws IOException {
        try (var in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            String name = in.readUTF(); long size = in.readLong(); String hash = in.readUTF(); UUID asset = canonical(in.readUTF());
            if (!name.startsWith(".pending-") || canonical(name.substring(9)) == null || size < 0 || size > StoreIO.MAX_ASSET
                    || !hash.matches("[0-9a-f]{64}") || asset == null || in.read() != -1)
                throw new StoreException("CORRUPT", "Invalid maintenance intent retained");
            return new Intent(name, size, hash, asset);
        } catch (EOFException invalid) { throw new StoreException("CORRUPT", "Truncated maintenance intent retained"); }
    }
}
