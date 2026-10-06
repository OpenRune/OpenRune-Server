package org.rsmod.content.areas.misc.stronghold_of_security

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.table.StrongholdFloorsRow
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class StrongholdTravel : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1("loc.sos_dung_ent_open") { climb(WarArrival) }
        onOpLoc1("loc.sos_war_ladd_up") { climb(Surface) }
        onOpLoc1("loc.sos_war_ladd_down") { climb(FamineArrival) }
        onOpLoc1("loc.sos_fam_ladd_up") { climb(WarArrival) }
        onOpLoc1("loc.sos_fam_ladd_down") { climb(PestilenceArrival) }
        onOpLoc1("loc.sos_pest_ladd_up") { climb(FamineArrival) }
        onOpLoc1("loc.sos_pest_ladd_down") { climb(DeathArrival) }
        onOpLoc1("loc.sos_death_ladd_up") { climb(PestilenceArrival) }

        onOpLoc1("loc.sos_war_chainbottom") { climb(StrongholdFloors.war.start) }
        onOpLoc1("loc.sos_fam_rope_up") { climb(StrongholdFloors.famine.start) }
        onOpLoc1("loc.sos_pest_rope_up") { climb(StrongholdFloors.pestilence.start) }
        onOpLoc1("loc.sos_death_rope_up") { climb(boneChainDestination(it.loc.coords)) }

        for (floor in StrongholdFloors.all) {
            onOpLoc1(floor.portal) { usePortal(floor) }
        }
    }

    private suspend fun ProtectedAccess.climb(dest: CoordGrid) {
        arriveDelay()
        anim("seq.human_reachforladder")
        delay(1)
        telejump(dest)
    }

    private suspend fun ProtectedAccess.usePortal(floor: StrongholdFloorsRow) {
        arriveDelay()
        if (!player.mayUsePortal(floor)) {
            mes(portalRefusal(floor))
            return
        }
        telejump(floor.rewardRoom)
    }

    private fun portalRefusal(floor: StrongholdFloorsRow): String {
        val level = floor.portalCombatLevel
            ?: return "The portal will only carry you to the reward room once you have claimed it."
        return "The portal will only carry you to the reward room if you have claimed it or are " +
            "combat level $level or higher."
    }

    internal companion object {
        val Surface = CoordGrid(3081, 3421, 0)
        val WarArrival = CoordGrid(1860, 5244, 0)
        val FamineArrival = CoordGrid(2042, 5245, 0)
        val PestilenceArrival = CoordGrid(2123, 5252, 0)
        val DeathArrival = CoordGrid(2357, 5216, 0)
        val CradleRoomChain = CoordGrid(2350, 5215, 0)

        fun boneChainDestination(chain: CoordGrid): CoordGrid =
            if (chain == CradleRoomChain) Surface else StrongholdFloors.death.start
    }
}

internal fun Player.mayUsePortal(floor: StrongholdFloorsRow): Boolean {
    if (hasClaimed(floor)) {
        return true
    }
    val required = floor.portalCombatLevel ?: return false
    return combatLevel >= required
}
