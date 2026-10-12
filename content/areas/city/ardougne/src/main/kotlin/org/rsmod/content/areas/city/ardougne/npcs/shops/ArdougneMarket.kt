package org.rsmod.content.areas.city.ardougne.npcs.shops

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
// TODO(content): [skill:thieving] overhead lines and refusing to trade after a stall theft
// TODO(content): [quest:song_of_the_elves] new taxes dialogue
class ArdougneMarket @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        for ((baker, inv) in BAKERS) {
            onOpNpc1(baker) { startDialogue(it.npc) { baker(it.npc, inv) } }
            onOpNpc3(baker) { openStall(it.npc, BAKER_STALL, inv) }
        }
        onOpNpc1(FUR_TRADER) { startDialogue(it.npc) { furTrader(it.npc) } }
        onOpNpc3(FUR_TRADER) { openStall(it.npc, FUR_STALL, FUR_INV) }
        onOpNpc1(GEM_MERCHANT) { startDialogue(it.npc) { gemMerchant(it.npc) } }
        onOpNpc3(GEM_MERCHANT) { openStall(it.npc, GEM_STALL, GEM_INV) }
        onOpNpc1(SILVER_MERCHANT) { startDialogue(it.npc) { silverMerchant(it.npc) } }
        onOpNpc3(SILVER_MERCHANT) { openStall(it.npc, SILVER_STALL, SILVER_INV) }
        onOpNpc1(SPICE_SELLER) { startDialogue(it.npc) { spiceSeller(it.npc) } }
        onOpNpc3(SPICE_SELLER) { openStall(it.npc, SPICE_STALL, SPICE_INV) }
        onOpNpc1(ZENESHA) { startDialogue(it.npc) { zenesha(it.npc) } }
        onOpNpc3(ZENESHA) { openStall(it.npc, ZENESHA_SHOP, ZENESHA_INV) }
    }

    private fun ProtectedAccess.openStall(npc: Npc, title: String, inv: String) {
        shops.open(player, npc, title, inv)
    }

    private fun Dialogue.openStall(npc: Npc, title: String, inv: String) {
        shops.open(player, npc, title, inv)
    }

    // https://oldschool.runescape.wiki/w/Transcript:Baker
    private suspend fun Dialogue.baker(npc: Npc, inv: String) {
        val title = if (access.isBodyTypeA()) "Sir" else "Madame"
        chatNpc(
            happy,
            "Good day $title. Would you like some nice freshly baked bread? Or perhaps a nice " +
                "piece of cake?",
        )
        if (choice2("Let's see what you have.", true, "No thank you.", false)) {
            openStall(npc, BAKER_STALL, inv)
            return
        }
        chatPlayer(neutral, "No thank you.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Fur_trader_(East_Ardougne)
    private suspend fun Dialogue.furTrader(npc: Npc) {
        chatNpc(happy, "Would you like to trade in fur?")
        yesOrNoThanks(npc, FUR_STALL, FUR_INV)
    }

    // https://oldschool.runescape.wiki/w/Transcript:Gem_merchant
    private suspend fun Dialogue.gemMerchant(npc: Npc) {
        chatNpc(happy, "Here, look at my lovely gems.")
        openStall(npc, GEM_STALL, GEM_INV)
    }

    // https://oldschool.runescape.wiki/w/Transcript:Silver_merchant (before Making History)
    // TODO(content): [quest:making_history] after-quest dialogue and replacing the enchanted key
    private suspend fun Dialogue.silverMerchant(npc: Npc) {
        chatNpc(happy, "Silver! Silver! Best prices for buying and selling in all Kandarin!")
        if (choice2("Yes please.", true, "No thank you.", false)) {
            chatPlayer(happy, "Yes please.")
            openStall(npc, SILVER_STALL, SILVER_INV)
            return
        }
        chatPlayer(neutral, "No thank you.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Spice_seller
    private suspend fun Dialogue.spiceSeller(npc: Npc) {
        chatNpc(happy, "Are you interested in buying or selling spice?")
        yesOrNoThanks(npc, SPICE_STALL, SPICE_INV)
    }

    private suspend fun Dialogue.yesOrNoThanks(npc: Npc, title: String, inv: String) {
        if (choice2("Yes please.", true, "No thanks.", false)) {
            openStall(npc, title, inv)
            return
        }
        chatPlayer(neutral, "No thanks.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Zenesha
    private suspend fun Dialogue.zenesha(npc: Npc) {
        chatNpc(happy, "Hello there! I sell plate mail bodies, are you interested?")
        if (choice2("I'm interested.", true, "Sorry, I'm not interested.", false)) {
            openStall(npc, ZENESHA_SHOP, ZENESHA_INV)
            return
        }
        chatPlayer(neutral, "Sorry, I'm not interested.")
    }

    private companion object {
        const val BAKER_STALL = "Ardougne Baker's Stall."
        const val FUR_STALL = "Ardougne Fur Stall."
        const val GEM_STALL = "Ardougne Gem Stall."
        const val SILVER_STALL = "Ardougne Silver Stall."
        const val SPICE_STALL = "Ardougne Spice Stall."
        const val ZENESHA_SHOP = "Zenesha's Plate Mail Body Shop."

        const val FUR_TRADER = "npc.fur_merchant_ardougne"
        const val GEM_MERCHANT = "npc.gem_merchant_ardougne"
        const val SILVER_MERCHANT = "npc.silver_merchant_ardougne"
        const val SPICE_SELLER = "npc.spice_merchant_ardougne"
        const val ZENESHA = "npc.zenesha"

        const val FUR_INV = "inv.viking_furshop"
        const val GEM_INV = "inv.ardougnegemstall"
        const val SILVER_INV = "inv.silverstall"
        const val SPICE_INV = "inv.spicestall"
        const val ZENESHA_INV = "inv.topshop"

        // TODO(content): [verify:in_game] which baker opens inv.bakery and which inv.bakery2
        val BAKERS =
            mapOf(
                "npc.baker_merchant_ardougne" to "inv.bakery",
                "npc.baker_merchant_ardougne2" to "inv.bakery2",
            )
    }
}
