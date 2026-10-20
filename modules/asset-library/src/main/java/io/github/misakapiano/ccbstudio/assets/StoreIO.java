package io.github.misakapiano.ccbstudio.assets;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.*;
import java.util.*;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardOpenOption.*;

/** Private, bounded v1 envelope for this slice; not a CCB, Recipe or project schema. */
final class StoreIO {
    static final int MAX_METADATA = 3 * 1024 * 1024;
    static final long MAX_ASSET = 256L * 1024 * 1024;
    static final int BUFFER = 64 * 1024;
    private static final int MAGIC = 0x43434241;
    final AssetStore.Faults faults;
    StoreIO(AssetStore.Faults faults) { this.faults = faults; }

    static MessageDigest digest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException e) { throw new AssertionError(e); }
    }
    static String hash(byte[] bytes) { return HexFormat.of().formatHex(digest().digest(bytes)); }

    static void safeChain(Path path) throws IOException {
        Path absolute = path.toAbsolutePath().normalize();
        Path cursor = absolute;
        while (cursor != null) {
            try {
                var a = Files.readAttributes(cursor, BasicFileAttributes.class, NOFOLLOW_LINKS);
                if (a.isSymbolicLink() || a.isOther() || !a.isDirectory()
                        || !cursor.toRealPath().equals(cursor))
                    throw new StoreException("UNSAFE_PATH", "Non-directory or redirected path component: " + cursor);
                // Real-path equality also rejects redirected ancestors, without repeatedly resolving every prefix.
                return;
            } catch (NoSuchFileException absent) { cursor = cursor.getParent(); }
        }
    }
    static void directory(Path path) throws IOException {
        safeChain(path);
        Files.createDirectories(path);
        safeChain(path);
    }
    static boolean regular(Path path) throws IOException {
        safeChain(path.getParent());
        try {
            var a = Files.readAttributes(path, BasicFileAttributes.class, NOFOLLOW_LINKS);
            if (!a.isRegularFile() || a.isSymbolicLink() || a.isOther())
                throw new IOException("Not a safe regular file: " + path);
            return true;
        } catch (NoSuchFileException absent) { return false; }
    }
    static List<Path> entries(Path dir, String glob) throws IOException {
        safeChain(dir);
        List<Path> entries = new ArrayList<>();
        try (var stream = Files.newDirectoryStream(dir, glob)) {
            for (Path p : stream) entries.add(p);
        }
        entries.sort(Comparator.comparing(p -> p.getFileName().toString()));
        return entries;
    }
    byte[] raw(Path path, int bound) throws IOException {
        faults.before(AssetStore.Io.READ, path);
        if (!regular(path)) throw new NoSuchFileException(path.toString());
        try (var in = Files.newInputStream(path, READ, NOFOLLOW_LINKS)) {
            byte[] data = in.readNBytes(bound + 1);
            if (data.length > bound) throw new StoreException("CORRUPT", "Metadata exceeds bound");
            return data;
        }
    }
    static byte[] encode(String type, byte[] payload) throws IOException {
        if (payload.length > MAX_METADATA) throw new IOException("Metadata exceeds bound");
        var buffer = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(buffer)) {
            out.writeInt(MAGIC); out.writeInt(1); out.writeUTF(type);
            out.writeInt(payload.length); out.write(digest().digest(payload)); out.write(payload);
        }
        return buffer.toByteArray();
    }
    ReadResult<byte[]> read(Path path, String type) {
        try {
            byte[] data = raw(path, MAX_METADATA + 128);
            try (var in = new DataInputStream(new ByteArrayInputStream(data))) {
                if (in.readInt() != MAGIC) return ReadResult.error(ReadResult.Status.CORRUPT, "Envelope magic mismatch");
                if (in.readInt() != 1) return ReadResult.error(ReadResult.Status.UNSUPPORTED_VERSION, "Unknown envelope version; bytes retained");
                if (!in.readUTF().equals(type)) return ReadResult.error(ReadResult.Status.CORRUPT, "Envelope type mismatch");
                int size = in.readInt();
                if (size < 0 || size > MAX_METADATA) return ReadResult.error(ReadResult.Status.CORRUPT, "Invalid metadata size");
                byte[] expected = in.readNBytes(32), payload = in.readNBytes(size);
                if (payload.length != size || in.read() != -1 || !Arrays.equals(expected, digest().digest(payload)))
                    return ReadResult.error(ReadResult.Status.CORRUPT, "Truncated or altered envelope");
                return ReadResult.valid(payload);
            } catch (IOException bad) { return ReadResult.error(ReadResult.Status.CORRUPT, "Malformed envelope"); }
        } catch (NoSuchFileException absent) { return ReadResult.error(ReadResult.Status.MISSING, "Path absent"); }
        catch (StoreException bad) { return ReadResult.error(ReadResult.Status.CORRUPT, bad.getMessage()); }
        catch (IOException | SecurityException bad) { return ReadResult.error(ReadResult.Status.READ_ERROR, "Metadata cannot be read; bytes retained"); }
    }
    void writeNew(Path target, byte[] bytes) throws IOException {
        directory(target.getParent());
        faults.before(AssetStore.Io.WRITE, target);
        try (var channel = FileChannel.open(target, CREATE_NEW, WRITE, NOFOLLOW_LINKS)) {
            var buffer = ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) channel.write(buffer);
            channel.force(true);
        }
    }
    void immutable(Path target, String type, byte[] payload) throws IOException {
        byte[] encoded = encode(type, payload);
        if (regular(target)) {
            read(target, type).require(); // Unknown/corrupt content must not become a replacement.
            if (!Arrays.equals(raw(target, MAX_METADATA + 128), encoded))
                throw new StoreException("CONFLICT", "Existing immutable document differs");
            return;
        }
        Path temporary = target.resolveSibling(".pending-" + UUID.randomUUID());
        writeNew(temporary, encoded);
        publish(temporary, target, false);
    }
    void publish(Path source, Path target, boolean replace) throws IOException {
        directory(target.getParent());
        if (!regular(source)) throw new NoSuchFileException(source.toString());
        boolean exists = regular(target);
        if (exists && !replace) throw new FileAlreadyExistsException(target.toString());
        faults.before(AssetStore.Io.MOVE, target);
        // Atomic rename is required. No silent non-atomic replacement fallback.
        if (replace) Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        else Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
    }
    record Fingerprint(long size, String sha256) {}
    Fingerprint fingerprint(Path file) throws IOException {
        faults.before(AssetStore.Io.READ, file);
        if (!regular(file)) throw new NoSuchFileException(file.toString());
        try (var in = Files.newInputStream(file, READ, NOFOLLOW_LINKS)) { return transfer(in, OutputStream.nullOutputStream()); }
    }
    static Fingerprint transfer(InputStream in, OutputStream out) throws IOException {
        var md = digest(); byte[] buffer = new byte[BUFFER]; long size = 0;
        for (int n; (n = in.read(buffer)) != -1;) {
            if (n == 0) continue;
            size = Math.addExact(size, n);
            if (size > MAX_ASSET) throw new IOException("Asset exceeds 256 MiB slice limit");
            out.write(buffer, 0, n); md.update(buffer, 0, n);
        }
        return new Fingerprint(size, HexFormat.of().formatHex(md.digest()));
    }
    Fingerprint copyNew(InputStream in, Path path) throws IOException {
        directory(path.getParent()); faults.before(AssetStore.Io.WRITE, path);
        try (var channel = FileChannel.open(path, CREATE_NEW, WRITE, NOFOLLOW_LINKS)) {
            var out = java.nio.channels.Channels.newOutputStream(channel);
            var result = transfer(in, out); channel.force(true); return result;
        }
    }
    void cache(Path target, String type, byte[] payload) throws IOException {
        byte[] bytes = encode(type, payload);
        if (regular(target)) {
            byte[] previous = raw(target, MAX_METADATA + 128);
            if (Arrays.equals(previous, bytes)) return;
            writeNew(target.getParent().resolve("observed").resolve(UUID.randomUUID() + ".raw"), previous);
        }
        Path pending = target.resolveSibling(".pending-" + UUID.randomUUID());
        writeNew(pending, bytes); publish(pending, target, true);
    }
}
