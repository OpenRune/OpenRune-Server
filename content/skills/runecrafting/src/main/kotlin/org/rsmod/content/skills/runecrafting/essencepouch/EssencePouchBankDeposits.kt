package org.rsmod.content.skills.runecrafting.essencepouch

import dev.openrune.rscm.RSCM.asRSCM
import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.interfaces.bank.BankDepositStorage
import org.rsmod.content.interfaces.bank.scripts.BankInvScript

internal class EssencePouchBankDeposits @Inject constructor() : BankDepositStorage {
    override fun depositContainers(access: ProtectedAccess, bank: BankInvScript): Long = with(access) {
        var moved = 0L
        for (tier in EssencePouch.Tier.entries) {
            if (!EssencePouch.ownsTier(player, tier)) continue
            val item = EssencePouch.storedEssenceTypeName(player, tier) ?: continue
            val n = with(bank) { depositStoredStack(item.asRSCM(), EssencePouch.storedAmount(player, tier)) }
            if (n > 0) { EssencePouch.removeStoredFromTier(player, tier, n); moved += n }
        }
        moved
    }
}
