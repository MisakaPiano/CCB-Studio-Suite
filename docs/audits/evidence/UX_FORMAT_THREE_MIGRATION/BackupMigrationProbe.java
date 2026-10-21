import io.github.misakapiano.ccbstudio.core.*;
import java.nio.file.*;
import kotlin.Unit;
public class BackupMigrationProbe {
  public static void main(String[] args)throws Exception{
    if(args[0].equals("backup")){
      try(var w=StudioWorkspace.Companion.openStudio(Path.of(args[1]),s->Unit.INSTANCE)){
        Files.writeString(Path.of(args[2]),w.backup().toString(),StandardOpenOption.CREATE_NEW);
        System.out.println("OLD_A_BACKUP_COMPLETE");
      }
    }else{
      Path target=Path.of(args[2]);StudioRestore.INSTANCE.restore(Path.of(Files.readString(Path.of(args[1]))),target,s->Unit.INSTANCE);
      StudioFormatMigration.INSTANCE.copyVersionTwo(target,Path.of(args[3]),s->Unit.INSTANCE);
      System.out.println("RESTORED_OLD_BACKUP_MIGRATED");
    }
  }
}
