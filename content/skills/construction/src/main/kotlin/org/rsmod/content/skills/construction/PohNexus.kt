package org.rsmod.content.skills.construction

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The portal nexus, which goes anywhere it has been configured for.
 *
 * Its configured destinations live in `poh_nexus_tele_1..` as the destination's nexus id, the same
 * varbits the client's own nexus interface reads; a tier holds as many as `telenexus_slotted_init`
 * lays out for it.
 *
 * ponytail: `interface.telenexus_teleport` is the real destination list, and like the add-room
 * window its rows are filled by the client from varcs the server does not set yet, so a chat menu
 * stands in for it and for the configuration screen. Removing a destination is not offered.
 */
class PohNexus @Inject constructor(private val portals: PohPortals) : PluginScript() {
    override fun ScriptContext.startup() {
        for ((name, capacity) in NEXUS_CAPACITY) {
            val locId = RSCM.getRSCMOrNull(name, RSCMType.LOC) ?: continue
            val type = ServerCacheManager.getObject(locId) ?: continue
            onOpLoc1(type) { teleport() }
            onOpLoc2(type) { teleport() }
            onOpLoc3(type) { configure(capacity) }
        }
    }

    private fun ProtectedAccess.configured(): List<PortalDestination> =
        (1..MAX_SLOTS).mapNotNull { slot ->
            vars[slotVarbit(slot)].takeIf { it > 0 }?.let(portals::byNexusId)
        }

    private suspend fun ProtectedAccess.teleport() {
        val destinations = configured()
        if (destinations.isEmpty()) {
            mes("You haven't added any teleports to the portal nexus yet.")
            return
        }
        val labels = destinations.map { it.name } + "Cancel"
        val destination = destinations.getOrNull(menu("Portal Nexus", hotkeys = false, choices = labels))
        val coord = destination?.coord ?: return
        leaveHouse(coord)
        spam("You step through the nexus.")
    }

    private suspend fun ProtectedAccess.configure(capacity: Int) {
        val known = configured()
        if (known.size >= capacity) {
            mes("Your portal nexus has no free slots. Upgrade it to add more teleports.")
            return
        }
        val options = portals.destinations().filter { option -> known.none { it.nexusId == option.nexusId } }
        if (options.isEmpty()) {
            mes("Your portal nexus already has every teleport it can hold.")
            return
        }
        val labels = options.map { it.name } + "Cancel"
        val chosen = options.getOrNull(menu("Add a teleport", hotkeys = false, choices = labels)) ?: return

        portals.refusal(player, chosen, boostable = false)?.let {
            mes(it)
            return
        }
        val slot = (1..capacity).firstOrNull { vars[slotVarbit(it)] == 0 } ?: return
        val cost = portals.runeCost(chosen, PohPortals.NEXUS_RUNE_MULTIPLIER)
        if (!player.spend(cost)) {
            mes("You do not have all the required runes in your inventory.")
            return
        }
        vars[slotVarbit(slot)] = chosen.nexusId
        mes("You add ${chosen.name} to your portal nexus.")
    }

    private fun slotVarbit(slot: Int): String = "varbit.poh_nexus_tele_$slot"

    private companion object {
        const val MAX_SLOTS = 41

        val NEXUS_CAPACITY: List<Pair<String, Int>> =
            listOf(
                "loc.poh_nexus_portal_1" to 4,
                "loc.poh_nexus_portal_2" to 8,
                "loc.poh_nexus_portal_3" to 41,
                "loc.poh_nexus_portal_league_5" to 41,
            )
    }
}
