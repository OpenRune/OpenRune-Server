package org.rsmod.content.bosses.brutus

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.SpotanimType
import jakarta.inject.Inject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.Condition
import org.rsmod.api.bosses.spec.DamageExpr
import org.rsmod.api.npc.apPlayer2
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.stat.baseHitpointsLvl
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.content.quest.area.lumbridge.idesofmilk.IdesOfMilkQuest
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

// https://oldschool.runescape.wiki/w/Brutus and https://oldschool.runescape.wiki/w/Brutus/Strategies
// Timings, sequences and tile patterns: live capture, rev 241, 20261012T180256-df49f48@1165-1980
// (the quest kill and five post-quest kills) and 20261012T200152-df49f48@723-943 (three more).
class Brutus
@Inject
constructor(
    deps: BossDeps,
    private val idesOfMilk: IdesOfMilkQuest,
    private val locRepo: LocRepository,
    private val aiPlayerInteractions: AiPlayerInteractions,
) : BossPluginScript(deps) {

    override fun ScriptContext.startup() {
        // live capture, rev 241, 20261012T180256-df49f48@1232: the death line on the killing hit.
        BossCombat.register(this, spec, deps, onLethal = { npc -> npc.say(DEATH_SAY) })
        deps.extensionRegistry.register(CHARGE_AIM) { ctx -> aimCharge(ctx.npc, ctx.target) }
        deps.extensionRegistry.register(CHARGE_LAND) { ctx -> landCharge(ctx.npc, ctx.target) }
        deps.extensionRegistry.register(STOMP_AIM) { ctx -> aimStomp(ctx.npc, ctx.target) }
        deps.extensionRegistry.register(STOMP_LAND) { ctx -> landStomp(ctx.npc, ctx.target) }
    }

    private fun onQuest(player: Player): Boolean =
        idesOfMilk.stage(player) < IdesOfMilkQuest.STAGE_COMPLETE

    /**
     * Basic attacks can't kill during the quest
     * (https://oldschool.runescape.wiki/w/The_Ides_of_Milk#Grabbing_the_bull_by_its_horns).
     */
    private val meleeCap =
        DamageExpr.Custom { _, target ->
            if (onQuest(target)) max(target.hitpoints - 1, 0) else Int.MAX_VALUE
        }

    // UNVERIFIED: special damage scaling. The wiki gives 15-19 and "scales with Hitpoints"; the
    // captures show 11-13 against 55 Hitpoints (live capture, rev 241,
    // 20261012T180256-df49f48@1740-1904, 20261012T200152-df49f48@837).
    private fun specialDamage(target: Player): Int {
        val level = target.baseHitpointsLvl
        val low = level * SPECIAL_LOW_PERCENT / 100
        val high = level * SPECIAL_HIGH_PERCENT / 100
        return deps.random.of(low, high).coerceIn(1, SPECIAL_MAX_HIT)
    }

    /**
     * The first special is random, then charge and stomp alternate; a charge that would run him
     * straight into a wall is a stomp instead
     * (https://oldschool.runescape.wiki/w/Brutus/Strategies).
     */
    private val chargeAllowed =
        Condition.Custom { npc, target ->
            target != null &&
                npc.vars[NEXT_SPECIAL] == CHARGE_TURN &&
                chargeRoom(npc.coords, chargeDirection(npc.coords, target.coords), deps.collision, ::diagonalWall) >=
                    MIN_CHARGE_ROOM
        }

    override val spec =
        boss(BRUTUS) {
            stats(attackRate = ATTACK_RATE)

            val melee =
                ability("melee") {
                    anim("seq.cow_boss_attack")
                    hit {
                        damage(DamageExpr.Min(DamageExpr.Accuracy(DamageExpr.Roll(0..MELEE_MAX)), meleeCap))
                        type(Melee)
                    }
                }

            val charge =
                ability("charge") {
                    say("*growls*")
                    anim("seq.cow_boss_charge", delay = CHARGE_ANIM_DELAY)
                    include(external(CHARGE_AIM))
                    wait(CHARGE_WINDUP)
                    include(external(CHARGE_LAND))
                    wait(CHARGE_RECOVERY)
                }

            val slam =
                sequence(
                    anim("seq.cow_boss_stomp", delay = STOMP_ANIM_DELAY),
                    say("*snorts*"),
                    external(STOMP_AIM),
                    wait(STOMP_WINDUP),
                    external(STOMP_LAND),
                    wait(STOMP_GAP - STOMP_WINDUP),
                )

            val stomp = ability("stomp") { include(repeat(STOMP_SLAMS, effect = slam)) }

            val pickSpecial =
                sequence(
                    whenever(
                        varnIs(NEXT_SPECIAL, 0),
                        oneOf(setVarn(NEXT_SPECIAL, CHARGE_TURN), setVarn(NEXT_SPECIAL, STOMP_TURN)),
                    ),
                    whenever(
                        chargeAllowed,
                        sequence(setVarn(NEXT_SPECIAL, STOMP_TURN), run(charge)),
                        sequence(setVarn(NEXT_SPECIAL, CHARGE_TURN), run(stomp)),
                    ),
                )
            val special = ability("special") { include(pickSpecial) }

            // live capture, rev 241, 20261012T180256-df49f48@1567-1943: two basic attacks open the
            // fight, then a special after every four basic attacks.
            // TODO(content): [verify:in_game] out of reach for 15 ticks: a charge through obstacles,
            // or "*huff*" (seq.cow_boss_heavy_breath) and a walk round them
            // (https://oldschool.runescape.wiki/w/Brutus/Strategies; live capture, rev 241,
            // 20261012T200152-df49f48@764, @819, @918).
            phase("fight") {
                rotationSelector {
                    +then(melee)
                    +then(melee)
                    +then(special)
                    +then(melee)
                    +then(melee)
                }
            }
        }

    /** Faces the tile five past his centre towards [target] and remembers the direction. */
    private fun aimCharge(npc: Npc, target: Player) {
        val (dx, dz) = chargeDirection(npc.coords, target.coords)
        npc.vars[AIM] = encodeAim(dx, dz)
        npc.faceSquare(centre(npc.coords).translate(dx * AIM_DISTANCE, dz * AIM_DISTANCE))
    }

    /**
     * The charge lands four ticks after the windup: Brutus jumps a few tiles in the aimed
     * direction, stopped only by walls, and hits a target standing anywhere in the swept lane.
     */
    private fun landCharge(npc: Npc, target: Player) {
        val (dx, dz) = decodeAim(npc.vars[AIM])
        val room = chargeRoom(npc.coords, dx to dz, deps.collision, ::diagonalWall)
        // UNVERIFIED: the charge length. The wiki says 4 tiles; the captures saw 4, 4, 4 and 5 in
        // the open and 3, 5 and 7 when a fence stopped him.
        val distance = min(deps.random.of(CHARGE_MIN, CHARGE_MAX), room)
        if (distance <= 0) {
            return
        }
        val start = npc.coords
        val dest = start.translate(dx * distance, dz * distance)
        if (inLane(start, dest, target.coords)) {
            target.queueHit(
                source = npc,
                delay = 1,
                type = HitType.Typeless,
                damage = specialDamage(target),
                modifier = deps.playerHitModifier,
            )
        }
        npc.telejump(deps.collision, dest)
        // The jump clears his interaction; he goes straight back for the target.
        npc.apPlayer2(target, aiPlayerInteractions)
    }

    private fun diagonalWall(tile: CoordGrid): Boolean =
        locRepo.findAll(tile).any { it.shape == LocShape.WallDiagonal }

    private fun aimStomp(npc: Npc, target: Player) {
        val (cx, cz) = stompAim(npc.coords, target.coords)
        npc.vars[AIM] = encodeAim(cx, cz)
        npc.faceSquare(centre(npc.coords).translate(cx * AIM_DISTANCE, cz * AIM_DISTANCE))
    }

    private fun landStomp(npc: Npc, target: Player) {
        val (cx, cz) = decodeAim(npc.vars[AIM])
        val slammed = stompTiles(npc.coords, cx, cz)
        npc.spotanim(STOMP_SELF_SPOTANIM)
        for (tile in slammed) {
            deps.worldRepo.spotanimMap(STOMP_TILE_SPOTANIM, tile)
        }
        if (target.coords in slammed || target.coords in beneathTiles(npc.coords, cx, cz)) {
            target.queueHit(
                source = npc,
                delay = 1,
                type = HitType.Typeless,
                damage = specialDamage(target),
                modifier = deps.playerHitModifier,
            )
        }
    }

    companion object {
        const val BRUTUS = "npc.cowboss"
        const val BRUTUS_SIZE = 3

        private const val NEXT_SPECIAL = "varn.brutus_next_special"
        private const val AIM = "varn.brutus_aim"
        private const val CHARGE_TURN = 1
        private const val STOMP_TURN = 2

        private const val CHARGE_AIM = "brutus.charge_aim"
        private const val CHARGE_LAND = "brutus.charge_land"
        private const val STOMP_AIM = "brutus.stomp_aim"
        private const val STOMP_LAND = "brutus.stomp_land"
        private const val DEATH_SAY = "m-moo..."
        private const val ATTACK_RATE = 5
        private const val MELEE_MAX = 3
        private const val AIM_DISTANCE = 5
        private const val CHARGE_ANIM_DELAY = 60
        private const val CHARGE_WINDUP = 4
        private const val CHARGE_RECOVERY = 4
        private const val CHARGE_MIN = 4
        private const val CHARGE_MAX = 7
        private const val MIN_CHARGE_ROOM = 3
        private const val STOMP_ANIM_DELAY = 30
        private const val STOMP_WINDUP = 4
        private const val STOMP_GAP = 8
        private const val STOMP_SLAMS = 3
        private const val SPECIAL_LOW_PERCENT = 20
        private const val SPECIAL_HIGH_PERCENT = 24
        private const val SPECIAL_MAX_HIT = 19

        private val STOMP_SELF_SPOTANIM = "spotanim.vfx_cowboss_stomp_impact02"
        private val STOMP_TILE_SPOTANIM by lazy {
            SpotanimType("spotanim.vfx_cowboss_stomp_impact01".asRSCM(RSCMType.SPOTANIM))
        }

        private fun centre(sw: CoordGrid): CoordGrid = sw.translate(BRUTUS_SIZE / 2, BRUTUS_SIZE / 2)

        private fun encodeAim(dx: Int, dz: Int): Int = (dx + 1) * 3 + (dz + 1)

        private fun decodeAim(code: Int): Pair<Int, Int> = code / 3 - 1 to code % 3 - 1

        /** The cardinal direction from Brutus at [sw] towards [player], along the longer axis. */
        fun chargeDirection(sw: CoordGrid, player: CoordGrid): Pair<Int, Int> {
            val centre = centre(sw)
            val dx = player.x - centre.x
            val dz = player.z - centre.z
            return if (abs(dx) >= abs(dz)) dx.sign to 0 else 0 to dz.sign
        }

        /**
         * How many tiles Brutus at [sw] can move in [direction] before a wall or fence stops him.
         * Scenery doesn't: he charges through plants and the sign
         * (https://oldschool.runescape.wiki/w/Brutus/Strategies).
         */
        fun chargeRoom(
            sw: CoordGrid,
            direction: Pair<Int, Int>,
            collision: CollisionFlagMap,
            diagonalWall: (CoordGrid) -> Boolean,
        ): Int {
            val (dx, dz) = direction
            if (dx == 0 && dz == 0) {
                return 0
            }
            val blocked = wallBlock(dx, dz)
            var room = 0
            var current = sw
            while (room < CHARGE_MAX) {
                val next = current.translate(dx, dz)
                val edge =
                    (0 until BRUTUS_SIZE).map {
                        if (dx != 0) {
                            next.translate(if (dx > 0) BRUTUS_SIZE - 1 else 0, it)
                        } else {
                            next.translate(it, if (dz > 0) BRUTUS_SIZE - 1 else 0)
                        }
                    }
                if (edge.any { blocks(collision[it.x, it.z, it.level], blocked) || diagonalWall(it) }) {
                    break
                }
                current = next
                room++
            }
            return room
        }

        // A diagonal fence fills its whole tile like scenery does, so a LOC tile only stops the
        // charge when it holds one.
        private fun blocks(flags: Int, wall: Int): Boolean = flags and wall != 0

        private fun wallBlock(dx: Int, dz: Int): Int {
            val wall =
                when {
                    dx < 0 -> CollisionFlag.WALL_EAST
                    dx > 0 -> CollisionFlag.WALL_WEST
                    dz < 0 -> CollisionFlag.WALL_NORTH
                    else -> CollisionFlag.WALL_SOUTH
                }
            return wall or CollisionFlag.BLOCK_WALK
        }

        /**
         * Whether [tile] lies in the lane Brutus sweeps from [start] to [dest]. The tiles he starts
         * on don't count: a player who walks under him avoids the charge
         * (https://oldschool.runescape.wiki/w/Brutus/Strategies).
         */
        fun inLane(start: CoordGrid, dest: CoordGrid, tile: CoordGrid): Boolean {
            val minX = min(start.x, dest.x)
            val minZ = min(start.z, dest.z)
            val maxX = max(start.x, dest.x) + BRUTUS_SIZE - 1
            val maxZ = max(start.z, dest.z) + BRUTUS_SIZE - 1
            val underStart = tile.x - start.x in 0 until BRUTUS_SIZE && tile.z - start.z in 0 until BRUTUS_SIZE
            return tile.level == start.level && tile.x in minX..maxX && tile.z in minZ..maxZ && !underStart
        }

        /**
         * Which way Brutus at [sw] stomps for a player at [player]: a side (one of the pair is 0)
         * when the player is level with its middle tile, otherwise the corner nearest the player.
         */
        fun stompAim(sw: CoordGrid, player: CoordGrid): Pair<Int, Int> {
            val max = BRUTUS_SIZE - 1
            val relX = player.x - sw.x
            val relZ = player.z - sw.z
            val sideX = if (relX < 0) -1 else if (relX > max) 1 else 0
            val sideZ = if (relZ < 0) -1 else if (relZ > max) 1 else 0
            val alongX = min(max(relX, 0), max)
            val alongZ = min(max(relZ, 0), max)
            val cornerX =
                when {
                    sideX != 0 -> sideX
                    sideZ != 0 && alongX == 0 -> -1
                    sideZ != 0 && alongX == max -> 1
                    else -> 0
                }
            val cornerZ =
                when {
                    sideZ != 0 -> sideZ
                    sideX != 0 && alongZ == 0 -> -1
                    sideX != 0 && alongZ == max -> 1
                    else -> 0
                }
            return cornerX to cornerZ
        }

        /**
         * The stomp's tiles around a [BRUTUS_SIZE] boss at [sw] for a player at [player]: the three
         * tiles along the side the player faces from its middle, or an L of five round the corner
         * nearest the player (the corner plus two tiles along each side). Live capture, rev 241,
         * 20261012T180256-df49f48@1740, @1828 (rows) and @1896, @1912 (L shapes).
         */
        fun stompTiles(sw: CoordGrid, player: CoordGrid): List<CoordGrid> {
            val (cx, cz) = stompAim(sw, player)
            return stompTiles(sw, cx, cz)
        }

        fun stompTiles(sw: CoordGrid, cx: Int, cz: Int): List<CoordGrid> {
            val max = BRUTUS_SIZE - 1
            if (cx == 0 || cz == 0) {
                return if (cz != 0) {
                    val z = if (cz < 0) -1 else BRUTUS_SIZE
                    (0..max).map { sw.translate(it, z) }
                } else {
                    val x = if (cx < 0) -1 else BRUTUS_SIZE
                    (0..max).map { sw.translate(x, it) }
                }
            }
            val edgeX = if (cx < 0) -1 else BRUTUS_SIZE
            val edgeZ = if (cz < 0) -1 else BRUTUS_SIZE
            val inX = if (cx < 0) 0 else max
            val inZ = if (cz < 0) 0 else max
            return listOf(
                sw.translate(edgeX, edgeZ),
                sw.translate(inX, edgeZ),
                sw.translate(inX - cx, edgeZ),
                sw.translate(edgeX, inZ),
                sw.translate(edgeX, inZ - cz),
            )
        }

        /**
         * The tiles under Brutus next to the slammed ones, which the slam also hits
         * (https://oldschool.runescape.wiki/w/Brutus/Strategies). UNVERIFIED: exactly which tiles.
         */
        fun beneathTiles(sw: CoordGrid, cx: Int, cz: Int): List<CoordGrid> {
            val max = BRUTUS_SIZE - 1
            val inX = if (cx < 0) 0 else max
            val inZ = if (cz < 0) 0 else max
            return when {
                cx == 0 -> (0..max).map { sw.translate(it, inZ) }
                cz == 0 -> (0..max).map { sw.translate(inX, it) }
                else -> listOf(sw.translate(inX, inZ), sw.translate(inX - cx, inZ), sw.translate(inX, inZ - cz))
            }
        }
    }
}
