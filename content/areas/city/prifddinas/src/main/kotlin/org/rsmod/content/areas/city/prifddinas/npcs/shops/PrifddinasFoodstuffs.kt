package org.rsmod.content.areas.city.prifddinas.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Derwen_(elf)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class PrifddinasFoodstuffs @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.prif_foodstore") { startDialogue(it.npc) { derwen(it.npc) } }
        onOpNpc3("npc.prif_foodstore") { player.openFoodstuffs(it.npc) }
    }

    private fun Player.openFoodstuffs(npc: Npc) {
        shops.open(this, npc, "Prifddinas Foodstuffs", "inv.prif_food_store")
    }

    private suspend fun Dialogue.derwen(npc: Npc) {
        chatNpc(
            happy,
            "You're a long way from home, traveller! How about some food to keep you going?",
        )
        when (choice2("Yes, please.", 1, "No thanks.", 2)) {
            1 -> {
                chatPlayer(happy, "Yes, please.")
                player.openFoodstuffs(npc)
            }
            2 -> {
                chatPlayer(neutral, "No thanks.")
                chatNpc(happy, "I'll be here if you find yourself hungry!")
            }
        }
    }
}
