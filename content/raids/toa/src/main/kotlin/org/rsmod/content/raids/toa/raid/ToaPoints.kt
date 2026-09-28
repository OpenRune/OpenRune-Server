package org.rsmod.content.raids.toa.raid

import kotlin.math.floor
import org.rsmod.game.entity.Player

/**
 * One raid's reward points (OSRS Wiki, Chest (Tombs of Amascut), and Jagex's 28 Oct 2022 blog
 * "Tombs of Amascut Drop Mechanics"). The player never sees them; the reward chest reads them.
 *
 * - Everyone starts with 5,000 total points.
 * - Damage to a room's npcs earns room points: 1 per damage times the npc's multiplier
 *   ([org.rsmod.content.raids.toa.raid.encounter.ToaEncounter.pointMultiplier]), at most 20,000
 *   per room.
 * - When the room is completed, room points move into the total (at most 64,000). The player with
 *   the most room points also gets 300 per team member (the MVP bonus).
 * - A death costs 20% of the total, at least 1,000.
 * - The chest ignores the starting 5,000 ([lootPoints]).
 *
 * Players who leave keep their entry: the wiki (Mod Ash, 28 Mar 2023) says their contribution
 * still counts towards the group's unique roll.
 *
 * Not settled by any source, so these are choices: a wipe keeps everyone's room points for the
 * next attempt (the wiki only says they're added on completion), and a tie for MVP goes to the
 * first player in HUD order.
 */
class ToaPoints(members: List<Player>) {
    private val totals = LinkedHashMap<Player, Int>()
    private val room = HashMap<Player, Int>()

    init {
        for (member in members) totals[member] = START
    }

    fun total(player: Player): Int = totals[player] ?: 0

    fun roomPoints(player: Player): Int = room[player] ?: 0

    /** What the chest uses: the total without the starting points. */
    fun lootPoints(player: Player): Int = (total(player) - START).coerceAtLeast(0)

    fun addDamage(player: Player, damage: Int, multiplier: Double) {
        if (player !in totals || damage <= 0 || multiplier <= 0.0) return
        val earned = floor(damage * multiplier).toInt()
        room[player] = (roomPoints(player) + earned).coerceAtMost(ROOM_MAX)
    }

    fun onDeath(player: Player) {
        val total = totals[player] ?: return
        val loss = maxOf(total * DEATH_LOSS_PERCENT / 100, DEATH_LOSS_MIN)
        totals[player] = (total - loss).coerceAtLeast(0)
    }

    /** Moves the room points into the totals, MVP bonus included, and clears them. */
    fun completeRoom(teamSize: Int) {
        val mvp = totals.keys.maxByOrNull { roomPoints(it) }?.takeIf { roomPoints(it) > 0 }
        for (entry in totals.entries) {
            var earned = roomPoints(entry.key)
            if (entry.key === mvp) earned += MVP_PER_PLAYER * teamSize
            entry.setValue((entry.value + earned).coerceAtMost(TOTAL_MAX))
        }
        room.clear()
    }

    private companion object {
        const val START = 5_000
        const val ROOM_MAX = 20_000
        const val TOTAL_MAX = 64_000
        const val MVP_PER_PLAYER = 300
        const val DEATH_LOSS_PERCENT = 20
        const val DEATH_LOSS_MIN = 1_000
    }
}
