package org.rsmod.content.areas.city.varrock.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc4
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Sawmill_operator
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [skill:construction] plank-making interface (Buy-plank op and "Yes, please make
// me some planks.")
class ConstructionSupplies @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.poh_sawmill_opp") { shopDialogue(it.npc) }
        onOpNpc4("npc.poh_sawmill_opp") { player.openConstructionSupplies(it.npc) }
    }

    private fun Player.openConstructionSupplies(npc: Npc) {
        shops.open(this, npc, "Construction supplies", "inv.poh_sawmill_shop")
    }

    private suspend fun ProtectedAccess.shopDialogue(npc: Npc) =
        startDialogue(npc) { sawmillOperator(npc) }

    private suspend fun Dialogue.sawmillOperator(npc: Npc) {
        chatNpc(
            happy,
            "Hello there. Do you want me to make some planks for you? I can make planks from " +
                "wood, oak, teak and mahogany logs. Or would you like to buy some other housing " +
                "supplies?",
        )
        val option =
            choice3(
                "Yes, please make me some planks.",
                1,
                "Can I buy some housing supplies?",
                2,
                "I'm good, thanks.",
                3,
            )
        when (option) {
            1 -> chatPlayer(happy, "Yes, please make me some planks.")
            2 -> {
                chatPlayer(quiz, "Can I buy some housing supplies?")
                chatNpc(happy, "Of course!")
                player.openConstructionSupplies(npc)
            }
            3 -> {
                chatPlayer(neutral, "I'm good, thanks.")
                chatNpc(
                    neutral,
                    "Well come back when you want some. You'll struggle to find quality planks " +
                        "anywhere but here!",
                )
            }
        }
    }
}
