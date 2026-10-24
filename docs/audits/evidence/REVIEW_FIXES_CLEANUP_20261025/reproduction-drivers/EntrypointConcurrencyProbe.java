import io.github.misakapiano.ccbstudio.desktop.DesktopEntrypoints;
import io.github.misakapiano.ccbstudio.core.DataProblem;
import java.nio.file.*;
public class EntrypointConcurrencyProbe {
 public static void main(String[] args)throws Exception {
  System.setProperty("jpackage.app-path",Path.of(args[0],"CCBStudioSuitePreview.exe").toString());
  for(int attempt=0;attempt<100;attempt++)try {
   boolean accepted=DesktopEntrypoints.INSTANCE.ensure(Path.of(args[1]),Path.of(args[2]));
   Files.writeString(Path.of(args[3]),"{\"status\":\"RETURNED\",\"accepted\":"+accepted+"}");return;
  }catch(DataProblem e){if(e.getStatus().equals("ENTRYPOINT_BUSY")){Thread.sleep(20);continue;}throw e;}
  catch(Exception e){Files.writeString(Path.of(args[3]),"{\"status\":\"REJECTED\",\"reason\":\""+e.getClass().getSimpleName()+"\"}");return;}
  throw new IllegalStateException("Entrypoint did not drain");
 }
}
