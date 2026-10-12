package org.rsmod.content.areas.city.desert.sophanem.npcs.shops

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

// https://oldschool.runescape.wiki/w/Transcript:Raetul (after Icthlarin's Little Helper)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:icthlarins_little_helper] dialogue during the quest
// TODO(content): [system:acne_potion] potion used on Raetul
class RaetulClothStore @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(RAETUL) { startDialogue(it.npc) { raetul(it.npc) } }
        onOpNpc3(RAETUL) { player.openStore(it.npc) }
    }

    // https://oldschool.runescape.wiki/w/Raetul_and_Co%27s_Cloth_Store.
    private fun Player.openStore(npc: Npc) {
        val stock =
            if (QuestRequirements.hasCompleted(this, CONTACT)) {
                "inv.sophanem_cloth_store_updated"
            } else {
                "inv.sophanem_cloth_store"
            }
        shops.open(this, npc, "Raetul and Co's Cloth Store", stock)
    }

    private suspend fun Dialogue.raetul(npc: Npc) {
        chatPlayer(happy, "Hello.")
        chatNpc(neutral, "What can I do for you?")
        if (choice2("What do you have for sale?", true, "Nothing.", false)) {
            chatPlayer(quiz, "What do you have for sale?")
            chatNpc(neutral, "All sorts of things. Why don't you have a look?")
            player.openStore(npc)
        } else {
            chatPlayer(neutral, "Nothing.")
            chatNpc(bored, "Right...")
        }
    }

    private companion object {
        const val RAETUL = "npc.ics_little_linen1"
        const val CONTACT = "quest_contact"
    }
}
