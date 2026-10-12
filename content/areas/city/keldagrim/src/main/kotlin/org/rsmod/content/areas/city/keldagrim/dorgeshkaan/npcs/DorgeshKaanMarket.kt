package org.rsmod.content.areas.city.keldagrim.dorgeshkaan.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invTakeFee
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class DorgeshKaanMarket
@Inject
constructor(private val shops: Shops, private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        for (stall in FOOD_STALLS) {
            onOpNpc1(stall.npc) { startDialogue(it.npc) { foodStall(stall) } }
        }
        onOpNpc1(LURGON) { startDialogue(it.npc) { lurgon(it.npc) } }
        onOpNpc3(LURGON) { openGeneralStore(it.npc) }
        onOpNpc1(MILTOG) { startDialogue(it.npc) { miltog(it.npc) } }
        onOpNpc3(MILTOG) { openLampShop(it.npc) }
        onOpNpc1(RELDAK) { startDialogue(it.npc) { reldak(it.npc) } }
        onOpNpc3(RELDAK) { openLeatherShop(it.npc) }
    }

    private fun ProtectedAccess.openGeneralStore(npc: Npc) =
        shops.open(player, npc, "Dorgesh-Kaan General Supplies", "inv.dorgesh_general_store")

    private fun ProtectedAccess.openLampShop(npc: Npc) =
        shops.open(player, npc, "Miltog's Lamps", "inv.dorgesh_lamp_shop")

    private fun ProtectedAccess.openLeatherShop(npc: Npc) =
        shops.open(player, npc, "Reldak's Leather Armour", "inv.dorgesh_leather_shop")

    // https://oldschool.runescape.wiki/w/Transcript:Durgok (one transcript per food stall)
    private suspend fun Dialogue.foodStall(stall: FoodStall) {
        chatNpc(happy, stall.pitch)
        if (!choice2("Yes please.", true, "No thanks.", false)) {
            chatPlayer(neutral, "No thanks.")
            return
        }
        chatPlayer(happy, "Yes please!")
        if (player.inv.count(COINS) < FOOD_PRICE) {
            chatPlayer(sad, "But I don't have enough money on me.")
            return
        }
        val fullInventory = player.inv.isFull() && player.inv.count(COINS) != FOOD_PRICE
        if (stall.refusesFullInventory && fullInventory) {
            chatNpc(
                neutral,
                "Looks like your hands are full. You'll have to free up some inventory space " +
                    "before I sell you anything.",
            )
            return
        }
        if (!player.invTakeFee(FOOD_PRICE)) {
            return
        }
        player.invAddOrDrop(objRepo, stall.food)
        chatNpc(happy, "There you go.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Lurgon
    // TODO(content): [system:dorgesh_kaan_market] council permission and the councillor's name
    private suspend fun Dialogue.lurgon(npc: Npc) {
        chatNpc(
            happy,
            "Welcome, surface-dweller, to the Dorgesh-Kaan marketplace! Are you here to sell " +
                "your exotic surface foods?",
        )
        val option =
            choice4(
                "Yes",
                1,
                "How can I sell food?",
                2,
                "Are you selling anything?",
                3,
                "Goodbye.",
                4,
            )
        when (option) {
            1 -> chatPlayer(happy, "Yes!")
            2 -> {
                chatPlayer(quiz, "How can I sell food?")
                chatNpc(
                    neutral,
                    "Most of the people wandering around the market want to buy food. Since the " +
                        "passage to the surface opened up there's been a craze in exotic food " +
                        "from up there.",
                )
                chatNpc(
                    neutral,
                    "To sell something, simply use a piece of food on one of the gourmets in " +
                        "the market, then name your price, and the gourmet will tell you how " +
                        "many they are interested in buying for that price.",
                )
                chatNpc(
                    neutral,
                    "If you want a rough idea of how much something will sell for, show it to " +
                        "me and I'll try to estimate.",
                )
                chatNpc(
                    neutral,
                    "But you should also take into account the gourmet's reactions when naming " +
                        "your price. If someone likes the look of a food, they will pay more " +
                        "for it.",
                )
                chatNpc(
                    neutral,
                    "Also remember that the novelty of new foods quickly wears off. If people " +
                        "have been eating a lot of something lately, then they won't pay as " +
                        "much for it.",
                )
                chatNpc(
                    neutral,
                    "If someone especially likes your food then you might even start a craze " +
                        "for it! If there's a food that's in fashion like that, then the " +
                        "gourmets will pay more for it.",
                )
            }
            3 -> {
                chatPlayer(quiz, "Are you selling anything?")
                access.openGeneralStore(npc)
            }
            else -> chatPlayer(neutral, "Goodbye.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Miltog
    private suspend fun Dialogue.miltog(npc: Npc) {
        chatNpc(happy, "Do you want to buy a lamp, surface-dweller?")
        val option =
            choice4(
                "Yes please.",
                1,
                "No thanks.",
                2,
                "Can you fill a lamp with oil please?",
                3,
                "How do the lamps around the city work?",
                4,
            )
        when (option) {
            1 -> {
                chatPlayer(happy, "Yes please.")
                access.openLampShop(npc)
            }
            2 -> {
                chatPlayer(neutral, "No thanks.")
                chatNpc(neutral, "Suit yourself.")
            }
            3 -> {
                chatPlayer(quiz, "Can you fill a lamp with oil please?")
                chatNpc(
                    neutral,
                    "You can do that yourself! Just put some swamp tar in the lamp oil still " +
                        "here and then use the lamp on the still.",
                )
            }
            else -> {
                chatPlayer(quiz, "How do the lamps around the city work?")
                chatNpc(happy, "They work by our very own Dorgeshuun magic!")
                chatNpc(
                    neutral,
                    "Sometimes they don't work very well, though. The light orbs blow and they " +
                        "have to be replaced.",
                )
                chatNpc(
                    neutral,
                    "If you ever see a lamp that's gone out, you can fix it by replacing the " +
                        "light orb. You can make new light orbs out of glass, and use a piece of " +
                        "wire on them to add the filament.",
                )
                chatNpc(
                    neutral,
                    "There's a machine in the south of the city that makes the wire you need.",
                )
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Reldak
    private suspend fun Dialogue.reldak(npc: Npc) {
        chatNpc(happy, "Do you want to buy some leather armour?")
        if (choice2("Yes please.", true, "No thanks.", false)) {
            chatPlayer(happy, "Yes please.")
            access.openLeatherShop(npc)
        } else {
            chatPlayer(neutral, "No thanks.")
            chatNpc(happy, "Have a good day!")
        }
    }

    private data class FoodStall(
        val npc: String,
        val pitch: String,
        val food: String,
        val refusesFullInventory: Boolean = false,
    )

    private companion object {
        const val COINS = "obj.coins"
        const val FOOD_PRICE = 10
        const val LURGON = "npc.dorgesh_market_head"
        const val MILTOG = "npc.dorgesh_female_lamp_seller"
        const val RELDAK = "npc.dorgesh_male_leather_seller"

        val FOOD_STALLS =
            listOf(
                FoodStall(
                    "npc.dorgesh_market_male3",
                    "Frogburger! There's nothing like grilled frog in a bun. Do you want one? " +
                        "Only 10gp!",
                    "obj.dorgesh_frog_burger",
                ),
                FoodStall(
                    "npc.dorgesh_market_female2",
                    "Bat shish kebab! Spicy bat-meat on a stick! How about a bat kebab for 10gp, " +
                        "human?",
                    "obj.dorgesh_bat_shish",
                ),
                FoodStall(
                    "npc.dorgesh_market_male2",
                    "Frogspawn gumbo! Lovely, chewy and wet. Only 10gp a bowl. Do you want some, " +
                        "surface-dweller?",
                    "obj.dorgesh_frog_spawn_gumbo",
                ),
                FoodStall(
                    "npc.dorgesh_market_female1",
                    "Creeespy frogs' legs! Get your creeeespy frogs' legs! You want some crispy " +
                        "frogs' legs? Just 10gp.",
                    "obj.dorgesh_crispy_froglegs",
                    refusesFullInventory = true,
                ),
                FoodStall(
                    "npc.dorgesh_market_male1",
                    "Cave slime soup! Do you want to buy some soup? Only 10 coins a bowl.",
                    "obj.dorgesh_green_gloop_soup",
                ),
                FoodStall(
                    "npc.dorgesh_market_female3",
                    "Wall beast fingers! How about a tasty snack of wall beast fingers? Only 10gp!",
                    "obj.dorgesh_wall_beast_fingers",
                ),
            )
    }
}
