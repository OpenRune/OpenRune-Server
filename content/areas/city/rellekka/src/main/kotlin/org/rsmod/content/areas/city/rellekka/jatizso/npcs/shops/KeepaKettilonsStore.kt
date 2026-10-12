package org.rsmod.content.areas.city.rellekka.jatizso.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Keepa_Kettilon (after The Fremennik Isles)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:the_fremennik_isles] in-quest dialogue that ends by opening the store,
// window and beard taxes
class KeepaKettilonsStore @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.frisd_cook") { startDialogue(it.npc) { keepaKettilon() } }
        onOpNpc3("npc.frisd_cook") { player.openKeepasStore(it.npc) }
    }

    private fun Player.openKeepasStore(npc: Npc) {
        shops.open(this, npc, "Keepa Kettilon's store", "inv.frisd_cook")
    }

    private suspend fun Dialogue.keepaKettilon() {
        chatNpc(sad, "Go away and let me wallow in self loathing.")
        chatPlayer(laugh, "Do you want any help with that? I can loath you as well.")
        chatNpc(sad, "*sigh*")
    }
}
