package org.rsmod.content.areas.city.desert.bedabin.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Bedabin_Nomad
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class BedabinVillageBartering @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(NOMAD) { startDialogue(it.npc) { nomad(it.npc) } }
        onOpNpc3(NOMAD) { player.openStore(it.npc) }
    }

    private fun Player.openStore(npc: Npc) {
        shops.open(this, npc, "Bedabin Village Bartering", "inv.bedabincampshop")
    }

    private suspend fun Dialogue.nomad(npc: Npc) {
        chatNpc(happy, "Hello Effendi! How can I help you?")
        while (true) {
            val option =
                choice4(
                    "What is this place?",
                    1,
                    "Where is the Shantay Pass?",
                    2,
                    "What do you have to sell?",
                    3,
                    "Okay, thanks",
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "What is this place?")
                    chatNpc(
                        happy,
                        "This is the camp of the Bedabin. Talk to our leader, Al Shabim, he'll be " +
                            "happy to chat. We can sell you very reasonably priced water...",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "Where is the Shantay Pass?")
                    chatNpc(
                        neutral,
                        "It is North East of here effendi, across the trackless desert. It will " +
                            "be a thirsty trip, can I interest you in a drink?",
                    )
                }
                3 -> {
                    player.openStore(npc)
                    return
                }
                4 -> {
                    chatPlayer(happy, "Okay, thanks")
                    return
                }
            }
        }
    }

    private companion object {
        const val NOMAD = "npc.bedabin"
    }
}
