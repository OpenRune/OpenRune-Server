package org.rsmod.api.mechanics.toxins.impl

import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

public object PlayerBleed {
    private val bleedKey = AttributeKey<Bleed>()

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

    public fun apply(
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
        player.attr[bleedKey] = Bleed(owner, duration, stillInterval, onApply, onStill, onMoving, player.coords)
        player.timer("timer.player_bleed", 1)
    }

    public fun isBleeding(player: Player): Boolean = player.attr.has(bleedKey)

    public fun ownerOf(player: Player): Npc? = player.attr[bleedKey]?.owner

    public fun clear(player: Player) {
        player.attr.remove(bleedKey)
        player.clearTimer("timer.player_bleed")
    }

    public fun removeOwned(owner: Npc, player: Player): Boolean {
        if (ownerOf(player) !== owner) return false
        clear(player)
        return true
    }

    public fun clearOwnedBy(owner: Npc, players: Iterable<Player>) {
        for (player in players) removeOwned(owner, player)
    }

    public fun onBleedTimerTick(player: Player) {
        val bleed = player.attr[bleedKey]
        if (bleed == null || player.hitpoints <= 0 || !bleed.owner.isSlotAssigned) {
            clear(player)
            return
        }
        val now = player.currentMapClock
        if (player.coords != bleed.tile) {
            bleed.tile = player.coords
            bleed.movedAt = now
        }
        if (bleed.start < 0) {
            bleed.start = now
            bleed.onApply(player)
            return
        }
        val elapsed = now - bleed.start
        val moved = bleed.movedAt?.let { now - it <= 1 } == true
        when {
            moved -> bleed.onMoving(player)
            bleed.stillInterval > 0 && elapsed % bleed.stillInterval == 0 -> bleed.onStill(player)
        }
        if (elapsed >= bleed.duration && player.attr[bleedKey] === bleed) {
            clear(player)
        }
    }
}
