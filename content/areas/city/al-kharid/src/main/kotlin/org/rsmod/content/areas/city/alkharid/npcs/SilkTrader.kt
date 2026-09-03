package org.rsmod.content.areas.city.alkharid.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Silk Trader — Al-Kharid silk salesman (south-east Al-Kharid market square). M5.ALKHARID.2.
 *
 * Per OSRS canon (Silk trader wiki page) the Silk Trader has no shop interface — silk is sold
 * exclusively through a Talk-to-only haggle dialogue. The full haggle mechanic (3gp default, 2gp on
 * convincing him to drop the price) is members-flavour content that ties into the Ardougne
 * silk-running money-making loop; the F2P-portion dialogue we ship is the canonical greeting +
 * flavour about the silk trade, matching what an F2P player would experience.
 */
class SilkTrader : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(SILK_TRADER) { startDialogue(it.npc) }
    }

    private suspend fun ProtectedAccess.startDialogue(npc: Npc) {
        startDialogue(npc) { silkTraderDialogue() }
    }

    private suspend fun Dialogue.silkTraderDialogue() {
        chatNpc(happy, "Greetings, would you be interested in buying some silk?")
        val choice =
            choice3(
                "How much are you selling it for?",
                OPT_PRICE,
                "What's silk used for?",
                OPT_USE,
                "No thanks.",
                OPT_NO,
            )
        when (choice) {
            OPT_PRICE -> {
                chatPlayer(quiz, "How much are you selling it for?")
                chatNpc(neutral, "I'll let you have a roll of silk for 3 gold coins.")
            }
            OPT_USE -> {
                chatPlayer(quiz, "What's silk used for?")
                chatNpc(
                    happy,
                    "Silk is a fine fabric. The clothiers and tailors of the world know how " +
                        "to weave wonderful garments from it.",
                )
            }
            OPT_NO -> chatPlayer(neutral, "No thanks.")
        }
    }

    public companion object {
        public const val SILK_TRADER: String = "npc.silk_trader"

        private const val OPT_PRICE: Int = 1
        private const val OPT_USE: Int = 2
        private const val OPT_NO: Int = 3
    }
}
