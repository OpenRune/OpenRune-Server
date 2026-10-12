package org.rsmod.content.areas.city.ardougne.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Chadwell
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:song_of_the_elves] new taxes dialogue
class WestArdougneGeneralStore @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.chadwell") { startDialogue(it.npc) { chadwell(it.npc) } }
        onOpNpc3("npc.chadwell") { player.openGeneralStore(it.npc) }
    }

    private fun Player.openGeneralStore(npc: Npc) {
        shops.open(this, npc, "West Ardougne General Store", "inv.upassgeneralshop")
    }

    private suspend fun Dialogue.chadwell(npc: Npc) {
        chatNpc(happy, "Good day. What can I get you?")
        if (choice2("Let's see what you've got.", true, "Nothing thanks.", false)) {
            chatPlayer(happy, "Let's see what you've got.")
            player.openGeneralStore(npc)
            return
        }
        chatPlayer(neutral, "Nothing thanks.")
        chatNpc(neutral, "Okay then.")
    }
}
