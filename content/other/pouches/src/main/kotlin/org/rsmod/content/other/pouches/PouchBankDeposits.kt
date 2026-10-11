package org.rsmod.content.other.pouches

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.interfaces.bank.BankDepositStorage
import org.rsmod.content.interfaces.bank.scripts.BankInvScript
import org.rsmod.content.other.pouches.lootingbag.*
import org.rsmod.content.other.pouches.runepouch.*

internal class PouchBankDeposits @Inject constructor() : BankDepositStorage {
    override fun depositContainers(access: ProtectedAccess, bank: BankInvScript): Long = with(access) {
        var moved = 0L
        val slots = player.runePouchSlots()
        if (slots != null) {
            val contents = player.readRunePouch(slots)
            for (slot in 0 until slots) {
                val rune = RunePouchRunes.byCompact(contents.types[slot]) ?: continue
                val n = with(bank) { depositStoredStack(rune.id, contents.counts[slot]) }
                if (n > 0) { contents.remove(slot, n); moved += n }
            }
            player.writeRunePouch(contents)
        }
        if (player.hasLootingBag()) {
            val bag = player.lootingBag
            for (slot in bag.indices.reversed()) {
                val original = bag[slot] ?: continue
                val n = with(bank) { depositStoredItem(original, original.count) }
                if (n > 0) { bag[slot] = if (n == original.count) null else original.copy(count = original.count - n); moved += n }
            }
            player.saveLootingBag()
        }
        moved
    }
}
