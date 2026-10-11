package org.rsmod.content.raids.toa.raid.encounter.het.akkha

import dev.or2.central.account.Rights
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onCommand
import org.rsmod.content.raids.toa.raid.encounter.ToaRooms
import org.rsmod.game.cheat.Cheat
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class AkkhaCheatScript : PluginScript() {
    override fun ScriptContext.startup() {
        onCommand("akkha") {
            requiredRights = Rights.ADMINISTRATOR
            desc = "Trigger an Akkha mechanic"
            invalidArgs = USAGE
            cheat { trigger(args[0].lowercase(), args.getOrNull(1)?.uppercase()) }
        }
    }

    private fun Cheat.trigger(mechanic: String, arg: String?) {
        val room = ToaRooms.of<AkkhaEncounter>(player)
        if (room == null) {
            player.mes("You are not in Akkha's room.")
            return
        }
        val quadrant = arg?.let { name -> Quadrant.entries.firstOrNull { it.name == name } }
        when (mechanic) {
            "blast",
            "slam" -> {
                if (quadrant == null) return player.mes(USAGE)
                room.debugBlast(quadrant, slam = mechanic == "slam")
            }
            "tiles" -> player.mes(room.quadrants.tileCounts.toString())
            "memory" -> room.debugMemory()?.let { return player.mes(it) }
            "final" -> room.debugFinalStand()?.let { return player.mes(it) }
            "trail" -> room.debugSpecial(AkkhaEncounter.Special.TRAIL)?.let { return player.mes(it) }
            "detonate" ->
                room.debugSpecial(AkkhaEncounter.Special.DETONATE)?.let { return player.mes(it) }
            else -> return player.mes(USAGE)
        }
        player.mes("Akkha: $mechanic.")
    }

    private companion object {
        const val USAGE = "Usage: ::akkha <blast|slam> <ne|se|sw|nw> | ::akkha <tiles|memory|trail|detonate|final>"
    }
}
