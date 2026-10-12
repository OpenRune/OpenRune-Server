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

// https://oldschool.runescape.wiki/w/Transcript:Flower_Girl
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [verify:in_game] one mixed flowers for 15 coins although she says "15gp for
// three" (https://oldschool.runescape.wiki/w/Mixed_flowers lists her price as 15 each)
class FlowerGirl @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.misc_flowergirl") { startDialogue(it.npc) { flowerGirl() } }
    }

    private suspend fun Dialogue.flowerGirl() {
        if (QuestRequirements.hasCompleted(player, THRONE_OF_MISCELLANIA)) {
            chatNpc(happy, "Good day, Your Royal Highness.")
            chatPlayer(quiz, "Good day. What are you doing?")
            chatNpc(
                happy,
                "I'm selling flowers, 15gp for three. Would you like some, Your Highness?",
            )
        } else {
            chatNpc(happy, "Hello.")
            chatPlayer(quiz, "Good day. What are you doing?")
            chatNpc(happy, "I'm selling flowers, 15gp for three. Would you like some?")
        }
        if (player.inv.count("obj.coins") < PRICE) {
            chatPlayer(sad, "I don't have enough money.")
            return
        }
        when (choice2("Yes, please.", 1, "No, thank you.", 2)) {
            1 -> {
                chatPlayer(happy, "Yes, please.")
                if (!player.invTakeFee(PRICE)) {
                    return
                }
                player.invAddOrDrop(objRepo, "obj.flowers_waterfall_quest_mixed")
                chatNpc(happy, "Thank you! Here you go.")
            }
            2 -> chatPlayer(neutral, "No, thank you.")
        }
    }

    private companion object {
        const val PRICE = 15
        const val THRONE_OF_MISCELLANIA = "quest_throneofmiscellania"
    }
}
