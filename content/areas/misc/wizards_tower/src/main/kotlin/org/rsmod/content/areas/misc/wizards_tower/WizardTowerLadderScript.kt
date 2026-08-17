package org.rsmod.content.areas.misc.wizards_tower

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class WizardTowerLadderScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1("loc.wizards_tower_laddertop") { climbDown() }
        onOpLoc1("loc.wizards_tower_ladder") { climbUp() }
    }

    private suspend fun ProtectedAccess.climbDown() {
        arriveDelay()
        anim("seq.human_pickupfloor")
        delay(1)
        telejump(Basement)
    }

    private suspend fun ProtectedAccess.climbUp() {
        arriveDelay()
        anim("seq.human_reachforladder")
        delay(1)
        telejump(GroundFloor)
    }

    private companion object {
        private val Basement = CoordGrid(3104, 9576)
        private val GroundFloor = CoordGrid(3105, 3162)
    }
}
