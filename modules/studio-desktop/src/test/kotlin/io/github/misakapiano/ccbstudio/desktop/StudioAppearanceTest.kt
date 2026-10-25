package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.ui.graphics.Color
import com.example.chatbar.domain.appearance.DefaultThemeColorHsv
import io.github.misakapiano.ccbstudio.core.*
import java.nio.file.Path
import kotlin.test.*

class StudioAppearanceTest {
    private fun root()=Path.of(requireNotNull(System.getProperty("preview.test.root")),newId())

    @Test fun modesAndColorStylesHaveReadableForegroundsWithoutChangingTheExistingDefault() {
        val defaults=studioSemanticColors(StudioDisplaySettings(),false)
        assertTrue(defaults.dark)
        assertEquals(Color(0xff7db5ff),defaults.primary)
        assertEquals(Color(0xff0d1117),defaults.background)
        assertEquals(Color(0xff233e5e),defaults.accent)
        for(mode in StudioThemeMode.entries)for(style in StudioColorStyle.entries)for(system in listOf(false,true)) {
            for(rgb in listOf(0,0xffffff,0xff0000,0x00ff00,0x0000ff,0x7db5ff)) {
                val palette=studioSemanticColors(StudioDisplaySettings(mode,style,rgb),system)
                assertEquals(when(mode){StudioThemeMode.SYSTEM->system;StudioThemeMode.LIGHT->false;StudioThemeMode.DARK->true},palette.dark)
                assertTrue(studioContrastRatio(palette.primary,palette.primaryForeground)>=4.5)
                assertTrue(studioContrastRatio(palette.foreground,palette.background)>=7)
                assertTrue(studioContrastRatio(palette.libraryText,palette.libraryPanel)>=7)
                assertTrue(studioContrastRatio(palette.mutedForeground,palette.background)>=4.5)
                if(style==StudioColorStyle.CCB_NATIVE)assertEquals(Color(DefaultThemeColorHsv.toOpaqueArgb()),palette.primary)
                if(style==StudioColorStyle.CUSTOM_ACCENT)assertEquals(Color(0xff000000.toInt() or rgb),palette.primary)
            }
        }
    }

    @Test fun strictSettingsRejectFutureMalformedAndOutOfRangeValuesInsteadOfReplacingThem() {
        assertEquals(StudioDisplaySettings(),StudioDisplaySettings.decode(null))
        val sample=StudioDisplaySettings(StudioThemeMode.SYSTEM,StudioColorStyle.NEUTRAL,0x123456)
        assertEquals(sample,StudioDisplaySettings.decode(sample.encode()))
        val valid=sample.encode()
        for(raw in listOf("", "[]", "{}",valid.replace("\"format\":1","\"format\":2"),
            valid.replace("\"format\":1","\"format\":\"1\""),valid.replace("SYSTEM","FUTURE"),
            valid.replace("1193046","-1"),valid.replace("1193046","16777216"),
            valid.dropLast(1)+",\"future\":true}"," ".repeat(1025)))assertFails {StudioDisplaySettings.decode(raw)}
        assertFails {StudioDisplaySettings(accentRgb=-1)}
        assertFails {StudioDisplaySettings(accentRgb=0x1000000)}
    }

    @Test fun saveUsesLatestMetadataAndReopensWithoutChangingOriginalImageBytes() {
        val directory=root();val bytes=EditorRenderer.synthetic(8,8);var image=""
        val chosen=StudioDisplaySettings(StudioThemeMode.LIGHT,StudioColorStyle.CUSTOM_ACCENT,0xd15747)
        StudioWorkspace.openStudio(directory).use {w->
            val repo=LibraryRepository(w);val store=StudioAppearanceStore(repo)
            val before=w.catalog().size
            assertEquals(StudioDisplaySettings(),store.load());assertEquals(before,w.catalog().size)
            image=w.importImage("synthetic-theme-original.png",bytes)
            repo.label(image,"synthetic retained alias",listOf("fixture"))
            // A separate writer changes an unrelated preference after the appearance owner loaded.
            repo.update{it.copy(preferences=it.preferences+("unrelatedFixture" to "latest"))}
            val latest=repo.load()
            store.save(chosen)
            assertEquals(latest.copy(preferences=latest.preferences+(StudioDisplaySettings.KEY to chosen.encode())),repo.load())
            assertContentEquals(bytes,w.bytes(image))
            val savedCount=w.catalog().size
            store.save(chosen);assertEquals(savedCount,w.catalog().size)
        }
        StudioWorkspace.openStudio(directory).use {w->
            assertEquals(chosen,StudioAppearanceStore(LibraryRepository(w)).load())
            assertContentEquals(bytes,w.bytes(image))
            assertEquals("latest",LibraryRepository(w).load().preferences["unrelatedFixture"])
        }
    }

    @Test fun damagedLatestAppearanceBlocksWritesAndRetainsItsRevisionAndOriginal() {
        StudioWorkspace.openStudio(root()).use {w->
            val repo=LibraryRepository(w);val store=StudioAppearanceStore(repo)
            assertEquals(StudioDisplaySettings(),store.load())
            val bytes=EditorRenderer.synthetic(8,8);val image=w.importImage("synthetic-retained.png",bytes)
            repo.update{it.copy(preferences=it.preferences+(StudioDisplaySettings.KEY to "{damaged fixture"))}
            val original=repo.load();val count=w.catalog().size
            assertFails {store.load()}
            assertFails {store.save(StudioDisplaySettings(mode=StudioThemeMode.LIGHT))}
            assertEquals(count,w.catalog().size);assertEquals(original,repo.load())
            assertContentEquals(bytes,w.bytes(image))
        }
    }
}
