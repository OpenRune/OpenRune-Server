package org.rsmod.content.areas.city.lletya.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class LletyaShops @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(DALLDAV) { startDialogue(it.npc) { dalldav(it.npc) } }
        onOpNpc3(DALLDAV) { player.openArchery(it.npc) }
        onOpNpc1(EUDAV) { startDialogue(it.npc) { eudav(it.npc) } }
        onOpNpc3(EUDAV) { player.openGeneral(it.npc) }
        onOpNpc1(GETHIN) { startDialogue(it.npc) { gethin(it.npc) } }
        onOpNpc3(GETHIN) { player.openFood(it.npc) }
        onOpNpc1(ORONWEN) { startDialogue(it.npc) { oronwen(it.npc) } }
        onOpNpc3(ORONWEN) { player.openSeamstress(it.npc) }
    }

    private fun Player.openArchery(npc: Npc) {
        shops.open(this, npc, "Lletya Archery Shop", "inv.lletyaarcheryshop1")
    }

    private fun Player.openGeneral(npc: Npc) {
        shops.open(this, npc, "Lletya General Store", "inv.lletyageneralshop1")
    }

    private fun Player.openFood(npc: Npc) {
        shops.open(this, npc, "Lletya Food Store", "inv.lletyafoodshop1")
    }

    private fun Player.openSeamstress(npc: Npc) {
        shops.open(this, npc, "Lletya Seamstress", "inv.lletyaseamstressshop1")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Dalldav
    private suspend fun Dialogue.dalldav(npc: Npc) {
        chatNpc(happy, "Can I help you at all?")
        val option =
            choice3(
                "Yes please. What are you selling?",
                1,
                "Why do you sell this stuff?",
                2,
                "No thanks.",
                3,
            )
        when (option) {
            1 -> player.openArchery(npc)
            2 -> {
                chatPlayer(
                    quiz,
                    "Why do you sell this stuff? The Crystal Bow is so much better.",
                )
                chatNpc(
                    happy,
                    "We keep all these old toys to train our children with, but if people will " +
                        "part with coins for them, then they are theirs!",
                )
            }
            3 -> chatPlayer(neutral, "No thanks.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Eudav
    private suspend fun Dialogue.eudav(npc: Npc) {
        chatNpc(happy, "Can I help you at all?")
        when (choice2("Yes please. What are you selling?", 1, "No thanks.", 2)) {
            1 -> player.openGeneral(npc)
            2 -> chatPlayer(neutral, "No thanks.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Gethin
    private suspend fun Dialogue.gethin(npc: Npc) {
        chatNpc(happy, "Can I help you at all?")
        when (choice2("Yes please. What are you selling?", 1, "No thanks.", 2)) {
            1 -> player.openFood(npc)
            2 -> chatPlayer(neutral, "No thanks.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Oronwen
    // TODO(content): [system:treasure_trails] elite challenge scroll
    private suspend fun Dialogue.oronwen(npc: Npc) {
        chatNpc(happy, "Hello, can I help?")
        when (choice2("Yes please. What are you selling?", 1, "No thanks.", 2)) {
            1 -> {
                chatPlayer(quiz, "Yes please. What are you selling?")
                player.openSeamstress(npc)
            }
            2 -> chatPlayer(neutral, "No thanks.")
        }
    }

    private companion object {
        const val DALLDAV = "npc.mourning_bow_seller"
        const val EUDAV = "npc.mourning_stores_man"
        const val GETHIN = "npc.mourning_elven_cook"
        const val ORONWEN = "npc.mourning_seamstress"
    }
}
