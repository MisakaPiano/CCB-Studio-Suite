package io.github.misakapiano.ccbstudio.assets;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;

/** Narrow, explicitly confirmed editor-content retirement. No generic GC and no media
 * deletion. Metadata tombstones retain identity, never the discarded editor payload. */
public final class EditorSnapshotRetirement {
    private EditorSnapshotRetirement() {}
    public interface DeleteVerified { void delete(Path path, long size, String sha256) throws IOException; }
    public record Result(UUID id, boolean contentDeleted, String detail) {}
    private static Path journal(AssetStore store, UUID id) { return store.root.resolve("maintenance/editor-retirements").resolve(id.toString()); }
    static boolean retired(AssetStore store, UUID id) throws IOException {
        Path intent = journal(store,id).resolve("intent.rec");
        if (!Files.exists(intent,NOFOLLOW_LINKS)) return false;
        var asset=AssetStore.decodeAsset(store.io.read(intent,"editor-retire").require());
        validate(asset); if(!asset.request().id().equals(id))throw new StoreException("CORRUPT","Editor retirement identity differs");
        return true;
    }
    private static void validate(AssetStore.Asset asset) throws IOException {
        var r=asset.request();String tag=new String(r.project(),StandardCharsets.UTF_8);
        if(r.kind()!=AssetStore.Kind.ORIGINAL || r.parentId()!=null || r.recipe().length!=0 ||
           !(tag.equals("CCB_EDITOR_DOCUMENT_V1") || (tag.equals("CCB_TYPESET_SOURCE_EXCHANGE_V1") && r.name().equals("source.typesetter-workspace.json"))))
            throw new StoreException("CONFLICT","Only proven editor snapshots/source exchanges may be retired; media and presets retained");
    }
    public static AssetStore.Asset identity(AssetStore store,UUID id) throws IOException {
        return store.operation(()->{
            var live=store.readInternal(id);if(live.status()==ReadResult.Status.VALID)return live.require();
            if(!retired(store,id))return live.require();
            return AssetStore.decodeAsset(store.io.read(journal(store,id).resolve("intent.rec"),"editor-retire").require());
        });
    }
    public static List<Result> retire(AssetStore store,List<AssetStore.Asset> expected,DeleteVerified delete) throws IOException {
        return store.operation(()->{
            if(expected.size()>50000 || expected.stream().map(a->a.request().id()).distinct().count()!=expected.size())throw new IllegalArgumentException("Invalid editor retirement set");
            var ids=new HashSet<UUID>();for(var a:expected){validate(a);ids.add(a.request().id());}
            var catalog=store.catalogInternal();
            for(var a:expected){
                if(catalog.stream().anyMatch(other->!ids.contains(other.request().id()) && a.request().id().equals(other.request().parentId())))throw new StoreException("CONFLICT","Editor resource is still a parent; retained");
                var live=store.readInternal(a.request().id());
                if(live.status()==ReadResult.Status.VALID && !AssetStore.sameAsset(live.require(),a))throw new StoreException("CONFLICT","Editor asset changed; retained");
                if(live.status()!=ReadResult.Status.VALID && !retired(store,a.request().id()))live.require();
            }
            List<Result> results=new ArrayList<>();
            for(var a:expected){
                UUID id=a.request().id();Path dir=journal(store,id);StoreIO.directory(dir);
                Path intent=dir.resolve("intent.rec");
                if(!Files.exists(intent,NOFOLLOW_LINKS))store.io.immutable(intent,"editor-retire",AssetStore.encodeAsset(a));
                var admitted=AssetStore.decodeAsset(store.io.read(intent,"editor-retire").require());
                if(!AssetStore.sameAsset(a,admitted))throw new StoreException("CONFLICT","Different retirement intent retained");
                Path record=store.recordPath(id),held=dir.resolve("relationship.rec");
                if(Files.exists(record,NOFOLLOW_LINKS)){
                    if(Files.exists(held,NOFOLLOW_LINKS))throw new StoreException("CONFLICT","Both retirement record locations retained");
                    store.io.publish(record,held,false);
                }
                if(!AssetStore.sameAsset(store.decodeAssetFile(held,"asset").require(),a))throw new StoreException("CORRUPT","Retired relationship differs; bytes retained");
                boolean shared=catalog.stream().anyMatch(other->!ids.contains(other.request().id())&&other.sha256().equals(a.sha256()));
                if(!shared){Path object=store.objectPath(a);if(Files.exists(object,NOFOLLOW_LINKS))delete.delete(object,a.size(),a.sha256());}
                Path staged=store.root.resolve("transactions").resolve(id.toString()).resolve("payload.part");
                if(Files.exists(staged,NOFOLLOW_LINKS))delete.delete(staged,a.size(),a.sha256());
                Path index=store.root.resolve("indexes").resolve(id+".idx");
                if(Files.exists(index,NOFOLLOW_LINKS)){
                    if(!AssetStore.sameAsset(store.decodeAssetFile(index,"index").require(),a))throw new StoreException("CONFLICT","Changed editor index retained");
                    var identity=store.io.fingerprint(index);delete.delete(index,identity.size(),identity.sha256());
                }
                Path done=dir.resolve("complete.rec");if(!Files.exists(done,NOFOLLOW_LINKS))store.io.immutable(done,"editor-retired",(shared?"SHARED_BYTES_RETAINED":"EXCLUSIVE_CONTENT_REMOVED").getBytes(StandardCharsets.UTF_8));
                else store.io.read(done,"editor-retired").require();
                results.add(new Result(id,!shared,shared?"Shared content retained":"Exclusive editor content removed"));
            }
            return List.copyOf(results);
        });
    }
}
