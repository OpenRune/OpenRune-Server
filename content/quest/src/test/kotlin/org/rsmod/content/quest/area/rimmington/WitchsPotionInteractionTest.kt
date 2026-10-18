package org.rsmod.content.quest.area.rimmington

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
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
class WitchsPotionInteractionTest {
    private fun fixture() = QuestTalkFixture(CoordGrid(2967, 3206, 0)) { WitchsPotion() }

    private val ingredients =
        listOf("obj.eye_of_newt", "obj.rats_tail", "obj.onion", "obj.burnt_meat")

    @Test fun `accepting sets varp 67 to 1`() {
        val f = fixture()
        f.talk("npc.hetty", 1, 1)
        assertEquals(1, f.player.vars["varp.hetty"])
    }

    @Test fun `declining and the witch topic leave the quest unstarted`() {
        val f = fixture()
        f.talk("npc.hetty", 1, 2)
        f.talk("npc.hetty", 2, 2)
        assertEquals(0, f.player.vars["varp.hetty"])
    }

    @Test fun `some ingredients get the matching line and nothing is taken`() {
        val f = fixture()
        f.talk("npc.hetty", 1, 1)
        f.give("obj.rats_tail")
        f.give("obj.eye_of_newt")
        f.talk("npc.hetty")
        assertTrue(
            f.output().contains(
                "I have the rat's tail (ewww), I don't have any burnt meat, I don't have an " +
                    "onion, and I have the eye of newt, yum!"
            ),
            f.output(),
        )
        assertEquals(1, f.player.vars["varp.hetty"])
        assertEquals(1, f.count("obj.rats_tail"))
        assertEquals(1, f.count("obj.eye_of_newt"))
    }

    @Test fun `the hand-in takes one of each and sets 2`() {
        val f = fixture()
        f.talk("npc.hetty", 1, 1)
        ingredients.forEach { f.give(it) }
        f.give("obj.onion")
        f.talk("npc.hetty")
        assertEquals(2, f.player.vars["varp.hetty"])
        assertEquals(listOf(0, 0, 1, 0), ingredients.map { f.count(it) })
        f.talk("npc.hetty")
        assertEquals(2, f.player.vars["varp.hetty"])
        assertEquals(1, f.count("obj.onion"))
    }

    @Test fun `the ingredient line follows the transcript wording`() {
        assertEquals(
            "I don't have a rat's tail, I have the burnt meat, I have an onion, and I don't " +
                "have an eye of newt.",
            WitchsPotion.ingredientsLine(tail = false, meat = true, onion = true, newt = false),
        )
    }

    companion object {
        @JvmStatic @BeforeAll fun cache() = QuestTalkFixture.setUpCache()

        @JvmStatic @AfterAll fun restore() = QuestTalkFixture.restore()
    }
}
