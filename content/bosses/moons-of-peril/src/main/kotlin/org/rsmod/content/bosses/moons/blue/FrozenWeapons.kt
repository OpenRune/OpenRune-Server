package org.rsmod.content.bosses.moons.blue

import dev.openrune.types.ItemServerType
import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.config.refs.params
import org.rsmod.api.invtx.invTransfer
import org.rsmod.api.player.events.interact.HeldEquipEvents
import org.rsmod.api.player.ironman.isUltimateIronman
import org.rsmod.api.player.worn.WornUnequipOp
import org.rsmod.content.interfaces.bank.bankAppend
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.getInvObj

@Singleton
class FrozenWeapons @Inject constructor(private val eventBus: EventBus) {
    fun holding(player: Player): Inventory = player.invMap.getOrPut(HOLDING_INV)

    fun held(player: Player): ItemServerType? =
        holding(player)[0]?.let { getInvObj(it) }

    fun freeze(player: Player): FreezeResult {
        val holding = holding(player)
        if (holding[0] != null) {
            release(player)
            if (holding[0] != null) {
                return FreezeResult.AlreadyHolding
            }
        }
        val obj = player.worn[Wearpos.RightHand.slot] ?: return FreezeResult.NoWeapon
        val type = getInvObj(obj)
        val result =
            player.invTransfer(
                from = player.worn,
                fromSlot = Wearpos.RightHand.slot,
                count = obj.count,
                into = holding,
                intoSlot = 0,
                strict = true,
            )
        if (!result.success) {
            return FreezeResult.Denied
        }
        WornUnequipOp.notifyWornUnequip(player, Wearpos.RightHand, type, eventBus)
        player.clearInteraction()
        return FreezeResult.Frozen
    }

    fun release(player: Player): ReleaseResult {
        val holding = holding(player)
        val obj = holding[0] ?: return ReleaseResult.Nothing
        val type = getInvObj(obj)
        if (toHands(player, holding, type)) {
            return ReleaseResult.ToHands
        }
        return stash(player, holding, type)
    }

    fun reclaim(player: Player): ReleaseResult {
        val holding = holding(player)
        val obj = holding[0] ?: return ReleaseResult.Nothing
        return stash(player, holding, getInvObj(obj))
    }

    private fun toHands(player: Player, holding: Inventory, type: ItemServerType): Boolean {
        val worn = player.worn
        if (worn[Wearpos.RightHand.slot] != null) {
            return false
        }
        if (type.isTwoHanded() && worn[Wearpos.LeftHand.slot] != null) {
            return false
        }
        val obj = holding[0] ?: return false
        val result =
            player.invTransfer(
                from = holding,
                fromSlot = 0,
                count = obj.count,
                into = worn,
                intoSlot = Wearpos.RightHand.slot,
                strict = true,
            )
        if (!result.success) {
            return false
        }
        eventBus.publish(HeldEquipEvents.WearposChange(player, Wearpos.RightHand, type))
        player.rebuildAppearance()
        return true
    }

    private fun stash(player: Player, holding: Inventory, type: ItemServerType): ReleaseResult {
        val obj = holding[0] ?: return ReleaseResult.Nothing
        val toInv =
            player.invTransfer(
                from = holding,
                fromSlot = 0,
                count = obj.count,
                into = player.inv,
                intoSlot = null,
                strict = true,
            )
        if (toInv.success) {
            return ReleaseResult.ToInventory
        }
        val bankable = !player.isUltimateIronman && type.param(params.no_bank) == 0
        if (bankable && player.bankAppend(holding, 0)) {
            return ReleaseResult.ToBank
        }
        return ReleaseResult.Kept
    }

    private fun ItemServerType.isTwoHanded(): Boolean =
        wearpos2 == Wearpos.LeftHand.slot || wearpos3 == Wearpos.LeftHand.slot

    enum class FreezeResult {
        Frozen,
        NoWeapon,
        Denied,
        AlreadyHolding,
    }

    enum class ReleaseResult {
        Nothing,
        ToHands,
        ToInventory,
        ToBank,
        Kept,
    }

    companion object {
        const val HOLDING_INV = "inv.pmoon_frozen_weapon"
    }
}
