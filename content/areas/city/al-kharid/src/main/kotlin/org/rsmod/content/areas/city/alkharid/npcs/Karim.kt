package org.rsmod.content.areas.city.alkharid.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** The kebab seller by the Al Kharid gate. */
class Karim : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.kebab_seller") { startDialogue(it.npc) }
    }

    private suspend fun ProtectedAccess.startDialogue(npc: Npc) {
        startDialogue(npc) { sellKebab() }
    }

    private suspend fun Dialogue.sellKebab() {
        chatNpc(quiz, "Would you like to buy a nice kebab? Only one gold.")
        if (!choice2("I think I'll give it a miss.", false, "Yes please.", true)) {
            chatPlayer(neutral, "I think I'll give it a miss.")
            return
        }
        chatPlayer(neutral, "Yes please.")
        if (access.invTotal(access.inv, COINS) < PRICE) {
            chatPlayer(sad, "Oops, I forgot to bring any money with me.")
            chatNpc(neutral, "Come back when you have some.")
            return
        }
        access.invDel(access.inv, COINS, PRICE)
        access.invAdd(access.inv, KEBAB)
        objbox(KEBAB, "You buy a kebab for $PRICE gold.")
    }

    private companion object {
        private const val COINS = "obj.coins"
        private const val KEBAB = "obj.kebab"
        private const val PRICE = 1
    }
}
