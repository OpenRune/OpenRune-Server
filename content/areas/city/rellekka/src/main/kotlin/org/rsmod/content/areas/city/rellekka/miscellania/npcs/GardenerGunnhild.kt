package org.rsmod.content.areas.city.rellekka.miscellania.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invTakeFee
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Gardener_Gunnhild
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [verify:in_game] what Gunnhild says when the player can't pay 15 coins
class GardenerGunnhild @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.misc_gardener") { startDialogue(it.npc) { gunnhild() } }
    }

    private suspend fun Dialogue.gunnhild() {
        if (QuestRequirements.hasCompleted(player, THRONE_OF_MISCELLANIA)) {
            royalGunnhild()
            return
        }
        chatNpc(happy, "Hello.")
        chatPlayer(quiz, "Good day. What are you doing?")
        chatNpc(
            neutral,
            "I'm weeding this herb garden. It's hard work! I also sell a few gardening supplies.",
        )
        when (choice2("Good luck with that.", 1, "What do you have for sale?", 2)) {
            1 -> {
                chatPlayer(happy, "Good luck with that.")
                chatNpc(happy, "Thanks.")
            }
            2 -> forSale()
        }
    }

    private suspend fun Dialogue.forSale() {
        chatPlayer(quiz, "What do you have for sale?")
        chatNpc(
            happy,
            "At the moment I'm selling garden rakes and iron sickles. They're 15gp each, if " +
                "you'd like one?",
        )
        val option =
            choice4(
                "I'd like a rake, please.",
                1,
                "I'd like an iron sickle, please.",
                2,
                "No, thank you.",
                3,
                "What are iron sickles for?",
                4,
            )
        when (option) {
            1 -> {
                chatPlayer(happy, "I'd like a rake, please.")
                buy("obj.rake")
            }
            2 -> {
                chatPlayer(happy, "I'd like an iron sickle, please.")
                buy("obj.iron_sickle")
            }
            3 -> chatPlayer(neutral, "No, thank you.")
            4 -> {
                chatPlayer(quiz, "What are iron sickles for?")
                chatNpc(
                    neutral,
                    "I can't think of anything just now. But you can buy one if you like!",
                )
            }
        }
    }

    private suspend fun Dialogue.royalGunnhild() {
        chatPlayer(happy, "Hello there.")
        chatNpc(happy, "Good day, Your Royal Highness.")
        chatPlayer(quiz, "Good day. What are you doing?")
        chatNpc(
            neutral,
            "I'm weeding this herb garden. It's hard work! I also sell a few gardening supplies.",
        )
        if (choice2("Good luck with that.", true, "What do you have for sale?", false)) {
            chatPlayer(happy, "Good luck with that.")
            chatNpc(happy, "Thank you, Your Royal Highness.")
            return
        }
        chatPlayer(quiz, "What do you have for sale?")
        chatNpc(
            happy,
            "At the moment I'm selling garden rakes and iron sickles. If you'd like one, Your " +
                "Royal Highness,",
        )
        val option =
            choice4(
                "I'd like a rake, please.",
                1,
                "I'd like an iron sickle, please.",
                2,
                "What are iron sickles for?",
                3,
                "No, thank you.",
                4,
            )
        when (option) {
            1 -> {
                chatPlayer(happy, "I'd like a rake, please.")
                buy("obj.rake")
            }
            2 -> {
                chatPlayer(happy, "I'd like an iron sickle, please.")
                buy("obj.iron_sickle")
            }
            3 -> {
                chatPlayer(quiz, "What are iron sickles for?")
                chatNpc(
                    neutral,
                    "I can't think of anything just now. But you can buy one if you like, Your " +
                        "Highness!",
                )
            }
            4 -> chatPlayer(neutral, "No, thank you.")
        }
    }

    private suspend fun Dialogue.buy(obj: String) {
        if (!player.invTakeFee(PRICE)) {
            return
        }
        player.invAddOrDrop(objRepo, obj)
        chatNpc(happy, "Thank you! Here you go.")
    }

    private companion object {
        const val PRICE = 15
        const val THRONE_OF_MISCELLANIA = "quest_throneofmiscellania"
    }
}
