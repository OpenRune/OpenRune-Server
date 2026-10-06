package org.rsmod.content.quest.area.baxtorianfalls.waterfall.npcs

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.AlmeraNpc
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.EnteredTomb
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.MetHudon
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.ReadBook
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.RecommendedCombat
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.Started
import org.rsmod.content.quest.manager.menu
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Almera, in her house at the top of the falls. She starts the quest. */
class Almera @Inject constructor(private val waterfall: WaterfallQuest) : PluginScript() {

    override fun ScriptContext.startup() {
        onOpNpc1(AlmeraNpc) { startDialogue(it.npc) { almera() } }
    }

    private suspend fun Dialogue.almera() {
        when (waterfall.stage(player)) {
            0 -> notStarted()
            Started -> {
                chatPlayer(happy, "Hello Almera.")
                chatNpc(happy, "Hello brave adventurer, have you seen my boy yet?")
                chatPlayer(neutral, "I'm afraid not, but I'm sure he hasn't gone far.")
                chatNpc(worried, "I do hope so, you can't be too careful these days.")
            }
            MetHudon -> {
                chatPlayer(happy, "Hello again.")
                chatNpc(happy, "Well hello, you're still around then.")
                chatPlayer(sad, "I saw Hudon by the river but he refused to come back with me.")
                chatNpc(
                    angry,
                    "Yes he told me, the foolish lad came in drenched to the bone, he had " +
                        "fallen into the waterfall, lucky he wasn't killed! Now he can spend " +
                        "the rest of the summer in his room.",
                )
                chatPlayer(quiz, "Any ideas what I could do while I'm here?")
                chatNpc(neutral, "Why don't you visit the tourist centre south of the waterfall?")
            }
            ReadBook -> {
                chatPlayer(happy, "Hello again Almera.")
                chatNpc(
                    happy,
                    "Well hello again brave adventurer, are you enjoying the tranquil scenery " +
                        "of these parts?",
                )
                chatPlayer(happy, "Yes, very relaxing.")
                chatNpc(
                    happy,
                    "Well I'm glad to hear it The authorities wanted to dig up this whole area " +
                        "for a mine, but the few locals who lived here wouldn't budge and they " +
                        "gave up.",
                )
                chatPlayer(happy, "Good for you.")
                chatNpc(laugh, "Good for all of us!")
            }
            in EnteredTomb..Int.MAX_VALUE -> afterTheTomb()
        }
    }

    private suspend fun Dialogue.afterTheTomb() {
        chatPlayer(happy, "Hello Almera.")
        chatNpc(quiz, "Hello adventurer, how's your treasure hunt going?")
        chatPlayer(neutral, "Oh, I'm just sight seeing.")
        chatNpc(
            laugh,
            "No adventurer stays here this long just to sight see. But your business is yours " +
                "alone, if you need to use the raft go ahead. But please try not crash it this " +
                "time!",
        )
        chatPlayer(happy, "Thanks Almera.")
    }

    private suspend fun Dialogue.notStarted() {
        chatPlayer(happy, "Hello.")
        chatNpc(
            happy,
            "Ah, hello there. Nice to see an outsider for a change. Are you busy? I have a " +
                "problem.",
        )
        if (player.combatLevel < RecommendedCombat) {
            mesbox(
                "Before starting this quest, be aware that your combat level is lower than the " +
                    "recommended level of $RecommendedCombat."
            )
        }
        val title = "Start the ${waterfall.quest.displayName}?"
        val start = menu("Yes." to true, "No." to false, title = title)
        if (!start) {
            chatPlayer(neutral, "I'm afraid I'm in a rush.")
            chatNpc(sad, "Oh okay, never mind.")
            return
        }
        chatPlayer(quiz, "How can I help?")
        chatNpc(
            worried,
            "It's my son Hudon, he's always getting into trouble. The boy's convinced there's " +
                "hidden treasure in the river and I'm a bit worried about his safety, the poor " +
                "lad can't even swim.",
        )
        chatPlayer(happy, "I could go and take a look for you if you like?")
        waterfall.quest.advanceQuestStage(access)
        chatNpc(
            happy,
            "Would you? You are kind. You can use the small raft out back if you wish, do be " +
                "careful, the current down stream is very strong.",
        )
    }
}
