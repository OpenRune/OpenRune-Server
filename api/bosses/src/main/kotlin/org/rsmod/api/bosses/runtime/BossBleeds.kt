package org.rsmod.api.bosses.runtime

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.queue.WorldQueueList
import org.rsmod.map.CoordGrid

@Singleton
class BossBleeds
@Inject
constructor(private val mapClock: MapClock, private val worldQueues: WorldQueueList) {
    private val bleeds = HashMap<Player, Bleed>()

    private class Bleed(
        val owner: Npc,
        val duration: Int,
        val stillInterval: Int,
        val onApply: (Player) -> Unit,
        val onStill: (Player) -> Unit,
        val onMoving: (Player) -> Unit,
        var tile: CoordGrid,
    ) {
        var start: Int = -1
        var movedAt: Int? = null
    }

    fun apply(
        owner: Npc,
        player: Player,
        duration: Int,
        stillInterval: Int = 0,
        onApply: (Player) -> Unit = {},
        onStill: (Player) -> Unit = {},
        onMoving: (Player) -> Unit,
    ) {
        require(duration > 0) { "`duration` must be greater than 0. (duration=$duration)" }
        require(stillInterval >= 0) { "`stillInterval` must not be negative. (stillInterval=$stillInterval)" }
        val bleed = Bleed(owner, duration, stillInterval, onApply, onStill, onMoving, player.coords)
        bleeds[player] = bleed
        worldQueues.add(1) { tick(player, bleed) }
    }

    fun isBleeding(player: Player): Boolean = player in bleeds

    fun remove(owner: Npc, player: Player): Boolean {
        val bleed = bleeds[player] ?: return false
        if (bleed.owner !== owner) return false
        bleeds.remove(player)
        return true
    }

    fun clear(owner: Npc) {
        bleeds.values.removeIf { it.owner === owner }
    }

    private fun tick(player: Player, bleed: Bleed) {
        if (bleeds[player] !== bleed) return
        if (!player.isSlotAssigned || player.hitpoints <= 0 || !bleed.owner.isSlotAssigned) {
            bleeds.remove(player)
            return
        }
        val now = mapClock.cycle
        if (player.coords != bleed.tile) {
            bleed.tile = player.coords
            bleed.movedAt = now
        }
        if (bleed.start < 0) {
            bleed.start = now
            bleed.onApply(player)
        } else {
            val elapsed = now - bleed.start
            val moved = bleed.movedAt?.let { now - it <= 1 } == true
            when {
                moved -> bleed.onMoving(player)
                bleed.stillInterval > 0 && elapsed % bleed.stillInterval == 0 -> bleed.onStill(player)
            }
            if (elapsed >= bleed.duration) {
                if (bleeds[player] === bleed) bleeds.remove(player)
                return
            }
        }
        worldQueues.add(1) { tick(player, bleed) }
    }
}
