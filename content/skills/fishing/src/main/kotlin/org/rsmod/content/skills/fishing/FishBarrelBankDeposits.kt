package org.rsmod.content.skills.fishing

import dev.openrune.rscm.RSCM.asRSCM
import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.interfaces.bank.BankDepositStorage
import org.rsmod.content.interfaces.bank.scripts.BankInvScript

internal class FishBarrelBankDeposits @Inject constructor() : BankDepositStorage {
    override fun depositContainers(access: ProtectedAccess, bank: BankInvScript): Long = with(access) {
        if (!FishBarrelContents.owns(player)) return 0
        var moved = 0L
        for ((item, count) in FishBarrelContents.contents(player).toMap()) {
            val n = with(bank) { depositStoredStack(item.asRSCM(), count) }
            if (n > 0) { FishBarrelContents.update(player, item, -n); moved += n }
        }
        moved
    }
}
