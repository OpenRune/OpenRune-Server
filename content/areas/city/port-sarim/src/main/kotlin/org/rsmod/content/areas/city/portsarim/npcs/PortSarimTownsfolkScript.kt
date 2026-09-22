package org.rsmod.content.areas.city.portsarim.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.stat.baseSmithingLvl
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class PortSarimTownsfolkScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.thurgo") { startDialogue(it.npc) { thurgo() } }
        onTalkAcross("npc.macro_pirate") { startDialogue(it) { capnHand() } }
        onOpNpc1("npc.jail_guard_sleeping") { startDialogue(it.npc) { sleepingGuard() } }
    }

    private suspend fun Dialogue.thurgo() {
        if (!QuestRequirements.hasCompleted(player, KNIGHTS_SWORD)) {
            if (player.baseSmithingLvl >= MAX_LEVEL) thurgoSkillcape() else thurgoCape()
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

    private suspend fun Dialogue.sleepingGuard() {
        chatNpc(mesanim("mesanim.sleep"), "Guh... mwww... zzzzz...")
        chatPlayer(neutral, "Maybe I should let him sleep.")
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
    }
}
