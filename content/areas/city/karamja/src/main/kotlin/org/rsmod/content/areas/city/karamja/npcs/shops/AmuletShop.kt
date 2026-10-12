package org.rsmod.content.areas.city.karamja.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Davon
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class AmuletShop @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.davon") { startDialogue(it.npc) { davon(it.npc) } }
        onOpNpc3("npc.davon") { player.openAmuletShop(it.npc) }
    }

    private fun Player.openAmuletShop(npc: Npc) {
        shops.open(this, npc, "Davon's Amulet Store.", "inv.amuletshop")
    }

    private suspend fun Dialogue.davon(npc: Npc) {
        chatNpc(shifty, "Pssst! Come here if you want to do some amulet trading.")
        val option =
            choice3(
                "What are you selling?",
                1,
                "What do you mean pssst?",
                2,
                "Why don't you ever restock some types of amulets?",
                3,
            )
        when (option) {
            1 -> player.openAmuletShop(npc)
            2 -> {
                chatPlayer(confused, "What do you mean pssst?")
                chatNpc(shifty, "Errr, I was...I was clearing my throat.")
            }
            3 -> {
                chatPlayer(quiz, "Why don't you ever restock some types of amulets?")
                chatNpc(
                    neutral,
                    "Some of these amulets are very hard to get. I have to wait until an " +
                        "adventurer supplies me.",
                )
            }
        }
    }
}
