package org.rsmod.content.other.pets.cats

import jakarta.inject.Inject
import org.rsmod.api.death.PlayerDeathContext
import org.rsmod.api.death.PlayerDeathHandling
import org.rsmod.api.death.PlayerDeathHook
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.output.mes
import org.rsmod.content.other.pets.PetFollowers
import org.rsmod.content.other.pets.followerObj

class CatDeathHook @Inject constructor(private val followers: PetFollowers) : PlayerDeathHook {
    override fun handleDeath(context: PlayerDeathContext): PlayerDeathHandling? {
        val player = context.player
        var lost = false
        if (Cats.forObj(player.followerObj) != null) {
            followers.dismiss(player)
            lost = true
        }
        for (slot in player.inv.indices) {
            val obj = player.inv[slot] ?: continue
            if (Cats.forObj(obj.id) == null) {
                continue
            }
            player.invDel(player.inv, obj.id, obj.count, slot)
            lost = true
        }
        if (lost) {
            player.mes("Your cat has run away.")
        }
        return null
    }
}
