package org.rsmod.content.areas.misc.islands.fossilisland.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Shop_keeper_(Fossil_Island)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:fossil_notebook] "I've lost my notebook."
class FossilGeneralStore @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(SHOP_KEEPER) { startDialogue(it.npc) { shopKeeper(it.npc) } }
        onOpNpc3(SHOP_KEEPER) { player.openGeneralStore(it.npc) }
    }

    private fun Player.openGeneralStore(npc: Npc) {
        shops.open(this, npc, "Fossil Island General Store", "inv.fossil_general_store")
    }

    private suspend fun Dialogue.shopKeeper(npc: Npc) {
        chatPlayer(happy, "Hello!")
        val address = if (access.isBodyTypeA()) "lad" else "lass"
        chatNpc(happy, "Good day $address. What can I do for you?")
        while (true) {
            val option =
                choice3(
                    "Why are you here?",
                    1,
                    "Show me your wares.",
                    2,
                    "Nothing for now.",
                    3,
                )
            when (option) {
                1 ->
                    chatNpc(
                        happy,
                        "Well, these folks digging up the island need some of your everyday " +
                            "things, so I offered to come along and offer my wares.",
                    )
                2 -> {
                    player.openGeneralStore(npc)
                    return
                }
                3 -> {
                    chatPlayer(neutral, "Nothing for now.")
                    return
                }
            }
        }
    }

    private companion object {
        const val SHOP_KEEPER = "npc.fossil_general_store"
    }
}
