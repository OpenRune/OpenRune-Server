package org.rsmod.content.skills.prayer.items.bonecrusher

import dev.openrune.rscm.RSCMType
import dev.openrune.rscm.RSCM.asRSCM
import jakarta.inject.Inject
import org.rsmod.api.config.constants
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpHeld5
import org.rsmod.api.script.onOpHeldU
import org.rsmod.game.inv.isType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class BonecrusherNecklaceScript @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld3("obj.bonecrusher_necklace") { toggleActivity() }
        onOpHeld4("obj.bonecrusher_necklace") { checkOrUnchargeMenu(it.slot) }
        onOpHeld5("obj.bonecrusher_necklace") { dismantleNecklace(it.slot) }

        onOpHeldU("obj.bonecrusher_necklace", "obj.ectotoken") { ev ->
            val newTotal = player.chargeCrusherItemWithEcto(ev.firstSlot, ev.secondSlot) ?: return@onOpHeldU
            if (player.isBonecrusherActivityEnabled()) {
                mes("The bonecrusher necklace has $newTotal charges. It is active and ready to crush bones.")
            } else {
                mes("The bonecrusher necklace has $newTotal charges. It has been deactivated, and will not crush bones now.")
            }
        }
    }

    private suspend fun ProtectedAccess.checkOrUnchargeMenu(slot: Int) {
        when (
            choice2(
                "Check charges.",
                CheckUnchargeChoice.Check,
                "Uncharge.",
                CheckUnchargeChoice.Uncharge,
                title = "What would you like to do?",
            )
        ) {
            CheckUnchargeChoice.Check -> checkCharges(slot)
            CheckUnchargeChoice.Uncharge -> uncharge(slot)
        }
    }

    private fun ProtectedAccess.checkCharges(slot: Int) {
        inv[slot] ?: return
        val charges = bonecrusherCharges
        when {
            charges == 0 -> mes("The bonecrusher necklace has no charges. It can be charged with ectotokens.")
            player.isBonecrusherActivityEnabled() -> mes("The bonecrusher necklace has $charges charges. It is active and ready to crush bones.")
            else -> mes("The bonecrusher necklace has $charges charges. It has been deactivated, and will not crush bones now.")
        }
    }

    private fun ProtectedAccess.toggleActivity() {
        val next = !player.isBonecrusherActivityEnabled()
        player.setBonecrusherActivity(next)
        mes(if (next) {
                "The bonecrusher necklace is active and ready to crush bones."
            } else {
                "The bonecrusher necklace has been deactivated, and will not crush bones now."
            },
        )
    }

    private suspend fun ProtectedAccess.uncharge(slot: Int) {
        when (val result = player.tryUnchargeBonecrusher(inv, slot, "obj.bonecrusher_necklace")) {
            BonecrusherUnchargeResult.WrongItem -> return
            BonecrusherUnchargeResult.NoCharges -> mes("The bonecrusher necklace has no charges.")
            BonecrusherUnchargeResult.CannotRedeemEcto ->
                mes("The bonecrusher necklace does not have enough charges for you to remove any ectotokens.")
            BonecrusherUnchargeResult.NoInvSpace -> mes(constants.dm_invspace)
            is BonecrusherUnchargeResult.Success ->
                if (result.remainingCharges == 0) {
                    doubleobjbox(
                        "obj.bonecrusher_necklace",
                        "obj.ectotoken",
                        "You remove all the charges from the bonecrusher necklace.",
                    )
                } else {
                    doubleobjbox(
                        "obj.bonecrusher_necklace",
                        "obj.ectotoken",
                        "The bonecrusher necklace has ${result.remainingCharges} charges left.",
                    )
                }
        }
    }

    private suspend fun ProtectedAccess.dismantleNecklace(slot: Int) {
        val neck = inv[slot]?.takeIf { it.isType("obj.bonecrusher_necklace") } ?: return
        if (inv.freeSpace() < 2) {
            mes(constants.dm_invspace)
            return
        }
        val proceed =
            choice2(
                "Yes, dismantle it.",
                true,
                "No.",
                false,
                title =
                    "Dismantle the bonecrusher necklace? You will receive the bonecrusher, hydra tail and dragonbone necklace. Charges stay on the bonecrusher.",
            )
        if (!proceed) {
            return
        }
        val preservedVars = neck.vars
        val result =
            player.invTransaction(inv) {
                val t = select(inv)
                delete {
                    from = t
                    obj = neck.id
                    strictCount = 1
                    strictSlot = slot
                }
                insert {
                    into = t
                    obj = "obj.bonecrusher".asRSCM(RSCMType.OBJ)
                    strictCount = 1
                    vars = preservedVars
                }
                insert {
                    into = t
                    obj = "obj.hydra_tail".asRSCM(RSCMType.OBJ)
                    strictCount = 1
                }
                insert {
                    into = t
                    obj = "obj.dragonbone_necklace".asRSCM(RSCMType.OBJ)
                    strictCount = 1
                }
            }
        if (!result.success) {
            mes(constants.dm_invspace)
            return
        }
        mes("You dismantle the bonecrusher necklace.")
    }

    private enum class CheckUnchargeChoice {
        Check,
        Uncharge,
    }
}

private var ProtectedAccess.bonecrusherCharges by intVarBit("varbit.charges_bonecrusher_quantity")
