package org.rsmod.content.bosses.dukesucellus

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.SpotanimType
import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.runtime.bossProjectile
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.Condition
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.combat.commons.player.finishNpcHit
import org.rsmod.api.death.NpcDeath
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onNpcQueue
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType as EngineHitType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

class DukeSucellus
@Inject
constructor(
    deps: BossDeps,
    private val npcDeath: NpcDeath,
) : BossPluginScript(deps) {

    override fun ScriptContext.startup() {
        BossCombat.register(this, spec, deps)
        registerDeath()

        deps.extensionRegistry.register(GAZE_RESOLVE) { _, npc, target, _ ->
            if (target.isValidTarget()) resolveGaze(npc, target)
        }
        deps.extensionRegistry.register(GAS_FLARE) { _, npc, target, _ ->
            if (target.isValidTarget()) fireGasFlare(npc, target)
        }
    }

    private fun ScriptContext.registerDeath() {
        val type = ServerCacheManager.getNpc(BOSS_NPC.asRSCM(RSCMType.NPC))!!
        onNpcQueue(type, "queue.death") { npcDeath.deathWithDrops(this, npc.coords) }
    }

    private fun awakened(npc: Npc): Boolean = npc.vars["varn.awakened_state"] == 1

    private fun onPillarColumn(target: Player): Boolean = target.coords.x in SAFE_COLUMNS

    private fun resolveGaze(npc: Npc, target: Player) {
        if (onPillarColumn(target)) {
            target.mes(GAZE_AVOID_MESSAGE)
            return
        }
        target.spotanim(GAZE_FREEZE_SPOTANIM)
        target.frozen = true
        target.routeDestination.clear()
        target.timer("timer.combat_freeze", GAZE_FREEZE_TICKS)
        val damage = GAZE_DAMAGE.first + deps.random.of(GAZE_DAMAGE.last - GAZE_DAMAGE.first + 1)
        target.finishNpcHit(npc, GAZE_HIT_DELAY, EngineHitType.Typeless, damage, deps.playerHitModifier)
    }

    private fun nearestVent(target: Player): CoordGrid =
        VENT_TILES.minByOrNull { it.chebyshevDistance(target.coords) } ?: VENT_TILES.first()

    private fun secondNearestVent(target: Player, nearest: CoordGrid): CoordGrid =
        VENT_TILES.filter { it != nearest }.minByOrNull { it.chebyshevDistance(target.coords) } ?: nearest

    private fun fireGasFlare(npc: Npc, target: Player) {
        val vent = nearestVent(target)
        castGasFlare(npc, target, vent, awakened(npc))
        if (awakened(npc) || npc.hitpoints < npc.baseHitpointsLvl / 2) {
            val echoVent = secondNearestVent(target, vent)
            deps.worldQueues.add(GAS_FLARE_ECHO_OFFSET) { castGasFlare(npc, target, echoVent, awakened(npc)) }
        }
    }

    private fun castGasFlare(npc: Npc, target: Player, vent: CoordGrid, isAwakened: Boolean) {
        deps.bossProjectile(
            spotanim = GAS_FLARE_PROJECTILE.asRSCM(RSCMType.SPOTANIM),
            src = npc.coords.translate(1, 1),
            target = vent,
            startHeight = GAS_PROJECTILE_START_HEIGHT,
            endHeight = GAS_PROJECTILE_END_HEIGHT,
            delay = GAS_PROJECTILE_DELAY,
            travel = GAS_PROJECTILE_TRAVEL,
            curve = GAS_PROJECTILE_ANGLE,
        )
        for ((index, spot) in GAS_RAMP_SPOTANIMS.withIndex()) {
            deps.worldQueues.add(GAS_PROJECTILE_DELAY + GAS_PROJECTILE_TRAVEL + index) {
                mapSpot(spot, vent)
            }
        }
        val damageRange = if (isAwakened) GAS_FLARE_DAMAGE_AWAKENED else GAS_FLARE_DAMAGE_NORMAL
        deps.worldQueues.add(GAS_PROJECTILE_DELAY + GAS_PROJECTILE_TRAVEL + GAS_RAMP_SPOTANIMS.size) {
            if (!target.isValidTarget() || vent.chebyshevDistance(target.coords) > GAS_CLOUD_RADIUS) return@add
            val damage = damageRange.first + deps.random.of(damageRange.last - damageRange.first + 1)
            target.finishNpcHit(npc, GAS_HIT_DELAY, EngineHitType.Typeless, damage, deps.playerHitModifier)
        }
    }

    private fun mapSpot(spot: String, coords: CoordGrid) {
        deps.worldRepo.spotanimMap(SpotanimType(spot.asRSCM(RSCMType.SPOTANIM)), coords)
    }

    private fun melee(): Effect =
        sequence(
            anim(MELEE_SEQ),
            spotanim(MELEE_CAST_SPOTANIM),
            tileAoE(
                center = CurrentTarget,
                radius = MELEE_CHIP_RADIUS,
                telegraph = telegraph(MELEE_TELEGRAPH_SPOTANIM, windup = 1),
                damage = (MELEE_CHIP_DAMAGE).roll(),
                type = Typeless,
            ),
            wait(1),
            hit {
                damage(MELEE_SLAM_DAMAGE)
                type(Melee)
                spotanim(MELEE_IMPACT_SPOTANIM)
                penetration(MELEE_SLAM_PENETRATION)
            },
        )

    private fun rangedMagic(): Effect =
        sequence(
            anim(MAGIC_SEQ),
            projectile(
                spotanim = MAGIC_PROJECTILE_SPOTANIM,
                target = CurrentTarget,
                hit = hit {
                    damage(MAGIC_DAMAGE)
                    type(Magic)
                    spotanim(MAGIC_IMPACT_SPOTANIM)
                },
            ),
        )

    private fun gazeSpecial(): Effect =
        sequence(
            anim(GAZE_SEQ),
            message(GAZE_WARNING_MESSAGE),
            wait(GAZE_RESOLVE_DELAY),
            external(GAZE_RESOLVE),
            wait(GAS_FLARE_DELAY_AFTER_GAZE),
            external(GAS_FLARE),
        )

    private fun blackOrb(): Effect =
        sequence(
            anim(GAZE_SEQ),
            message(BLACK_ORB_MESSAGE),
            debris(
                telegraph = BLACK_ORB_TELEGRAPH_SPOTANIM,
                damage = BLACK_ORB_DAMAGE.roll(),
                windup = BLACK_ORB_WINDUP,
                targetRadius = 3,
                count = 1..1,
            ),
            statDrain("stat.attack", "stat.strength", "stat.defence", "stat.ranged", "stat.magic", amount = BLACK_ORB_STAT_DRAIN),
        )

    private fun bile(): Effect =
        sequence(
            anim(MAGIC_SEQ),
            message(BILE_MESSAGE),
            hit {
                damage(BILE_DAMAGE)
                type(Typeless)
                spotanim(MAGIC_IMPACT_SPOTANIM)
            },
            poison(BILE_POISON_DAMAGE),
        )

    private val awakenedCondition: Condition = Condition.Custom { it.vars["varn.awakened_state"] == 1 }

    override val spec: BossSpec by lazy {
        boss(BOSS_NPC) {
            stats(attackRate = ATTACK_RATE, aggressionRadius = AGGRESSION_RADIUS)

            val melee = ability("melee", melee())
            val rangedMagic = ability("ranged_magic", rangedMagic())
            val special = ability("gaze_special", gazeSpecial())
            val blackOrb = ability("black_orb", blackOrb())
            val bile = ability("bile", bile())

            phase("main") {
                weightedSelectorRandom {
                    +random(melee, weight = 1, requires = WithinMeleeRange)
                    +random(rangedMagic, weight = 1, requires = Condition.Not(WithinMeleeRange))
                }
                forceEveryAttacks(SPECIAL_MIN_ATTACKS, SPECIAL_MAX_ATTACKS, special)
                forceWhen(Condition.HpBelow(BLACK_ORB_HP) and awakenedCondition, blackOrb, once = true)
                forceWhen(Condition.HpBelow(BILE_HP) and awakenedCondition, bile, once = true)
            }

            phase("enrage", entryHp = ENRAGE_HP_FRACTION, attackRate = ENRAGE_ATTACK_RATE) {
                weightedSelectorRandom {
                    +random(melee, weight = 1, requires = WithinMeleeRange)
                    +random(rangedMagic, weight = 1, requires = Condition.Not(WithinMeleeRange))
                }
                forceEveryAttacks(SPECIAL_MIN_ATTACKS, SPECIAL_MAX_ATTACKS, special)
            }
        }
    }

    private companion object {
        private const val BOSS_NPC = "npc.duke_sucellus_awake"

        private const val ATTACK_RATE = 5
        private const val ENRAGE_ATTACK_RATE = 4
        private const val ENRAGE_HP_FRACTION = 0.25
        private const val AGGRESSION_RADIUS = 15

        private const val SPECIAL_MIN_ATTACKS = 5
        private const val SPECIAL_MAX_ATTACKS = 6

        private const val MELEE_SEQ = "seq.npc_duke_sucellus01_attack_melee_01"
        private const val MELEE_CAST_SPOTANIM = "spotanim.spotanim_npc_duke_sucellus01_attack_melee_main_01"
        private const val MELEE_TELEGRAPH_SPOTANIM = "spotanim.vfx_duke_sucellus_attack_melee_spotanim_tile_01"
        private const val MELEE_IMPACT_SPOTANIM = "spotanim.vfx_duke_sucellus_attack_melee_spotanim_npc_01"
        private const val MELEE_CHIP_RADIUS = 1
        private val MELEE_CHIP_DAMAGE = 5..11
        private val MELEE_SLAM_DAMAGE = (25..56).roll()
        private const val MELEE_SLAM_PENETRATION = 75

        private const val MAGIC_SEQ = "seq.npc_duke_sucellus01_magic_attack_01"
        private const val MAGIC_PROJECTILE_SPOTANIM = "spotanim.vfx_duke_sucellus_attack_magic_projectile_01"
        private const val MAGIC_IMPACT_SPOTANIM = "spotanim.vfx_duke_sucellus_attack_magic_projectile_impact_01"
        private val MAGIC_DAMAGE = (28..48).roll()

        private const val GAZE_RESOLVE = "duke_sucellus.gaze_resolve"
        private const val GAS_FLARE = "duke_sucellus.gas_flare"
        private const val GAZE_SEQ = "seq.npc_duke_sucellus01_sight_attack_01"
        private const val GAZE_WARNING_MESSAGE = "<col=ff00ff>Duke Sucellus turns his gaze upon you...</col>"
        private const val GAZE_AVOID_MESSAGE = "<col=00ff00>You manage to avoid Duke Sucellus' gaze.</col>"
        private const val GAZE_RESOLVE_DELAY = 5
        private const val GAS_FLARE_DELAY_AFTER_GAZE = 10
        private const val GAZE_FREEZE_SPOTANIM = "spotanim.vfx_duke_sucellus_attack_magic_projectile_impact_01"
        private const val GAZE_FREEZE_TICKS = 8
        private const val GAZE_HIT_DELAY = 1
        private val GAZE_DAMAGE = 60..101

        private const val GAS_FLARE_PROJECTILE = "spotanim.spotanim_duke_vent_01_spawn_01"
        private val GAS_RAMP_SPOTANIMS =
            listOf(
                "spotanim.spotanim_duke_vent_01_idle_01",
                "spotanim.spotanim_duke_vent_01_idle_02",
                "spotanim.spotanim_duke_vent_01_idle_03",
            )
        private const val GAS_PROJECTILE_START_HEIGHT = 40
        private const val GAS_PROJECTILE_END_HEIGHT = 10
        private const val GAS_PROJECTILE_DELAY = 30
        private const val GAS_PROJECTILE_TRAVEL = 40
        private const val GAS_PROJECTILE_ANGLE = 0
        private const val GAS_CLOUD_RADIUS = 2
        private const val GAS_HIT_DELAY = 1
        private const val GAS_FLARE_ECHO_OFFSET = 3
        private val GAS_FLARE_DAMAGE_NORMAL = 8..15
        private val GAS_FLARE_DAMAGE_AWAKENED = 15..27

        private const val BLACK_ORB_HP = 0.7
        private const val BLACK_ORB_TELEGRAPH_SPOTANIM = "spotanim.spotanim_duke_vent_01_idle_04"
        private const val BLACK_ORB_WINDUP = 3
        private val BLACK_ORB_DAMAGE = 10..27
        private const val BLACK_ORB_STAT_DRAIN = 8
        private const val BLACK_ORB_MESSAGE = "<col=ff00ff>A black orb begins to sweep across the room...</col>"

        private const val BILE_HP = 0.4
        private val BILE_DAMAGE = (10..24).roll()
        private const val BILE_POISON_DAMAGE = 4
        private const val BILE_MESSAGE = "<col=ff00ff>Duke Sucellus coats you in bile!</col>"

        private const val LEFT_PILLAR_X = 3035
        private const val RIGHT_PILLAR_X = 3043
        private val SAFE_COLUMNS = setOf(LEFT_PILLAR_X - 1, LEFT_PILLAR_X, RIGHT_PILLAR_X, RIGHT_PILLAR_X + 1)

        private val VENT_TILES: List<CoordGrid> =
            buildList {
                for (col in 0..2) {
                    for (row in 0..2) {
                        add(CoordGrid(3036 + col * 3, 6442 + row * 4, 0))
                    }
                }
            }
    }
}
