package org.rsmod.content.areas.zeah.kebos.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Leke_quo_Keran
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class MountKaruulmWeaponShop @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.kebos_spear_salesmen") { startDialogue(it.npc) { leke(it.npc) } }
        onOpNpc3("npc.kebos_spear_salesmen") { player.openWeaponShop(it.npc) }
    }

    private fun Player.openWeaponShop(npc: Npc) {
        shops.open(this, npc, "Mount Karuulm Weapon Shop", "inv.kebos_weapon_shop")
    }

    private suspend fun Dialogue.leke(npc: Npc) {
        chatNpc(
            neutral,
            "Do you wish to serve the balance? You'll need weapons if you do. These, I can " +
                "provide.",
        )
        while (true) {
            val option =
                choice3(
                    "Let's see what you have.",
                    1,
                    "Do you make a lot of money selling weapons?",
                    2,
                    "I'm good thanks.",
                    3,
                )
            when (option) {
                1 -> {
                    chatPlayer(neutral, "Let's see what you have.")
                    return player.openWeaponShop(npc)
                }
                2 -> money()
                else -> return chatPlayer(neutral, "I'm good thanks.")
            }
        }
    }

    private suspend fun Dialogue.money() {
        chatPlayer(quiz, "Do you make a lot of money selling weapons?")
        chatNpc(
            neutral,
            "To be a Kahlith is to abandon all attachment. Both physical and emotional. I have " +
                "no use for money.",
        )
        chatPlayer(quiz, "So why don't you give out the weapons for free?")
        chatNpc(
            neutral,
            "While we care not for money, your kind still do. While a Kahlith will do anything " +
                "for the balance, your kind are not so generous.",
        )
        chatPlayer(quiz, "So you use the money to pay us for things?")
        chatNpc(
            neutral,
            "Exactly. For us to serve the balance, we must trade with your people. That is what " +
                "the money is for.",
        )
        chatNpc(neutral, "Anyway, would you like to purchase some weapons?")
    }
}
