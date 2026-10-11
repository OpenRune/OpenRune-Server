package org.rsmod.content.skills.fishing

import org.rsmod.api.stats.levelmod.InvisibleLevelMod
import org.rsmod.api.stats.xpmod.XpMod
import org.rsmod.plugin.module.PluginModule

class FishingModule : PluginModule() {
    override fun bind() {
        addSetBinding<org.rsmod.content.interfaces.bank.BankDepositStorage>(FishBarrelBankDeposits::class.java)
        addSetBinding<org.rsmod.api.inv.storage.PlayerItemStorageHook>(FishBarrelStorageHook::class.java)
        addSetBinding<org.rsmod.api.death.PlayerDeathDropHook>(FishBarrelDeathHook::class.java)
        addSetBinding<InvisibleLevelMod>(FishingLevelBoosts::class.java)
        addSetBinding<XpMod>(SpiritAnglerWornSetXpModifiers::class.java)
    }
}
