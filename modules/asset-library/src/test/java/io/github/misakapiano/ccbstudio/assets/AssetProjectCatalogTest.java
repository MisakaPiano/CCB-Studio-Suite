package io.github.misakapiano.ccbstudio.assets;
import org.junit.Test;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

public class AssetProjectCatalogTest {
    @Test public void projectCatalogVerifiesSelectedPayloadsAndAllRecordEnvelopesWithoutClaimingFullIntegrity() throws Exception {
        Path root=Path.of(System.getProperty("asset.test.root"),UUID.randomUUID().toString());
        var blobReads=new HashMap<Path,Integer>();
        var faults=new AssetStore.Faults(){@Override public void before(AssetStore.Io io,Path path){if(io==AssetStore.Io.READ&&path.toString().endsWith(".blob"))blobReads.merge(path,1,Integer::sum);}};
        try(var store=AssetStore.open(root,faults)) {
            var image=store.put(new AssetStore.Request(UUID.randomUUID(),"image.png",AssetStore.Kind.ORIGINAL,null,new byte[0],new byte[0]),new ByteArrayInputStream(new byte[]{1,2,3})).asset();
            var metadata=store.put(new AssetStore.Request(UUID.randomUUID(),"metadata.json",AssetStore.Kind.ORIGINAL,null,new byte[0],"test-metadata".getBytes()),new ByteArrayInputStream(new byte[]{4,5,6})).asset();
            blobReads.clear();assertEquals(List.of(metadata.request().id()),store.catalogProject("test-metadata".getBytes()).stream().map(a->a.request().id()).toList());
            assertFalse(blobReads.containsKey(store.objectPath(image)));assertTrue(blobReads.containsKey(store.objectPath(metadata)));
            Files.write(store.objectPath(image),new byte[]{9,9,9});
            assertEquals(1,store.catalogProject("test-metadata".getBytes()).size());
            try{store.catalog();fail("Full integrity scan must still reject corrupt unrelated originals");}catch(StoreException expected){assertEquals("CORRUPT",expected.code());}
            Files.write(store.objectPath(metadata),new byte[]{8,8,8});
            try{store.catalogProject("test-metadata".getBytes());fail("Selected corrupt payload must be refused");}catch(StoreException expected){assertEquals("CORRUPT",expected.code());}
            assertArrayEquals(new byte[]{9,9,9},Files.readAllBytes(store.objectPath(image)));
        }
    }
    @Test public void unknownRecordIsNotHiddenByProjectFiltering() throws Exception {
        Path root=Path.of(System.getProperty("asset.test.root"),UUID.randomUUID().toString());
        try(var store=AssetStore.open(root)) {
            Files.writeString(root.resolve("records/unknown.entry"),"synthetic unknown");
            try{store.catalogProject("not-present".getBytes());fail("Unknown records require recovery");}catch(StoreException expected){assertEquals("CORRUPT",expected.code());}
            assertTrue(Files.exists(root.resolve("records/unknown.entry")));
        }
    }
}
