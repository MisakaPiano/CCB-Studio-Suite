package io.github.misakapiano.ccbstudio.desktop;

import com.sun.jna.Native;
import com.sun.jna.win32.StdCallLibrary;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/** Compatibility for older helpers which started jpackage in the retired program.
 * Changing only the JVM cwd cannot release jpackage's waiting native parent.
 * Re-enter BEFORE opening a workspace; keep the original process alive until the
 * existing helper reaches its terminal receipt, so its child-exit check cannot race
 * the acknowledgement. No writer is killed, no acknowledgement is manufactured.
 */
public final class UpdateLaunchDirectory {
    private UpdateLaunchDirectory() {}
    private interface Kernel extends StdCallLibrary {
        int GetCurrentDirectoryW(int length, char[] buffer);
    }
    private static final Pattern TERMINAL = Pattern.compile("\"status\"\\s*:\\s*\"(?:COMPLETE|FAILED_RETAINED)\"");
    private static String option(String[] args, String key) {
        for (int i=0;i+1<args.length;i++) if (args[i].equals(key)) return args[i+1];
        return null;
    }
    public static boolean handoffIfNeeded(String[] args) throws Exception {
        String image=System.getProperty("jpackage.app-path"), planValue=option(args,"--update-handoff"), runtimeValue=option(args,"--runtime-root");
        if (!System.getProperty("os.name").startsWith("Windows") || image==null || planValue==null || runtimeValue==null) return false;
        Path exe=Path.of(image).toAbsolutePath().normalize(), app=exe.getParent();
        Path plan=Path.of(planValue), runtime=Path.of(runtimeValue);
        if (!plan.isAbsolute() || !runtime.isAbsolute() || !plan.normalize().startsWith(runtime.normalize().resolve("updates/handoffs"))) return false;
        char[] buffer=new char[32768];
        int length=Native.load("kernel32",Kernel.class).GetCurrentDirectoryW(buffer.length,buffer);
        if (length<=0 || length>=buffer.length) throw new java.io.IOException("Cannot inspect update launch directory");
        if (Path.of(new String(buffer,0,length)).toAbsolutePath().normalize().equals(app)) return false;
        ArrayList<String> command=new ArrayList<>();command.add(exe.toString());command.addAll(Arrays.asList(args));
        Process child=new ProcessBuilder(command).directory(app.toFile()).inheritIO().start();
        System.out.println("UPDATE_DIRECTORY_BRIDGE_STARTED pid="+child.pid());
        Path result=plan.getParent().resolve("result.json");
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(100);
        while (child.isAlive() && System.nanoTime()<deadline) {
            try {
                if (Files.isRegularFile(result) && Files.size(result)<=4*1024*1024
                        && TERMINAL.matcher(Files.readString(result)).find()) return true;
            } catch (java.io.IOException transientReceipt) { /* atomic receipt replacement; poll again */ }
            Thread.sleep(50);
        }
        // The old helper owns failure reporting. Do not kill the child or unlock data.
        System.out.println("UPDATE_DIRECTORY_BRIDGE_EXIT_WITHOUT_TERMINAL");
        return true;
    }
}
