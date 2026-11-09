package org.rsmod.content.areas.city.barbarianvillage.npcs

import jakarta.inject.Inject
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class Barbarian @Inject constructor(private val aiPlayerInteractions: AiPlayerInteractions) :
    PluginScript() {

    override fun ScriptContext.startup() {
        for (barbarian in BARBARIANS) {
            onOpNpc1(barbarian) { talk(it.npc) }
        }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) = startDialogue(npc) { taunt(npc) }

    private suspend fun Dialogue.taunt(npc: Npc) {
        chatNpc(angry, TAUNTS[access.random.of(TAUNTS.size)])
        npc.opPlayer2(player, aiPlayerInteractions)
    }

    private companion object {
        val TAUNTS =
            listOf(
                "Wanna fight?",
                "Ah, you come for fight, ja?",
                "You look funny.",
                "Grrr!",
                "What you want?",
                "Go Away!",
            )
    }
}

internal val BARBARIANS =
    (1..14).filter { it != 9 }.map { "npc.fai_barbarian_$it" } +
        (1..4).map { "npc.fai_barbarian_female_$it" }
