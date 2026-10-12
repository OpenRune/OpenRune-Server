package org.rsmod.content.areas.city.desert.pollnivneach.npcs

import jakarta.inject.Inject
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class PollnivneachGangs
@Inject
constructor(private val aiPlayerInteractions: AiPlayerInteractions) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.feud_arabian_guard_multi") { startDialogue(it.npc) { bandit() } }
        onOpNpc1("npc.feud_arabian_guard2_multi") { startDialogue(it.npc) { bandit() } }
        onOpNpc1("npc.feud_egyptian_doorman_multi") { startDialogue(it.npc) { menaphiteThug(it.npc) } }
        onOpNpc1("npc.feud_egyptian_minder_multi") { startDialogue(it.npc) { operator() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Bandit_(Pollnivneach)
    // TODO(content): [quest:the_feud] line during the quest once the Bandit Champion is killed
    private suspend fun Dialogue.bandit() {
        if (QuestRequirements.hasCompleted(player, THE_FEUD)) {
            chatNpc(angry, "You dog! You killed our champion and for that you will die!")
            return
        }
        chatPlayer(happy, "Hello.")
        chatNpc(angry, "What do you want?")
        chatPlayer(
            neutral,
            "Nothing really, I'm new to town and wanted to get to know the locals.",
        )
        chatNpc(
            angry,
            "I'm too busy for chitchat. Come back if you have some business to discuss with us.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Menaphite_Thug
    private suspend fun Dialogue.menaphiteThug(npc: Npc) {
        if (QuestRequirements.hasCompleted(player, THE_FEUD)) {
            menaphiteThugAfterFeud(npc)
            return
        }
        chatPlayer(happy, "Hello.")
        chatNpc(angry, "What do you want?")
        chatPlayer(
            neutral,
            "Nothing really, I'm new to town and wanted to get to know the locals.",
        )
        chatNpc(
            angry,
            "I'm too busy for chitchat. Come back if you have some business to discuss with us.",
        )
    }

    // TODO(content): [verify:in_game] the thug attacking mid-dialogue
    private suspend fun Dialogue.menaphiteThugAfterFeud(npc: Npc) {
        chatPlayer(happy, "Hello.")
        chatNpc(angry, "You treacherous jackal! I should never have trusted you.")
        chatPlayer(laugh, "What can I say, you live an learn")
        npc.opPlayer2(player, aiPlayerInteractions)
        chatNpc(angry, "I will avenge my master!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ali_the_Operator
    // TODO(content): [quest:the_feud] "I'm looking for Ali." during the quest
    private suspend fun Dialogue.operator() {
        chatPlayer(happy, "Hello good sir.")
        chatNpc(angry, "What do you want?")
        chatPlayer(neutral, "I'm new in town and wanted to get to know the locals.")
        chatNpc(neutral, "What do you want to know?")
        while (true) {
            val aboutYou =
                choice2(
                    "Tell me about yourself.",
                    true,
                    "Tell me about the other people in the town.",
                    false,
                )
            if (aboutYou) {
                chatPlayer(quiz, "Tell me about yourself.")
                chatNpc(
                    shifty,
                    "That information is available on a need to know basis and right now, you " +
                        "don't need to know.",
                )
            } else {
                chatPlayer(quiz, "Tell me about the other people in the town.")
                chatNpc(
                    shifty,
                    "Sheep, ready for the slau... hang on. I shouldn't be telling you that. Just " +
                        "forget I said anything.",
                )
            }
            chatNpc(neutral, "Can I help you with anything else?")
            val more =
                choice2(
                    "Yes I'd like to ask you about something else.",
                    true,
                    "No thanks.",
                    false,
                )
            if (!more) {
                chatPlayer(neutral, "No thanks.")
                return
            }
            chatPlayer(happy, "Yes I'd like to ask you about something else.")
        }
    }

    private companion object {
        const val THE_FEUD = "quest_feud"
    }
}
