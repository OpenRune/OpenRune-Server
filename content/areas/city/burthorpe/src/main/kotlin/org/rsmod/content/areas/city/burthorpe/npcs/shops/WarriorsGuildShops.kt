package org.rsmod.content.areas.city.burthorpe.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class WarriorsGuildShops @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.warguild_armour_shopkeeper") { startDialogue(it.npc) { anton(it.npc) } }
        onOpNpc3("npc.warguild_armour_shopkeeper") { player.openArmoury(it.npc) }
        onOpNpc1("npc.warguild_food_shopkeeper") { startDialogue(it.npc) { lidio(it.npc) } }
        onOpNpc3("npc.warguild_food_shopkeeper") { player.openFoodShop(it.npc) }
        onOpNpc1("npc.warguild_potion_shopkeeper") { startDialogue(it.npc) { lilly(it.npc) } }
        onOpNpc3("npc.warguild_potion_shopkeeper") { player.openPotionShop(it.npc) }
    }

    private fun Player.openArmoury(npc: Npc) {
        shops.open(this, npc, "Warrior Guild Armoury", "inv.warguild_armour_shop")
    }

    private fun Player.openFoodShop(npc: Npc) {
        shops.open(this, npc, "Warrior Guild Food Shop", "inv.warguild_food_shop")
    }

    private fun Player.openPotionShop(npc: Npc) {
        shops.open(this, npc, "Warrior Guild Potion Shop", "inv.warguild_potion_shop")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Anton
    private suspend fun Dialogue.anton(npc: Npc) {
        chatNpc(happy, "Ahhh, hello there. How can I help?")
        chatPlayer(neutral, "Looks like you have a good selection of weapons around here...")
        chatNpc(
            happy,
            "Indeed so, specially imported from the finest smiths around the lands, take a look " +
                "at my wares.",
        )
        player.openArmoury(npc)
    }

    // https://oldschool.runescape.wiki/w/Transcript:Lidio
    private suspend fun Dialogue.lidio(npc: Npc) {
        chatNpc(happy, "Greetings warrior, how can I fill your stomach today?")
        chatPlayer(happy, "With food preferably.")
        player.openFoodShop(npc)
    }

    // https://oldschool.runescape.wiki/w/Transcript:Lilly
    // TODO(content): [verify:in_game] Lilly's wave animation after "Bye!"
    private suspend fun Dialogue.lilly(npc: Npc) {
        chatNpc(drunk, "Uh..... hi... didn't see you there. Can.... I help?")
        chatPlayer(quiz, "Umm... do you sell potions?")
        chatNpc(drunk, "Erm... yes. When I'm not drinking them.")
        val option =
            choice3(
                "I'd like to see what you have for sale.",
                1,
                "That's a pretty wall hanging.",
                2,
                "Bye!",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(happy, "I'd like to see what you have for sale.")
                chatNpc(drunk, "Of course...")
                player.openPotionShop(npc)
            }
            2 -> {
                chatPlayer(happy, "That's a pretty wall hanging.")
                chatNpc(happy, "Do you think so? I made it myself.")
                chatPlayer(quiz, "Really? Is that why there's all this cloth and dye around?")
                chatNpc(drunk, "Yes, it's a hobby of mine when I'm.... relaxing.")
            }
            3 -> {
                chatPlayer(happy, "Bye!")
                chatNpc(happy, "Have fun and come back soon!")
            }
        }
    }
}
