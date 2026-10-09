package org.rsmod.content.minigames.gauntlet

import jakarta.inject.Inject
import org.rsmod.api.instances.InstanceEnterTransition
import org.rsmod.api.instances.withInstanceEnterTransition
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class GauntletLobby @Inject constructor(private val runs: GauntletRuns) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1("loc.gauntlet_lobby_entrance") { enterLobby() }
        for (entrance in ENTRANCES) {
            onOpLoc1(entrance) { with(runs) { enter(GauntletMode.NORMAL) } }
            onOpLoc2(entrance) { with(runs) { enter(GauntletMode.CORRUPTED) } }
        }
    }

    private suspend fun ProtectedAccess.enterLobby() {
        withInstanceEnterTransition(InstanceEnterTransition()) {
            telejump(LOBBY, TeleportType.Exempt)
        }
    }

    companion object {
        val PRIF_ENTRANCE = CoordGrid(3229, 6114, 0)
        val LOBBY = CoordGrid(3032, 6127, 1)

        private val ENTRANCES =
            listOf(
                "loc.gauntlet_entrance",
                "loc.gauntlet_entrance_hm_disabled",
                "loc.gauntlet_entrance_hm_enabled",
            )
    }
}
