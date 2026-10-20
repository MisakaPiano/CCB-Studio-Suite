package io.github.misakapiano.ccbstudio.core
import java.nio.file.*
import kotlin.test.*

class LiveRequestBudgetTest {
    @Test fun allServicesSharePersistentRollingLimitAndFiveMinuteSpacing() {
        val root=Paths.get(System.getProperty("preview.test.root")).resolve(newId());var now=1_800_000_000_000L
        fun budget()=LiveRequestBudget(root){now}
        repeat(12) {i->
            assertEquals(i+1,budget().reserve(if(i%2==0)"LLM" else "NAI"))
            assertFailsWith<BudgetWait>{budget().reserve("NAI")}
            if(i<11)now+=300000
        }
        now+=299999;assertFailsWith<BudgetWait>{budget().reserve("LLM")}
        now++;assertEquals(13,budget().reserve("NAI"))
        now-=1;assertEquals("CLOCK_ROLLBACK",assertFailsWith<DataProblem>{budget().reserve("NAI")}.status)
    }
    @Test fun truncatedReservationBlocksInsteadOfFreeingBudget() {
        val root=Paths.get(System.getProperty("preview.test.root")).resolve(newId());val budget=LiveRequestBudget(root)
        budget.reserve("LLM");Files.writeString(root.resolve("00000001.json"),"partial")
        assertEquals("CORRUPT",assertFailsWith<DataProblem>{budget.reserve("NAI")}.status)
        assertEquals("partial",Files.readString(root.resolve("00000001.json")))
    }
}
