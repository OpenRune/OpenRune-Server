package org.rsmod.content.slayer.requirements

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.combat.commons.npc.NpcAttackOverride
import org.rsmod.api.table.slayer.SlayerGearRow
import org.rsmod.game.inv.InvObj

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class SlayerGearTest {
    private fun gear(symbol: String): SlayerGearRow =
        SlayerMonsters.find(symbol.asRSCM(RSCMType.NPC))!!.gear!!

    private fun worn(vararg items: String): Array<InvObj?> =
        arrayOfNulls<InvObj>(WORN_SLOTS).also { slots ->
            items.forEachIndexed { index, item -> slots[index] = InvObj(item) }
        }

    private fun drains(gear: SlayerGearRow, level: Int): Map<String, Int> =
        SlayerGear.drainAmounts(gear) { level }.toMap()

    @Test
    fun `every slayer helmet colour protects like the single pieces`() {
        val nosePeg = gear("npc.slayer_abberant_spectre_1")
        val earmuffs = gear("npc.slayer_banshee_1")

        assertTrue(SlayerGear.isProtected(worn("obj.slayer_nosepeg"), nosePeg))
        assertTrue(SlayerGear.isProtected(worn("obj.slayer_helm_i_black"), nosePeg))
        assertTrue(SlayerGear.isProtected(worn("obj.slayer_helm"), earmuffs))
        assertFalse(SlayerGear.isProtected(worn("obj.slayer_earmuffs"), nosePeg))
        assertFalse(SlayerGear.isProtected(worn(), nosePeg))
    }

    @Test
    fun `a v shield protects like a mirror shield`() {
        val cockatrice = gear("npc.slayer_cockatrice")

        assertTrue(SlayerGear.isProtected(worn("obj.slayer_mirror_shield"), cockatrice))
        assertTrue(SlayerGear.isProtected(worn("obj.v_shield"), cockatrice))
        assertFalse(SlayerGear.isProtected(worn("obj.slayer_helm"), cockatrice))
    }

    @Test
    fun `an unprotected spectre hit always lands through prayer and protection ends it`() {
        val nosePeg = gear("npc.slayer_abberant_spectre_1")

        assertEquals(
            NpcAttackOverride.Fixed(damage = 14, penetration = 100),
            SlayerGear.attack(nosePeg, protected = false, engagementStart = false, 99),
        )
        assertNull(SlayerGear.attack(nosePeg, protected = true, engagementStart = false, 99))
    }

    @Test
    fun `unprotected banshee and dust devil hits do fixed damage`() {
        val banshee = gear("npc.slayer_banshee_1")
        val dustDevil = gear("npc.slayer_dustdevil")

        assertEquals(
            NpcAttackOverride.Fixed(6),
            SlayerGear.attack(banshee, protected = false, engagementStart = false, 99),
        )
        assertEquals(
            NpcAttackOverride.Fixed(16),
            SlayerGear.attack(dustDevil, protected = false, engagementStart = false, 99),
        )
    }

    @Test
    fun `a superior only drains and keeps its own attack`() {
        val choke = gear("npc.superior_dustdevil")

        assertNull(SlayerGear.attack(choke, protected = false, engagementStart = false, 99))
        assertEquals(99, drains(choke, 99).getValue("stat.attack"))
    }

    @Test
    fun `drains take a share of the current level rounded down`() {
        assertEquals(79, drains(gear("npc.slayer_abberant_spectre_1"), 99).getValue("stat.attack"))
        assertEquals(39, drains(gear("npc.slayer_abberant_spectre_1"), 99).getValue("stat.defence"))
        assertEquals(19, drains(gear("npc.slayer_banshee_1"), 99).getValue("stat.attack"))
        assertEquals(84, drains(gear("npc.smoke_devil"), 99).getValue("stat.magic"))
        assertEquals(24, drains(gear("npc.basilisk_knight"), 35).getValue("stat.attack"))
    }

    @Test
    fun `the cave horror scream is a tenth of base hitpoints without an icon`() {
        val horror = gear("npc.harmless_island_cave_horror_worker")

        assertEquals(
            NpcAttackOverride.Fixed(9),
            SlayerGear.attack(horror, protected = false, engagementStart = false, 96),
        )
        assertEquals(
            NpcAttackOverride.Fixed(9),
            SlayerGear.attack(horror, protected = false, engagementStart = false, 99),
        )
        assertNull(SlayerGear.attack(horror, protected = true, engagementStart = false, 99))
    }

    @Test
    fun `killerwatts roll a higher max hit without insulated boots`() {
        val killerwatt = gear("npc.slayer_killerwatt")

        assertEquals(
            NpcAttackOverride.MaxHit(15),
            SlayerGear.attack(killerwatt, protected = false, engagementStart = false, 99),
        )
        assertNull(SlayerGear.attack(killerwatt, protected = true, engagementStart = false, 99))
    }

    @Test
    fun `the wall beast only grabs when it starts attacking`() {
        val wallBeast = gear("npc.swamp_wallbeast_combat")

        assertEquals(
            NpcAttackOverride.MaxHit(18, guaranteed = true),
            SlayerGear.attack(wallBeast, protected = false, engagementStart = true, 99),
        )
        assertEquals(
            NpcAttackOverride.Fixed(0),
            SlayerGear.attack(wallBeast, protected = true, engagementStart = true, 99),
        )
        assertNull(SlayerGear.attack(wallBeast, protected = false, engagementStart = false, 99))
    }

    @Test
    fun `a new target or a long pause starts a new engagement`() {
        val reset = SlayerGear.ENGAGEMENT_RESET_TICKS

        assertTrue(SlayerGear.startsEngagement(1, 100, 2, 101))
        assertTrue(SlayerGear.startsEngagement(1, 100, 1, 101 + reset))
        assertFalse(SlayerGear.startsEngagement(1, 100, 1, 100 + reset))
    }

    companion object {
        private const val WORN_SLOTS = 14

        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(240).close()
        }
    }
}
