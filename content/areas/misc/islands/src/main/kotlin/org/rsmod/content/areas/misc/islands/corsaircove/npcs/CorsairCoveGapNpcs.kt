package org.rsmod.content.areas.misc.islands.corsaircove.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class CorsairCoveGapNpcs : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.corscurs_thief") { startDialogue(it.npc) { arsen() } }
        onOpNpc1("npc.corscurs_thief_postquest") { startDialogue(it.npc) { arsen() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Arsen_the_Thief
    private suspend fun Dialogue.arsen() {
        if (QuestRequirements.hasCompleted(player, CORSAIR_CURSE)) {
            chatNpc(
                happy,
                "Thanks again for clearing up that 'curse'. Ever since you proved it wasn't " +
                    "supernatural, I've been feeling a lot better.",
            )
            chatPlayer(happy, "You're welcome.")
            return
        }
        chatNpc(sad, "Urgh...")
        mesbox("He looks too sick to chat.")
    }

    private companion object {
        const val CORSAIR_CURSE = "quest_corsaircurse"
    }
}
