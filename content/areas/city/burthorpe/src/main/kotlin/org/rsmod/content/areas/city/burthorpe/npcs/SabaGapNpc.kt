package org.rsmod.content.areas.city.burthorpe.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class SabaGapNpc : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.death_hermit") { startDialogue(it.npc) { saba() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Saba
    // TODO(content): [quest:death_plateau] dialogue while the quest is in progress
    // TODO(content): [system:treasure_trails] medium clue step reward
    private suspend fun Dialogue.saba() {
        val trollStronghold =
            QuestRequirements.isOnQuest(player, TROLL_STRONGHOLD) ||
                QuestRequirements.hasCompleted(player, TROLL_STRONGHOLD)
        if (trollStronghold) {
            chatPlayer(neutral, "Hello.")
            chatNpc(angry, "Have you got rid of those pesky trolls yet?")
            chatPlayer(worried, "I'm afraid there's been some trouble...")
            chatNpc(angry, "You told me you'd get rid of the trolls!")
            chatPlayer(neutral, "I'm sure the Imperial Guard will deal with them in due course.")
            chatNpc(angry, "Hmph! They'd better!")
            return
        }
        if (QuestRequirements.hasCompleted(player, DEATH_PLATEAU)) {
            chatPlayer(neutral, "Hello.")
            chatNpc(angry, "Have you got rid of those pesky trolls yet?")
            chatPlayer(
                happy,
                "They will be gone soon! The Imperial Guard will use a secret way that starts " +
                    "from the back of the Sherpa's hut to destroy the troll camp!",
            )
            chatNpc(happy, "I shall have peace again at last!")
            chatNpc(
                angry,
                "If those pesky humans don't start trampling all over Death Plateau again that is!",
            )
            return
        }
        chatPlayer(happy, "Hello!")
        chatNpc(angry, "Why won't people leave me alone?!")
    }

    private companion object {
        const val DEATH_PLATEAU = "quest_deathplateau"
        const val TROLL_STRONGHOLD = "quest_trollstronghold"
    }
}
