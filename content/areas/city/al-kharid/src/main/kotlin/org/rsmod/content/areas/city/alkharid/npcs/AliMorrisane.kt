package org.rsmod.content.areas.city.alkharid.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Ali Morrisane — north Al-Kharid trader (M5.ALKHARID.1).
 *
 * Ali Morrisane's canonical role is the Rogue Trader miniquest (members-only Kharidian Desert
 * content) where he sets the player up with the Pollnivneach black-market trade. The F2P-portion
 * dialogue we ship matches the pre-miniquest greeting on the OSRS wiki: Ali introduces himself and
 * gestures at his uncle's "Discount Wares" stall, directing the player to come back when they're
 * ready to do business in the desert.
 *
 * The rev240 cache still exposes no dedicated `ali_morrisane` NPC symbol, so this dialogue binds
 * against the Al-Kharid populace symbol `npc.al_kharid_man` directly. The pre-sync build routed the
 * same binding through a Silo-minted `content.alkharid_ali_morrisane` group applied to that very
 * NPC type; since `contentGroup` is a type-level attribute, the indirection was never per-spawn and
 * bought nothing. The inherit-self `npc.al_kharid_man` overlay in
 * `.data/raw-cache/server/npcs.toml` supplies the `Talk-to` op slot the generic populace symbol
 * ships without.
 */
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
