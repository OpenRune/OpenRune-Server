package org.rsmod.content.areas.city.prifddinas.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc4
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Sawmill_Operator_(Prifddinas)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [skill:construction] plank-making interface (Buy-plank op and "Yes, please make
// me some planks.")
class ConstructionSupplies @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.prif_sawmill_operator") { startDialogue(it.npc) { sawmillOperator(it.npc) } }
        onOpNpc4("npc.prif_sawmill_operator") { player.openConstructionSupplies(it.npc) }
    }

    private fun Player.openConstructionSupplies(npc: Npc) {
        shops.open(this, npc, "Construction supplies", "inv.poh_sawmill_shop")
    }

    private suspend fun Dialogue.sawmillOperator(npc: Npc) {
        chatNpc(
            happy,
            "Do you want me to make some planks for you? I can make planks from wood, oak, teak " +
                "and mahogany logs. Or would you like to buy some other housing supplies?",
        )
        val option =
            choice4(
                "Yes, please make me some planks.",
                1,
                "What kind of planks can you make?",
                2,
                "Can I buy some housing supplies?",
                3,
                "Nothing, thanks",
                4,
            )
        when (option) {
            1 -> chatPlayer(happy, "Yes, please make me some planks.")
            2 -> {
                chatNpc(
                    neutral,
                    "I can make planks from wood, oak, teak and mahogany. I don't make planks " +
                        "from other woods as they're no good for making furniture.",
                )
                chatNpc(
                    neutral,
                    "Wood and oak are all over the place, but teak and mahogany can only be " +
                        "found in a few places like Karamja and Etceteria.",
                )
            }
            3 -> player.openConstructionSupplies(npc)
            4 -> {
                chatPlayer(neutral, "Nothing, thanks.")
                chatNpc(
                    neutral,
                    "Well come back when you want some. You can't get good quality planks " +
                        "anywhere but here!",
                )
            }
        }
    }
}
