package org.rsmod.content.areas.city.keldagrim.keldagrim.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invTakeFee
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class RatPits @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.vc_dwarf_rich1") { startDialogue(it.npc) { rauborn() } }
        onOpNpc1("npc.vc_dwarf_rich2") { startDialogue(it.npc) { vaeringk() } }
        onOpNpc1("npc.vc_dwarf_poor1") { startDialogue(it.npc) { oxi() } }
        onOpNpc1("npc.vc_dwarf_poor2") { startDialogue(it.npc) { fior() } }
        onOpNpc1("npc.vc_dwarf_poor3") { startDialogue(it.npc) { sagira() } }
        onOpNpc1("npc.vc_dwarf_poor4") { startDialogue(it.npc) { anleif() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Rauborn
    private suspend fun Dialogue.rauborn() {
        chatNpc(happy, "Welcome to the ratpit bar, human traveller.")
        val option =
            choice3(
                "I'd like a beer please.",
                1,
                "I'd like some food please.",
                2,
                "I have to go",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(happy, "I'd like a beer please.")
                chatNpc(neutral, "That'll be 2 gold coins.")
                order(2, "obj.beer")
            }
            2 -> {
                chatPlayer(happy, "I'd like some food please.")
                chatNpc(neutral, "I can make you a stew for 20 gold coins.")
                order(20, "obj.stew")
            }
            else -> chatPlayer(neutral, "I have to go.")
        }
    }

    private suspend fun Dialogue.order(price: Int, item: String) {
        if (!choice2("Pay.", true, "Don't pay.", false)) {
            chatPlayer(neutral, "Sorry, I changed my mind.")
            return
        }
        if (!player.invTakeFee(price)) {
            chatPlayer(sad, "Sorry, I don't have $price coins on me.")
            return
        }
        player.invAddOrDrop(objRepo, item)
        chatNpc(happy, "Thanks for your custom.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Vaeringk
    private suspend fun Dialogue.vaeringk() {
        chatPlayer(quiz, "Hello. Shouldn't you be on guard duty?")
        chatNpc(drunk, "In this state? I've had too many beer s'already, beside' s'I'm off duty.")
        chatNpc(drunk, "The commander 's'given me the day off.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Oxi
    private suspend fun Dialogue.oxi() {
        chatPlayer(quiz, "Hello. What are you doing?")
        chatNpc(neutral, "I'm the rat keeper, it's my job to look after these here rats.")
        chatPlayer(quiz, "You do know that there are some rats running loose?")
        chatNpc(
            neutral,
            "Some nibble their way out from time to time. Don't worry there's enough cats down " +
                "here to sort anything that gets out.",
        )
        ratPitsExplained()
    }

    // https://oldschool.runescape.wiki/w/Transcript:Fior
    private suspend fun Dialogue.fior() {
        chatPlayer(quiz, "Hi. What are you doing?")
        chatNpc(neutral, "Relaxing after a long day at the factory.")
        chatNpc(
            happy,
            "If I keep up this winning streak, I mightn't have to go into work tomorrow.",
        )
        chatPlayer(quiz, "Do you mind me asking how much you have won?")
        chatNpc(happy, "Not at all, I'm up 10 coins so far.")
        chatPlayer(confused, "Umm ok.")
        ratPitsExplained()
    }

    // https://oldschool.runescape.wiki/w/Transcript:Sagira
    private suspend fun Dialogue.sagira() {
        chatPlayer(happy, "Hello.")
        chatNpc(neutral, "Hi, can I help you with something?")
        chatPlayer(quiz, "You're not from around here are you?")
        chatNpc(quiz, "Why do you say that?")
        chatPlayer(
            neutral,
            "Well your accent for one sounds a little funny, and you seem to have a little " +
                "more colour than most dwarves.",
        )
        chatNpc(
            neutral,
            "Living under a mountain isn't the best way to get a tan. No I'm not from around " +
                "here. I was born and worked most my life in the desert.",
        )
        chatNpc(
            neutral,
            "Just even thinking of the place makes me thirsty. Now can I help you with " +
                "anything?",
        )
        ratPitsExplained()
    }

    // https://oldschool.runescape.wiki/w/Transcript:Anleif
    private suspend fun Dialogue.anleif() {
        chatPlayer(happy, "Hello.")
        mesbox("The dwarf lady seems to be looking for someone.")
        chatNpc(worried, "Hello, I'm looking for my husband. Have you seen him?")
        chatPlayer(quiz, "What does he look like?")
        chatNpc(
            shifty,
            "Oh, to pot with this silly charade, I'm down here to try and win big on the " +
                "moggies.",
        )
        chatPlayer(confused, "Hang on, I thought you were looking for your husband.")
        chatNpc(
            shifty,
            "Um no, I was just pretending. It's frowned on a little to be seen in these parts.",
        )
        ratPitsExplained()
    }

    // TODO(content): [system:rat_pits] betting on cat fights against other players
    private suspend fun Dialogue.ratPitsExplained() {
        chatPlayer(quiz, "So how do these rat pits work?")
        chatNpc(neutral, "The Keldagrim pits cater for those with the larger variety of cats.")
        chatNpc(
            neutral,
            "If you have an overgrown cat and a couple of coins you can challenge someone else " +
                "who also has an overgrown cat and some spare change.",
        )
        chatPlayer(neutral, "That sounds simple enough so far.")
        chatNpc(
            neutral,
            "You then agree on a wager. It's rumoured that some ratcatchers can talk to their " +
                "own cats and give them tactics to aid them.",
        )
        chatPlayer(quiz, "So then how do you win the challenge?")
        chatNpc(
            neutral,
            "I was just getting to that. Both the challengers' cats are placed inside the " +
                "arena, and whichever kills 9 rats first wins.",
        )
        while (true) {
            val option =
                choice5(
                    "That sounds vile and cruel.",
                    1,
                    "It sounds a little dangerous for my cat.",
                    2,
                    "Wow that sounds like fun.",
                    3,
                    "Is there a limit to how much you can bet?",
                    4,
                    "Can I challenge you and your cat?",
                    5,
                )
            ratPitsAnswer(option)
            if (choice2("One more thing...", 1, "Thanks for your help.", 2) == 2) {
                chatPlayer(happy, "Thanks for your help.")
                return
            }
        }
    }

    private suspend fun Dialogue.ratPitsAnswer(option: Int) {
        when (option) {
            1 -> {
                chatPlayer(angry, "That sounds vile and cruel.")
                chatNpc(neutral, "It's a cruel world and the pits are no exception.")
                chatNpc(neutral, "Each to their own, I say.")
            }
            2 -> {
                chatPlayer(worried, "It sounds a little dangerous for my cat.")
                chatNpc(neutral, "I agree. Cats have been known to be killed in fights.")
                chatPlayer(
                    shocked,
                    "Oh how terrible. Is there nothing you can do to save your cat?",
                )
                chatNpc(
                    neutral,
                    "Of course there is, you can instruct it to be cautious, and it will leave " +
                        "once it feels as if it is in danger.",
                )
                chatPlayer(happy, "Oh that sounds quite wise.")
                chatNpc(neutral, "That depends on how you value your cat.")
                chatPlayer(confused, "Sorry, I don't follow.")
                chatNpc(neutral, "Well if your cat dies or it runs away you lose.")
                chatNpc(
                    neutral,
                    "So the longer it stays in the fight the better chance you have of winning.",
                )
            }
            3 -> {
                chatPlayer(happy, "Wow that sounds like fun!")
                chatNpc(neutral, "I don't know about that.")
                chatNpc(
                    neutral,
                    "One thing I know though is that it's a quick way to make and lose money.",
                )
            }
            4 -> {
                chatPlayer(quiz, "Is there a limit to how much you can bet?")
                chatNpc(
                    neutral,
                    "Hmmm... let me think. I don't think that's ever arisen but I think it's " +
                        "20,000.",
                )
                chatPlayer(
                    shocked,
                    "Hmm, that sounds like a silly amount of money to bet on a cat fight.",
                )
                chatNpc(neutral, "You'd be surprised what some people would be willing to bet.")
                chatNpc(neutral, "There is a minimum bet of 100 coins too.")
            }
            else -> {
                chatPlayer(quiz, "Can I challenge you and your cat?")
                chatNpc(neutral, "No, not now, we don't really bet with outsiders.")
                chatPlayer(confused, "But I'm not an outsider, am I not inside in the pits now?")
                chatNpc(
                    neutral,
                    "I don't know, maybe once you've done something to win over my trust.",
                )
                chatPlayer(quiz, "Can it be bought?")
                chatNpc(confused, "What?")
                chatPlayer(neutral, "Your trust.")
                chatNpc(
                    angry,
                    "No. Regular townsfolk don't like us, which leads us to distrust them and " +
                        "anyone else.",
                )
            }
        }
    }
}
