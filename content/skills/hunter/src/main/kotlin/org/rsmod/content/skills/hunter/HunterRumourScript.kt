package org.rsmod.content.skills.hunter

import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onOpHeld3
import org.rsmod.game.cheat.Cheat
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class HunterRumourScript @Inject constructor(private val rumours: HunterRumours) : PluginScript() {
    override fun ScriptContext.startup() {
        for (whistle in WHISTLES) {
            onOpHeld3(whistle) { mes(rumours.whistleMessage(player)) }
        }
        onCommand("rumour") {
            desc = "Assign, show or hand in a Hunters' Rumour"
            cheat { rumourCheat() }
        }
    }

    private fun Cheat.rumourCheat() {
        val args = this.args.map { it.lowercase() }
        if (args.firstOrNull() == "turnin") {
            val hunter = hunterArg(args.getOrNull(1)) ?: rumours.currentHunter(player)
            val done = hunter != null && rumours.turnIn(player, hunter)
            player.mes(if (done) "Rumour handed in to ${hunter?.name}." else "Nothing to hand in.")
            return
        }
        val hunter = hunterArg(args.firstOrNull())
        if (hunter == null) {
            player.mes(rumours.whistleMessage(player))
            for (h in rumours.hunters) {
                player.mes("${h.name}: ${rumours.rumourOf(player, h)?.name ?: "-"}")
            }
            return
        }
        val creature = args.drop(1).joinToString(" ").replace('_', ' ').ifEmpty { null }
        val forced = creature?.let { name -> rumours.rumours.firstOrNull { it.name == name } }
        val rumour = rumours.assign(player, hunter, forced)
        player.mes(rumour?.let { "${hunter.name} assigned ${it.name}." } ?: "${hunter.name} has no rumour for you.")
    }

    private fun hunterArg(arg: String?) = rumours.hunters.firstOrNull { it.name.lowercase() == arg }

    private companion object {
        val WHISTLES =
            listOf(
                "obj.hg_quetzalwhistle_basic",
                "obj.hg_quetzalwhistle_enhanced",
                "obj.hg_quetzalwhistle_perfected",
                "obj.hg_quetzalwhistle_perfected_infinite",
            )
    }
}
