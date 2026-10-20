package com.example.chatbar.desktop

import kotlin.test.*

/** Suite contract tests for the pinned native bridge, independent of Desktop routes. */
class SuiteChromeTest {
    private val layout=DesktopChromeLayout(1000f,700f,
        title=ChromeRect(0f,0f,1000f,44f),
        interactive=listOf(ChromeRect(160f,6f,440f,38f)),
        captions=mapOf(ChromeHit.MINIMIZE to ChromeRect(862f,0f,908f,44f),
            ChromeHit.MAXIMIZE to ChromeRect(908f,0f,954f,44f),
            ChromeHit.CLOSE to ChromeRect(954f,0f,1000f,44f)),
        icon=ChromeRect(0f,0f,42f,44f))
    private fun hit(x:Float,y:Float,maximized:Boolean=false,width:Float=1000f,height:Float=700f)=
        DesktopChromeHitTest.hit(x,y,width,height,6f,maximized,layout)

    @Test fun measuredControlsNeverBecomeDragRegions() {
        assertEquals(ChromeHit.CLIENT,hit(200f,20f))
        assertEquals(ChromeHit.CAPTION,hit(600f,20f))
        assertEquals(ChromeHit.SYSTEM_MENU,hit(20f,20f))
        assertEquals(ChromeHit.CLIENT,hit(600f,150f))
    }
    @Test fun captionsKeepTheirCommandsAtScaledClientSize() {
        assertEquals(ChromeHit.MINIMIZE,hit(880f*1.5f,20f*1.5f,width=1500f,height=1050f))
        assertEquals(ChromeHit.MAXIMIZE,hit(930f*1.5f,20f*1.5f,width=1500f,height=1050f))
        assertEquals(ChromeHit.CLOSE,hit(977f*1.5f,20f*1.5f,width=1500f,height=1050f))
        assertEquals(ChromeHit.CLIENT,hit(200f*1.5f,20f*1.5f,width=1500f,height=1050f))
    }
    @Test fun resizeEdgesBelongToWindowsAndStopWhenMaximized() {
        assertEquals(ChromeHit.TOP_LEFT,hit(1f,1f))
        assertEquals(ChromeHit.BOTTOM_RIGHT,hit(999f,699f))
        assertEquals(ChromeHit.LEFT,hit(1f,350f))
        assertEquals(ChromeHit.CLIENT,hit(1f,350f,true))
        assertEquals(ChromeHit.CLOSE,hit(999f,20f,true))
        assertEquals(ChromeHit.CLIENT,hit(-1f,20f))
    }
    @Test fun maximizedButtonRestoresAndNativeCloseUsesDrainCallback() {
        assertEquals(DesktopWindowCommand.MAXIMIZE,desktopCaptionCommand(ChromeHit.MAXIMIZE,false))
        assertEquals(DesktopWindowCommand.RESTORE,desktopCaptionCommand(ChromeHit.MAXIMIZE,true))
        assertNull(desktopCaptionCommand(ChromeHit.CAPTION,false))
        var requested=0
        val chrome=DesktopWindowChrome { requested++ }
        chrome.command(DesktopWindowCommand.CLOSE)
        assertEquals(1,requested)
        chrome.requestNativeClose()
        java.awt.EventQueue.invokeAndWait { }
        assertEquals(2,requested)
    }
    @Test fun negativeMonitorCoordinatesAreSignExtended() {
        val packed=((-720L and 0xffff) shl 16) or (-1920L and 0xffff)
        assertEquals(-1920 to -720,DesktopChromeHitTest.screenPoint(packed))
        assertEquals(20 to 40,DesktopChromeHitTest.screenToClient(-1900,-680,-1920,-720))
    }
    @Test fun unmeasuredLayoutCannotInventCaptionTargets() {
        assertEquals(ChromeHit.CLIENT,DesktopChromeHitTest.hit(200f,20f,1000f,700f,6f,false,DesktopChromeLayout()))
    }
}
