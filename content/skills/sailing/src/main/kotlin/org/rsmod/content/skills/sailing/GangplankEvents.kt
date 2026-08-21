package org.rsmod.content.skills.sailing

import jakarta.inject.Inject
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.script.onOpLoc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class GangplankEvents
@Inject
constructor(private val boats: BoatManager) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1("loc.sailing_gangplank_embark") {
            val dock = Docks.nearest(it.loc.coords)
            if (dock == null) {
                mes("Nothing interesting happens.")
                return@onOpLoc1
            }
            val boat = boats.mooredAt(dock) ?: boats.spawnAtDock(BoatTypes.RAFT, dock)
            if (boat == null) {
                mes("Your boat cannot moor here right now.")
                return@onOpLoc1
            }
            player.aboardPlayerBoat = 1
            telejump(boat.boardDest, TeleportType.Exempt)
            mes("You board your boat.")
        }
        onOpLoc1("loc.sailing_gangplank_disembark") {
            val dock = Docks.nearest(it.loc.coords)
            val dest = dock?.returnTile ?: player.lastKnownNormalCoord
            boats.releaseHelm(player)
            player.aboardPlayerBoat = 0
            telejump(dest, TeleportType.Exempt)
            if (dock != null) {
                mes("You disembark at ${dock.name}.")
            } else {
                mes("You disembark.")
            }
        }
    }
}
