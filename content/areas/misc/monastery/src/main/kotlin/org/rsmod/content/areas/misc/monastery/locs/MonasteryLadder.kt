package org.rsmod.content.areas.misc.monastery.locs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.basePrayerLvl
import org.rsmod.api.script.onOpLoc1
import org.rsmod.content.areas.misc.monastery.npcs.monasteryOrder
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** The ladder to the upper floor of Edgeville Monastery, open only to the order. */
class MonasteryLadder : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1("loc.monasteryladder") { climb() }
    }

    private suspend fun ProtectedAccess.climb() {
        arriveDelay()
        if (!player.monasteryOrder) {
            startDialogue { refuse() }
            return
        }
        anim("seq.human_reachforladder")
        delay(1)
        telejump(coords.translateLevel(1))
    }

    private suspend fun Dialogue.refuse() {
        chatNpcSpecific("Abbot Langley", ABBOT, neutral, "Only members of our order can go up there.")
        if (!choice2("Well can I join your order?", true, "Oh, sorry.", false)) {
            chatPlayer(neutral, "Oh, sorry.")
            return
        }
        chatPlayer(quiz, "Well can I join your order?")
        if (player.basePrayerLvl < ORDER_PRAYER_LEVEL) {
            chatNpcSpecific(
                "Abbot Langley",
                ABBOT,
                neutral,
                "No. I am sorry, but I feel you are not devout enough.",
            )
            access.mes("You need a prayer level of $ORDER_PRAYER_LEVEL to join the order.")
            return
        }
        chatNpcSpecific(
            "Abbot Langley",
            ABBOT,
            happy,
            "Ok, I see you are someone suitable for our order. You may join.",
        )
        player.monasteryOrder = true
    }

    private companion object {
        private const val ABBOT = "npc.abbot_langley"
        private const val ORDER_PRAYER_LEVEL = 31
    }
}
