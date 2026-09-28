package org.rsmod.content.bosses.callisto

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ProjAnimType
import dev.openrune.types.aconverted.SpotanimType
import jakarta.inject.Inject
import kotlin.math.sign
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.runtime.encounter
import org.rsmod.api.bosses.runtime.repeatTick
import org.rsmod.api.bosses.spec.Condition
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.bosses.spec.ProjectileConfig
import org.rsmod.api.npc.events.NpcHitEvents
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.player.hit.modifier.PlayerHitModifier
import org.rsmod.api.player.hit.modify
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.hit.queueImpactHit
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.player.output.CamShakeAxis
import org.rsmod.api.player.output.Camera
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onEvent
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.game.proj.ProjAnim
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.StepValidator

data class BearDen(
    val key: String,
    val bossNpc: String,
    val multi: Boolean,
    val attackRate: Int,
    val meleeHit: IntRange,
    val rangedMaxHit: Int,
    val trapHit: IntRange,
    val roarSpotanim: String,
    val minX: Int,
    val maxX: Int,
    val minZ: Int,
    val maxZ: Int,
    val trapMinX: Int,
    val trapMaxX: Int,
    val trapMinZ: Int,
    val trapMaxZ: Int,
) {
    fun contains(coord: CoordGrid): Boolean =
        coord.level == LEVEL && coord.x in minX..maxX && coord.z in minZ..maxZ

    companion object {
        const val LEVEL = 0
    }
}

val ARTIO_DEN =
    BearDen(
        key = "artio",
        bossNpc = "npc.callisto_singles",
        multi = false,
        attackRate = 5,
        meleeHit = 15..35,
        rangedMaxHit = 17,
        trapHit = 5..8,
        roarSpotanim = "spotanim.fx_callisto_enrage_small",
        minX = 1739,
        maxX = 1777,
        minZ = 11525,
        maxZ = 11562,
        trapMinX = 1751,
        trapMaxX = 1767,
        trapMinZ = 11533,
        trapMaxZ = 11552,
    )

val CALLISTO_DEN =
    BearDen(
        key = "callisto",
        bossNpc = "npc.callisto",
        multi = true,
        attackRate = 4,
        meleeHit = 35..55,
        rangedMaxHit = 31,
        trapHit = 8..15,
        roarSpotanim = "spotanim.fx_callisto_enrage",
        minX = 3341,
        maxX = 3377,
        minZ = 10309,
        maxZ = 10347,
        trapMinX = 3351,
        trapMaxX = 3367,
        trapMinZ = 10317,
        trapMaxZ = 10336,
    )

class Artio
@Inject
constructor(deps: BossDeps, locRepo: LocRepository) : WildernessBear(ARTIO_DEN, deps, locRepo)

class Callisto
@Inject
constructor(deps: BossDeps, locRepo: LocRepository) : WildernessBear(CALLISTO_DEN, deps, locRepo)

abstract class WildernessBear(
    private val den: BearDen,
    deps: BossDeps,
    private val locRepo: LocRepository,
) : BossPluginScript(deps) {

    private val postAttackHandler = "${den.key}.post_attack"
    private val magicHandler = "${den.key}.magic"
    private val bossId by lazy { den.bossNpc.asRSCM(RSCMType.NPC) }
    private val traps = mutableMapOf<CoordGrid, LocInfo>()

    override val spec =
        boss(den.bossNpc) {
            stats(attackRate = den.attackRate, aggressionRadius = AGGRO_RANGE)

            val melee =
                ability(MELEE) {
                    anim("seq.npc_callisto_attack_melee01")
                    hit {
                        damage(den.meleeHit).roll()
                        type(Melee)
                        penetration(MELEE_PRAYER_PENETRATION)
                        target =
                            if (den.multi) AllInRadius(radius = 1, of = CurrentTarget)
                            else CurrentTarget
                    }
                    include(external(postAttackHandler))
                }

            val ranged =
                ability(RANGED) {
                    anim("seq.npc_callisto_attack_ranged01")
                    spotanim("spotanim.fx_callisto_ranged_spotanim")
                    include(
                        onEach(
                            AllInRadius(radius = ARENA_RADIUS),
                            Effect.Projectile(
                                spotanim = "spotanim.fx_callisto_ranged_projectile",
                                config = RANGED_PROJECTILE_CONFIG,
                                hit =
                                    Effect.Hit(
                                        damage = Accuracy(Roll(0..den.rangedMaxHit)),
                                        type = Ranged,
                                        spotanim = "spotanim.fx_callisto_ranged_impact",
                                    ),
                            ),
                        )
                    )
                    include(external(postAttackHandler))
                }

            val magic =
                ability(MAGIC) {
                    anim("seq.npc_callisto_attack_magic01")
                    include(external(magicHandler))
                    include(external(postAttackHandler))
                }

            phase(PHASE_FIGHT) {
                weightedSelectorRandom(noRepeatBias = 0.0) {
                    +random(melee, weight = MELEE_WEIGHT, requires = Condition.Not(WithinMeleeRange))
                    +random(ranged, weight = RANGED_WEIGHT)
                    +random(magic, weight = MAGIC_WEIGHT)
                }
            }
        }

    override fun ScriptContext.startup() {
        BossCombat.register(
            this,
            spec,
            deps,
            onLethal = { deps.worldQueues.add(DEATH_ANIM_TICKS) { springAllTraps() } },
        )
        deps.extensionRegistry.register(postAttackHandler) { _, npc, _, _ -> onAttack(npc) }
        deps.extensionRegistry.register(magicHandler) { _, npc, target, _ ->
            launchMagicOrb(npc, target)
        }
        onEvent<NpcStateEvents.Spawn>(bossId) { resetFight(npc) }
        onEvent<NpcStateEvents.Respawn> { if (npc.id == bossId) resetFight(npc) }
        onEvent<NpcHitEvents.Impact>(bossId) { checkRoar(npc) }
    }

    private fun resetFight(npc: Npc) {
        npc.vars[ROARS_VARN] = 0
    }

    private fun checkRoar(npc: Npc) {
        if (npc.hitpoints <= 0) return
        val roars = npc.vars[ROARS_VARN]
        val threshold = ROAR_THRESHOLDS.getOrNull(roars) ?: return
        if (npc.hitpoints * 100 >= npc.baseHitpointsLvl * threshold) return

        npc.vars[ROARS_VARN] = roars + 1
        npc.anim("seq.npc_callisto_enraged01")
        npc.spotanim(den.roarSpotanim)
        deps.encounter(npc).lastAbilityTick = deps.mapClock.cycle
        deps.worldQueues.add(1) { if (npc.isValidTarget()) deployTraps(npc) }
    }

    private fun onAttack(npc: Npc) {
        if (npc.vars[ROARS_VARN] == 0) return
        deps.worldQueues.add(1) { if (npc.isValidTarget()) deployTraps(npc) }
    }

    private fun deployTraps(npc: Npc) {
        val tiles = pickTrapTiles(npc)
        if (tiles.isEmpty()) return
        val telegraph = SpotanimType("spotanim.fx_callisto_trap".asRSCM(RSCMType.SPOTANIM))
        tiles.forEach { deps.worldRepo.spotanimMap(telegraph, it) }
        deps.worldQueues.add(1) {
            if (!npc.isValidTarget()) return@add
            val placed = tiles.filter { it !in traps }
            for (tile in placed) {
                traps[tile] =
                    locRepo.add(
                        tile,
                        TRAP_LOC,
                        TRAP_DURATION,
                        LocAngle.West,
                        LocShape.CentrepieceStraight,
                        onDespawn = { traps.remove(tile) },
                    )
            }
            watchTraps(npc, placed.toSet())
        }
    }

    private fun pickTrapTiles(npc: Npc): Set<CoordGrid> {
        val tiles = mutableSetOf<CoordGrid>()
        var attempts = 0
        while (tiles.size < TRAPS_PER_BATCH && attempts++ < TRAP_TILE_ATTEMPTS) {
            val x = den.trapMinX + deps.random.of(den.trapMaxX - den.trapMinX + 1)
            val z = den.trapMinZ + deps.random.of(den.trapMaxZ - den.trapMinZ + 1)
            val tile = CoordGrid(x, z, BearDen.LEVEL)
            if (tile in traps || deps.collision.isWalkBlocked(tile)) continue
            if (npc.isWithinDistance(tile, 0)) continue
            tiles += tile
        }
        return tiles
    }

    private fun watchTraps(npc: Npc, batch: Set<CoordGrid>) {
        val armed = batch.toMutableSet()
        deps.repeatTick(
            ticks = TRAP_DURATION,
            onTick = { _ ->
                armed.retainAll(traps.keys)
                if (armed.isEmpty()) return@repeatTick false
                for (player in deps.playerList) {
                    val tile = player.coords
                    if (tile !in armed || player.hitpoints <= 0) continue
                    armed -= tile
                    springTrap(npc, player, tile)
                }
                true
            },
        )
    }

    private fun springTrap(npc: Npc, player: Player, tile: CoordGrid) {
        val loc = traps.remove(tile) ?: return
        deps.worldRepo.locAnim(loc, TRAP_ACTIVATE_SEQ)
        deps.worldQueues.add(1) { locRepo.del(loc, Int.MAX_VALUE) }
        player.mes(TRAP_MESSAGE)
        player.frozen = true
        player.routeDestination.clear()
        player.timer("timer.combat_freeze", TRAP_BIND_TICKS)
        val damage = den.trapHit.first + deps.random.of(den.trapHit.last - den.trapHit.first + 1)
        player.queueHit(npc, 1, HitType.Typeless, damage, deps.playerHitModifier)
    }

    private fun springAllTraps() {
        val sprung = traps.values.toList()
        traps.clear()
        sprung.forEach { deps.worldRepo.locAnim(it, TRAP_ACTIVATE_SEQ) }
        deps.worldQueues.add(1) { sprung.forEach { locRepo.del(it, Int.MAX_VALUE) } }
    }

    private fun launchMagicOrb(npc: Npc, target: Player) {
        val spotanim = "spotanim.windblast_travel".asRSCM(RSCMType.SPOTANIM)
        val proj = ProjAnim.fromNpcToPlayer(npc, target, spotanim, MAGIC_PROJECTILE)
        deps.worldRepo.projAnim(proj)
        target.queueImpactHit(npc, proj.serverCycles, HitType.Magic, 0, orbImpact(npc))
    }

    private fun orbImpact(npc: Npc) = PlayerHitModifier { target ->
        if (npc.isValidTarget() && target.vars[PROTECT_FROM_MAGIC] != 1) {
            damage = knockBack(npc, target)
            if (den.multi) knockBackSplashed(npc, target)
        }
        deps.playerHitModifier.modify(this, target)
    }

    private fun knockBackSplashed(npc: Npc, target: Player) {
        for (other in deps.playerList) {
            if (other === target || !other.isValidTarget()) continue
            if (other.coords.level != target.coords.level) continue
            if (other.coords.chebyshevDistance(target.coords) > 1) continue
            if (other.vars[PROTECT_FROM_MAGIC] == 1) continue
            val damage = knockBack(npc, other)
            other.queueHit(npc, 1, HitType.Magic, damage, deps.playerHitModifier)
        }
    }

    /** Pushes [player] up to 3 tiles away from [npc] and returns the damage for how far they flew. */
    private fun knockBack(npc: Npc, player: Player): Int {
        val centre = npc.coords.translate(npc.size / 2, npc.size / 2)
        val dx = (player.coords.x - centre.x).sign
        val dz = (player.coords.z - centre.z).sign.let { if (it == 0 && dx == 0) 1 else it }

        val steps = StepValidator(deps.collision)
        val start = player.coords
        var dest = start
        var moved = 0
        while (moved < KNOCKBACK_TILES) {
            if (!steps.canTravel(dest.level, dest.x, dest.z, dx, dz)) break
            dest = dest.translate(dx, dz)
            moved++
        }

        PathingEntityCommon.exactMove(
            player,
            start,
            dest,
            EXACTMOVE_DELAY1,
            EXACTMOVE_DELAY2,
            bearing(-dx, -dz),
            deps.collision,
        )
        player.anim(FLYBACK_SEQ, delay = FLYBACK_SEQ_DELAY)
        Camera.camShake(player, CamShakeAxis.FORWARDS_BACKWARDS, random = 2, amplitude = 10, rate = 10)
        player.frozen = true
        player.routeDestination.clear()
        player.timer("timer.combat_freeze", KNOCKBACK_STUN_TICKS)
        deps.worldQueues.add(KNOCKBACK_STUN_TICKS) {
            if (player.isSlotAssigned) Camera.camReset(player)
        }
        return KNOCKBACK_DAMAGE[moved]
    }

    private fun bearing(dx: Int, dz: Int): Int =
        when {
            dx == 0 && dz < 0 -> SOUTH
            dx < 0 && dz < 0 -> SOUTH_WEST
            dx < 0 && dz == 0 -> WEST
            dx < 0 && dz > 0 -> NORTH_WEST
            dx == 0 && dz > 0 -> NORTH
            dx > 0 && dz > 0 -> NORTH_EAST
            dx > 0 && dz == 0 -> EAST
            else -> SOUTH_EAST
        }

    private companion object {
        private const val PHASE_FIGHT = "fight"
        private const val MELEE = "melee"
        private const val RANGED = "ranged"
        private const val MAGIC = "magic"

        private const val ROARS_VARN = "varn.callisto_roars"

        private const val AGGRO_RANGE = 15
        private const val ARENA_RADIUS = 20
        private const val MELEE_PRAYER_PENETRATION = 50
        private const val MELEE_WEIGHT = 250
        private const val RANGED_WEIGHT = 6
        private const val MAGIC_WEIGHT = 1
        private val ROAR_THRESHOLDS = listOf(66, 33)

        private const val TRAP_LOC = "loc.callisto_trap_loc"
        private const val TRAP_ACTIVATE_SEQ = "seq.trap_bear01_activate"
        private const val TRAP_MESSAGE = "<col=ef1020>The bear trap immobilizes you.</col>"
        private const val TRAPS_PER_BATCH = 5
        private const val TRAP_TILE_ATTEMPTS = 50
        private const val TRAP_DURATION = 49
        private const val TRAP_BIND_TICKS = 4
        private const val DEATH_ANIM_TICKS = 4

        private const val PROTECT_FROM_MAGIC = "varbit.prayer_protectfrommagic"
        private const val KNOCKBACK_TILES = 3
        private val KNOCKBACK_DAMAGE = intArrayOf(50, 35, 20, 5)
        private const val FLYBACK_SEQ = "seq.human_troll_flyback_withsound"
        private const val FLYBACK_SEQ_DELAY = 20
        private const val EXACTMOVE_DELAY1 = 10
        private const val EXACTMOVE_DELAY2 = 30
        private const val KNOCKBACK_STUN_TICKS = 3

        private const val SOUTH = 0
        private const val SOUTH_WEST = 256
        private const val WEST = 512
        private const val NORTH_WEST = 768
        private const val NORTH = 1024
        private const val NORTH_EAST = 1280
        private const val EAST = 1536
        private const val SOUTH_EAST = 1792

        private val RANGED_PROJECTILE_CONFIG =
            ProjectileConfig(
                startHeight = 0,
                endHeight = 40,
                startDelay = 30,
                travelTime = 12,
                angle = 10,
                progress = 50,
                stepMultiplier = 6,
            )

        private val MAGIC_PROJECTILE =
            ProjAnimType(
                startHeight = 40,
                endHeight = 40,
                delay = 60,
                angle = 10,
                lengthAdjustment = 12,
                progress = 50,
                stepMultiplier = 12,
            )
    }
}
