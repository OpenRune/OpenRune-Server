package org.rsmod.content.other.pets.dialogue

import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.content.other.pets.onPetOpU
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class HerbiDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(NPC, "Talk-to") { talk(it) }
        onPetOpU(NPC) { useItem(it.npc, it.objType) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            when (access.random.of(5)) {
                0 -> {
                    chatPlayer(quiz, "Are you hungry?")
                    chatNpc(quiz, "That depends, what have you got?")
                    chatPlayer(happy, "I'm sure I could knock you up a decent salad.")
                    chatNpc(neutral, "I'm actually an insectivore.")
                    chatPlayer(confused, "Oh, but your name suggests that-")
                    chatNpc(
                        angry,
                        "I think you'll find I didn't name myself, you humans and your silly puns.",
                    )
                    chatPlayer(laugh, "No need to PUNish us for our incredible wit.")
                    chatNpc(bored, "Please. Stop.")
                }
                1 -> {
                    chatPlayer(quiz, "Have your herbs died?")
                    chatNpc(
                        sad,
                        "These old things? I guess they've dried up... I'm getting old and I need " +
                            "caring for. I've chosen you to do that by the way.",
                    )
                    chatPlayer(
                        angry,
                        "Oh fantastic! I spend all that time training the Farming skill, and now " +
                            "I'm supposed to care for your herbs as well?",
                    )
                    chatNpc(quiz, "I could try the next person if you'd prefer?")
                    chatPlayer(laugh, "I'm just joking you old swine!")
                }
                2 -> {
                    chatPlayer(
                        quiz,
                        "So you live in a hole? I would've thought Boars are surface dwelling mammals.",
                    )
                    chatNpc(
                        happy,
                        "Well, I'm special! I bore down a little so I'm nice and cosy with my herbs " +
                            "exposed to the sun, it's all very interesting.",
                    )
                    chatPlayer(laugh, "Sounds rather... Boring!")
                    chatNpc(bored, "How very original...")
                }
                3 -> {
                    chatPlayer(quiz, "Tell me... do you like Avacado?")
                    chatNpc(angry, "I'm an insectivore, but even if I wasn't I'd hate Avacado!")
                    chatPlayer(confused, "Why ever not? It's delicious!")
                    chatNpc(
                        neutral,
                        "I don't know why people like it so much... it tastes a like a ball of " +
                            "chewed up grass.",
                    )
                    chatPlayer(laugh, "Sometimes you can be such a bore...")
                }
                else -> {
                    chatNpc(shocked, "When I was a young HERBIBOAR!!")
                    chatPlayer(worried, "I'm standing right next to you, no need to shout...")
                    chatNpc(sad, "I was trying to sing you a song...")
                }
            }
        }

    private suspend fun ProtectedAccess.useItem(npc: Npc, obj: ItemServerType) {
        val name = obj.name
        when {
            name in BUTTERFLY_JARS ->
                startDialogue(npc) {
                    chatPlayer(quiz, "Ever tried a butterfly before?")
                    chatNpc(neutral, "I've never been able to catch one, they've always eluded me.")
                    mesbox("Herbi chomps excitedly on the butterfly, he seems pleased.")
                    chatNpc(happy, "Woah! You should try those, they're amazing.")
                }
            name.equals("Grubs à la mode", ignoreCase = true) ->
                startDialogue(npc) {
                    chatPlayer(happy, "I got you some grubs!")
                    chatNpc(happy, "My favorite!")
                    mesbox("Herbi munches happily on the grubs. It's rather gross.")
                }
            name.equals("King worm", ignoreCase = true) ||
                name.equals("Red vine worm", ignoreCase = true) ->
                startDialogue(npc) {
                    chatPlayer(quiz, "How about some wyrms?")
                    chatNpc(
                        neutral,
                        "I assume you mean worms, not sure I could stomach a wyrm, far too big.",
                    )
                    chatPlayer(neutral, "Oh yeah, those words don't even sound the same.")
                    chatNpc(happy, "I don't judge you, now hand over those worms!")
                    mesbox("Herbi hungrily devours the worms, you regret watching him do it.")
                    chatNpc(happy, "Slimy, yet satisfying.")
                }
            name.equals("Spider carcass", ignoreCase = true) ->
                startDialogue(npc) {
                    chatPlayer(happy, "I got you a spider carcass!")
                    chatNpc(neutral, "Not really an insect, but admittedly I'm not too fussy.")
                    mesbox("Herbi gobbles the spider down, several legs fall to the ground around him.")
                    chatPlayer(neutral, "You missed a bit.")
                    chatNpc(happy, "I'll save them for later.")
                }
        }
    }

    private companion object {
        const val NPC = "npc.herbiboar_pet"
        val BUTTERFLY_JARS = setOf(
            "Ruby harvest",
            "Sapphire glacialis",
            "Snowy knight",
            "Black warlock",
            "Moonlight moth",
            "Sunlight moth",
        )
    }
}
