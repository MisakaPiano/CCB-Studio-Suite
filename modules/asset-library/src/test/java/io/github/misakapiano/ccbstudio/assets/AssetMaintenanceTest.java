package io.github.misakapiano.ccbstudio.assets;

import org.junit.Test;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;

public class AssetMaintenanceTest {
    private Path fixture() throws IOException {
        Path root = Path.of(System.getProperty("asset.test.root")); Files.createDirectories(root);
        return Files.createDirectory(root.resolve(UUID.randomUUID().toString()));
    }
    @FunctionalInterface interface Checked { void run() throws Exception; }
    private void expect(String code, Checked action) throws Exception {
        try { action.run(); fail("Expected " + code); } catch (StoreException expected) { assertEquals(code, expected.code()); }
    }
    private AssetStore.Request interruptedRecord(Path root) throws Exception {
        var fail = new AtomicBoolean(true); var request = StoreProcess.request(UUID.randomUUID());
        try (var store = AssetStore.open(root, new AssetStore.Faults() {
            @Override public void before(AssetStore.Io op, Path path) throws IOException {
                if (op == AssetStore.Io.MOVE && path.getParent().equals(root.resolve("records")) && fail.compareAndSet(true,false))
                    throw new IOException("synthetic publish interruption");
            }
        })) {
            try { store.put(request, new ByteArrayInputStream(StoreProcess.PAYLOAD)); fail(); } catch (IOException expected) { }
        }
        return request;
    }
    @Test public void CP1_pendingInspectionRecoveryAndReconciliationPreserveEveryByteAndBackup() throws Exception {
        Path f=fixture(), root=f.resolve("source"); var r=interruptedRecord(root);
        try (var store=AssetStore.open(root)) {
            var entry=AssetMaintenance.inspect(store).get(0);
            assertEquals(AssetMaintenance.Category.PENDING,entry.category()); assertEquals(ReadResult.Status.VALID,entry.status());
            Path staging=root.resolve("records").resolve(entry.name()); byte[] before=Files.readAllBytes(staging);
            expect("MISSING",()->AssetMaintenance.reconcilePending(store,entry.name(),entry.size(),entry.sha256()));
            assertArrayEquals(before,Files.readAllBytes(staging));
            assertEquals("COMPLETED",store.recover().get(0).state());
            expect("RECOVERY_REQUIRED",store::catalog);
            var result=AssetMaintenance.reconcilePending(store,entry.name(),entry.size(),entry.sha256());
            assertEquals("PRESERVED_DUPLICATE",result.state()); assertEquals(1,store.catalog().size());
            assertEquals(1,store.rebuildIndex()); assertEquals(List.of(result),AssetMaintenance.recover(store));
            Path held=root.resolve("maintenance/reconciliations/"+result.id()+"/candidate.rec");
            assertArrayEquals(before,Files.readAllBytes(held));
            assertEquals(ReadResult.Status.VALID,store.read(r.id()).status());
            assertEquals(1,AssetBackup.create(store,f.resolve("backup")).committedAssets());
            assertArrayEquals(before,Files.readAllBytes(f.resolve("backup").resolve(root.relativize(held))));
        }
        assertEquals(1,AssetBackup.restore(f.resolve("backup"),f.resolve("restored")).committedAssets());
        try(var store=AssetStore.open(f.resolve("restored"))) {
            assertEquals(1,AssetMaintenance.recover(store).size());
            var bytes=new ByteArrayOutputStream();store.export(r.id(),bytes);assertArrayEquals(StoreProcess.PAYLOAD,bytes.toByteArray());
        }
    }
    @Test public void CP1_unknownCorruptFutureAndStaleCandidatesCannotBeDiscarded() throws Exception {
        Path root=fixture().resolve("source"); var r=interruptedRecord(root);
        try(var store=AssetStore.open(root)) {
            store.recover();var pending=AssetMaintenance.inspect(store).stream().filter(e->e.category()==AssetMaintenance.Category.PENDING).findFirst().orElseThrow();
            expect("CONFLICT",()->AssetMaintenance.reconcilePending(store,pending.name(),pending.size(),"0".repeat(64)));
            Path unknown=root.resolve("records/unknown.bin");Files.write(unknown,new byte[]{44});
            expect("REFUSED",()->AssetMaintenance.reconcilePending(store,"unknown.bin",1,StoreIO.hash(new byte[]{44})));
            expect("REFUSED",()->AssetMaintenance.reconcilePending(store,r.id()+".rec",1,"0".repeat(64)));
            assertArrayEquals(new byte[]{44},Files.readAllBytes(unknown));
            Path candidate=root.resolve("records").resolve(pending.name());byte[] original=Files.readAllBytes(candidate);
            byte[] future=original.clone();future[7]=2;Files.write(candidate,future);
            var futureEntry=AssetMaintenance.inspect(store).stream().filter(e->e.name().equals(pending.name())).findFirst().orElseThrow();
            assertEquals(ReadResult.Status.UNSUPPORTED_VERSION,futureEntry.status());
            expect("UNSUPPORTED_VERSION",()->AssetMaintenance.reconcilePending(store,pending.name(),future.length,StoreIO.hash(future)));
            Files.write(candidate,new byte[]{1});
            expect("CORRUPT",()->AssetMaintenance.reconcilePending(store,pending.name(),1,StoreIO.hash(new byte[]{1})));
            assertArrayEquals(new byte[]{1},Files.readAllBytes(candidate));
            expect("CORRUPT",store::catalog);assertEquals(ReadResult.Status.VALID,store.read(r.id()).status());
        }
    }
    @Test public void CP1_reconciliationSurvivesRealChildTerminationAtEveryBoundary() throws Exception {
        for(var point:List.of(AssetStore.Point.MAINTENANCE_INTENT,AssetStore.Point.MAINTENANCE_MOVED,AssetStore.Point.MAINTENANCE_COMPLETE)) {
            Path root=fixture().resolve("source");var r=interruptedRecord(root);
            try(var store=AssetStore.open(root)){store.recover();}
            var child=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin/java.exe").toString(),"-Xmx64m","-XX:ActiveProcessorCount=1",
                    "-cp",System.getProperty("asset.child.classpath"),MaintenanceProcess.class.getName(),root.toString(),point.name())
                    .redirectErrorStream(true).redirectOutput(root.getParent().resolve("maintenance-child.log").toFile()).start();
            assertTrue(child.waitFor(30,TimeUnit.SECONDS));assertEquals(71,child.exitValue());
            try(var store=AssetStore.open(root)) {
                var once=AssetMaintenance.recover(store);assertEquals(once,AssetMaintenance.recover(store));
                assertEquals(1,once.size());assertEquals(1,store.catalog().size());
                assertEquals(ReadResult.Status.VALID,store.read(r.id()).status());
                assertTrue(Files.exists(root.resolve("maintenance/reconciliations/"+once.get(0).id()+"/candidate.rec")));
            }
            System.out.println("RECONCILIATION_CRASH "+point+" verified");
        }
    }
    @Test public void CP1_futureOrAlteredMaintenanceIntentIsNeverReplacedByDefaults() throws Exception {
        Path root=fixture().resolve("source");var r=interruptedRecord(root);
        try(var store=AssetStore.open(root)) {
            store.recover();var entry=AssetMaintenance.inspect(store).stream().filter(e->e.category()==AssetMaintenance.Category.PENDING).findFirst().orElseThrow();
            var done=AssetMaintenance.reconcilePending(store,entry.name(),entry.size(),entry.sha256());
            Path intent=root.resolve("maintenance/reconciliations/"+done.id()+"/intent.rec");
            byte[] original=Files.readAllBytes(intent), future=original.clone();future[7]=2;Files.write(intent,future);
            expect("UNSUPPORTED_VERSION",()->AssetMaintenance.recover(store));assertArrayEquals(future,Files.readAllBytes(intent));
            Files.write(intent,new byte[]{1,2});expect("CORRUPT",()->AssetMaintenance.recover(store));
            assertArrayEquals(new byte[]{1,2},Files.readAllBytes(intent));assertEquals(ReadResult.Status.VALID,store.read(r.id()).status());
        }
    }
    public static final class MaintenanceProcess {
        public static void main(String[] args) throws Exception {
            try(var store=AssetStore.open(Path.of(args[0]),new AssetStore.Faults(){
                @Override public void after(AssetStore.Point p,Path path){if(p==AssetStore.Point.valueOf(args[1]))Runtime.getRuntime().halt(71);}
            })) {
                var e=AssetMaintenance.inspect(store).stream().filter(x->x.category()==AssetMaintenance.Category.PENDING).findFirst().orElseThrow();
                AssetMaintenance.reconcilePending(store,e.name(),e.size(),e.sha256());
            }
            throw new AssertionError("Crash point not reached");
        }
    }
}
