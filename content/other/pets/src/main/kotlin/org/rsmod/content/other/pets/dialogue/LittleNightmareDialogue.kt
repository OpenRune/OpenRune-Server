package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class LittleNightmareDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(NIGHTMARE, "Talk-to") { talkNightmare(it) }
        onPetOp(PARASITE, "Talk-to") { talkParasite(it) }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Little_Nightmare
    private suspend fun ProtectedAccess.talkNightmare(npc: Npc) =
        startDialogue(npc) {
            when (access.random.of(2)) {
                0 -> {
                    chatPlayer(quiz, "You eat dreams right?")
                    chatNpc(neutral, "Well... not exact-")
                    chatPlayer(quiz, "What do dreams taste like?")
                    chatNpc(neutral, "I don't eat dre-")
                    chatPlayer(happy, "I bet they taste like strawberries! I love strawberries!")
                    chatNpc(neutral, "...")
                }
                else -> {
                    chatPlayer(quiz, "If you're a nightmare, would a male version of you be a nighthorse?")
                    chatNpc(quiz, "What's a horse?")
                    chatPlayer(confused, "I have no idea.")
                }
            }
        }

    // https://oldschool.runescape.wiki/w/Transcript:Little_Nightmare
    private suspend fun ProtectedAccess.talkParasite(npc: Npc) =
        startDialogue(npc) {
            chatPlayer(happy, "What's up b?")
            chatNpc(neutral, "Bzz.")
            chatPlayer(neutral, "I see.")
        }

    private companion object {
        const val NIGHTMARE = "npc.nightmare_pet"
        const val PARASITE = "npc.nightmare_pet_parasite"
    }
}
