import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
public class InstalledFutureRootProbe {
  public static void main(String[] args) throws Exception {
    Path root=Path.of(args[0]), runtime=Path.of(args[1]), installed=Path.of(args[2]);
    Class<?> type=Class.forName("io.github.misakapiano.ccbstudio.desktop.StudioDataLaunchKt");
    Method method=Arrays.stream(type.getDeclaredMethods()).filter(m->m.getName().equals("openStudioForLaunch")&&m.getParameterCount()==5).findFirst().orElseThrow();
    Object confirm=Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{method.getParameterTypes()[3]},(proxy,m,a)->{throw new AssertionError("Future root must not request migration");});
    try {method.invoke(null,root,runtime,null,confirm,installed);throw new AssertionError("Future root unexpectedly opened");}
    catch(InvocationTargetException failure){
      Throwable cause=failure.getCause();
      if(!cause.getClass().getName().equals("io.github.misakapiano.ccbstudio.core.DataProblem"))throw failure;
      Object status=cause.getClass().getMethod("getStatus").invoke(cause);
      if(!"UNSUPPORTED_VERSION".equals(status))throw failure;
      System.out.println("INSTALLED_STARTUP_CORE_REJECTED_UNSUPPORTED_VERSION");
    }
  }
}
