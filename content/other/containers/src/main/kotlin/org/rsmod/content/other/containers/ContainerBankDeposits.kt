package org.rsmod.content.other.containers

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.interfaces.bank.BankDepositStorage
import org.rsmod.content.interfaces.bank.scripts.BankInvScript

internal class ContainerBankDeposits @Inject constructor(private val store: ContainerStore, private val barrel: ChuggingBarrel, private val potions: PotionStorage) : BankDepositStorage {
    override fun depositContainers(access: ProtectedAccess, bank: BankInvScript): Long = with(access) {
        var moved = 0L
        val families = setOf("herb_sack", "low_gems", "gem_bag", "gem_sack", "reagent_pouch", "seed_box", "plank_sack", "log_basket", "fur_pouch", "meat_pouch")
        for (spec in ContainerCatalog.all.filter { it.family in families && store.owns(player, it) }.distinctBy { it.family }) {
            for ((key, count) in store.contents(player, spec).toMap()) {
                val id = key.toInt(); val deposited = with(bank) { depositStoredStack(id, count) }
                if (deposited > 0) { store.update(player, spec, id, -deposited); moved += deposited }
            }
        }
        if (barrel.owns(player)) with(BarrelLoadouts(barrel, bank, potions)) { for (slot in 0..4) moved += deposit(slot, Int.MAX_VALUE) }
        moved
    }
}
