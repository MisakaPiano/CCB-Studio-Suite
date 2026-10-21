package io.github.misakapiano.ccbstudio.core

import kotlin.test.*
import java.nio.file.Path

class TypesetPreferencesTest {
    @Test fun presetsSaveOnlyStyleAndSourceMaskSlotsHaveExactParameters() {
        val source=TypesetText(text="本页正文",x=200.0,y=90.0,locked=true,style=TypesetStyle(fontSize=72.0),vertical=true)
        var prefs=TypesetPreferences().saveText("字幕",source);val id=prefs.selectedTextPresetId!!
        val saved=prefs.textPresets.single().text;assertEquals("",saved.text);assertEquals(0.0,saved.x);assertFalse(saved.locked)
        val target=TypesetText(text="另一页",x=12.0,y=24.0,locked=true);val applied=TypesetPreferences.applyText(saved,target)
        assertEquals(target.text,applied.text);assertEquals(target.x,applied.x);assertTrue(applied.locked);assertTrue(applied.vertical);assertEquals(72.0,applied.style.fontSize)
        prefs=prefs.saveText("改名",source.copy(style=TypesetStyle(fontSize=48.0)),id);assertEquals(1,prefs.textPresets.size);assertEquals(id,prefs.textPresets.single().id)
        assertEquals(MaskToolSettings("mosaic",10,48.0,6.0,5.0,12.0),prefs.resolveMask("light_mosaic"))
        assertEquals(MaskToolSettings("mosaic",28,92.0,6.0,8.0,22.0),prefs.resolveMask("heavy_mosaic"))
        assertEquals(MaskToolSettings("white",16,70.0,14.0,3.0,14.0),prefs.copy(currentMask=MaskToolSettings(effect="white")).resolveMask("fine_stripes"))
        assertEquals(MaskToolSettings("black",16,100.0,6.0,12.0,10.0),prefs.resolveMask("coarse_stripes"))
        assertTrue(prefs.removeText(id).textPresets.isEmpty());assertFails{prefs.copy(preferredMaskSlot="unknown").validate()}
    }
    @Test fun newTextRespectsRememberedStyleAndSourceImageLimits() {
        val remembered=TypesetText(text="不复制",width=900.0,x=77.0,locked=true,style=TypesetStyle(fontSize=180.0),vertical=true)
        val small=TypesetPreferences.newText(100,80,remembered)
        assertEquals(95.0,small.width);assertEquals(25.0,small.style.fontSize);assertEquals(50.0,small.x);assertEquals(12.0,small.y);assertEquals("",small.text);assertFalse(small.locked);assertTrue(small.vertical)
        val normal=TypesetPreferences.newText(600,400,null);assertEquals(27.0,normal.style.fontSize);assertEquals(2.0,normal.style.strokeWidth);assertEquals(450.0,normal.width)
    }
    @Test fun globalPreferencesSurviveWorkspaceBackupAndRestore() {
        val root=Path.of(System.getProperty("preview.test.root")).resolve(newId());val target=root.resolveSibling(newId())
        val prefs=TypesetPreferences(preferredMaskSlot="heavy_mosaic",maskOverrides=mapOf("heavy_mosaic" to MaskToolSettings(strength=44))).saveText("竖排",TypesetText(vertical=true))
        val backup=StudioWorkspace.openStudio(root).use{w->w.save(w.state.copy(typesetPreferences=prefs));w.backup()}
        StudioRestore.restore(backup,target)
        StudioWorkspace.openStudio(target).use{w->assertEquals(prefs,w.state.typesetPreferences);assertEquals(44,w.state.typesetPreferences.resolveMask("heavy_mosaic").strength)}
    }
}
