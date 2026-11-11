package org.rsmod.content.travel.jewellery

import jakarta.inject.Inject
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpWorn3
import org.rsmod.api.script.onOpWorn4
import org.rsmod.api.table.SkillcapeTeleportDestinationsRow
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class SkillcapeTeleportScript @Inject constructor(private val caster: ItemTeleportCaster) :
    PluginScript() {
    private val destinations: Map<String, List<SkillcapeTeleportDestinationsRow>> =
        SkillcapeTeleportDestinationsRow.all().sortedBy { it.rowId }.groupBy { it.cape }

    override fun ScriptContext.startup() {
        for ((cape, entry) in CAPES) {
            val (stat, items) = entry
            val options = destinations.getValue(cape)
            for (item in items) {
                onOpHeld3(item) { teleport(stat, options, destination = 0) }
                onOpWorn3(item) { teleport(stat, options, destination = 0) }
                if (options.size > 1) {
                    onOpHeld4(item) { teleport(stat, options, destination = 1) }
                    onOpWorn4(item) { teleport(stat, options, destination = 1) }
                }
            }
        }
    }

    private suspend fun ProtectedAccess.teleport(
        stat: String,
        options: List<SkillcapeTeleportDestinationsRow>,
        destination: Int,
    ) {
        if (actionDelay > mapClock) {
            return
        }
        if (statBase(stat) < MAX_LEVEL) {
            mes("You need to have a level of $MAX_LEVEL in that skill.")
            return
        }
        with(caster) { cast(TeleportType.Standard, options[destination].coords) }
    }

    private companion object {
        private const val MAX_LEVEL = 99

        private val CAPES =
            mapOf(
                "strength" to ("stat.strength" to listOf("obj.skillcape_strength", "obj.skillcape_strength_trimmed")),
                "farming" to ("stat.farming" to listOf("obj.skillcape_farming", "obj.skillcape_farming_trimmed")),
                "fishing" to ("stat.fishing" to listOf("obj.skillcape_fishing", "obj.skillcape_fishing_trimmed")),
            )
    }
}
