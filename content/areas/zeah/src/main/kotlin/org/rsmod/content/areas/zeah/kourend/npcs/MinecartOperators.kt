package org.rsmod.content.areas.zeah.kourend.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Elnes (the same for every operator)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:lovakengj_minecart] the network interface, Travel op and fee refund
// TODO(content): [quest:the_forsaken_tower] free rides after the quest
class MinecartOperators : PluginScript() {
    override fun ScriptContext.startup() {
        for (type in OPERATORS) {
            onOpNpc1(type) { startDialogue(it.npc) { operator() } }
        }
    }

    private suspend fun Dialogue.operator() {
        chatNpc(neutral, "Can I help you today?")
        val ride =
            choice2(
                "Can I go for a ride?",
                true,
                "I'm fine, thanks.",
                false,
            )
        if (!ride) {
            chatPlayer(neutral, "I'm fine, thanks.")
            return
        }
        chatPlayer(quiz, "Can I go for a ride?")
        if (player.inv.count(COINS) < FARE) {
            chatNpc(neutral, "Minecart rides are 20 coins.")
            return
        }
        chatNpc(happy, "Of course.")
    }

    private companion object {
        const val COINS = "obj.coins"
        const val FARE = 20
        val OPERATORS =
            listOf(
                "npc.lovakengj_minecart_lovakengj",
                "npc.lovakengj_minecart_hosidius",
                "npc.lovakengj_minecart_piscarilius",
                "npc.lovakengj_minecart_shayzien",
                "npc.lovakengj_minecart_arceuus",
                "npc.lovakengj_minecart_quidamortem",
                "npc.lovakengj_minecart_farming_guild",
                "npc.lovakengj_minecart_tithe_farm",
                "npc.lovakengj_minecart_kingstown",
                "npc.lovakengj_minecart_kourend_woodland",
                "npc.lovakengj_minecart_northern_tundras",
                "npc.lovakengj_minecart_shayzien_wall",
            )
    }
}
