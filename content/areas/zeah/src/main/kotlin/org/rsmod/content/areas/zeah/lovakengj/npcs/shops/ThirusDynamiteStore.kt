package org.rsmod.content.areas.zeah.lovakengj.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Thirus (favour removed, so always served)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [diary:kourend_and_kebos] Claim op for the daily free dynamite
class ThirusDynamiteStore @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(THIRUS) { startDialogue(it.npc) { thirus(it.npc) } }
        onOpNpc3(THIRUS) { player.openShop(it.npc) }
    }

    private fun Player.openShop(npc: Npc) {
        shops.open(this, npc, "Thirus Urkar's Fine Dynamite Store", "inv.lovakengj_dynamite_shop")
    }

    private suspend fun Dialogue.thirus(npc: Npc) {
        chatNpc(happy, "If you're looking for dynamite, you've come to the right place, human.")
        while (true) {
            val option =
                choice3(
                    "Who are you?",
                    1,
                    "I'd like to buy some dynamite.",
                    2,
                    "No, I'm fine.",
                    3,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Who are you?")
                    chatNpc(
                        neutral,
                        "The name's Thirus Urkar. The Urkars have supplied the blast mines with " +
                            "the finest dynamite since they opened many ages ago.",
                    )
                    chatNpc(quiz, "Now, can I help you today?")
                }
                2 -> {
                    chatPlayer(happy, "I'd like to buy some dynamite.")
                    player.openShop(npc)
                    return
                }
                else -> {
                    chatPlayer(neutral, "No, I'm fine.")
                    return
                }
            }
        }
    }

    private companion object {
        const val THIRUS = "npc.lovakengj_thirus"
    }
}
