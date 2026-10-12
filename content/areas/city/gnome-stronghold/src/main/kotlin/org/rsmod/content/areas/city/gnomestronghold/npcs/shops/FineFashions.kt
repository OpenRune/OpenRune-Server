package org.rsmod.content.areas.city.gnomestronghold.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Rometti
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:gnome_restaurant] delivery hand-in dialogue
class FineFashions @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(ROMETTI) { startDialogue(it.npc) { rometti(it.npc) } }
        onOpNpc3(ROMETTI) { player.openShop(it.npc) }
    }

    private fun Player.openShop(npc: Npc) {
        shops.open(this, npc, "Fine Fashions", "inv.gnomeshop_rometti")
    }

    private suspend fun Dialogue.rometti(npc: Npc) {
        chatPlayer(happy, "Hello.")
        chatNpc(
            happy,
            "Hello traveller. Have a look at my latest range of gnome fashion. Rometti is the " +
                "ultimate label in gnome high society.",
        )
        chatPlayer(confused, "Really.")
        chatNpc(happy, "Pastels are all the rage this season.")
        if (choice2("I've no time for fashion.", false, "OK then, let's have a look.", true)) {
            chatPlayer(happy, "OK then, let's have a look.")
            player.openShop(npc)
            return
        }
        chatPlayer(neutral, "I've no time for fashion.")
        chatNpc(neutral, "Hmm... I did wonder.")
    }

    private companion object {
        const val ROMETTI = "npc.gnome_rometti"
    }
}
