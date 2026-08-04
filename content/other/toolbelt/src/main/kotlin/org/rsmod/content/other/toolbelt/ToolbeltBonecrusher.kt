package org.rsmod.content.other.toolbelt

import org.rsmod.api.config.constants
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.table.ToolbeltSlotsRow
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.isType

object ToolbeltBonecrusher {
    const val ITEM = "obj.bonecrusher"
    private const val ECTO_TOKEN = "obj.ectotoken"
    private const val CHARGES_PER_TOKEN = 25
    private const val MAX_CHARGES = 60_000

    private fun bonecrusherSlot(): ToolbeltSlotsRow? = Toolbelt.slotFor(ITEM)

    fun hasOnToolbelt(player: Player): Boolean {
        val slot = bonecrusherSlot() ?: return false
        return Toolbelt.has(player, slot)
    }

    fun checkCharges(access: ProtectedAccess): Boolean {
        if (!hasOnToolbelt(access.player)) {
            return false
        }
        val charges = access.player.bonecrusherCharges
        when {
            charges == 0 ->
                access.mes("The bonecrusher has no charges. It can be charged with ectotokens.")
            isActivityEnabled(access.player) ->
                access.mes(
                    "The bonecrusher has $charges charges. It is active and ready to crush bones.",
                )
            else ->
                access.mes(
                    "The bonecrusher has $charges charges. It has been deactivated, and will not crush bones now.",
                )
        }
        return true
    }

    fun charge(access: ProtectedAccess): Boolean {
        if (!hasOnToolbelt(access.player)) {
            return false
        }
        val tokenSlot = access.inv.indexOfFirst { it?.isType(ECTO_TOKEN) == true }
        if (tokenSlot < 0) {
            access.mes("You need ectotokens to charge the bonecrusher.")
            return true
        }
        val tokenCount = access.inv.count(ECTO_TOKEN)
        val removed = access.invDel(access.inv, ECTO_TOKEN, count = tokenCount, slot = tokenSlot)
        if (removed.failure) {
            return true
        }
        val add = tokenCount * CHARGES_PER_TOKEN
        val next = (access.player.bonecrusherCharges + add).coerceAtMost(MAX_CHARGES)
        if (next == access.player.bonecrusherCharges) {
            access.invAdd(access.inv, ECTO_TOKEN, count = tokenCount, strict = false)
            access.mes("The bonecrusher cannot hold any more charges.")
            return true
        }
        access.player.bonecrusherCharges = next
        access.mes(chargeStatusMessage(access.player, access.player.bonecrusherCharges))
        return true
    }

    fun toggleActivity(access: ProtectedAccess): Boolean {
        if (!hasOnToolbelt(access.player)) {
            return false
        }
        val enabled = toggleActivity(access.player)
        access.mes(activityToggleMessage(enabled))
        return true
    }

    suspend fun uncharge(access: ProtectedAccess): Boolean {
        if (!hasOnToolbelt(access.player)) {
            return false
        }
        val stored = access.player.bonecrusherCharges
        when {
            stored == 0 -> access.mes("The bonecrusher has no charges.")
            stored < CHARGES_PER_TOKEN ->
                access.mes("The bonecrusher does not have enough charges for you to remove any ectotokens.")
            else -> {
                val redeemableTokens = stored / CHARGES_PER_TOKEN
                val chargesToRemove = redeemableTokens * CHARGES_PER_TOKEN
                val added = access.invAdd(access.inv, ECTO_TOKEN, count = redeemableTokens)
                if (added.failure) {
                    access.mes(constants.dm_invspace)
                    return true
                }
                val remaining = stored - chargesToRemove
                access.player.bonecrusherCharges = remaining.coerceAtLeast(0)
                if (remaining == 0) {
                    access.doubleobjbox(
                        ITEM,
                        ECTO_TOKEN,
                        "You remove all the charges from the bonecrusher.",
                    )
                } else {
                    access.doubleobjbox(
                        ITEM,
                        ECTO_TOKEN,
                        "The bonecrusher has $remaining charges left.",
                    )
                }
            }
        }
        return true
    }

    fun toggleActivity(player: Player): Boolean {
        val next = !player.bonecrusherActivityEnabled
        player.bonecrusherActivityEnabled = next
        return next
    }

    fun isActivityEnabled(player: Player): Boolean = player.bonecrusherActivityEnabled

    fun activityToggleMessage(enabled: Boolean): String =
        if (enabled) {
            "The bonecrusher is active and ready to crush bones."
        } else {
            "The bonecrusher has been deactivated, and will not crush bones now."
        }

    private fun chargeStatusMessage(player: Player, charges: Int): String =
        if (isActivityEnabled(player)) {
            "The bonecrusher has $charges charges. It is active and ready to crush bones."
        } else {
            "The bonecrusher has $charges charges. It has been deactivated, and will not crush bones now."
        }

    private var Player.bonecrusherActivityEnabled by boolVarBit("varbit.bonecrusher_activity_enabled")

    private var Player.bonecrusherCharges by intVarBit("varbit.charges_bonecrusher_quantity")
}
