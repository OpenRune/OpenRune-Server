package org.rsmod.content.skills.prayer.npcs

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpcU
import org.rsmod.content.skills.prayer.PrayerBuryEvents
import org.rsmod.game.entity.Npc
import org.rsmod.game.inv.isType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ElderChaosDruid : PluginScript() {

    private val sacrificeableBones = PrayerBuryEvents.Companion.bones.filterNot { it.ashes }.map { it.item.internalName }.toSet()

    override fun ScriptContext.startup() {
        onOpNpc1("npc.wild_chaos_uncerter") { startDialogue(it.npc) }
        onOpNpcU("npc.wild_chaos_uncerter") { exchangeBanknote(it.npc, it.invSlot, it.objType) }
    }

    private suspend fun ProtectedAccess.startDialogue(npc: Npc) {
        startDialogue(npc) {
            chatNpcNoTurn(
                neutral,
                "Hello, and welcome to The Dark Lord's temple. Zamorak will reward those who offer the bones of the vanquished on his altar.",
            )
            chatNpcNoTurn(
                neutral,
                "I can exchange banknotes for bones, should you require such a service.",
            )
            chatNpcNoTurn(
                neutral,
                "My services, however, are not free. I charge 50 coins per banknote.",
            )
            options()
        }
    }

    private suspend fun Dialogue.options() {
        when (choice3("Yes please.", 1, "Who are you?", 2, "No thanks.", 3)) {
            1 -> chatNpcNoTurn(neutral, "Hand me the banknotes you wish me to exchange.")
            2 -> {
                chatNpcNoTurn(
                    neutral,
                    "That is not important. I am here to facilitate your offerings to The Dark Lord. Would you like any bone banknotes exchanged?",
                )
                when (choice2("Yes please.", 1, "No thanks.", 2)) {
                    1 -> chatNpcNoTurn(neutral, "Hand me the banknotes you wish me to exchange.")
                    else -> chatPlayer(neutral, "No thanks.")
                }
            }

            else -> chatPlayer(neutral, "No thanks.")
        }
    }

    private suspend fun ProtectedAccess.exchangeBanknote(
        npc: Npc,
        invSlot: Int,
        objType: ItemServerType,
    ) {
        if (!objType.isCert) {
            startDialogue(npc) {
                chatNpcNoTurn(neutral, "I exchange banknotes for real items. That's not a banknote.")
            }
            return
        }

        val uncert = ocUncert(objType)
        val uncertInternal = RSCM.getReverseMapping(RSCMType.OBJ, uncert.id)
        if (uncertInternal !in sacrificeableBones) {
            startDialogue(npc) {
                chatNpcNoTurn(
                    neutral,
                    "I only exchange items that can be offered as a sacrifice to The Dark Lord's altar.",
                )
            }
            return
        }

        val invObj = inv[invSlot]
        if (invObj == null || !invObj.isType(objType)) {
            return
        }

        if (inv.isFull()) {
            startDialogue(npc) { chatNpcNoTurn(neutral, "Your inventory is too full.") }
            return
        }

        val availableCoins = inv.count("obj.coins")
        if (availableCoins < 50) {
            startDialogue(npc) {
                chatNpcNoTurn(neutral, "I charge 50 coins for exchanging each banknote.")
            }
            return
        }

        val maxByCoins = availableCoins / 50
        val maxBySpace = inv.freeSpace()
        val maxExchange = minOf(invObj.count, maxByCoins, maxBySpace)
        val exchangeCount = chooseExchangeAmount(uncert.name, maxExchange, 50) ?: return
        if (exchangeCount <= 0) {
            startDialogue(npc) { chatNpcNoTurn(neutral, "Your inventory is too full.") }
            return
        }

        val exchangeFee = exchangeCount * 50
        val takeFeeAndNote = invDel(
            inv = inv,
            type1 = "obj.coins",
            count1 = exchangeFee,
            type2 = objType.internalName,
            count2 = exchangeCount
        )
        if (takeFeeAndNote.failure) {
            startDialogue(npc) {
                chatNpcNoTurn(neutral, "I charge 50 coins for exchanging each banknote.")
            }
            return
        }

        val add = invAdd(inv = inv, type = uncertInternal, count = exchangeCount)
        if (add.failure) {
            return
        }
    }

    private suspend fun ProtectedAccess.chooseExchangeAmount(
        itemName: String,
        maxExchange: Int,
        feePerNote: Int,
    ): Int? {
        if (maxExchange <= 0) {
            return null
        }
        val exchange1 = minOf(1, maxExchange)
        val exchange5 = minOf(5, maxExchange)
        val exchangeAll = maxExchange
        val selection = choice4(
            "Exchange $exchange1 (${exchange1 * feePerNote} coins)",
            1,
            "Exchange $exchange5 (${exchange5 * feePerNote} coins)",
            2,
            "Exchange all (${exchangeAll * feePerNote} coins)",
            3,
            "Exchange X",
            4,
            title = "Exchanging: $itemName",
        )
        return when (selection) {
            1 -> exchange1
            2 -> exchange5
            3 -> exchangeAll
            else -> countDialog("Enter amount:").coerceIn(0, maxExchange)
        }
    }

}
