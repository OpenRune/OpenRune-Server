package org.rsmod.content.areas.city.varrock.npcs.shops

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

// https://oldschool.runescape.wiki/w/Transcript:Tea_Seller
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [skill:thieving] overhead lines after stealing from the tea stall
class TeaShop @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.tea_seller") { shopDialogue(it.npc) }
        onOpNpc3("npc.tea_seller") { player.openTeaShop(it.npc) }
    }

    private fun Player.openTeaShop(npc: Npc) {
        shops.open(this, npc, "Ye olde Tea Shoppe.", "inv.teashop")
    }

    private suspend fun ProtectedAccess.shopDialogue(npc: Npc) =
        startDialogue(npc) { shopKeeper(npc) }

    private suspend fun Dialogue.shopKeeper(npc: Npc) {
        chatNpc(happy, "Greetings! Are you in need of refreshment?")
        when (choice3("Yes please.", 1, "What are you selling?", 2, "No thanks.", 3)) {
            1 -> {
                chatPlayer(happy, "Yes please.")
                player.openTeaShop(npc)
            }
            2 -> {
                chatPlayer(quiz, "What are you selling?")
                chatNpc(
                    happy,
                    "Only the most delicious infusion of the leaves of the tea plant. Grown in " +
                        "the exotic regions of this world. Buy yourself a cup !",
                )
            }
            3 -> {
                chatPlayer(neutral, "No thanks.")
                chatNpc(neutral, "Well, if you're sure. You know where to come if you do!")
            }
        }
    }
}
