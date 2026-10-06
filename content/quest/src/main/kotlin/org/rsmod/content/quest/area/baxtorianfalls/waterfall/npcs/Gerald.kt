package org.rsmod.content.quest.area.baxtorianfalls.waterfall.npcs

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.GeraldNpc
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.MetHudon
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.Started
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.heardOfTreasure
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Gerald fishes on the bank where the river washes treasure hunters ashore. */
class Gerald @Inject constructor(private val waterfall: WaterfallQuest) : PluginScript() {

    override fun ScriptContext.startup() {
        onOpNpc1(GeraldNpc) { startDialogue(it.npc) { gerald() } }
    }

    private suspend fun Dialogue.gerald() {
        val stage = waterfall.stage(player)
        when {
            stage == 0 -> {
                chatPlayer(happy, "Hello there.")
                chatNpc(
                    happy,
                    "Good day, traveller. Come to fish, or just taking in the view? I've " +
                        "landed some real monsters down here.",
                )
                chatPlayer(quiz, "Is that so?")
                chatNpc(laugh, "The last one was THIS big!")
            }
            stage == Started -> {
                chatPlayer(happy, "Hello.")
                chatNpc(happy, "Hello there.")
                chatPlayer(quiz, "Have you seen a young boy around here?")
                chatNpc(laugh, "Can't say I have. Plenty of young fish, mind.")
            }
            player.heardOfTreasure -> {
                chatNpc(happy, "Hello there.")
                chatNpc(quiz, "Back again, traveller? Fishing, or treasure hunting?")
                chatPlayer(quiz, "What makes you say that?")
                chatNpc(neutral, AdventurersLine)
            }
            stage >= MetHudon -> {
                chatPlayer(happy, "Hello.")
                chatNpc(quiz, "Hello traveller. Here to fish, or to go looking for treasure?")
                chatPlayer(quiz, "What makes you say that?")
                chatNpc(neutral, AdventurersLine)
                chatPlayer(quiz, "What is it they're looking for?")
                chatNpc(neutral, LegendLine)
                chatPlayer(quiz, "Interesting. Where could I find out more?")
                chatNpc(happy, HadleyLine)
                player.heardOfTreasure = true
            }
        }
    }
}

private const val GeraldName = "Gerald"
private const val AdventurersLine =
    "Adventurers come through here every week. None of them ever find a thing."
private const val LegendLine =
    "Legend has it the old elf king left a treasure hidden inside the waterfall. Not that " +
        "anybody has ever found it."
private const val HadleyLine = "Try Hadley, the tourist guide. He's in the building right here."

/**
 * Gerald's greeting when the river dumps the player at his feet, once they have met Hudon and
 * before they have heard the legend.
 */
internal suspend fun ProtectedAccess.geraldGreetsWashedUp(waterfall: WaterfallQuest) {
    if (waterfall.stage(player) < MetHudon || player.heardOfTreasure) {
        return
    }
    startDialogue {
        chatNpcSpecific(
            GeraldName,
            GeraldNpc,
            shocked,
            "Good grief! Where did you spring from? You're not another of those treasure " +
                "hunters, are you?",
        )
        chatPlayer(quiz, "Treasure hunters?")
        chatNpcSpecific(GeraldName, GeraldNpc, neutral, LegendLine)
        chatPlayer(quiz, "Interesting. Where could I find out more?")
        chatNpcSpecific(GeraldName, GeraldNpc, happy, HadleyLine)
        player.heardOfTreasure = true
    }
}
