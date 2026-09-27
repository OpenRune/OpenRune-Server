package org.rsmod.content.raids.toa.raid.encounter.crondis.zebak

import org.rsmod.api.npc.hit.queueHit as queueNpcHit
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.interact.InteractionNpc
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.map.CoordGrid

/**
 * Jugs (Offline_Scape CrondisJug / JugPushAction). Push or Pull sets one rolling a tile a tick,
 * diagonally too; the standing jug becomes the rolling jug npc. It shatters on a boulder or a tick
 * after a hit, splashes away at the floor's edge, and clears acid when it shatters: 5x5, or 3x3
 * with Upset Stomach.
 */
internal class ZebakJugs(private val room: ZebakEncounter) {
    private val deps = room.raid.deps
    private val jugs = HashMap<Npc, Roll>()

    /** Rolling direction (0/0 while standing) and the tiles it has left to roll. */
    private class Roll(var dx: Int = 0, var dz: Int = 0, var left: Int = 0)

    val npcs: Collection<Npc>
        get() = jugs.keys

    /** Jugs thrown by a special; anyone underneath takes 2-5. */
    fun land(tiles: List<CoordGrid>, targets: List<Player>) {
        val dust = spotanim(ZebakSpots.DUST)
        for (tile in tiles) {
            val jug = room.spawn(ZebakNpcs.JUG, tile)
            // Capture: a hit jug never turns or moves toward its attacker. Without this, the
            // standard retaliation paths it at the player in the tick before it breaks.
            jug.ignoreCombatInteractions = true
            jugs[jug] = Roll()
            deps.worldRepo.spotanimMap(dust, tile)
            // Capture: the same landing sound as a boulder.
            deps.worldRepo.soundArea(tile, ZebakSynths.BOULDER_LAND, radius = LAND_SOUND_RADIUS)
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
        setRolling(jug, roll, dx, dz, ROLL_DISTANCE)
        player.anim(ZebakSeqs.PLAYER_MOVE_JUG)
    }

    /**
     * A wave sets [jug] rolling its way, even if it already rolls (Offline_Scape WaveNPC).
     * Capture: wave jugs rolled 11 and 18 tiles to the last row, so they have no limit.
     */
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

    /** Back to a standing jug, which can be pushed or pulled again. */
    private fun stop(jug: Npc, roll: Roll) {
        roll.dx = 0
        roll.dz = 0
        jug.resetTransmog()
    }

    /**
     * Capture: any hit breaks a jug, a tick after the hitsplat, where it stands; a rolling jug
     * stops. Rolling jugs can be attacked too.
     */
    fun hit(jug: Npc) {
        val roll = jugs[jug] ?: return
        roll.dx = 0
        roll.dz = 0
        room.schedule(1) { shatter(jug, jug.coords) }
    }

    /**
     * Once a tick (config `timer = 1`). The boulder's tile blocks walking, so a jug shatters beside
     * it with the splash centred on the boulder (Offline_Scape rolled onto it; same tiles cleared).
     * Capture: waves roll jugs to the last row (z 5397 / 5419), where they splash away on their own
     * tile. A push or pull rolls [ROLL_DISTANCE] tiles and stops (Offline_Scape never stopped).
     */
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

    /** Water flies to each acid tile in range, cleared a tick later. */
    fun shatter(jug: Npc, centre: CoordGrid) {
        if (jug !in jugs) return
        remove(jug)
        deps.worldRepo.spotanimMap(spotanim(ZebakSpots.JUG_BREAK), centre)
        deps.worldRepo.soundArea(centre, ZebakSynths.JUG_BREAK, radius = BREAK_SOUND_RADIUS)
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

    /**
     * Capture: the roar's first wave (T+36) puts a 5 on every jug at T+37, and each breaks at T+38
     * through [hit]. A real hit rather than a bare hitsplat, so the headbar drops too. The delay is
     * 2 because this runs in the world tick, before npc queues count down that same tick.
     */
    fun roarHit() {
        for (jug in jugs.keys) {
            jug.queueNpcHit(ROAR_HIT_DELAY, HitType.Typeless, ROAR_DAMAGE, NOOP_NPC_MODIFIER)
        }
    }

    /**
     * One attack per click, with no attack delay afterwards. OSRS Wiki (Zebak, Changes):
     * - 1 Sep 2022: jugs no longer have an attack delay if attacked before Zebak;
     * - 28 Sep 2022: jugs got an attack delay so a player attacks them only once, saving ammo.
     *   Without it, no delay meant attacking the jug again every tick until it broke.
     *
     * The capture's shots at ticks 216 and 217 rule out a normal weapon delay, so the later change
     * is read as ending the attack on the jug rather than delaying the player: the attack delay is
     * put back and the combat with that jug stops. An existing delay still holds the attack back
     * (PvNCombat checks it first).
     *
     * PvNCombat asks the attack hooks ([ZebakJugAttackHook]) just before it sets the delay, so the
     * state before the attack is saved here and restored at the start of the next tick, which runs
     * before any player acts. A raised delay is the sign the attack happened: a player still
     * waiting out an older delay keeps the interaction, so the attack goes ahead once it's over.
     * Another click in between replaces the interaction, so only one still aimed at this jug is
     * cleared.
     */
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
        /**
         * Server-only op2 so PvNCombat accepts the standing jug; the client still shows
         * Push/Pull/Hit.
         */
        fun registerAttackOp() {
            val jug = npcType(ZebakNpcs.JUG)
            jug.actions = jug.actions.toBuilder().op(1, "Attack").build()
        }

        /** How many jugs a special throws. */
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

        /** A push or pull (checked in game). */
        private const val ROLL_DISTANCE = 8
    }
}
