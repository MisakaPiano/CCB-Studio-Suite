import io.github.misakapiano.ccbstudio.core.*;
import java.nio.file.*;
import java.util.*;
import java.security.*;
import kotlin.Unit;
public class FormatMigrationProbe {
  public static void main(String[] args)throws Exception{
    Path source=Path.of(args[0]).toAbsolutePath(),target=Path.of(args[1]).toAbsolutePath();
    StudioFormatMigration.INSTANCE.copyVersionTwo(source,target,s->Unit.INSTANCE);
    try(var w=StudioWorkspace.Companion.openStudio(target,s->Unit.INSTANCE)){
      if(w.getState().getVersion()!=3)throw new AssertionError("format");
      if(w.getState().getProjects().size()!=1||w.getState().getTypesetProjects().size()!=1)throw new AssertionError("projects");
      var library=new LibraryRepository(w);
      if(library.prompts().size()!=2||new PromptReferences(library).load().getIds().size()!=1)throw new AssertionError("prompts");
      if(new AssetArchive(w).ids(library.load()).size()!=1)throw new AssertionError("archive");
      String ref=w.getState().getAccounts().get(0).getSecretRef();
      String credentialHash=new SecretVault(target.resolve("secrets")).use(ref,bytes->{try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(Exception e){throw new RuntimeException(e);}});
      System.out.println("MIGRATED_FORMAT=3 assets="+w.catalog().size()+" credentialSha256="+credentialHash);
      for(var asset:w.catalog())w.bytes(asset.request().id().toString());
    }
  }
}
