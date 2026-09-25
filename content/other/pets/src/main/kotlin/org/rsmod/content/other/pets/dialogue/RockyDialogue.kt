package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class RockyDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(ROCKY, "Talk-to") { talkRocky(it) }
        onPetOp(RED_PANDA, "Talk-to") { talkRed(it) }
        onPetOp(TANUKI, "Talk-to") { talkZiggy(it) }
    }

    private suspend fun ProtectedAccess.talkRocky(npc: Npc) =
        startDialogue(npc) {
            when (access.random.of(4)) {
                0 -> {
                    chatPlayer(shifty, "*Whistles*")
                    mesbox("You slip your hand into Rocky's pocket.")
                    chatNpc(
                        angry,
                        "OY!! You're going to have to do better than that! Sheesh, what an amateur.",
                    )
                }
                1 -> {
                    chatPlayer(quiz, "Is there much competition between you raccoons and the magpies?")
                    chatNpc(neutral, "Magpies have nothing on us! They're just interested in shinies.")
                    chatNpc(
                        happy,
                        "Us raccoons have a finer taste, we can see the value in anything, whether " +
                            "it shines or not.",
                    )
                }
                2 -> {
                    chatPlayer(shifty, "Hey Rocky, do you want to commit a bank robbery with me?")
                    chatNpc(
                        neutral,
                        "If that is the level you are at, I do not wish to participate in criminal " +
                            "acts with you ${player.displayName}.",
                    )
                    chatPlayer(quiz, "Well what are you interested in stealing?")
                    chatNpc(happy, "The heart of a lovely raccoon called Rodney.")
                    chatPlayer(neutral, "I cannot really help you there I'm afraid.")
                }
                else -> {
                    chatPlayer(happy, "Hi Rocky, how are you?")
                    chatNpc(shocked, "Your shoe laces are untied!")
                    mesbox("Rocky jumps into your backpack...")
                    chatNpc(laugh, "The master thief strikes again! Here you go, have it back.")
                    mesbox("Rocky flicks the coin back into your backpack")
                    chatPlayer(shifty, "I better keep my eyes on you...")
                }
            }
        }

    private suspend fun ProtectedAccess.talkZiggy(npc: Npc) =
        startDialogue(npc) {
            when (access.random.of(3)) {
                0 -> {
                    chatPlayer(quiz, "Hey Ziggy, do you have any relation to a raccoon named Rocky?")
                    chatNpc(neutral, "Who me? No, me and Rocky are not related.")
                    chatPlayer(quiz, "Really? But you look so similar.")
                    chatNpc(
                        happy,
                        "That's because a Tanuki is a master of disguise! I could pretend to be a " +
                            "human if I wanted to.",
                    )
                    chatPlayer(happy, "Wow! Why don't you do it now then?")
                    chatNpc(bored, "I said if I wanted to, I don't want to.")
                }
                1 -> {
                    chatPlayer(laugh, "Hey Ziggy, *giggles* do you happen to know any good plumbers?")
                    chatNpc(neutral, "Well that's a bit of a silly question isn't it.")
                    chatPlayer(quiz, "What do you mean?")
                    chatNpc(quiz, "Why would I, a Tanuki, know any good plumbers?")
                    chatPlayer(neutral, "I don't know, you might have come across one before.")
                    chatNpc(
                        bored,
                        "Well, clearly not, since I spend most of my time following you around.",
                    )
                }
                else -> {
                    chatPlayer(happy, "Ziggy! I've got a treat for you!")
                    chatNpc(happy, "Really! Where! Let me have it!")
                    mesbox("You hold out your fist.")
                    chatNpc(happy, "Why are you hiding it? Let me have it! Please, please, please!")
                    mesbox("You open your fist... there is no treat.")
                    chatNpc(confused, "Oh, where did the treat go? Did you drop it?")
                    chatPlayer(shifty, "Yeah, I must have dropped it, sorry about that Ziggy.")
                }
            }
        }

    private suspend fun ProtectedAccess.talkRed(npc: Npc) =
        startDialogue(npc) {
            when (access.random.of(3)) {
                0 -> {
                    chatPlayer(quiz, "Have you ever seen a blue panda before?")
                    chatNpc(
                        neutral,
                        "Blue Panda? That's ridiculous, there's only one type of panda and it is a " +
                            "beautiful red.",
                    )
                }
                1 -> {
                    chatPlayer(happy, "Hey Red, hows it going?")
                    chatNpc(happy, "Hi! Want to see me do a really fast cartwheel?")
                    chatPlayer(neutral, "Uh, yeah, sure go ahead.")
                    mesbox("Nothing happens...")
                    chatNpc(happy, "Want to see me do it again?")
                    chatPlayer(confused, "Huh? Nothing happened...")
                    chatNpc(shifty, "To your eyes...")
                }
                else -> {
                    chatPlayer(quiz, "Do you know a raccoon called Dufresne?")
                    chatNpc(neutral, "No, I don't believe I have.")
                }
            }
        }

    private companion object {
        const val ROCKY = "npc.skillpet_thieving"
        const val RED_PANDA = "npc.skillpet_thieving_panda"
        const val TANUKI = "npc.skillpet_thieving_tanuki"
    }
}
