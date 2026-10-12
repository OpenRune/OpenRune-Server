package org.rsmod.content.areas.misc.islands.apeatoll.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invTakeFee
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Tutab
// https://oldschool.runescape.wiki/w/Transcript:Daga
// https://oldschool.runescape.wiki/w/Transcript:Hamab
// https://oldschool.runescape.wiki/w/Transcript:Ifaba
// https://oldschool.runescape.wiki/w/Transcript:Solihib
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:monkey_disguise] guards are called on a player in human form until
// Monkey Madness II is complete (QuestRequirements, "quest_monkeymadness2")
class MarimShops
@Inject
constructor(private val shops: Shops, private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(TUTAB) { startDialogue(it.npc) { tutab(it.npc) } }
        onOpNpc3(TUTAB) { shops.open(player, it.npc, TUTAB_SHOP, TUTAB_INV) }
        onOpNpc1(DAGA) { startDialogue(it.npc) { daga(it.npc) } }
        onOpNpc3(DAGA) { shops.open(player, it.npc, DAGA_SHOP, DAGA_INV) }
        onOpNpc1(HAMAB) { startDialogue(it.npc) { hamab(it.npc) } }
        onOpNpc3(HAMAB) { shops.open(player, it.npc, HAMAB_SHOP, HAMAB_INV) }
        onOpNpc1(IFABA) { startDialogue(it.npc) { ifaba(it.npc) } }
        onOpNpc3(IFABA) { shops.open(player, it.npc, IFABA_SHOP, IFABA_INV) }
        onOpNpc1(SOLIHIB) { startDialogue(it.npc) { solihib(it.npc) } }
        onOpNpc3(SOLIHIB) { shops.open(player, it.npc, SOLIHIB_SHOP, SOLIHIB_INV) }
    }

    private suspend fun Dialogue.tutab(npc: Npc) {
        chatNpc(happy, "Would you like to buy or sell some magical items?")
        offerShop(npc, TUTAB_SHOP, TUTAB_INV)
    }

    private suspend fun Dialogue.hamab(npc: Npc) {
        chatNpc(happy, "Would you like to buy or sell some crafting equipment?")
        offerShop(npc, HAMAB_SHOP, HAMAB_INV)
    }

    private suspend fun Dialogue.ifaba(npc: Npc) {
        chatNpc(happy, "Would you like to buy or sell anything?")
        offerShop(npc, IFABA_SHOP, IFABA_INV)
    }

    private suspend fun Dialogue.solihib(npc: Npc) {
        chatNpc(happy, "Would you like to buy or sell some food?")
        offerShop(npc, SOLIHIB_SHOP, SOLIHIB_INV)
    }

    private suspend fun Dialogue.offerShop(npc: Npc, title: String, inv: String) {
        if (choice2("Yes, please.", true, "No, thanks.", false)) {
            chatPlayer(happy, "Yes, please.")
            shops.open(player, npc, title, inv)
        } else {
            chatPlayer(neutral, "No, thanks.")
        }
    }

    // TODO(content): [verify:in_game] mm_scimitar_shop2 is the stock after Monkey Madness I; both
    // scimitar shop invs hold 0 dragon scimitars
    private suspend fun Dialogue.daga(npc: Npc) {
        chatNpc(happy, "Would you like to buy or sell some scimitars?")
        val option =
            choice3(
                "Yes, please.",
                1,
                "No, thanks.",
                2,
                "Do you have any Dragon Scimitars in stock?",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(happy, "Yes, please.")
                shops.open(player, npc, DAGA_SHOP, DAGA_INV)
            }
            2 -> chatPlayer(neutral, "No, thanks.")
            3 -> dragonScimitar()
        }
    }

    // TODO(content): [verify:in_game] the purchase message is a plain objbox
    private suspend fun Dialogue.dragonScimitar() {
        chatPlayer(quiz, "Do you have any Dragon Scimitars in stock?")
        chatNpc(
            happy,
            "It just so happens I recently got a fresh delivery. Do you want to buy one?",
        )
        if (!choice2("Yes, please.", true, "No, thanks.", false)) {
            chatPlayer(neutral, "No, thanks.")
            return
        }
        chatPlayer(happy, "Yes, please.")
        if (!player.invTakeFee(DRAGON_SCIMITAR_PRICE)) {
            chatNpc(
                neutral,
                "Sorry but you don't have enough to buy one. At the moment it costs 100000 gold " +
                    "coins.",
            )
            return
        }
        player.invAddOrDrop(objRepo, DRAGON_SCIMITAR)
        objbox(DRAGON_SCIMITAR, "You buy a dragon scimitar!")
    }

    private companion object {
        const val TUTAB = "npc.mm_tutab"
        const val TUTAB_SHOP = "Tutab's Magical Market"
        const val TUTAB_INV = "inv.mm_magic_shop"
        const val DAGA = "npc.mm_daga"
        const val DAGA_SHOP = "Daga's Scimitar Smithy"
        const val DAGA_INV = "inv.mm_scimitar_shop2"
        const val HAMAB = "npc.mm_hamab"
        const val HAMAB_SHOP = "Hamab's Crafting Emporium"
        const val HAMAB_INV = "inv.mm_crafting_shop"
        const val IFABA = "npc.mm_ifaba"
        const val IFABA_SHOP = "Ifaba's General Store"
        const val IFABA_INV = "inv.mm_general_shop"
        const val SOLIHIB = "npc.mm_solihib"
        const val SOLIHIB_SHOP = "Solihib's Food Stall"
        const val SOLIHIB_INV = "inv.mm_food_shop"
        const val DRAGON_SCIMITAR = "obj.dragon_scimitar"
        const val DRAGON_SCIMITAR_PRICE = 100_000
    }
}
