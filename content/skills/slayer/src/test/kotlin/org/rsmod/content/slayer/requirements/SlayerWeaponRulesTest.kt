package org.rsmod.content.slayer.requirements

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.game.hit.HitType

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class SlayerWeaponRulesTest {
    private val rule = SlayerMonsters.find("npc.slayer_kursk_1".asRSCM(RSCMType.NPC))!!.weaponRule!!

    private fun obj(symbol: String): Int = symbol.asRSCM(RSCMType.OBJ)

    private fun melee(weapon: String) =
        SlayerWeaponRules.damages(rule, HitType.Melee, obj(weapon), null)

    private fun ranged(ammo: String) =
        SlayerWeaponRules.damages(rule, HitType.Ranged, obj("obj.xbows_crossbow_runite"), obj(ammo))

    private fun magic(spell: String) =
        SlayerWeaponRules.damages(rule, HitType.Magic, obj("obj.slayer_staff"), obj(spell))

    @Test
    fun `only leaf-bladed weapons hurt in melee`() {
        assertTrue(melee("obj.slayer_leafbladed_spear"))
        assertTrue(melee("obj.leafbladed_battleaxe"))
        assertFalse(melee("obj.rune_scimitar"))
    }

    @Test
    fun `only broad ammunition hurts at range`() {
        assertTrue(ranged("obj.slayer_broad_bolt"))
        assertTrue(ranged("obj.slayer_broad_arrows"))
        assertFalse(ranged("obj.xbows_crossbow_bolts_runite"))
    }

    @Test
    fun `only magic dart hurts with magic`() {
        assertTrue(magic("obj.50_magic_dart"))
        assertFalse(magic("obj.13_fire_strike"))
    }

    @Test
    fun `typeless damage is not weapon damage`() {
        assertTrue(SlayerWeaponRules.damages(rule, HitType.Typeless, null, null))
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(240).close()
        }
    }
}
