package io.github.misakapiano.ccbstudio.assets;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;

/** Minimal local snapshots. No secrets, caches or credentials; no restore over a live populated root. */
public final class AssetBackup {
    private AssetBackup() {}
    public record Entry(String relativePath, long size, String sha256) {}
    public record Report(int files, int committedAssets) {}

    public static Report create(AssetStore store, Path supplied) throws IOException {
        return store.operation(() -> {
            store.io.faults.after(AssetStore.Point.BACKUP_REGISTERED, store.root);
            Path destination = supplied.toAbsolutePath().normalize();
            disjoint(store.root, destination);
            AssetStore.rejectForeignRoot(destination);
            StoreIO.directory(destination);
            if (!StoreIO.entries(destination, "*").isEmpty()) throw new StoreException("CONFLICT", "Backup target must be empty");
            int assets = store.catalogInternal().size(); // Already admitted: finish under this registration even during drain.
            List<Path> sourceFiles = new ArrayList<>();
            sourceFiles.add(store.root.resolve("suite-root.rec"));
            for (String directory : List.of("objects", "records", "transactions")) {
                try (var paths = Files.walk(store.root.resolve(directory))) {
                    for (Path p : paths.toList()) {
                        if (Files.isDirectory(p, NOFOLLOW_LINKS)) { StoreIO.safeChain(p); continue; }
                        if (!StoreIO.regular(p)) throw new IOException("Backup source missing");
                        sourceFiles.add(p);
                    }
                }
            }
            sourceFiles.sort(Comparator.comparing(Path::toString));
            List<Entry> manifest = new ArrayList<>();
            for (Path source : sourceFiles) {
                String relative = store.root.relativize(source).toString().replace('\\', '/');
                validateRelative(relative);
                var expected = store.io.fingerprint(source);
                try (var input = Files.newInputStream(source, NOFOLLOW_LINKS)) {
                    var actual = store.io.copyNew(input, destination.resolve(relative));
                    if (!actual.equals(expected)) throw new StoreException("CORRUPT", "Snapshot source changed during copy");
                }
                manifest.add(new Entry(relative, expected.size(), expected.sha256()));
                store.io.faults.after(AssetStore.Point.BACKUP_FILE, destination.resolve(relative));
            }
            store.io.immutable(destination.resolve("backup-manifest.rec"), "backup-manifest", encode(manifest));
            store.io.faults.after(AssetStore.Point.BACKUP_MANIFEST, destination);
            validate(destination, store.io);
            return new Report(manifest.size(), assets);
        });
    }

    public static Report restore(Path suppliedBackup, Path suppliedRoot) throws IOException {
        return restore(suppliedBackup, suppliedRoot, AssetStore.NONE);
    }
    public static Report restore(Path suppliedBackup, Path suppliedRoot, AssetStore.Faults faults) throws IOException {
        Path backup = suppliedBackup.toAbsolutePath().normalize(), root = suppliedRoot.toAbsolutePath().normalize();
        disjoint(backup, root);
        StoreIO io = new StoreIO(faults);
        List<Entry> entries = validate(backup, io); // Complete verification before touching destination.
        byte[] identity = StoreIO.hash(io.raw(backup.resolve("backup-manifest.rec"), StoreIO.MAX_METADATA + 128))
                .getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        try (AssetStore destination = AssetStore.open(root, faults, true)) {
            return destination.operation(() -> {
                Path intent = root.resolve("restore-intent.rec");
                if (!StoreIO.regular(intent)) {
                    if (!destination.catalogInternal().isEmpty()
                            || !StoreIO.entries(root.resolve("transactions"), "*").isEmpty()
                            || !StoreIO.entries(root.resolve("objects"), "*").isEmpty())
                        throw new StoreException("CONFLICT", "Restore requires a new empty Suite root");
                    io.immutable(intent, "restore-intent", identity);
                } else if (!Arrays.equals(io.read(intent, "restore-intent").require(), identity)) {
                    throw new StoreException("CONFLICT", "Pending restore belongs to a different backup");
                }
                for (Entry entry : entries) {
                    Path target = root.resolve(entry.relativePath());
                    if (StoreIO.regular(target)) {
                        if (!matches(io.fingerprint(target), entry)) throw new StoreException("CONFLICT", "Restore target differs; retained");
                    } else {
                        // A partial copy is retained under a distinct owned staging filename. Resume never replaces unknown target bytes.
                        Path staging = root.resolve(".restore-staging").resolve(UUID.randomUUID() + ".part");
                        try (var input = Files.newInputStream(backup.resolve(entry.relativePath()), NOFOLLOW_LINKS)) {
                            if (!matches(io.copyNew(input, staging), entry)) throw new StoreException("CORRUPT", "Backup changed during restore");
                        }
                        io.publish(staging, target, false);
                    }
                    io.faults.after(AssetStore.Point.RESTORE_FILE, target);
                }
                int count = destination.catalogInternal().size();
                // Preserve pending transactions too; they remain explicit recovery work after reopening.
                io.immutable(root.resolve("restore-done.rec"), "restore-done", identity);
                io.faults.after(AssetStore.Point.RESTORE_COMPLETE, root);
                return new Report(entries.size(), count);
            });
        }
    }
    static void disjoint(Path a, Path b) throws IOException {
        StoreIO.safeChain(a); StoreIO.safeChain(b);
        if (a.startsWith(b) || b.startsWith(a)) throw new StoreException("UNSAFE_PATH", "Snapshot and root must not overlap");
    }
    private static boolean matches(StoreIO.Fingerprint fingerprint, Entry entry) {
        return fingerprint.size() == entry.size() && fingerprint.sha256().equals(entry.sha256());
    }
    private static List<Entry> validate(Path backup, StoreIO io) throws IOException {
        StoreIO.safeChain(backup);
        byte[] bytes = io.read(backup.resolve("backup-manifest.rec"), "backup-manifest").require();
        List<Entry> entries = decode(bytes);
        Set<String> expected = new HashSet<>(); expected.add("backup-manifest.rec");
        for (Entry entry : entries) {
            validateRelative(entry.relativePath());
            if (!expected.add(entry.relativePath())) throw new StoreException("CORRUPT", "Duplicate snapshot entry");
            if (!matches(io.fingerprint(backup.resolve(entry.relativePath())), entry))
                throw new StoreException("CORRUPT", "Snapshot size/hash mismatch");
        }
        if (!expected.contains("suite-root.rec")) throw new StoreException("CORRUPT", "Snapshot has no root identity");
        try (var paths = Files.walk(backup)) {
            for (Path p : paths.toList()) {
                if (Files.isDirectory(p, NOFOLLOW_LINKS)) { StoreIO.safeChain(p); continue; }
                if (!StoreIO.regular(p) || !expected.contains(backup.relativize(p).toString().replace('\\', '/')))
                    throw new StoreException("CORRUPT", "Unexpected snapshot file retained");
            }
        }
        return entries;
    }
    private static void validateRelative(String relative) throws IOException {
        String uuid = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
        if (relative.equals("suite-root.rec")
                || relative.matches("objects/[0-9a-f]{64}\\.blob")
                || relative.matches("records/(" + uuid + "\\.rec|\\.pending-" + uuid + ")")
                || relative.matches("transactions/" + uuid + "/(request\\.rec|seal\\.rec|complete\\.rec|payload\\.part|\\.pending-" + uuid + ")")) return;
        throw new StoreException("CORRUPT", "Unrecognized or unsafe snapshot path");
    }
    private static byte[] encode(List<Entry> entries) throws IOException {
        var bytes = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(bytes)) {
            out.writeInt(entries.size());
            for (Entry e : entries) { out.writeUTF(e.relativePath()); out.writeLong(e.size()); out.writeUTF(e.sha256()); }
        }
        return bytes.toByteArray();
    }
    private static List<Entry> decode(byte[] bytes) throws IOException {
        try (var in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            int count = in.readInt();
            if (count < 1 || count > 20_000) throw new StoreException("CORRUPT", "Snapshot entry count invalid");
            List<Entry> entries = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                var e = new Entry(in.readUTF(), in.readLong(), in.readUTF());
                if (e.size() < 0 || e.size() > StoreIO.MAX_ASSET || !e.sha256().matches("[0-9a-f]{64}"))
                    throw new StoreException("CORRUPT", "Invalid snapshot fingerprint");
                entries.add(e);
            }
            if (in.read() != -1) throw new StoreException("CORRUPT", "Unknown snapshot fields retained");
            return List.copyOf(entries);
        } catch (EOFException invalid) { throw new StoreException("CORRUPT", "Truncated snapshot manifest"); }
    }
}
