package org.rsmod.content.other.treasuretrails

import dev.openrune.types.ItemServerType
import dev.openrune.util.Wearpos
import org.rsmod.api.table.cluehelper.CluehelperOutfitRow
import org.rsmod.game.entity.Player

object ClueOutfits {
    fun wearing(player: Player, outfit: CluehelperOutfitRow?): Boolean {
        if (outfit == null) return true
        if (outfit.paramSlots().any { it != null }) return false
        return outfit.objSlots().all { (wearpos, accepted) ->
            accepted.isEmpty() || player.worn[wearpos.slot]?.id in accepted.map { it.id }
        }
    }

    private fun CluehelperOutfitRow.objSlots(): List<Pair<Wearpos, List<ItemServerType>>> =
        listOf(
            Wearpos.Hat to wearposHat,
            Wearpos.Back to wearposBack,
            Wearpos.Front to wearposFront,
            Wearpos.RightHand to wearposRhand,
            Wearpos.Torso to wearposTorso,
            Wearpos.LeftHand to wearposLhand,
            Wearpos.Legs to wearposLegs,
            Wearpos.Hands to wearposHands,
            Wearpos.Feet to listOfNotNull(wearposFeet),
            Wearpos.Ring to wearposRing,
            Wearpos.Quiver to listOfNotNull(wearposQuiver),
        )

    private fun CluehelperOutfitRow.paramSlots(): List<Int?> =
        listOf(
            wearposParamHat,
            wearposParamBack,
            wearposParamFront,
            wearposParamRhand,
            wearposParamTorso,
            wearposParamLhand,
            wearposParamLegs,
            wearposParamHands,
            wearposParamFeet,
            wearposParamRing,
            wearposParamQuiver,
            wearposParamAny,
        )
}
