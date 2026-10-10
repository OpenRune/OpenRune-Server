package org.rsmod.content.quest.area.baxtorianfalls.waterfall.npcs

import dev.openrune.types.hunt.HuntVis
import jakarta.inject.Inject
import org.rsmod.api.hunt.NpcSearch
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.COMPLETE
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.GERALD_NPC
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.MET_HUDON
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.STARTED
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.heardOfTreasure
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class Gerald @Inject constructor(private val waterfall: WaterfallQuest) : PluginScript() {

    override fun ScriptContext.startup() {
        onOpNpc1(GERALD_NPC) { startDialogue(it.npc) { gerald() } }
    }

    private suspend fun Dialogue.gerald() {
        val stage = waterfall.stage(player)
        when {
            stage == 0 || stage == COMPLETE -> beforeTheQuest()
            stage == STARTED -> {
                chatPlayer(happy, "Hello.")
                chatNpc(happy, "Hello there.")
                chatPlayer(quiz, "Have you seen a small boy?")
                chatNpc(laugh, "Nope, plenty of small fish though.")
            }
            player.heardOfTreasure -> {
                chatNpc(happy, "Hello there.")
                chatNpc(quiz, "Hello traveller. Are you here to fish or to hunt for treasure?")
                chatPlayer(quiz, "Why do you say that?")
                chatNpc(neutral, ADVENTURERS_LINE)
            }
            else -> {
                chatPlayer(happy, "Hello.")
                chatNpc(quiz, "Hello traveller. Are you here to fish or to hunt for treasure?")
                chatPlayer(quiz, "Why do you say that?")
                chatNpc(neutral, ADVENTURERS_LINE)
                chatPlayer(quiz, "What treasure are they looking for?")
                chatNpc(neutral, LEGEND_LINE)
                learnOfHadley()
            }
        }
    }

    private suspend fun Dialogue.beforeTheQuest() {
        chatPlayer(happy, "Hello there.")
        chatNpc(
            happy,
            "Good day to you traveller, are you here to fish or just looking around? I've " +
                "caught some beauties down here.",
        )
        chatPlayer(quiz, "Really?")
        chatNpc(laugh, "The last one was this big!")
    }
}

private const val ADVENTURERS_LINE =
    "Adventurers pass through here every week, they never find anything though."
private const val LEGEND_LINE =
    "They say there's treasure hidden within the waterfall, left behind by the old elven king. " +
        "Not that anyone's ever found anything."
private const val GERALD_SEARCH_RADIUS = 8

private suspend fun Dialogue.learnOfHadley() {
    chatPlayer(quiz, "Interesting, is there somewhere I can learn more about this?")
    chatNpc(happy, "You could ask Hadley the tourist guide. He'll be in this building just here.")
    player.heardOfTreasure = true
}

internal suspend fun ProtectedAccess.geraldGreetsWashedUp(
    waterfall: WaterfallQuest,
    search: NpcSearch,
) {
    if (waterfall.stage(player) != MET_HUDON || player.heardOfTreasure) {
        return
    }
    delay(1)
    val gerald =
        npcFind(player.coords, GERALD_NPC, GERALD_SEARCH_RADIUS, HuntVis.Off, search) ?: return
    startDialogue(gerald) {
        chatNpc(
            shocked,
            "Blimey! Where did you come from? Not another one of those treasure hunters are " +
                "you?",
        )
        chatPlayer(quiz, "Treasure hunters?")
        chatNpc(neutral, LEGEND_LINE)
        learnOfHadley()
    }
}
