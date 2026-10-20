package io.github.misakapiano.ccbstudio.assets;

import org.junit.Test;
import java.io.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.Assert.*;
import static io.github.misakapiano.ccbstudio.assets.ReadResult.Status.*;

public class AssetStoreTest {
    private Path fixture() throws IOException {
        Path parent = Path.of(System.getProperty("asset.test.root")).toAbsolutePath();
        Files.createDirectories(parent);
        return Files.createDirectory(parent.resolve(UUID.randomUUID().toString()));
    }
    private AssetStore.Request request() { return StoreProcess.request(UUID.randomUUID()); }
    private AssetStore.Receipt put(AssetStore s, AssetStore.Request r, byte[] b) throws IOException {
        return s.put(r, new ByteArrayInputStream(b));
    }
    private void expect(String code, Throwing action) throws Exception {
        try { action.run(); fail("Expected " + code); }
        catch (StoreException e) { assertEquals(code, e.code()); }
    }
    @FunctionalInterface interface Throwing { void run() throws Exception; }
    private Process child(String mode, Path root, String... extra) throws IOException {
        var args = new ArrayList<>(List.of(Path.of(System.getProperty("java.home"), "bin", "java.exe").toString(),
                "-Xmx64m", "-XX:ActiveProcessorCount=1", "-cp", System.getProperty("asset.child.classpath"),
                StoreProcess.class.getName(), mode, root.toString()));
        args.addAll(List.of(extra));
        return new ProcessBuilder(args).redirectErrorStream(true).redirectOutput(root.getParent().resolve("child-" + UUID.randomUUID() + ".log").toFile()).start();
    }
    private void exit(Process process, int expected) throws Exception {
        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            process.destroyForcibly(); fail("Only the owned synthetic child exceeded 30 seconds");
        }
        assertEquals(expected, process.exitValue());
    }

    @Test public void D01_rawBytesOpaqueDocumentsAndDerivedAssetsSurviveRestart() throws Exception {
        Path root = fixture().resolve("store");
        var original = request();
        var synthetic = new java.awt.image.BufferedImage(2, 2, java.awt.image.BufferedImage.TYPE_INT_RGB);
        synthetic.setRGB(0, 0, 0x123456);
        var encoded = new ByteArrayOutputStream();
        assertTrue(javax.imageio.ImageIO.write(synthetic, "jpeg", encoded));
        byte[] jpeg = encoded.toByteArray();
        assertNotNull(javax.imageio.ImageIO.read(new ByteArrayInputStream(jpeg)));
        assertNotNull(javax.imageio.ImageIO.read(new ByteArrayInputStream(StoreProcess.PAYLOAD)));
        UUID derivedId = UUID.randomUUID();
        var derived = new AssetStore.Request(derivedId, "same-name.jpg", AssetStore.Kind.DERIVED, original.id(), new byte[]{9}, new byte[]{8});
        try (var s = AssetStore.open(root)) {
            assertTrue(put(s, original, jpeg).indexReady());
            put(s, derived, new byte[]{99});
            byte[] copy = original.recipe(); copy[0] = 0;
            assertNotEquals(0, original.recipe()[0]);
        }
        for (int i = 0; i < 3; i++) try (var s = AssetStore.open(root)) {
            var read = s.read(original.id()).require();
            assertEquals(jpeg.length, read.size()); assertEquals(StoreIO.hash(jpeg), read.sha256());
            assertArrayEquals(original.recipe(), read.request().recipe());
            assertArrayEquals(original.project(), read.request().project());
            var output = new ByteArrayOutputStream(); s.export(original.id(), output); assertArrayEquals(jpeg, output.toByteArray());
            assertEquals(original.id(), s.read(derivedId).require().request().parentId());
            assertEquals(2, s.catalog().size());
        }
    }
    @Test public void D01_independentJvmReadsExactSyntheticPng() throws Exception {
        Path root = fixture().resolve("store"); var request = request();
        try (var s = AssetStore.open(root)) { put(s, request, StoreProcess.PAYLOAD); }
        exit(child("verify", root, request.id().toString()), 0);
    }
    @Test public void D02_replayDedupNameCollisionAndConflictingId() throws Exception {
        try (var s = AssetStore.open(fixture().resolve("store"))) {
            var a = request(); var b = request(); var c = request();
            put(s, a, new byte[]{1}); put(s, a, new byte[]{1});
            put(s, b, new byte[]{1}); put(s, c, new byte[]{2});
            assertEquals(3, s.catalog().size());
            assertEquals(2, StoreIO.entries(s.root().resolve("objects"), "*.blob").size());
            expect("CONFLICT", () -> put(s, a, new byte[]{2}));
            var altered = new AssetStore.Request(a.id(), a.name(), a.kind(), null, new byte[]{7}, a.project());
            expect("CONFLICT", () -> put(s, altered, new byte[]{1}));
            assertEquals(StoreIO.hash(new byte[]{1}), s.read(a.id()).require().sha256());
        }
    }
    @Test public void D02_concurrentSubmissionsPreserveEachAssociation() throws Exception {
        try (var s = AssetStore.open(fixture().resolve("store"))) {
            var executor = Executors.newFixedThreadPool(2);
            try {
                var requests = List.of(request(), request()); List<Future<?>> jobs = new ArrayList<>();
                for (var r : requests) jobs.add(executor.submit(() -> { try { put(s, r, new byte[]{3}); } catch (IOException e) { throw new UncheckedIOException(e); } }));
                for (var job : jobs) job.get(10, TimeUnit.SECONDS);
                assertEquals(2, s.catalog().size());
            } finally { executor.shutdownNow(); }
        }
    }
    @Test public void D03_actualProcessTerminationAtEveryCommitBoundary() throws Exception {
        for (var point : List.of(AssetStore.Point.INTENT, AssetStore.Point.BYTES, AssetStore.Point.SEALED,
                AssetStore.Point.ASSET, AssetStore.Point.RECORD, AssetStore.Point.INDEX, AssetStore.Point.COMPLETE)) {
            Path root = fixture().resolve("store"); var r = request();
            exit(child("crash", root, r.id().toString(), point.name()), 71);
            try (var s = AssetStore.open(root)) {
                var first = s.recover(); var second = s.recover();
                if (point == AssetStore.Point.INTENT || point == AssetStore.Point.BYTES) {
                    assertEquals("INCOMPLETE", first.get(0).state()); assertEquals(first, second);
                    assertEquals(MISSING, s.read(r.id()).status());
                    expect("RECOVERY_REQUIRED", () -> put(s, r, StoreProcess.PAYLOAD));
                    if (point == AssetStore.Point.BYTES)
                        assertArrayEquals(StoreProcess.PAYLOAD, Files.readAllBytes(root.resolve("transactions/" + r.id() + "/payload.part")));
                } else {
                    assertEquals("COMPLETED", first.get(0).state()); assertEquals(first, second);
                    assertEquals(VALID, s.read(r.id()).status());
                    put(s, r, StoreProcess.PAYLOAD);
                    assertEquals(1, s.catalog().size());
                }
            }
            System.out.println("PROCESS_CRASH " + point + " verified");
        }
    }
    @Test public void D03_indexWriteFailureReturnsCommittedReceiptAndRecovers() throws Exception {
        Path root = fixture().resolve("store"); var r = request();
        try (var s = AssetStore.open(root, new AssetStore.Faults() {
            @Override public void before(AssetStore.Io op, Path p) throws IOException {
                if (op == AssetStore.Io.MOVE && p.toString().endsWith(".idx")) throw new AccessDeniedException(p.toString());
            }
        })) {
            assertFalse(put(s, r, new byte[]{4}).indexReady());
            assertEquals(VALID, s.read(r.id()).status()); assertEquals(MISSING, s.readIndex(r.id()).status());
        }
        try (var s = AssetStore.open(root)) {
            assertEquals("COMPLETED", s.recover().get(0).state());
            assertEquals(VALID, s.readIndex(r.id()).status());
        }
    }
    @Test public void D04_missingCorruptReadErrorAndUnknownVersionAreDistinct() throws Exception {
        Path root = fixture().resolve("store"); var r = request();
        try (var s = AssetStore.open(root)) {
            assertEquals(MISSING, s.read(r.id()).status()); put(s, r, new byte[]{1, 2});
        }
        Path record = root.resolve("records/" + r.id() + ".rec"); byte[] original = Files.readAllBytes(record);
        try (var s = AssetStore.open(root, new AssetStore.Faults() {
            @Override public void before(AssetStore.Io op, Path path) throws IOException {
                if (op == AssetStore.Io.READ && path.equals(record)) throw new AccessDeniedException(path.toString());
            }
        })) { assertEquals(READ_ERROR, s.read(r.id()).status()); }
        assertArrayEquals(original, Files.readAllBytes(record));
        byte[] future = original.clone(); future[7] = 2; Files.write(record, future);
        try (var s = AssetStore.open(root)) {
            assertEquals(UNSUPPORTED_VERSION, s.read(r.id()).status());
            assertEquals("UNSUPPORTED_VERSION", s.recover().get(0).state());
            expect("UNSUPPORTED_VERSION", () -> put(s, r, new byte[]{1, 2}));
        }
        assertArrayEquals(future, Files.readAllBytes(record));
        Files.write(record, new byte[]{0, 1});
        try (var s = AssetStore.open(root)) {
            assertEquals(CORRUPT, s.read(r.id()).status());
            expect("CORRUPT", s::rebuildIndex);
        }
        assertArrayEquals(new byte[]{0, 1}, Files.readAllBytes(record));
    }
    @Test public void D04_truncatedInputAndInjectedDiskFullRetainPendingBytes() throws Exception {
        Path root = fixture().resolve("store"); var r = request();
        try (var s = AssetStore.open(root)) {
            InputStream failed = new InputStream() {
                int reads;
                @Override public int read() throws IOException { throw new IOException("synthetic disk/network read failure"); }
                @Override public int read(byte[] b, int off, int len) throws IOException {
                    if (reads++ == 0) { b[off] = 7; return 1; }
                    throw new IOException("synthetic interrupted source");
                }
            };
            try { s.put(r, failed); fail(); } catch (IOException expected) { /* received partial bytes remain */ }
            assertEquals("INCOMPLETE", s.recover().get(0).state());
            assertArrayEquals(new byte[]{7}, Files.readAllBytes(root.resolve("transactions/" + r.id() + "/payload.part")));
        }
        Path fullRoot = fixture().resolve("store");
        try (var s = AssetStore.open(fullRoot, new AssetStore.Faults() {
            @Override public void before(AssetStore.Io op, Path path) throws IOException {
                if (op == AssetStore.Io.WRITE && path.endsWith("payload.part")) throw new FileSystemException(path.toString(), null, "injected disk full");
            }
        })) {
            try { put(s, request(), new byte[]{1}); fail(); } catch (FileSystemException expected) { }
            assertEquals("INCOMPLETE", s.recover().get(0).state());
        }
    }
    @Test public void D04_corruptIndexRebuildPreservesEvidenceAndUniqueOriginal() throws Exception {
        Path root = fixture().resolve("store"); var r = request();
        try (var s = AssetStore.open(root)) {
            var asset = put(s, r, new byte[]{9}).asset();
            Path index = root.resolve("indexes/" + r.id() + ".idx"); Files.write(index, new byte[]{55});
            assertEquals(CORRUPT, s.readIndex(r.id()).status()); assertEquals(VALID, s.read(r.id()).status());
            assertEquals(1, s.rebuildIndex()); assertEquals(VALID, s.readIndex(r.id()).status());
            assertArrayEquals(new byte[]{55}, Files.readAllBytes(StoreIO.entries(root.resolve("indexes/observed"), "*").get(0)));
            Files.delete(index); assertEquals(MISSING, s.readIndex(r.id()).status()); s.rebuildIndex();
            assertArrayEquals(new byte[]{9}, Files.readAllBytes(s.objectPath(asset)));
            Files.write(s.objectPath(asset), new byte[]{8});
            assertEquals(CORRUPT, s.read(r.id()).status());
            expect("CORRUPT", () -> put(s, r, new byte[]{9}));
            assertArrayEquals(new byte[]{8}, Files.readAllBytes(s.objectPath(asset)));
        }
    }
    @Test public void D05_foreignRootsTraversalAndRedirectedPathsAreRefused() throws Exception {
        Path parent = fixture(), foreign = parent.resolve("ChatChatBar-Desktop/data");
        expect("FOREIGN_ROOT", () -> AssetStore.open(foreign)); assertFalse(Files.exists(foreign));
        Path occupied = Files.createDirectory(parent.resolve("occupied")); Files.writeString(occupied.resolve("unrelated.txt"), "retain");
        expect("FOREIGN_ROOT", () -> AssetStore.open(occupied));
        assertEquals("retain", Files.readString(occupied.resolve("unrelated.txt")));
        try (var s = AssetStore.open(parent.resolve("suite"))) {
            expect("IN_USE", () -> AssetStore.open(s.root()));
            // Names remain opaque strings, never filesystem destinations.
            var r = new AssetStore.Request(UUID.randomUUID(), "../../outside.png", AssetStore.Kind.ORIGINAL, null, new byte[0], new byte[0]);
            put(s, r, new byte[]{1}); assertFalse(Files.exists(parent.resolve("outside.png")));
        }
    }
    @Test public void D05_otherProcessCannotAcquireThenCanAcquireAfterExit() throws Exception {
        Path root = fixture().resolve("store");
        Process owner = child("owner", root);
        try {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
            while (!Files.exists(root.resolve("test-owner-ready")) && owner.isAlive() && System.nanoTime() < deadline) Thread.sleep(25);
            assertTrue("Child acquired root", Files.exists(root.resolve("test-owner-ready")));
            expect("IN_USE", () -> AssetStore.open(root));
        } finally { owner.getOutputStream().close(); exit(owner, 0); }
        try (var ignored = AssetStore.open(root)) { assertTrue(Files.exists(root.resolve(".owner.lock"))); }
    }
    @Test public void D05_actualWindowsJunctionIsRefusedWithoutWritingThroughIt() throws Exception {
        Path f = fixture(), actual = Files.createDirectory(f.resolve("actual")), junction = f.resolve("junction");
        Process p = new ProcessBuilder("cmd.exe", "/c", "mklink", "/J", junction.toString(), actual.toString())
                .redirectErrorStream(true).redirectOutput(f.resolve("junction-command.log").toFile()).start();
        exit(p, 0);
        expect("UNSAFE_PATH", () -> AssetStore.open(junction.resolve("store")));
        assertFalse(Files.exists(actual.resolve("store")));
    }
    @Test public void D04_missingReferencedOriginalAndMalformedIntentAreCorruptNotEmptyDefaults() throws Exception {
        Path root = fixture().resolve("store"); var r = request();
        try (var s = AssetStore.open(root)) {
            var a = put(s, r, new byte[]{1}).asset();
            Files.delete(s.objectPath(a));
            assertEquals(CORRUPT, s.read(r.id()).status());
            assertEquals("CORRUPT", s.recover().get(0).state());
            expect("CORRUPT", s::rebuildIndex);
            assertFalse(Files.exists(s.objectPath(a)));
        }
        Path other = fixture().resolve("store"); UUID id = UUID.randomUUID();
        try (var s = AssetStore.open(other)) {
            s.io.immutable(other.resolve("transactions/" + id + "/request.rec"), "request", new byte[]{0, 0});
            assertEquals("CORRUPT", s.recover().get(0).state());
        }
    }
    @Test public void D05_drainTimeoutRetainsLockAndRefusesNewWrites() throws Exception {
        Path root = fixture().resolve("store");
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
        var executor = Executors.newSingleThreadExecutor();
        var s = AssetStore.open(root);
        try {
            var job = executor.submit(() -> {
                try { s.put(request(), new ByteArrayInputStream(new byte[]{1}) {
                    @Override public synchronized int read(byte[] b, int off, int len) {
                        entered.countDown(); try { release.await(); } catch (InterruptedException e) { throw new RuntimeException(e); }
                        return super.read(b, off, len);
                    }
                }); } catch (IOException e) { throw new UncheckedIOException(e); }
            });
            assertTrue(entered.await(10, TimeUnit.SECONDS));
            assertFalse(s.close(Duration.ofMillis(25)));
            expect("IN_USE", () -> AssetStore.open(root));
            expect("CLOSING", () -> put(s, request(), new byte[]{1}));
            release.countDown(); job.get(10, TimeUnit.SECONDS);
            assertTrue(s.close(Duration.ofSeconds(1)));
            try (var reopened = AssetStore.open(root)) { assertEquals(1, reopened.catalog().size()); }
        } finally { release.countDown(); s.close(); executor.shutdownNow(); }
    }
    @Test public void D04_noncanonicalRecordNameCannotMasqueradeAsAnotherRecord() throws Exception {
        try (var s = AssetStore.open(fixture().resolve("store"))) {
            var r = request(); put(s, r, new byte[]{1});
            Path alias = s.root().resolve("records/" + r.id() + ".rec.rec");
            Files.write(alias, new byte[]{42});
            expect("CORRUPT", s::catalog);
            expect("CORRUPT", s::rebuildIndex);
            assertArrayEquals(new byte[]{42}, Files.readAllBytes(alias));
            assertEquals(VALID, s.read(r.id()).status());
        }
    }
    @Test public void D06_scaleOneHundredAndThousandAndStreamingAreBounded() throws Exception {
        long start = System.nanoTime();
        for (int count : List.of(1, 100, 1000)) {
            Path root = fixture().resolve("store");
            try (var s = AssetStore.open(root)) {
                for (int i = 0; i < count; i++) put(s, request(), new byte[]{(byte)i, (byte)(i >>> 8)});
                assertEquals(count, s.catalog().size()); assertEquals(count, s.rebuildIndex());
            }
            try (var s = AssetStore.open(root)) {
                assertEquals(count, s.recover().size()); assertEquals(count, s.rebuildIndex());
                assertEquals(count, s.catalog().size());
            }
            System.out.println("SCALE " + count + " verified");
        }
        try (var s = AssetStore.open(fixture().resolve("store"))) {
            var maxRead = new AtomicInteger(); final long length = 8L * 1024 * 1024;
            InputStream generated = new InputStream() {
                long remaining = length;
                @Override public int read() { throw new AssertionError("Expected buffered IO"); }
                @Override public int read(byte[] b, int offset, int len) {
                    maxRead.accumulateAndGet(len, Math::max); if (remaining == 0) return -1;
                    int n = (int)Math.min(len, remaining); Arrays.fill(b, offset, offset+n, (byte)7); remaining-=n; return n;
                }
            };
            var asset = s.put(request(), generated).asset();
            assertEquals(length, asset.size()); assertTrue(maxRead.get() <= 64 * 1024);
            assertEquals(VALID, s.read(asset.request().id()).status());
        }
        System.out.println("SCALE_AND_STREAM elapsedMs=" + (System.nanoTime()-start)/1_000_000);
    }
}
