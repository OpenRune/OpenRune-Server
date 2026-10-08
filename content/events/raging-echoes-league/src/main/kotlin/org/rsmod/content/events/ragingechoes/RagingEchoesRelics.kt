package org.rsmod.content.events.ragingechoes

import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.game.world.WorldType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class RagingEchoesRelics @Inject constructor() : PluginScript() {
    override val worldTypes = listOf(WorldType.RAGING_ECHOES_LEAGUE)

    override val worldTypeDenyMessage =
        "The relic keeper looks straight through you. Switch mode with ::worldtype."

    override fun ScriptContext.startup() {
        onPlayerLogin { player.mes("Your relics carry over from your last Echo.") }

        onOpNpc1("npc.league_sage") { player.mes("The relic keeper nods. Relics are active here.") }

        onCommand("echo") {
            desc = "List this world type's relics"
            cheat {
                player.mes("Relics on ${WorldType.RAGING_ECHOES_LEAGUE.label}:")
                player.mes("  Endless Harvest, Trickster.")
            }
        }
    }
}
