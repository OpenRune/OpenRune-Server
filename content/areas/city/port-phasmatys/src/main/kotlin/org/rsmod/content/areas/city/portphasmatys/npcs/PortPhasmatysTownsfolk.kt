package org.rsmod.content.areas.city.portphasmatys.npcs

import org.rsmod.api.config.Constants
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class PortPhasmatysTownsfolk : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(GHOST_GUARD) { startDialogue(it.npc) { ghostGuard() } }
        onOpNpc1(GHOST_CAPTAIN) { startDialogue(it.npc) { ghostCaptain() } }
        onOpNpc3(GHOST_CAPTAIN) { startDialogue(it.npc) { ghostCaptainTravel() } }
        onOpNpc1(ANDRAS) { startDialogue(it.npc) { andras() } }
        onOpNpc1(PIRATE_PETE) { startDialogue(it.npc) { piratePete() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ghost_guard
    // TODO(content): [system:phasmatys_barrier] paying 2 ectotokens and passing the barrier, free
    //  after Ghosts Ahoy
    private suspend fun Dialogue.ghostGuard() {
        val questDone = QuestRequirements.hasCompleted(player, GHOSTS_AHOY)
        if (!player.understandsGhosts() && questDone) {
            mesbox(
                "The ghostly guards wail at you incomprehensibly, and though you cannot " +
                    "understand their exact words you understand their meaning - you may pass " +
                    "the barriers of Port Phasmatys without charge."
            )
            return
        }
        if (questDone) {
            chatNpc(
                happy,
                "All visitors to Port Phasmatys must pay a toll charge of 2 Ectotokens. " +
                    "However, you have done the ghosts of our town a service that surpasses all " +
                    "value, so you may pass without charge.",
            )
            return
        }
        if (!player.understandsGhosts()) {
            mesbox(
                "The ghostly guards wail at you incomprehensibly, and though you cannot " +
                    "understand their exact words you understand their meaning - you may not " +
                    "pass the barriers of Port Phasmatys."
            )
            return
        }
        chatNpc(neutral, "All visitors to Port Phasmatys must pay a toll charge of 2 Ectotokens.")
        val option =
            choice3(
                "I would like to enter Port Phasmatys - here's 2 Ectotokens.",
                1,
                "I'm not paying you Ectotokens just to go through a gate.",
                2,
                "Where can I get Ectotokens?",
                3,
            )
        when (option) {
            1 -> chatPlayer(happy, "I would like to enter Port Phasmatys - here's 2 Ectotokens.")
            2 -> {
                chatPlayer(angry, "I'm not paying you Ectotokens just to go through a gate.")
                chatNpc(neutral, "Sorry - it's Town Policy.")
            }
            3 -> {
                chatPlayer(quiz, "Where can I get Ectotokens?")
                chatNpc(
                    neutral,
                    "You need to go to the Temple and earn some. Talk to the disciples - they " +
                        "will tell you how.",
                )
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ghost_captain
    // https://oldschool.runescape.wiki/w/Transcript:Ghosts_Ahoy (travelling to Dragontooth Island)
    // TODO(content): [system:ship_travel] taking 25 or 500 ectotokens, the permanent access
    //  unlock and sailing between Port Phasmatys and Dragontooth Island
    private suspend fun Dialogue.ghostCaptain() {
        if (player.coords.mx == DRAGONTOOTH_MX && player.coords.mz == DRAGONTOOTH_MZ) {
            return dragontoothCaptain()
        }
        if (!player.understandsGhosts()) {
            return wail(cannotUnderstand = false)
        }
        chatNpc(
            neutral,
            "Would you like to visit Dragontooth Island? It is a most pleasant island in the sea " +
                "between Morytania and the lands of the east. It'll cost you $TRIP_FARE " +
                "ectotokens for the return trip.",
        )
        val option =
            if (canAffordTrip()) {
                choice3(
                    "Okay, here's 25 ectotokens. Let's go.",
                    1,
                    "If I pay 500 ectotokens, can I get unlimited trips?",
                    2,
                    "Not now, thanks.",
                    3,
                    title = "Select an option",
                )
            } else {
                choice2(
                    "If I pay 500 ectotokens, can I get unlimited trips?",
                    2,
                    "Not now, thanks.",
                    3,
                    title = "Select an option",
                )
            }
        when (option) {
            1 -> chatPlayer(happy, "Okay, here's 25 ectotokens. Let's go.")
            2 -> unlimitedTrips()
            3 -> chatPlayer(neutral, "Not now, thanks.")
        }
    }

    private suspend fun Dialogue.unlimitedTrips() {
        chatPlayer(quiz, "If I pay 500 ectotokens, can I get unlimited trips?")
        if (access.inv.count(ECTOTOKEN) >= UNLIMITED_FARE) {
            chatNpc(happy, "Yes, that seems fair. Okay, hand them over and we can set off.")
            return
        }
        chatNpc(
            neutral,
            "Yes, I'd accept that deal. But as you don't have enough ectotokens for that, would " +
                "you like to pay 25 ectotokens for a trip now?",
        )
        val payNow =
            if (canAffordTrip()) {
                choice2(
                    "Okay, here's 25 ectotokens. Let's go.",
                    true,
                    "Not now, thanks.",
                    false,
                    title = "Select an option",
                )
            } else {
                false
            }
        if (payNow) {
            chatPlayer(happy, "Okay, here's 25 ectotokens. Let's go.")
        } else {
            chatPlayer(neutral, "Not now, thanks.")
        }
    }

    private suspend fun Dialogue.dragontoothCaptain() {
        while (true) {
            val option =
                choice3(
                    "Can you take me back to Phasmatys, now?",
                    1,
                    "There isn't much on this island, is there?",
                    2,
                    "Actually, I don't want anything.",
                    3,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Can you take me back to Phasmatys, now?")
                    if (player.understandsGhosts()) {
                        chatNpc(neutral, "Yes, climb in.")
                    } else {
                        mesbox(
                            "The ghost sailor wails at you incomprehensibly, and points at the " +
                                "boat, nodding his ghostly head."
                        )
                    }
                    return
                }
                2 -> {
                    chatPlayer(quiz, "There isn't much on this island, is there?")
                    if (!player.understandsGhosts()) {
                        wail(cannotUnderstand = false)
                        continue
                    }
                    chatNpc(happy, "Beautiful isn't it?")
                    chatPlayer(angry, "But it cost me a lot of ectotokens to get here!")
                    chatNpc(happy, "Worth every token, if you ask me.")
                }
                3 -> {
                    chatPlayer(neutral, "Actually, I don't want anything.")
                    if (!player.understandsGhosts()) {
                        wail(cannotUnderstand = false)
                    }
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.ghostCaptainTravel() {
        if (!player.understandsGhosts()) {
            return wail(cannotUnderstand = false)
        }
        if (!canAffordTrip()) {
            chatNpc(neutral, "A return trip to Dragontooth Island costs 25 ectotokens.")
        }
    }

    private fun Dialogue.canAffordTrip(): Boolean = access.inv.count(ECTOTOKEN) >= TRIP_FARE

    // https://oldschool.runescape.wiki/w/Transcript:Andras
    // TODO(content): [system:ship_travel] taking 10,000 coins (or 1,000,000 for permanent
    //  access) and sailing to Slepe
    private suspend fun Dialogue.andras() {
        chatNpc(
            neutral,
            "Hello there. Want to travel to Slepe? I charge 10,000 coins for the journey.",
        )
        val option =
            choice5(
                "Yes please.",
                1,
                "Where's Slepe?",
                2,
                "Why is it so expensive?",
                3,
                "I'll give you 1,000,000 coins for permanent access.",
                4,
                "No thank you.",
                5,
            )
        when (option) {
            1 -> andrasTravel()
            2 -> {
                chatPlayer(quiz, "Where's Slepe?")
                chatNpc(
                    neutral,
                    "It's a small village to the south of here. You can walk but the boat is " +
                        "safer and faster.",
                )
                chatPlayer(quiz, "Is there anything to do there?")
                chatNpc(
                    neutral,
                    "I guess that depends what interests you. The place is pretty quiet, though " +
                        "there's been rumours recently of a great evil making its home beneath " +
                        "the village.",
                )
                chatNpc(
                    neutral,
                    "There's also Ver Sinhaza to the south, home to the Theatre of Blood. It's " +
                        "definitely worth having a look around, even if you don't intend to " +
                        "participate yourself.",
                )
                andrasOffer()
            }
            3 -> {
                chatPlayer(quiz, "Why is it so expensive?")
                chatNpc(
                    neutral,
                    "Not many people want to visit this part of the world so I have to keep the " +
                        "price high. If I didn't I wouldn't be able to keep my boat maintained.",
                )
                andrasOffer()
            }
            4 -> permanentAccess()
            5 -> chatPlayer(neutral, "No thank you.")
        }
    }

    private suspend fun Dialogue.andrasOffer() {
        chatNpc(quiz, "So, do you want to go to Slepe?")
        andrasYesNo()
    }

    private suspend fun Dialogue.andrasYesNo(title: String = Constants.cm_options) {
        if (choice2("Yes please.", true, "No thank you.", false, title = title)) {
            andrasTravel()
        } else {
            chatPlayer(neutral, "No thank you.")
        }
    }

    private suspend fun Dialogue.andrasTravel() {
        chatPlayer(happy, "Yes please.")
        if (access.inv.count(COINS) < SLEPE_FARE) {
            chatNpc(
                neutral,
                "Doesn't look like you have enough money. I charge 10,000 coins for the journey.",
            )
        }
    }

    private suspend fun Dialogue.permanentAccess() {
        chatPlayer(happy, "I'll give you 1,000,000 coins for permanent access.")
        chatNpc(shocked, "1,000,000 coins! Are you serious?")
        if (access.inv.count(COINS) < PERMANENT_FARE) {
            chatPlayer(sad, "No. I don't have that sort of money.")
            chatNpc(neutral, "Oh, I see. So, do you want to go to Slepe?")
            return andrasYesNo(title = "Select an Option")
        }
        val pay =
            choice2(
                "Yes. Here's 1,000,000 coins.",
                true,
                "No. That's too much for me to spend.",
                false,
            )
        if (pay) {
            chatPlayer(happy, "Yes. Here's 1,000,000 coins.")
            return
        }
        chatPlayer(neutral, "No. That's too much for me to spend.")
        chatNpc(neutral, "Oh, I see. So, do you want to go to Slepe?")
        andrasYesNo()
    }

    // https://oldschool.runescape.wiki/w/Transcript:Pirate_Pete (lacking the requirements)
    // TODO(content): [quest:rum_deal] starting Rum Deal once its requirements are met
    private suspend fun Dialogue.piratePete() {
        chatPlayer(happy, "Hello there!")
        chatNpc(neutral, "Mornin'.")
        chatPlayer(quiz, "Got any quests?")
        chatNpc(
            neutral,
            "I may have a quest, but you don't look like you'd be able to handle the kind of " +
                "monsters I have problems with.",
        )
    }

    private companion object {
        const val GHOST_GUARD = "npc.ahoy_ghost_guard"
        const val GHOST_CAPTAIN = "npc.ahoy_ghost_captain_1"
        const val ANDRAS = "npc.slepe_boatman"
        const val PIRATE_PETE = "npc.deal_pete"
        const val COINS = "obj.coins"
        const val ECTOTOKEN = "obj.ectotoken"
        const val TRIP_FARE = 25
        const val UNLIMITED_FARE = 500
        const val SLEPE_FARE = 10_000
        const val PERMANENT_FARE = 1_000_000
        const val DRAGONTOOTH_MX = 59
        const val DRAGONTOOTH_MZ = 55
        const val GHOSTS_AHOY = "quest_ghostsahoy"
    }
}
