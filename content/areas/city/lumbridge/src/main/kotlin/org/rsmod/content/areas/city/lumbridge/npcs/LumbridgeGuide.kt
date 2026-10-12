package org.rsmod.content.areas.city.lumbridge.npcs

import dev.openrune.util.Wearpos
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.righthand
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.content.quest.manager.menu
import org.rsmod.game.type.getInvObj
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Lumbridge_Guide
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:temple_of_the_eye] "Ask about the Amulet of the Eye" after losing it
// TODO(content): [system:treasure_trails] hard clue puzzle box
// TODO(content): [verify:in_game] the combat, Smithing and Fishing levels (the wiki says "X")
class LumbridgeGuide : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.lumbridge_guide") { startDialogue(it.npc) { guide() } }
    }

    private enum class Next {
        Initial,
        Previous,
        End,
    }

    private suspend fun Dialogue.guide() {
        chatNpc(
            happy,
            "Greetings, adventurer. I am Phileas, the Lumbridge Guide. I am here to give " +
                "information and directions to new players. Is there anything I can help you " +
                "with?",
        )
        while (true) {
            val next =
                when (
                    menu(
                        "Where can I find a quest to go on?" to 1,
                        "What monsters should I fight?" to 2,
                        "Where can I make money?" to 3,
                        "Where can I find more information?" to 4,
                        "More options..." to 5,
                    )
                ) {
                    1 -> quests()
                    2 -> monsters()
                    3 -> money()
                    4 -> moreInformation()
                    else -> moreOptions()
                }
            if (next == Next.End) return
        }
    }

    private suspend fun Dialogue.anythingElse(
        vararg options: String = arrayOf(YES_PLEASE, NO_THANK_YOU)
    ): Next {
        chatNpc(happy, "Is there anything else you need help with?")
        return when (val reply = menu(options.map { it to it })) {
            YES_PLEASE,
            "Yes, please." -> {
                chatPlayer(happy, YES_PLEASE)
                Next.Initial
            }
            NO_THANK_YOU,
            "No, thank you." -> {
                chatPlayer(neutral, NO_THANK_YOU)
                Next.End
            }
            SOMETHING_ELSE,
            SOMETHING_ELSE_MORE -> {
                chatPlayer(neutral, SOMETHING_ELSE)
                chatNpc(neutral, "Well, what would you like to know?")
                Next.Initial
            }
            else -> {
                check(reply in PREVIOUS_OPTIONS) { "Unhandled option: $reply" }
                Next.Previous
            }
        }
    }

    private suspend fun Dialogue.quests(): Next {
        chatPlayer(quiz, "Where can I find a quest to go on?")
        val current = QUEST_LINE.firstOrNull { !QuestRequirements.hasCompleted(player, it.key) }
        if (current == null) {
            chatNpc(
                confused,
                "I'm afraid I can't think of any more quests for you. Don't worry though, " +
                    "there'll be plenty more out there. You'll just need to find them.",
            )
        } else if (!QuestRequirements.isOnQuest(player, current.key)) {
            chatNpc(neutral, current.suggestion)
        } else {
            chatNpc(
                neutral,
                "I heard you were already ${current.started}. Are you sure you need another " +
                    "quest?",
            )
            when (
                menu(
                    "I want another quest." to 1,
                    "I'm not too sure what to do." to 2,
                    "Actually, I'm fine." to 3,
                )
            ) {
                1 -> {
                    chatPlayer(neutral, "I want another quest.")
                    val unstarted =
                        QUEST_LINE.firstOrNull {
                            !QuestRequirements.hasCompleted(player, it.key) &&
                                !QuestRequirements.isOnQuest(player, it.key)
                        }
                    if (unstarted == null) {
                        chatNpc(
                            confused,
                            "I'm afraid I can't think of any more quests for you. Don't worry " +
                                "though, there'll be plenty more out there. You'll just need to " +
                                "find them.",
                        )
                    } else {
                        chatNpc(neutral, unstarted.suggestion)
                    }
                }
                2 -> {
                    chatNpc(
                        neutral,
                        "Well if you need a reminder, try having a look at your quest journal.",
                    )
                    return anythingElse()
                }
                else -> {
                    chatPlayer(neutral, "Actually, I'm fine.")
                    return Next.End
                }
            }
        }
        objbox(
            QUEST_ICON,
            "The minimap in the top right corner of the screen has various icons to show " +
                "different points of interest. Look for the icon to the left to find quest start " +
                "points.",
        )
        return anythingElse()
    }

    private suspend fun Dialogue.monsters(): Next {
        chatPlayer(quiz, "What monsters should I fight?")
        if (player.combatLevel >= SELF_SUFFICIENT_COMBAT) {
            chatNpc(
                neutral,
                "You're strong enough to work out what monsters to fight for yourself now, but " +
                    "the combat tutors might help you with any questions you have about the " +
                    "skills; they're just over there to the south of the general store.",
            )
            objbox(
                COMBAT_TUTOR_ICON,
                "The minimap in the top right corner of the game screen has various icons " +
                    "showing the locations of things. This icon to the left shows where Combat " +
                    "tutors are.",
            )
            return Next.Initial
        }
        if (!hasWeapon()) {
            chatNpc(
                neutral,
                "It's going to be tough fighting monsters without a weapon. I suggest you find " +
                    "one of those first.",
            )
            while (true) {
                val next =
                    when (
                        menu(
                            "How can I get a weapon?" to 1,
                            "Could I get a staff like yours?" to 2,
                            "Yes, but what should I fight once I have got a weapon?" to 3,
                        )
                    ) {
                        1 -> gettingWeapons()
                        2 -> staff()
                        else -> {
                            chatPlayer(
                                quiz,
                                "Yes, but what should I fight once I have got a weapon?",
                            )
                            return fightAdvice()
                        }
                    }
                if (next != Next.Previous) return next
            }
        }
        return fightAdvice()
    }

    private fun Dialogue.hasWeapon(): Boolean =
        player.righthand != null ||
            player.inv.any { it != null && Wearpos[getInvObj(it).wearpos1] == Wearpos.RightHand }

    private suspend fun Dialogue.gettingWeapons(): Next {
        chatPlayer(quiz, "How can I get a weapon?")
        chatNpc(neutral, "Well you can either make them, find them or buy them.")
        while (true) {
            val next =
                when (
                    menu(
                        "Where can I find a weapon?" to 1,
                        "How do I make a weapon?" to 2,
                        "Where can I buy a weapon?" to 3,
                    )
                ) {
                    1 -> findWeapon()
                    2 -> makeWeapon()
                    else -> buyWeapon()
                }
            if (next != Next.Previous) return next
        }
    }

    private suspend fun Dialogue.findWeapon(): Next {
        chatPlayer(quiz, "Where can I find a weapon?")
        chatNpc(
            neutral,
            "If you visit the combat tutors south of the general store they may be able to help " +
                "you with that. Failing that there are a few cheap weapons lying around in the " +
                "world. For example, you can get an iron dagger if you",
        )
        chatNpc(
            neutral,
            "cross the bridge and look in the little hut where some goblins live. If it's not " +
                "there it probably means someone else has taken it. If you wait a while a new " +
                "one should appear. As you explore you may find other weapons,",
        )
        chatNpc(neutral, "too.")
        return anythingElse(NO_THANK_YOU, MORE_WEAPONS, SOMETHING_ELSE)
    }

    private suspend fun Dialogue.makeWeapon(): Next {
        chatPlayer(quiz, "How do I make a weapon?")
        if (access.statBase("stat.smithing") >= EXPERIENCED_LEVEL) {
            chatNpc(
                neutral,
                "You already have a reasonable Smithing level, so I think you know that already.",
            )
            return Next.End
        }
        chatNpc(
            neutral,
            "The Smithing skill allows you to make armour and weapons. Talk to the boy who " +
                "smelts metal in the furnace, I'm sure he can help.",
        )
        chatPlayer(quiz, "Where can I smith?")
        return smithingTutor()
    }

    private suspend fun Dialogue.smithingTutor(): Next {
        chatNpc(
            neutral,
            "You will find a helpful Smithing tutor in the west of Varrock - that's north of " +
                "here. Follow the path across the river and head north.",
        )
        objbox(SMITHING_ICON, "Look for this icon in the west of Varrock.")
        objbox(
            MINING_ICON,
            "The minimap in the top right corner of the game screen has various icons showing " +
                "the locations of things. This icon to the left shows where Mining sites are.",
        )
        chatNpc(
            neutral,
            "I suggest you go and mine some ore; find the Mining symbol - with the guide symbol " +
                "near it - in the swamp south of here. The Mining guide there can teach you how " +
                "to mine ore.",
        )
        return anythingElse()
    }

    private suspend fun Dialogue.buyWeapon(): Next {
        chatPlayer(quiz, "Where can I buy a weapon?")
        chatNpc(
            neutral,
            "You can get a weapon free from the combat tutors if they think you need it. Failing " +
                "that, the nearest shop that would sell you something of that nature is Bob's " +
                "Brilliant Axes in this very town. If you want a bigger",
        )
        chatNpc(
            neutral,
            "choice I suggest you head to one of the big cities. If you follow the road east " +
                "over the bridge and then head north you will eventually reach Varrock, where " +
                "you can buy all manner of things. You can use the world map",
        )
        chatNpc(neutral, "to help you find your way.")
        return anythingElse(NO_THANK_YOU, MORE_WEAPONS, SOMETHING_ELSE)
    }

    private suspend fun Dialogue.staff(): Next {
        chatPlayer(quiz, "Could I get a staff like yours?")
        chatNpc(
            neutral,
            "There is no other staff like this in all the land. It's a very important staff. It " +
                "shows who holds the job as the Lumbridge Guide, and that's me.",
        )
        return anythingElse(MORE_WEAPONS, SOMETHING_ELSE, NO_THANK_YOU)
    }

    private suspend fun Dialogue.fightAdvice(): Next {
        chatNpc(
            neutral,
            "There's lots of beasts to fight in the woods around here, especially to the west. " +
                "There are certainly some goblins and spiders that are pests and could do with " +
                "being cleared out. There's also a chicken farm or two up the",
        )
        chatNpc(
            neutral,
            "road for some fairly easy pickings. Non-player characters usually appear as yellow " +
                "dots on your minimap, although there are some that you won't be able to fight, " +
                "such as myself. A monster's combat level is",
        )
        chatNpc(
            neutral,
            "shown next to their 'Attack' option. If that level is coloured green it means the " +
                "monster is weaker than you. If it is red, it means that the monster is tougher " +
                "than you.",
        )
        chatNpc(
            neutral,
            "Remember, you will do better if you have better armour and weapons and it's always " +
                "worth carrying a bit of food to heal yourself.",
        )
        while (true) {
            val next =
                when (
                    menu(
                        "Where can I get food to heal myself?" to 1,
                        "Where can I get better armour and weapons?" to 2,
                        "Okay, thanks, I will go and kill things." to 3,
                        "Can I kill other players?" to 4,
                    )
                ) {
                    1 -> food()
                    2 -> betterEquipment()
                    3 -> {
                        chatPlayer(happy, "Okay, thanks, I will go and kill things.")
                        Next.End
                    }
                    else -> killPlayers()
                }
            if (next != Next.Previous) return next
        }
    }

    private suspend fun Dialogue.killPlayers(): Next {
        chatPlayer(quiz, "Can I kill other players?")
        chatNpc(
            neutral,
            "Well, you can go to the Wilderness. That is the area for killing other players. " +
                "Just keep heading north and eventually you will be bound to reach it. Be very " +
                "careful, though. Player-killing is not for the unprepared.",
        )
        chatNpc(
            neutral,
            "I'd suggest that you leave any items you don't want to lose in the bank and take a " +
                "good supply of food with you.",
        )
        return anythingElse(NO_THANK_YOU, MORE_COMBAT, SOMETHING_ELSE)
    }

    private suspend fun Dialogue.betterEquipment(): Next {
        chatNpc(
            neutral,
            "Well, you can make them, buy them or talk to the combat tutors just west of here.",
        )
        while (true) {
            val next =
                when (
                    menu(
                        "How do I make a weapon?" to 1,
                        "Where can I buy a weapon?" to 2,
                        "Could I get a staff like yours?" to 3,
                    )
                ) {
                    1 -> makeWeapon()
                    2 -> buyWeapon()
                    else -> staff()
                }
            if (next != Next.Previous) return next
        }
    }

    private suspend fun Dialogue.food(): Next {
        chatPlayer(quiz, "Where can I get food to heal myself?")
        chatNpc(
            neutral,
            "There are many different foods in the game such as cabbage, fish, meat and many " +
                "more. Which do you wish to hear about?",
        )
        while (true) {
            val next =
                when (
                    menu(
                        "How do I get cabbages?" to 1,
                        "How do I fish?" to 2,
                        "Where can I find meat?" to 3,
                    )
                ) {
                    1 -> {
                        chatPlayer(quiz, "How do I get cabbages?")
                        chatNpc(
                            neutral,
                            "There is a field a little distance to the north of here packed full " +
                                "of cabbages which are there for the picking.",
                        )
                        chatNpc(
                            neutral,
                            "You could also farm your own cabbages; there's a Farming patch just " +
                                "south of Falador.",
                        )
                        anythingElse(OTHER_FOOD, SOMETHING_ELSE, NO_THANK_YOU)
                    }
                    2 -> {
                        chatPlayer(quiz, "How do I fish?")
                        if (!fishing()) return Next.End
                        anythingElse(OTHER_FOOD, SOMETHING_ELSE, NO_THANK_YOU)
                    }
                    else -> meat()
                }
            if (next != Next.Previous) return next
        }
    }

    /** Returns false when the player is already an experienced fisher, which ends the dialogue. */
    private suspend fun Dialogue.fishing(): Boolean {
        if (access.statBase("stat.fishing") >= EXPERIENCED_LEVEL) {
            chatNpc(
                confused,
                "I am confused. You are already an experienced fisher. You should know the " +
                    "answer to that already.",
            )
            return false
        }
        chatNpc(
            neutral,
            "Fishing spots require different levels and equipment to use. To start Fishing, " +
                "you'll want to talk to the Fishing tutor who can be found in the swamps south " +
                "of here. He will also give you a small fishing net if you don't",
        )
        chatNpc(neutral, "own one already.")
        objbox(
            FISHING_ICON,
            "The minimap in the top right corner of the game screen has various icons showing " +
                "the locations of things. This icon to the left shows where Fishing spots are.",
        )
        objbox(
            "obj.net",
            "You will need some Fishing equipment. At the Fishing spots to the south you can " +
                "only use a small fishing net.",
        )
        chatPlayer(quiz, "Where could I find one of those?")
        chatNpc(
            neutral,
            "You can get them from a Fishing shop or our Fishing Tutor south of here in the " +
                "swamp. There is a Fishing shop in Port Sarim; you can find it on the world map. " +
                "Port Sarim is some way to the west of here, beyond",
        )
        chatNpc(neutral, "the village of Draynor.")
        return true
    }

    private suspend fun Dialogue.meat(): Next {
        chatPlayer(quiz, "Where can I find meat?")
        chatNpc(
            neutral,
            "I suggest you go and kill some chickens. The roads on either side of this river " +
                "eventually go past a chicken farm. When you have killed some chickens, cook " +
                "them. You could either make a fire or use a range.",
        )
        chatNpc(
            neutral,
            "There is a range at the southern end in this town and a Cooking tutor in south " +
                "Lumbridge near Bob's Brilliant Axes shop.",
        )
        objbox(
            RANGE_ICON,
            "The minimap in the top right corner of the game screen has various icons showing " +
                "the locations of things. This icon to the left shows where Cooking ranges are.",
        )
        return anythingElse(OTHER_FOOD, SOMETHING_ELSE, NO_THANK_YOU)
    }

    private suspend fun Dialogue.money(): Next {
        chatPlayer(quiz, "Where can I make money?")
        chatNpc(
            neutral,
            "There are many ways to make money in the game. I would suggest either killing " +
                "monsters or doing a trade skill such as Smithing or Fishing.",
        )
        chatNpc(
            neutral,
            "Please don't try to get money by begging off other players. It will make you " +
                "unpopular. Nobody likes a beggar. It is very irritating to have other players " +
                "asking for your hard-earned cash.",
        )
        return when (
            menu(
                "Where can I smith?" to 1,
                "How do I fish?" to 2,
                "What monsters should I fight?" to 3,
            )
        ) {
            1 -> {
                chatPlayer(quiz, "Where can I smith?")
                smithingTutor()
            }
            2 -> {
                chatPlayer(quiz, "How do I fish?")
                fishing()
                Next.End
            }
            else -> monsters()
        }
    }

    // TODO(content): [system:account_management] open the useful links section first
    private suspend fun Dialogue.moreInformation(): Next {
        chatPlayer(quiz, "Where can I find more information?")
        chatNpc(
            neutral,
            "What you'll want is the OSRS Wiki! I've taken the liberty of opening the useful " +
                "links section of the account management tab for you. Here you can find a link " +
                "to the wiki. There's information and guides on almost",
        )
        chatNpc(
            neutral,
            "everything, all easily searched and it's even written and maintained by players.",
        )
        return anythingElse()
    }

    private suspend fun Dialogue.moreOptions(): Next =
        when (
            menu(
                "I'd like to know more about security." to 1,
                "Where can I find a bank?" to 2,
                "I don't need any help." to 3,
                "Previous options..." to 4,
            )
        ) {
            1 -> security()
            2 -> {
                chatPlayer(quiz, "Where can I find a bank?")
                chatNpc(
                    neutral,
                    "You'll find a bank upstairs in Lumbridge Castle - go right to the top.",
                )
                objbox(
                    BANK_ICON,
                    "The minimap in the top right corner of the game screen has various icons " +
                        "showing the locations of things. This icon to the left shows where " +
                        "banks are.",
                )
                anythingElse()
            }
            3 -> {
                chatPlayer(neutral, "No, I can find things myself thank you.")
                Next.End
            }
            else -> Next.Initial
        }

    private suspend fun Dialogue.security(): Next {
        while (true) {
            chatPlayer(quiz, "I'd like to know more about security.")
            chatNpc(
                neutral,
                "First I must warn you to take every precaution to keep your RuneScape password " +
                    "and PIN secure. The most important thing to remember is to never give your " +
                    "password to, or share your account with, anyone.",
            )
            chatNpc(
                neutral,
                "I can also tell you about password security, avoiding item scamming and in-game " +
                    "moderation. I can also tell you about a place called the Stronghold of " +
                    "Security, where you can learn more about account security and",
            )
            chatNpc(neutral, "have a bit of an adventure at the same time.")
            val next =
                when (
                    menu(
                        "I'd like to know about password security." to 1,
                        "I'd like to know more about avoiding item scamming." to 2,
                        "I'd like to know more about in-game moderation." to 3,
                        "I'd like to know about the Stronghold of Security." to 4,
                    )
                ) {
                    1 -> {
                        passwordSecurity()
                        anythingElse(MORE_SECURITY, SOMETHING_ELSE_MORE, NO_THANK_YOU)
                    }
                    2 -> {
                        itemScamming()
                        anythingElse(MORE_SECURITY, SOMETHING_ELSE, NO_THANK_YOU)
                    }
                    3 -> {
                        moderation()
                        anythingElse("No, thank you.", "Yes, please.", MORE_SECURITY)
                    }
                    else -> {
                        strongholdOfSecurity()
                        anythingElse(MORE_SECURITY, SOMETHING_ELSE, NO_THANK_YOU)
                    }
                }
            if (next != Next.Previous) return next
        }
    }

    private suspend fun Dialogue.passwordSecurity() {
        chatPlayer(quiz, "I'd like to know about password security.")
        chatNpc(
            neutral,
            "Well, the first thing to remember with password security, which I can't stress " +
                "enough, is to never tell your password, bank PIN or authenticator details to " +
                "anyone, not even if they claim to be Jagex staff. Real",
        )
        chatNpc(
            neutral,
            "Jagex staff will NEVER ask for your password. Sharing your account is a very bad " +
                "idea, even if you think that you know and trust the person you are sharing " +
                "with. Players have lost items and even their",
        )
        chatNpc(neutral, "account this way. Sharing accounts is also against the rules.")
        chatPlayer(quiz, "Is there anything else to be aware of?")
        chatNpc(
            neutral,
            "Yes, also be careful where you enter your password. Be aware that fake Old School " +
                "RuneScape websites exist. These are sites that claim to give things such as " +
                "item/stat upgrades, free items, beta testing and",
        )
        chatNpc(neutral, "moderator applications. Jagex do not offer any of these services.")
        chatNpc(
            neutral,
            "Also be aware of trojans and keyloggers. These nasty programs can send everything " +
                "you type into your device back to the computers of malicious people, so you can " +
                "lose more than just your Old School RuneScape",
        )
        chatNpc(
            neutral,
            "password in this way. To avoid getting these on your device, be very wary of what " +
                "you download, especially when it comes to downloading third party software. " +
                "Also be very wary of email attachments and instant",
        )
        chatNpc(neutral, "messenger file transfers, which can contain such things.")
        chatNpc(
            neutral,
            "Finally, remember the game doesn't censor your password or bank PIN when you type " +
                "them; anyone who tells you otherwise is probably trying to steal your character " +
                "or items.",
        )
    }

    private suspend fun Dialogue.itemScamming() {
        chatPlayer(quiz, "I'd like to know more about avoiding item scamming.")
        chatNpc(
            neutral,
            "There are many nice and helpful players in Old School RuneScape; unfortunately, as " +
                "in real life, there are some who aren't so honest. Some people may try to trick " +
                "you out of your items. Try not to fall for this as",
        )
        chatNpc(
            neutral,
            "Jagex policy is to never return lost items to players that have been scammed. Doing " +
                "so just encourages users to claim they lost items they never had to gain an " +
                "unfair advantage. However, people carrying out such",
        )
        chatNpc(neutral, "scams will get banned as it is against the rules.")
        chatNpc(
            neutral,
            "When trading, you have a second trade confirmation screen. Always double check that " +
                "you are getting the items you expect on this page as some people may try and " +
                "change what they are trading at the last minute on",
        )
        chatNpc(
            neutral,
            "the first trade screen. Remember when you have clicked accept on the second trade " +
                "screen, the trade is not reversible.",
        )
        chatNpc(
            neutral,
            "Try and find out from someone else what an item is before spending a lot of money " +
                "on it. Not all merchants are honest about the rarity or the uses of their " +
                "wares. For example, some people try to pass off spinach rolls as",
        )
        chatNpc(neutral, "rare when in fact they're as common as cabbages!")
        chatNpc(
            neutral,
            "Be wary of people offering to improve your items. For example, some people will " +
                "claim they can add a trim to your armour or upgrade your sword to a more " +
                "powerful one. This is not possible and they are trying",
        )
        chatNpc(neutral, "to steal your hard-earned equipment.")
    }

    private suspend fun Dialogue.moderation() {
        chatPlayer(quiz, "I'd like to know more about in-game moderation.")
        chatNpc(
            neutral,
            "You will, from time to time, see moderator characters in game. Remember: a real " +
                "staff member will never ask for your password or any other personal " +
                "information. There are two types of moderator. You may see Jagex",
        )
        chatNpc(
            neutral,
            "staff characters in game - these can be identified by a gold crown next to their " +
                "name and are the people who work in the Jagex offices. They might be from " +
                "Customer Support, or they might be a tester or a",
        )
        chatNpc(
            neutral,
            "programmer. You may also see player moderators - these can be identified by a " +
                "silver crown next to their name. Player moderators are trusted players of Old " +
                "School RuneScape that help keep in-game behaviour in",
        )
        chatNpc(neutral, "line with the Old School RuneScape rules.")
        chatPlayer(quiz, "How do I become a player moderator?")
        chatNpc(
            neutral,
            "The only way to become a player moderator is to know and play by the rules laid " +
                "down by Jagex. Report those who break the rules, help players who need it and " +
                "one day you might find an invite to become a moderator in",
        )
        chatNpc(
            neutral,
            "your Old School RuneScape inbox. Note that Jagex will never contact you in game or " +
                "by email to become a player moderator.",
        )
    }

    private suspend fun Dialogue.strongholdOfSecurity() {
        chatPlayer(quiz, "I'd like to know about the Stronghold of Security.")
        chatNpc(
            neutral,
            "An exciting new discovery has been made in the Barbarian Village. Soon after they " +
                "started mining in the middle of the village, one of the miners got caught in a " +
                "cave.",
        )
        chatPlayer(quiz, "Sounds painful, did he fall on his head?")
        chatNpc(neutral, "No, but it turned out that he had discovered a place unlike any other.")
        chatPlayer(quiz, "How so?")
        chatNpc(
            neutral,
            "Well an explorer went to look down there, it's full of new and exciting creatures " +
                "and other surprises on top of that. It turns out this place can also help you " +
                "to secure your account more effectively.",
        )
        chatPlayer(happy, "Wow, that sounds good. Where can I find the explorer to learn more?")
        chatNpc(
            neutral,
            "Sadly the explorer went missing and has not been found. However, here is some " +
                "information to get you started on securing your account. You can find the " +
                "Barbarian Village north of Falador and west of Varrock.",
        )
        if (access.inv.count(SECURITY_BOOK) == 0 && access.bank.count(SECURITY_BOOK) == 0 &&
            !access.inv.isFull()
        ) {
            access.invAdd(access.inv, SECURITY_BOOK)
            objbox(SECURITY_BOOK, "The guide hands you a book.")
        }
    }

    private data class GuideQuest(val key: String, val suggestion: String, val started: String)

    private companion object {
        const val SELF_SUFFICIENT_COMBAT = 30
        const val EXPERIENCED_LEVEL = 20

        const val YES_PLEASE = "Yes please."
        const val NO_THANK_YOU = "No thank you."
        const val SOMETHING_ELSE = "I'd like to know about something else."
        const val SOMETHING_ELSE_MORE = "I'd like to know more about something else."
        const val MORE_WEAPONS = "I'd like to know more about getting weapons."
        const val MORE_COMBAT = "I'd like to know more about combat."
        const val OTHER_FOOD = "I'd like to know about other food."
        const val MORE_SECURITY = "Tell me more about security."
        val PREVIOUS_OPTIONS = setOf(MORE_WEAPONS, MORE_COMBAT, OTHER_FOOD, MORE_SECURITY)

        const val QUEST_ICON = "obj.quest_start_icon_dummy"
        const val COMBAT_TUTOR_ICON = "obj.combat_tutor_icon_dummy"
        const val RANGE_ICON = "obj.range_icon_dummy"
        const val BANK_ICON = "obj.bank_icon_dummy"
        const val FISHING_ICON = "obj.fishing_spot_icon_dummy"
        const val SMITHING_ICON = "obj.smithing_tutor_icon_dummy"
        const val MINING_ICON = "obj.mining_site_icon_dummy"
        const val SECURITY_BOOK = "obj.sos_security_book"

        val QUEST_LINE =
            listOf(
                GuideQuest(
                    "quest_cooksassistant",
                    "Well, I've heard my friend the cook is in need of a spot of help. He'll be " +
                        "in the kitchen of this here castle. Just talk to him and he'll set you " +
                        "off.",
                    "helping the cook",
                ),
                GuideQuest(
                    "quest_restlessghost",
                    "Well, I heard that Father Aereck could use a hand. You'll find him in the " +
                        "church just here. Talk to him and he'll give you the details.",
                    "helping Father Aereck",
                ),
                GuideQuest(
                    "quest_xmarksthespot",
                    "Well, I heard that a chap called Veos needs help finding some treasure. " +
                        "Last I heard, he was looking for assistance in the pub just north of " +
                        "here. Talk to him and see if you can help.",
                    "helping Veos",
                ),
                GuideQuest(
                    "quest_sheepshearer",
                    "Well, I heard that Farmer Fred could use some help. You'll find him on the " +
                        "farm just north of here. Talk to him and see if he needs a hand.",
                    "helping Farmer Fred",
                ),
                GuideQuest(
                    "quest_misthalinmystery",
                    "Well, I heard there was some commotion in the swamp to the south. Might be " +
                        "worth looking into.",
                    "investigating the issue in the swamp",
                ),
                GuideQuest(
                    "quest_runemysteries",
                    "Well, I believe the Duke of Lumbridge requires an errand running for him. " +
                        "You can find him upstairs in the castle. Just talk to him to get the " +
                        "details.",
                    "helping the Duke",
                ),
            )
    }
}
