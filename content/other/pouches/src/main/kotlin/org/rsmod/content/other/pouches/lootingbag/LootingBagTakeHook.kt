package org.rsmod.content.other.pouches.lootingbag

import dev.openrune.types.ItemServerType
import dev.openrune.types.util.UncheckedType
import jakarta.inject.Inject
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.hook.PlayerObjTakeStorageHook
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.obj.Obj

/**
 * With an open looting bag, tradeable loot picked up in the Wilderness goes straight into the bag.
 * Anything the bag refuses, or no longer fits, takes the normal inventory route.
 */
@OptIn(UncheckedType::class)
class LootingBagTakeHook @Inject constructor() : PlayerObjTakeStorageHook {
    override fun inventory(player: Player, obj: Obj, objType: ItemServerType): Inventory? {
        if (!player.hasOpenLootingBag()) {
            return null
        }
        if (player.vars["varbit.inside_wilderness"] != 1) {
            return null
        }
        if (LootingBags.rejection(InvObj(obj.type, obj.count), objType) != null) {
            return null
        }
        return player.lootingBag.takeIf { player.invAdd(it, obj.type, obj.count, autoCommit = false).success }
    }

    override fun committed(player: Player, obj: Obj, objType: ItemServerType) {
        player.saveLootingBag()
    }
}
