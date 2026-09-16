package org.rsmod.content.areas.misc.dwarvenmine.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class DwarvenMineNpcsScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.boot_the_dwarf") { startDialogue(it.npc) { boot() } }
        onOpNpc1("npc.favour_hammerspike_stoutbeard") { startDialogue(it.npc) { hammerspike() } }
        for (gangMember in GANG_MEMBERS) {
            onOpNpc1(gangMember) { startDialogue(it.npc) { gangMember() } }
        }
        onOpNpc1("npc.motherlode_guard") { startDialogue(it.npc) { motherlodeGuard() } }
        onOpNpc1(CART_CONDUCTOR) { startDialogue(it.npc) { conductor() } }
        onOpNpc3(CART_CONDUCTOR) { startDialogue(it.npc) { conductorTickets() } }
    }

    private suspend fun Dialogue.conductor() {
        while (true) {
            when (
                choice4(
                    "Who are you?",
                    1,
                    "Where can you take me?",
                    2,
                    "I'd like to buy a ticket.",
                    3,
                    "I have to go.",
                    4,
                )
            ) {
                1 -> {
                    chatPlayer(quiz, "Who are you?")
                    chatNpc(
                        neutral,
                        "I'm an employee of Keldagrim Carts. I make sure the carts in this area run on time " +
                            "and that people pay their fares.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "Where can you take me?")
                    if (player.hasStartedGiantDwarf()) {
                        chatNpc(neutral, "This track leads right up to Keldagrim. Only stop.")
                    } else {
                        chatNpc(
                            neutral,
                            "I don't think I'm allowed to take you into the city of Keldagrim, human. Perhaps " +
                                "when you find another way into the city and talk to someone of importance there " +
                                "you will be allowed to.",
                        )
                    }
                }
                3 -> if (buyTicketFromMenu()) return
                else -> {
                    chatPlayer(neutral, "I have to go.")
                    chatNpc(
                        neutral,
                        "Just remember, wherever you go, you go there faster through Keldagrim Carts.",
                    )
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.buyTicketFromMenu(): Boolean {
        chatPlayer(neutral, "I'd like to buy a ticket.")
        chatNpc(neutral, "One ticket to Keldagrim, that's $TICKET_PRICE coins then.")
        val charmed = wearsCharmedRing()
        val choice =
            if (charmed) {
                choice3("Buy.", 1, "Don't buy.", 2, "[Charm] Only pay half.", 3)
            } else {
                choice2("Buy.", 1, "Don't buy.", 2)
            }
        when (choice) {
            1 -> {
                if (!purchaseTicket(TICKET_PRICE)) return false
                chatNpc(happy, "Thanks for your custom!")
            }
            2 -> {
                chatPlayer(neutral, "No thanks, I changed my mind.")
                return false
            }
            else -> {
                if (!purchaseTicket(TICKET_PRICE / 2)) return false
                chatNpc(neutral, "This is only ${TICKET_PRICE / 2} coins, I can't let you have a ticket for that.")
                chatPlayer(happy, "Oh, don't worry. I'll pay you another 75 coins for the next ticket I buy, alright?")
                chatNpc(neutral, "Well... I suppose so.")
            }
        }
        return true
    }

    private suspend fun Dialogue.conductorTickets() {
        val price = if (wearsCharmedRing()) TICKET_PRICE / 2 else TICKET_PRICE
        if (choice2("Keldagrim ($price coins)", true, "Cancel", false)) {
            purchaseTicket(price)
        }
    }

    private suspend fun Dialogue.purchaseTicket(price: Int): Boolean {
        val inv = access.inv
        if (inv.count("obj.coins") < price) {
            chatPlayer(sad, "I'm sorry, I've run out of money.")
            return false
        }
        if (inv.isFull() && inv.count("obj.coins") != price) {
            chatNpc(neutral, "I'm sorry, sir, but I don't believe you can hold the ticket.")
            return false
        }
        if (access.invDel(inv, "obj.coins", price).failure) {
            return false
        }
        access.invAdd(inv, TICKET)
        return true
    }

    private fun Dialogue.wearsCharmedRing(): Boolean = access.worn.count("obj.ring_of_charos_unlocked") > 0

    private fun Player.hasStartedGiantDwarf(): Boolean =
        QuestRequirements.isOnQuest(this, GIANT_DWARF) || QuestRequirements.hasCompleted(this, GIANT_DWARF)

    private suspend fun Dialogue.boot() {
        chatNpc(neutral, "Hello tall person.")
        while (true) {
            if (choice2("Hello short person.", true, "Why are you called boot?", false)) {
                chatPlayer(neutral, "Hello short person.")
                chatNpc(neutral, "Hello tall person.")
                continue
            }
            chatPlayer(quiz, "Why are you called Boot?")
            chatNpc(
                neutral,
                "I'm called Boot, because when I was very young, I used to sleep, in a large boot.",
            )
            chatPlayer(bored, "Yeah, great, I didn't want your life story.")
            return
        }
    }

    private suspend fun Dialogue.hammerspike() {
        chatNpc(angry, "You looking at me? I don't see nobody else here!")
    }

    private suspend fun Dialogue.gangMember() {
        chatNpc(angry, "Yeah...whada you want?")
        while (true) {
            when (
                choice4(
                    "Who are you?",
                    1,
                    "What're you doing here?",
                    2,
                    "Who do you work for?",
                    3,
                    "Ok, thanks.",
                    4,
                )
            ) {
                1 -> {
                    chatPlayer(quiz, "Who are you?")
                    chatNpc(
                        angry,
                        "I'm nobody you need to worry about...who are you? Why are you asking so many " +
                            "questions? Are you with the Varrock guards?",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "What're you doing here?")
                    chatNpc(
                        neutral,
                        "Well, as if it's any of your business, I'm an associate of Hammerspike. He's a great " +
                            "dwarf you know. You could learn a lot from a dwarf like him.",
                    )
                }
                3 -> {
                    chatPlayer(quiz, "Who do you work for?")
                    chatNpc(
                        neutral,
                        "I have an ongoing contract with Hammerspike, when he gives the word, the hammer " +
                            "starts flying.",
                    )
                }
                else -> {
                    chatPlayer(neutral, "Okay, thanks.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.motherlodeGuard() {
        chatNpc(angry, "Halt! Are you here to sabotage the Motherlode Mine?")
        when (
            choice3(
                "What's the Motherlode Mine?",
                1,
                "No, I'm not.",
                2,
                "What would you do if I said 'yes'?",
                3,
                title = "What would you like to say?",
            )
        ) {
            1 -> {
                chatPlayer(quiz, "What's the Motherlode Mine?")
                chatNpc(
                    neutral,
                    "Prospector Percy discovered unusual mineral veins in this cave. Now he's built a machine to " +
                        "let us extract useful ores.",
                )
                chatNpc(
                    neutral,
                    "Percy calls it the Motherlode. I don't know why, and I'm not asking - that's between him " +
                        "and his mother.",
                )
                chatNpc(
                    neutral,
                    "Dwarves don't normally let humans claim areas of the mine like this, but Percy's machine is " +
                        "really useful, so we struck a deal.",
                )
            }
            2 -> {
                chatPlayer(neutral, "No, I'm not.")
                chatNpc(happy, "That's a relief. Have a nice day.")
            }
            else -> {
                chatPlayer(quiz, "What would you do if I said 'yes'?")
                chatNpc(neutral, "I'd ask you not to.")
            }
        }
    }

    private companion object {
        const val CART_CONDUCTOR = "npc.dwarf_city_train_conductor7"
        const val GIANT_DWARF = "quest_giantdwarf"
        const val TICKET = "obj.dwarf_minecart_ticket_ice_kelda"
        const val TICKET_PRICE = 150

        val GANG_MEMBERS =
            listOf("npc.favour_gangster_dwarf", "npc.favour_gangster_dwarf_2", "npc.favour_gangster_dwarf_3")
    }
}
