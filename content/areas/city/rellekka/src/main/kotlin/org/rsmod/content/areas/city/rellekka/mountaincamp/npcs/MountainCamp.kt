package org.rsmod.content.areas.city.rellekka.mountaincamp.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:mountain_daughter] the wiki only has the camp dwellers' dialogue after the
// quest: the lvl 25 dweller's thanks waits for the quest, the others' lines are used before it too
// https://oldschool.runescape.wiki/w/Transcript:Camp_dweller
class MountainCamp : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.mdaughter_camp_female3") {
            if (QuestRequirements.hasCompleted(player, MOUNTAIN_DAUGHTER)) {
                startDialogue(it.npc) { grateful() }
            }
        }
        onOpNpc1("npc.mdaughter_camp_male1") {
            startDialogue(it.npc) { chatNpc(neutral, "I'd rather be left alone, outerlander.") }
        }
        onOpNpc1("npc.mdaughter_camp_male2") { startDialogue(it.npc) { hello() } }
        onOpNpc1("npc.mdaughter_camp_female1") { startDialogue(it.npc) { nicePlace() } }
        onOpNpc1("npc.mdaughter_camp_female2") { startDialogue(it.npc) { niceDay() } }
    }

    private suspend fun Dialogue.grateful() {
        chatNpc(happy, "I've heard about all the things you did for us!")
        chatPlayer(happy, "I was happy to help.")
        chatNpc(
            happy,
            "And I'm happy that you helped us! Maybe now this place can be a bit more cheerful " +
                "again!",
        )
    }

    private suspend fun Dialogue.hello() {
        chatPlayer(happy, "Hello!")
        chatNpc(neutral, "Hello.")
    }

    private suspend fun Dialogue.nicePlace() {
        chatPlayer(happy, "Nice place you have here!")
        chatNpc(neutral, "It is indeed.")
    }

    private suspend fun Dialogue.niceDay() {
        chatPlayer(happy, "Nice day, isn't it?")
        chatNpc(neutral, "Yes.")
    }

    private companion object {
        const val MOUNTAIN_DAUGHTER = "quest_mountaindaughter"
    }
}
