package org.rsmod.api.bosses.runtime

import dev.openrune.types.NpcServerType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.rsmod.api.bosses.dsl.boss
import org.rsmod.api.bosses.dsl.resetAnim
import org.rsmod.api.bosses.spec.Selector
import org.rsmod.api.bosses.spec.WeightedRef
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.map.CoordGrid

class BossPersistentCooldownTest {
    @Test fun `persistent prayer disable cooldown prevents another cast after a health phase transition`() {
        val e = encounter()
        val selector = Selector.WeightedRandom(WeightedRef("disable", cooldown = 20, persistCooldown = true))
        assertEquals("disable", e.selectAbility(selector, 10))
        e.transitionTo("last", 11)
        assertNull(e.selectAbility(selector, 29))
        assertEquals("disable", e.selectAbility(selector, 30))
    }

    @Test fun `ordinary cooldowns still reset when the boss changes phase`() {
        val e = encounter()
        val selector = Selector.WeightedRandom(WeightedRef("disable", cooldown = 20))
        assertEquals("disable", e.selectAbility(selector, 10))
        e.transitionTo("last", 11)
        assertEquals("disable", e.selectAbility(selector, 11))
    }
    private fun encounter(): BossEncounter {
        val spec = boss("npc.boss") {
            ability("disable", resetAnim())
            phase("first") {}
            phase("last") {}
        }
        return BossEncounter(Npc(NpcServerType(id = 1, name = "Boss", hitpoints = 100, size = 1), CoordGrid(100, 100, 0)), spec, MapClock())
    }
}
