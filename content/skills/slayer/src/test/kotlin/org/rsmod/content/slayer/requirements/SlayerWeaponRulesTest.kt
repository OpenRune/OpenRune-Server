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

    @Test
    fun `only leaf-bladed weapons hurt in melee`() {
        assertTrue(melee("obj.slayer_leafbladed_spear"))
        assertTrue(melee("obj.leafbladed_battleaxe"))
        assertFalse(melee("obj.rune_scimitar"))
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(240).close()
        }
    }
}
