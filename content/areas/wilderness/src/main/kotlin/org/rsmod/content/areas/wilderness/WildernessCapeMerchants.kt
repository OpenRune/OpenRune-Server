package org.rsmod.content.areas.wilderness

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

class WildernessCapeMerchants @Inject constructor(private val shops: Shops) : PluginScript() {

    override fun ScriptContext.startup() {
        for (i in 1..10) {
            onOpNpc1("npc.wilderness_capeseller_$i") { merchantDialogue(it.npc) }
            onOpNpc3("npc.wilderness_capeseller_$i") { shops.open(player, it.npc) }
        }
    }

    private suspend fun ProtectedAccess.merchantDialogue(npc: Npc, openingDialogue: Boolean = true) =
        startDialogue(npc) {
            if (openingDialogue) {
                chatNpc(happy, "Hello there, are you interested in buying one of my special capes?")
            }
            shopKeeper(npc)
        }


    private suspend fun Dialogue.shopKeeper(npc: Npc) {
        val choice = choice3(
            "What's so special about your capes?", 1,
            "Yes please!", 2,
            "No thanks.", 3,
        )

        when (choice) {
            1 -> whatSoSpecial(npc)
            2 -> {
                chatPlayer(happy, "Yes please!")
                shops.open(player, npc)
            }
            3 -> chatPlayer(happy, "No thanks.")
        }
    }

    private suspend fun Dialogue.whatSoSpecial(npc: Npc) {
        chatPlayer(happy, "What's so special about your capes?")
        chatNpc(
            happy,
            "Ahh well they make it less likely that you'll accidently attack anyone wearing" +
                " the same cape as you and easier to attack everyone else. " +
                "They also make it easier to distinguish people who're wearing the same cape as you",
        )
        chatNpc(
            happy,
            "from everyone else. They're very useful when out in the wilderness with " +
                "friends or anyone else you don't want to harm.",
        )
        chatNpc(happy, "So would you like to buy one?")
        access.merchantDialogue(npc, false)
    }
}
