package org.rsmod.content.minigames.gauntlet

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc5
import org.rsmod.game.entity.Player
import org.rsmod.game.region.util.RegionRotations
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal var Player.gauntletBossStarted by boolVarBit("varbit.gauntlet_boss_started")

@Singleton
class GauntletBossEntry
@Inject
constructor(private val runs: GauntletRuns, private val regions: RegionRegistry) {
    fun ProtectedAccess.begin() {
        val run = runs.runFor(player) ?: return
        if (player.gauntletBossStarted) return
        player.gauntletBossStarted = true
        player.clearSoftTimer(GauntletRuns.TIME_LIMIT_TIMER)
        runClientScript(TIMER_END.asRSCM(RSCMType.CLIENTSCRIPT))
        val region = regions[player.coords] ?: return
        val room = run.layout.bossRoom
        val inside = RegionRotations.translateZone(room.rotation, INSIDE_X, INSIDE_Z, ROOM, ROOM)
        telejump(
            CoordGrid(
                region.southWest.x + room.x * ROOM + inside.x,
                region.southWest.z + room.z * ROOM + inside.z,
                GauntletZones.WALKABLE_LEVEL,
            ),
            TeleportType.Exempt,
        )
    }

    private companion object {
        const val TIMER_END = "clientscript.[clientscript,gauntlet_timer_end]"
        const val ROOM = GauntletLighting.ROOM_TILES
        const val INSIDE_X = 8
        const val INSIDE_Z = 13
    }
}

class GauntletBoss
@Inject
constructor(private val entry: GauntletBossEntry, private val runs: GauntletRuns) :
    PluginScript() {
    override fun ScriptContext.startup() {
        for (suffix in listOf("", "_hm")) {
            val enter = "loc.gauntlet_blockade_enter$suffix"
            onOpLoc1(enter) { confirmEnter() }
            onOpLoc2(enter) { with(entry) { begin() } }
            onOpLoc5("loc.gauntlet_blockade_escape$suffix") { with(runs) { leave() } }
        }
    }

    private suspend fun ProtectedAccess.confirmEnter() {
        val enter =
            choice2(
                "Enter the boss room. You will not be able to return.",
                true,
                "Stay.",
                false,
                title = "Face the Hunllef?",
            )
        if (enter) with(entry) { begin() }
    }
}
