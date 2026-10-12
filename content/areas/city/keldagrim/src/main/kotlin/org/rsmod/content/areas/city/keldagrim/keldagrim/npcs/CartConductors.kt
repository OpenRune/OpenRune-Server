package org.rsmod.content.areas.city.keldagrim.keldagrim.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invTakeFee
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Cart_conductor
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class CartConductors @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        for ((conductor, variant) in TICKET_SELLERS) {
            onOpNpc1(conductor) { startDialogue(it.npc) { conductor(variant) } }
            onOpNpc3(conductor) { startDialogue(it.npc) { tickets() } }
        }
        onOpNpc1("npc.dwarf_city_train_conductor3") { startDialogue(it.npc) { busyConductor() } }
        onOpNpc1("npc.dwarf_city_train_conductor5") { startDialogue(it.npc) { onBreak() } }
        onOpNpc1("npc.slice_dwarf_inspector") { startDialogue(it.npc) { ticketDwarf() } }
        for (dwarf in STATION_DWARVES) {
            onOpNpc1(dwarf) { startDialogue(it.npc) { stationDwarf() } }
        }
    }

    private suspend fun Dialogue.conductor(variant: Int) {
        when (variant) {
            1 -> {
                val title = if (access.isBodyTypeA()) "sir" else "m'am"
                chatNpc(neutral, "Yes $title? Can I help you at all?")
            }
            2 -> chatNpc(neutral, "Anything I can help you with?")
            4 -> chatNpc(happy, "Tickets, tickets, get your tickets here!")
        }
        while (true) {
            val option =
                choice4(
                    "What is this place?",
                    1,
                    "Where can you take me?",
                    2,
                    "I'd like to buy a ticket.",
                    3,
                    "I have to go.",
                    4,
                )
            when (option) {
                1 -> station(variant)
                2 -> {
                    if (variant != 2) {
                        chatPlayer(quiz, "Where can you take me?")
                    }
                    chatNpc(
                        neutral,
                        "Let's see, we could let you travel to Ice Mountain, or to the Grand " +
                            "Exchange.",
                    )
                    if (whiteWolfMountainOpen()) {
                        chatNpc(
                            neutral,
                            "I could also let you go to White Wolf Mountain, since you've " +
                                "befriended our brothers there.",
                        )
                    }
                }
                3 -> if (buyTicketFromMenu()) return
                else -> {
                    chatPlayer(neutral, "I have to go.")
                    chatNpc(
                        neutral,
                        "Just remember, wherever you go, you go there faster through Keldagrim " +
                            "Carts.",
                    )
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.station(variant: Int) {
        chatPlayer(quiz, "What is this place?")
        if (variant == 1) {
            chatNpc(neutral, "You're in the main waiting area of Keldagrim Carts- station.")
        } else {
            chatNpc(neutral, "This is the Keldagrim Carts-station, human.")
        }
        chatPlayer(quiz, "Cart-station? What goes on here then?")
        chatNpc(
            neutral,
            "Keldagrim is the hub of all the traffic of the dwarven realm. Almost every dwarven " +
                "outpost has railtracks leading to Keldagrim, connecting them all for easy " +
                "travel and transportation of goods.",
        )
        chatPlayer(happy, "Amazing! And what powers the carts? Magic?")
        chatNpc(
            neutral,
            "Oh, no no, we dwarves don't use magic. No, all of this is powered by steam " +
                "engines, developed during the past few centuries after the Consortium came to " +
                "power.",
        )
        chatPlayer(happy, "Thanks for the info.")
    }

    private suspend fun Dialogue.buyTicketFromMenu(): Boolean {
        chatPlayer(neutral, "I'd like to buy a ticket.")
        chatNpc(quiz, "And where would you like to go?")
        val destination =
            if (whiteWolfMountainOpen()) {
                choice4(
                    "To the Grand Exchange.",
                    1,
                    "To Ice Mountain.",
                    2,
                    "To White Wolf Mountain.",
                    4,
                    "Nowhere.",
                    3,
                )
            } else {
                choice3("To the Grand Exchange.", 1, "To Ice Mountain.", 2, "Nowhere.", 3)
            }
        when (destination) {
            1 -> {
                chatPlayer(neutral, "I'd like to buy a ticket to the Grand Exchange.")
                chatNpc(
                    happy,
                    "Oh, that service is free. Just hop on a cart on Track 3, and away you go.",
                )
                return true
            }
            2 -> {
                chatPlayer(neutral, "I'd like to buy a ticket to Ice Mountain.")
                chatNpc(neutral, "That'll be $ICE_MOUNTAIN_PRICE gold coins please.")
                return iceMountainPurchase()
            }
            4 -> {
                chatPlayer(neutral, "I'd like to buy a ticket to White Wolf Mountain.")
                chatNpc(neutral, "That'll be $WHITE_WOLF_PRICE gold coins please.")
                return whiteWolfPurchase()
            }
            else -> return true
        }
    }

    private suspend fun Dialogue.iceMountainPurchase(): Boolean {
        val choice =
            if (wearsCharmedRing()) {
                choice3("Buy.", 1, "Don't buy.", 2, "[Charm] Only pay half.", 3)
            } else {
                choice2("Buy.", 1, "Don't buy.", 2)
            }
        when (choice) {
            1 -> {
                if (!buyTicket(ICE_MOUNTAIN_PRICE)) return false
                chatNpc(happy, "Thanks, carts to Ice Mountain depart from track 1.")
            }
            2 -> {
                chatPlayer(neutral, "No thanks, I changed my mind.")
                return false
            }
            else -> {
                if (!buyTicket(ICE_MOUNTAIN_PRICE / 2)) return false
                chatNpc(
                    neutral,
                    "You're alright, I suppose... I can let you go for 75 coins. And remember, " +
                        "carts to Ice Mountain depart from track 1.",
                )
            }
        }
        return true
    }

    private suspend fun Dialogue.whiteWolfPurchase(): Boolean {
        val choice =
            if (wearsCharmedRing()) {
                choice3("Buy.", 1, "Don't buy.", 2, "[Charm] Only pay half.", 3)
            } else {
                choice2("Buy.", 1, "Don't buy.", 2)
            }
        when (choice) {
            1 -> {
                if (!buyTicket(WHITE_WOLF_PRICE, WHITE_WOLF_TICKET)) return false
                chatNpc(happy, "Thanks. Go to track 4 for carts heading to White Wolf Mountain.")
            }
            2 -> {
                chatPlayer(neutral, "No thanks, I changed my mind.")
                return false
            }
            else -> {
                if (!buyTicket(WHITE_WOLF_PRICE / 2, WHITE_WOLF_TICKET)) return false
                chatNpc(
                    neutral,
                    "You're a decent human, I suppose... I can let you go for 50 coins. Go to " +
                        "track 4 for the carts heading to White Wolf Mountain.",
                )
            }
        }
        return true
    }

    // White Wolf Mountain opens once the player has finished Fishing Contest.
    private fun Dialogue.whiteWolfMountainOpen(): Boolean =
        QuestRequirements.hasCompleted(player, FISHING_CONTEST)

    private suspend fun Dialogue.tickets() {
        val divisor = if (wearsCharmedRing()) 2 else 1
        val price = ICE_MOUNTAIN_PRICE / divisor
        val whiteWolfPrice = WHITE_WOLF_PRICE / divisor
        val option =
            if (whiteWolfMountainOpen()) {
                choice4(
                    "Grand Exchange (free)",
                    1,
                    "Ice Mountain ($price coins)",
                    2,
                    "White Wolf Mountain ($whiteWolfPrice coins)",
                    4,
                    "Cancel",
                    3,
                )
            } else {
                choice3(
                    "Grand Exchange (free)",
                    1,
                    "Ice Mountain ($price coins)",
                    2,
                    "Cancel",
                    3,
                )
            }
        when (option) {
            1 ->
                chatNpc(
                    happy,
                    "That service is free. Just hop on a cart on Track 3, and away you go.",
                )
            2 -> buyTicket(price)
            4 -> buyTicket(whiteWolfPrice, WHITE_WOLF_TICKET)
        }
    }

    private suspend fun Dialogue.buyTicket(
        price: Int,
        ticket: String = ICE_MOUNTAIN_TICKET,
    ): Boolean {
        val coins = player.inv.count(COINS)
        if (coins < price) {
            chatPlayer(sad, "I'm sorry, I've run out of money.")
            return false
        }
        if (player.inv.isFull() && coins != price) {
            val title = if (access.isBodyTypeA()) "sir" else "madam"
            chatNpc(neutral, "I'm sorry, $title, but I don't believe you can hold the ticket.")
            return false
        }
        if (!player.invTakeFee(price)) {
            return false
        }
        player.invAddOrDrop(objRepo, ticket)
        return true
    }

    private fun Dialogue.wearsCharmedRing(): Boolean =
        player.worn.count("obj.ring_of_charos_unlocked") > 0

    private suspend fun Dialogue.busyConductor() {
        chatNpc(angry, "Out of the way human!")
        chatPlayer(quiz, "Why, what's happening?")
        chatNpc(
            angry,
            "There's important traffic coming through here and I don't want any stray humans " +
                "distracting me.",
        )
    }

    private suspend fun Dialogue.onBreak() {
        chatNpc(bored, "Wot you want?")
        val option =
            choice4(
                "What is this place?",
                1,
                "Where can you take me?",
                2,
                "I'd like to buy a ticket.",
                3,
                "I have to go.",
                4,
            )
        when (option) {
            1 -> chatPlayer(quiz, "What is this place?")
            2 -> chatPlayer(quiz, "Where can you take me?")
            3 -> chatPlayer(neutral, "I'd like to buy a ticket.")
            else -> {
                chatPlayer(neutral, "I have to go.")
                chatNpc(angry, "Then why did ya waste my time?")
                return
            }
        }
        chatNpc(angry, "Can't you see I'm 'aving a lil' break?")
        chatPlayer(neutral, "Now that you mention it, yes.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ticket_dwarf
    private suspend fun Dialogue.ticketDwarf() {
        chatPlayer(quiz, "Can I buy a ticket?")
        chatNpc(
            happy,
            "There's no need. Under our agreement with the Dorgeshuun, you get free train " +
                "travel in return for services rendered.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Dwarf_(train_station)
    // TODO(content): [verify:in_game] whether they stand there before Another Slice of H.A.M.
    private suspend fun Dialogue.stationDwarf() {
        val line =
            when (access.random.of(5)) {
                0 ->
                    "I hope the goblins are properly maintaining their stretch of track! I'd " +
                        "hate to see them spoil good Dwarven workmanship."
                1 ->
                    "You're ${player.displayName}, aren't you? It's a good job you did helping " +
                        "to get this train link open."
                2 ->
                    "Y'know, at first I didn't like the idea of visiting a goblin city, but " +
                        "these cave goblins are alright."
                3 ->
                    "These goblins have so much more advanced technology than us. Take a look " +
                        "at their magical lights! We could never create something like them."
                else ->
                    "Dorgesh-Kaan's very nice to visit, but I don't think I'd want to live there."
            }
        chatNpc(neutral, line)
    }

    private companion object {
        const val COINS = "obj.coins"
        const val ICE_MOUNTAIN_TICKET = "obj.dwarf_minecart_ticket_kelda_ice"
        const val ICE_MOUNTAIN_PRICE = 150
        const val WHITE_WOLF_TICKET = "obj.dwarf_minecart_ticket_kelda_whitewolf"
        const val WHITE_WOLF_PRICE = 100
        const val FISHING_CONTEST = "quest_fishingcontest"

        val TICKET_SELLERS =
            mapOf(
                "npc.dwarf_city_train_conductor1" to 1,
                "npc.dwarf_city_train_conductor2" to 2,
                "npc.dwarf_city_train_conductor4" to 4,
                "npc.dwarf_city_train_conductor8" to 6,
            )

        val STATION_DWARVES =
            listOf(
                "npc.slice_dwarf1",
                "npc.slice_dwarf2",
                "npc.slice_dwarf3",
                "npc.slice_dwarf4",
                "npc.slice_dwarf5",
                "npc.slice_dwarf6",
            )
    }
}
