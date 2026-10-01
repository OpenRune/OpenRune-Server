package org.rsmod.content.raids.toa.raid.encounter.crondis.zebak

import org.rsmod.api.mechanics.toxins.impl.PlayerPoison
import org.rsmod.content.raids.toa.raid.encounter.ToaStage
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.map.CoordGrid

internal class ZebakPoison(private val room: ZebakEncounter) {
    private val deps = room.raid.deps
    private val hazards = deps.bossDeps.hazards

    operator fun contains(tile: CoordGrid): Boolean {
        val owner = room.zebak ?: return false
        return hazards.contains(owner, tile)
    }

    fun land(tiles: List<CoordGrid>) {
        for (tile in tiles) {
            deps.worldRepo.soundArea(
                tile,
                ZebakSynths.ACID_LAND,
                delay = LAND_SOUND_DELAY,
                radius = LAND_SOUND_RADIUS,
            )
            add(tile, spread = true, guaranteed = false)
        }
    }

    fun add(tile: CoordGrid, spread: Boolean, guaranteed: Boolean) {
        val owner = room.zebak ?: return
        if (hazards.contains(owner, tile)) return
        val type = ZebakLocs.POISON[deps.random.of(0, ZebakLocs.POISON.lastIndex)]
        val angle = LocAngle.entries[deps.random.of(0, LocAngle.entries.lastIndex)]
        hazards.place(owner, tile, type, angle = angle, onStand = ::hurt)
        if (!spread) return

        val range = if (room.raid.isActive(ZebakInvocations.UPSET_STOMACH)) 2 else 1
        val spreadTo = ArrayList<CoordGrid>()
        for (dx in -range..range) {
            for (dz in -range..range) {
                if (dx == 0 && dz == 0) continue
                if ((!guaranteed || dz != 0) && deps.random.of(0, 2) != 0) continue
                val next = tile.translate(dx, dz)
                if (hazards.contains(owner, next) || deps.collision.isWalkBlocked(next)) continue
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
        val owner = room.zebak ?: return
        hazards.remove(owner, tile)
    }

    fun clear() {
        val owner = room.zebak ?: return
        hazards.clear(owner)
    }

    private fun hurt(player: Player) {
        if (room.stage != ToaStage.STARTED || player !in room.targets()) return
        val damage = room.maxHit(deps.random.of(MIN_DAMAGE, MAX_DAMAGE))
        val poisoned =
            !PlayerPoison.isPoisoned(player) &&
                PlayerPoison.tryPoison(player, damage, severity = POISON_SEVERITY)
        if (!poisoned) PlayerPoison.incidentalPoisonHit(player, damage)
    }

    private companion object {
        const val MIN_DAMAGE = 6
        const val MAX_DAMAGE = 10
        const val POISON_SEVERITY = 11
        const val LAND_SOUND_RADIUS = 15
        const val LAND_SOUND_DELAY = 1
    }
}
