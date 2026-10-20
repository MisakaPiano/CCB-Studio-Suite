package io.github.misakapiano.ccbstudio.assets;

import org.junit.Test;
import java.io.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;

/** R1 regressions use synthetic bytes and bounded barriers, never timing sleeps. */
public class AssetSafetyTest {
    private Path fixture() throws IOException {
        Path parent = Path.of(System.getProperty("asset.test.root"));
        Files.createDirectories(parent);
        return Files.createDirectory(parent.resolve(UUID.randomUUID().toString()));
    }
    @FunctionalInterface interface Checked { void run() throws Exception; }
    private void expect(String code, Checked action) throws Exception {
        try { action.run(); fail("Expected " + code); }
        catch (StoreException expected) { assertEquals(code, expected.code()); }
    }
    private Map<String, String> contents(Path root) throws IOException {
        Map<String, String> files = new TreeMap<>();
        try (var walk = Files.walk(root)) {
            for (Path path : walk.toList()) {
                if (path.getFileName().toString().equals(".owner.lock")) continue; // Windows denies reading the live owner lock.
                String relative = root.relativize(path).toString();
                files.put(relative, Files.isDirectory(path) ? "DIRECTORY" : StoreIO.hash(Files.readAllBytes(path)));
            }
        }
        return files;
    }
    private void refuseWithoutMutation(AssetStore store, String code, UUID healthy) throws Exception {
        var before = contents(store.root());
        expect(code, store::catalog);
        expect(code, store::rebuildIndex);
        assertEquals(before, contents(store.root()));
        var exported = new ByteArrayOutputStream();
        store.export(healthy, exported);
        assertArrayEquals(StoreProcess.PAYLOAD, exported.toByteArray());
        assertEquals(ReadResult.Status.VALID, store.read(healthy).status());
    }

    @Test public void R1_01_unknownNamesAndDirectoriesCannotBecomeACompleteCatalog() throws Exception {
        for (String name : List.of("orphan.bin", "new-format.v2", UUID.randomUUID().toString(),
                UUID.randomUUID() + ".REC", ".pending-not-a-uuid", "unknown-directory")) {
            try (var store = AssetStore.open(fixture().resolve("source"))) {
                var request = StoreProcess.request(UUID.randomUUID());
                store.put(request, new ByteArrayInputStream(StoreProcess.PAYLOAD));
                Path unknown = store.root().resolve("records").resolve(name);
                if (name.equals("unknown-directory")) {
                    Files.createDirectory(unknown);
                    Files.write(unknown.resolve("retained.bin"), new byte[]{42});
                } else Files.write(unknown, new byte[]{42});
                // If rebuilding started before the full scan, it would replace this sentinel.
                Files.write(store.root().resolve("indexes/" + request.id() + ".idx"), new byte[]{66});
                refuseWithoutMutation(store, "CORRUPT", request.id());
            }
        }
    }

    @Test public void R1_01_actualInterruptedRecordPublicationRemainsVisibleAfterRecoveryAndRestart() throws Exception {
        Path root = fixture().resolve("source");
        var interrupt = new AtomicBoolean();
        var healthy = StoreProcess.request(UUID.randomUUID());
        var pending = StoreProcess.request(UUID.randomUUID());
        Path staged;
        try (var store = AssetStore.open(root, new AssetStore.Faults() {
            @Override public void before(AssetStore.Io op, Path path) throws IOException {
                if (op == AssetStore.Io.MOVE && path.getParent().equals(root.resolve("records"))
                        && interrupt.compareAndSet(true, false)) throw new IOException("synthetic record publication interrupted");
            }
        })) {
            store.put(healthy, new ByteArrayInputStream(StoreProcess.PAYLOAD));
            interrupt.set(true);
            try { store.put(pending, new ByteArrayInputStream(new byte[]{9})); fail("Expected interruption"); }
            catch (IOException expected) { assertEquals("synthetic record publication interrupted", expected.getMessage()); }
            staged = StoreIO.entries(root.resolve("records"), ".pending-*").get(0);
            byte[] original = Files.readAllBytes(staged);
            refuseWithoutMutation(store, "RECOVERY_REQUIRED", healthy.id());
            assertTrue(store.recover().stream().anyMatch(r -> r.id().equals(pending.id()) && r.state().equals("COMPLETED")));
            assertEquals(ReadResult.Status.VALID, store.read(pending.id()).status());
            refuseWithoutMutation(store, "RECOVERY_REQUIRED", healthy.id());
            assertArrayEquals(original, Files.readAllBytes(staged));
        }
        try (var store = AssetStore.open(root)) {
            refuseWithoutMutation(store, "RECOVERY_REQUIRED", healthy.id());
            assertTrue(Files.exists(staged));
        }
    }

    @Test public void R1_01_damagedUnsupportedAndUnreadableEntriesRetainTheirClassification() throws Exception {
        for (boolean temporary : List.of(false, true)) for (String code : List.of("CORRUPT", "UNSUPPORTED_VERSION", "READ_ERROR")) {
            Path root = fixture().resolve("source");
            Path anomaly = root.resolve("records").resolve(temporary ? ".pending-" + UUID.randomUUID() : UUID.randomUUID() + ".rec");
            var deny = new AtomicBoolean();
            try (var store = AssetStore.open(root, new AssetStore.Faults() {
                @Override public void before(AssetStore.Io op, Path path) throws IOException {
                    if (deny.get() && op == AssetStore.Io.READ && path.equals(anomaly))
                        throw new AccessDeniedException(path.toString());
                }
            })) {
                var healthy = StoreProcess.request(UUID.randomUUID());
                var asset = store.put(healthy, new ByteArrayInputStream(StoreProcess.PAYLOAD)).asset();
                byte[] bytes = StoreIO.encode("asset", AssetStore.encodeAsset(asset));
                if (code.equals("CORRUPT")) bytes = new byte[]{0, 1};
                if (code.equals("UNSUPPORTED_VERSION")) bytes[7] = 2;
                Files.write(anomaly, bytes);
                deny.set(code.equals("READ_ERROR"));
                refuseWithoutMutation(store, code, healthy.id());
            }
        }
    }

    @Test public void R1_01_normalEmptyAndCommittedCatalogsStillRebuild() throws Exception {
        Path root = fixture().resolve("source");
        var request = StoreProcess.request(UUID.randomUUID());
        try (var store = AssetStore.open(root)) {
            assertTrue(store.catalog().isEmpty()); assertEquals(0, store.rebuildIndex());
            store.put(request, new ByteArrayInputStream(StoreProcess.PAYLOAD));
            assertEquals(1, store.catalog().size()); assertEquals(1, store.rebuildIndex());
        }
        try (var store = AssetStore.open(root)) { assertEquals(request.id(), store.catalog().get(0).request().id()); }
    }

    private static void await(CountDownLatch latch) throws IOException {
        try { if (!latch.await(20, TimeUnit.SECONDS)) throw new IOException("Synthetic barrier timed out"); }
        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IOException(interrupted); }
    }
    private void backupRacesClose(boolean interruptCopy) throws Exception {
        Path f = fixture(), root = f.resolve("source"), backup = f.resolve("backup"), restored = f.resolve("restored");
        var registered = new CountDownLatch(1); var permitCatalog = new CountDownLatch(1);
        var copied = new CountDownLatch(1); var permitCompletion = new CountDownLatch(1);
        var firstCopy = new AtomicBoolean(true);
        var executor = Executors.newFixedThreadPool(2);
        var store = AssetStore.open(root, new AssetStore.Faults() {
            @Override public void after(AssetStore.Point point, Path path) throws IOException {
                if (point == AssetStore.Point.BACKUP_REGISTERED) { registered.countDown(); await(permitCatalog); }
                if (point == AssetStore.Point.BACKUP_FILE && firstCopy.compareAndSet(true, false)) {
                    copied.countDown(); await(permitCompletion);
                    if (interruptCopy) throw new IOException("synthetic backup copy interrupted");
                }
            }
        });
        var request = StoreProcess.request(UUID.randomUUID());
        try {
            store.put(request, new ByteArrayInputStream(StoreProcess.PAYLOAD));
            var job = executor.submit(() -> AssetBackup.create(store, backup));
            assertTrue("Backup registered before close", registered.await(10, TimeUnit.SECONDS));
            assertFalse(store.close(Duration.ZERO)); // Deterministically enters draining while backup is registered.
            expect("IN_USE", () -> AssetStore.open(root));
            expect("CLOSING", store::catalog);
            permitCatalog.countDown();
            assertTrue("Admitted backup must pass its catalog while draining", copied.await(10, TimeUnit.SECONDS));
            assertFalse(Files.exists(backup.resolve("backup-manifest.rec")));
            assertFalse(store.close(Duration.ofMillis(25)));
            expect("IN_USE", () -> AssetStore.open(root));
            var closing = executor.submit(() -> store.close(Duration.ofSeconds(15)));
            permitCompletion.countDown();
            if (interruptCopy) {
                try { job.get(10, TimeUnit.SECONDS); fail("Incomplete backup must not report success"); }
                catch (ExecutionException expected) {
                    assertTrue(expected.getCause() instanceof IOException);
                    assertEquals("synthetic backup copy interrupted", expected.getCause().getMessage());
                }
                assertFalse(Files.exists(backup.resolve("backup-manifest.rec")));
                expect("MISSING", () -> AssetBackup.restore(backup, restored));
                assertFalse(Files.exists(restored));
            } else {
                assertEquals(1, job.get(10, TimeUnit.SECONDS).committedAssets());
                assertEquals(ReadResult.Status.VALID, new StoreIO(AssetStore.NONE).read(backup.resolve("backup-manifest.rec"), "backup-manifest").status());
                assertEquals(1, AssetBackup.restore(backup, restored).committedAssets());
                try (var restoredStore = AssetStore.open(restored)) {
                    var exported = new ByteArrayOutputStream(); restoredStore.export(request.id(), exported);
                    assertArrayEquals(StoreProcess.PAYLOAD, exported.toByteArray());
                }
            }
            assertTrue(closing.get(10, TimeUnit.SECONDS));
            try (var reopened = AssetStore.open(root)) { assertEquals(ReadResult.Status.VALID, reopened.read(request.id()).status()); }
        } finally {
            permitCatalog.countDown(); permitCompletion.countDown();
            executor.shutdown(); assertTrue(executor.awaitTermination(25, TimeUnit.SECONDS));
            store.close();
        }
    }
    @Test public void R1_02_registeredBackupCompletesDuringDrainWithVerifiedMarker() throws Exception { backupRacesClose(false); }
    @Test public void R1_02_interruptedRegisteredBackupNeverReportsSuccessAndRetainsLockUntilSettled() throws Exception { backupRacesClose(true); }
}
