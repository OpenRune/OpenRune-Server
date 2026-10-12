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

// https://oldschool.runescape.wiki/w/Transcript:Gnome_Waiter
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:gnome_restaurant] "Actually I'd like to sell some dishes." only shows
//  when the player carries gnome food
class GiannesRestaurant @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(GNOME_WAITER) { startDialogue(it.npc) { gnomeWaiter(it.npc) } }
        onOpNpc3(GNOME_WAITER) { player.openShop(it.npc) }
    }

    private fun Player.openShop(npc: Npc) {
        shops.open(this, npc, "Gianne's Restaurant", "inv.giannerestaurant")
    }

    private suspend fun Dialogue.gnomeWaiter(npc: Npc) {
        val title = if (access.isBodyTypeA()) "sir" else "madam"
        chatPlayer(happy, "Hello.")
        chatNpc(
            happy,
            "Hello $title, can I tempt you with any of the dishes on our new menu?",
        )
        if (choice2("I'll take a look.", true, "Not really.", false)) {
            chatPlayer(happy, "I'll take a look.")
            chatNpc(
                happy,
                "I hope you like what you see. Although all these premade dishes are good to eat " +
                    "- they were made by the last human assistant chef. He wasn't up to Chef's " +
                    "exacting standards - and you may find that they will",
            )
            chatNpc(happy, "not be accepted by some people as the 'real' thing.")
            player.openShop(npc)
            return
        }
        chatPlayer(neutral, "Not really.")
        chatNpc(happy, "That's your choice $title, enjoy your stay.")
    }

    private companion object {
        const val GNOME_WAITER = "npc.gnome_waiter"
    }
}
