package org.rsmod.content.raids.toa.raid.encounter.het.puzzle

import dev.or2.central.account.Rights
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onCommand
import org.rsmod.content.raids.toa.raid.encounter.ToaRooms
import org.rsmod.game.cheat.Cheat
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class HetCheatScript : PluginScript() {
    override fun ScriptContext.startup() {
        onCommand("het") {
            requiredRights = Rights.ADMINISTRATOR
            desc = "Trigger a Path of Het mechanic"
            invalidArgs = USAGE
            cheat { trigger(args[0].lowercase()) }
        }
    }

    private fun Cheat.trigger(mechanic: String) {
        val room = ToaRooms.of<HetPuzzleEncounter>(player)
        if (room == null) {
            player.mes("You are not in the Path of Het room.")
            return
        }
        when (mechanic) {
            "beam" -> room.debugBeam(room.players)
            "weaken" -> room.debugWeaken()
            "redraft" -> room.debugRedraft()
            else -> return player.mes(USAGE)
        }
        player.mes("Het: $mechanic.")
    }

    private companion object {
        const val USAGE = "Usage: ::het <beam|weaken|redraft>"
    }
}
