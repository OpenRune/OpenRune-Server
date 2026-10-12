package org.rsmod.content.areas.city.alkharid.npcs.shops

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

// https://oldschool.runescape.wiki/w/Transcript:Louie_Legs
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class LegsShop @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.louie_legs") { shopDialogue(it.npc) }
        onOpNpc3("npc.louie_legs") { player.openLegsShop(it.npc) }
    }

    private fun Player.openLegsShop(npc: Npc) {
        shops.open(this, npc, "Louie's Armoured Legs Bazaar.", "inv.legsshop")
    }

    private suspend fun ProtectedAccess.shopDialogue(npc: Npc) =
        startDialogue(npc) { shopKeeper(npc) }

    private suspend fun Dialogue.shopKeeper(npc: Npc) {
        chatNpc(happy, "Hey, wanna buy some armour?")
        when (choice2("What have you got?", 1, "No, thank you.", 2)) {
            1 -> {
                chatPlayer(quiz, "What have you got?")
                chatNpc(happy, "I provide items to help you keep your legs!")
                player.openLegsShop(npc)
            }
            2 -> chatPlayer(neutral, "No, thank you.")
        }
    }
}
