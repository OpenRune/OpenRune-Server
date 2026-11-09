package org.rsmod.content.areas.city.alkharid.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Avan Fitzharmon, at the north end of the Al Kharid mine. */
class Avan : PluginScript() {
    override fun ScriptContext.startup() {
        for (id in AVAN) {
            onOpNpc1(id) { startDialogue(it.npc) }
        }
    }

    private suspend fun ProtectedAccess.startDialogue(npc: Npc) {
        val helped = QuestRequirements.hasCompleted(player, FAMILY_CREST)
        startDialogue(npc) { if (helped) afterFamilyCrest() else busy() }
    }

    private suspend fun Dialogue.busy() {
        chatNpc(angry, "What? Can't you see I'm busy?")
        chatPlayer(neutral, "Well, sooooorry...")
    }

    private suspend fun Dialogue.afterFamilyCrest() {
        chatNpc(
            happy,
            "I received word from my father of your assistance to our family. You have my " +
                "thanks for restoring our honour.",
        )
        chatPlayer(neutral, "Not a problem.")
        chatNpc(neutral, "Yes well... Is there anything I can help you with?")
        if (choice2("Can you change my gauntlets for me?", true, "No thanks.", false)) {
            changeGauntlets()
        } else {
            chatPlayer(neutral, "No thanks.")
        }
    }

    private suspend fun Dialogue.changeGauntlets() {
        chatPlayer(quiz, "Can you change my gauntlets for me?")
        val held = GAUNTLETS.firstOrNull { access.invTotal(access.inv, it) > 0 }
        if (held == null) {
            chatNpc(neutral, "You don't have any gauntlets I can imbue.")
            return
        }
        chatNpc(neutral, "Yes certainly, though it will cost you $PRICE coins.")
        if (!choice2("Great thanks!", true, "No that's okay thanks.", false)) {
            chatPlayer(neutral, "No that's okay thanks.")
            return
        }
        chatPlayer(happy, "Great thanks!")
        if (access.invTotal(access.inv, COINS) < PRICE) {
            chatPlayer(sad, "Oh dear. I don't have enough money.")
            return
        }
        val next = GAUNTLETS[(GAUNTLETS.indexOf(held) + 1) % GAUNTLETS.size]
        if (!access.swapForCoins(take = held, give = next, price = PRICE)) {
            return
        }
        mesbox(
            "Avan takes your gauntlets, takes out a small hammer, and pounds them into a " +
                "slightly new shape, then hands them back to you.",
        )
    }

    private companion object {
        private const val FAMILY_CREST = "quest_familycrest"
        private const val COINS = "obj.coins"
        private const val PRICE = 25000

        private val AVAN = listOf("npc.avan", "npc.avan_fitzharmon_avan_2ops")

        private val GAUNTLETS =
            listOf(
                "obj.gauntlets_of_cooking",
                "obj.gauntlets_of_goldsmithing",
                "obj.gauntlets_of_chaos",
            )
    }
}
