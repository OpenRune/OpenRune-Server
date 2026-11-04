package org.rsmod.content.skills.farming

import dev.openrune.types.ItemServerType
import org.rsmod.api.death.PlayerDeathDropHook
import org.rsmod.api.player.hook.PlayerObjTakeReceiveHook
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.mes
import org.rsmod.game.entity.Player
import org.rsmod.plugin.module.PluginModule

/** Open herb sacks, seed boxes and reagent pouches keep what the player picks up while it fits. */
internal class FarmingBagTakeHook : PlayerObjTakeReceiveHook {
    override fun canReceive(player: Player, objType: ItemServerType, count: Int): Boolean =
        target(player, objType.id, count) != null

    override fun receive(player: Player, objType: ItemServerType, count: Int): Boolean {
        val (bag, held) = target(player, objType.id, count) ?: return false
        if (!bag.put(player, objType.id, count).success) return false
        if (bag === FarmingBags.herbSack) {
            val sack = FarmingBags.herbSackName(held)
            player.mes("You put the ${objType.name} herb into your $sack.", ChatType.Spam)
        }
        return true
    }

    private fun target(player: Player, obj: Int, count: Int): Pair<FarmingBag, String>? {
        for (bag in BAGS) {
            if (!bag.isStorable(obj)) continue
            val held = bag.openHeld(player) ?: return null
            return if (bag.room(player, held, obj) >= count) bag to held else null
        }
        return null
    }

    private companion object {
        val BAGS = listOf(FarmingBags.herbSack, FarmingBags.seedBox, FarmingBags.reagentPouch)
    }
}

public class FarmingBagModule : PluginModule() {
    override fun bind() {
        addSetBinding<PlayerObjTakeReceiveHook>(FarmingBagTakeHook::class.java)
        addSetBinding<PlayerDeathDropHook>(FarmingBagDeathHook::class.java)
    }
}
