package org.rsmod.content.areas.city.canifis.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Barker
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:treasure_trails] elite clue step reward
class BarkersHaberdashery @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(BARKER) { startDialogue(it.npc) { barker(it.npc) } }
        onOpNpc3(BARKER) { player.openShop(it.npc) }
    }

    private fun Player.openShop(npc: Npc) {
        shops.open(this, npc, "Barkers' Haberdashery", "inv.werewolfstore2")
    }

    private suspend fun Dialogue.barker(npc: Npc) {
        chatNpc(
            happy,
            "You are looking for clothes, yes? You look at my products! I have very many nice " +
                "clothes, yes?",
        )
        when (choice2("Yes, please.", 1, "No thanks.", 2)) {
            1 -> {
                chatPlayer(happy, "Yes, please.")
                player.openShop(npc)
            }
            2 -> {
                chatPlayer(neutral, "No thanks.")
                chatNpc(sad, "Unfortunate for you, yes? Many bargains, won't find elsewhere!")
            }
        }
    }

    private companion object {
        const val BARKER = "npc.werewolfshopkeeper2"
    }
}
