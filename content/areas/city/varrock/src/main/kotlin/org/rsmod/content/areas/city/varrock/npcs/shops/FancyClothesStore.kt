package org.rsmod.content.areas.city.varrock.npcs.shops

import dev.openrune.definition.type.widget.IfEvent
import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class FancyClothesStore @Inject constructor(private val shops: Shops) : PluginScript() {

    override fun ScriptContext.startup() {
        onOpNpc1("npc.tailorp") { shopDialogue(it.npc) }
        onOpNpc3("npc.tailorp") { player.openFancyClothesStore(it.npc) }
        onOpNpc4("npc.tailorp") { openFurClothing() }
    }

    private fun ProtectedAccess.openFurClothing() {
        ifOpenMainModal("interface.hunting_customfurs")
        ifSetEvents("component.hunting_customfurs:choices_items", 0..19, IfEvent.Op1, IfEvent.Op2, IfEvent.Op3, IfEvent.Op4, IfEvent.Op5)
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

        val choice = choice4(
            "Okay, let's see what you've got then.", 1,
            "Can you make clothing suitable for hunting in?", 3,
            "What sort of unusual materials did you have in mind?", 4,
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
            3 -> {
                chatPlayer(quiz, "Can you make clothing suitable for hunting?")
                chatNpc(
                    happy,
                    "Certainly. Take a look at my range of made-to-order items. If you can supply the furs, I'll " +
                        "gladly make any of these for you.",
                )
                access.openFurClothing()
            }
            4 -> {
                chatPlayer(quiz, "What sort of unusual materials did you have in mind?")
                chatNpc(
                    neutral,
                    "Well, some more colourful feathers might be useful. For some surreal reason, all I normally seem " +
                        "to get offered are large quantities of rather beaten-up looking chicken feathers.",
                )
                chatNpc(
                    neutral,
                    "People must have some very strange pastimes around these parts, that's all I can say.",
                )
                chatPlayer(happy, "Ok, let's see what you've got then.")
                player.openFancyClothesStore(npc)
            }
        }
    }
}
