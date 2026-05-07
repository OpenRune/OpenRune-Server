package org.rsmod.content.skills.prayer.items.bonecrusher

import jakarta.inject.Inject
import org.rsmod.api.config.constants
import org.rsmod.api.obj.charges.ObjChargeManager
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpHeldU
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class BonecrusherScript @Inject constructor(private val charges: ObjChargeManager) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld2("obj.bonecrusher") { checkCharges(it.slot) }
        onOpHeld3("obj.bonecrusher") { toggleActivity() }
        onOpHeld4("obj.bonecrusher") { uncharge(it.slot) }

        onOpHeldU("obj.bonecrusher", "obj.ectotoken") { ev ->
            val newTotal = player.chargeCrusherItemWithEcto(charges, ev.firstSlot, ev.secondSlot) ?: return@onOpHeldU
            if (player.isBonecrusherActivityEnabled()) {
                mes("The bonecrusher has $newTotal charges. It is active and ready to crush bones.")
            } else {
                mes("The bonecrusher has $newTotal charges. It has been deactivated, and will not crush bones now.")
            }
        }
    }

    private fun ProtectedAccess.checkCharges(slot: Int) {
        val obj = inv[slot] ?: return
        val charges = this@BonecrusherScript.charges.getCharges(obj, "varbit.charges_bonecrusher_quantity")
        when {
            charges == 0 -> mes("The bonecrusher has no charges. It can be charged with ectotokens.")
            player.isBonecrusherActivityEnabled() -> mes("The bonecrusher has $charges charges. It is active and ready to crush bones.")
            else -> mes("The bonecrusher has $charges charges. It has been deactivated, and will not crush bones now.",)
        }
    }

    private fun ProtectedAccess.toggleActivity() {
        val next = !player.isBonecrusherActivityEnabled()
        player.setBonecrusherActivity(next)
        mes(if (next) {
            "The bonecrusher is active and ready to crush bones."
        } else {
            "The bonecrusher has been deactivated, and will not crush bones now."
        })
    }

    private suspend fun ProtectedAccess.uncharge(slot: Int) {
        when (val result = player.tryUnchargeBonecrusher(charges, inv, slot, "obj.bonecrusher")) {
            BonecrusherUnchargeResult.WrongItem -> return
            BonecrusherUnchargeResult.NoCharges -> mes("The bonecrusher has no charges.")
            BonecrusherUnchargeResult.CannotRedeemEcto -> mes("The bonecrusher does not have enough charges for you to remove any ectotokens.",)
            BonecrusherUnchargeResult.NoInvSpace -> mes(constants.dm_invspace)
            is BonecrusherUnchargeResult.Success ->
                if (result.remainingCharges == 0) {
                    doubleobjbox(
                        "obj.bonecrusher",
                        "obj.ectotoken",
                        "You remove all the charges from the bonecrusher.",
                    )
                } else {
                    doubleobjbox(
                        "obj.bonecrusher",
                        "obj.ectotoken",
                        "The bonecrusher has ${result.remainingCharges} charges left.",
                    )
                }
        }
    }
}
