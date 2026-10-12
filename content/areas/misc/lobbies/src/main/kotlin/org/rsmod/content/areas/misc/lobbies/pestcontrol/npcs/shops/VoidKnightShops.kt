package org.rsmod.content.areas.misc.lobbies.pestcontrol.npcs.shops

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

// https://oldschool.runescape.wiki/w/Transcript:Squire_(Void_Knights_general_store)
// https://oldschool.runescape.wiki/w/Transcript:Squire_(Void_Knights_magic_shop)
// https://oldschool.runescape.wiki/w/Transcript:Squire_(Void_Knights_ranged_shop)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class VoidKnightShops @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        bindShop(
            "npc.pest_squire_storemaster",
            "Void Knight General Store",
            "inv.pest_general_store",
        )
        bindShop("npc.pest_squire_runeshop", "Void Knight Magic Store", "inv.pest_rune_store")
        bindShop(
            "npc.pest_squire_rangeshop",
            "Void Knight Archery Store",
            "inv.pest_archery_store",
        )
    }

    private fun ScriptContext.bindShop(npc: String, title: String, inv: String) {
        onOpNpc1(npc) { shopDialogue(it.npc, title, inv) }
        onOpNpc3(npc) { player.openShop(it.npc, title, inv) }
    }

    private fun Player.openShop(npc: Npc, title: String, inv: String) {
        shops.open(this, npc, title, inv)
    }

    private suspend fun ProtectedAccess.shopDialogue(npc: Npc, title: String, inv: String) =
        startDialogue(npc) { squire(npc, title, inv) }

    private suspend fun Dialogue.squire(npc: Npc, title: String, inv: String) {
        chatNpc(happy, "Hi, how can I help you?")
        when (choice2("What do you have for sale?", 1, "I'm fine thanks.", 2)) {
            1 -> player.openShop(npc, title, inv)
            2 -> chatPlayer(neutral, "I'm fine thanks.")
        }
    }
}
