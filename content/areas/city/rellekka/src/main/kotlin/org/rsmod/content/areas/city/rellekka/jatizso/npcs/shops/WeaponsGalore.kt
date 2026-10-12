package org.rsmod.content.areas.city.rellekka.jatizso.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Skuli_Myrka (after The Fremennik Isles)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:the_fremennik_isles] in-quest dialogue (window and beard taxes)
class WeaponsGalore @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.frisd_weaponmerchant") { shopDialogue(it.npc) }
        onOpNpc3("npc.frisd_weaponmerchant") { player.openShop(it.npc) }
    }

    private fun Player.openShop(npc: Npc) {
        shops.open(this, npc, "Weapons galore", "inv.frisd_weaponshop")
    }

    private suspend fun ProtectedAccess.shopDialogue(npc: Npc) =
        startDialogue(npc) { shopKeeper(npc) }

    private suspend fun Dialogue.shopKeeper(npc: Npc) {
        chatNpc(happy, "Oh, hello again. Want to buy some weapons?")
        when (choice2("I'll have a look.", 1, "Not right now...", 2)) {
            1 -> {
                chatPlayer(happy, "I'll have a look.")
                player.openShop(npc)
            }
            2 -> chatPlayer(neutral, "Not right now.")
        }
    }
}
