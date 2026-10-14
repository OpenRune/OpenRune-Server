package org.rsmod.content.raids.toa.raid.encounter.wardens

import dev.or2.central.account.Rights
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onCommand
import org.rsmod.content.raids.toa.raid.encounter.ToaRooms
import org.rsmod.game.cheat.Cheat
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class WardensCheatScript : PluginScript() {
    override fun ScriptContext.startup() {
        onCommand("wardens") {
            requiredRights = Rights.ADMINISTRATOR
            desc = "Trigger a Wardens mechanic"
            invalidArgs = USAGE
            cheat { trigger(args[0].lowercase(), args.getOrNull(1)) }
        }
    }

    private fun Cheat.trigger(mechanic: String, arg: String?) {
        val room = ToaRooms.of<WardensEncounter>(player)
        if (room == null) {
            player.mes("You are not in the Wardens room.")
            return
        }
        val error =
            when (mechanic) {
                "divine" ->
                    room.debugRun(
                        player,
                        when (arg?.lowercase()) {
                            "melee" -> WardenAbilities.DIVINE_MELEE
                            "ranged" -> WardenAbilities.DIVINE_RANGED
                            else -> WardenAbilities.DIVINE_MAGIC
                        },
                    )
                "entomb" -> room.debugRun(player, WardenAbilities.ENTOMB)
                "special" -> room.debugRun(player, WardenAbilities.SPECIAL)
                "hp" -> {
                    val percent = arg?.toIntOrNull() ?: return player.mes(USAGE)
                    room.realHpFraction = percent.coerceIn(0, 100) / PERCENT
                    null
                }
                else -> return player.mes(USAGE)
            }
        player.mes(error ?: "Wardens: $mechanic.")
    }

    private companion object {
        const val PERCENT = 100.0
        const val USAGE =
            "Usage: ::wardens divine [melee|ranged|magic] | entomb | special | hp <percent>"
    }
}
