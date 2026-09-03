package org.rsmod.content.areas.city.alkharid.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class AliMorrisane : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(ALI_MORRISANE) { startDialogue(it.npc) }
    }

    private suspend fun ProtectedAccess.startDialogue(npc: Npc) {
        startDialogue(npc) { aliDialogue() }
    }

    private suspend fun Dialogue.aliDialogue() {
        chatNpc(
            happy,
            "Aha! A potential customer! Welcome, welcome — I am Ali Morrisane, master trader of " +
                "the Kharidian Desert!",
        )
        val choice =
            choice3(
                "What do you sell?",
                OPT_SELL,
                "Where do your wares come from?",
                OPT_WARES,
                "Goodbye.",
                OPT_BYE,
            )
        when (choice) {
            OPT_SELL -> {
                chatPlayer(quiz, "What do you sell?")
                chatNpc(
                    happy,
                    "A little of this, a little of that — gems from the desert, silks from the " +
                        "south, fine wares from across Gielinor. Come back when you wish to do " +
                        "business in the desert and we shall talk further.",
                )
            }
            OPT_WARES -> {
                chatPlayer(quiz, "Where do your wares come from?")
                chatNpc(
                    happy,
                    "My many nephews work the trade routes all across the Kharidian Desert. From " +
                        "Pollnivneach to Nardah they bring me only the finest goods.",
                )
            }
            OPT_BYE -> chatPlayer(neutral, "Goodbye.")
        }
    }

    public companion object {
        public const val ALI_MORRISANE: String = "npc.al_kharid_man"

        private const val OPT_SELL: Int = 1
        private const val OPT_WARES: Int = 2
        private const val OPT_BYE: Int = 3
    }
}
