package org.rsmod.content.bosses.brutus

import jakarta.inject.Inject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.DamageExpr
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.stat.baseHitpointsLvl
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.route.StepFactory
import org.rsmod.content.quest.area.lumbridge.idesofmilk.IdesOfMilkQuest
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.hit.HitType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionStrategy

// https://oldschool.runescape.wiki/w/Brutus and https://oldschool.runescape.wiki/w/Brutus/Strategies
// Timings, sequences and tile patterns: live capture, rev 241, 20261012T180256-df49f48@1165-1980
// (the quest kill and five post-quest kills).
class Brutus
@Inject
constructor(
    deps: BossDeps,
    private val stepFactory: StepFactory,
    private val idesOfMilk: IdesOfMilkQuest,
) : BossPluginScript(deps) {

    override fun ScriptContext.startup() {
        // live capture, rev 241, 20261012T180256-df49f48@1232: the death line on the killing hit.
        BossCombat.register(this, spec, deps, onLethal = { npc -> npc.say(DEATH_SAY) })
        deps.extensionRegistry.register(CHARGE) { ctx -> charge(ctx.npc, ctx.target) }
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
    // capture shows 11-13 against 55 Hitpoints (live capture, rev 241,
    // 20261012T180256-df49f48@1740-1904).
    private fun specialDamage(target: Player): Int {
        val level = target.baseHitpointsLvl
        val low = level * SPECIAL_LOW_PERCENT / 100
        val high = level * SPECIAL_HIGH_PERCENT / 100
        return deps.random.of(low, high).coerceIn(1, SPECIAL_MAX_HIT)
    }

    private val specialHit = DamageExpr.Custom { _, target -> specialDamage(target) }

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
                    anim("seq.cow_boss_charge")
                    faceTarget()
                    wait(CHARGE_WINDUP)
                    include(external(CHARGE))
                    wait(CHARGE_RECOVERY)
                }

            val slam =
                sequence(
                    anim("seq.cow_boss_stomp"),
                    onTiles(
                        customTiles(ARENA) { npc, target, _ -> stompTiles(npc.coords, target.coords) },
                        after(
                            STOMP_WINDUP,
                            sequence(
                                mapSpotanim("spotanim.vfx_cowboss_stomp_impact01", CurrentTile),
                                hit {
                                    target = playersOn(CurrentTile)
                                    damage(specialHit)
                                    type(Typeless)
                                    hazard()
                                },
                            ),
                        ),
                    ),
                    after(STOMP_WINDUP, spotanim("spotanim.vfx_cowboss_stomp_impact02")),
                    wait(STOMP_GAP),
                )

            // UNVERIFIED: whether every slam of a burst repeats the "*snorts*" text.
            val stomp =
                ability("stomp") {
                    say("*snorts*")
                    include(repeat(STOMP_SLAMS, effect = slam))
                }

            // live capture, rev 241, 20261012T180256-df49f48@1567-1943: two basic attacks open the
            // fight, then specials alternate with four basic attacks between them.
            // TODO(content): [verify:in_game] the safespot rules (stomp when one tile away, charge
            // after ~15 ticks out of reach) and stomping first when a charge would hit a wall.
            phase("fight") {
                rotationSelector {
                    +then(melee)
                    +then(melee)
                    +then(charge)
                    +then(melee)
                    +then(melee)
                    +then(melee)
                    +then(melee)
                    +then(stomp)
                    +then(melee)
                    +then(melee)
                }
            }
        }

    /**
     * The charge: a cardinal dash towards [target] that hits anyone in the swept lane. It's resolved
     * before the move, so a player who stepped out during the windup is safe.
     */
    private fun charge(npc: Npc, target: Player) {
        val centre = npc.coords.translate(1, 1)
        val dx = target.coords.x - centre.x
        val dz = target.coords.z - centre.z
        val (stepX, stepZ) = if (abs(dx) >= abs(dz)) dx.sign to 0 else 0 to dz.sign
        if (stepX == 0 && stepZ == 0) {
            return
        }
        // UNVERIFIED: the charge length (wiki: 4 tiles; the capture saw 4-7, some against the fence)
        val dest = npc.coords.translate(stepX * CHARGE_TILES, stepZ * CHARGE_TILES)
        val path =
            stepFactory.createPath(
                npc.coords,
                dest,
                size = BRUTUS_SIZE,
                collision = CollisionStrategy.Normal,
            )
        val end = path.lastOrNull() ?: return
        val swept = path.flatMap { footprint(it) }.toSet()
        if (target.coords in swept) {
            target.queueHit(
                source = npc,
                delay = 1,
                type = HitType.Typeless,
                damage = specialDamage(target),
                modifier = deps.playerHitModifier,
            )
        }
        PathingEntityCommon.exactMove(
            npc,
            npc.coords,
            end,
            delay1 = 0,
            delay2 = CHARGE_CLIENT_CYCLES,
            dir = facing(stepX, stepZ),
            collision = deps.collision,
        )
    }

    private fun footprint(sw: CoordGrid): List<CoordGrid> =
        (0 until BRUTUS_SIZE).flatMap { x -> (0 until BRUTUS_SIZE).map { z -> sw.translate(x, z) } }

    private fun facing(stepX: Int, stepZ: Int): Int =
        when {
            stepX < 0 -> FACE_WEST
            stepX > 0 -> FACE_EAST
            stepZ > 0 -> FACE_NORTH
            else -> FACE_SOUTH
        }

    companion object {
        const val BRUTUS = "npc.cowboss"
        const val BRUTUS_SIZE = 3

        private const val CHARGE = "brutus.charge"
        private const val DEATH_SAY = "m-moo..."
        private const val ATTACK_RATE = 5
        private const val MELEE_MAX = 3
        private const val CHARGE_WINDUP = 4
        private const val CHARGE_RECOVERY = 4
        private const val CHARGE_TILES = 4
        private const val CHARGE_CLIENT_CYCLES = 60
        private const val STOMP_WINDUP = 4
        private const val STOMP_GAP = 8
        private const val STOMP_SLAMS = 3
        private const val SPECIAL_LOW_PERCENT = 20
        private const val SPECIAL_HIGH_PERCENT = 24
        private const val SPECIAL_MAX_HIT = 19

        private const val FACE_SOUTH = 0
        private const val FACE_WEST = 512
        private const val FACE_NORTH = 1024
        private const val FACE_EAST = 1536

        private val ARENA = area(spawnTile(-12, -12), spawnTile(12, 12))

        /**
         * The stomp's tiles around a [BRUTUS_SIZE] boss at [sw]: the three tiles along the side
         * the player faces from its middle, or an L of five round the corner nearest the player
         * (the corner plus two tiles along each side). Live capture, rev 241,
         * 20261012T180256-df49f48@1740, @1828 (rows) and @1896, @1912 (L shapes).
         */
        fun stompTiles(sw: CoordGrid, player: CoordGrid): List<CoordGrid> {
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
            if (cornerX == 0 || cornerZ == 0) {
                return if (sideZ != 0) {
                    val z = if (sideZ < 0) -1 else BRUTUS_SIZE
                    (0..max).map { sw.translate(it, z) }
                } else {
                    val x = if (sideX < 0) -1 else BRUTUS_SIZE
                    (0..max).map { sw.translate(x, it) }
                }
            }
            val edgeX = if (cornerX < 0) -1 else BRUTUS_SIZE
            val edgeZ = if (cornerZ < 0) -1 else BRUTUS_SIZE
            val inX = if (cornerX < 0) 0 else max
            val inZ = if (cornerZ < 0) 0 else max
            val stepX = -cornerX
            val stepZ = -cornerZ
            return listOf(
                sw.translate(edgeX, edgeZ),
                sw.translate(inX, edgeZ),
                sw.translate(inX + stepX, edgeZ),
                sw.translate(edgeX, inZ),
                sw.translate(edgeX, inZ + stepZ),
            )
        }
    }
}
