package org.rsmod.content.areas.misc.islands.fossilisland.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.stat.stat
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class Merfolk : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.fossil_mermaid_deepwater") { startDialogue(it.npc) { mairin() } }
        onOpNpc1("npc.fossil_mermaid_fishing") { startDialogue(it.npc) { ceto() } }
        onOpNpc1("npc.fossil_mermaid_driftnets") { startDialogue(it.npc) { annette() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Mairin
    // TODO(content): [system:mairins_market] "What have you got to trade?" and Mairin's Market
    // TODO(content): [verify:in_game] "Thank you." after the player takes the fishbowl helmet off
    private suspend fun Dialogue.mairin() {
        chatPlayer(happy, "Hello.")
        if (FISHBOWL_HELMET in player.worn || MEDALLION in player.worn) {
            chatNpc(angry, "Oh! Take that horrid thing off your head immediately!")
            return
        }
        if (FISHBOWL_HELMET in player.inv || MEDALLION in player.inv) {
            mairinHelmet()
        }
        while (true) {
            val option =
                choice4(
                    "Who are you?",
                    1,
                    "How do I breathe underwater without a helmet",
                    2,
                    "Is there anything I can do to help you?",
                    3,
                    "Bye.",
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Who are you?")
                    chatNpc(happy, "I'm Mairin, one of the many merfolk here.")
                    chatPlayer(confused, "Many? I don't see that many...")
                    chatNpc(
                        neutral,
                        "Just because you don't see us, doesn't mean we're not there.",
                    )
                    chatPlayer(worried, "That's kind of creepy. What do you actually do?")
                    chatNpc(
                        neutral,
                        "Mostly stop people entering beyond this wreck with those horrid " +
                            "fishbowls.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "How do I breathe underwater without a helmet.")
                    chatNpc(neutral, "Just find some bubbles, or other sources of air.")
                    chatPlayer(quiz, "Other sources?")
                    chatNpc(neutral, "Yes, some fish can be a good source of air too..")
                }
                3 -> mairinTears()
                4 -> {
                    chatPlayer(neutral, "Bye.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.mairinHelmet() {
        chatNpc(angry, "You're not going into deeper water with that bowl.")
        chatPlayer(quiz, "Why don't you like my helmet?")
        chatNpc(
            sad,
            "My sister was once trapped in one of those for a long time by your kind. It was " +
                "awful and she's still terrified of them. We don't allow them beyond this point " +
                "anymore.",
        )
        chatPlayer(quiz, "What shall I do with it?")
        chatNpc(neutral, "Put it on the pile over there.")
        chatPlayer(worried, "But how will I breathe?")
        chatNpc(neutral, "Breathing is for pesky humans.")
        chatPlayer(bored, "Yes. I am one.")
        chatNpc(neutral, "Oops. Nevermind. I'm sure you'll manage. Just look for bubbles.")
    }

    private suspend fun Dialogue.mairinTears() {
        chatPlayer(quiz, "Is there anything I can do to help you?")
        chatNpc(happy, "Yes! You can collect the tears of my poor trapped sister.")
        chatPlayer(confused, "What? Tears? But, they're water, surely underwater they would...")
        chatNpc(
            neutral,
            "She's a mermaid, like me. Didn't you know our tears are the pearls of the sea? You " +
                "humans.",
        )
        chatPlayer(neutral, "Ok, keep your scales on. Where would I find these tears?")
        chatNpc(neutral, "They usually end up secreted in clams or other places.")
        chatPlayer(quiz, "What do I get out of it?")
        chatNpc(
            neutral,
            "Well, I have access to a lot of old stuff that's fallen down under the water, so " +
                "I'm sure I could reward you in some way.",
        )
        chatNpc(
            happy,
            "If you find any special glistening tears, I can teach you some useful Agility and " +
                "Thieving techniques too. So will you help me?",
        )
        if (choice2("Yes, I'll help.", true, "Nah. Not for me.", false)) {
            chatPlayer(happy, "Yes, I'll help.")
        } else {
            chatPlayer(neutral, "Nah. Not for me.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ceto
    // TODO(content): [system:drift_net_fishing] the 200 numulite day pass, the 20,000 numulite
    // permanent access, the Pay option and entering the drift net fishing instance
    private suspend fun Dialogue.ceto() {
        chatNpc(happy, "Greetings, I suppose you're one of those twolegs obsessed with fish?")
        chatPlayer(neutral, "Maybe. I could just like swimming around.")
        chatNpc(happy, "Well, if you are, you've come to the right plaice.")
        val option =
            choice5(
                "Plaice? Really?",
                1,
                "I'd like to enter a private instance please.",
                2,
                "Who are you?",
                3,
                "How do I catch fish here?",
                4,
                "Bye.",
                5,
            )
        when (option) {
            1 -> {
                cetoPuns()
                cetoOptions(
                    "I'd like to enter a private instance please.",
                    2,
                    "Who are you?",
                    3,
                    "How do I catch fish here?",
                    4,
                )
            }
            else -> cetoTopic(option)
        }
    }

    private suspend fun Dialogue.cetoOptions(
        first: String,
        firstResult: Int,
        second: String,
        secondResult: Int,
        third: String,
        thirdResult: Int,
    ) {
        val option =
            choice4(first, firstResult, second, secondResult, third, thirdResult, "Bye.", 5)
        cetoTopic(option)
    }

    private suspend fun Dialogue.cetoTopic(option: Int) {
        when (option) {
            2 -> cetoInstance()
            3 -> {
                cetoWho()
                cetoFollowUp(
                    "I'd like to enter a private instance please.",
                    2,
                    "How do I catch fish here?",
                    4,
                )
            }
            4 -> {
                cetoFishing()
                cetoFollowUp("I'd like to enter a private instance please.", 2, "Who are you?", 3)
            }
            5 -> {
                chatPlayer(neutral, "Bye")
                chatNpc(happy, "Sea you later.")
            }
        }
    }

    private suspend fun Dialogue.cetoFollowUp(
        first: String,
        firstResult: Int,
        second: String,
        secondResult: Int,
    ) {
        cetoTopic(choice3(first, firstResult, second, secondResult, "Bye.", 5))
    }

    private suspend fun Dialogue.cetoPuns() {
        chatPlayer(confused, "Plaice? Really?")
        chatNpc(
            happy,
            "Is there somefin wrong with that? I mean, it's not like I have anyfin better to do.",
        )
        chatPlayer(bored, "Please stop.")
        chatNpc(
            happy,
            "Reely? And there I trout you were here to sea what you could sea and experience the " +
                "opportunaty of a lifetime swimming with the fishes.",
        )
        chatPlayer(angry, "Really. Really stop.")
        chatNpc(happy, "What's wrong, do you need a herring aid?")
        chatPlayer(sad, "Nooooo....")
        chatNpc(
            laugh,
            "Oh don't be so crabby. You should be more up to date with current affairs. Shoaly " +
                "you like puns chum?",
        )
        chatPlayer(silent, "...")
        chatNpc(laugh, "A blenny for your thoughts?")
        chatPlayer(sad, "Make it stop.")
        chatNpc(
            laugh,
            "Ahh, salty I sea. You provoked me into this and now you'll feel the wrasse of my " +
                "tongue.",
        )
        chatPlayer(sad, "*whimpers*")
    }

    private suspend fun Dialogue.cetoInstance() {
        chatPlayer(quiz, "I'd like to enter a private instance please.")
        if (player.stat("stat.fishing") < 47 || player.stat("stat.hunter") < 44) {
            chatNpc(neutral, "Sorry, but you're really not experienced enough to use my cavern.")
            mesbox("You need level 47 Fishing and level 44 Hunter to enter the fishing cavern.")
            return
        }
        chatNpc(
            neutral,
            "I'll want 200 numulites first. If you pay me that, I'll let you in there as much as " +
                "you like for the rest of this day.",
        )
    }

    private suspend fun Dialogue.cetoWho() {
        chatPlayer(quiz, "Who are you?")
        chatNpc(
            neutral,
            "My name is Ceto. I used to be a carefree soul, but now that the twolegs are " +
                "interested in these little bits of bone, I just keep my little fish farm going " +
                "and the twolegs pay me a few squid.",
        )
        chatPlayer(quiz, "Shouldn't you be with your people or something?")
        chatNpc(
            sad,
            "Well, they said I was too young to swim with them, and I should be responsible and " +
                "get a job. They're so old school.",
        )
    }

    private suspend fun Dialogue.cetoFishing() {
        chatPlayer(quiz, "How do I catch fish here?")
        chatNpc(
            happy,
            "Ahh well, I've set up a pretty good fish trap here, the terrain really lends itself " +
                "to it. But you'll need a couple of drift nets.",
        )
        chatPlayer(quiz, "How do I get those?")
        chatNpc(
            neutral,
            "Well I weave mine from my sisters' hair, but you'll have to make do with some plant " +
                "fibres I guess. I think the twolegs have ways to do that, right?",
        )
        chatPlayer(neutral, "Yes, usually we use jute on a loom.")
        chatNpc(
            happy,
            "Fintastic. You may also find something spikey helps to disturb the fish and make " +
                "them panic.",
        )
        chatPlayer(confused, "Spikey?")
        chatNpc(happy, "Yes, something with lots of spikes, maybe like a trident.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Annette
    // TODO(content): [system:drift_net_storage] the "Yes please." option (with "No thanks." again
    // after her explanation) and the Nets option open Annette's Nets
    private suspend fun Dialogue.annette() {
        chatNpc(happy, "Can I help you with drift net storage, human?")
        if (choice2("What do you do?", true, "No thanks.", false, title = "Select an Option")) {
            chatPlayer(quiz, "What do you do?")
            chatNpc(
                neutral,
                "My aunt lets people come in here to catch the fish. Sometimes they have trouble " +
                    "carrying enough stuff, so I help by holding onto spare drift nets for them.",
            )
            chatNpc(
                happy,
                "It's a free service to help more of you humans use Ceto's cave; she's paying " +
                    "me, so you don't have to. So can I help you with your drift nets?",
            )
            return
        }
        chatPlayer(neutral, "No thanks.")
    }

    private companion object {
        const val FISHBOWL_HELMET = "obj.hundred_pirate_diving_helmet"
        const val MEDALLION = "obj.medallion_of_the_deep"
    }
}
