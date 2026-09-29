package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class WispDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(NPC, "Talk-to") { talk(it) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            when (access.random.of(3)) {
                0 -> {
                    chatPlayer(shifty, "I don't trust shadows.")
                    chatPlayer(laugh, "They act pretty shady...")
                    chatNpc(bored, "Wow.")
                    chatNpc(neutral, "You can trust me, ${player.displayName}.")
                    chatNpc(shifty, "Now, come closer... I have something to tell you...")
                    mesbox("Wisp starts to whisper into your ear...")
                    chatPlayer(
                        shocked,
                        "Woah, woah, woah! None of that! I know what your whispers are capable of.",
                    )
                }
                1 -> {
                    chatPlayer(quiz, "Can you sing me a song?")
                    chatNpc(neutral, "If I were to do that, your mind would perish.")
                    chatPlayer(happy, "Maybe I can sing you a song instead?")
                    chatNpc(worried, "I'd rather you d-")
                    chatPlayer(neutral, "Ehem...")
                    chatPlayer(happy, "Let my voice lead you this waaay!")
                    chatPlayer(happy, "I will not lead you astraaay!")
                    chatNpc(sad, "Please... No more.")
                    chatPlayer(neutral, "Suit yourself. I'd make a great siren.")
                    chatNpc(bored, "Mmmhmm.")
                }
                else -> {
                    chatPlayer(quiz, "Hows it going, Wisp?")
                    chatNpc(sad, "There is too much light here. It's awful.")
                    chatPlayer(happy, "I love it!")
                    chatNpc(sad, "I miss the shadows.")
                }
            }
        }

    private companion object {
        const val NPC = "npc.whisperer_pet"
    }
}
