package org.rsmod.content.minigames.gauntlet.hunllef

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.IdentityHashMap
import kotlin.random.Random
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.content.minigames.gauntlet.GauntletLighting
import org.rsmod.content.minigames.gauntlet.GauntletZones
import org.rsmod.content.minigames.gauntlet.layout.GauntletRoom
import org.rsmod.content.minigames.gauntlet.layout.Tile
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.region.Region
import org.rsmod.game.region.util.RegionRotations
import org.rsmod.map.CoordGrid

@Singleton
class HunllefFloor
@Inject
constructor(private val deps: BossDeps, private val locRepo: LocRepository) {
    private enum class State(val suffix: String) {
        Normal(""),
        Warning("_warning"),
        Hit("_hit"),
    }

    private class Session(
        val boss: Npc,
        val player: Player,
        val corrupted: Boolean,
        val tiles: Map<Tile, CoordGrid>,
    ) {
        var stopped = false
        var lastPattern: Set<Tile>? = null
        var pattern: Set<Tile> = emptySet()
        var timing = FloorTiming(0, 0)
        var step = 0
    }

    private val sessions = IdentityHashMap<Npc, Session>()

    private val floorIds: Set<Int> by lazy {
        buildSet {
            for (state in State.entries) {
                for (suffix in listOf("", "_hm")) {
                    add("loc.prif_gauntlet_floor_tile_01${state.suffix}$suffix".asRSCM(RSCMType.LOC))
                }
            }
        }
    }

    fun start(boss: Npc, player: Player, corrupted: Boolean, region: Region, room: GauntletRoom) {
        stop(boss)
        val tiles =
            buildMap {
                for (x in 0 until HunllefPatterns.SIZE) {
                    for (z in 0 until HunllefPatterns.SIZE) {
                        put(Tile(x, z), resolve(region, room, Tile(x, z)))
                    }
                }
            }
        val session = Session(boss, player, corrupted, tiles)
        sessions[boss] = session
        deps.worldQueues.add(FIRST_WARNING_DELAY) { if (!session.stopped) beginPattern(session) }
    }

    fun stop(boss: Npc) {
        val session = sessions.remove(boss) ?: return
        session.stopped = true
        paint(session, session.pattern, State.Normal)
    }

    fun outerRingCoords(player: Player): List<CoordGrid> {
        val session = sessions.values.firstOrNull { it.player === player } ?: return emptyList()
        return HunllefPatterns.outerRing().mapNotNull { session.tiles[it] }
    }

    fun stopFor(player: Player) {
        sessions.values.filter { it.player === player }.forEach { stop(it.boss) }
    }

    private fun beginPattern(session: Session) {
        val stage = HunllefStage.forHp(hpFraction(session.boss))
        val options = HunllefPatterns.patterns(stage).filter { it !== session.lastPattern }
        session.pattern = options[Random.nextInt(options.size)]
        session.lastPattern = session.pattern
        session.timing =
            HunllefPatterns.timing(session.corrupted, stage, hpFraction(session.boss))
        session.step = 0
        paint(session, session.pattern, State.Warning)
        schedule(session)
    }

    private fun schedule(session: Session) {
        deps.worldQueues.add(1) { advance(session) }
    }

    private fun advance(session: Session) {
        if (session.stopped) return
        if (!session.boss.isSlotAssigned || session.boss.hitpoints <= 0) {
            stop(session.boss)
            return
        }
        session.step++
        val timing = session.timing
        if (session.step == timing.warning) paint(session, session.pattern, State.Hit)
        if (session.step >= timing.warning) damage(session)
        if (session.step >= timing.cycle) {
            paint(session, session.pattern, State.Normal)
            beginPattern(session)
            return
        }
        schedule(session)
    }

    private fun damage(session: Session) {
        val player = session.player
        if (player.hitpoints <= 0) return
        val standing = session.pattern.any { session.tiles[it] == player.coords }
        if (!standing) return
        val damage = Random.nextInt(FLOOR_MIN_DAMAGE, FLOOR_MAX_DAMAGE + 1)
        player.queueHit(session.boss, 1, HitType.Typeless, damage, deps.playerHitModifier)
    }

    private fun paint(session: Session, pattern: Set<Tile>, state: State) {
        val suffix = if (session.corrupted) "_hm" else ""
        val target = "loc.prif_gauntlet_floor_tile_01${state.suffix}$suffix"
        for (tile in pattern) {
            val coords = session.tiles[tile] ?: continue
            val current = locRepo.findAll(coords).firstOrNull { it.id in floorIds } ?: continue
            locRepo.add(coords, target, Int.MAX_VALUE, current.angle, current.shape)
        }
    }

    private fun hpFraction(boss: Npc): Double =
        boss.hitpoints.toDouble() / boss.baseHitpointsLvl.coerceAtLeast(1)

    private fun resolve(region: Region, room: GauntletRoom, tile: Tile): CoordGrid {
        val rotated =
            RegionRotations.translateZone(
                room.rotation,
                ARENA_ORIGIN + tile.x,
                ARENA_ORIGIN + tile.z,
                GauntletLighting.ROOM_TILES,
                GauntletLighting.ROOM_TILES,
            )
        return CoordGrid(
            region.southWest.x + room.x * GauntletLighting.ROOM_TILES + rotated.x,
            region.southWest.z + room.z * GauntletLighting.ROOM_TILES + rotated.z,
            GauntletZones.WALKABLE_LEVEL,
        )
    }

    private companion object {
        const val ARENA_ORIGIN = 2
        const val FIRST_WARNING_DELAY = 7
        const val FLOOR_MIN_DAMAGE = 10
        const val FLOOR_MAX_DAMAGE = 20
    }
}
