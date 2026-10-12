package org.rsmod.content.areas.misc.morytania.slepe

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invTakeFee
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class RatAndBat @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(CARL) { startDialogue(it.npc) { carl() } }
        onOpNpc1(ROY) { startDialogue(it.npc) { roy() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Carl
    private suspend fun Dialogue.carl() {
        chatPlayer(quiz, "What ales are you serving?")
        chatNpc(neutral, "We serve Crombwick's finest Bloody Bracer, that is all.")
        chatPlayer(quiz, "Sounds rough, how much?")
        chatNpc(neutral, "99 of you finest coins.")
        while (true) {
            val option =
                choice4(
                    "Alright, one Bloody Bracer, if that's all you have.",
                    1,
                    "99 coins? That's ridiculous!",
                    2,
                    "What can you tell me about this town?",
                    3,
                    "Actually, I'm no longer thirsty.",
                    4,
                )
            when (option) {
                1 -> return carlBuy()
                2 -> {
                    chatPlayer(angry, "99 coins? That's ridiculous!")
                    chatNpc(
                        neutral,
                        "What can I say? The ingredients are locally sourced so it's high " +
                            "quality, I promise.",
                    )
                    chatPlayer(neutral, "Well, I guess I shouldn't shoot the messenger.")
                    chatNpc(neutral, "So...")
                }
                3 -> {
                    chatPlayer(quiz, "What can you tell me about this town?")
                    chatNpc(
                        neutral,
                        "I've not been here long myself, so I'm afraid I may not be of much help.",
                    )
                    chatPlayer(quiz, "What happened to the previous barkeep?")
                    chatNpc(neutral, "Oh, he had to leave.")
                    chatPlayer(quiz, "How come?")
                    chatNpc(worried, "Scared of turning.")
                    chatPlayer(confused, "Turning?")
                    chatNpc(neutral, "Yes, into a sleeper.")
                    chatPlayer(
                        laugh,
                        "Oh right! You see, for a second there I thought you were talking about " +
                            "vampyres.",
                    )
                    chatNpc(
                        shifty,
                        "Ha ha! We, err, don't see many of those types around here. No, no, not " +
                            "at all.",
                    )
                    chatPlayer(shifty, "If you say so.")
                    chatNpc(neutral, "I do. So...")
                }
                4 -> {
                    chatPlayer(neutral, "Actually, I'm no longer thirsty.")
                    chatNpc(shifty, "Oh, the red thirst will be upon you soon.")
                    chatPlayer(confused, "Excuse me?")
                    chatNpc(laugh, "Just a jest, traveller.")
                    chatPlayer(neutral, "Oh.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.carlBuy() {
        chatPlayer(neutral, "Alright, one Bloody Bracer, if that's all you have.")
        if (!player.buyBracer()) {
            chatNpc(angry, "I said 99 coins! You haven't got 99 coins!")
            chatPlayer(sad, "Sorry, I'll come back another day.")
            return
        }
        chatPlayer(happy, "Thanks, Carl.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Roy
    private suspend fun Dialogue.roy() {
        chatNpc(
            happy,
            "Good day sir. Could I interest you in one of our finest Bloody Bracers? It's a " +
                "local speciality.",
        )
        while (true) {
            val option =
                choice4(
                    "One Bloody Bracer please.",
                    1,
                    "I'm not paying that!",
                    2,
                    "What happened to Carl?",
                    3,
                    "I'm not thirsty.",
                    4,
                )
            when (option) {
                1 -> return royBuy()
                2 -> {
                    chatPlayer(angry, "I'm not paying that!")
                    chatNpc(neutral, "Then for now, our business is at an end.")
                    return
                }
                3 -> {
                    chatPlayer(quiz, "What happened to Carl?")
                    chatNpc(confused, "Carl?")
                    chatPlayer(neutral, "The old barkeep.")
                    chatNpc(happy, "Oh yes, Carl!")
                    chatNpc(
                        neutral,
                        "He had to leave I'm afraid. Fear not though. I have taken over things " +
                            "here.",
                    )
                    chatPlayer(quiz, "And you are?")
                    chatNpc(
                        happy,
                        "Roy is the name. Now, could I interest you in one of our finest Bloody " +
                            "Bracers?",
                    )
                }
                4 -> {
                    chatPlayer(neutral, "I'm not thirsty.")
                    chatNpc(neutral, "Then for now, our business is at an end.")
                    return
                }
            }
        }
    }

    // TODO(content): [verify:in_game] whether "Here you go." is a menu option
    private suspend fun Dialogue.royBuy() {
        chatPlayer(happy, "One Bloody Bracer please.")
        chatNpc(neutral, "That will be 99 coins.")
        if (access.inv.count(COINS) < BRACER_PRICE) {
            chatPlayer(sad, "I don't have that much.")
            chatNpc(neutral, "Then for now, our business is at an end.")
            return
        }
        chatPlayer(happy, "Here you go.")
        if (!player.buyBracer()) {
            return
        }
        mesbox("You buy a Bloody Bracer.")
        chatNpc(happy, "Enjoy.")
        chatPlayer(happy, "Thanks.")
    }

    private fun Player.buyBracer(): Boolean {
        if (!invTakeFee(BRACER_PRICE)) {
            return false
        }
        invAddOrDrop(objRepo, BLOODY_BRACER)
        return true
    }

    private companion object {
        const val CARL = "npc.slepe_bartender_carl"
        const val ROY = "npc.slepe_bartender_roy"
        const val COINS = "obj.coins"
        const val BLOODY_BRACER = "obj.slepe_bloody_bracer"
        const val BRACER_PRICE = 99
    }
}
