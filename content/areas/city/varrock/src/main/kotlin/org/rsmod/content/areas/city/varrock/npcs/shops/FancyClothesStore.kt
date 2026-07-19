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

class FancyClothesStore @Inject constructor(private val shops: Shops) : PluginScript() {

    override fun ScriptContext.startup() {
        onOpNpc1("npc.tailorp") { shopDialogue(it.npc) }
        onOpNpc3("npc.tailorp") { player.openFancyClothesStore(it.npc) }
    }

    private fun Player.openFancyClothesStore(npc: Npc) {
        shops.open(this, npc, "Fancy Clothes Store", "inv.fancyclothesstore")
    }

    private suspend fun ProtectedAccess.shopDialogue(npc: Npc) =
        startDialogue(npc) { shopKeeper(npc) }

    private suspend fun Dialogue.shopKeeper(npc: Npc) {
        chatNpc(happy, "Now you look like someone who goes to a lot of fancy dress parties.")
        chatPlayer(happy, "Errr...what are you saying exactly?")
        chatNpc(
            happy,
            "I'm just saying that perhaps you would like to peruse my selection of garments.",
        )
        chatNpc(
            happy,
            "Or, if that doesn't interest you, then maybe you have something else to offer? " +
                "I'm always on the look out for interesting or unusual new materials.",
        )


        val choice = choice2(
            "Okay, let's see what you've got then.", 1,
            "I think I might just leave the perusing for now thanks.", 2,
        )

        when (choice) {
            1 -> {
                chatPlayer(happy, "Okay, let's see what you've got then.")
                player.openFancyClothesStore(npc)
            }
            2 -> {
                chatPlayer(neutral, "I think I might just leave the perusing for now thanks.")
            }
        }
    }
}
