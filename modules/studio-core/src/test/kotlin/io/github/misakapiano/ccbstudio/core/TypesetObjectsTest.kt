package io.github.misakapiano.ccbstudio.core

import kotlin.test.*

class TypesetObjectsTest {
    @Test fun layerOrderControlsRenderingCopyIsIndependentAndDeleteKeepsValidOrder() {
        val text=TypesetText(text="■",x=80.0,y=30.0,width=100.0,style=TypesetStyle(fontSize=80.0,fill="#ffffff"))
        val mask=TypesetMask(effect="black",x=0.0,y=0.0,width=160.0,height=160.0)
        val p=TypesetPage(assetId=newId(),name="layers",texts=listOf(text),masks=listOf(mask))
        assertEquals(listOf(mask.id,text.id),TypesetObjects.order(p))
        val reordered=TypesetObjects.move(p,mask.id,1)
        assertEquals(listOf(text.id,mask.id),TypesetObjects.order(reordered))
        val image=TypesetRenderer.render(EditorRenderer.synthetic(160,160),reordered)
        try{for(y in 1..158)for(x in 1..158)assertEquals(0xff000000.toInt(),image.getRGB(x,y))}finally{image.flush()}
        val locked=TypesetObjects.lock(reordered,text.id,true);assertTrue(locked.texts.single().locked)
        val copiedId=newId();val copied=TypesetObjects.copy(locked,text.id,copiedId)
        assertTrue(copied.texts.first().locked);assertFalse(copied.texts.last().locked)
        assertEquals(text.x+20,copied.texts.last().x);assertEquals(text.text,copied.texts.last().text)
        val removed=TypesetObjects.remove(copied,text.id);TypesetDocument(pages=listOf(removed)).validate()
        assertEquals(listOf(mask.id,copiedId),TypesetObjects.order(removed))
    }
    @Test fun batchStyleDoesNotChangeTargetLockState() {
        val target=TypesetText(locked=true);val p=TypesetPage(assetId=newId(),name="lock",texts=listOf(target))
        val result=TypesetBatch.apply(TypesetDocument(pages=listOf(p)),setOf(p.id),false,TypesetText(locked=false),TypesetMask()){100}
        assertTrue(result.pages.single().texts.single().locked)
    }
}
