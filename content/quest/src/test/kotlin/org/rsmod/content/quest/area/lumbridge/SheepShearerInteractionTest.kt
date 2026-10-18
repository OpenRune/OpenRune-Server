package org.rsmod.content.quest.area.lumbridge

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.content.quest.area.QuestTalkFixture
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class SheepShearerInteractionTest {
    private fun fixture() = QuestTalkFixture(CoordGrid(3190, 3272, 0)) { SheepShearer(it) }

    @Test fun `accepting starts the quest at 1 on varp 179 and hands over shears`() {
        val f = fixture()
        f.talk("npc.fred_the_farmer", 1, 1)
        assertEquals(1, f.player.vars["varp.sheep"])
        assertEquals(1, f.count("obj.shears"))
        assertTrue(f.output().contains("This icon denotes a Spinning Wheel on the world map."))
    }

    @Test fun `accepting with shears in the inventory gives no second pair`() {
        val f = fixture()
        f.give("obj.shears")
        f.talk("npc.fred_the_farmer", 1, 1)
        assertEquals(1, f.player.vars["varp.sheep"])
        assertEquals(1, f.count("obj.shears"))
    }

    @Test fun `partial hand-ins add the balls to the stage and the last ball completes`() {
        val f = fixture()
        f.talk("npc.fred_the_farmer", 1, 1)
        repeat(5) { f.give("obj.ball_of_wool") }
        f.talk("npc.fred_the_farmer")
        assertEquals(6, f.player.vars["varp.sheep"])
        assertEquals(0, f.count("obj.ball_of_wool"))
        assertTrue(f.output().contains("You give Fred 5 balls of wool."), f.output())
        assertTrue(f.output().contains("I need 15 more before I can pay you."), f.output())

        repeat(20) { f.give("obj.ball_of_wool") }
        f.talk("npc.fred_the_farmer")
        assertEquals(21, f.player.vars["varp.sheep"])
        assertEquals(5, f.count("obj.ball_of_wool"))
        assertEquals(60, f.count("obj.coins"))
        assertEquals(1, f.player.vars["varp.qp"])
        assertEquals(150, f.player.statMap.getXP("stat.crafting"))
        assertFalse(f.output().contains("You give Fred"), f.output())
        assertTrue(f.output().contains("That's the last of them."), f.output())

        f.talk("npc.fred_the_farmer", 2)
        assertEquals(60, f.count("obj.coins"))
        assertEquals(5, f.count("obj.ball_of_wool"))
    }

    @Test fun `twenty balls on the first talk complete the quest before The Thing and the prompt`() {
        val f = fixture()
        repeat(21) { f.give("obj.ball_of_wool") }
        f.talk("npc.fred_the_farmer", 1)
        assertEquals(21, f.player.vars["varp.sheep"])
        assertEquals(1, f.count("obj.ball_of_wool"))
        assertEquals(60, f.count("obj.coins"))
        assertEquals(1, f.player.vars["varp.qp"])
        assertTrue(f.output().contains("I actually have 20 balls of wool already on me."))
        assertFalse(f.output().contains("What do you mean, The Thing?"), f.output())
        assertFalse(f.output().contains("You give Fred"), f.output())
        assertFalse(f.output().contains("That's the last of them."), f.output())
    }

    @Test fun `a full coin stack keeps the last wool and leaves the quest open`() {
        val f = fixture()
        f.talk("npc.fred_the_farmer", 1, 1)
        f.give("obj.coins", Int.MAX_VALUE)
        repeat(20) { f.give("obj.ball_of_wool") }
        f.talk("npc.fred_the_farmer")
        assertEquals(1, f.player.vars["varp.sheep"])
        assertEquals(20, f.count("obj.ball_of_wool"))
        assertEquals(Int.MAX_VALUE, f.count("obj.coins"))
        assertEquals(0, f.player.vars["varp.qp"])
        assertTrue(f.output().contains("You don't have enough room for the reward."), f.output())
    }

    @Test fun `a full coin stack keeps the wool on the first talk too`() {
        val f = fixture()
        f.give("obj.coins", Int.MAX_VALUE)
        repeat(20) { f.give("obj.ball_of_wool") }
        f.talk("npc.fred_the_farmer", 1)
        assertEquals(0, f.player.vars["varp.sheep"])
        assertEquals(20, f.count("obj.ball_of_wool"))
        assertEquals(0, f.player.vars["varp.qp"])
    }

    companion object {
        @JvmStatic @BeforeAll fun cache() = QuestTalkFixture.setUpCache()

        @JvmStatic @AfterAll fun restore() = QuestTalkFixture.restore()
    }
}
