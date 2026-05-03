package org.rsmod.api.account.character.inv

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.util.UncheckedType
import kotlin.collections.iterator
import org.rsmod.api.account.character.CharacterDataStage
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj

@OptIn(UncheckedType::class)
public class CharacterInventoryApplier : CharacterDataStage.Applier<CharacterInventoryData> {
    override fun apply(player: Player, data: CharacterInventoryData) {
        for (loaded in data.inventories) {
            val type = ServerCacheManager.getInventory(loaded.type) ?: return
            val inventory = player.invMap.getOrPut(RSCM.getReverseMapping(RSCMType.INV, type.id))

            for ((slot, obj) in loaded.objs) {
                val (type, count, vars) = obj
                inventory[slot] = InvObj(type, count, vars = vars)
            }
        }
    }
}
