package org.rsmod.content.areas.city.catherby.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invTakeFee
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Harry
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class FishingShop
@Inject
constructor(private val shops: Shops, private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.harry") { startDialogue(it.npc) { harry(it.npc) } }
        onOpNpc3("npc.harry") { player.openFishingShop(it.npc) }
    }

    private fun Player.openFishingShop(npc: Npc) {
        shops.open(this, npc, "Harry's Fishing Shop.", "inv.fishingshop2")
    }

    private suspend fun Dialogue.harry(npc: Npc) {
        chatNpc(
            happy,
            "Welcome, you can buy fishing equipment at my store. We'll also give you a good " +
                "price for any fish that you catch.",
        )
        val weedBowl = FISHBOWL_WATER_WEED in player.inv
        val hasBowl = weedBowl || FISHBOWL_WATER in player.inv
        val option =
            if (hasBowl) {
                choice4(
                    "Let's see what you've got then.",
                    1,
                    "Can I get a fish for this bowl?",
                    2,
                    "Do you have any fishfood?",
                    3,
                    "Sorry, I'm not interested.",
                    4,
                )
            } else {
                choice2("Let's see what you've got then.", 1, "Sorry, I'm not interested.", 4)
            }
        when (option) {
            1 -> player.openFishingShop(npc)
            2 -> fishForBowl(weedBowl)
            3 -> fishFood()
            4 -> chatPlayer(neutral, "Sorry, I'm not interested.")
        }
    }

    private suspend fun Dialogue.fishForBowl(weedBowl: Boolean) {
        chatPlayer(quiz, "Can I get a fish for this bowl?")
        if (!weedBowl) {
            chatNpc(neutral, "Sorry, you need to put some seaweed into the bowl first.")
            chatPlayer(confused, "Seaweed?")
            chatNpc(
                neutral,
                "Yes, the fish seem to like it. Come and see me when you have put some in the " +
                    "bowl.",
            )
            return
        }
        chatNpc(happy, "Yes you can!")
        chatNpc(
            happy,
            "I can see that you have a nicely filled fishbowl there to use, and you can catch a " +
                "fish from my aquarium if you want.",
        )
        chatNpc(
            neutral,
            "You will need a special net to do this though, and I sell them for 10 gold.",
        )
        if (!choice2("I'll take it!", true, "No thanks, later maybe", false)) {
            chatPlayer(neutral, "No thanks, later maybe.")
            return
        }
        chatPlayer(happy, "I'll take it!")
        if (!player.invTakeFee(TINY_NET_PRICE)) {
            chatNpc(
                neutral,
                "Well, I'll be happy to give you the net once you have the cash, but not before!",
            )
            return
        }
        player.invAddOrDrop(objRepo, TINY_NET)
        chatNpc(happy, "Here you go!")
    }

    private suspend fun Dialogue.fishFood() {
        chatPlayer(quiz, "Do you have any fishfood?")
        chatNpc(
            sad,
            "Sorry, I'm all out. I used up the last of it feeding the fish in the aquarium.",
        )
        chatNpc(
            neutral,
            "I have some empty boxes though, they have the ingredients written on the back. I'm " +
                "sure if you pick up a pestle and mortar you will be able to make your own.",
        )
        player.invAddOrDrop(objRepo, EMPTY_FISHFOOD_BOX)
        chatNpc(happy, "Here. I can hardly charge you for an empty box.")
    }

    private companion object {
        const val TINY_NET_PRICE = 10
        const val TINY_NET = "obj.fishbowl_net"
        const val EMPTY_FISHFOOD_BOX = "obj.empty_fishfood_box"
        const val FISHBOWL_WATER = "obj.fishbowl_water"
        const val FISHBOWL_WATER_WEED = "obj.fishbowl_water_weed"
    }
}
