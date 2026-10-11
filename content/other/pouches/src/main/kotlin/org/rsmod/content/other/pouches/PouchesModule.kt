package org.rsmod.content.other.pouches

import org.rsmod.api.death.PlayerDeathDropHook
import org.rsmod.api.player.hook.PlayerObjTakeStorageHook
import org.rsmod.content.other.pouches.lootingbag.LootingBagTakeHook
import org.rsmod.plugin.module.PluginModule

class PouchesModule : PluginModule() {
    override fun bind() {
        addSetBinding<org.rsmod.content.interfaces.bank.BankDepositStorage>(PouchBankDeposits::class.java)
        addSetBinding<org.rsmod.api.inv.storage.PlayerItemStorageHook>(org.rsmod.content.other.pouches.runepouch.RunePouchIncomingHook::class.java)
        addSetBinding<PlayerDeathDropHook>(PouchDeathHook::class.java)
        addSetBinding<PlayerObjTakeStorageHook>(LootingBagTakeHook::class.java)
        addSetBinding<PlayerObjTakeStorageHook>(org.rsmod.content.other.pouches.runepouch.RunePouchPickupLocks::class.java)
    }
}
