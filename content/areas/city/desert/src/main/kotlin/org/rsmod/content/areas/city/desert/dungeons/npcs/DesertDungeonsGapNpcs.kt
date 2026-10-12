package org.rsmod.content.areas.city.desert.dungeons.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class DesertDungeonsGapNpcs : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.bcs_scabaras_high_priest") { startDialogue(it.npc) { highPriest() } }
        onOpNpc1("npc.dt2_vault_bandit_vis") { startDialogue(it.npc) { mysteriousBandit() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:High_Priest_of_Scabaras
    private suspend fun Dialogue.highPriest() {
        chatPlayer(happy, "Hello again.")
        chatNpc(
            neutral,
            "Hello, adventurer. Thank you for freeing me from the Devourer. I would be alone " +
                "now. Scabaras teaches us to remain isolated, and I have much to think about.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Mysterious_Bandit
    // TODO(content): [system:met_flags] "Hello again" standard dialogue after the first talk
    // TODO(content): [quest:desert_treasure_ii] ancient lamps to claim and the ring of shadows
    // for a player who never received it
    // TODO(content): [system:shops] the ring of shadows shop has no inv in the cache
    private suspend fun Dialogue.mysteriousBandit() {
        chatPlayer(shifty, "You're looking very shady.")
        chatNpc(shifty, "I'm here for good reason, ${player.displayName}.")
        chatPlayer(confused, "How do you know my name?")
        chatNpc(neutral, "You and I share a common friend.")
        chatPlayer(quiz, "Azzanadra?")
        chatNpc(neutral, "Indeed.")
        val option =
            choice3("Why are you here?", 1, "I need another ring.", 2, "I'd best get going.", 3)
        when (option) {
            1 -> {
                chatPlayer(quiz, "Why are you here?")
                chatNpc(
                    shifty,
                    "Ever heard of criminals returning to the scene of the crime? Thats why.",
                )
                chatPlayer(neutral, "I'm not sure this one is going to return any time soon.")
                chatNpc(
                    neutral,
                    "Well never say never, ${player.displayName}. Now be on your way.",
                )
            }
            2 -> {
                chatPlayer(neutral, "I need another ring.")
                chatNpc(shifty, "Very well, but it will cost you...")
            }
            3 -> {
                chatPlayer(neutral, "I'd best get going.")
                chatNpc(neutral, "Until next time, ${player.displayName}.")
            }
        }
    }
}
