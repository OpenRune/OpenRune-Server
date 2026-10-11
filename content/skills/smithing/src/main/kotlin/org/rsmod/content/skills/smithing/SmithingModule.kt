package org.rsmod.content.skills.smithing

import org.rsmod.api.death.PlayerDeathDropHook
import org.rsmod.api.inv.storage.PlayerItemStorageHook
import org.rsmod.api.stats.xpmod.XpMod
import org.rsmod.content.skills.smithing.coalbag.CoalBagDeathHook
import org.rsmod.content.skills.smithing.coalbag.CoalBagStorageHook
import org.rsmod.plugin.module.PluginModule

public class SmithingModule : PluginModule() {
    override fun bind() {
        addSetBinding<org.rsmod.content.interfaces.bank.BankDepositStorage>(org.rsmod.content.skills.smithing.coalbag.CoalBagBankDeposits::class.java)
        addSetBinding<PlayerItemStorageHook>(CoalBagStorageHook::class.java)
        addSetBinding<PlayerDeathDropHook>(CoalBagDeathHook::class.java)
        addSetBinding<XpMod>(SmithingUniformXpModifiers::class.java)
    }
}
