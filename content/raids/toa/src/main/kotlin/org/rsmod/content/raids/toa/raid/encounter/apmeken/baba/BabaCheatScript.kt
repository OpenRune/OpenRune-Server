package org.rsmod.content.raids.toa.raid.encounter.apmeken.baba

import dev.or2.central.account.Rights
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onCommand
import org.rsmod.content.raids.toa.raid.encounter.ToaRooms
import org.rsmod.game.cheat.Cheat
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class BabaCheatScript : PluginScript() {
    override fun ScriptContext.startup() {
        onCommand("baba") {
            requiredRights = Rights.ADMINISTRATOR
            desc = "Trigger a Ba-Ba mechanic"
            invalidArgs = USAGE
            cheat { trigger(args[0].lowercase()) }
        }
    }

    private fun Cheat.trigger(mechanic: String) {
        val room = ToaRooms.of<BabaEncounter>(player)
        if (room == null) {
            player.mes("You are not in Ba-Ba's room.")
            return
        }
        val error =
            when (mechanic) {
                "roll" -> room.debugRoll()
                "rockfall" -> room.debugRockfall()
                "shockwave" -> room.debugShockwave(false)
                "stomp" -> room.debugShockwave(true)
                "discharge" -> room.debugDischarge()
                else -> "Unknown mechanic."
            }
        player.mes(error ?: "Ba-Ba: $mechanic.")
    }

    private companion object {
        const val USAGE = "Usage: ::baba <roll|rockfall|shockwave|stomp|discharge>"
    }
}
