package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class JalNibRekDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(NIB, "Talk-to") { talkNib(it) }
        onPetOp(ZUK, "Talk-to") { talkZuk(it) }
    }

    private suspend fun ProtectedAccess.talkNib(npc: Npc) =
        startDialogue(npc) {
            when (access.random.of(3)) {
                0 -> {
                    chatPlayer(happy, "Yo Nib, what's going on?")
                    chatNpc(quiz, "Nibnib? Kl-Rek Nib?")
                    mesbox("Jal-Nib-Rek nips you.")
                    chatPlayer(angry, "What'd you do that for?")
                    chatNpc(laugh, "Heh Nib get you.")
                }
                1 -> {
                    chatPlayer(quiz, "What'd you have for dinner?")
                    chatNpc(happy, "Nibblings!")
                    chatPlayer(quiz, "Nibblings of what exactly?")
                    chatNpc(neutral, "Nib.")
                    chatPlayer(shocked, "Oh no! That's horrible.")
                }
                else -> {
                    chatPlayer(quiz, "Can you speak like a human can Nib?")
                    chatNpc(neutral, "No, I most definitely can not.")
                    chatPlayer(confused, "Aren't you speaking like a human right now...?")
                    chatNpc(neutral, "Jal-Nib-Rek Nib Kl-Jal, Zuk is mum.")
                    chatPlayer(neutral, "Interesting.")
                }
            }
        }

    private suspend fun ProtectedAccess.talkZuk(npc: Npc) =
        startDialogue(npc) {
            val bulwark = BULWARK in player.worn
            when (access.random.of(if (bulwark) 4 else 3)) {
                0 -> {
                    chatPlayer(happy, "What's up Zuk?")
                    chatNpc(sad, "Feeling a bit down to be honest.")
                    chatPlayer(quiz, "Why's that?")
                    chatNpc(sad, "Well...")
                    chatNpc(sad, "Not so long ago, I was a big fearsome boss, Now I'm just another pet.")
                    chatPlayer(happy, "Indeed, and you're going to follow me everywhere I go.")
                }
                1 -> {
                    chatPlayer(quiz, "Why have you got lava around your feet?")
                    chatNpc(neutral, "Keeps me cool.")
                    chatPlayer(confused, "But... lava is hot?")
                    chatNpc(neutral, "No no, I wasn't referring to the temperature.")
                    chatPlayer(neutral, "Ah...")
                }
                2 -> {
                    if (bulwark) {
                        chatPlayer(happy, "You're a lot smaller now, I don't even need this shield.")
                    } else {
                        chatPlayer(happy, "You're a lot smaller now, I don't even need a shield.")
                    }
                    chatNpc(
                        angry,
                        "Mere mortal, you only survived my challenge because of that convenient pile of rock.",
                    )
                    chatPlayer(laugh, "Well, you couldn't even break that pile of rock to get at me!")
                    chatNpc(neutral, "...")
                }
                else -> {
                    chatNpc(angry, "That shield won't protect you forever mortal.")
                    chatPlayer(happy, "It might actually.")
                    chatNpc(shifty, "We shall see about that.")
                    mesbox("TzRek-Zuk channels infernal energy into your shield burning your hands.")
                    chatPlayer(shocked, "Ouch! What the...")
                    chatNpc(laugh, "He he.")
                }
            }
        }

    private companion object {
        const val NIB = "npc.inferno_pet"
        const val ZUK = "npc.zuk_pet"
        const val BULWARK = "obj.dinhs_bulwark_ornament"
    }
}
