package org.rsmod.content.raids.toa.raid.encounter.crondis.zebak

import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.map.CoordGrid

/**
 * Jugs (Offline_Scape CrondisJug / JugPushAction). Push or Pull sets one rolling a tile a tick,
 * diagonally too; the standing jug becomes the rolling jug npc. It shatters on a boulder, splashes
 * away off the floor, and clears acid when it shatters: 5x5, or 3x3 with Upset Stomach.
 */
internal class ZebakJugs(private val room: ZebakEncounter) {
    private val deps = room.raid.deps
    private val jugs = HashMap<Npc, Roll>()

    /** Rolling direction; 0/0 while standing. */
    private class Roll(var dx: Int = 0, var dz: Int = 0)

    val npcs: Collection<Npc>
        get() = jugs.keys

    /** Jugs thrown by a special; anyone underneath takes 2-5. */
    fun land(tiles: List<CoordGrid>, targets: List<Player>) {
        val dust = spotanim(ZebakSpots.DUST)
        for (tile in tiles) {
            jugs[room.spawn(ZebakNpcs.JUG, tile)] = Roll()
            deps.worldRepo.spotanimMap(dust, tile)
            for (player in targets) {
                if (player.coords != tile) continue
                player.hitTypeless(deps.random.of(LANDING_MIN, LANDING_MAX))
            }
        }
    }

    /** Push: away from the player. Pull: towards them. */
    fun move(player: Player, jug: Npc, push: Boolean) {
        val roll = jugs[jug] ?: return
        if (roll.dx != 0 || roll.dz != 0) return
        val sign = if (push) 1 else -1
        val dx = Integer.signum(jug.coords.x - player.coords.x) * sign
        val dz = Integer.signum(jug.coords.z - player.coords.z) * sign
        if (dx == 0 && dz == 0) return
        setRolling(jug, roll, dx, dz)
        player.anim(ZebakSeqs.PLAYER_MOVE_JUG)
    }

    /** A wave sets [jug] rolling its way, even if it already rolls (Offline_Scape WaveNPC). */
    fun roll(jug: Npc, dz: Int) {
        val roll = jugs[jug] ?: return
        setRolling(jug, roll, 0, dz)
    }

    private fun setRolling(jug: Npc, roll: Roll, dx: Int, dz: Int) {
        if (roll.dx == 0 && roll.dz == 0) {
            jug.transmog(npcType(ZebakNpcs.JUG_ROLLING), Int.MAX_VALUE)
        }
        roll.dx = dx
        roll.dz = dz
    }

    /**
     * Once a tick (config `timer = 1`). The boulder's tile blocks walking, so a jug shatters beside
     * it with the splash centred on the boulder (Offline_Scape rolled onto it; same tiles cleared).
     * TODO: the wiki says vanilla jugs stop after some distance; Offline_Scape rolls until stopped.
     */
    fun tick(jug: Npc) {
        val roll = jugs[jug] ?: return
        if (roll.dx == 0 && roll.dz == 0) return
        val next = jug.coords.translate(roll.dx, roll.dz)
        when {
            room.boulders.isBoulder(next) -> shatter(jug, next)
            deps.collision.isWalkBlocked(next) -> {
                deps.worldRepo.spotanimMap(spotanim(ZebakSpots.WATER_SPLASH), next)
                remove(jug)
            }
            else -> jug.walk(next)
        }
    }

    /** Water flies to each acid tile in range, cleared a tick later. */
    fun shatter(jug: Npc, centre: CoordGrid) {
        if (jug !in jugs) return
        remove(jug)
        deps.worldRepo.spotanimMap(spotanim(ZebakSpots.JUG_BREAK), centre)
        val range = if (room.raid.isActive(ZebakInvocations.UPSET_STOMACH)) 1 else 2
        val cleared = ArrayList<CoordGrid>()
        for (dx in -range..range) {
            for (dz in -range..range) {
                val tile = centre.translate(dx, dz)
                if (tile !in room.poison) continue
                cleared += tile
                val splash = ZebakSpots.JUG_SPLASH
                deps.worldRepo.projectile(splash, centre, tile, ZebakProjs.JUG_SPLASH)
            }
        }
        if (cleared.isEmpty()) return
        room.schedule(1) {
            val splash = spotanim(ZebakSpots.ACID_CLEARED)
            for (tile in cleared) {
                room.poison.remove(tile)
                deps.worldRepo.spotanimMap(splash, tile)
            }
        }
    }

    fun shatterAll() {
        for (jug in jugs.keys.toList()) shatter(jug, jug.coords)
    }

    private fun remove(jug: Npc) {
        jugs.remove(jug)
        room.despawn(jug)
    }

    fun clear() {
        for (jug in jugs.keys.toList()) remove(jug)
    }

    companion object {
        /** How many jugs a special throws. */
        const val THROWN_MIN = 6
        const val THROWN_MAX = 8
        private const val LANDING_MIN = 2
        private const val LANDING_MAX = 5
    }
}
