package org.rsmod.content.areas.city.rellekka.neitiznot.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Jofridr_Mordstatter
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class NeitiznotSupplies @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.frisb_n_sto") { shopDialogue(it.npc) }
        onOpNpc3("npc.frisb_n_sto") { player.openNeitiznotSupplies(it.npc) }
    }

    private fun Player.openNeitiznotSupplies(npc: Npc) {
        shops.open(this, npc, "Neitiznot supplies.", "inv.frisb_n_shop")
    }

    private suspend fun ProtectedAccess.shopDialogue(npc: Npc) =
        startDialogue(npc) { jofridr(npc) }

    private suspend fun Dialogue.jofridr(npc: Npc) {
        chatNpc(happy, "Hello there. Would you like to see the goods I have for sale?")
        while (true) {
            val option =
                choice3(
                    "Yes please, Jofridr",
                    1,
                    "No thank you, Jofridr",
                    2,
                    "Why do you have so much wool in your store?",
                    3,
                )
            when (option) {
                1 -> {
                    chatPlayer(happy, "Yes please, Jofridr.")
                    player.openNeitiznotSupplies(npc)
                    return
                }
                2 -> {
                    chatPlayer(neutral, "No thank you, Jofridr.")
                    chatNpc(happy, "Fair thee well.")
                    return
                }
                3 -> {
                    wool()
                    chatNpc(happy, "Would you like to see the goods I have for sale?")
                }
            }
        }
    }

    private suspend fun Dialogue.wool() {
        chatPlayer(
            quiz,
            "Why do you have so much wool in your store? I haven't seen any sheep anywhere.",
        )
        chatNpc(
            happy,
            "Ah, I have contacts on the mainland. I have a sailor friend who brings me crates of " +
                "wool on a regular basis.",
        )
        chatPlayer(quiz, "What do you trade for it?")
        chatNpc(
            happy,
            "Rope of course! What else can we sell? Fish would go off before it got so far south.",
        )
        chatPlayer(quiz, "Where does all this rope go?")
        chatNpc(
            confused,
            "Err, I don't remember the name of the place very well. Dreinna? Drennor? Something " +
                "like that.",
        )
        chatPlayer(happy, "That's very interesting. Thanks Jofridr.")
    }
}
