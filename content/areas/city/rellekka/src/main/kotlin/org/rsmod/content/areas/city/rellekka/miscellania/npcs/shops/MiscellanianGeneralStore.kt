package org.rsmod.content.areas.city.rellekka.miscellania.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Finn
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:royal_trouble] monster rumours while the quest is in progress
class MiscellanianGeneralStore @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(FINN) { startDialogue(it.npc) { finn(it.npc) } }
        onOpNpc3(FINN) { player.openGeneralStore(it.npc) }
    }

    private fun Player.openGeneralStore(npc: Npc) {
        shops.open(this, npc, "Miscellanian General Store", "inv.royal_generalstore")
    }

    private suspend fun Dialogue.finn(npc: Npc) {
        chatNpc(happy, "Can I help you, Your Royal Highness?")
        val option =
            choice3(
                "Yes please. What are you selling?",
                1,
                "No thanks.",
                2,
                "What's it like living down here?",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(happy, "Yes please. What are you selling?")
                player.openGeneralStore(npc)
            }
            2 -> chatPlayer(neutral, "No thanks.")
            3 -> {
                chatPlayer(quiz, "What's it like living down here?")
                chatNpc(neutral, "A lot drier in the winter than it is above ground.")
            }
        }
    }

    private companion object {
        const val FINN = "npc.royal_generalstore_owner"
    }
}
