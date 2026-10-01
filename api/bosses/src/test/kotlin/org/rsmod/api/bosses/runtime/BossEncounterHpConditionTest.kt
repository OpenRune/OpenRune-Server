package org.rsmod.api.bosses.runtime

import dev.openrune.types.NpcServerType
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.bosses.dsl.resetAnim
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.BossStats
import org.rsmod.api.bosses.spec.Condition
import org.rsmod.api.bosses.spec.PhaseSpec
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.map.CoordGrid

class BossEncounterHpConditionTest {
    @Test
    fun `inclusive HpBelow includes the threshold, exclusive does not`() {
        val encounter = encounter(hp = 750, max = 1000)
        assertTrue(encounter.evaluate(Condition.HpBelow(0.75, inclusive = true)))
        assertFalse(encounter.evaluate(Condition.HpBelow(0.75)))
    }

    @Test
    fun `inclusive HpBelow is false above the threshold`() {
        assertFalse(encounter(hp = 751, max = 1000).evaluate(Condition.HpBelow(0.75, inclusive = true)))
    }

    @Test
    fun `inclusive and exclusive both hold below the threshold`() {
        val encounter = encounter(hp = 749, max = 1000)
        assertTrue(encounter.evaluate(Condition.HpBelow(0.75, inclusive = true)))
        assertTrue(encounter.evaluate(Condition.HpBelow(0.75)))
    }

    private fun encounter(hp: Int, max: Int): BossEncounter {
        val type = NpcServerType(id = 1, name = "Boss", size = 1, hitpoints = max)
        val npc = Npc(type, CoordGrid(0, 1, 1, 0, 0)).apply { hitpoints = hp }
        val spec =
            BossSpec(
                npcTypes = listOf("npc.boss"),
                stats = BossStats(),
                abilities = mapOf("a" to resetAnim()),
                phases = mapOf("main" to PhaseSpec("main")),
                triggers = emptyList(),
            )
        return BossEncounter(npc, spec, MapClock())
    }
}
