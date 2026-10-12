package org.rsmod.content.areas.misc.morytania.slepe

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invTakeFee
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class SanctuaryTravellers @Inject constructor(private val objRepo: ObjRepository) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(HAMELN) { startDialogue(it.npc) { hameln() } }
        onOpNpc1(HANCHEN) { startDialogue(it.npc) { hanchen() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Hameln_the_Jester (before starting Sins of
    // the Father)
    // TODO(content): [quest:sins_of_the_father] the church dialogue once the quest is started
    //  and after it is completed
    // TODO(content): [verify:in_game] whether a parchment in the bank counts as having it
    private suspend fun Dialogue.hameln() {
        chatPlayer(happy, "Play us a song!")
        chatNpc(neutral, "Not now, perhaps another time.")
        chatPlayer(sad, "Oh, alright.")
        while (true) {
            val option =
                choice3(
                    "You don't look like a local.",
                    1,
                    "What're you doing down here?",
                    2,
                    "Bye!",
                    3,
                    title = "Select an option",
                )
            when (option) {
                1 -> notALocal()
                2 -> if (!downHere()) return
                3 -> {
                    chatPlayer(happy, "Bye!")
                    chatNpc(happy, "Timoiva, traveller.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.notALocal() {
        chatPlayer(quiz, "You don't look like a local.")
        chatNpc(
            happy,
            "Me? Not at all. That thief over there and the tattooed guy upstairs are my " +
                "travelling companions. We come from afar.",
        )
        chatPlayer(quiz, "How far?")
        chatNpc(neutral, "We travelled eastwards over many oceans.")
        chatPlayer(quiz, "Why travel all this way to sit in a dirty cave?")
        chatNpc(
            neutral,
            "I ask myself the same question, but we're on a quest, of sorts. You see, I found a " +
                "poem in the College of Bards that told of this place. It seemed worthy of " +
                "exploring.",
        )
        chatPlayer(confused, "College of Bards? Can't say I've heard of it.")
        chatNpc(
            happy,
            "Oh, it's beautiful! Have you not visited the grand Kingdom of Varlamore? The " +
                "college is but one of many attractions.",
        )
        chatPlayer(quiz, "Oh really? Perhaps I should go there sometime.")
        chatNpc(happy, "You won't regret it!")
    }

    private suspend fun Dialogue.downHere(): Boolean {
        chatPlayer(quiz, "What're you doing down here?")
        chatNpc(neutral, "We were told to follow the rats and they led us to this tomb.")
        chatPlayer(confused, "Doesn't look much like a tomb.")
        chatNpc(
            neutral,
            "Well, it may not have been a tomb to begin with, but we're to believe it has " +
                "become one.",
        )
        chatPlayer(quiz, "Where are all the bodies?")
        chatNpc(neutral, "Deeper in the sanctuary, this is just the entrance.")
        chatPlayer(quiz, "So why haven't you gone any further?")
        chatNpc(
            worried,
            "Well... We were going to. But then we heard of the Nightmare that dwells within the " +
                "sanctuary. We've decided to stay here until she's gone.",
        )
        chatPlayer(quiz, "I see. How do you know all this?")
        if (access.inv.contains(PARCHMENT)) {
            chatNpc(
                neutral,
                "It's all in that parchment that I gave you earlier. You should give it a read.",
            )
            return false
        }
        chatNpc(neutral, "Oh, I found this when searching around in the church above.")
        player.invAddOrDrop(objRepo, PARCHMENT)
        objbox(PARCHMENT, "The jester hands you an old piece of parchment.")
        chatPlayer(happy, "Interesting, I'll give it a read!")
        return true
    }

    // https://oldschool.runescape.wiki/w/Transcript:Hanchen_the_Hound
    // TODO(content): [verify:in_game] the 10 coin fee, taken from
    //  https://oldschool.runescape.wiki/w/Hanchen_the_Hound
    private suspend fun Dialogue.hanchen() {
        chatNpc(shifty, "Have you coin?")
        val coins = access.inv.count(COINS)
        if (coins == 0) {
            chatPlayer(sad, "It seems I'm all out.")
            chatNpc(neutral, "Then we have nothing to discuss.")
            return
        }
        chatPlayer(confused, "Umm, just a little, why?")
        if (coins < HANCHEN_FEE) {
            chatNpc(neutral, "Not nearly enough, we have nothing to discuss.")
            return
        }
        chatNpc(
            neutral,
            "I'd rather not waste time in conversation with someone who can't pay for " +
                "information.",
        )
        chatPlayer(quiz, "Who said I need information?")
        chatNpc(shifty, "You travelling types, all you have is questions.")
        val option =
            choice3(
                "I guess I'm a little curious about your companion.",
                1,
                "Do you know much about the Painted One in the town center?",
                2,
                "I'm not giving you my money.",
                3,
                title = "Select an Option",
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "I guess I'm a little curious about your companion.")
                chatNpc(
                    neutral,
                    "The jester? The journey has put him off performing, for the better, I'd say.",
                )
                if (!player.invTakeFee(HANCHEN_FEE)) {
                    return
                }
                mesbox("The thief takes a handful of coins from your pack.")
                chatPlayer(angry, "That was hardly worth it.")
                chatNpc(shifty, "I never promised worthwhile information.")
            }
            2 -> {
                chatPlayer(quiz, "Do you know much about the Painted One in the town center?")
                chatNpc(
                    neutral,
                    "Oh yes, he came with us. He tends to find himself in strange situations, " +
                        "but he can handle himself.",
                )
                if (!player.invTakeFee(HANCHEN_FEE)) {
                    return
                }
                mesbox("The thief takes a handful of coins from your pack.")
                chatPlayer(angry, "Do you care more for gold than you do your friend?")
                chatNpc(shifty, "Apparently.")
            }
            3 -> {
                chatPlayer(angry, "I'm not giving you my money.")
                chatNpc(neutral, "Then we have nothing to discuss.")
            }
        }
    }

    private companion object {
        const val HAMELN = "npc.slepe_jester_child"
        const val HANCHEN = "npc.slepe_thief_child"
        const val PARCHMENT = "obj.slepe_manuscript_buried_alive"
        const val COINS = "obj.coins"
        const val HANCHEN_FEE = 10
    }
}
