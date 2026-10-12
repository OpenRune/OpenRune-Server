package org.rsmod.content.areas.misc.islands.entrana.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Frincos
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class HerbStore @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(FRINCOS) { startDialogue(it.npc) { frincos(it.npc) } }
        onOpNpc3(FRINCOS) { player.openHerbStore(it.npc) }
    }

    private fun Player.openHerbStore(npc: Npc) {
        shops.open(this, npc, "Frincos' Fabulous Herb Store.", "inv.herbloreshop2")
    }

    private suspend fun Dialogue.frincos(npc: Npc) {
        chatNpc(happy, "Hello, how can I help you?")
        val option =
            choice3(
                "What are you selling?",
                1,
                "You can't; I'm beyond help.",
                2,
                "I'm okay, thank you.",
                3,
            )
        when (option) {
            1 -> player.openHerbStore(npc)
            2 -> chatPlayer(sad, "You can't; I'm beyond help.")
            3 -> chatPlayer(happy, "I'm okay, thank you.")
        }
    }

    private companion object {
        const val FRINCOS = "npc.frincos"
    }
}
