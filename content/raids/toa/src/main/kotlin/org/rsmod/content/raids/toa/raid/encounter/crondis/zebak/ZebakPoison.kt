package org.rsmod.content.raids.toa.raid.encounter.crondis.zebak

import org.rsmod.api.mechanics.toxins.impl.PlayerPoison
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.map.CoordGrid

/**
 * The acid pools (Offline_Scape ZebakEncounter.addPoison / process). A pool hurts from the tick
 * after it lands.
 *
 * Capture (standonpool, solo, raid level 45, 14 ticks on pools):
 * - every tick on a pool is a hit with the poison hitsplat (hitmark 65), 7-11 each time;
 * - the tile is the one you stood on at the start of the tick, so moving off still takes that
 *   tick's hit;
 * - the first hit also poisons you: "You have been poisoned!" and poison varp 10 (2 damage),
 *   on the same hitsplat, with no second one.
 *
 * Offline_Scape only hit players who weren't already poisoned, and rolled 5-20, so after the first
 * pool the long poison made every pool harmless. The 7-11 fits a 6-10 roll scaled by raid level
 * (1.18 at 45); one raid level can't show the scaling itself, so it uses Zebak's (`maxHit`).
 * Poison protection only stops the poison: the pool still hits (OSRS Wiki, Strategies: a serpentine
 * helm negates Zebak's poison).
 */
internal class ZebakPoison(private val room: ZebakEncounter) {
    private val deps = room.raid.deps
    private val pools = HashMap<CoordGrid, LocInfo>()
    private val active = HashSet<CoordGrid>()
    private val pending = ArrayList<CoordGrid>()

    operator fun contains(tile: CoordGrid): Boolean = tile in pools

    fun tick(targets: List<Player>) {
        active += pending
        pending.clear()
        if (active.isEmpty()) return
        for (player in targets) {
            if (player.coords !in active) continue
            val damage = room.maxHit(deps.random.of(MIN_DAMAGE, MAX_DAMAGE))
            // tryPoison refuses antipoison, immunity gear and venom; the pool hits anyway.
            val poisoned =
                !PlayerPoison.isPoisoned(player) &&
                    PlayerPoison.tryPoison(player, damage, severity = POISON_SEVERITY)
            if (!poisoned) PlayerPoison.incidentalPoisonHit(player, damage)
        }
    }

    /** Pools thrown by a special: a splat sound and a spreading pool on each tile. */
    fun land(tiles: List<CoordGrid>) {
        for (tile in tiles) {
            // Capture: delay 1.
            deps.worldRepo.soundArea(
                tile,
                ZebakSynths.ACID_LAND,
                delay = LAND_SOUND_DELAY,
                radius = LAND_SOUND_RADIUS,
            )
            add(tile, spread = true, guaranteed = false)
        }
    }

    /**
     * With [spread], each neighbour within 1 (2 with Upset Stomach) gets a pool a tick later with a
     * 1 in 3 chance; [guaranteed] makes the east and west neighbours certain.
     */
    fun add(tile: CoordGrid, spread: Boolean, guaranteed: Boolean) {
        if (tile in pools) return
        val type = ZebakLocs.POISON[deps.random.of(0, ZebakLocs.POISON.lastIndex)]
        val angle = LocAngle.entries[deps.random.of(0, LocAngle.entries.lastIndex)]
        val shape = LocShape.CentrepieceStraight
        pools[tile] = deps.locRepo.add(tile, type, Int.MAX_VALUE, angle, shape)
        pending += tile
        if (!spread) return

        val range = if (room.raid.isActive(ZebakInvocations.UPSET_STOMACH)) 2 else 1
        val spreadTo = ArrayList<CoordGrid>()
        for (dx in -range..range) {
            for (dz in -range..range) {
                if (dx == 0 && dz == 0) continue
                if ((!guaranteed || dz != 0) && deps.random.of(0, 2) != 0) continue
                val next = tile.translate(dx, dz)
                if (next in pools || deps.collision.isWalkBlocked(next)) continue
                val spot = ZebakSpots.POISON_SPREAD
                deps.worldRepo.projectile(spot, tile, next, ZebakProjs.POISON_SPREAD)
                spreadTo += next
            }
        }
        if (spreadTo.isNotEmpty()) {
            room.schedule(1) {
                for (next in spreadTo) add(next, spread = false, guaranteed = false)
            }
        }
    }

    fun remove(tile: CoordGrid) {
        val loc = pools.remove(tile) ?: return
        active.remove(tile)
        pending.remove(tile)
        deps.locRepo.del(loc, Int.MAX_VALUE)
    }

    fun clear() {
        for (loc in pools.values) deps.locRepo.del(loc, Int.MAX_VALUE)
        pools.clear()
        active.clear()
        pending.clear()
    }

    private companion object {
        /** Before scaling; the capture's 7-11 at raid level 45. */
        const val MIN_DAMAGE = 6
        const val MAX_DAMAGE = 10

        /**
         * Capture: poison varp 10 after the first hit, i.e. 2 damage (ceil(severity / 5)).
         * PlayerPoison takes one off for the hit it applies with, hence 11.
         */
        const val POISON_SEVERITY = 11
        const val LAND_SOUND_RADIUS = 15
        const val LAND_SOUND_DELAY = 1
    }
}
