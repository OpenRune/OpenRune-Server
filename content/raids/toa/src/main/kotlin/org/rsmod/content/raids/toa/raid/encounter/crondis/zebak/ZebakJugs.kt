package org.rsmod.content.raids.toa.raid.encounter.crondis.zebak

import org.rsmod.api.npc.hit.queueHit as queueNpcHit
import org.rsmod.content.raids.toa.party.ToaInvocationKey
import org.rsmod.content.raids.toa.raid.encounter.hitTypeless
import org.rsmod.content.raids.toa.raid.encounter.npcType
import org.rsmod.content.raids.toa.raid.encounter.spotanim
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.interact.InteractionNpc
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.map.CoordGrid

internal class ZebakJugs(private val room: ZebakEncounter) {
    private val deps = room.raid.deps
    private val jugs = HashMap<Npc, Roll>()

    private class Roll(var dx: Int = 0, var dz: Int = 0, var left: Int = 0)

    val npcs: Collection<Npc>
        get() = jugs.keys

    fun land(tiles: List<CoordGrid>, targets: List<Player>) {
        val dust = spotanim(ZebakSpots.DUST)
        for (tile in tiles) {
            val jug = room.spawn(ZebakNpcs.JUG, tile)
            jug.ignoreCombatInteractions = true
            jugs[jug] = Roll()
            deps.worldRepo.spotanimMap(dust, tile)
            deps.worldRepo.soundArea(tile, ZebakSynths.BOULDER_LAND, radius = LAND_SOUND_RADIUS)
            for (player in targets) {
                if (player.coords != tile) continue
                player.hitTypeless(deps.random.of(LANDING_MIN, LANDING_MAX))
            }
        }
    }

    fun move(player: Player, jug: Npc, push: Boolean) {
        val roll = jugs[jug] ?: return
        if (roll.dx != 0 || roll.dz != 0) return
        val sign = if (push) 1 else -1
        val dx = Integer.signum(jug.coords.x - player.coords.x) * sign
        val dz = Integer.signum(jug.coords.z - player.coords.z) * sign
        if (dx == 0 && dz == 0) return
        setRolling(jug, roll, dx, dz, ROLL_DISTANCE)
        player.anim(ZebakSeqs.PLAYER_MOVE_JUG)
    }

    fun roll(jug: Npc, dz: Int) {
        val roll = jugs[jug] ?: return
        setRolling(jug, roll, 0, dz, Int.MAX_VALUE)
    }

    private fun setRolling(jug: Npc, roll: Roll, dx: Int, dz: Int, distance: Int) {
        if (roll.dx == 0 && roll.dz == 0) {
            jug.transmog(npcType(ZebakNpcs.JUG_ROLLING), Int.MAX_VALUE)
        }
        roll.dx = dx
        roll.dz = dz
        roll.left = distance
    }

    private fun stop(jug: Npc, roll: Roll) {
        roll.dx = 0
        roll.dz = 0
        jug.resetTransmog()
    }

    fun hit(jug: Npc) {
        val roll = jugs[jug] ?: return
        roll.dx = 0
        roll.dz = 0
        room.schedule(1) { shatter(jug, jug.coords) }
    }

    fun tick(jug: Npc) {
        val roll = jugs[jug] ?: return
        if (roll.dx == 0 && roll.dz == 0) return
        if (roll.left <= 0) {
            stop(jug, roll)
            return
        }
        val next = jug.coords.translate(roll.dx, roll.dz)
        when {
            room.boulders.isBoulder(next) -> shatter(jug, next)
            deps.collision.isWalkBlocked(next) || !onFloor(next) -> splashAway(jug)
            else -> {
                jug.walk(next)
                roll.left--
            }
        }
    }

    private fun onFloor(tile: CoordGrid): Boolean =
        tile.z in room.coords(ZebakCoords.WAVE_SOUTH).z..room.coords(ZebakCoords.WAVE_NORTH).z

    private fun splashAway(jug: Npc) {
        val tile = jug.coords
        deps.worldRepo.spotanimMap(spotanim(ZebakSpots.WATER_SPLASH), tile, height = SPLASH_HEIGHT)
        deps.worldRepo.soundArea(tile, ZebakSynths.JUG_OFF_FLOOR, radius = SPLASH_SOUND_RADIUS)
        remove(jug)
    }

    fun shatter(jug: Npc, centre: CoordGrid) {
        if (jug !in jugs) return
        remove(jug)
        deps.worldRepo.spotanimMap(spotanim(ZebakSpots.JUG_BREAK), centre)
        deps.worldRepo.soundArea(centre, ZebakSynths.JUG_BREAK, radius = BREAK_SOUND_RADIUS)
        val range = if (room.raid.isActive(ToaInvocationKey.UpsetStomach)) 1 else 2
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

    fun roarHit() {
        for (jug in jugs.keys) {
            jug.queueNpcHit(ROAR_HIT_DELAY, HitType.Typeless, ROAR_DAMAGE, NOOP_NPC_MODIFIER)
        }
    }

    fun attacking(player: Player, jug: Npc) {
        if (jug !in jugs) return
        val before = player.actionDelay
        room.schedule(1) {
            if (player.actionDelay <= before) return@schedule
            player.actionDelay = before
            if ((player.interaction as? InteractionNpc)?.target === jug) player.clearInteraction()
        }
    }

    private fun remove(jug: Npc) {
        jugs.remove(jug)
        room.despawn(jug)
    }

    fun clear() {
        for (jug in jugs.keys.toList()) remove(jug)
    }

    companion object {
        fun registerAttackOp() {
            val jug = npcType(ZebakNpcs.JUG)
            jug.actions = jug.actions.toBuilder().op(1, "Attack").build()
        }

        const val THROWN_MIN = 6
        const val THROWN_MAX = 8
        private const val LANDING_MIN = 2
        private const val LANDING_MAX = 5
        private const val LAND_SOUND_RADIUS = 5
        private const val BREAK_SOUND_RADIUS = 10
        private const val SPLASH_SOUND_RADIUS = 2
        private const val SPLASH_HEIGHT = 10
        private const val ROAR_DAMAGE = 5
        private const val ROAR_HIT_DELAY = 2

        private const val ROLL_DISTANCE = 8
    }
}
