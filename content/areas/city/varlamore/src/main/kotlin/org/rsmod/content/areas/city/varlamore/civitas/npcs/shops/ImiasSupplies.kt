package org.rsmod.content.areas.city.varlamore.civitas.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.content.areas.city.varlamore.civitas.npcs.hasGuildHunterLevel
import org.rsmod.content.areas.city.varlamore.civitas.npcs.refuseUnqualifiedHunter
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Imia
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class ImiasSupplies @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(IMIA) { startDialogue(it.npc) { imia(it.npc) } }
        onOpNpc3(IMIA) { trade(it.npc) }
    }

    private suspend fun ProtectedAccess.trade(npc: Npc) {
        if (!hasGuildHunterLevel()) {
            return startDialogue(npc) { refuseUnqualifiedHunter() }
        }
        openSupplies(npc)
    }

    private fun ProtectedAccess.openSupplies(npc: Npc) {
        shops.open(player, npc, "Imia's Supplies", "inv.hunting_shop_guild")
    }

    private suspend fun Dialogue.imia(npc: Npc) {
        if (!hasGuildHunterLevel()) {
            return refuseUnqualifiedHunter()
        }
        chatNpc(happy, "Hi there. How's it going?")
        while (true) {
            val option =
                choice3(
                    "What do you do here?",
                    1,
                    "What have you got for sale?",
                    2,
                    "Never mind.",
                    3,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "What do you do here?")
                    chatNpc(
                        happy,
                        "I keep the hunters well equipped! I can sell you hunting equipment at a " +
                            "reasonable price.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "What have you got for sale?")
                    chatNpc(happy, "Hunting supplies for every occasion.")
                    return access.openSupplies(npc)
                }
                else -> {
                    chatPlayer(neutral, "Never mind.")
                    return
                }
            }
        }
    }

    private companion object {
        const val IMIA = "npc.hg_mixedhide_seller"
    }
}
