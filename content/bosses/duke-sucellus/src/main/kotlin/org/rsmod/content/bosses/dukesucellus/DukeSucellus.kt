package org.rsmod.content.bosses.dukesucellus

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcMode
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
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.player.output.spam
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.player.ui.ifOpenFullOverlay
import org.rsmod.api.player.ui.ifSetAnim
import org.rsmod.api.script.onEvent
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.hit.HitType as EngineHitType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

class DukeSucellus
@Inject
constructor(
    deps: BossDeps,
    private val eventBus: EventBus,
) : BossPluginScript(deps) {

    override fun ScriptContext.startup() {
        BossCombat.register(this, spec, deps)

        deps.extensionRegistry.register(MELEE_ICICLES) { _, npc, _, _ -> spawnIcicles(npc) }
        deps.extensionRegistry.register(MELEE_SLAM) { _, npc, _, _ -> slam(npc) }
        deps.extensionRegistry.register(GAZE_START) { _, npc, target, _ ->
            npc.vars[AFTER_GAZE_VARN] = 1
            if (target.isValidTarget()) openGaze(npc, target)
        }
        deps.extensionRegistry.register(STANDARD_ATTACK) { _, npc, _, _ ->
            npc.vars[FLARE_ATTACKS_VARN] = npc.vars[FLARE_ATTACKS_VARN] + 1
            npc.vars[AFTER_GAZE_VARN] = 0
        }
        deps.extensionRegistry.register(GAS_FLARE_CAST) { _, npc, target, _ ->
            fireGasFlare(npc, target)
        }
        deps.extensionRegistry.register(GAS_FLARE_ECHO_CAST) { _, npc, target, _ ->
            fireGasFlareEcho(npc, target)
        }
        onEvent<NpcStateEvents.Respawn> {
            if (npc.type.id in bossNpcIds) resetFlareState(npc)
        }
        deps.extensionRegistry.register(GAZE_RESOLVE) { _, npc, target, _ ->
            if (isFighting(npc) && target.isValidTarget()) resolveGaze(npc, target)
        } }

    private val bossNpcIds: Set<Int> by lazy {
        listOf(BOSS_NPC, SLEEP_NPC).map { it.asRSCM(RSCMType.NPC) }.toSet()
    }

    private fun isFighting(npc: Npc): Boolean = npc.isSlotAssigned && npc.isVisType(BOSS_NPC)

    private fun behindPillar(npc: Npc, target: Player): Boolean {
        val west = npc.coords.x
        val east = west + npc.size - 1
        return target.coords.x in (west - 2)..(west - 1) || target.coords.x in (east + 1)..(east + 2)
    }

    private fun glowComponent(): Int = GLOW_COMPONENT.asRSCM(RSCMType.COMPONENT)

    private fun seq(name: String) = ServerCacheManager.getAnim(name.asRSCM(RSCMType.SEQ))

    private fun openGaze(npc: Npc, target: Player) {
        npc.setBodyModels(listOf(DUKE_BODY_MODEL, DUKE_OPEN_EYE_MODEL))
        target.runClientScript(SCREEN_GLOW_START_SCRIPT, GAZE_GLOW_COLOUR, glowComponent(), 0, 120, 255, 150)
        target.ifOpenFullOverlay(GAZE_INTERFACE, eventBus)
        target.ifSetAnim(GAZE_EYE_COMPONENT, seq(GAZE_EYE_APPEAR_SEQ))
        deps.worldQueues.add(GAZE_RESOLVE_DELAY + GAZE_CLOSE_DELAY) {
            if (npc.isSlotAssigned) npc.setBodyModels(listOf(DUKE_BODY_MODEL, -1))
            target.ifCloseOverlay(GAZE_INTERFACE, eventBus)
        }
    }

    private fun resolveGaze(npc: Npc, target: Player) {
        target.runClientScript(SCREEN_GLOW_END_SCRIPT, GAZE_GLOW_COLOUR, glowComponent(), 0, 30, 255)
        target.ifSetAnim(GAZE_EYE_COMPONENT, seq(GAZE_EYE_DISAPPEAR_SEQ))
        target.soundSynth(GAZE_RESOLVE_SYNTH)
        if (behindPillar(npc, target)) {
            target.spam(GAZE_AVOID_MESSAGE)
            return
        }
        target.spam(GAZE_FAIL_MESSAGE)
        target.spotanim(GAZE_FREEZE_SPOTANIM, slot = GAZE_FREEZE_SPOTANIM_SLOT)
        target.frozen = true
        target.routeDestination.clear()
        target.timer("timer.combat_freeze", GAZE_FREEZE_TICKS)
        deps.worldQueues.add(1) { if (target.isValidTarget()) target.mes(GAZE_FROZEN_MESSAGE) }
        val damage = GAZE_DAMAGE.first + deps.random.of(GAZE_DAMAGE.last - GAZE_DAMAGE.first + 1)
        target.finishNpcHit(npc, GAZE_HIT_DELAY, EngineHitType.Typeless, damage, deps.playerHitModifier)
    }

    private fun ventCentres(npc: Npc): List<CoordGrid> =
        VENT_OFFSETS.map { (dx, dy) -> npc.coords.translate(dx, dy) }

    private fun nearestVents(npc: Npc, target: Player): List<CoordGrid> =
        ventCentres(npc).sortedBy { it.chebyshevDistance(target.coords) }

    private fun flareThreshold(npc: Npc): Int {
        val stored = npc.vars[FLARE_THRESHOLD_VARN]
        return if (stored > 0) stored else FIRST_FLARE_ATTACKS
    }

    private fun flareDue(npc: Npc): Boolean =
        npc.vars[AFTER_GAZE_VARN] == 0 && npc.vars[FLARE_ATTACKS_VARN] >= flareThreshold(npc)

    private fun resetFlareState(npc: Npc) {
        npc.vars[FLARE_ATTACKS_VARN] = 0
        npc.vars[FLARE_THRESHOLD_VARN] = 0
        npc.vars[AFTER_GAZE_VARN] = 0
    }

    private fun hpFraction(npc: Npc): Double = npc.hitpoints.toDouble() / npc.baseHitpointsLvl.coerceAtLeast(1)

    private fun fireGasFlare(npc: Npc, target: Player) {
        val fraction = hpFraction(npc)
        npc.vars[FLARE_ATTACKS_VARN] = 0
        npc.vars[FLARE_THRESHOLD_VARN] =
            when {
                fraction > FLARE_SLOW_HP -> FLARE_ATTACKS_HIGH_HP
                fraction > ENRAGE_HP_FRACTION -> FLARE_ATTACKS_MID_HP
                else -> FLARE_ATTACKS_LOW_HP
            }

        castGasFlare(npc, target, nearestVents(npc, target).first())
    }

    private fun fireGasFlareEcho(npc: Npc, target: Player) {
        val vents = nearestVents(npc, target)
        castGasFlare(npc, target, vents.getOrElse(1) { vents.first() })
    }

    private fun castGasFlare(npc: Npc, target: Player, vent: CoordGrid) {
        target.soundSynth(GAS_FLARE_SYNTH)
        deps.bossProjectile(
            spotanim = GAS_FLARE_PROJECTILE.asRSCM(RSCMType.SPOTANIM),
            src = npc.coords.translate(GAS_SOURCE_DX, GAS_SOURCE_DY),
            target = vent,
            startHeight = GAS_PROJECTILE_START_HEIGHT,
            endHeight = GAS_PROJECTILE_END_HEIGHT,
            delay = GAS_PROJECTILE_DELAY,
            travel = GAS_PROJECTILE_TRAVEL,
            curve = GAS_PROJECTILE_ANGLE,
            progress = GAS_PROJECTILE_PROGRESS,
        )
        deps.worldQueues.add(GAS_VENT_SPAWN_TICKS) { if (isFighting(npc)) spawnGasVent(npc, vent) }
    }

    private fun spawnGasVent(npc: Npc, centre: CoordGrid) {
        val ventType = ServerCacheManager.getNpc(GAS_VENT_NPC.asRSCM(RSCMType.NPC)) ?: return
        val vent = Npc(ventType, centre.translate(-1, -1))
        vent.mode = NpcMode.None
        deps.npcRepo.add(vent, GAS_VENT_DESPAWN_DURATION)
        vent.spotanim(GAS_VENT_SPAWN_SPOTANIM, GAS_VENT_SPAWN_SPOTANIM_DELAY)

        for (tick in GAS_VENT_IDLE_TICKS until GAS_VENT_DESPAWN_TICKS step GAS_VENT_IDLE_INTERVAL) {
            deps.worldQueues.add(tick) {
                if (vent.isSlotAssigned) vent.spotanim(GAS_VENT_IDLE_SPOTANIM)
            }
        }
        deps.worldQueues.add(GAS_VENT_DESPAWN_TICKS) {
            if (vent.isSlotAssigned) vent.spotanim(GAS_VENT_DESPAWN_SPOTANIM)
        }
        for (tick in GAS_VENT_IDLE_TICKS..GAS_VENT_DESPAWN_TICKS) {
            deps.worldQueues.add(tick) { if (isFighting(npc)) strikeGas(npc, centre) }
        }
    }

    private fun strikeGas(npc: Npc, centre: CoordGrid) {
        for (player in deps.playerList) {
            if (!player.isValidTarget() || centre.chebyshevDistance(player.coords) > GAS_CLOUD_RADIUS) continue
            player.statSub("stat.prayer", GAS_PRAYER_DRAIN, 0)
            val damage = GAS_DAMAGE.first + deps.random.of(GAS_DAMAGE.last - GAS_DAMAGE.first + 1)
            player.finishNpcHit(npc, GAS_HIT_DELAY, EngineHitType.Typeless, damage, deps.playerHitModifier)
        }
    }

    private fun mapSpot(spot: String, coords: CoordGrid) {
        deps.worldRepo.spotanimMap(SpotanimType(spot.asRSCM(RSCMType.SPOTANIM)), coords)
    }

    private fun tilesInFront(npc: Npc): List<CoordGrid> =
        List(npc.size) { npc.coords.translate(it, -1) }

    private fun spawnIcicles(npc: Npc) {
        val tiles = tilesInFront(npc)
        for ((index, tile) in tiles.withIndex()) {
            val fromEdge = minOf(index, tiles.lastIndex - index)
            val spot = MELEE_ICICLE_SPOTANIMS[fromEdge.coerceAtMost(MELEE_ICICLE_SPOTANIMS.lastIndex)]
            mapSpot(spot, tile)
        }
    }

    private fun slam(npc: Npc) {
        if (!isFighting(npc)) return
        val tiles = tilesInFront(npc).toSet()
        val range = MELEE_SLAM_DAMAGE
        for (player in deps.playerList) {
            if (!player.isValidTarget() || player.coords !in tiles) continue
            val damage = range.first + deps.random.of(range.last - range.first + 1)
            val modifier = deps.playerHitModifier
            player.finishNpcHit(npc, 1, EngineHitType.Melee, damage, modifier, MELEE_SLAM_PENETRATION)
        }
    }

    private fun melee(): Effect =
        sequence(
            external(STANDARD_ATTACK),
            anim(MELEE_SEQ),
            spotanim(MELEE_NPC_SPOTANIM),
            external(MELEE_ICICLES),
            hit {
                damage(MELEE_CHIP_DAMAGE)
                type(Melee)
                penetration(MELEE_CHIP_PENETRATION)
            },
            wait(MELEE_SLAM_DELAY),
            external(MELEE_SLAM),
        )

    private fun rangedMagic(): Effect =
        sequence(
            external(STANDARD_ATTACK),
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
            external(GAZE_START),
            wait(GAZE_RESOLVE_DELAY),
            external(GAZE_RESOLVE),
            wait(GAZE_CLOSE_DELAY),
        )

    private fun gasFlare(): Effect =
        sequence(
            anim(GAS_FLARE_SEQ),
            external(GAS_FLARE_CAST),
            whenever(
                Condition.HpBelow(FLARE_ECHO_HP),
                sequence(
                    wait(GAS_FLARE_ECHO_OFFSET),
                    anim(GAS_FLARE_SEQ),
                    external(GAS_FLARE_ECHO_CAST),
                    wait(GAS_FLARE_ECHO_RECOVERY),
                ),
            ),
        )

    private val flareDueCondition: Condition = Condition.Custom { npc, _ -> flareDue(npc) }

    override val spec: BossSpec by lazy {
        boss(BOSS_NPC, SLEEP_NPC) {
            stats(attackRate = ATTACK_RATE, aggressionRadius = AGGRESSION_RADIUS)

            val melee = ability("melee", melee())
            val rangedMagic = ability("ranged_magic", rangedMagic())
            val special = ability("gaze_special", gazeSpecial())
            val gasFlare = ability("gas_flare", gasFlare())

            phase("main") {
                weightedSelectorRandom {
                    +random(melee, weight = 1, requires = WithinMeleeRange)
                    +random(rangedMagic, weight = 1, requires = Condition.Not(WithinMeleeRange))
                }
                forceEveryAttacks(SPECIAL_MIN_ATTACKS, SPECIAL_MAX_ATTACKS, special)
                forceWhen(flareDueCondition, gasFlare)
            }

            phase("enrage", entryHp = ENRAGE_HP_FRACTION, attackRate = ENRAGE_ATTACK_RATE) {
                weightedSelectorRandom {
                    +random(melee, weight = 1, requires = WithinMeleeRange)
                    +random(rangedMagic, weight = 1, requires = Condition.Not(WithinMeleeRange))
                }
                forceEveryAttacks(SPECIAL_MIN_ATTACKS, SPECIAL_MAX_ATTACKS, special)
                forceWhen(flareDueCondition, gasFlare)
            }
        }
    }

    private companion object {
        private const val BOSS_NPC = "npc.duke_sucellus_awake"
        private const val SLEEP_NPC = "npc.duke_sucellus_asleep"

        private const val ATTACK_RATE = 5
        private const val ENRAGE_ATTACK_RATE = 4
        private const val ENRAGE_HP_FRACTION = 0.25
        private const val AGGRESSION_RADIUS = 15

        private const val SPECIAL_MIN_ATTACKS = 5
        private const val SPECIAL_MAX_ATTACKS = 6

        private const val MELEE_SEQ = "seq.npc_duke_sucellus01_attack_melee_01"
        private const val MELEE_NPC_SPOTANIM = "spotanim.vfx_duke_sucellus_attack_melee_spotanim_npc_01"
        private val MELEE_ICICLE_SPOTANIMS =
            listOf(
                "spotanim.vfx_duke_sucellus_attack_melee_spotanim_tile_01",
                "spotanim.vfx_duke_sucellus_attack_melee_spotanim_tile_02",
                "spotanim.vfx_duke_sucellus_attack_melee_spotanim_tile_03",
                "spotanim.vfx_duke_sucellus_attack_melee_spotanim_tile_04",
            )
        private const val MELEE_ICICLES = "duke_sucellus.melee_icicles"
        private const val MELEE_SLAM = "duke_sucellus.melee_slam"
        private val MELEE_CHIP_DAMAGE = (0..11).roll()
        private const val MELEE_CHIP_PENETRATION = 45
        private const val MELEE_SLAM_DELAY = 2
        private val MELEE_SLAM_DAMAGE = 25..56
        private const val MELEE_SLAM_PENETRATION = 75

        private const val MAGIC_SEQ = "seq.npc_duke_sucellus01_magic_attack_01"
        private const val MAGIC_PROJECTILE_SPOTANIM = "spotanim.vfx_duke_sucellus_attack_magic_projectile_01"
        private const val MAGIC_IMPACT_SPOTANIM = "spotanim.vfx_duke_sucellus_attack_magic_projectile_impact_01"
        private val MAGIC_DAMAGE = (28..48).roll()

        private const val GAZE_START = "duke_sucellus.gaze_start"
        private const val GAZE_RESOLVE = "duke_sucellus.gaze_resolve"
        private const val GAZE_SEQ = "seq.npc_duke_sucellus01_sight_attack_01"
        private const val GAZE_WARNING_MESSAGE = "<col=a53fff>Duke Sucellus turns his gaze upon you..."
        private const val GAZE_AVOID_MESSAGE = "<col=229628>You manage to avoid Duke Sucellus' gaze."
        private const val GAZE_FAIL_MESSAGE = "<col=ff3045>You failed to avoid Duke Sucellus' gaze."
        private const val GAZE_FROZEN_MESSAGE = "<col=ef1020>You have been frozen!</col>"
        private const val GAZE_RESOLVE_DELAY = 4
        private const val GAZE_CLOSE_DELAY = 2
        private const val DUKE_BODY_MODEL = 49194
        private const val DUKE_OPEN_EYE_MODEL = 49193
        private const val GAZE_INTERFACE = "interface.duke_sight"
        private const val GAZE_EYE_COMPONENT = "component.duke_sight:duke_eye_model"
        private const val GAZE_EYE_APPEAR_SEQ = "seq.interface_icon_cthonian01_appear"
        private const val GAZE_EYE_DISAPPEAR_SEQ = "seq.interface_icon_cthonian01_disappear"
        private const val SCREEN_GLOW_START_SCRIPT = 3128
        private const val SCREEN_GLOW_END_SCRIPT = 1896
        private const val GLOW_COMPONENT = "component.hpbar_hud:container"
        private const val GAZE_GLOW_COLOUR = 0x4243A1
        private const val GAZE_RESOLVE_SYNTH = "synth.ice_barrage_impact"
        private const val GAZE_FREEZE_SPOTANIM = "spotanim.ice_barrage_impact"
        private const val GAZE_FREEZE_SPOTANIM_SLOT = 2
        private const val GAZE_FREEZE_TICKS = 8
        private const val GAZE_HIT_DELAY = 2
        private val GAZE_DAMAGE = 60..101

        private const val STANDARD_ATTACK = "duke_sucellus.standard_attack"
        private const val GAS_FLARE_CAST = "duke_sucellus.gas_flare_cast"
        private const val GAS_FLARE_ECHO_CAST = "duke_sucellus.gas_flare_echo_cast"
        private const val GAS_FLARE_SEQ = "seq.npc_duke_sucellus01_magic_attack_faster01"
        private const val GAS_FLARE_SYNTH = 5002
        private const val GAS_FLARE_PROJECTILE = "spotanim.projanim_duke_spit_01"
        private const val GAS_SOURCE_DX = 3
        private const val GAS_SOURCE_DY = 1
        private const val GAS_PROJECTILE_START_HEIGHT = 87
        private const val GAS_PROJECTILE_END_HEIGHT = 0
        private const val GAS_PROJECTILE_DELAY = 20
        private const val GAS_PROJECTILE_TRAVEL = 70
        private const val GAS_PROJECTILE_ANGLE = 5
        private const val GAS_PROJECTILE_PROGRESS = 32
        private const val GAS_FLARE_ECHO_OFFSET = 3
        private const val GAS_FLARE_ECHO_RECOVERY = 2
        private const val FIRST_FLARE_ATTACKS = 8
        private const val FLARE_ATTACKS_HIGH_HP = 5
        private const val FLARE_ATTACKS_MID_HP = 4
        private const val FLARE_ATTACKS_LOW_HP = 3
        private const val FLARE_SLOW_HP = 0.75
        private const val FLARE_ECHO_HP = 0.5
        private const val FLARE_ATTACKS_VARN = "varn.duke_flare_attacks"
        private const val FLARE_THRESHOLD_VARN = "varn.duke_flare_threshold"
        private const val AFTER_GAZE_VARN = "varn.duke_after_gaze"

        private const val GAS_VENT_NPC = "npc.duke_vent_spotanim_npc"
        private const val GAS_VENT_SPAWN_SPOTANIM = "spotanim.spotanim_duke_vent_01_spawn_01"
        private const val GAS_VENT_IDLE_SPOTANIM = "spotanim.spotanim_duke_vent_01_idle_01"
        private const val GAS_VENT_DESPAWN_SPOTANIM = "spotanim.spotanim_duke_vent_01_despawn_01"
        private const val GAS_VENT_SPAWN_SPOTANIM_DELAY = 30
        private const val GAS_VENT_SPAWN_TICKS = 2
        private const val GAS_VENT_IDLE_TICKS = 2
        private const val GAS_VENT_DESPAWN_TICKS = 12
        private const val GAS_VENT_IDLE_INTERVAL = 2
        private const val GAS_VENT_LIFESPAN = 16
        private const val GAS_VENT_DESPAWN_DURATION = GAS_VENT_LIFESPAN + 1
        private const val GAS_CLOUD_RADIUS = 1
        private const val GAS_PRAYER_DRAIN = 4
        private const val GAS_HIT_DELAY = 1
        private val GAS_DAMAGE = 0..12

        private val VENT_OFFSETS: List<Pair<Int, Int>> =
            buildList {
                for (col in 0..2) {
                    for (row in 0..2) {
                        add(col * 3 to -10 + row * 4)
                    }
                }
            }
    }
}
