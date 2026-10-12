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

// https://oldschool.runescape.wiki/w/Transcript:Aemad
// https://oldschool.runescape.wiki/w/Transcript:Kortan
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:song_of_the_elves] new taxes dialogue
class AdventuringSupplies @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.aemad") {
            startDialogue(it.npc) { shopKeeper(it.npc, "No, I've come to the wrong place.") }
        }
        onOpNpc1("npc.kortan") {
            startDialogue(it.npc) { shopKeeper(it.npc, "No, sorry, I've come to the wrong place.") }
        }
        onOpNpc3("npc.aemad") { player.openAdventuringSupplies(it.npc) }
        onOpNpc3("npc.kortan") { player.openAdventuringSupplies(it.npc) }
    }

    private fun Player.openAdventuringSupplies(npc: Npc) {
        shops.open(this, npc, "Aemad's Adventuring Supplies.", "inv.adventurershop")
    }

    private suspend fun Dialogue.shopKeeper(npc: Npc, declineOption: String) {
        chatNpc(
            happy,
            "Hello there. You've come to the right place if you're looking for adventurer's " +
                "equipment.",
        )
        val interested =
            choice2(
                "Oh that sounds interesting.",
                true,
                declineOption,
                false,
            )
        if (interested) {
            chatPlayer(happy, "Oh that sounds interesting.")
            player.openAdventuringSupplies(npc)
            return
        }
        chatPlayer(neutral, "No, sorry, I've come to the wrong place.")
        chatNpc(bored, "Hmph. Well, perhaps next time you'll need something from me?")
    }
}
