package org.rsmod.content.skills.smithing.coalbag

import dev.openrune.rscm.RSCM.asRSCM
import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.interfaces.bank.BankDepositStorage
import org.rsmod.content.interfaces.bank.scripts.BankInvScript

internal class CoalBagBankDeposits @Inject constructor() : BankDepositStorage {
    override fun depositContainers(access: ProtectedAccess, bank: BankInvScript): Long = with(access) {
        if (!CoalBag.owns(player)) return 0
        val moved = with(bank) { depositStoredStack("obj.coal".asRSCM(), CoalBag.storedAmount(player)) }
        if (moved > 0) CoalBag.removeStored(player, moved)
        moved.toLong()
    }
}
