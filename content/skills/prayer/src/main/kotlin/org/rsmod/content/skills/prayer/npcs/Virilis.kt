package org.rsmod.content.skills.prayer.npcs

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpcU
import org.rsmod.api.table.prayer.PrayerBlessedBoneRow
import org.rsmod.game.entity.Npc
import org.rsmod.game.inv.isType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class Virilis : PluginScript() {

    private val blessableBones = PrayerBlessedBoneRow.all().map { it.bone.internalName }.toSet()

    override fun ScriptContext.startup() {
        onOpNpc1("npc.vm_prayer_uncerter") { startDialogue(it.npc) }
        onOpNpcU("npc.vm_prayer_uncerter") { exchangeBanknote(it.npc, it.invSlot, it.objType) }
    }

    private suspend fun ProtectedAccess.startDialogue(npc: Npc) {
        startDialogue(npc) { standardDialogue() }
    }

    private suspend fun Dialogue.standardDialogue() {
        chatNpcNoTurn(
            neutral,
            "Nilsal. Do you wish me to exchange banknotes for you? I charge only 10 coins for each banknote.",
        )
        optionsDialogue()
    }

    private suspend fun Dialogue.optionsDialogue() {
        when (
            choice4(
                "Yes please.",
                1,
                "Who are you?",
                2,
                "What can I do here?",
                3,
                "No thanks.",
                4,
            )
        ) {
            1 -> {
                chatPlayer(happy, "Yes please.")
                chatNpcNoTurn(neutral, "Then hand me the banknotes you wish me to exchange.")
            }

            2 -> {
                chatPlayer(quiz, "Who are you?")
                chatNpcNoTurn(
                    neutral,
                    "I am Virilis. I can exchange banknotes of bones for real items. Do you want any banknotes converted?",
                )
                optionsDialogue()
            }

            3 -> {
                chatPlayer(quiz, "What can I do here?")
                chatNpcNoTurn(
                    neutral,
                    "I'm glad you've asked! Next to me is an old altar of Ralos, upon which you can ask for bones and wines to be blessed.",
                )
                chatNpcNoTurn(
                    neutral,
                    "You should beware though, Ralos won't hear your prayers if you aren't experienced enough. I wouldn't want you to get caught in the cold just waiting for nothing to happen.",
                )
                chatNpcNoTurn(
                    neutral,
                    "Anyway, once you've had Ralos' blessing, you can break the bones down into shards. Then, along with your blessed wine, you can take them to the libation bowl and sacrifice them in Ralos' honour!",
                )
                chatPlayer(quiz, "Libation bowl?")
                chatNpcNoTurn(neutral, "Yes! You'll find it inside the Teomat, near the library.")
                chatPlayer(quiz, "I see. And what happens if I drink the blessed wine?")
                chatNpcNoTurn(neutral, "The last person to try that ended up in the libation bowl. I do not recommend it.")
                chatNpcNoTurn(neutral, "Anyway, do you want any banknotes converted?")
                optionsDialogue()
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
                chatNpcNoTurn(neutral, "I'm afraid I can only exchange bank notes for real items.")
            }
            return
        }

        val uncert = ocUncert(objType)
        val uncertInternal = RSCM.getReverseMapping(RSCMType.OBJ, uncert.id)
        if (uncertInternal !in blessableBones) {
            startDialogue(npc) {
                chatNpcNoTurn(neutral, "I only exchange bones that you can bless at Ralos' altar.")
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
        if (availableCoins < 10) {
            startDialogue(npc) {
                chatNpcNoTurn(neutral, "I require 10 coins for exchanging each banknote.")
            }
            return
        }

        val maxByCoins = availableCoins / 10
        val maxBySpace = inv.freeSpace()
        val maxExchange = minOf(invObj.count, maxByCoins, maxBySpace)
        val exchangeCount = chooseExchangeAmount(uncert.name, maxExchange, 10) ?: return
        if (exchangeCount <= 0) {
            startDialogue(npc) { chatNpcNoTurn(neutral, "Your inventory is too full.") }
            return
        }

        val exchangeFee = exchangeCount * 10
        val takeFeeAndNote =
            invDel(
                inv = inv,
                type1 = "obj.coins",
                count1 = exchangeFee,
                type2 = objType.internalName,
                count2 = exchangeCount,
            )
        if (takeFeeAndNote.failure) {
            startDialogue(npc) {
                chatNpcNoTurn(neutral, "I require 10 coins for exchanging each banknote.")
            }
            return
        }

        val add = invAdd(inv = inv, type = uncertInternal, count = exchangeCount)
        if (add.failure) {
            return
        }
        objbox(uncertInternal, "Virilis converts your banknote(s).")
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
        val selection = choice4(
            "Exchange $exchange1 (${exchange1 * feePerNote} coins)",
            1,
            "Exchange $exchange5 (${exchange5 * feePerNote} coins)",
            2,
            "Exchange all (${maxExchange * feePerNote} coins)",
            3,
            "Exchange X",
            4,
            title = "Exchanging: $itemName",
        )
        return when (selection) {
            1 -> exchange1
            2 -> exchange5
            3 -> maxExchange
            else -> countDialog("Enter amount:").coerceIn(0, maxExchange)
        }
    }
}
