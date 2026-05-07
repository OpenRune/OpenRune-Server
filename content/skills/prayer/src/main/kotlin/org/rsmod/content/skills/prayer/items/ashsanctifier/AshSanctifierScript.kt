package org.rsmod.content.skills.prayer.items.ashsanctifier

import jakarta.inject.Inject
import org.rsmod.api.config.constants
import org.rsmod.api.obj.charges.ObjChargeManager
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpHeldU
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.isType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class AshSanctifierScript @Inject constructor(private val charges: ObjChargeManager) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld2("obj.ash_sanctifier") { checkCharges(it.slot) }
        onOpHeld3("obj.ash_sanctifier") { toggleActivity() }
        onOpHeld4("obj.ash_sanctifier") { uncharge(it.slot) }

        onOpHeldU("obj.ash_sanctifier", "obj.deathrune") { ev ->
            tryCharge(ev.firstSlot, ev.secondSlot)
        }
    }

    private fun ProtectedAccess.tryCharge(sanctifierSlot: Int, runeSlot: Int) {
        if (!player.hasKourendKebosHardDiaryComplete()) {
            mes("You need to complete the Hard Kourend & Kebos Diary before you can use the ash sanctifier.")
            return
        }
        val newTotal = player.chargeAshSanctifierWithDeathRunes(charges, inv, sanctifierSlot, runeSlot) ?: run {
            val sanctifier = inv[sanctifierSlot]?.takeIf { it.isType("obj.ash_sanctifier") }
            val curr = if (sanctifier != null) {
                charges.getCharges(sanctifier, "varbit.charges_ash_sanctifier_quantity")
            } else {
                0
            }
            when {
                sanctifier != null && curr >= Int.MAX_VALUE -> mes("Your ash sanctifier cannot hold any more charges.")
                sanctifier != null && (Int.MAX_VALUE - curr) < 10 -> mes("Your ash sanctifier cannot hold enough charges for a whole death rune.")
            }
            return
        }
        if (player.ashSanctifierActivityEnabled) {
            mes("The ash sanctifier has $newTotal charges. It is active and ready to purify demonic ashes.")
        } else {
            mes("The ash sanctifier has $newTotal charges. It has been deactivated, and will not purify ashes now.",)
        }
    }

    private fun ProtectedAccess.checkCharges(slot: Int) {
        val obj = inv[slot] ?: return
        val count = charges.getCharges(obj, "varbit.charges_ash_sanctifier_quantity")
        when {
            count == 0 -> mes("The ash sanctifier has no charges. It can be charged with death runes.")
            player.ashSanctifierActivityEnabled -> mes("The ash sanctifier has $count charges. It is active and ready to purify demonic ashes.")
            else -> mes("The ash sanctifier has $count charges. It has been deactivated, and will not purify ashes now.",)
        }
    }

    private fun ProtectedAccess.toggleActivity() {
        if (!player.hasKourendKebosHardDiaryComplete()) {
            mes("You need to complete the Hard Kourend & Kebos Diary before you can use the ash sanctifier.")
            return
        }
        player.ashSanctifierActivityEnabled = !player.ashSanctifierActivityEnabled
        mes(if (player.ashSanctifierActivityEnabled) {
            "The ash sanctifier is active and ready to purify demonic ashes."
        } else {
            "The ash sanctifier has been deactivated, and will not purify ashes now."
        })
    }

    private suspend fun ProtectedAccess.uncharge(slot: Int) {
        when (val result = player.tryUnchargeAshSanctifier(charges, inv, slot)) {
            AshSanctifierUnchargeResult.WrongItem -> return
            AshSanctifierUnchargeResult.NoCharges -> mes("The ash sanctifier has no charges.")
            AshSanctifierUnchargeResult.CannotRedeemDeathRunes -> mes("The ash sanctifier does not have enough charges for you to remove any death runes.",)
            AshSanctifierUnchargeResult.NoInvSpace -> mes(constants.dm_invspace)
            is AshSanctifierUnchargeResult.Success ->
                if (result.remainingCharges == 0) {
                    doubleobjbox(
                        "obj.ash_sanctifier",
                        "obj.deathrune",
                        "You remove all the charges from the ash sanctifier.",
                    )
                } else {
                    doubleobjbox(
                        "obj.ash_sanctifier",
                        "obj.deathrune",
                        "The ash sanctifier has ${result.remainingCharges} charges left.",
                    )
                }
        }
    }

    companion object {
        fun Player.hasKourendKebosHardDiaryComplete(): Boolean = true
        fun Player.hasKourendKebosEliteDiaryComplete(): Boolean = true
        var Player.ashSanctifierActivityEnabled by boolVarBit("varbit.ash_sanctifier_activity_enabled")
    }

}
