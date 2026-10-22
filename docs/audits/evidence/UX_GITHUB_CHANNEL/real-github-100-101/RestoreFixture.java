import java.nio.file.Path;
import io.github.misakapiano.ccbstudio.core.StudioRestore;
import kotlin.Unit;
public class RestoreFixture {
    public static void main(String[] args) {
        Path restored = StudioRestore.INSTANCE.restore(Path.of(args[0]), Path.of(args[1]), point -> Unit.INSTANCE);
        System.out.println("PRODUCT_RESTORE_COMPLETED: " + restored);
    }
}
