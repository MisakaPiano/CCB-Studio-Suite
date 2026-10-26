import io.github.misakapiano.ccbstudio.core.StudioWorkspace;
import io.github.misakapiano.ccbstudio.core.SecretVault;
import kotlin.Unit;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.util.*;

class SeedRelocation {
    static String hash(byte[] bytes) {
        try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
        catch(Exception e){throw new RuntimeException(e);}
    }
    public static void main(String[] args) throws Exception {
        Path data=Path.of(args[1]),record=Path.of(args[2]);
        if(!data.toString().contains("review-update-native")&&!data.toString().contains("proof117-pop-"))throw new IllegalArgumentException("Synthetic root only");
        try(StudioWorkspace workspace=StudioWorkspace.Companion.openStudio(data,s->Unit.INSTANCE)) {
            SecretVault vault=new SecretVault(data.resolve("secrets"));
            if(args[0].equals("seed")) {
                List<String> lines=new ArrayList<>();
                for(int i=0;i<2;i++) {
                    BufferedImage image=new BufferedImage(24+i,26+i,BufferedImage.TYPE_INT_ARGB);
                    for(int x=0;x<image.getWidth();x++)for(int y=0;y<image.getHeight();y++)image.setRGB(x,y,0xff224466+i*0x112233);
                    ByteArrayOutputStream out=new ByteArrayOutputStream();ImageIO.write(image,"png",out);byte[] bytes=out.toByteArray();
                    String id=workspace.importImage("synthetic-before-move-"+i+".png",bytes,"synthetic retained recipe "+i);
                    lines.add("asset,"+id+","+hash(bytes));image.flush();
                    byte[] secret=("SYNTHETIC_RELOCATION_CREDENTIAL_"+i).getBytes(StandardCharsets.UTF_8);
                    String ref=vault.put(secret);lines.add("credential,"+ref+","+hash(secret));Arrays.fill(secret,(byte)0);
                }
                Files.write(record,lines,StandardCharsets.UTF_8);
            }
            int assets=0,secrets=0;
            for(String line:Files.readAllLines(record,StandardCharsets.UTF_8)) {
                String[] values=line.split(",");
                if(values[0].equals("asset")){if(!hash(workspace.bytes(values[1])).equals(values[2]))throw new AssertionError("Asset changed");assets++;}
                else {vault.use(values[1],bytes->{if(!hash(bytes).equals(values[2]))throw new AssertionError("Synthetic credential changed");return Unit.INSTANCE;});secrets++;}
            }
            if(assets!=2||secrets!=2)throw new AssertionError("Incomplete seed");
            System.out.println("PASS assets=2 syntheticCredentials=2; production workspace and vault API; plaintext not exported");
        }
    }
}
