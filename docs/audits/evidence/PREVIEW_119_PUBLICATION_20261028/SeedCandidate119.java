import io.github.misakapiano.ccbstudio.core.*;
import io.github.misakapiano.ccbstudio.desktop.StudioUpdateHandoffKt;
import kotlin.Unit;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;

class SeedCandidate119 {
 static String hash(byte[] b)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));}
 public static void main(String[] a)throws Exception{
  Path data=Path.of(a[1]).toAbsolutePath().normalize(), record=Path.of(a[2]);
  if(!data.equals(Path.of("H:/CCB-Studio-Suite/.local/disposable/p119n-9f3fa12a1bfd436ab87e075b099f7068/payload/install/data/workspace")))throw new IllegalArgumentException("Authorized synthetic root required");
  try(StudioWorkspace w=StudioWorkspace.Companion.openStudio(data,s->Unit.INSTANCE)){
   SecretVault vault=new SecretVault(data.resolve("secrets"));
   if(a[0].equals("seed")){
    List<String> rows=new ArrayList<>();List<AccountProfile> accounts=new ArrayList<>();
    for(int i=0;i<2;i++){
     BufferedImage image=new BufferedImage(24+i,28+i,BufferedImage.TYPE_INT_ARGB);
     for(int x=0;x<image.getWidth();x++)for(int y=0;y<image.getHeight();y++)image.setRGB(x,y,0xff225588+i*0x112233);
     ByteArrayOutputStream bytes=new ByteArrayOutputStream();ImageIO.write(image,"png",bytes);image.flush();
     byte[] raw=bytes.toByteArray();String id=w.importImage("synthetic-retained-"+i+".png",raw,"synthetic upgrade recipe "+i);rows.add("asset,"+id+","+hash(raw));
     byte[] secret=("SYNTHETIC_CANDIDATE119_CREDENTIAL_"+i).getBytes(StandardCharsets.UTF_8);String ref=vault.put(secret);rows.add("credential,"+ref+","+hash(secret));Arrays.fill(secret,(byte)0);
     accounts.add(new AccountProfile(UUID.randomUUID().toString(),"Synthetic upgrade "+i,i==0?"NAI":"LLM","SYNTHETIC","https://synthetic.invalid","fixture-model",ref,false,null));
    }
    StudioState s=w.getState();w.save(s.copy(s.getVersion(),accounts,s.getProjects(),s.getDesigns(),s.getJobs(),s.getCreation(),s.getSavedCopies(),s.getTypesetProjects(),s.getTypesetFonts(),s.getTypesetPreferences(),s.getBatch(),s.getGenerationOrganization()));Files.write(record,rows,StandardCharsets.UTF_8);
   }
   int assets=0,secrets=0;
   for(String line:Files.readAllLines(record)){
    String[] row=line.split(",");if(row[0].equals("asset")){if(!hash(w.bytes(row[1])).equals(row[2]))throw new AssertionError("Asset changed");assets++;}
    else{vault.use(row[1],bytes->{try{if(!hash(bytes).equals(row[2]))throw new AssertionError("Synthetic credential changed");return Unit.INSTANCE;}catch(Exception e){throw new RuntimeException(e);}});secrets++;}
   }
   if(assets!=2||secrets!=2)throw new AssertionError("Incomplete fixture");
   if(a[0].equals("snapshot"))Files.writeString(Path.of(a[3]),StudioUpdateHandoffKt.updateSnapshot(w,w.getState().getVersion(),false).toString());
   System.out.println("PASS 2 original assets, 2 referenced synthetic credentials; plaintext not exported");
  }
 }
}
