package org.rsmod.content.raids.toa.raid

import kotlin.math.floor
import org.rsmod.api.player.vars.intVarp
import org.rsmod.game.entity.Player

class ToaPoints(members: List<Player>) {
    private val totals = LinkedHashMap<Player, Int>()
    private val room = HashMap<Player, Int>()

    init {
        for (member in members) totals[member] = START
    }

    fun total(player: Player): Int = totals[player] ?: 0

    fun roomPoints(player: Player): Int = room[player] ?: 0

    fun lootPoints(player: Player): Int = (total(player) - START).coerceAtLeast(0)

    fun addDamage(player: Player, damage: Int, multiplier: Double, roomCap: Int) {
        if (player !in totals || damage <= 0 || multiplier <= 0.0) return
        val earned = floor(damage * multiplier).toInt()
        room[player] = (roomPoints(player) + earned).coerceAtMost(roomCap)
    }

    fun resetRoom() {
        room.clear()
    }

    fun onDeath(player: Player) {
        val total = totals[player] ?: return
        val loss = maxOf(total * DEATH_LOSS_PERCENT / 100, DEATH_LOSS_MIN)
        totals[player] = (total - loss).coerceAtLeast(0)
    }

    fun completeRoom(teamSize: Int, completionPoints: Int, awardsMvp: Boolean) {
        val mvp = if (awardsMvp) totals.keys.maxByOrNull { roomPoints(it) } else null
        for (entry in totals.entries) {
            var earned = roomPoints(entry.key) + completionPoints
            if (entry.key === mvp) earned += MVP_PER_PLAYER * teamSize
            entry.setValue((entry.value + earned).coerceAtMost(TOTAL_MAX))
        }
        room.clear()
    }

    private companion object {
        const val START = 5_000
        const val TOTAL_MAX = 64_000
        const val MVP_PER_PLAYER = 300
        const val DEATH_LOSS_PERCENT = 20
        const val DEATH_LOSS_MIN = 1_000
    }
}

internal var Player.personalContribution by intVarp("varp.toa_personal_contribution")
