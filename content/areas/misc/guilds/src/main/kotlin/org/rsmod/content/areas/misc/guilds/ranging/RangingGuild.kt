package org.rsmod.content.areas.misc.guilds.ranging

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.stat.rangedLvl
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class RangingGuild
@Inject
constructor(private val objRepo: ObjRepository, private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.ranging_guild_doorman") { startDialogue(it.npc) { doorman() } }
        onOpNpc1("npc.ranging_guild_guard") { startDialogue(it.npc) { guard() } }
        for (advisor in TOWER_ADVISORS) {
            onOpNpc1(advisor) { startDialogue(it.npc) { towerAdvisor() } }
        }
        onOpNpc1("npc.ranging_guild_shield_master") { startDialogue(it.npc) { shieldMaster() } }
        onOpNpc1("npc.ranging_guild_competition_judge") {
            startDialogue(it.npc) { competitionJudge() }
        }
        onOpNpc1("npc.ranging_guild_leatherworker") { startDialogue(it.npc) { leatherworker() } }
        onOpNpc1("npc.ranging_guild_bow_salesman") { startDialogue(it.npc) { bowSalesman(it.npc) } }
        onOpNpc3("npc.ranging_guild_bow_salesman") { player.openBowShop(it.npc) }
        onOpNpc1("npc.ranging_guild_tribal_salesman") {
            startDialogue(it.npc) { tribalSalesman(it.npc) }
        }
        onOpNpc3("npc.ranging_guild_tribal_salesman") { player.openTribalShop(it.npc) }
    }

    private fun Player.openBowShop(npc: Npc) {
        shops.open(this, npc, "Dargaud's Bow and Arrows.", "inv.ranging_guild_bowshop")
    }

    private fun Player.openTribalShop(npc: Npc) {
        shops.open(this, npc, "Authentic Throwing Weapons.", "inv.ranging_guild_tribalshop")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ranging_Guild_Doorman
    private suspend fun Dialogue.doorman() {
        chatPlayer(happy, "Hello there.")
        chatNpc(
            neutral,
            "Greetings. If you are an experienced archer, you may want to visit the guild here...",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Guard_(Ranging_Guild)
    private suspend fun Dialogue.guard() {
        chatPlayer(happy, "Hello there.")
        chatNpc(happy, "Greetings, traveller. Enjoy your time at the Ranging Guild.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Tower_Advisor
    // TODO(content): [verify:in_game] whether the advice uses the boosted or the base Ranged level
    private suspend fun Dialogue.towerAdvisor() {
        chatPlayer(quiz, "Hello there, what do you do here?")
        chatNpc(happy, "Hi. We are in charge of this practice area.")
        chatPlayer(quiz, "This is a practice area?")
        chatNpc(
            neutral,
            "Surrounding us are four towers. Each tower contains trained archers of a different " +
                "level. You'll notice it's quite a distance, so you'll need a longbow.",
        )
        val level = player.rangedLvl
        when {
            level <= 50 ->
                chatNpc(
                    neutral,
                    "As you're not very skilled, so I advise you to practise on the north tower. " +
                        "That'll provide the best challenge for you.",
                )
            level <= 60 ->
                chatNpc(
                    neutral,
                    "You seem to have some skill, so I advise you to practice on the east tower. " +
                        "That'll provide the best challenge for you.",
                )
            level <= 70 ->
                chatNpc(
                    neutral,
                    "You appear to be fairly skilled with a bow, so I advise you to practice on " +
                        "the south tower. That'll provide the best challenge for you.",
                )
            else ->
                chatNpc(
                    happy,
                    "Looks like you're very skilled, so I advise you to practice on the west " +
                        "tower. That'll provide the best challenge for you.",
                )
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Shield_master
    private suspend fun Dialogue.shieldMaster() {
        chatPlayer(happy, "Hello there.")
        chatNpc(happy, "Greetings adventurer.")
        val option =
            choice4(
                "What's with all the shields?",
                1,
                "Who are you?",
                2,
                "May I have a resource flyer?",
                3,
                "Bye!",
                4,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "What's with all the shields?")
                chatNpc(
                    neutral,
                    "Although archers like myself like to keep distance from our enemies, it's " +
                        "always good to have a bit of protection.",
                )
                chatPlayer(quiz, "Very sensible! Did you make these yourself?")
                chatNpc(happy, "I am a bit of a craftsman, I indeed made all these shields.")
                chatPlayer(quiz, "How do you make them?")
                chatNpc(
                    neutral,
                    "It's very simple! First you'll need to be skilled in crafting and " +
                        "fletching, the exact details you can find in your skill guide.",
                )
                chatNpc(
                    neutral,
                    "For every shield, you'll require some tools! A knife for fletching the wood " +
                        "and a hammer for crafting the shield.",
                )
                chatNpc(
                    neutral,
                    "As for resources, you'll need 2 logs, 2 pieces of tanned leather and 15 " +
                        "nails.",
                )
                chatPlayer(
                    quiz,
                    "Where can I find out more information about the resources needed?",
                )
                chatNpc(
                    happy,
                    "I have a flyer! It details all the shields and the resources needed for them.",
                )
                resourceFlyer()
            }
            2 -> {
                chatPlayer(quiz, "Who are you?")
                chatNpc(
                    happy,
                    "My name is William! And I'll tell you this, I'm the best crossbowman this " +
                        "side of White Wolf Mountain.",
                )
                chatPlayer(quiz, "Who's better than you on the other side of the mountain?")
                chatNpc(neutral, "Couldn't tell you that myself, never been there.")
                chatPlayer(quiz, "Oh... How did you come across that crossbow then?")
                chatNpc(
                    neutral,
                    "Oh this? Some wandering merchant came by recently, told me it was the " +
                        "stealthiest crossbow there was.",
                )
                chatPlayer(quiz, "Is it?")
                chatNpc(
                    neutral,
                    "Well, he told me it was his last one too, so I figured I'd give it a try.",
                )
                chatPlayer(happy, "It was nice meeting you William.")
                chatNpc(laugh, "I bet!")
            }
            3 -> resourceFlyer()
            4 -> {
                chatPlayer(happy, "Bye!")
                chatNpc(neutral, "Stay protected adventurer, for it's dangerous out there.")
            }
        }
    }

    // TODO(content): [verify:in_game] what happens with a full inventory; the transcript is silent
    private suspend fun Dialogue.resourceFlyer() {
        chatPlayer(quiz, "May I have a resource flyer?")
        if (FLYER in player.inv) {
            chatNpc(neutral, "You already have a flyer, I've only so many left.")
            chatPlayer(neutral, "Fair enough.")
            return
        }
        chatNpc(happy, "Of course! The more you know, the better!")
        player.invAddOrDrop(objRepo, FLYER)
        objbox(FLYER, "The Shield Master hands you 'Leather Shields - Resource information flyer'.")
        chatPlayer(happy, "Thanks!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Competition_Judge
    // TODO(content): [system:archery_competition] "Sure, I'll give it a go." takes 200 coins and
    //  hands out 10 bronze arrows; the target scoring, tickets and the "Hello again" dialogue
    private suspend fun Dialogue.competitionJudge() {
        chatNpc(
            happy,
            "Hello there, would you like to take part in the archery competition? It only costs " +
                "200 coins to enter.",
        )
        val option =
            choice3("Sure, I'll give it a go.", 1, "What are the rules?", 2, "No thanks.", 3)
        when (option) {
            1 -> chatPlayer(happy, "Sure, I'll give it a go.")
            2 -> {
                chatPlayer(quiz, "What are the rules?")
                chatNpc(neutral, "The rules are very simple:")
                chatNpc(
                    neutral,
                    "You're given 10 shots at the targets, for each hit you will receive points. " +
                        "At the end you'll be awarded 1 ticket for every 10 points.",
                )
                chatNpc(
                    neutral,
                    "The tickets can be exchanged for goods from our stores. Do you want to give " +
                        "it a go? Only 200 coins.",
                )
                when (choice2("Sure, I'll give it a go.", 1, "No thanks.", 2)) {
                    1 -> chatPlayer(happy, "Sure, I'll give it a go.")
                    2 -> chatPlayer(neutral, "No thanks.")
                }
            }
            3 -> chatPlayer(neutral, "No thanks.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Leatherworker
    // TODO(content): [skill:crafting] "I'd like you to tan something for me." when holding
    //  tannable hides (needs the crafting module's held-hides check made public)
    private suspend fun Dialogue.leatherworker() {
        chatPlayer(happy, "Hello.")
        chatNpc(neutral, "Can I help you?")
        while (true) {
            when (choice2("What do you do here?", 1, "No thanks.", 2)) {
                1 -> {
                    chatPlayer(quiz, "What do you do here?")
                    chatNpc(
                        neutral,
                        "Well, I can cure plain cowhides into pieces of leather ready for " +
                            "crafting.",
                    )
                    chatNpc(
                        neutral,
                        "I work with ordinary, hard or dragonhide leather and also snakeskin.",
                    )
                    if (!leatherTypes()) {
                        return
                    }
                }
                2 -> {
                    chatPlayer(neutral, "No thanks.")
                    chatNpc(neutral, "Suit yourself.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.leatherTypes(): Boolean {
        val option =
            choice5(
                "Ordinary leather.",
                1,
                "Hard leather.",
                2,
                "Snakeskin.",
                3,
                "Dragonhide leather.",
                4,
                "I'm bored of leather already.",
                5,
                title = "What would you like to know about?",
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "Ordinary leather.")
                chatNpc(
                    neutral,
                    "I can work one piece of cow hide into ordinary leather. It only costs one " +
                        "piece of gold.",
                )
            }
            2 -> {
                chatPlayer(quiz, "Hard leather.")
                chatNpc(
                    neutral,
                    "Hard leather is made by specially treating normal leather by wetting and " +
                        "drying several times.",
                )
                chatNpc(
                    neutral,
                    "It's then treated with pine oils. It's harder wearing than ordinary leather.",
                )
            }
            3 -> {
                chatPlayer(quiz, "Snake skins.")
                chatNpc(
                    neutral,
                    "I can work one piece of snake hide into a snakeskin. It only costs 15 " +
                        "pieces of gold. Snakeskin armour is great for intermediate level " +
                        "rangers and provides more defence than hard and studded leather.",
                )
            }
            4 -> {
                chatPlayer(quiz, "Dragonhide leather.")
                chatNpc(
                    neutral,
                    "Dragon leather is made from dragonhides. One piece of hide can be worked " +
                        "into one piece of dragon leather. It costs 20 coins each piece.",
                )
                chatNpc(
                    neutral,
                    "Dragon leather is hard-wearing and has excellent flexibility. You can make " +
                        "strong and supple armour with this.",
                )
            }
            5 -> {
                chatPlayer(neutral, "I'm bored of leather already.")
                chatNpc(
                    laugh,
                    "You may be bored now, but an arrow in the backside will soon fix that!",
                )
                return false
            }
        }
        chatNpc(neutral, "Is there anything else I can help you with?")
        return true
    }

    // https://oldschool.runescape.wiki/w/Transcript:Bow_and_Arrow_salesman
    private suspend fun Dialogue.bowSalesman(npc: Npc) {
        chatPlayer(happy, "Hello.")
        chatNpc(happy, "A fair day, traveller. Would you like to see my wares?")
        val option =
            choice3(
                "Yes please.",
                1,
                "I'd like to ask you about magic crossbows.",
                2,
                "No thanks.",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(happy, "Yes please.")
                player.openBowShop(npc)
            }
            2 -> {
                chatPlayer(quiz, "I'd like to ask you about magic crossbows.")
                chatNpc(
                    neutral,
                    "Ahh crossbows. Not exactly what I'd call skilled ranging, but I guess " +
                        "dwarven engineering is good, what did you want to know?",
                )
                chatPlayer(
                    quiz,
                    "The dwarves don't work with magic logs, I'm wondering if you know how to " +
                        "make a crossbow out of them?",
                )
                chatNpc(
                    neutral,
                    "As it happens, a recent discovery of dragon limbs has allowed the creation " +
                        "of such a crossbow.",
                )
                chatPlayer(happy, "That's fantastic!")
            }
            3 -> {
                chatPlayer(neutral, "No thanks.")
                chatNpc(neutral, "Okay good day to you.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Tribal_Weapon_Salesman
    private suspend fun Dialogue.tribalSalesman(npc: Npc) {
        chatPlayer(happy, "Hello there.")
        chatNpc(happy, "Greetings, traveller. Are you interested in any throwing weapons?")
        when (choice2("Yes I am.", 1, "Not really.", 2)) {
            1 -> {
                chatPlayer(happy, "Yes I am.")
                chatNpc(happy, "That is a good thing.")
                player.openTribalShop(npc)
            }
            2 -> {
                chatPlayer(neutral, "Not really.")
                chatNpc(neutral, "No bother to me.")
            }
        }
    }

    private companion object {
        const val FLYER = "obj.leather_shield_info"
        val TOWER_ADVISORS =
            listOf(
                "npc.ranging_advisor1",
                "npc.ranging_advisor2",
                "npc.ranging_advisor3",
                "npc.ranging_advisor4",
            )
    }
}
