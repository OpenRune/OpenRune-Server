package org.rsmod.content.areas.city.catherby.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.stat.baseFletchingLvl
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Hickton
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:skillcapes] cape-buying dialogue and the trimmed cape stock
// TODO(content): [system:treasure_trails] medium challenge scroll
class ArcheryShop @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.hickton") { startDialogue(it.npc) { hickton(it.npc) } }
        onOpNpc3("npc.hickton") { player.openArcheryShop(it.npc) }
    }

    private fun Player.openArcheryShop(npc: Npc) {
        val inv = if (baseFletchingLvl >= MAX_LEVEL) SKILLCAPE_STOCK else STOCK
        shops.open(this, npc, "Hickton's Archery Emporium.", inv)
    }

    private suspend fun Dialogue.hickton(npc: Npc) {
        chatNpc(happy, "Welcome to Hickton's Archery Store. Do you want to see my wares?")
        val option =
            choice3(
                "Can you tell me about your cape?",
                1,
                "Yes, please.",
                2,
                "No, I prefer to bash things close up.",
                3,
            )
        when (option) {
            1 -> cape(npc)
            2 -> player.openArcheryShop(npc)
            3 -> chatPlayer(neutral, "No, I prefer to bash things close up.")
        }
    }

    private suspend fun Dialogue.cape(npc: Npc) {
        chatPlayer(quiz, "Can you tell me about your cape?")
        chatNpc(
            happy,
            "Certainly! Skillcapes are a symbol of achievement. Only people who have mastered a " +
                "skill and reached level 99 can get their hands on them and gain the benefits " +
                "they carry.",
        )
        chatNpc(
            happy,
            "The Cape of Fletching can be searched for a mithril grapple and crossbow three " +
                "times every day. Is there anything else I can help you with?",
        )
        if (choice2("I'd like to view your store please.", true, "No thank you.", false)) {
            player.openArcheryShop(npc)
        } else {
            chatPlayer(neutral, "No thank you.")
        }
    }

    private companion object {
        const val MAX_LEVEL = 99
        const val STOCK = "inv.archeryshop2"
        const val SKILLCAPE_STOCK = "inv.archeryshop2_skillcape"
    }
}
