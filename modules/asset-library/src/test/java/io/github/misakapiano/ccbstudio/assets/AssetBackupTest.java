package io.github.misakapiano.ccbstudio.assets;

import org.junit.Test;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;

public class AssetBackupTest {
    private Path fixture() throws IOException {
        Path parent = Path.of(System.getProperty("asset.test.root")); Files.createDirectories(parent);
        return Files.createDirectory(parent.resolve(UUID.randomUUID().toString()));
    }
    private void expect(String code, Checked action) throws Exception {
        try { action.run(); fail("Expected " + code); }
        catch (StoreException e) { assertEquals(code, e.code()); }
    }
    @FunctionalInterface interface Checked { void run() throws Exception; }

    @Test public void D07_snapshotRestoresOriginalsDocumentsAndPendingInputButExcludesSecrets() throws Exception {
        Path f = fixture(), source = f.resolve("source"), backup = f.resolve("backup"), restored = f.resolve("restored");
        var r = StoreProcess.request(UUID.randomUUID());
        var pending = StoreProcess.request(UUID.randomUUID());
        try (var s = AssetStore.open(source)) {
            s.put(r, new ByteArrayInputStream(StoreProcess.PAYLOAD));
            try { s.put(pending, new InputStream() { @Override public int read() throws IOException { throw new IOException("synthetic source failure"); } }); fail(); }
            catch (IOException expected) { }
            Files.createDirectory(source.resolve("secrets")); Files.writeString(source.resolve("secrets/test-only.txt"), "synthetic-do-not-export");
            Files.writeString(source.resolve("unrelated.txt"), "not-owned-by-backup");
            var report = AssetBackup.create(s, backup); assertEquals(1, report.committedAssets());
            assertFalse(Files.exists(backup.resolve(".owner.lock"))); assertFalse(Files.exists(backup.resolve("indexes")));
            assertFalse(Files.exists(backup.resolve("secrets"))); assertFalse(Files.exists(backup.resolve("unrelated.txt")));
            expect("UNSAFE_PATH", () -> AssetBackup.create(s, source.resolve("nested")));
        }
        assertEquals(1, AssetBackup.restore(backup, restored).committedAssets());
        try (var s = AssetStore.open(restored)) {
            assertArrayEquals(r.recipe(), s.read(r.id()).require().request().recipe());
            assertArrayEquals(r.project(), s.read(r.id()).require().request().project());
            var out = new ByteArrayOutputStream(); s.export(r.id(), out); assertArrayEquals(StoreProcess.PAYLOAD, out.toByteArray());
            assertTrue(s.recover().stream().anyMatch(item -> item.id().equals(pending.id()) && item.state().equals("INCOMPLETE")));
            assertEquals(1, s.rebuildIndex());
        }
        assertTrue(Files.exists(source.resolve("secrets/test-only.txt")));
    }
    @Test public void D07_interruptedBackupIsNotACompleteBackupAndSourceStaysUsable() throws Exception {
        Path f = fixture(), root = f.resolve("source"), backup = f.resolve("backup"), target = f.resolve("restore");
        var r = StoreProcess.request(UUID.randomUUID());
        try (var s = AssetStore.open(root, new AssetStore.Faults() {
            @Override public void after(AssetStore.Point p, Path path) throws IOException {
                if (p == AssetStore.Point.BACKUP_FILE) throw new IOException("injected interrupted backup");
            }
        })) {
            s.put(r, new ByteArrayInputStream(StoreProcess.PAYLOAD));
            try { AssetBackup.create(s, backup); fail(); } catch (IOException expected) { }
            assertEquals(ReadResult.Status.VALID, s.read(r.id()).status());
            assertFalse(Files.exists(backup.resolve("backup-manifest.rec")));
        }
        expect("MISSING", () -> AssetBackup.restore(backup, target)); assertFalse(Files.exists(target));
    }
    @Test public void D07_interruptedRestoreBlocksNormalOpenAndResumesWithoutReplacingContent() throws Exception {
        Path f = fixture(), source = f.resolve("source"), backup = f.resolve("backup"), target = f.resolve("target");
        var r = StoreProcess.request(UUID.randomUUID());
        try (var s = AssetStore.open(source)) { s.put(r, new ByteArrayInputStream(StoreProcess.PAYLOAD)); AssetBackup.create(s, backup); }
        try {
            AssetBackup.restore(backup, target, new AssetStore.Faults() {
                @Override public void after(AssetStore.Point p, Path path) throws IOException {
                    if (p == AssetStore.Point.RESTORE_FILE) throw new IOException("injected restore interruption");
                }
            }); fail();
        } catch (IOException expected) { }
        expect("RESTORE_INCOMPLETE", () -> AssetStore.open(target));
        assertEquals(1, AssetBackup.restore(backup, target).committedAssets());
        try (var s = AssetStore.open(target)) { assertEquals(ReadResult.Status.VALID, s.read(r.id()).status()); }
        assertEquals(1, AssetBackup.restore(backup, target).committedAssets());
    }
    @Test public void D07_badBackupIsRejectedBeforeDestinationAndCannotOverwritePopulatedRoot() throws Exception {
        Path f = fixture(), source = f.resolve("source"), backup = f.resolve("backup"), target = f.resolve("target");
        var r = StoreProcess.request(UUID.randomUUID());
        try (var s = AssetStore.open(source)) { s.put(r, new ByteArrayInputStream(StoreProcess.PAYLOAD)); AssetBackup.create(s, backup); }
        try (var s = AssetStore.open(target)) { s.put(StoreProcess.request(UUID.randomUUID()), new ByteArrayInputStream(new byte[]{3})); }
        expect("CONFLICT", () -> AssetBackup.restore(backup, target));
        try (var s = AssetStore.open(target)) { assertEquals(1, s.catalog().size()); }
        Path object = StoreIO.entries(backup.resolve("objects"), "*").get(0); Files.write(object, new byte[]{4});
        Path untouched = f.resolve("never-created");
        expect("CORRUPT", () -> AssetBackup.restore(backup, untouched)); assertFalse(Files.exists(untouched));
        assertArrayEquals(new byte[]{4}, Files.readAllBytes(object));
    }
    @Test public void D07_restorePartialCopyRemainsRecoverableAndChangedTargetIsNotOverwritten() throws Exception {
        Path f = fixture(), source = f.resolve("source"), backup = f.resolve("backup"), target = f.resolve("target");
        var r = StoreProcess.request(UUID.randomUUID());
        try (var s = AssetStore.open(source)) { s.put(r, new ByteArrayInputStream(StoreProcess.PAYLOAD)); AssetBackup.create(s, backup); }
        var once = new AtomicBoolean();
        try {
            AssetBackup.restore(backup, target, new AssetStore.Faults() {
                @Override public void before(AssetStore.Io op, Path path) throws IOException {
                    if (op == AssetStore.Io.MOVE && path.toString().endsWith(".blob") && once.compareAndSet(false, true))
                        throw new AccessDeniedException(path.toString());
                }
            }); fail();
        } catch (AccessDeniedException expected) { }
        assertEquals(1, StoreIO.entries(target.resolve(".restore-staging"), "*.part").size());
        assertEquals(1, AssetBackup.restore(backup, target).committedAssets());
        Path object = StoreIO.entries(target.resolve("objects"), "*.blob").get(0); Files.write(object, new byte[]{6});
        expect("CONFLICT", () -> AssetBackup.restore(backup, target));
        assertArrayEquals(new byte[]{6}, Files.readAllBytes(object));
    }
}
