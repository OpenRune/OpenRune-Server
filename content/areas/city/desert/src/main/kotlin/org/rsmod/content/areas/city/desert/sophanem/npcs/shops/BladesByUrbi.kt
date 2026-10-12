package org.rsmod.content.areas.city.desert.sophanem.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Urbi
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class BladesByUrbi @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(URBI) { startDialogue(it.npc) { urbi(it.npc) } }
        onOpNpc3(URBI) { player.openStore(it.npc) }
    }

    private fun Player.openStore(npc: Npc) {
        shops.open(this, npc, "Blades by Urbi.", "inv.contact_daggershop")
    }

    private suspend fun Dialogue.urbi(npc: Npc) {
        chatNpc(
            happy,
            "Hello there; are you interested in any of my fine daggers? Ideal for slipping under " +
                "desert robes to give your enemies a nasty surprise...",
        )
        val option =
            choice3(
                "I'm looking for daggers, yes.",
                1,
                "I'd rather help you in return for better prices.",
                2,
                "No thanks.",
                3,
            )
        when (option) {
            1 -> player.openStore(npc)
            2 -> {
                chatPlayer(quiz, "I'd rather help you in return for better prices.")
                chatNpc(
                    neutral,
                    "I think it will take months for me to return to normal business. If you can " +
                        "wait that long, however, I may have tasks for you.",
                )
            }
            3 -> chatPlayer(neutral, "No thanks.")
        }
    }

    private companion object {
        const val URBI = "npc.contact_market_dagger"
    }
}
