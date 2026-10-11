package org.rsmod.content.interfaces.bank

import dev.openrune.types.ItemServerType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.interfaces.bank.scripts.BankInvScript
import org.rsmod.events.SuspendEvent
import org.rsmod.game.inv.InvObj

/** Native bank item Op9 exposes Configure-Charges when param 2257 names a charge dbrow. */
class BankChargeConfigure(val slot: Int, val item: InvObj, val type: ItemServerType, val script: BankInvScript) : SuspendEvent<ProtectedAccess> {
    override val id: Long = type.id.toLong()
    companion object { const val NATIVE_CHARGE_DBROW_PARAM = 2257 }
}
