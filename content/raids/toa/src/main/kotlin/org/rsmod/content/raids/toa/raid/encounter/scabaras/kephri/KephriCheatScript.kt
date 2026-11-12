package org.rsmod.content.raids.toa.raid.encounter.scabaras.kephri

import dev.or2.central.account.Rights
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onCommand
import org.rsmod.content.raids.toa.raid.encounter.ToaRooms
import org.rsmod.game.cheat.Cheat
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class KephriCheatScript : PluginScript() {
    override fun ScriptContext.startup() {
        onCommand("kephri") {
            requiredRights = Rights.ADMINISTRATOR
            desc = "Trigger a Kephri mechanic"
            invalidArgs = USAGE
            cheat { trigger(args[0].lowercase(), args.getOrNull(1)?.toIntOrNull()) }
        }
    }

    private fun Cheat.trigger(mechanic: String, arg: Int?) {
        val room = ToaRooms.of<KephriEncounter>(player)
        if (room == null) {
            player.mes("You are not in Kephri's room.")
            return
        }
        val error =
            when (mechanic) {
                "break" -> room.debugBreak()
                "shield" -> room.debugShield(arg ?: FULL_SHIELD)
                else -> room.debugMechanic(mechanic, player)
            }
        player.mes(error ?: "Kephri: $mechanic.")
    }

    private companion object {
        const val FULL_SHIELD = 100
        const val USAGE =
            "Usage: ::kephri <break|final|shield [percent]|dung|eggs|bomb|swarm|agile|spitting|soldier|arcane>"
    }
}
