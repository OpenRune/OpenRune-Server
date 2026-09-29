package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class LilZikDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(ZIK, "Talk-to") { talkZik(it) }
        onPetOp(MAIDEN, "Talk-to") { talkMaiden(it) }
        onPetOp(BLOAT, "Talk-to") { talkBloat(it) }
        onPetOp(NYLO, "Talk-to") { talkNylo(it) }
        onPetOp(SOT, "Talk-to") { talkSot(it) }
        onPetOp(XARP, "Talk-to") { talkXarp(it) }
    }

    private suspend fun ProtectedAccess.talkZik(npc: Npc) =
        startDialogue(npc) {
            when (access.random.of(4)) {
                0 -> {
                    chatPlayer(happy, "Hey Lil' Zik.")
                    chatNpc(angry, "Stop.")
                    chatNpc(angry, "Calling.")
                    chatNpc(angry, "Me.")
                    chatNpc(verymad, "Little!")
                    chatPlayer(laugh, "Never!")
                }
                1 -> {
                    chatPlayer(neutral, "You know... you're not like other spiders.")
                    chatNpc(sad, "You know I hate it when you say that... please leave me alone.")
                    chatPlayer(
                        happy,
                        "But I earned you fair and square at the Theatre of Blood! You're mine to keep.",
                    )
                    chatNpc(neutral, "...")
                }
                2 -> {
                    chatPlayer(happy, "Incy wincy Verzik climbed up the water spout...")
                    chatPlayer(happy, "Down came the rain and washed poor Verzik out...")
                    chatNpc(
                        verymad,
                        "Out came the Vampyre to put an end to this at once. Humans deserve only one fate!",
                    )
                    chatPlayer(worried, "Wow, calm down. It's just a nursery rhyme.")
                    chatNpc(angry, "I'm not in the mood.")
                }
                else -> {
                    chatPlayer(happy, "Hi, I'm here for my reward!")
                    chatNpc(bored, "Not again...")
                }
            }
        }

    private suspend fun ProtectedAccess.talkMaiden(npc: Npc) =
        startDialogue(npc) {
            when (access.random.of(2)) {
                0 -> {
                    chatNpc(quiz, "Have you seen my brother?")
                    chatPlayer(quiz, "I'm not sure, what did he look like?")
                    chatNpc(sad, "He was a kind boy, with a bright smile...")
                    chatPlayer(neutral, "Hmm... Can't say I have I'm afraid.")
                    chatNpc(sad, "Oh... I'd very much like to see him again.")
                }
                else -> {
                    chatNpc(happy, "Thank you for freeing me...")
                    chatPlayer(confused, "Freeing you? I didn't know you were trapped.")
                    chatNpc(sad, "The vampyres... they tricked me...")
                    chatPlayer(happy, "Oh, you're welcome then!")
                }
            }
        }

    private suspend fun ProtectedAccess.talkBloat(npc: Npc) =
        startDialogue(npc) {
            when (access.random.of(3)) {
                0 -> {
                    chatNpc(quiz, "Maaasteeer?")
                    chatPlayer(neutral, "I suppose I am your master now, I hadn't thought about it.")
                    chatNpc(angry, "Kill maasteer...")
                    chatPlayer(shocked, "No don't do that!")
                }
                1 -> {
                    chatPlayer(quiz, "Do you smell that?")
                    chatNpc(quiz, "Hnrgh?")
                    chatPlayer(confused, "*Sniffs* What are you made of Lil'Bloat?")
                    chatNpc(neutral, "Cccorpsssee...")
                    chatPlayer(worried, "That would explain the smell...")
                }
                else -> {
                    chatPlayer(happy, "Hello Lil' Bloat.")
                    chatNpc(neutral, "...Hhhh-.")
                    chatPlayer(quiz, "Are you trying to talk?")
                    chatNpc(neutral, "...Hhhhuu-")
                    chatPlayer(happy, "Are you trying to say Hello? Come on you can do it!")
                    chatNpc(neutral, "...Hhhuun-")
                    chatNpc(neutral, "...Hhuunnggrryy.")
                    chatPlayer(bored, "Oh, of course...")
                }
            }
        }

    private suspend fun ProtectedAccess.talkNylo(npc: Npc) =
        startDialogue(npc) {
            chatPlayer(happy, "How are you doing little guy?")
            chatNpc(quiz, "Chitter-chitter?")
            chatPlayer(happy, "You seem happy to be free of the Theatre.")
            chatNpc(happy, "Chitter-chitter!")
        }

    private suspend fun ProtectedAccess.talkSot(npc: Npc) =
        startDialogue(npc) {
            when (access.random.of(2)) {
                0 -> {
                    chatPlayer(happy, "Hello my angry little monster.")
                    chatNpc(angry, "Grrrrruff!")
                    chatPlayer(happy, "You don't mean that, you're just grumpy.")
                    chatNpc(angry, "Grrrr...")
                    chatPlayer(happy, "That's a good girl.")
                }
                else -> {
                    chatPlayer(quiz, "Would you like a treat?")
                    chatNpc(quiz, "Grruff?")
                    chatPlayer(
                        worried,
                        "Oh, you don't eat normal food, I'm afraid I don't have any adventurers to feed you.",
                    )
                    chatNpc(verymad, "Grrrrrr...")
                    chatPlayer(shocked, "I'm sorry! Please don't send me to the shadow realm!")
                }
            }
        }

    private suspend fun ProtectedAccess.talkXarp(npc: Npc) =
        startDialogue(npc) {
            chatPlayer(neutral, "We're not in the Theatre anymore, so no poisoning anyone, got that?")
            chatNpc(quiz, "Scraaa?")
            chatPlayer(angry, "I mean it, no spitting poison at people or on the floor.")
            chatNpc(sad, "Scraaa....")
        }

    private companion object {
        const val ZIK = "npc.verzik_pet"
        const val MAIDEN = "npc.verzik_pet_maiden"
        const val BLOAT = "npc.verzik_pet_bloat"
        const val NYLO = "npc.verzik_pet_nylocas"
        const val SOT = "npc.verzik_pet_sotetseg"
        const val XARP = "npc.verzik_pet_xarpus"
    }
}
