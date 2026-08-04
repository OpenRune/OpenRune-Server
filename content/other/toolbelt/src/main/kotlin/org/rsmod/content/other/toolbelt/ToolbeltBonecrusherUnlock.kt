package org.rsmod.content.other.toolbelt

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.table.ToolbeltSlotsRow
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.getInvObj

object ToolbeltBonecrusherUnlock {
    const val ECTO_TOKEN = "obj.ectotoken"
    const val ECTO_TOKEN_COST = 2000

    fun bonecrusherSlot(): ToolbeltSlotsRow? = Toolbelt.slotFor(ToolbeltBonecrusher.ITEM)

    fun canOfferUnlock(player: Player): Boolean {
        if (!Toolbelt.isEnabled()) {
            return false
        }
        val slot = bonecrusherSlot() ?: return false
        if (ToolbeltUnlocks.isUnlocked(player, slot)) {
            return false
        }
        if (!meetsSlotSkillRequirement(player, slot)) {
            return false
        }
        return playerCarriesBonecrusher(player)
    }

    fun playerCarriesBonecrusher(player: Player): Boolean {
        if (player.inv.any { it.isBonecrusher() }) {
            return true
        }
        return player.worn.any { it.isBonecrusher() }
    }

    fun tryUnlock(access: ProtectedAccess): PayResult {
        if (!Toolbelt.isEnabled()) {
            return PayResult.NotEnabled
        }
        val slot = bonecrusherSlot() ?: return PayResult.NoSlot
        if (ToolbeltUnlocks.isUnlocked(access.player, slot)) {
            return PayResult.AlreadyUnlocked
        }
        if (!meetsSlotSkillRequirement(access.player, slot)) {
            val required = slot.requireLevel ?: 1
            return PayResult.InsufficientPrayer(required)
        }
        if (!playerCarriesBonecrusher(access.player)) {
            return PayResult.NeedsBonecrusher
        }
        val tokens = access.invContentTotal(access.inv, ECTO_TOKEN)
        if (tokens < ECTO_TOKEN_COST) {
            return PayResult.InsufficientTokens(tokens)
        }
        if (access.invDel(access.inv, ECTO_TOKEN, ECTO_TOKEN_COST).failure) {
            return PayResult.InsufficientTokens(tokens)
        }
        ToolbeltUnlocks.setUnlocked(access.player, slot, unlocked = true)
        return PayResult.Success
    }

    private fun meetsSlotSkillRequirement(player: Player, slot: ToolbeltSlotsRow): Boolean {
        val level = slot.requireLevel ?: return true
        val skill = slot.skillStat ?: return true
        return player.statBase(skill.internalName) >= level
    }

    sealed class PayResult {
        data object Success : PayResult()

        data object NotEnabled : PayResult()

        data object NoSlot : PayResult()

        data object AlreadyUnlocked : PayResult()

        data object NeedsBonecrusher : PayResult()

        data class InsufficientPrayer(val required: Int) : PayResult()

        data class InsufficientTokens(val held: Int) : PayResult()
    }
}

private fun InvObj?.isBonecrusher(): Boolean =
    this != null && getInvObj(this).internalName == ToolbeltBonecrusher.ITEM
