package org.rsmod.content.areas.wilderness.locs

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.script.onOpLoc1
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The upper floor of the Chaos Temple holds the Wine of Zamorak spawns, so the monks only let a
 * well travelled Zamorakian up the ladder. Coming back down is unrestricted.
 */
class ChaosTempleLadder : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1("loc.chaos_temple_ladder") { climbUp() }
        onOpLoc1("loc.chaos_temple_laddertop") { climb(-1) }
    }

    private suspend fun ProtectedAccess.climbUp() {
        arriveDelay()
        if (player.skillTotal() < REQUIRED_TOTAL || !wearsZamorakRobes()) {
            startDialogue {
                chatNpcSpecific(
                    "Monk of Zamorak",
                    MONK,
                    angry,
                    "You better dress appropriately, if you want to go up there!",
                )
            }
            mes(
                "You need a skill total of $REQUIRED_TOTAL and a set of Zamorak robes equipped " +
                    "to go upstairs.",
            )
            return
        }
        climb(1)
    }

    private suspend fun ProtectedAccess.climb(levels: Int) {
        arriveDelay()
        anim("seq.human_reachforladder")
        delay(1)
        telejump(coords.translateLevel(levels))
    }

    private fun ProtectedAccess.wearsZamorakRobes(): Boolean =
        ROBE_TOPS.any { worn.count(it) > 0 } && ROBE_BOTTOMS.any { worn.count(it) > 0 }

    private fun Player.skillTotal(): Int =
        ServerCacheManager.getStats().values
            .map { RSCM.getReverseMapping(RSCMType.STAT, it.id) }
            .filter { it !in UNTRAINABLE }
            .sumOf { statBase(it) }

    private companion object {
        private const val REQUIRED_TOTAL = 500
        private const val MONK = "npc.chaos_temple_monk1"
        private val ROBE_TOPS = listOf("obj.zamrobetop", "obj.trail_zamorak_robe_t")
        private val ROBE_BOTTOMS = listOf("obj.zamrobebottom", "obj.trail_zamorak_robe_l")

        private val UNTRAINABLE = setOf("stat.sailing", "stat.unreleased")
    }
}
