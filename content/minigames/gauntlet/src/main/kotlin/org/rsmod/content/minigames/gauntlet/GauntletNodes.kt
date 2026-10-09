package org.rsmod.content.minigames.gauntlet

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.api.script.onOpLoc1
import org.rsmod.content.minigames.gauntlet.layout.RoomKind
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class GauntletNodes
@Inject
constructor(
    private val runs: GauntletRuns,
    private val lighting: GauntletLighting,
    private val contents: GauntletContents,
    private val regions: RegionRegistry,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (suffix in listOf("", "_hm")) {
            for (node in listOf("01", "02")) {
                onOpLoc1("loc.prif_gauntlet_door_wall_unlit_$node$suffix") {
                    lightNode(it.loc.coords)
                }
            }
        }
    }

    private suspend fun ProtectedAccess.lightNode(coords: CoordGrid) {
        val run = runs.runFor(player) ?: return
        val sceptre = gauntletObj("sceptre", run.mode.corrupted)
        if (sceptre !in inv && sceptre !in player.worn) {
            mesbox("You need something to light the nodes with.")
            return
        }
        val region = regions[coords] ?: return
        val from = lighting.roomAt(region, run, coords) ?: return
        val target = lighting.neighbour(run, from, lighting.edgeOf(region, coords)) ?: return
        if (target.index in run.revealed || target.kind == RoomKind.BOSS) return
        with(lighting) { light(coords, run, from, target) }
        contents.spawn(region, run, target, player)
        spam("You light the nodes in the corridor to help guide the way.")
    }
}
