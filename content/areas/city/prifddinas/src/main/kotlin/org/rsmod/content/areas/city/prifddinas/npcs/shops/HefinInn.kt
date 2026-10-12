package org.rsmod.content.areas.city.prifddinas.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invTakeFee
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Eira
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class HefinInn @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.prif_bartender") { startDialogue(it.npc) { eira() } }
    }

    private suspend fun Dialogue.eira() {
        chatNpc(happy, "Welcome to the Hefin Inn! What can I getcha?")
        val option =
            choice5(
                "Just a beer.",
                1,
                "Something a bit fruity.",
                2,
                "Something to eat.",
                3,
                "Some kind of local specialty.",
                4,
                "Nothing.",
                5,
            )
        when (option) {
            1 -> {
                chatPlayer(happy, "Just a beer.")
                chatNpc(happy, "I can get you a Beer for 2 coins.")
                offer("obj.beer", 2)
            }
            2 -> {
                chatPlayer(happy, "Something a bit fruity.")
                chatNpc(happy, "I can get you a Cider for 2 coins.")
                offer("obj.cider", 2)
            }
            3 -> {
                chatPlayer(happy, "Something to eat.")
                chatNpc(happy, "I can get you a Stew for 20 coins.")
                offer("obj.stew", 20)
            }
            4 -> {
                chatPlayer(happy, "Some kind of local specialty.")
                chatNpc(happy, "I can get you an Elven dawn for 6 coins.")
                offer("obj.prif_elven_dawn", 6)
            }
            5 -> chatPlayer(neutral, "Nothing. Maybe later!")
        }
    }

    private suspend fun Dialogue.offer(obj: String, price: Int) {
        if (access.invTotal(player.inv, COINS) < price) {
            chatPlayer(sad, "I don't have that much on me... Maybe later.")
            return
        }
        when (choice2("Here you go!", 1, "Actually, maybe not.", 2)) {
            1 -> {
                chatPlayer(happy, "Here you go!")
                if (player.invTakeFee(price)) {
                    player.invAddOrDrop(objRepo, obj)
                }
            }
            2 -> chatPlayer(neutral, "Actually, maybe not.")
        }
    }

    private companion object {
        const val COINS = "obj.coins"
    }
}
