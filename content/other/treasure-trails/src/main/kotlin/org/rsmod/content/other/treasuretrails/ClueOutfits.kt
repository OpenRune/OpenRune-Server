package org.rsmod.content.other.treasuretrails

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.util.Wearpos
import org.rsmod.api.table.cluehelper.CluehelperOutfitRow
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory

/**
 * An outfit slot either lists the objs it accepts (`blankobject` meaning the slot must be empty) or
 * an item group that any obj with a matching `trail_item_group` param satisfies.
 */
object ClueOutfits {
    fun wearing(player: Player, outfit: CluehelperOutfitRow?): Boolean {
        if (outfit == null) return true
        return outfit.slots().all { it.wornBy(player.worn) }
    }

    fun piecesIn(inv: Inventory, outfit: CluehelperOutfitRow): List<Int>? {
        val taken = mutableSetOf<Int>()
        for (slot in outfit.slots().filterNot { it.empty }) {
            val match =
                (0 until inv.size).firstOrNull { index ->
                    index !in taken && inv[index]?.let { slot.accepts(TrailTiers.type(it.id)) } == true
                } ?: return null
            taken += match
        }
        return taken.toList()
    }

    /**
     * The [worn] slots that must be unequipped to complete [outfit] once every piece [inv] already
     * covers is used first, or `null` if the two together still lack a piece.
     */
    fun wornPiecesNeeded(inv: Inventory, worn: Inventory, outfit: CluehelperOutfitRow): List<Int>? {
        val taken = mutableSetOf<Int>()
        val wornSlots = mutableListOf<Int>()
        for (slot in outfit.slots().filterNot { it.empty }) {
            val match =
                inv.indices.firstOrNull { index ->
                    index !in taken && inv[index]?.let { slot.accepts(TrailTiers.type(it.id)) } == true
                }
            if (match != null) {
                taken += match
                continue
            }
            wornSlots +=
                worn.indices.firstOrNull { index ->
                    index !in wornSlots &&
                        worn[index]?.let { slot.accepts(TrailTiers.type(it.id)) } == true
                } ?: return null
        }
        return wornSlots
    }

    private class Slot(
        val wearpos: Wearpos?,
        val objs: List<ItemServerType>,
        val group: Int?,
    ) {
        val empty: Boolean
            get() = group == null && objs.all { it.id == BLANK_OBJ.asRSCM(RSCMType.OBJ) }

        fun accepts(type: ItemServerType): Boolean =
            if (group != null) TrailTiers.itemGroup(type) == group
            else objs.any { it.id == type.id }

        fun wornBy(worn: Inventory): Boolean {
            if (wearpos == null) {
                return worn.objs.any { obj -> obj != null && accepts(TrailTiers.type(obj.id)) }
            }
            val obj = worn[wearpos.slot]
            if (empty) return obj == null
            return obj != null && accepts(TrailTiers.type(obj.id))
        }
    }

    private fun CluehelperOutfitRow.slots(): List<Slot> =
        listOf(
                slot(Wearpos.Hat, wearposHat, wearposParamHat),
                slot(Wearpos.Back, wearposBack, wearposParamBack),
                slot(Wearpos.Front, wearposFront, wearposParamFront),
                slot(Wearpos.RightHand, wearposRhand, wearposParamRhand),
                slot(Wearpos.Torso, wearposTorso, wearposParamTorso),
                slot(Wearpos.LeftHand, wearposLhand, wearposParamLhand),
                slot(Wearpos.Legs, wearposLegs, wearposParamLegs),
                slot(Wearpos.Hands, wearposHands, wearposParamHands),
                slot(Wearpos.Feet, listOfNotNull(wearposFeet), wearposParamFeet),
                slot(Wearpos.Ring, wearposRing, wearposParamRing),
                slot(Wearpos.Quiver, listOfNotNull(wearposQuiver), wearposParamQuiver),
                wearposParamAny?.let { Slot(null, emptyList(), it) },
            )
            .filterNotNull()

    private fun slot(wearpos: Wearpos, objs: List<ItemServerType>, group: Int?): Slot? =
        if (objs.isEmpty() && group == null) null else Slot(wearpos, objs, group)

    private const val BLANK_OBJ = "obj.blankobject"
}
