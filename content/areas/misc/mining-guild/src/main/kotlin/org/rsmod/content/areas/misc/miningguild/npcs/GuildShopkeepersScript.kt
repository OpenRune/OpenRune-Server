package org.rsmod.content.areas.misc.miningguild.npcs

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class GuildShopkeepersScript @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(HENDOR) { talk(it.npc) { hendor() } }
        onOpNpc3(HENDOR) { player.openOreShop() }

        onOpNpc1(YARSUL) { talk(it.npc) { yarsul() } }
        onOpNpc3(YARSUL) { player.openPickaxeShop() }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc, conversation: suspend Dialogue.() -> Unit) =
        startDialogue(npc) { conversation() }

    private suspend fun Dialogue.hendor() {
        chatNpc(happy, "Hello there! If you have any ores to trade I'm always buying.")
        while (true) {
            when (
                choice3(
                    "Let's trade.",
                    1,
                    "Why don't you ever restock your shop?",
                    2,
                    "Goodbye.",
                    3,
                )
            ) {
                1 -> {
                    player.openOreShop()
                    return
                }
                2 -> {
                    chatPlayer(quiz, "Why don't you ever restock your shop?")
                    chatNpc(neutral, "The only ores I sell are the ones that are sold to me.")
                    chatNpc(neutral, "Anything else?")
                }
                else -> {
                    chatPlayer(happy, "See you later.")
                    chatNpc(happy, "Until next time.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.yarsul() {
        chatNpc(
            happy,
            "Good day to you, welcome to my pickaxe shop. Are you interested in making a purchase?",
        )
        if (choice2("Yes please.", true, "No thanks.", false)) {
            player.openPickaxeShop()
            return
        }
        chatPlayer(happy, "No thanks.")
        chatNpc(neutral, "Suit yourself. I'll be here if you change your mind.")
    }

    private fun Player.openOreShop() {
        shops.open(
            player = this,
            title = "Hendor's Awesome Ores",
            shopInv = "inv.mguild_oreshop",
            buyPercentage = 70.0,
            sellPercentage = 100.0,
            changePercentage = 2.0,
        )
    }

    private fun Player.openPickaxeShop() {
        shops.open(
            player = this,
            title = "Yarsul's Prodigious Pickaxes",
            shopInv = "inv.mguild_pickaxeshop",
            buyPercentage = 60.0,
            sellPercentage = 100.0,
            changePercentage = 2.0,
        )
    }

    private companion object {
        const val HENDOR = "npc.mguild_oreseller"
        const val YARSUL = "npc.mguild_pickaxeseller"
    }
}
