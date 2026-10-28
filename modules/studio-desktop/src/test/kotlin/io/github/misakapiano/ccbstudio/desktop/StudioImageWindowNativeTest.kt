package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.example.chatbar.desktop.*
import com.sun.jna.Native
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.*
import kotlinx.coroutines.*
import java.awt.EventQueue
import java.awt.Window as AwtWindow
import java.awt.event.WindowEvent
import java.nio.file.*
import java.util.concurrent.atomic.AtomicReference
import javax.accessibility.AccessibleContext
import kotlin.test.*

/** Real HWND regression. Only fresh synthetic windows; never drives a user application. */
class StudioImageWindowNativeTest {
    @Test fun firstMaximizeRestoreAndCloseAreConfinedToEachImageWindow(){
        check(DesktopWindowChrome.isWindows){"Windows evidence required, not a skipped native test"}
        val failure=AtomicReference<Throwable?>();val trace=mutableListOf<String>()
        fun record(s:String){trace+=s}
        application(exitProcessOnExit=false){
            var child by remember{mutableStateOf(0)}
            var shown by remember{mutableStateOf(true)}
            Window(onCloseRequest={failure.set(AssertionError("Parent close was wrongly requested"));exitApplication()},title="synthetic-window-owner",undecorated=true,state=rememberWindowState(width=800.dp,height=600.dp)){
                val chrome=rememberDesktopWindowChrome(window){failure.set(AssertionError("Parent chrome close was wrongly requested"));exitApplication()}
                StudioTheme{StudioImageWindowFrame("synthetic-window-owner",chrome){Text("Synthetic parent preserved")}}
                LaunchedEffect(Unit){
                    try{
                        withTimeout(10000){while(chrome.layout.captions.size!=3)delay(25)}
                        val parent=window;val bounds=parent.bounds;val parentHandle=HWND(Native.getWindowPointer(parent))
                        for(kind in 0..2){
                            child=kind;shown=true
                            var viewer:ComposeWindow?=null
                            withTimeout(15000){while(viewer==null){delay(50);viewer=AwtWindow.getWindows().filterIsInstance<ComposeWindow>().firstOrNull{it.isShowing&&it.title=="synthetic-image-$kind"}}}
                            val v=requireNotNull(viewer);delay(350)
                            assertTrue(v.isUndecorated);val normal=v.bounds;val h=HWND(Native.getWindowPointer(v));assertNotEquals(parentHandle.pointer,h.pointer)
                            val api=Native.load("user32",ChromeUser32::class.java)
                            fun target(hit:ChromeHit):LRESULT {
                                val origin=POINT();check(api.ClientToScreen(h,origin));val rect=RECT();check(User32.INSTANCE.GetClientRect(h,rect))
                                val scale=api.GetDpiForWindow(h)/96.0
                                val x=origin.x+rect.right-(if(hit==ChromeHit.CLOSE)23*scale else 69*scale).toInt()
                                val y=origin.y+(20*scale).toInt();val packed=((y.toLong() and 0xffff) shl 16) or (x.toLong() and 0xffff)
                                return User32.INSTANCE.SendMessage(h,0x84,WPARAM(0),LPARAM(packed))
                            }
                            suspend fun caption(name:String){
                                val hit=if(name=="关闭")ChromeHit.CLOSE else ChromeHit.MAXIMIZE
                                assertEquals(hit.nativeValue.toLong(),target(hit).toLong(),"Native caption hit belongs to viewer")
                                val command=desktopCaptionCommand(hit,api.IsZoomed(h))!!
                                val sys=when(command){DesktopWindowCommand.MAXIMIZE->0xF030;DesktopWindowCommand.RESTORE->0xF120;DesktopWindowCommand.CLOSE->0xF060;else->error("Unexpected command")}
                                User32.INSTANCE.PostMessage(h,0x112,WPARAM(sys.toLong()),LPARAM(0));delay(200)
                            }
                            caption("最大化")
                            withTimeout(7000){while(!api.IsZoomed(h))delay(25)}
                            assertFalse(api.IsZoomed(parentHandle));assertEquals(bounds,parent.bounds)
                            assertEquals(DesktopWindowCommand.RESTORE,desktopCaptionCommand(ChromeHit.MAXIMIZE,api.IsZoomed(h)));record("kind=$kind FIRST_MAXIMIZE_NATIVE_STATE_PASS")
                            caption("还原");withTimeout(7000){while(api.IsZoomed(h))delay(25)}
                            assertEquals(normal,v.bounds);record("kind=$kind RESTORE_BOUNDS_PASS")
                            caption("最大化");caption("关闭")
                            withTimeout(7000){while(v.isDisplayable)delay(25)}
                            assertFalse(shown);assertTrue(parent.isShowing);assertEquals(bounds,parent.bounds);assertNull(failure.get());record("kind=$kind CLOSE_MAXIMIZED_PARENT_ALIVE_PASS")
                        }
                    }catch(t:Throwable){failure.set(t)}finally{shown=false;exitApplication()}
                }
            }
            if(shown)CompositionLocalProvider(LocalStudioNativeWindows provides true){StudioTheme{
                when(child){
                    0->TypesetPreviewDialog("synthetic-image-0",{shown=false},actions={}){Text("Synthetic editor preview")}
                    1->StudioImageWindow("synthetic-image-1",{shown=false}){Text("Synthetic gallery image")}
                    else->DesktopImageToolWindow("synthetic-image-2",{shown=false}){Text("Synthetic existing image tool")}
                }
            }}
        }
        val output=Path.of(System.getProperty("preview.test.root")).resolve("native-image-window-results.txt");Files.createDirectories(output.parent);Files.writeString(output,trace.joinToString("\n")+"\nfailure="+failure.get())
        println(trace.joinToString("\n"));println("nativeWindowFailure="+failure.get())
        failure.get()?.let{throw it};assertEquals(9,trace.size)
    }
}
