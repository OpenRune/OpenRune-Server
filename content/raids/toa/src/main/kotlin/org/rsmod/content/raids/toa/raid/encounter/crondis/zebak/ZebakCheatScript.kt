package org.rsmod.content.raids.toa.raid.encounter.crondis.zebak

import dev.or2.central.account.Rights
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onCommand
import org.rsmod.content.raids.toa.raid.ToaRaidManager.currentRaid
import org.rsmod.game.cheat.Cheat
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * `::zebak <clouds|barrage|roar|waves|enrage>` triggers one of Zebak's mechanics, so each can be
 * tested without waiting for its timer or hp threshold. Blood magic works without Not Just a Head;
 * a special starts on his next attack.
 */
class ZebakCheatScript : PluginScript() {
    override fun ScriptContext.startup() {
        onCommand("zebak") {
            requiredRights = Rights.ADMINISTRATOR
            desc = "Trigger a Zebak mechanic"
            invalidArgs = USAGE
            cheat { trigger(args[0].lowercase()) }
        }
    }

    private fun Cheat.trigger(mechanic: String) {
        val room = player.currentRaid?.encounterOf(player) as? ZebakEncounter
        if (room == null) {
            player.mes("You are not in Zebak's room.")
            return
        }
        val error =
            when (mechanic) {
                "clouds" -> room.debugBloodMagic(barrage = false)
                "barrage" -> room.debugBloodMagic(barrage = true)
                "roar" -> room.debugSpecial(roar = true)
                "waves" -> room.debugSpecial(roar = false)
                "enrage" -> room.debugEnrage()
                else -> USAGE
            }
        player.mes(error ?: "Zebak: $mechanic.")
    }

    private companion object {
        const val USAGE = "Usage: ::zebak <clouds|barrage|roar|waves|enrage>"
    }
}
