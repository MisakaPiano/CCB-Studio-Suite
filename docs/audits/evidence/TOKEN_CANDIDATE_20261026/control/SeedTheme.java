import java.nio.file.*;
import io.github.misakapiano.ccbstudio.core.*;
import io.github.misakapiano.ccbstudio.desktop.*;
public final class SeedTheme {
 public static void main(String[] args) throws Exception {
  Path base=Path.of("H:/CCB-Studio-Suite/.local/token-candidate-20261026").toAbsolutePath().normalize();
  Path root=Path.of(args[0]).toAbsolutePath().normalize();
  if(!root.equals(base.resolve("entry-review/workspace")) || Files.exists(root))throw new IllegalArgumentException("Fresh exact synthetic root required");
  StudioWorkspace w=StudioWorkspace.Companion.openStudio(root,note->kotlin.Unit.INSTANCE);
  try {
   if(!w.getState().getAccounts().isEmpty())throw new IllegalStateException("Synthetic accounts must be empty");
   StudioAppearanceStore store=new StudioAppearanceStore(new LibraryRepository(w,note->kotlin.Unit.INSTANCE));
   StudioDisplaySettings settings=new StudioDisplaySettings(StudioThemeMode.DARK,StudioColorStyle.CUSTOM_ACCENT,0x000000);
   store.save(settings);
   if(!settings.equals(store.load()))throw new IllegalStateException("Theme seed not retained");
   System.out.println("SYNTHETIC_THEME_SEEDED DARK 000000; no provider/credentials; root="+root);
  } finally {w.close();}
 }
}