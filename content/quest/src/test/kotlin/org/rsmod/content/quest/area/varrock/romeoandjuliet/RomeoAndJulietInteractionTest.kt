package org.rsmod.content.quest.area.varrock.romeoandjuliet

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.content.quest.area.QuestTalkFixture
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class RomeoAndJulietInteractionTest {
    private fun fixture() = QuestTalkFixture(CoordGrid(3211, 3421, 0)) { RomeoAndJuliet(it) }

    private fun QuestTalkFixture.stage() = player.vars["varp.rjquest"]

    @Test fun `the quest walks through the OSRS stages on varp 144`() {
        val f = fixture()
        f.talk("npc.romeo", 1, 1, 3)
        assertEquals(10, f.stage())

        f.talk("npc.juliet")
        assertEquals(20, f.stage())
        assertEquals(1, f.count("obj.julietmessage"))

        f.talk("npc.romeo", 4)
        assertEquals(30, f.stage())
        assertEquals(0, f.count("obj.julietmessage"))

        f.talk("npc.father_lawrence")
        assertEquals(40, f.stage())

        f.talk("npc.apothecary", 2, 1, 4)
        assertEquals(50, f.stage())
        assertEquals(0, f.count("obj.cadava"))

        f.give("obj.cadavaberries")
        f.talk("npc.apothecary", 2, 1)
        assertEquals(50, f.stage())
        assertEquals(0, f.count("obj.cadavaberries"))
        assertEquals(1, f.count("obj.cadava"))

        f.talk("npc.juliet_multi_visible")
        assertEquals(60, f.stage())
        assertEquals(0, f.count("obj.cadava"))

        f.talk("npc.romeo")
        assertEquals(100, f.stage())
        assertEquals(5, f.player.vars["varp.qp"])
    }

    @Test fun `a lost message is replaced and the apothecary hides the quest topic early`() {
        val f = fixture()
        f.talk("npc.romeo", 3, 1, 3)
        f.talk("npc.juliet")
        f.player.inv.indices.forEach { if (f.player.inv[it]?.id != null) f.player.inv[it] = null }
        f.talk("npc.juliet")
        assertEquals(1, f.count("obj.julietmessage"))
        assertEquals(20, f.stage())

        f.talk("npc.apothecary", 2, 3)
        assertEquals(20, f.stage())
    }

    @Test fun `berries in hand on the first apothecary visit make the potion at once`() {
        val f = fixture()
        f.player.vars.let { org.rsmod.api.player.vars.VarPlayerIntMapSetter.set(f.player, "varbit.romeo_juliet_progress", 40) }
        f.give("obj.cadavaberries")
        f.talk("npc.apothecary", 2, 1)
        assertEquals(50, f.stage())
        assertEquals(1, f.count("obj.cadava"))
    }

    companion object {
        @JvmStatic @BeforeAll fun cache() = QuestTalkFixture.setUpCache()

        @JvmStatic @AfterAll fun restore() = QuestTalkFixture.restore()
    }
}
