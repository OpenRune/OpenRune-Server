package org.rsmod.content.areas.city.gnomestronghold.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invTakeFee
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.output.mes
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Barman_(Blurberry_Bar)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [miniquest:alfred_grimhands_barcrawl] "I'm trying to do Alfred Grimhand's
//  barcrawl."
class BlurberryBar
@Inject
constructor(private val shops: Shops, private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(BARMAN) { startDialogue(it.npc) { barman(it.npc) } }
        onOpNpc3(BARMAN) { player.openShop(it.npc) }
    }

    private fun Player.openShop(npc: Npc) {
        shops.open(this, npc, "Blurberry Bar", "inv.blurberrybar")
    }

    private suspend fun Dialogue.barman(npc: Npc) {
        chatNpc(happy, "Good day to you. What can I get you to drink?")
        val option =
            choice3(
                "What do you have?",
                1,
                "Nothing thanks.",
                2,
                "Can I buy some ingredients?",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "What do you have.")
                chatNpc(happy, "Here, take a look at our menu.")
                player.openShop(npc)
            }
            2 -> {
                chatPlayer(neutral, "Nothing thanks.")
                chatNpc(happy, "Ok, take it easy.")
            }
            3 -> ingredients()
        }
    }

    private suspend fun Dialogue.ingredients() {
        chatPlayer(quiz, "I was just wanting to buy a cocktail ingredient actually.")
        chatNpc(happy, "Sure thing, what did you want?")
        val option =
            choice4(
                "A lemon",
                LEMON,
                "An orange.",
                ORANGE,
                "A cocktail shaker.",
                COCKTAIL_SHAKER,
                "Nothing thanks.",
                null,
            )
        when (option) {
            LEMON -> chatPlayer(happy, "A lemon.")
            ORANGE -> chatPlayer(happy, "An orange.")
            COCKTAIL_SHAKER -> chatPlayer(happy, "A cocktail shaker.")
        }
        if (option == null) {
            chatPlayer(neutral, "Actually nothing thanks.")
            return
        }
        if (!player.inv.hasFreeSpace()) {
            mesbox("You don't have enough room in your inventory to buy that.")
            return
        }
        chatNpc(happy, "$PRICE coins please.")
        if (!player.invTakeFee(PRICE)) {
            player.mes("You do not have enough money to buy that.")
            return
        }
        player.invAddOrDrop(objRepo, option)
    }

    private companion object {
        const val BARMAN = "npc.blurberrybarmen"
        const val LEMON = "obj.lemon"
        const val ORANGE = "obj.orange"
        const val COCKTAIL_SHAKER = "obj.cocktail_shaker"
        const val PRICE = 20
    }
}
