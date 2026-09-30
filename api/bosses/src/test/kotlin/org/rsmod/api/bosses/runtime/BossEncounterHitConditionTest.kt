package org.rsmod.api.bosses.runtime

import dev.openrune.types.NpcServerType
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.rsmod.api.bosses.dsl.resetAnim
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.BossStats
import org.rsmod.api.bosses.spec.Condition
import org.rsmod.api.bosses.spec.HitType as BossHitType
import org.rsmod.api.bosses.spec.PhaseSpec
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.hit.HitType
import org.rsmod.map.CoordGrid

class BossEncounterHitConditionTest {
    private val encounter = encounter()
    private val meleeHit = HitContext(HitType.Melee, 30, righthand = null, secondary = null)

    @Test
    fun `hit style compares against the engine style`() {
        assertTrue(encounter.evaluate(Condition.HitStyle(BossHitType.Melee), hit = meleeHit))
        assertFalse(encounter.evaluate(Condition.HitStyle(BossHitType.Ranged), hit = meleeHit))
        val magicHit = HitContext(HitType.Magic, 10, null, null)
        assertTrue(encounter.evaluate(Condition.HitStyle(BossHitType.Dragonfire), hit = magicHit))
    }

    @Test
    fun `hit damage is inclusive`() {
        assertTrue(encounter.evaluate(Condition.HitDamageAtLeast(30), hit = meleeHit))
        assertFalse(encounter.evaluate(Condition.HitDamageAtLeast(31), hit = meleeHit))
    }

    @Test
    fun `a hit without a weapon or spell is not demonbane`() {
        assertFalse(encounter.evaluate(Condition.HitDemonbane, hit = meleeHit))
        val typeless = HitContext(HitType.Typeless, 5, null, null)
        assertFalse(encounter.evaluate(Condition.HitDemonbane, hit = typeless))
    }

    @Test
    fun `hit conditions combine and throw without a hit`() {
        val punish = Condition.And(Condition.Always, Condition.HitStyle(BossHitType.Melee))
        assertTrue(encounter.evaluate(punish, hit = meleeHit))
        assertTrue(encounter.evaluate(Condition.Not(Condition.HitDemonbane), hit = meleeHit))
        assertThrows<IllegalStateException> { encounter.evaluate(punish) }
    }

    private fun encounter(): BossEncounter {
        val type = NpcServerType(id = 1, name = "Boss", size = 1, hitpoints = 100)
        val spec =
            BossSpec(
                npcTypes = listOf("npc.boss"),
                stats = BossStats(),
                abilities = mapOf("a" to resetAnim()),
                phases = mapOf("main" to PhaseSpec("main")),
                triggers = emptyList(),
            )
        return BossEncounter(Npc(type, CoordGrid(0, 1, 1, 0, 0)), spec, MapClock())
    }
}
