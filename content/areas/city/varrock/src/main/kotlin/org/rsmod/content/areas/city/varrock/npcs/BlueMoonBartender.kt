package org.rsmod.content.areas.city.varrock.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invTakeFee
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.output.mes
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.menu
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class BlueMoonBartender @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.bluemoon_bartender") { startDialogue(it.npc) { bartender() } }
    }

    private suspend fun Dialogue.bartender() {
        chatNpc(neutral, "What can I do yer for?")
        when (
            menu(
                "A glass of your finest ale please." to Topic.Ale,
                "Can you recommend where an adventurer might make his fortune?" to Topic.Fortune,
                "Do you know where I can get some good equipment?" to Topic.Equipment,
            )
        ) {
            Topic.Ale -> ale()
            Topic.Fortune -> fortune()
            Topic.Equipment -> {
                chatPlayer(quiz, "Do you know where I can get some good equipment?")
                chatNpc(
                    neutral,
                    "Well, there's the sword shop across the road, or there's also all sorts of " +
                        "shops up around the market.",
                )
            }
        }
    }

    private suspend fun Dialogue.ale() {
        chatPlayer(happy, "A glass of your finest ale please.")
        chatNpc(happy, "No problemo. That'll be 2 coins.")
        if (!player.invTakeFee(fee = BEER_PRICE)) {
            chatPlayer(sad, "Oh dear. I don't seem to have enough money.")
            return
        }
        player.mes("You buy a pint of beer.")
        player.invAddOrDrop(objRepo, "obj.beer")
    }

    private suspend fun Dialogue.fortune() {
        chatPlayer(quiz, "Can you recommend where an adventurer might make his fortune?")
        chatNpc(
            neutral,
            "Ooh I don't know if I should be giving away information, makes the game too easy.",
        )
        when (
            menu(
                "Oh ah well..." to 1,
                "Game? What are you talking about?" to 2,
                "Just a small clue?" to 3,
            )
        ) {
            1 -> chatPlayer(sad, "Oh ah well...")
            2 -> {
                chatPlayer(confused, "Game? What are you talking about?")
                chatNpc(
                    neutral,
                    "This world around us... is an online game... called Old School RuneScape.",
                )
                chatPlayer(
                    confused,
                    "Nope, still don't understand what you are talking about. What does " +
                        "'online' mean?",
                )
                chatNpc(
                    neutral,
                    "It's a sort of connection between magic boxes across the world, big boxes " +
                        "on people's desktops and little ones people can carry. They can talk to " +
                        "each other to play games.",
                )
                chatPlayer(angry, "I give up. You're obviously completely mad!")
            }
            else -> {
                chatPlayer(quiz, "Just a small clue?")
                chatNpc(
                    neutral,
                    "Go and talk to the bartender at the Jolly Boar Inn, he doesn't seem to mind " +
                        "giving away clues.",
                )
            }
        }
    }

    private enum class Topic {
        Ale,
        Fortune,
        Equipment,
    }

    private companion object {
        const val BEER_PRICE = 2
    }
}
