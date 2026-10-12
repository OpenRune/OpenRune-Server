package org.rsmod.content.areas.city.gnomestronghold.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Hudo
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:gnome_restaurant] delivery hand-in dialogue
class GrandTreeGroceries
@Inject
constructor(private val shops: Shops, private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(HUDO) { startDialogue(it.npc) { hudo(it.npc) } }
        onOpNpc3(HUDO) { player.openShop(it.npc) }
    }

    private fun Player.openShop(npc: Npc) {
        shops.open(this, npc, "Grand Tree Groceries", "inv.gnomeshop_hudo")
    }

    private suspend fun Dialogue.hudo(npc: Npc) {
        chatPlayer(happy, "Hello there.")
        if (player.inv.physicalCount(FROG_LEGS) == 0) {
            chatNpc(
                happy,
                "Hello there traveller. Would you like some groceries? I have a large selection.",
            )
            groceries(npc)
            return
        }
        chatNpc(happy, "Hello there traveller. Would you like...")
        chatNpc(
            shocked,
            "Great Guthix! Where did you get those giant frogs' legs? They're enormous!",
        )
        chatNpc(happy, "Are you selling them? I'll give you 1000gp for all the frogs' legs you have!")
        if (choice2("They're not for sale.", false, "Yes, that's reasonable.", true)) {
            chatPlayer(happy, "Yes, that's reasonable.")
            buyFrogLegs()
            chatNpc(
                happy,
                "A pleasure doing business with you.  Now do you need any groceries?",
            )
        } else {
            chatPlayer(neutral, "They're not for sale.")
            chatNpc(sad, "Oh well.  Would you like to buy any groceries?")
        }
        groceries(npc)
    }

    private suspend fun Dialogue.groceries(npc: Npc) {
        if (choice2("No thank you.", false, "I'll have a look.", true)) {
            chatPlayer(happy, "I'll have a look.")
            chatNpc(happy, "Great stuff.")
            player.openShop(npc)
            return
        }
        chatPlayer(neutral, "No thank you.")
        chatNpc(happy, "No problem.")
    }

    private fun Dialogue.buyFrogLegs() {
        val count = player.inv.physicalCount(FROG_LEGS)
        if (count == 0) {
            return
        }
        val taken = player.invDel(player.inv, FROG_LEGS, count = count, ignoreVirtualStorage = true)
        if (!taken.success) {
            return
        }
        player.invAddOrDrop(objRepo, COINS, count * FROG_LEGS_PRICE)
    }

    private companion object {
        const val HUDO = "npc.gnome_hudo"
        const val COINS = "obj.coins"
        const val FROG_LEGS = "obj.giant_frog_legs"
        const val FROG_LEGS_PRICE = 1000
    }
}
