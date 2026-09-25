package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class NoonDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(NOON, "Talk-to") { talkNoon(it) }
        onPetOp(MIDNIGHT, "Talk-to") { talkMidnight(it) }
    }

    private suspend fun ProtectedAccess.talkNoon(npc: Npc) =
        startDialogue(npc) {
            when (access.random.of(3)) {
                0 -> {
                    chatPlayer(happy, "Hello little one.")
                    chatNpc(neutral, "I may be small but at least I'm perfectly formed.")
                }
                1 -> {
                    chatPlayer(quiz, "What's your favourite rock?")
                    chatNpc(angry, "You're going tufa with that question. That's personal.")
                    chatPlayer(
                        neutral,
                        "Was just trying to make light conversation, not trying to aggregate you.",
                    )
                }
                else -> {
                    chatPlayer(quiz, "Metaphorically speaking, do you have a heart of stone?")
                    chatNpc(neutral, "Yes, but you're not having it.")
                }
            }
        }

    private suspend fun ProtectedAccess.talkMidnight(npc: Npc) =
        startDialogue(npc) {
            when (access.random.of(3)) {
                0 -> {
                    chatPlayer(happy, "Hello little other one.")
                    chatNpc(quiz, "Other?")
                    chatPlayer(quiz, "Yes, don't you have a sister?")
                    chatNpc(sad, "I don't want to chalk about it.")
                }
                1 -> {
                    chatPlayer(worried, "Sometimes I'm worried you'll attack me whilst my back is turned.")
                    chatNpc(laugh, "Are you petrified of my tuffness?")
                    chatPlayer(bored, "Not really, but your puns are awful.")
                    chatNpc(neutral, "I thought they were clastic.")
                }
                else -> {
                    chatPlayer(sad, "I feel like our relationship is slowly eroding away.")
                    chatNpc(neutral, "Geode willing.")
                }
            }
        }

    private companion object {
        const val NOON = "npc.dawn_pet"
        const val MIDNIGHT = "npc.dusk_pet"
    }
}
