package org.rsmod.content.skills.construction

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The portal nexus, which goes anywhere a portal goes without being built for one destination.
 *
 * ponytail: `interface.telenexus_teleport` is the real destination list, and like the add-room
 * window its rows are filled by the client from varcs the server does not set yet, so a chat menu
 * stands in. Both of the nexus' teleport options open it; telling them apart needs the per-player
 * favourite that Configuration sets, which is not modelled.
 */
class PohNexus @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        for (locId in nexusLocs()) {
            val type = ServerCacheManager.getObject(locId) ?: continue
            onOpLoc1(type) { openNexus() }
            onOpLoc2(type) { openNexus() }
        }
    }

    private suspend fun ProtectedAccess.openNexus() {
        val destinations = PohPortals.labels()
        val choice = menu("Portal Nexus", hotkeys = false, choices = destinations + "Cancel")
        val destination = PohPortals.destinationAt(choice) ?: return
        val coord = PohPortals.destinationCoord(destination) ?: return
        telejump(coord)
        spam("You step through the nexus.")
    }

    private fun nexusLocs(): List<Int> =
        NEXUS_LOCS.mapNotNull { name ->
            val id = runCatching { RSCM.getRSCM(name) }.getOrDefault(-1)
            id.takeIf { it > 0 }
        }

    private companion object {
        val NEXUS_LOCS: List<String> =
            listOf(
                "loc.poh_nexus_portal_1",
                "loc.poh_nexus_portal_2",
                "loc.poh_nexus_portal_3",
                "loc.poh_nexus_portal_league_5",
            )
    }
}
