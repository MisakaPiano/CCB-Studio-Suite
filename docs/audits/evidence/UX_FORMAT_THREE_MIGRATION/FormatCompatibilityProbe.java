import io.github.misakapiano.ccbstudio.core.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import kotlin.Unit;

/** Run against the frozen A distribution, not current build classes. Synthetic only. */
public class FormatCompatibilityProbe {
  static String hash(byte[] bytes)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
  public static void main(String[] args)throws Exception{
    Path root=Path.of(args[1]).toAbsolutePath();
    if(args[0].equals("reject")){
      try(var w=StudioWorkspace.Companion.openStudio(root,s->Unit.INSTANCE)){throw new AssertionError("Old reader accepted new format");}
      catch(Exception e){if(!(e instanceof DataProblem d)||!d.getStatus().equals("UNSUPPORTED_VERSION"))throw e;System.out.println("OLD_A_REJECTED_FORMAT_3");}
      return;
    }
    if(Files.exists(root))throw new IllegalStateException("Fixture destination exists; retained");
    try(var w=StudioWorkspace.Companion.openStudio(root,s->Unit.INSTANCE)){
      if(w.getState().getVersion()!=2)throw new AssertionError("Not the frozen format-2 A");
      byte[] pixels=EditorRenderer.INSTANCE.synthetic(160,120);
      String image=w.importImage("format-two-fixture.png",pixels,"");
      w.newProject("Migration fixture",List.of(image));
      new TypesetRepository(w).create("Book fixture",List.of(image));
      var library=new LibraryRepository(w);
      library.label(image,"Synthetic original",List.of("migration","retained"));
      String prompt=library.savePrompt("synthetic blue landscape","Independent prompt","manual",null,List.of(),null);
      new PromptReferences(library).add(prompt);
      String archived=library.savePrompt("synthetic archived prompt","Archived prompt","manual",null,List.of(),null);
      new AssetArchive(w).change(Set.of(archived),true,()->Unit.INSTANCE);
      byte[] secret=new byte[32];new SecureRandom().nextBytes(secret);String fingerprint=hash(secret);
      String ref=new SecretVault(root.resolve("secrets")).put(secret);Arrays.fill(secret,(byte)0);
      var account=new AccountProfile(UUID.randomUUID().toString(),"DPAPI migration fixture","LLM","SYNTHETIC","https://synthetic.invalid","fixture",ref,false,null);
      var state=w.getState();w.save(state.copy(2,List.of(account),state.getProjects(),state.getDesigns(),state.getJobs(),state.getCreation(),state.getSavedCopies(),state.getTypesetProjects(),state.getTypesetFonts(),state.getTypesetPreferences()));
      String receipt="{\"sourceSha\":\"251dc34950990b0878e1910d16ed289e9400db8d\",\"format\":2,\"image\":\""+image+"\",\"imageSha256\":\""+hash(pixels)+"\",\"prompt\":\""+prompt+"\",\"archived\":\""+archived+"\",\"credentialRef\":\""+ref+"\",\"credentialSha256\":\""+fingerprint+"\"}";
      Files.writeString(Path.of(args[2]),receipt,StandardOpenOption.CREATE_NEW);
      System.out.println("FROZEN_A_CREATED_FORMAT_2_FIXTURE assets="+w.catalog().size());
    }
  }
}
