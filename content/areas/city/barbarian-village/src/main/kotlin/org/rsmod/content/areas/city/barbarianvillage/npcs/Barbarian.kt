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

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class Barbarian @Inject constructor(private val aiPlayerInteractions: AiPlayerInteractions) :
    PluginScript() {

    override fun ScriptContext.startup() {
        for (barbarian in BARBARIANS) {
            onOpNpc1(barbarian) { talk(it.npc) }
        }
        onOpNpc1("npc.gunthor_the_brave") { gunthor(it.npc) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) = startDialogue(npc) { taunt(npc) }

    // https://oldschool.runescape.wiki/w/Transcript:Barbarian
    private suspend fun Dialogue.taunt(npc: Npc) {
        chatNpc(angry, TAUNTS[access.random.of(TAUNTS.size)])
        npc.opPlayer2(player, aiPlayerInteractions)
    }

    // https://oldschool.runescape.wiki/w/Transcript:Gunthor_the_brave
    private suspend fun ProtectedAccess.gunthor(npc: Npc) =
        startDialogue(npc) {
            when (access.random.of(GUNTHOR_LINES)) {
                0 -> chatNpc(angry, "Ah, you've come for fight!")
                1 -> {
                    chatNpc(angry, "What do you want?")
                    chatPlayer(quiz, "What are you offering?")
                    chatNpc(angry, "A fight!")
                }
                2 -> chatNpc(laugh, "You look funny!")
                3 -> chatNpc(angry, "Wanna fight?")
                4 -> chatNpc(angry, "Grrr!")
                else -> chatNpc(angry, "Go away!")
            }
            npc.opPlayer2(player, aiPlayerInteractions)
        }

    private companion object {
        const val GUNTHOR_LINES = 6

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
