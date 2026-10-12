package org.rsmod.content.areas.city.desert.sophanem.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Jamila
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:beneath_cursed_sands] start offer after "What's this special item?"
class JamilasCraftStall @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(JAMILA) { startDialogue(it.npc) { jamila(it.npc) } }
        onOpNpc3(JAMILA) { player.openStore(it.npc) }
    }

    private fun Player.openStore(npc: Npc) {
        shops.open(this, npc, "Jamila's Craft Stall.", "inv.contact_craftingshop")
    }

    private suspend fun Dialogue.jamila(npc: Npc) {
        chatNpc(
            happy,
            "Hello there. Looking to buy some of my wares? I also have a special item in stock " +
                "that I reckon you'll love!",
        )
        while (true) {
            val option =
                choice4(
                    "Show me what you have.",
                    1,
                    "What's this special item?",
                    2,
                    "How is business?",
                    3,
                    "I'm good, thanks.",
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Show me what you have.")
                    chatNpc(happy, "Certainly!")
                    player.openStore(npc)
                    return
                }
                2 -> {
                    chatPlayer(quiz, "What's this special item?")
                    chatNpc(
                        happy,
                        "Oh it's fantastic! It's an exclusive item just for you! Consider it a " +
                            "thank you for all you've done for our city.",
                    )
                    return
                }
                3 -> {
                    chatPlayer(quiz, "How is business?")
                    chatNpc(
                        sad,
                        "Slow at the moment. Maybe time will make things better. Perhaps you'd " +
                            "like to help by buying something yourself?",
                    )
                }
                4 -> {
                    chatPlayer(happy, "I'm good, thanks.")
                    chatNpc(happy, "Come back soon!")
                    return
                }
            }
        }
    }

    private companion object {
        const val JAMILA = "npc.contact_market_craft"
    }
}
