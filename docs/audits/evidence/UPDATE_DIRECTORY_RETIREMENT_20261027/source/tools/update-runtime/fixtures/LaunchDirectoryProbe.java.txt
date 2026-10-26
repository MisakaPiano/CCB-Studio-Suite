import io.github.misakapiano.ccbstudio.desktop.UpdateLaunchDirectory;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

/** No UI, credentials, provider, or application data. Real jpackage process host. */
public class LaunchDirectoryProbe {
    public static void main(String[] args) throws Exception {
        if (UpdateLaunchDirectory.handoffIfNeeded(args)) return;
        Path plan=null;
        for (int i=0;i+1<args.length;i++) if (args[i].equals("--update-handoff")) plan=Path.of(args[i+1]);
        if (plan==null) throw new IllegalArgumentException("Synthetic plan required");
        Path folder=plan.getParent();
        Files.writeString(folder.resolve("B_VERIFIED.json"),"{\"syntheticDirectoryProbe\":true}");
        Files.writeString(folder.resolve("READY"),ProcessHandle.current().pid()+"\n"+System.getProperty("user.dir")+"\n");
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(60);
        while (!Files.exists(folder.resolve("STOP")) && System.nanoTime()<deadline) Thread.sleep(50);
    }
}
