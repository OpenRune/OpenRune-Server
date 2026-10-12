package org.rsmod.content.areas.city.taverley.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Jatix
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:treasure_trails] easy clue reward
class HerbloreShop @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.jatix") { startDialogue(it.npc) { jatix(it.npc) } }
        onOpNpc3("npc.jatix") { player.openHerbloreShop(it.npc) }
    }

    private fun Player.openHerbloreShop(npc: Npc) {
        shops.open(this, npc, "Jatix's Herblore Shop.", "inv.herbloreshop")
    }

    private suspend fun Dialogue.jatix(npc: Npc) {
        chatNpc(happy, "Hello, how can I help you?")
        val option =
            choice3(
                "What are you selling?",
                1,
                "You can't; I'm beyond help.",
                2,
                "I'm okay, thank you.",
                3,
            )
        when (option) {
            1 -> player.openHerbloreShop(npc)
            2 -> chatPlayer(sad, "You can't; I'm beyond help.")
            3 -> chatPlayer(neutral, "I'm okay, thank you.")
        }
    }
}
