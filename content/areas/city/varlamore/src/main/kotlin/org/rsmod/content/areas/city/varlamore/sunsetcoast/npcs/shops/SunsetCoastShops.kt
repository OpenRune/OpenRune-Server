package org.rsmod.content.areas.city.varlamore.sunsetcoast.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class SunsetCoastShops @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        for (shop in SHOPS) {
            onOpNpc3(shop.npc) { open(it.npc, shop) }
        }
        onOpNpc1(FISHING.npc) { startDialogue(it.npc) { picaria(it.npc) } }
        onOpNpc1(GENERAL.npc) { startDialogue(it.npc) { shopkeeper(it.npc) } }
        onOpNpc1(HELMETS.npc) { startDialogue(it.npc) { thurid(it.npc) } }
    }

    private fun ProtectedAccess.open(npc: Npc, shop: Shop) {
        shops.open(player, npc, shop.title, shop.inv)
    }

    private fun Dialogue.open(npc: Npc, shop: Shop) {
        access.open(npc, shop)
    }

    // https://oldschool.runescape.wiki/w/Transcript:Picaria
    private suspend fun Dialogue.picaria(npc: Npc) {
        chatNpc(
            happy,
            "Hey there. Here to do some fishing? It's a great place for it. Or are you looking " +
                "to buy some fishing supplies?",
        )
        while (true) {
            val option =
                choice3(
                    "Let's see what you're selling.",
                    1,
                    "Do you get many customers out here?",
                    2,
                    "Just passing through.",
                    3,
                )
            when (option) {
                1 -> {
                    chatPlayer(happy, "Let's see what you're selling.")
                    return open(npc, FISHING)
                }
                2 -> {
                    chatPlayer(quiz, "Do you get many customers out here?")
                    chatNpc(
                        neutral,
                        "Not so much out here, but I sell plenty of fish to merchants in the " +
                            "capital. If you buy any fish there, there's a good chance it came " +
                            "from me.",
                    )
                    chatNpc(happy, "So anyway, looking to buy some fishing supplies?")
                }
                else -> {
                    chatPlayer(neutral, "Just passing through.")
                    chatNpc(neutral, "Fair enough.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Shopkeeper_(Sunset_Coast)
    private suspend fun Dialogue.shopkeeper(npc: Npc) {
        chatNpc(happy, "Nilsal. In need of supplies?")
        when (choice2("What do you have?", 1, "No thanks.", 2)) {
            1 -> {
                chatPlayer(quiz, "What do you have?")
                chatNpc(happy, "Take a look.")
                open(npc, GENERAL)
            }
            2 -> {
                chatPlayer(neutral, "No thanks.")
                chatNpc(neutral, "As you wish.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Thurid (before The Fremennik Trials)
    // TODO(content): [quest:the_fremennik_trials] the greeting with the player's Fremennik name
    private suspend fun Dialogue.thurid(npc: Npc) {
        chatNpc(
            happy,
            "Looking to protect that nice brain of yours? Then you'll be wanting one of my " +
                "helmets.",
        )
        when (choice2("I'll have a look.", 1, "I'm good.", 2)) {
            1 -> {
                chatPlayer(happy, "I'll have a look.")
                chatNpc(happy, "Good choice.")
                open(npc, HELMETS)
            }
            2 -> {
                chatPlayer(neutral, "I'm good.")
                chatNpc(neutral, "If you're sure.")
            }
        }
    }

    private class Shop(val npc: String, val title: String, val inv: String)

    private companion object {
        val FISHING =
            Shop(
                "npc.sunset_coast_fishing_shop",
                "Picaria's Fishing Shop",
                "inv.sunset_coast_fishing_shop",
            )
        val GENERAL =
            Shop(
                "npc.sunset_coast_general_store",
                "Sunset Coast General Store",
                "inv.sunset_coast_general_store",
            )
        val HELMETS =
            Shop(
                "npc.sunset_coast_helmet_shop",
                "Thurid's Brain Buckets",
                "inv.sunset_coast_helmet_shop",
            )

        val SHOPS = listOf(FISHING, GENERAL, HELMETS)
    }
}
