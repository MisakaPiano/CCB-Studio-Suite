package io.github.misakapiano.ccbstudio.desktop

import io.github.misakapiano.ccbstudio.core.*
import java.nio.file.*
import kotlin.test.*

class EditorReviewCandidateTest {
    @Test fun isolatedReviewSeedPreservesOriginalsRejectsReseedAndHasNoAccounts(){
        val root=Path.of(System.getProperty("preview.test.root")).parent
        assertFailsWith<IllegalArgumentException>{EditorReviewCandidate.checkedRoot(root)}
        EditorReviewCandidate.prepare(root);EditorReviewCandidate.verifyOriginals(root)
        assertFailsWith<IllegalArgumentException>{EditorReviewCandidate.prepare(root)}
        StudioWorkspace.openStudio(root.resolve("workspace")).use{w->
            assertTrue(w.state.accounts.isEmpty());assertEquals(12,SharedAssets.images(w).size)
            val repo=TypesetRepository(w);val p=w.state.typesetProjects.single();val d=repo.load(p)
            assertEquals(12,d.pages.size);assertTrue(d.pages.first().texts.single().vertical);assertEquals(9,d.pages[1].masks.size)
            assertEquals(2,TypesetEditorExtrasStore.load(w).namedMasks.size);assertEquals(1,w.state.typesetPreferences.textPresets.size)
            repo.update(p.id,p.revision){it.copy(pages=it.pages.map{page->page.copy(caption="修改旁白不改原图")})}
        }
        EditorReviewCandidate.verifyOriginals(root)
        assertFailsWith<IllegalArgumentException>{EditorReviewCandidate.checkedRoot(root.resolve("workspace"),false)}
    }
}
