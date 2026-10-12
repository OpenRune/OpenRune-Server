package org.rsmod.content.areas.city.gnomestronghold.village.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Bolkoy (before Tree Gnome Village)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:tree_gnome_village] after-quest greeting
// TODO(content): [system:treasure_trails] hard challenge scroll
class VillageShop @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(BOLKOY) { startDialogue(it.npc) { bolkoy(it.npc) } }
        onOpNpc3(BOLKOY) { player.openShop(it.npc) }
    }

    private fun Player.openShop(npc: Npc) {
        shops.open(this, npc, "Bolkoy's Village Shop", "inv.gnomeshop")
    }

    private suspend fun Dialogue.bolkoy(npc: Npc) {
        chatPlayer(happy, "Hello there.")
        chatNpc(happy, "Hello stranger, new to these parts?")
        chatNpc(
            happy,
            "I'm Bolkoy by the way. I'm the village shopkeeper. Would you like to buy something?",
        )
        if (choice2("What have you got?", true, "No thank you.", false)) {
            chatPlayer(quiz, "What have you got?")
            chatNpc(happy, "Take a look.")
            player.openShop(npc)
            return
        }
        chatPlayer(neutral, "No thank you.")
        chatNpc(happy, "Ok, maybe later.")
    }

    private companion object {
        const val BOLKOY = "npc.treevillage_shopkeeper1"
    }
}
