package org.rsmod.content.areas.city.alkharid.npcs

import org.rsmod.api.invtx.invTakeFee
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Karim — Al-Kharid kebab seller on the market square (M5.ALKHARID.2).
 *
 * Per OSRS canon (Transcript:Karim) Karim has no shop interface — kebabs are sold exclusively
 * through a Talk-to dialogue at one coin each. Only the standard branch ships here: the Rogue
 * Trader miniquest and clue-scroll branches are members-flavoured content out of scope for the F2P
 * portion.
 */
class Karim : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(NPC) { startDialogue(it.npc) }
    }

    private suspend fun ProtectedAccess.startDialogue(npc: Npc) {
        startDialogue(npc) { karimDialogue() }
    }

    private suspend fun Dialogue.karimDialogue() {
        chatNpc(happy, "Would you like to buy a nice kebab? Only one gold.")
        val choice = choice2("Yes please.", OPT_YES, "I think I'll give it a miss.", OPT_MISS)
        when (choice) {
            OPT_YES -> {
                if (!access.player.invTakeFee(fee = KEBAB_PRICE)) {
                    chatPlayer(sad, "Oops, I forgot to bring any money with me.")
                    chatNpc(neutral, "Come back when you have some.")
                } else {
                    access.invAdd(access.inv, KEBAB)
                }
            }
            OPT_MISS -> chatPlayer(neutral, "I think I'll give it a miss.")
        }
    }

    public companion object {
        public const val NPC: String = "npc.kebab_seller"

        /** OSRS canon: Karim sells kebabs for one coin (Karim wiki page). */
        public const val KEBAB_PRICE: Int = 1

        private const val KEBAB: String = "obj.kebab"

        private const val OPT_YES: Int = 1
        private const val OPT_MISS: Int = 2
    }
}
