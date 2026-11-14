package org.rsmod.content.bosses.bryophyta

import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import org.rsmod.api.player.hook.PlayerObjTakeValidateHook
import org.rsmod.game.entity.Player
import org.rsmod.game.obj.Obj

class BryophytaGroundItemHook @Inject constructor() : PlayerObjTakeValidateHook {
    override fun validateTake(player: Player, obj: Obj, objType: ItemServerType): String? =
        if (objType.internalName == GIANT_BONES) TOO_HEAVY_MESSAGE else null

    private companion object {
        private const val GIANT_BONES = "obj.giant_bones"
        private const val TOO_HEAVY_MESSAGE = "I don't think those will fit in my backpack..."
    }
}
