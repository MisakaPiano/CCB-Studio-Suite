package com.example.chatbar.desktop

import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.HWND
import java.awt.EventQueue
import java.awt.Window

/** Main attaches only its actual top-level ComposeWindow, never a foreground or renderer child. */
internal class DesktopDialogOwner {
    @Volatile private var attached: Pair<Window, Long>? = null

    fun attach(window: Window) {
        check(EventQueue.isDispatchThread())
        check(attached == null || attached?.first === window) { "A dialog owner is already attached" }
        if (DesktopWindowChrome.isWindows) {
            if (!window.isDisplayable) window.addNotify()
            val handle = HWND(Native.getWindowPointer(window))
            check(User32.INSTANCE.GetAncestor(handle, 2) == handle) { "Dialog owner must be the top-level HWND" }
            attached = window to Pointer.nativeValue(handle.pointer)
        }
    }

    fun detach(window: Window) {
        check(EventQueue.isDispatchThread())
        if (attached?.first === window) attached = null
    }

    fun currentHandle(): Long? = attached?.takeIf { (window, handle) ->
        window.isDisplayable && User32.INSTANCE.IsWindow(HWND(Pointer(handle)))
    }?.second

    fun requireHandle(): Long = checkNotNull(currentHandle()) { "Native file dialog requires the attached main window" }
}


