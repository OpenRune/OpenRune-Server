package org.rsmod.content.raids.toa.raid.encounter.apmeken.puzzle

import dev.or2.central.account.Rights
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onCommand
import org.rsmod.content.raids.toa.raid.encounter.ToaRooms
import org.rsmod.game.cheat.Cheat
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ApmekenCheatScript : PluginScript() {
    override fun ScriptContext.startup() {
        onCommand("apmeken") {
            requiredRights = Rights.ADMINISTRATOR
            desc = "Trigger a Path of Apmeken mechanic"
            invalidArgs = USAGE
            cheat { trigger(args[0].lowercase(), args.getOrNull(1)?.toIntOrNull()) }
        }
    }

    private fun Cheat.trigger(mechanic: String, arg: Int?) {
        val room = ToaRooms.of<ApmekenPuzzleEncounter>(player)
        if (room == null) {
            player.mes("You are not in the Path of Apmeken room.")
            return
        }
        when (mechanic) {
            "wave" -> room.baboons.skipTo(arg ?: 1)
            "kill" -> room.baboons.killAll()
            "roof" -> room.sight.forceIssue(roof = true)
            "vents" -> room.sight.forceIssue(roof = false)
            "sight" -> room.sight.grantTo(player)
            else -> return player.mes(USAGE)
        }
    }

    private companion object {
        const val USAGE = "Usage: ::apmeken <wave [n]|kill|roof|vents|sight>"
    }
}
