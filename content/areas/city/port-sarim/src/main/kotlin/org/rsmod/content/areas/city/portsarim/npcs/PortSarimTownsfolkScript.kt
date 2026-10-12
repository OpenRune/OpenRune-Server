package org.rsmod.content.areas.city.portsarim.npcs

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.baseSmithingLvl
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.other.pets.cats.CatDialogue
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class PortSarimTownsfolkScript @Inject constructor(private val cats: CatDialogue) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.thurgo") { startDialogue(it.npc) { thurgo() } }
        onTalkAcross("npc.macro_pirate") { startDialogue(it) { capnHand() } }
        onOpNpc1("npc.jail_guard_sleeping") { sleepingGuard(it.npc) }
        onOpNpc1("npc.jail_guard_lookout") { startDialogue(it.npc) { lookoutGuard() } }
        onOpNpc1("npc.wormbrain") { startDialogue(it.npc) { wormbrain() } }
        onOpNpc1("npc.vc_bellemorde") { startDialogue(it.npc) { bellemorde() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Guard_(Port_Sarim)
    private suspend fun Dialogue.lookoutGuard() {
        chatNpc(angry, "HALT! Who goes there?")
        val name = player.displayName
        when (
            choice3(
                "Don't worry, I'm not going to cause trouble.",
                1,
                "I am $name the Mighty!",
                2,
                "No-one, there's no-one here.",
                3,
            )
        ) {
            1 -> {
                chatPlayer(neutral, "Don't worry, I'm not going to cause trouble.")
                chatNpc(angry, "But you shouldn't be here - be off with you!")
                chatPlayer(neutral, "I was going anyway.")
            }
            2 -> {
                chatPlayer(happy, "I am $name the Mighty!")
                chatNpc(
                    laugh,
                    "Mighty? You look like another of those silly adventurers who thinks they're " +
                        "the bee's knees just because they've done a few lousy quests!",
                )
                chatPlayer(
                    angry,
                    "Well it sounds better than sitting on this rooftop all day looking at trees!",
                )
                chatNpc(angry, "I'll have you know it's a very important job guarding this jail!")
                chatNpc(
                    neutral,
                    "If anyone comes sneaking in here to mess around with the prisoners, the " +
                        "lads downstairs will make mincemeat of them, and I'll be here to pick " +
                        "them off if they try to escape.",
                )
                chatPlayer(
                    quiz,
                    "You mean people aren't meant to be able to shoot the prisoners in the cells?",
                )
                chatNpc(neutral, "Yes, that's right.")
                chatPlayer(neutral, "Okay, it's been nice talking to you.")
            }
            3 -> {
                chatPlayer(shifty, "No-one, there's no-one here.")
                chatNpc(confused, "What? I can see you!")
                chatPlayer(
                    shifty,
                    "No, you're just imagining it. Perhaps you've been up here in the sun for " +
                        "too long?",
                )
                chatNpc(confused, "So who am I talking to?")
                chatPlayer(
                    worried,
                    "Oh dear, you've started talking to yourself. That's a common sign that " +
                        "you're going mad!",
                )
                chatNpc(confused, "But... but... you're standing right there...")
                mesbox("Maybe you should leave him alone now.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Wormbrain
    // TODO(content): [quest:dragon_slayer_i] the map piece
    private suspend fun Dialogue.wormbrain() {
        chatNpc(angry, "Whut you want?")
        if (!choice2("What are you in for?", true, "Sorry, thought this was a zoo.", false)) {
            chatPlayer(neutral, "Sorry, thought this was a zoo.")
            return
        }
        chatPlayer(quiz, "What are you in for?")
        chatNpc(confused, "Me not sure. Me pick some stuff up and take it away.")
        chatPlayer(quiz, "Well, did the stuff belong to you?")
        chatNpc(confused, "Umm...no.")
        chatPlayer(neutral, "Well, that would be why then.")
        chatNpc(neutral, "Oh, right.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Bellemorde
    private suspend fun Dialogue.bellemorde() {
        chatPlayer(happy, "Hello puss.")
        if (!cats.catspeak(player)) {
            chatNpc(angry, "Hiss!")
            return
        }
        chatNpc(neutral, "Hello human.")
        chatPlayer(quiz, "Would you like a fish?")
        chatNpc(neutral, "I don't want your fish. I hunt and eat what I need by myself.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Thurgo
    // TODO(content): [quest:the_knights_sword] quest stages
    // TODO(content): [quest:below_ice_mountain] "I've met another Imcando dwarf."
    // TODO(content): [quest:prying_times] "Can you help me make another crowbar?"
    // TODO(content): [system:wise_old_man_tasks] the Wise Old Man's letter
    private suspend fun Dialogue.thurgo() {
        if (!QuestRequirements.hasCompleted(player, KNIGHTS_SWORD)) {
            thurgoCape()
            return
        }
        while (true) {
            val options = buildList {
                add("Can you make me another of Sir Vyvin's swords?" to ThurgoTopic.Sword)
                if (access.inv.count(REDBERRY_PIE) > 0) {
                    add("Would you like a redberry pie?" to ThurgoTopic.Pie)
                }
                if (player.baseSmithingLvl >= MAX_LEVEL) {
                    add("Can we talk about skillcapes?" to ThurgoTopic.Skillcape)
                } else {
                    add("What is that cape you're wearing?" to ThurgoTopic.Cape)
                }
            }
            when (menu(options)) {
                ThurgoTopic.Sword -> return thurgoSword()
                ThurgoTopic.Pie -> thurgoPie()
                ThurgoTopic.Skillcape -> return thurgoSkillcape()
                ThurgoTopic.Cape -> return thurgoCape()
            }
        }
    }

    private suspend fun Dialogue.thurgoSword() {
        chatPlayer(happy, "Can you make me another of Sir Vyvin's swords?")
        chatNpc(
            quiz,
            "You want that knight's sword again? I suppose you brought me a lovely pie, so I " +
                "don't mind. I'll need a blurite ore and two iron bars, like before.",
        )
        val inv = access.inv
        if (inv.count(BLURITE_ORE) < 1 || inv.count(IRON_BAR) < 2) {
            chatPlayer(happy, "Okay, I'll go and find that.")
            return
        }
        access.invDel(inv, BLURITE_ORE, 1)
        access.invDel(inv, IRON_BAR, 2)
        access.invAdd(inv, BLURITE_SWORD)
        objbox(BLURITE_SWORD, zoom = 400, "Thurgo makes you another sword.")
    }

    private suspend fun Dialogue.thurgoPie() {
        chatPlayer(happy, "Would you like a redberry pie?")
        mesbox("You see Thurgo's eyes light up.")
        chatNpc(
            happy,
            "I'd never say no to a redberry pie! We Imcando dwarves love them - they're GREAT!",
        )
        if (access.invDel(access.inv, REDBERRY_PIE, 1).failure) {
            return
        }
        objbox(
            REDBERRY_PIE,
            zoom = 400,
            "You hand over the pie. Thurgo eats the pie. Thurgo pats his stomach.",
        )
        chatNpc(
            happy,
            "By Guthix! THAT was good pie! Anyone who makes pie like THAT has got to be alright!",
        )
    }

    private suspend fun Dialogue.thurgoCape() {
        chatPlayer(quiz, "What is that cape you're wearing?")
        chatNpc(
            happy,
            "It's a Skillcape of Smithing. It shows that I'm a master blacksmith, but that's only " +
                "to be expected - after all, my ancestors were the greatest blacksmiths in " +
                "dwarven history.",
        )
        chatNpc(
            happy,
            "If you ever achieve level 99 Smithing you'll be able to wear a cape like this, and " +
                "receive more experience when smelting gold ore.",
        )
    }

    private suspend fun Dialogue.thurgoSkillcape() {
        chatPlayer(quiz, "Can we talk about skillcapes?")
        chatNpc(neutral, "What do you need, human?")
        chatPlayer(
            quiz,
            "Now that I am so skilled at Smithing, can I buy a Skillcape of Smithing from you?",
        )
        chatNpc(
            happy,
            "I reckon so; we master smiths must stick together! I'll give it to you for just " +
                "99000 coins.",
        )
        if (!choice2("Sorry, that's too much money.", false, "Of course I'll pay you.", true)) {
            chatPlayer(sad, "Sorry, that's too much money.")
            chatNpc(
                neutral,
                "Too much money? A smith of your calibre should be able to make that amount in " +
                    "an hour!",
            )
            return
        }
        chatPlayer(happy, "Of course I'll pay you.")
        val inv = access.inv
        if (inv.count("obj.coins") < CAPE_PRICE) {
            // TODO(content): [verify:in_game] the wiki is missing the line without enough coins
            chatPlayer(sad, "But, unfortunately, I don't have enough money with me.")
            return
        }
        if (inv.freeSpace() < 2) {
            chatNpc(
                neutral,
                "All Skillcapes come with a free hood. It's part of a deal: buy one get one " +
                    "free, you know. So you'll need to free up some inventory space before I can " +
                    "sell you one.",
            )
            return
        }
        if (access.invDel(inv, "obj.coins", count = CAPE_PRICE).failure) {
            return
        }
        access.invAdd(inv, "obj.skillcape_smithing")
        access.invAdd(inv, "obj.skillcape_smithing_hood")
        chatNpc(happy, "Excellent! Wear that cape with pride my friend.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Cap'n_Hand
    private suspend fun Dialogue.capnHand() {
        chatNpc(confused, "Arrr, what do ye want?")
        if (!choice2("What are you doing here?", true, "Nothing, thanks.", false)) {
            chatPlayer(neutral, "Nothing, thanks.")
            chatNpc(sad, "Nothing, eh? Nothing be all I got, anyway.")
            return
        }
        chatPlayer(quiz, "What are you doing here?")
        chatNpc(
            sad,
            "Arrr, it be a sad tale. I used to sail the seven seas, a-lootin' and a-plunderin' " +
                "whatever took my fancy.",
        )
        chatNpc(
            neutral,
            "Then I got a conscience, so I stopped being a pirate, and travelled the world " +
                "giving back some of the loot I'd taken.",
        )
        chatNpc(
            sad,
            "Seems people didn't appreciate me interruptin' their business to give 'em free " +
                "stuff, so now I'm locked up for harassment.",
        )
        chatPlayer(sad, "Oh, I see.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Guard_(Port_Sarim,_sleeping)
    private suspend fun ProtectedAccess.sleepingGuard(npc: Npc) {
        npc.say(SLEEP_TALK[random.of(SLEEP_TALK.size)])
        startDialogue(npc) { chatPlayer(neutral, "Maybe I should let him sleep.") }
    }

    private enum class ThurgoTopic {
        Sword,
        Pie,
        Skillcape,
        Cape,
    }

    private companion object {
        const val MAX_LEVEL = 99
        const val CAPE_PRICE = 99_000
        const val KNIGHTS_SWORD = "quest_knightssword"
        const val REDBERRY_PIE = "obj.redberry_pie"
        const val BLURITE_ORE = "obj.blurite_ore"
        const val IRON_BAR = "obj.iron_bar"
        const val BLURITE_SWORD = "obj.faladian_sword"

        val SLEEP_TALK =
            listOf(
                "Guh... mwww... zzzzz...",
                "Hmph... heh heh heh...",
                "Mmmm... big pint of beer... kebab...",
                "Mmmmmm... donuts...",
            )
    }
}
