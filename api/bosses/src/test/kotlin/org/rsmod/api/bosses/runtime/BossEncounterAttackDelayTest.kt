package org.rsmod.api.bosses.runtime

import dev.openrune.types.NpcServerType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.bosses.dsl.boss
import org.rsmod.api.bosses.dsl.resetAnim
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.map.CoordGrid

class BossEncounterAttackDelayTest {
    private val spec =
        boss("npc.boss") {
            stats(attackRate = 5)
            ability("tongue", resetAnim(), attackDelay = 4)
            ability("rock") {
                attackDelay = 8
                resetAnim()
            }
            val standard = ability("standard", resetAnim())
            phase("main") { weightedSelectorRandom { +random(standard, weight = 1) } }
        }

    @Test
    fun `without an attack delay the next attack waits the attack rate`() {
        val encounter = encounter()
        encounter.startAttack("standard", tick = 10)
        assertNull(encounter.nextAttackTick)
        assertFalse(encounter.attackReady(14))
        assertTrue(encounter.attackReady(15))
    }

    @Test
    fun `an ability's attack delay replaces the attack rate for the gap after it`() {
        val encounter = encounter()
        encounter.startAttack("tongue", tick = 10)
        assertTrue(encounter.attackReady(14))
        encounter.startAttack("rock", tick = 14)
        assertFalse(encounter.attackReady(21))
        assertTrue(encounter.attackReady(22))
    }

    @Test
    fun `the delay only covers one gap`() {
        val encounter = encounter()
        encounter.startAttack("rock", tick = 10)
        encounter.startAttack("standard", tick = 18)
        assertNull(encounter.nextAttackTick)
        assertFalse(encounter.attackReady(22))
        assertTrue(encounter.attackReady(23))
    }

    @Test
    fun `nextAttackTick overrides the ability's delay either way`() {
        val encounter = encounter()
        encounter.startAttack("rock", tick = 10)
        encounter.nextAttackTick = 12
        assertTrue(encounter.attackReady(12))
        encounter.nextAttackTick = 30
        assertFalse(encounter.attackReady(29))
        assertEquals(30, encounter.nextAttackTick)
    }

    private fun encounter(): BossEncounter {
        val type = NpcServerType(id = 1, name = "Boss", size = 1, hitpoints = 100)
        return BossEncounter(Npc(type, CoordGrid(0, 1, 1, 0, 0)), spec, MapClock())
    }
}
