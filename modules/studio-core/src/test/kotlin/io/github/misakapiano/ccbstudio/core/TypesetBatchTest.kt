package io.github.misakapiano.ccbstudio.core

import kotlin.test.*

class TypesetBatchTest {
    private fun page(vararg sizes:Double)=TypesetPage(assetId=newId(),name="合成页",texts=sizes.mapIndexed{i,size->TypesetText(text="第${i}项",x=17.0,y=29.0,width=300.0,style=TypesetStyle(fontSize=size))})
    @Test fun previousUsesFrozenSnapshotAndLastStyleForExtraObjects() {
        val a=page(20.0,30.0);val b=page(40.0,50.0,60.0);val c=page(70.0,80.0,90.0,100.0)
        val original=TypesetDocument(pages=listOf(a,b,c))
        val result=TypesetBatch.apply(original,setOf(a.id,b.id,c.id),true,null,TypesetMask()){100}
        assertEquals(a,result.pages[0])
        assertEquals(listOf(20.0,30.0,30.0),result.pages[1].texts.map{it.style.fontSize})
        assertEquals(listOf(40.0,50.0,60.0,60.0),result.pages[2].texts.map{it.style.fontSize})
        result.pages.drop(1).zip(original.pages.drop(1)).forEach{(after,before)->after.texts.zip(before.texts).forEach{(x,y)->
            assertEquals(y.id,x.id);assertEquals(y.text,x.text);assertEquals(y.x,x.x);assertEquals(y.y,x.y);assertEquals(95.0,x.width)
        }}
    }
    @Test fun currentStyleAppliesToAllTextsAndKeepsGeometryVisibilityAndUnselectedPage() {
        val a=page(20.0,30.0);val b=page(40.0)
        val style=TypesetText(text="不复制内容",vertical=true,visible=false,style=TypesetStyle(fontSize=72.0))
        val result=TypesetBatch.apply(TypesetDocument(pages=listOf(a,b)),setOf(a.id),false,style,TypesetMask(effect="mosaic")){1000}
        assertEquals(b,result.pages[1]);assertTrue(result.pages[0].texts.all{it.vertical && it.visible && it.style.fontSize==72.0})
        assertEquals(a.texts.map{it.text},result.pages[0].texts.map{it.text})
        assertEquals("mosaic",result.pages[0].toolDefaults!!.effect)
    }
    @Test fun missingPreviousTextDoesNotEraseTargetAndMaskDefaultsSurviveReopen() {
        val a=page().copy(toolDefaults=TypesetMask(effect="blur",strength=24,size=80.0))
        val b=page(50.0).copy(masks=listOf(TypesetMask(shape="rect",size=10.0),TypesetMask(shape="brush",points=listOf(TypesetPoint(1.0,2.0)))))
        val input=TypesetDocument(pages=listOf(a,b));val result=TypesetBatch.apply(input,setOf(b.id),true,null,TypesetMask()){100}
        assertEquals(b.texts,result.pages[1].texts);assertEquals(listOf(10.0,80.0),result.pages[1].masks.map{it.size})
        assertTrue(result.pages[1].masks.all{it.effect=="blur" && it.strength==24})
        val root=java.nio.file.Path.of(System.getProperty("preview.test.root")).resolve(newId())
        StudioWorkspace.openStudio(root).use{w->val id=w.importImage("fixture.png",EditorRenderer.synthetic(20,20));val repo=TypesetRepository(w);val p=repo.create("批次",listOf(id,id));repo.update(p.id,p.revision){result.copy(pages=result.pages.map{it.copy(assetId=id)})}}
        StudioWorkspace.openStudio(root).use{w->val d=TypesetRepository(w).load(w.state.typesetProjects.single());assertEquals(a.toolDefaults,d.pages[1].toolDefaults)}
    }
}
