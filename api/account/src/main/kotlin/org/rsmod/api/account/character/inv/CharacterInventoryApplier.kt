package org.rsmod.api.account.character.inv

import dev.openrune.types.util.UncheckedType
import kotlin.collections.iterator
import org.rsmod.api.account.character.CharacterDataStage
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj

@OptIn(UncheckedType::class)
public class CharacterInventoryApplier : CharacterDataStage.Applier<CharacterInventoryData> {
    override fun apply(player: Player, data: CharacterInventoryData) {
        for (loaded in data.inventories) {
            val inventory = player.invMap.getOrPut(loaded.invKey)

            for ((slot, obj) in loaded.objs) {
                inventory[slot] = InvObj(obj.objKey, count = obj.count, vars = obj.vars)
            }
        }
    }
}
