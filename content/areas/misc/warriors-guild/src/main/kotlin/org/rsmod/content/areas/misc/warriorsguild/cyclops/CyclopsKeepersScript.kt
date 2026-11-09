package org.rsmod.content.areas.misc.warriorsguild.cyclops

import org.rsmod.api.enums.WarguildEnums
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpcU
import org.rsmod.content.areas.misc.warriorsguild.Defenders
import org.rsmod.content.areas.misc.warriorsguild.basementUnlocked
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CyclopsKeepersScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(CyclopsRooms.KAMFREENA) { startDialogue(it.npc) { kamfreena() } }
        onOpNpcU(CyclopsRooms.KAMFREENA) {
            val tier = Defenders.tierOf(it.objType.id)
            startDialogue(it.npc) { showKamfreena(tier) }
        }
        onOpNpc1(CyclopsRooms.LORELAI) { startDialogue(it.npc) { lorelai() } }
        onOpNpcU(CyclopsRooms.LORELAI) {
            val tier = Defenders.tierOf(it.objType.id)
            startDialogue(it.npc) { showLorelai(tier) }
        }
        onOpNpc1("npc.warguild_cyclops_pet") {
            startDialogue {
                chatNpcSpecific(
                    "Lorelai",
                    CyclopsRooms.LORELAI,
                    happy,
                    "Ahh I see you've met Rory. As a young Cyclops he's not ready for combat yet " +
                        "so he keeps me company.",
                )
            }
        }
    }

    private suspend fun Dialogue.kamfreena() {
        chatNpc(
            happy,
            "Why hello there! I'm Kamfreena. Like the look of my pets? I think they're eyeing you up.",
        )
        chatPlayer(confused, "That was a really bad pun.")
        chatNpc(
            sad,
            "Sorry... I don't get to see the rest of the guild much, stuck up here. The Cyclopes " +
                "don't talk much you see.",
        )
        chatPlayer(quiz, "Shouldn't that be cyclopses?")
        chatNpc(happy, "Nope! Cyclopes is the plural of cyclops. One cyclops, many cyclopes.")
        chatPlayer(neutral, "Oh, right, thanks.")
        kamfreenaQuestions()
    }

    private suspend fun Dialogue.kamfreenaQuestions() {
        when (
            choice4(
                "Where are they from?",
                Topic.From,
                "How did they get here?",
                Topic.Arrival,
                "Why are they here?",
                Topic.Purpose,
                "Bye!",
                Topic.Bye,
            )
        ) {
            Topic.From -> {
                chatPlayer(quiz, "Where are they from?")
                chatNpc(neutral, "They're from the far east lands.")
                kamfreenaQuestions()
            }
            Topic.Arrival -> {
                chatPlayer(quiz, "How did they get here?")
                chatNpc(
                    neutral,
                    "Ahh.. our guildmaster, Harrallak, went on an expedition there. He brought " +
                        "them back with him.",
                )
                kamfreenaQuestions()
            }
            Topic.Purpose -> {
                chatPlayer(quiz, "Why are they here?")
                chatNpc(
                    happy,
                    "For the warriors to train on of course! They also drop a rather nice blade.",
                )
                chatPlayer(quiz, "Oh? What would that be?")
                chatNpc(neutral, "Defenders.")
                chatPlayer(confused, "Err what are they?")
                chatNpc(
                    neutral,
                    "It's a blade you can defend with using your shield hand, like I have.",
                )
                chatPlayer(happy, "Wow!")
                chatNpc(
                    neutral,
                    "For every 10 tokens you collect around the guild, you can spend one minute " +
                        "in with my pets. As you get defenders you can show them to me to earn " +
                        "even better ones... but remember if you lose them you'll have",
                )
                chatNpc(neutral, "to start at bronze again. I'd advise keeping a spare in your bank.")
                chatPlayer(happy, "Okay!")
                chatNpc(
                    neutral,
                    "Oh, by the way, you'll need to earn 100 tokens before I'll let you in!",
                )
                chatPlayer(neutral, "Right, I'd better go play some games then!")
                kamfreenaQuestions()
            }
            Topic.Bye -> {
                chatPlayer(neutral, "Bye!")
                chatNpc(happy, "See you back here soon I hope!")
            }
        }
    }

    private suspend fun Dialogue.showKamfreena(tier: Int) {
        if (tier == Defenders.NONE) {
            chatNpc(neutral, "I don't need that.")
            return
        }
        Defenders.remember(player, tier)
        val name = WarguildEnums.warguild_metal_names.getValue(tier)
        val article = if (name.first() in "AEIOU") "an" else "a"
        chatPlayer(
            happy,
            "I have $article $name defender, please let me in to kill some more Cyclopes!",
        )
        when (tier) {
            0 -> chatNpc(neutral, "Bronze eh? The next cyclopes are ready, toddle in when you're ready.")
            1 -> chatNpc(neutral, "Wow, iron eh? The next cyclopes are ready, toddle in when you're ready.")
            2 ->
                chatNpc(
                    neutral,
                    "Strong as steel eh? The next cyclopes are ready, toddle in when you're ready.",
                )
            3 ->
                chatNpc(
                    happy,
                    "Black, hmmm, about half way to Rune then! Keep going! Cyclopes have been " +
                        "released. Enter when you're ready.",
                )
            4 ->
                chatNpc(
                    happy,
                    "Mithril? Getting good at this aren't you! Okay, I've released those cyclopes " +
                        "that sometimes drop adamant defenders. Enter when you're ready.",
                )
            5 ->
                chatNpc(
                    happy,
                    "An adamant defender eh? Trying for the Rune defender I'm betting. Okay, I've " +
                        "released those cyclopes. Enter when you're ready.",
                )
            Defenders.RUNE -> {
                chatNpc(
                    happy,
                    "Well done, you truly are a brilliant warrior! You should speak to my " +
                        "apprentice Lorelai in the basement for an additional challenge.",
                )
                chatNpc(
                    neutral,
                    "If you'd rather stay up here, I've released the cyclopes. Enter when you're " +
                        "ready.",
                )
            }
            else -> {
                chatNpc(happy, "Well done! You can only get more Dragon defenders from the basement.")
                chatNpc(
                    neutral,
                    "If you'd rather stay up here, I've released the cyclopes that can drop rune " +
                        "defenders. Enter when you're ready.",
                )
            }
        }
    }

    private suspend fun Dialogue.lorelai() {
        chatNpc(happy, "Hello! I'm Lorelai. Welcome to the Warriors' Guild basement!")
        chatPlayer(quiz, "What are you doing down here?")
        chatNpc(
            neutral,
            "I'm training this group of special Cyclopes to be stronger and more powerful for " +
                "Kamfreena.",
        )
        lorelaiQuestions()
    }

    private suspend fun Dialogue.lorelaiQuestions() {
        when (
            choice4(
                "What's special about these Cyclopes?",
                Topic.From,
                "How exactly are you training them?",
                Topic.Arrival,
                "Can I fight them?",
                Topic.Purpose,
                "Bye!",
                Topic.Bye,
            )
        ) {
            Topic.From -> {
                chatPlayer(quiz, "What's special about these Cyclopes?")
                chatNpc(
                    neutral,
                    "They have a chance of dropping a new, more powerful defender. Kamfreena " +
                        "doesn't want just anyone getting their hands on one so she moved them " +
                        "down here and tasked me with training them.",
                )
                lorelaiQuestions()
            }
            Topic.Arrival -> {
                chatNpc(neutral, "With a punishing routine of combat, fighting and the odd death!")
                chatPlayer(
                    confused,
                    "So, you just let them fight each other and hope they get stronger?",
                )
                chatNpc(
                    neutral,
                    "Of course not, that would just be cruel! Kamfreena and I only allow the most " +
                        "worthy of adventurers to fight these cyclopes.",
                )
                askToFight()
            }
            Topic.Purpose -> askToFight()
            Topic.Bye -> lorelaiBye()
        }
    }

    private suspend fun Dialogue.askToFight() {
        chatPlayer(happy, "I am a mighty adventurer! Can I fight them?")
        if (player.basementUnlocked) {
            chatNpc(neutral, "Yes, you have already proved yourself to me, the door is unlocked.")
            return
        }
        chatNpc(
            neutral,
            "You'll need to display your skills as a Warrior before I can allow you to enter the " +
                "room.",
        )
        chatPlayer(quiz, "How can I do that?")
        chatNpc(
            neutral,
            "Bring me a Rune Defender and I shall unlock the door for you, this shows me you have " +
                "dedicated enough time to fighting the Cyclops upstairs.",
        )
        when (choice2("I have a Rune Defender here.", Topic.Purpose, "Bye!", Topic.Bye)) {
            Topic.Purpose -> {
                chatPlayer(happy, "I have a Rune Defender here, please let me in!")
                if (Defenders.bestCarried(player) < Defenders.RUNE) {
                    chatNpc(
                        angry,
                        "And the Wise Old Man is my Dad... come back when you have the Rune " +
                            "Defender!",
                    )
                    return
                }
                player.basementUnlocked = true
                lorelaiUnlocks()
            }
            else -> lorelaiBye()
        }
    }

    private suspend fun Dialogue.lorelaiUnlocks() {
        chatNpc(
            happy,
            "Very well, I've unlocked the door but you'll still need 100 tokens from upstairs " +
                "before you can enter as well as 10 for every minute you want to stay inside. " +
                "Have fun in there!",
        )
    }

    private suspend fun Dialogue.lorelaiBye() {
        chatPlayer(neutral, "Bye!")
        chatNpc(neutral, "See you around.")
    }

    private suspend fun Dialogue.showLorelai(tier: Int) {
        when {
            tier == Defenders.DRAGON ->
                chatNpc(
                    happy,
                    "Congratulations you've found a Dragon defender! You're welcome to go back " +
                        "inside and try to get more.",
                )
            tier == Defenders.RUNE && player.basementUnlocked ->
                chatNpc(neutral, "You've already proved yourself to me, the door is unlocked.")
            tier == Defenders.RUNE -> {
                chatPlayer(happy, "I have a Rune Defender here, please let me in!")
                player.basementUnlocked = true
                lorelaiUnlocks()
            }
            else -> chatNpc(neutral, "I don't need that.")
        }
    }

    private enum class Topic {
        From,
        Arrival,
        Purpose,
        Bye,
    }
}
