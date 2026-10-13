package org.rsmod.content.raids.toa.raid.encounter.scabaras.puzzle

import dev.or2.central.account.Rights
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onCommand
import org.rsmod.content.raids.toa.raid.encounter.ToaRooms
import org.rsmod.game.cheat.Cheat
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ScabarasCheatScript : PluginScript() {
    override fun ScriptContext.startup() {
        onCommand("scabaras") {
            requiredRights = Rights.ADMINISTRATOR
            desc = "Trigger a Path of Scabaras mechanic"
            invalidArgs = USAGE
            cheat { trigger(args[0].lowercase(), args.getOrNull(1)?.toIntOrNull()) }
        }
    }

    private fun Cheat.trigger(mechanic: String, arg: Int?) {
        val room = ToaRooms.of<ScabarasPuzzleEncounter>(player)
        if (room == null) {
            player.mes("You are not in the Path of Scabaras room.")
            return
        }
        when (mechanic) {
            "layout" -> player.mes("Layout: ${room.layoutText()}")
            "slot" -> room.debugCompleteSlot((arg ?: 0).coerceIn(0, ScabarasCoords.SLOT_COUNT - 1))
            "final" -> room.completeFinal()
            "goal" -> room.sum.debugSetGoal(arg ?: DEFAULT_GOAL)
            "order" -> player.mes("Pillar order: ${room.pillars.debugOrder()}")
            else -> return player.mes(USAGE)
        }
    }

    private companion object {
        const val DEFAULT_GOAL = 30
        const val USAGE = "Usage: ::scabaras <layout|slot [0-3]|final|goal [n]|order>"
    }
}
