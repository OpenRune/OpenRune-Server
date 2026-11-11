package org.rsmod.content.events.leaguetasks.regions

import jakarta.inject.Inject
import org.rsmod.api.player.events.PlayerTeleportValidateEvent
import org.rsmod.api.script.onEvent
import org.rsmod.game.world.WorldType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class LeagueRegionLockScript @Inject constructor(private val regions: LeagueRegions) :
    PluginScript() {
    override val worldTypes = WorldType.entries.filter { it.isLeague }

    override fun ScriptContext.startup() {
        onEvent<PlayerTeleportValidateEvent> {
            if (refusal != null) {
                return@onEvent
            }
            val locked = regions.lockedRegion(player, dest) ?: return@onEvent
            // UNVERIFIED: wording borrowed from the charter refusal.
            refusal = "You haven't unlocked access to the <col=ff0000>${locked.name}</col> area."
        }
    }
}
