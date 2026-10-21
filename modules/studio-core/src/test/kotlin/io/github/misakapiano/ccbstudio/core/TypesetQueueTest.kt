package io.github.misakapiano.ccbstudio.core

import kotlin.test.*

class TypesetQueueTest {
    @Test fun displayedNumberBeforeAfterUsesSourceRoundingAndRetainsBatchOrder() {
        val pages=(1..6).map{TypesetPage(assetId=newId(),name=it.toString())};val d=TypesetDocument(pages=pages,startNumber=98,numberDigits=4)
        val selected=setOf(pages[1].id,pages[3].id)
        assertEquals(listOf("1","3","2","4","5","6"),TypesetQueue.moveByNumber(d,selected,102.0,false).pages.map{it.name})
        assertEquals(listOf("1","3","5","2","4","6"),TypesetQueue.moveByNumber(d,selected,101.5,true).pages.map{it.name})
        assertEquals(d,TypesetQueue.moveByNumber(d,selected,99.0,false))
        listOf(Double.NaN,Double.POSITIVE_INFINITY,97.0,104.0).forEach{assertFails{TypesetQueue.moveByNumber(d,selected,it,false)}}
        assertEquals((1..6).map{it.toString()},d.pages.map{it.name})
    }
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
