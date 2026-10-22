package io.github.misakapiano.ccbstudio.core

import java.nio.file.Path
import kotlin.test.*

class DisguiseCoversTest {
    private fun root()=Path.of(System.getProperty("preview.test.root"),newId())
    @Test fun multiplePresetsDefaultChoiceAndBackupKeepOriginalCoverBytes() {
        val r=root();lateinit var first:DisguiseCover;lateinit var second:DisguiseCover;lateinit var backup:Path
        val a=EditorRenderer.synthetic(60,40);val b=EditorRenderer.synthetic(70,50)
        StudioWorkspace.openStudio(r.resolve("workspace")).use{w->
            val covers=DisguiseCovers(w);first=covers.add("First cover","first.png",a);second=covers.add("Second cover","second.png",b)
            assertEquals(2,covers.load().covers.size);assertEquals(second.id,covers.load().selected)
            assertEquals(second,covers.add("Duplicate","duplicate.png",b));assertEquals(2,covers.load().covers.size)
            covers.select(null);assertNull(covers.load().selected);assertEquals(2,covers.load().covers.size)
            covers.select(first.id);covers.rename(first.id,"Renamed cover")
            assertContentEquals(a,covers.bytes(covers.load().covers.first{it.id==first.id}))
            assertTrue(SharedAssets.images(w).isEmpty(),"Cover storage is separate from the ordinary gallery projection")
            backup=w.backup()
        }
        StudioWorkspace.openStudio(r.resolve("workspace")).use{w->
            val covers=DisguiseCovers(w);assertEquals(first.id,covers.load().selected);assertEquals("Renamed cover",covers.load().covers.first{it.id==first.id}.name)
            assertContentEquals(b,covers.bytes(second));covers.remove(second.id);assertEquals(1,covers.load().covers.size)
            assertContentEquals(b,w.bytes(second.id),"Removing a preset must not delete immutable bytes")
        }
        val restored=StudioRestore.restore(backup,r.resolve("restored"))
        StudioWorkspace.openStudio(restored).use{w->
            val covers=DisguiseCovers(w);assertEquals(2,covers.load().covers.size);assertEquals(first.id,covers.load().selected)
            assertContentEquals(a,covers.bytes(covers.load().covers.first{it.id==first.id}));assertContentEquals(b,covers.bytes(second))
        }
    }
    @Test fun unreadableOrFutureCoverSettingsAreNeverSilentlyReset() {
        StudioWorkspace.openStudio(root()).use{w->
            val library=LibraryRepository(w)
            for(raw in listOf("{broken","{\"version\":2,\"covers\":[],\"selected\":null}")) {
                library.update{it.copy(preferences=it.preferences+(DisguiseCovers.KEY to raw))}
                assertFails{DisguiseCovers(w).select(null)};assertFails{DisguiseCovers(w).add("new","new.png",EditorRenderer.synthetic(8,8))}
                assertEquals(raw,library.load().preferences[DisguiseCovers.KEY])
            }
        }
    }
}
