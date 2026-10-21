package io.github.misakapiano.ccbstudio.core

import kotlin.test.*

class TypesetQueueTest {
    @Test fun multiSelectionKeepsRelativeOrderAndAnchorSemantics() {
        val pages=(1..6).map{TypesetPage(assetId=newId(),name=it.toString())};val d=TypesetDocument(pages=pages,startNumber=98,numberDigits=4)
        val selected=setOf(pages[1].id,pages[3].id)
        fun names(x:TypesetDocument)=x.pages.map{it.name}
        assertEquals(listOf("2","4","1","3","5","6"),names(TypesetQueue.move(d,selected,null,false)))
        assertEquals(listOf("1","3","5","6","2","4"),names(TypesetQueue.move(d,selected,null,true)))
        assertEquals(listOf("1","3","2","4","5","6"),names(TypesetQueue.move(d,selected,pages[4].id,false)))
        assertEquals(listOf("1","3","5","2","4","6"),names(TypesetQueue.move(d,selected,pages[4].id,true)))
        assertEquals(d,TypesetQueue.move(d,selected,pages[1].id,false))
        assertEquals("0098",TypesetQueue.number(d,0));assertEquals("0103",TypesetQueue.number(d,5))
    }
}
