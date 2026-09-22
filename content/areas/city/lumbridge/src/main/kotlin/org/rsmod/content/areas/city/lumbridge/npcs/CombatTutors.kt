package org.rsmod.content.areas.city.lumbridge.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpcU
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.content.quest.manager.menu
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private var Player.lastTutorClaim by intVarp("varp.lumbridge_tutor_claim")

/** Harlan, Nemarti and Mikasi, the combat tutors south of the Lumbridge general store. */
class CombatTutors : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(MELEE) { startDialogue(it.npc) { melee() } }
        onOpNpcU(MELEE) {
            val item = it.objType.internalName
            startDialogue(it.npc) {
                when (item) {
                    SWORD -> returnTrainingItem(SWORD, "sword")
                    SHIELD -> returnTrainingItem(SHIELD, "shield")
                    else -> chatNpc(neutral, "I have no use for that, sorry.")
                }
            }
        }
        onOpNpc1(RANGED) { startDialogue(it.npc) { ranged() } }
        onOpNpc3(RANGED) { startDialogue(it.npc) { claimArrows() } }
        onOpNpc1(MAGIC) { startDialogue(it.npc) { magic() } }
        onOpNpc3(MAGIC) { startDialogue(it.npc) { claimRunes() } }
    }

    private fun Dialogue.owns(item: String): Boolean =
        access.inv.count(item) > 0 || access.player.worn.count(item) > 0 || access.bank.count(item) > 0

    private suspend fun Dialogue.melee() {
        if (access.statBase("stat.defence") >= MAX_LEVEL) {
            defenceCape()
            return
        }
        chatNpc(
            quiz,
            "Greetings adventurer, I am the Melee combat tutor. Is there anything I can do for you?",
        )
        meleeMenu()
    }

    private suspend fun Dialogue.meleeMenu() {
        while (true) {
            val topic =
                menu(
                    buildList {
                        add("Tell me about melee combat." to 1)
                        add("Tell me about different weapon types I can use." to 2)
                        add("Tell me about skillcapes." to 3)
                        if (!owns(SWORD) || !owns(SHIELD)) add("I'd like a training sword and shield." to 4)
                        add("Goodbye." to 5)
                    }
                )
            when (topic) {
                1 -> meleeCombat()
                2 -> {
                    chatPlayer(quiz, "Tell me about different weapon types I can use")
                    chatNpc(
                        neutral,
                        "Well let me see now...There are stabbing type weapons such as daggers, then you " +
                            "have swords which are slashing, maces that have great crushing abilities, " +
                            "battle axes which are powerful.",
                    )
                    chatNpc(
                        neutral,
                        "It depends a lot on how you want to fight. Experiment and find out what is best " +
                            "for you. Never be scared to try out a new weapon; you never know, you might " +
                            "like it!",
                    )
                    chatNpc(neutral, "While I tried all of them for a while, I settled on this rather good sword.")
                    chatNpc(
                        neutral,
                        "You might also find that different weapon types are more accurate against " +
                            "different monsters.",
                    )
                    chatNpc(quiz, "Is there anything else you would like to know?")
                }
                3 -> {
                    chatPlayer(quiz, "Tell me about skillcapes.")
                    chatNpc(
                        neutral,
                        "Of course. Skillcapes are a symbol of achievement. Only people who have mastered " +
                            "a skill and reached level 99 can get their hands on them and gain the " +
                            "benefits they carry.",
                    )
                    chatNpc(
                        neutral,
                        "The Cape of Defence will act as ring of life, saving you from combat if your " +
                            "hitpoints become low.",
                    )
                    chatNpc(quiz, "Is there anything else you would like to know?")
                }
                4 -> trainingKit()
                else -> {
                    chatPlayer(neutral, "Goodbye.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.meleeCombat() {
        chatPlayer(neutral, "Tell me about melee combat.")
        chatNpc(
            neutral,
            "Well adventurer, the first thing you will need is a sword and a shield appropriate for " +
                "your level.",
        )
        access.toplevelSidebuttonSwitch(WORN_TAB)
        chatNpc(
            neutral,
            "Make sure to equip your sword and shield. Click on them in your inventory, they will " +
                "disappear from your inventory and move to your worn items. You can see your worn " +
                "items in the worn items tab here.",
        )
        access.toplevelSidebuttonSwitch(COMBAT_TAB)
        chatNpc(
            neutral,
            "When you are wielding your sword you will then be able to see the correct options in " +
                "the combat interface.",
        )
        chatNpc(
            neutral,
            "There are four different melee styles. Accurate, aggressive, defensive and controlled. " +
                "Not all weapons will have all four styles though.",
        )
        chatPlayer(quiz, "Interesting, what does each style do?")
        chatNpc(
            neutral,
            "Well I am glad you asked. The accurate style will give you experience points in your " +
                "Attack skill, you will also find you will deal damage more frequently as a result of " +
                "being, well, more accurate.",
        )
        chatNpc(
            neutral,
            "Next we have the aggressive style. This style will give you experience points in your " +
                "Strength skill. When using this style you will notice that your attacks will hit a " +
                "little harder.",
        )
        chatNpc(
            neutral,
            "Now for the defensive style, this style will give you experience points in your " +
                "Defensive skill. When using this style you will notice that you get hit less often.",
        )
        chatNpc(
            neutral,
            "Finally, we have the controlled style. This style will give you the same amount of " +
                "experience as the other styles would but shared across all three of the combat skills.",
        )
        chatNpc(
            neutral,
            "If you were using the training sword for example, there are four different attack types. " +
                "Stab, lunge, slash and block.",
        )
        chatNpc(
            neutral,
            "Each type uses one of the attack styles. Stab uses accurate, lunge and slash use " +
                "aggressive and block uses defensive.",
        )
        chatNpc(
            neutral,
            "To find out which style an attack type uses, hover your mouse cursor over the style button.",
        )
        chatNpc(quiz, "Is there anything else you would like to know?")
        if (menu("What if I wanted to fight something a bit more... human." to true, "Something else." to false)) {
            fightingPlayers()
        }
    }

    private suspend fun Dialogue.fightingPlayers() {
        chatPlayer(shifty, "What if I wanted to fight something a bit more... human.")
        chatNpc(neutral, "Well adventurer, there are a few places you might be able to do this.")
        chatNpc(
            neutral,
            "You could try your luck at castle wars. Here, two teams fight each other to defend their " +
                "respective flags. To win the game you will need to get the other team's flag and " +
                "return it to your flag stand.",
        )
        chatPlayer(happy, "Capture the flag, sounds like a lot of fun.")
        chatNpc(
            neutral,
            "If you are in a clan, you should gather some clan members and try out clan wars. There " +
                "you can see which clan is better than the other by fighting each other in an arena.",
        )
        chatNpc(
            neutral,
            "Both activities are safe minigame, which means if you die you will not loose any of your " +
                "items. You can get to them by using the teleport option in your minigames tab.",
        )
        chatNpc(
            neutral,
            "There is also the wilderness. The wilderness is north of Varrock and you can fight other " +
                "players there. But bare in mind if you die to another player in the wilderness you " +
                "will lose your stuff.",
        )
        chatNpc(
            neutral,
            "But this also means that if you kill another player you will be able to take their stuff " +
                "too.",
        )
        chatNpc(
            neutral,
            "Only go into the wilderness with items you are willing to lose and pay attention to the " +
                "wilderness level you are in. The higher the level you go, more player will be able to " +
                "attack you.",
        )
        chatNpc(neutral, "You can find which player can attack you by checking your combat level.")
        chatNpc(
            neutral,
            "Minus the wilderness level from your combat level to find the lowest level that you can " +
                "attack, then add the wilderness level to your combat level to find the highest level " +
                "that you can attack.",
        )
        chatNpc(quiz, "Is there anything else you would like to know?")
    }

    private suspend fun Dialogue.trainingKit() {
        chatPlayer(neutral, "I'd like a training sword and shield.")
        val needSword = !owns(SWORD)
        val needShield = !owns(SHIELD)
        val needed = listOfNotNull(SWORD.takeIf { needSword }, SHIELD.takeIf { needShield })
        if (access.inv.freeSpace() < needed.size) {
            chatNpc(neutral, "You don't have enough room in your inventory for them.")
            return
        }
        needed.forEach { access.invAdd(access.inv, it) }
        when {
            needSword && needShield ->
                doubleobjbox(SHIELD, SWORD, "Harlan gives you a Training sword and shield.")
            needSword -> objbox(SWORD, "Harlan gives you Training sword.")
            else -> objbox(SHIELD, "Harlan gives you a Training shield.")
        }
        chatNpc(neutral, "There you go, use it well.")
        chatNpc(quiz, "Is there anything else I can help you with?")
    }

    private suspend fun Dialogue.returnTrainingItem(item: String, name: String) {
        chatPlayer(neutral, "I no longer need my Training $name, would you like it back?")
        chatNpc(
            happy,
            "Sure thing, I can give this to another adventurer who is in need of training.",
        )
        access.invDel(access.inv, item)
        objbox(item, "You give back you training $name to the Melee combat tutor.")
    }

    private suspend fun Dialogue.defenceCape() {
        chatNpc(
            happy,
            "Ah, but I can see you're already a master in the fine art of Defence. Perhaps you have " +
                "come to me to purchase a Skillcape of Defence, and thus join the elite few who have " +
                "mastered this exacting skill?",
        )
        chatNpc(
            neutral,
            "In recognition of your defensive abilities, when you have it equipped it will act as ring " +
                "of life, saving you from combat if your hitpoints become low.",
        )
        val buy =
            menu(
                "Yes, please sell me a Skillcape of Defence." to true,
                "Can you tell me about different weapon types I can use?" to false,
            )
        if (!buy) {
            meleeMenu()
            return
        }
        chatPlayer(quiz, "May I buy a Skillcape of Defence, please?")
        chatNpc(
            neutral,
            "You wish to join the elite defenders of this world? I'm afraid such things do not come " +
                "cheaply - in fact they cost $CAPE_PRICE coins, to be precise!",
        )
        val pay =
            menu(
                "$CAPE_PRICE coins? That's much too expensive." to false,
                "I think I have the money right here, actually." to true,
            )
        if (!pay) {
            chatPlayer(shocked, "$CAPE_PRICE coins? That's much too expensive.")
            chatNpc(
                neutral,
                "Not at all; there are many other adventurers who would love the opportunity to " +
                    "purchase such a prestigious item! You can find me here if you change your mind.",
            )
            return
        }
        chatPlayer(happy, "I think I have the money right here, actually.")
        if (access.inv.count(COINS) < CAPE_PRICE) {
            chatPlayer(sad, "But, unfortunately, I was mistaken.")
            chatNpc(neutral, "Well, come back and see me when you do.")
            return
        }
        if (access.inv.freeSpace() < 2) {
            chatNpc(
                neutral,
                "Unfortunately all Skillcapes are only available with a free hood, it's part of a " +
                    "skill promotion deal; buy one get one free, you know. So you'll need to free up some " +
                    "inventory space before I can sell you one.",
            )
            return
        }
        access.invDel(access.inv, COINS, CAPE_PRICE)
        val trimmed = SKILLS.count { access.statBase(it) >= MAX_LEVEL } > 1
        access.invAdd(access.inv, if (trimmed) "obj.skillcape_defence_trimmed" else "obj.skillcape_defence")
        access.invAdd(access.inv, "obj.skillcape_defence_hood")
        chatNpc(happy, "Excellent! Wear that cape with pride my friend.")
    }

    private suspend fun Dialogue.ranged() {
        chatNpc(
            quiz,
            "Hey there adventurer, I am the Ranged combat tutor. Is there anything you would like to " +
                "know?",
        )
        var asked = setOf<Int>()
        while (true) {
            val topic =
                menu(
                    buildList {
                        if (1 !in asked) add("How can I train my Ranged?" to 1)
                        if (2 !in asked) add("How do I create a bow and arrows?" to 2)
                        add("Let's discuss how my ammo acts when I pick it up." to 3)
                        add("Goodbye." to 4)
                    }
                )
            when (topic) {
                1 -> if (trainRanged()) return
                2 -> fletching()
                3 -> ammoPickup()
                else -> {
                    chatPlayer(neutral, "Goodbye.")
                    return
                }
            }
            asked = asked + topic
        }
    }

    /** Returns true when the player takes the map directions, which ends the conversation. */
    private suspend fun Dialogue.trainRanged(): Boolean {
        chatPlayer(quiz, "How can I train my Ranged?")
        val level = access.statBase("stat.ranged")
        if (level < EXPERIENCED_LEVEL) {
            chatNpc(
                neutral,
                "To start with you'll need a bow and arrows, you were given a Shortbow and some arrows " +
                    "when you arrived here from Tutorial island.",
            )
            chatNpc(neutral, "Alternatively, you can claim a training bow and some arrows from me.")
            chatNpc(
                neutral,
                "Mikasi, the Magic Combat tutor and I both give out items every 30 minutes, however you " +
                    "must choose whether you want runes or ranged equipment.",
            )
            chatNpc(
                neutral,
                "To claim the Training bow and arrows, right-click on me and choose Claim, to claim runes " +
                    "right-click on the Magic Combat tutor and select Claim.",
            )
            access.toplevelSidebuttonSwitch(SKILLS_TAB)
            chatNpc(
                neutral,
                "Not all bows can use every type of arrow, most bows have a limit. You can find out your " +
                    "bows limit by checking the Ranged skill guide.",
            )
            chatNpc(
                neutral,
                "If you do decide to use the Training bow, you will only be able to use the Training " +
                    "arrows with it. Remember to pick up your arrows, re-use them and come back when you " +
                    "need more.",
            )
            chatNpc(
                neutral,
                "Once you have your bow and arrows, equip them by selecting their Wield option in your " +
                    "inventory.",
            )
            access.toplevelSidebuttonSwitch(COMBAT_TAB)
            chatNpc(
                neutral,
                "You can change the way you attack by going to the combat options tab. There are three " +
                    "attack styles for bows. Those styles are Accurate, Rapid and Longrange.",
            )
            chatNpc(
                neutral,
                "Accurate increases your bows attack accuracy. Rapid will increase your attack speed with " +
                    "the bow. Longrange will let you attack your enemies from a greater distance.",
            )
        } else {
            if (level >= MAX_LEVEL) {
                chatNpc(
                    happy,
                    "My word, I think you should be the one giving me advice instead, but I will do my " +
                        "best.",
                )
            }
            chatNpc(
                neutral,
                "You should know the basics of ranged combat by now, but if you are looking for somewhere " +
                    "to test your skills and make some money, you should try hunting Moss giants in " +
                    "Varrock sewers.",
            )
            chatNpc(
                neutral,
                "If you are lucky you might find a Mossy key, with this key you will be able to enter " +
                    "Bryophyta's lair. Make sure to bring plenty of food and good armour and weapons when " +
                    "you fight her as you will need it.",
            )
            chatNpc(
                shifty,
                "If your lucky you might snag some goodies that you can sell for a decent profit.",
            )
            chatNpc(
                happy,
                "If you are looking to do something different, visit the ranging guild over near " +
                    "Hemenster, there you can practice your archery and earn tickets for it at the same " +
                    "time.",
            )
            chatNpc(
                happy,
                "You can then hand those tickets in for a variety of goods you can use to train your " +
                    "Ranged skill.",
            )
        }
        chatNpc(
            neutral,
            "If you are ever in the market for a new bow or some arrows, you should head on over to " +
                "Lowe's Archery Emporium in Varrock. I can show you where the shop is if you like.",
        )
        if (menu("Yes please." to true, "No thanks." to false)) {
            chatPlayer(happy, "Yes please.")
            return true
        }
        chatPlayer(neutral, "No thanks")
        chatNpc(quiz, "Is there anything else you want to know?")
        return false
    }

    private suspend fun Dialogue.fletching() {
        chatPlayer(quiz, "How do I create a bow and arrows?")
        chatNpc(happy, "Ahh the art of Fletching. Fletching is used to create your own bow and arrows.")
        chatNpc(
            happy,
            "It's quite simple really. You'll need an axe to cut some logs from trees and a knife. " +
                "Knives can be found in and around the Lumbridge castle and in the Varrock General store " +
                "upstairs.",
        )
        chatNpc(happy, "Use your knife on the logs. This will bring up a menu listing items you can fletch.")
        chatNpc(happy, "For arrows you will need to smith some arrow heads and kill some chickens for feathers.")
        chatNpc(
            happy,
            "Add the feathers to your Arrow shafts to make Headless arrows, then use your chosen arrow " +
                "heads on the Headless arrows to make your arrows.",
        )
        chatNpc(
            happy,
            "Now for making bows. When accessing the fletching menu, instead of choosing Arrows shafts, " +
                "you can make an unstrung bow instead.",
        )
        chatNpc(happy, "To complete the bow you will need to get your hands on a Bow string.")
        chatNpc(
            happy,
            "First you will need to get some flax from a flax field. There's one south of Seers' " +
                "Village. Gather flax, then spin it on a spinning wheel, there's one in Seers' Village too.",
        )
        chatNpc(happy, "This makes bow strings which you can then use on the unstrung bows to make a working bow!")
        chatPlayer(happy, "Brilliant. If I forget anything I'll come talk to you again.")
        chatNpc(quiz, "Is there anything else you want to know?")
    }

    private suspend fun Dialogue.ammoPickup() {
        chatPlayer(quiz, "Let's discuss how my ammo acts when I pick it up.")
        chatNpc(
            neutral,
            "Certainly. If you pick up ammo that is the same ammo you are currently using, I can make " +
                "that ammo automatically equip for you, providing you have space of course!",
        )
        chatNpc(neutral, "Or you can leave it to going into your inventory like normal.")
        val equip =
            menu(
                "Automatically equip it." to true,
                "Place it in my inventory." to false,
                title = "How do you want to handle picking up ammo?",
            )
        chatNpc(
            happy,
            if (equip) "There you go! Your ammo will now automatically be equipped when you pick it up."
            else "There you go! Your ammo will now go to your inventory when you pick it up.",
        )
        chatNpc(quiz, "Is there anything else you want to know?")
    }

    private suspend fun Dialogue.magic() {
        chatNpc(
            quiz,
            "Hello there adventurer, I am the Magic combat tutor. Would you like to learn about magic " +
                "combat, or perhaps how to make runes?",
        )
        var asked = setOf<Int>()
        while (true) {
            val topic =
                menu(
                    buildList {
                        if (1 !in asked) add("Tell me about magic combat please." to 1)
                        if (2 !in asked) add("How do I make runes?" to 2)
                        add("Let's discuss how my runes act when I pick them up." to 3)
                        add("Goodbye." to 4)
                    }
                )
            when (topic) {
                1 -> magicCombat()
                2 -> runecrafting()
                3 -> runePickup()
                else -> {
                    chatPlayer(happy, "Goodbye.")
                    return
                }
            }
            asked = asked + topic
        }
    }

    private suspend fun Dialogue.magicCombat() {
        chatPlayer(neutral, "Tell me about magic combat please.")
        val level = access.statBase("stat.magic")
        if (level < EXPERIENCED_LEVEL) {
            access.toplevelSidebuttonSwitch(SPELLBOOK_TAB)
            chatNpc(
                neutral,
                "This is your spell book, this contains all the spells you can cast. To cast a spell, you " +
                    "will need to have the required magic level and have the right amount of the runes " +
                    "used to cast the spell.",
            )
            chatNpc(
                neutral,
                "To check the level and runes required to cast a spell, hover your mouse cursor over the " +
                    "spell.",
            )
            chatNpc(
                neutral,
                "If you have the right amount of runes and the magic level the spell will light up, " +
                    "meaning you will be able to cast it.",
            )
            chatNpc(
                neutral,
                "When you have a combat spell available, click on it once, then select your target. A good " +
                    "target would be a monster that is below your combat level.",
            )
            chatNpc(
                neutral,
                "Try rats in the castle or, if you're feeling brave, the goblins to the west have been " +
                    "causing a nuisance of themselves.",
            )
            chatNpc(
                neutral,
                "Nemarti, the Ranged Combat tutor and I both give out items every 30 minutes, however you " +
                    "must choose whether you want runes or ranged equipment.",
            )
            chatNpc(
                neutral,
                "To claim runes, right-click on me and choose Claim, to claim ranged equipment right-click " +
                    "on the Ranged Combat tutor and select Claim.",
            )
        } else {
            chatNpc(
                happy,
                "Of course ${player.displayName}! As a rule of thumb, if you cast the highest spell of " +
                    "which you're capable, you'll get the best experience possible.",
            )
            chatNpc(
                happy,
                "Wearing metal armour and ranged armour can seriously impair your magical abilities. Make " +
                    "sure you wear some robes to maximise your capabilities.",
            )
            if (level >= ADVANCED_MAGIC) {
                chatNpc(
                    happy,
                    "Superheat Item and the Alchemy spells are good ways to level magic if you are not " +
                        "interested in the combat aspect of magic.",
                )
                chatNpc(
                    shifty,
                    "There's always the Magic Training Arena. You can find it north of the Emir's Arena, " +
                        "north-east of Al Kharid. You will be able to earn some special rewards there by " +
                        "practicing your magic there.",
                )
                chatNpc(
                    shifty,
                    "If you want a challenge, Kolodion, the master of battle magic, has set up an arena " +
                        "deep in the Wilderness. There you will have the chance to learn some extremely " +
                        "powerful spells.",
                )
            }
        }
        chatNpc(quiz, "Is there anything else you would like to know?")
    }

    private suspend fun Dialogue.runecrafting() {
        chatPlayer(quiz, "How do I make runes?")
        chatNpc(
            neutral,
            "There are a couple of things you will need to make runes, rune essence and a talisman to " +
                "enter the temple ruins.",
        )
        if (QuestRequirements.hasCompleted(player, "quest_runemysteries")) {
            chatNpc(
                neutral,
                "To get rune essence you will need to gather them in the essence mine. You can get to the " +
                    "mine by talking to Aubury who owns the runes shop in south east Varrock.",
            )
        } else {
            chatNpc(
                neutral,
                "To get rune essence you will need to gather them somehow. You should talk to the Duke of " +
                    "Lumbridge, he may be able to help you with that. Alternatively, other players may " +
                    "sell you the essence.",
            )
        }
        if (access.statBase("stat.runecraft") < EXPERIENCED_LEVEL) {
            chatNpc(neutral, "As you're fairly new to runecrafting you should start with air runes and mind runes.")
        } else {
            chatNpc(
                neutral,
                "I see you have some experience already in Runecrafting. Perhaps you should try crafting " +
                    "some runes which you can then use in magic.",
            )
            chatNpc(neutral, "Check the skill guide to see which runes you can craft.")
            access.toplevelSidebuttonSwitch(SKILLS_TAB)
        }
        chatNpc(
            neutral,
            "You will need a talisman for the rune you would like to create. You can right-click on it " +
                "and select the Locate option. This will tell you the rough location of the altar.",
        )
        chatNpc(
            neutral,
            "When you find the ruined altar, use the talisman on it to be transported to a temple where " +
                "you can craft your runes.",
        )
        chatNpc(
            neutral,
            "Clicking on the temple's altar will imbue your rune essence with the altar's magical property.",
        )
        chatNpc(
            neutral,
            "If you want to save yourself an inventory space, you could always try binding the talisman " +
                "to a tiara.",
        )
        chatNpc(
            neutral,
            "To make one, take a tiara and talisman to the ruins and use the tiara on the temple altar. " +
                "This will bind the talisman to your tiara.",
        )
        chatNpc(quiz, "Is there anything else you would like to know?")
    }

    private suspend fun Dialogue.runePickup() {
        chatPlayer(quiz, "Let's discuss how my runes act when I pick them up.")
        chatNpc(
            neutral,
            "When you pick up runes from the ground, if you're carrying a rune pouch that contains " +
                "matching runes, I can make the runes you've picked up go straight into the pouch, " +
                "provided there's space.",
        )
        chatNpc(neutral, "Alternatively, you can let them go into your inventory like normal.")
        val pouch =
            menu(
                "Automatically send to rune pouch." to true,
                "Place them in my inventory." to false,
                title = "How do you want to handle picking up runes?",
            )
        chatNpc(
            happy,
            if (pouch) {
                "Certainly! Runes will go straight to your rune pouch when you pick them up, if it " +
                    "contains matching runes."
            } else {
                "Certainly! Runes will go to your inventory when you pick them up, rather than going into " +
                    "a rune pouch."
            },
        )
        mesbox(
            "You can also control this option via the Settings menu, rather than having to return to " +
                "this Tutor in future."
        )
        chatNpc(quiz, "Is there anything else I can do for you?")
    }

    private fun ProtectedAccess.claimReady(): Boolean {
        val now = (System.currentTimeMillis() / MILLIS_PER_MINUTE).toInt()
        return now - player.lastTutorClaim >= CLAIM_COOLDOWN_MINUTES
    }

    private fun ProtectedAccess.markClaim() {
        player.lastTutorClaim = (System.currentTimeMillis() / MILLIS_PER_MINUTE).toInt()
    }

    private suspend fun Dialogue.claimArrows() {
        if (!access.claimReady()) {
            chatNpc(
                neutral,
                "I work with the Magic tutor to give out consumable items that you may need for combat " +
                    "such as arrows and runes. However we have had some cheeky people try to take both!",
            )
            chatNpc(
                neutral,
                "So, every half an hour, you may come back and claim either arrows OR runes, but not " +
                    "both. Come back in a while for arrows, or simply buy some.",
            )
            return
        }
        val needBow = !owns(BOW)
        val needArrows = !owns(ARROWS)
        when {
            needBow && needArrows -> {
                if (access.inv.freeSpace() < 2) {
                    chatNpc(
                        neutral,
                        "If you had any room in your pack I'd give you a training bow. That's a shame. Come " +
                            "back when you do. If you had enough space in your inventory I'd give you some " +
                            "training arrows, come back when you do.",
                    )
                    return
                }
                access.invAdd(access.inv, BOW)
                access.invAdd(access.inv, ARROWS, ARROW_COUNT)
                access.markClaim()
                doubleobjbox(
                    BOW,
                    ARROWS,
                    "Nemarti gives you a Training shortbow and $ARROW_COUNT arrows. They can only be used " +
                        "together.",
                )
            }
            needBow -> {
                if (access.inv.isFull()) {
                    chatNpc(
                        neutral,
                        "If you had any room in your pack I'd give you a training bow. That's a shame. Come " +
                            "back when you do.",
                    )
                    return
                }
                chatNpc(neutral, "You already have some training arrows.")
                access.invAdd(access.inv, BOW)
                access.markClaim()
                objbox(BOW, "Nemarti gives you a Training shortbow. It can only be used with Training arrows.")
            }
            needArrows -> {
                if (access.inv.isFull() && access.inv.count(ARROWS) == 0) {
                    chatNpc(
                        neutral,
                        "If you had enough space in your inventory I'd give you some training arrows, come " +
                            "back when you do.",
                    )
                    return
                }
                chatNpc(neutral, "You already have a training bow.")
                access.invAdd(access.inv, ARROWS, ARROW_COUNT)
                access.markClaim()
                objbox(
                    ARROWS,
                    "Nemarti gives you $ARROW_COUNT training arrows. They can only be used with the Training " +
                        "shortbow.",
                )
            }
            else -> {
                chatNpc(neutral, "You already have a training bow.")
                chatNpc(neutral, "You already have some training arrows.")
            }
        }
    }

    private suspend fun Dialogue.claimRunes() {
        if (!access.claimReady()) {
            chatNpc(
                neutral,
                "I work with the Ranged tutor to give out consumable items that you may need for combat " +
                    "such as runes and arrows. However we have had some cheeky people try to take both!",
            )
            chatNpc(
                neutral,
                "So, every half an hour, you may come back and claim either runes OR arrows, but not " +
                    "both. Come back in a while for runes, or simply buy some.",
            )
            return
        }
        var gave = false
        var banked = false
        for ((rune, name) in listOf(MIND_RUNE to "mind", AIR_RUNE to "air")) {
            when {
                access.bank.count(rune) > 0 -> {
                    chatNpc(neutral, "You have some $name runes in your bank.")
                    banked = true
                }
                access.inv.count(rune) > 0 -> chatNpc(neutral, "You already have some $name runes.")
                access.inv.isFull() ->
                    chatNpc(
                        neutral,
                        "If you had enough space in your inventory I'd give you some $name runes, come back " +
                            "when you do.",
                    )
                else -> {
                    access.invAdd(access.inv, rune, RUNE_COUNT)
                    objbox(rune, "Mikasi gives you $RUNE_COUNT $name runes.")
                    gave = true
                }
            }
        }
        if (gave) access.markClaim()
        if (banked) {
            mesbox(
                "You have some runes in your bank. Climb the stairs in Lumbridge Castle until you see this " +
                    "icon on your minimap. There you will find a bank."
            )
        }
    }

    private companion object {
        const val MELEE = "npc.aide_tutor_melee"
        const val RANGED = "npc.aide_tutor_ranging"
        const val MAGIC = "npc.aide_tutor_magic"

        const val SWORD = "obj.aide_shortsword"
        const val SHIELD = "obj.aide_shield"
        const val BOW = "obj.aide_shortbow"
        const val ARROWS = "obj.aide_arrow"
        const val MIND_RUNE = "obj.mindrune"
        const val AIR_RUNE = "obj.airrune"
        const val COINS = "obj.coins"

        const val ARROW_COUNT = 25
        const val RUNE_COUNT = 30
        const val CAPE_PRICE = 99000
        const val MAX_LEVEL = 99
        const val EXPERIENCED_LEVEL = 20
        const val ADVANCED_MAGIC = 42
        const val CLAIM_COOLDOWN_MINUTES = 30
        const val MILLIS_PER_MINUTE = 60_000L

        const val COMBAT_TAB = 0
        const val SKILLS_TAB = 1
        const val WORN_TAB = 4
        const val SPELLBOOK_TAB = 6

        val SKILLS =
            listOf(
                "stat.attack", "stat.defence", "stat.strength", "stat.hitpoints", "stat.ranged",
                "stat.prayer", "stat.magic", "stat.cooking", "stat.woodcutting", "stat.fletching",
                "stat.fishing", "stat.firemaking", "stat.crafting", "stat.smithing", "stat.mining",
                "stat.herblore", "stat.agility", "stat.thieving", "stat.slayer", "stat.farming",
                "stat.runecraft", "stat.hunter", "stat.construction",
            )
    }
}
