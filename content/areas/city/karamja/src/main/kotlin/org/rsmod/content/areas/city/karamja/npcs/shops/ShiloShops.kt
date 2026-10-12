package org.rsmod.content.areas.city.karamja.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class ShiloShops @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.shilofishowner") { startDialogue(it.npc) { fernahei(it.npc) } }
        onOpNpc3("npc.shilofishowner") { player.openFishingHut(it.npc) }
        onOpNpc1("npc.shilogeneralstore") { startDialogue(it.npc) { obli(it.npc) } }
        onOpNpc3("npc.shilogeneralstore") { player.openJungleStore(it.npc) }
    }

    private fun Player.openFishingHut(npc: Npc) {
        shops.open(this, npc, "Fernahei's Fishing Hut.", "inv.shilofishingshop")
    }

    private fun Player.openJungleStore(npc: Npc) {
        shops.open(this, npc, "Obli's General Store.", "inv.shilojunglestore")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Fernahei
    private suspend fun Dialogue.fernahei(npc: Npc) {
        if (!QuestRequirements.hasCompleted(player, SHILO_VILLAGE)) {
            zombies()
            return
        }
        chatNpc(
            happy,
            "Welcome to Fernahei's Fishing Shop Bwana! Would you like to see my items?",
        )
        if (choice2("Yes please!", true, "No, but thanks for the offer.", false)) {
            chatPlayer(happy, "Yes please!")
            player.openFishingHut(npc)
        } else {
            chatPlayer(neutral, "No, but thanks for the offer.")
            chatNpc(happy, "That's fine and thanks for your interest.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Obli
    private suspend fun Dialogue.obli(npc: Npc) {
        if (!QuestRequirements.hasCompleted(player, SHILO_VILLAGE)) {
            zombies()
            return
        }
        chatNpc(happy, "Welcome to Obli's General Store Bwana! Would you like to see my items?")
        if (choice2("Yes please!", true, "No, but thanks for the offer.", false)) {
            chatPlayer(happy, "Yes please!")
            player.openJungleStore(npc)
        } else {
            chatPlayer(neutral, "No, but thanks for the offer.")
        }
    }

    private suspend fun Dialogue.zombies() {
        chatNpc(sad, "I'm getting out of here Bwana, the place is infested with zombies.")
    }

    private companion object {
        const val SHILO_VILLAGE = "quest_shilovillage"
    }
}
