package org.rsmod.content.other.ironman

import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import org.rsmod.api.player.hook.PlayerObjTakeValidateHook
import org.rsmod.api.player.ironman.IronmanActivity
import org.rsmod.api.player.ironman.isSoloIronman
import org.rsmod.game.entity.Player
import org.rsmod.game.obj.Obj

public class IronmanObjTakeHook @Inject constructor() : PlayerObjTakeValidateHook {
    override fun validateTake(player: Player, obj: Obj, objType: ItemServerType): String? {
        if (!player.isSoloIronman) {
            return null
        }
        if (obj.nullableOwnerId == null || obj.isOriginalOwner(player)) {
            return null
        }
        return IronmanActivity.FOREIGN_LOOT.defaultMessage
    }
}
