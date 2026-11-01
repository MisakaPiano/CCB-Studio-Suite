package io.github.misakapiano.ccbstudio.assets;

import org.junit.Test;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.Assert.*;

public class SingleOriginalStorageTest {
    private Path root() throws IOException {
        return Files.createDirectories(Path.of(System.getProperty("asset.test.root"),UUID.randomUUID().toString()));
    }
    private AssetStore.Request request() {
        return new AssetStore.Request(UUID.randomUUID(),"synthetic.bin",AssetStore.Kind.ORIGINAL,null,new byte[]{3},new byte[0]);
    }
    private AssetStore.Receipt put(AssetStore s,AssetStore.Request r) throws IOException {
        return s.put(r,new ByteArrayInputStream(new byte[]{7,8,9}));
    }
    private AssetStore.Faults retain = new AssetStore.Faults() {
        public void after(AssetStore.Point point,Path path) throws IOException {
            if(point==AssetStore.Point.COMPLETE)throw new IOException("Synthetic interrupted postcommit maintenance");
        }
    };
    @Test public void sharedBytesKeepDistinctAuthorityAndNewCommitsDoNotRetainSecondPayload() throws Exception {
        Path root=root();var a=request();var b=request();
        try(var s=AssetStore.open(root)) {
            put(s,a);put(s,b);
            assertEquals(2,s.catalog().size());assertEquals(1,StoreIO.entries(root.resolve("objects"),"*.blob").size());
            assertFalse(Files.exists(root.resolve("transactions/"+b.id()+"/payload.part")));
            assertTrue(Files.exists(root.resolve("transactions/"+b.id()+"/complete.rec")));
            assertNotEquals(a.id(),b.id());
        }
        try(var s=AssetStore.open(root)) {assertEquals(2,s.catalog().size());assertEquals(2,s.recover().size());}
    }
    @Test public void historicalStagingRequiresExplicitProofAndCleanupIsIdempotent() throws Exception {
        Path root=root();var a=request();var b=request();
        try(var s=AssetStore.open(root)){put(s,a);}
        try(var s=AssetStore.open(root,retain)){put(s,b);}
        Path staged=root.resolve("transactions/"+b.id()+"/payload.part");assertTrue(Files.exists(staged));
        try(var s=AssetStore.open(root)) {
            s.recover();assertTrue(Files.exists(staged));var original=s.read(b.id()).require();
            try{s.discardVerifiedStaging(b.id(),original.size()+1,original.sha256());fail();}catch(StoreException expected){assertEquals("CONFLICT",expected.code());}
            assertTrue(Files.exists(staged));assertEquals(3,s.discardVerifiedStaging(b.id(),3,original.sha256()));
            assertEquals(0,s.discardVerifiedStaging(b.id(),3,original.sha256()));assertEquals(2,s.catalog().size());
            assertArrayEquals(new byte[]{7,8,9},Files.readAllBytes(root.resolve("objects/"+original.sha256()+".blob")));
        }
    }
    @Test public void futureMissingModifiedAndAdmissionChangesRetainAllUnprovenBytes() throws Exception {
        Path root=root();var a=request();var b=request();
        try(var s=AssetStore.open(root)){put(s,a);}
        try(var s=AssetStore.open(root,retain)){put(s,b);}
        Path tx=root.resolve("transactions/"+b.id());Path staged=tx.resolve("payload.part"),complete=tx.resolve("complete.rec");
        byte[] proof=Files.readAllBytes(complete);String sha=StoreIO.hash(new byte[]{7,8,9});
        byte[] future=proof.clone();future[7]=99;Files.write(complete,future);
        try(var s=AssetStore.open(root)) {
            try{s.discardVerifiedStaging(b.id(),3,sha);fail();}catch(IOException expected){}
            assertArrayEquals(future,Files.readAllBytes(complete));assertTrue(Files.exists(staged));
            Files.delete(complete);
            try{s.discardVerifiedStaging(b.id(),3,sha);fail();}catch(IOException expected){}
            assertTrue(Files.exists(staged));Files.write(complete,proof);
            Files.write(staged,new byte[]{4,4,4});
            try{s.discardVerifiedStaging(b.id(),3,sha);fail();}catch(IOException expected){}
            assertArrayEquals(new byte[]{4,4,4},Files.readAllBytes(staged));Files.write(staged,new byte[]{7,8,9});
        }
        try(var s=AssetStore.open(root,new AssetStore.Faults(){public void before(AssetStore.Io io,Path path)throws IOException {
            if(io==AssetStore.Io.DELETE)Files.write(path,new byte[]{5,5,5});
        }})) {
            try{s.discardVerifiedStaging(b.id(),3,sha);fail();}catch(IOException expected){}
            assertArrayEquals(new byte[]{5,5,5},Files.readAllBytes(staged));assertEquals(2,s.catalog().size());
        }
    }
}
