package org.rsmod.api.combat.formulas.attributes.collector

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.rsmod.game.inv.InvObj
import org.rsmod.utils.bits.getBits

internal fun InvObj?.hasUsableRevenantEther(): Boolean {
    if (this == null) return false
    val bits = ServerCacheManager.getVarObj("varobj.revenant_ether".asRSCM())?.bits ?: return false
    return vars.getBits(bits) > 1_000
}
