package org.rsmod.content.bosses.kraken

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.annotations.InternalApi
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.dsl.soundTo as soundToEffect
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.Condition
import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.api.death.NpcDeath
import org.rsmod.api.npc.events.NpcHitEvents
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.queueDeath
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onApNpcT
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onNpcQueue
import org.rsmod.api.script.onOpNpcU
import org.rsmod.content.bosses.kraken.Whirlpools.Companion.ARISE_ANIM
import org.rsmod.content.bosses.kraken.Whirlpools.Companion.ATTACK_ANIM
import org.rsmod.content.bosses.kraken.Whirlpools.Companion.IMMUNE_MESSAGE
import org.rsmod.content.bosses.kraken.Whirlpools.Companion.IMPACT_HEIGHT
import org.rsmod.content.bosses.kraken.Whirlpools.Companion.RANGED_DIVISOR
import org.rsmod.content.bosses.kraken.Whirlpools.Companion.SPLASH
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.proj.ProjAnim
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.ScriptContext

class Kraken @Inject constructor(
    deps: BossDeps,
    aiPlayerInteractions: AiPlayerInteractions,
    private val npcDeath: NpcDeath,
    private val attackValidateHooks: Set<NpcAttackValidateHook>,
) : BossPluginScript(deps) {

    private val whirlpools = Whirlpools(deps, aiPlayerInteractions)

    override fun ScriptContext.startup() {
        BossCombat.register(
            this,
            spec,
            deps,
            onHit = { disturbOnImpact(this, BOSS, ARISE_ANIM, BOSS_ARISE_SYNTH, spec, gated = true) },
            onCombatTick = { whirlpools.touchCombat(npc) },
        )
        BossCombat.register(
            this,
            tentacleSpec,
            deps,
            onHit = {
                disturbOnImpact(
                    this,
                    TENTACLE,
                    TENTACLE_ARISE_ANIM,
                    TENTACLE_ARISE_SYNTH,
                    tentacleSpec,
                    gated = false,
                )
            },
            onCombatTick = { whirlpools.touchCombat(npc) },
        )

        val bossType = ServerCacheManager.getNpc(BOSS.asRSCM(RSCMType.NPC))!!
        onNpcQueue(bossType, "queue.death") {
            val dropCoords = findHero(deps.playerList)?.coords ?: coords
            killTentacles(npc)
            npcDeath.deathWithDrops(this, dropCoords)
            jitterRespawn(npc)
        }

        val poolType = ServerCacheManager.getNpc(WHIRLPOOL.asRSCM(RSCMType.NPC))!!
        val explosiveType = ServerCacheManager.getItem(EXPLOSIVE.asRSCM(RSCMType.OBJ))!!
        onApNpcT(poolType, "component.inventory:items") {
            if (it.objType?.id != explosiveType.id) {
                apRange(-1)
                return@onApNpcT
            }
            if (isWithinApRange(it.npc, EXPLOSIVE_RANGE)) throwExplosive(it.npc)
        }
        onOpNpcU(poolType, explosiveType) { throwExplosive(it.npc) }

        onEvent<NpcStateEvents.Respawn> {
            if (npc.id == bossPoolId) respawnTentacles(npc)
            if (npc.id in poolIds) refreshNearby(npc)
        }
        for (poolId in poolIds) {
            onEvent<NpcStateEvents.Spawn>(poolId.toLong()) { refreshNearby(npc) }
        }
    }

    private val bossPoolId by lazy { WHIRLPOOL.asRSCM(RSCMType.NPC) }
    private val tentaclePoolId by lazy { TENTACLE_WHIRLPOOL.asRSCM(RSCMType.NPC) }
    private val poolIds by lazy { setOf(bossPoolId, tentaclePoolId) }

    private fun refreshNearby(npc: Npc) {
        refreshImmunity(npc)
        deps.npcRepo
            .findAll(ZoneKey.from(npc.coords), TENTACLE_ZONE_RADIUS)
            .filter { it.type.id == bossPoolId && it !== npc }
            .forEach { refreshImmunity(it) }
    }

    private fun refreshImmunity(npc: Npc) {
        val isBoss = npc.type.id == bossPoolId
        val gated = whirlpools.isSubmerged(npc) && (!isBoss || hasUndisturbedTentacles(npc))
        whirlpools.setImmunity(npc, melee = isBoss || gated, other = gated)
    }

    @OptIn(InternalApi::class)
    private fun jitterRespawn(npc: Npc) {
        if (npc.lifecycleRespawnCycle > 0) {
            npc.lifecycleRespawnCycle += deps.random.of(RESPAWN_JITTER + 1)
        }
    }

    private fun ProtectedAccess.throwExplosive(pool: Npc) {
        if (!whirlpools.isSubmerged(pool)) return
        for (hook in attackValidateHooks) {
            val result = hook.validate(player, pool)
            if (result is NpcAttackValidateResult.Deny) {
                mes(result.message)
                return
            }
        }
        if (invDel(inv, EXPLOSIVE, 1).failure) return
        mes("You throw the fishing explosive into the whirlpool...")
        anim("seq.human_throw")
        spotanim("spotanim.vial_launch", height = EXPLOSIVE_LAUNCH_HEIGHT)
        deps.worldRepo.projAnim(explosiveProjectile(player, pool))
        val thrower = player
        deps.worldQueues.add(EXPLOSIVE_TENTACLE_DELAY) {
            if (!thrower.isValidTarget()) return@add
            surfaceTentacles(pool, thrower)
            deps.worldQueues.add(EXPLOSIVE_BOSS_DELAY) {
                if (thrower.isValidTarget()) surfaceBoss(pool, thrower)
            }
        }
    }

    private fun explosiveProjectile(thrower: Player, pool: Npc): ProjAnim {
        val flightCycles = (EXPLOSIVE_TENTACLE_DELAY - 1) * CLIENT_CYCLES_PER_TICK + EXPLOSIVE_LANDING_OFFSET
        return ProjAnim(
            spotanim = "spotanim.vial_travel".asRSCM(RSCMType.SPOTANIM),
            startHeight = EXPLOSIVE_START_HEIGHT,
            endHeight = EXPLOSIVE_END_HEIGHT,
            startTime = EXPLOSIVE_START_TIME,
            endTime = flightCycles,
            angle = EXPLOSIVE_ANGLE,
            progress = EXPLOSIVE_PROGRESS,
            sourceIndex = -(thrower.slotId + 1),
            targetIndex = pool.slotId + 1,
            startCoord = thrower.coords,
            endCoord = pool.coords,
        )
    }

    private fun surfaceTentacles(boss: Npc, attacker: Player) {
        deps.npcRepo
            .findAll(ZoneKey.from(boss.coords), TENTACLE_ZONE_RADIUS)
            .filter { it.type.id == tentaclePoolId && whirlpools.isSubmerged(it) }
            .toList()
            .forEach {
                whirlpools.wake(
                    it,
                    attacker,
                    TENTACLE,
                    TENTACLE_ARISE_ANIM,
                    tentacleSpec,
                    ::refreshNearby,
                    TENTACLE_ARISE_SYNTH,
                    TENTACLE_WAKE_ATTACK_DELAY,
                )
            }
    }

    @OptIn(InternalApi::class)
    private fun surfaceBoss(pool: Npc, attacker: Player) {
        if (!pool.isSlotAssigned || !whirlpools.isSubmerged(pool)) return
        whirlpools.wake(
            pool,
            attacker,
            BOSS,
            ARISE_ANIM,
            spec,
            ::refreshNearby,
            BOSS_ARISE_SYNTH,
            BOSS_WAKE_ATTACK_DELAY,
        )
    }

    @OptIn(InternalApi::class)
    private fun respawnTentacles(boss: Npc) {
        deps.npcRepo
            .findAll(ZoneKey.from(boss.coords), TENTACLE_ZONE_RADIUS)
            .filter { it.type.id == tentaclePoolId && it.hidden }
            .forEach { it.lifecycleRespawnCycle = deps.mapClock.cycle + 1 }
    }

    private fun killTentacles(boss: Npc) {
        val surfaced =
            deps.npcRepo
                .findAll(ZoneKey.from(boss.coords), TENTACLE_ZONE_RADIUS)
                .filter {
                    it.type.id == tentaclePoolId && it.visType.id != tentaclePoolId && it.hitpoints > 0
                }
                .toList()
        for (tentacle in surfaced) {
            tentacle.hitpoints = 0
            tentacle.queueDeath()
        }
    }

    private fun disturbOnImpact(
        event: NpcHitEvents.Impact,
        into: String,
        anim: String,
        synth: String,
        whirlpoolSpec: BossSpec,
        gated: Boolean,
    ) {
        val npc = event.npc
        whirlpools.touchCombat(npc)
        if (!whirlpools.isSubmerged(npc)) return
        val attacker = event.hit.resolvePlayerSource(deps.playerList) ?: return
        if (gated && hasUndisturbedTentacles(npc)) {
            attacker.mes("There was no response.")
            return
        }
        val attackDelay = if (gated) BOSS_WAKE_ATTACK_DELAY else TENTACLE_WAKE_ATTACK_DELAY
        whirlpools.wake(
            npc,
            attacker,
            into,
            anim,
            whirlpoolSpec,
            ::refreshNearby,
            synth,
            attackDelay,
        )
    }

    private fun hasUndisturbedTentacles(boss: Npc): Boolean =
        deps.npcRepo.findAll(ZoneKey.from(boss.coords), TENTACLE_ZONE_RADIUS).any {
            it.type.id == tentaclePoolId && it.visType.id == tentaclePoolId
        }

    override val spec =
        boss(BOSS, WHIRLPOOL) {
            stats(attackRate = ATTACK_RATE)

            val immune = ability("immune") { message(IMMUNE_MESSAGE) }

            val blast =
                ability("blast") {
                    anim(ATTACK_ANIM)
                    sound(CAST_SYNTH, radius = CAST_SOUND_RADIUS)
                    projectile {
                        spotanim = "spotanim.firewave_travel"
                        travel = "projanim.magic_spell"
                        hit {
                            damage(Accuracy(Roll(0..MAGIC_MAX), kind = MagicalRanged))
                            type(Magic)
                            penetration(TYPELESS_PENETRATION)
                            missSpotanim(SPLASH)
                            missSound(MISS_SYNTH)
                            onHit(soundToEffect(HIT_SYNTH))
                            spotanim("spotanim.firewave_impact", IMPACT_HEIGHT)
                        }
                    }
                }

            incoming {
                rule(Condition.HitStyle(Melee)) {
                    cap(0)
                    run(immune)
                }
                rule(Condition.HitStyle(Ranged)) { divideMinOne(RANGED_DIVISOR) }
            }

            phase("combat") {
                attackRate = ATTACK_RATE
                weightedSelectorRandom { +random(blast, weight = 1) }
            }
        }

    private val tentacleSpec =
        boss(TENTACLE, TENTACLE_WHIRLPOOL) {
            stats(attackRate = TENTACLE_ATTACK_RATE)

            val blast =
                ability("blast") {
                    anim("seq.tentacle_monster_attack")
                    projectile {
                        spotanim = "spotanim.waterwave_travel"
                        travel = "projanim.magic_spell"
                        hit {
                            damage(Accuracy(Roll(0..TENTACLE_MAX), kind = MagicalRanged))
                            type(Magic)
                            penetration(TYPELESS_PENETRATION)
                            missSpotanim(SPLASH)
                            missSound(MISS_SYNTH)
                            onHit(soundToEffect(HIT_SYNTH))
                            spotanim("spotanim.waterwave_impact", IMPACT_HEIGHT)
                        }
                    }
                }

            phase("combat") { weightedSelectorRandom { +random(blast, weight = 1) } }
        }

    private companion object {
        private const val BOSS = "npc.slayer_kraken_boss"
        private const val WHIRLPOOL = "npc.slayer_kraken_boss_whirlpool"
        private const val TENTACLE = "npc.slayer_kraken_boss_tentacle"
        private const val TENTACLE_WHIRLPOOL = "npc.slayer_kraken_boss_tentacle_whirlpool"
        private const val TENTACLE_ARISE_ANIM = "seq.tentacle_monster_spawn"

        private const val EXPLOSIVE = "obj.fishing_explosive"
        private const val EXPLOSIVE_LAUNCH_HEIGHT = 96
        private const val EXPLOSIVE_RANGE = 6
        private const val EXPLOSIVE_START_HEIGHT = 163
        private const val EXPLOSIVE_END_HEIGHT = 30
        private const val EXPLOSIVE_START_TIME = 41
        private const val EXPLOSIVE_ANGLE = 40
        private const val EXPLOSIVE_PROGRESS = 11
        private const val EXPLOSIVE_LANDING_OFFSET = 15
        private const val CLIENT_CYCLES_PER_TICK = 30
        private const val EXPLOSIVE_TENTACLE_DELAY = 8
        private const val EXPLOSIVE_BOSS_DELAY = 2

        private const val CAST_SYNTH = "synth.waterwave_cast_and_fire"
        private const val CAST_SOUND_RADIUS = 10
        private const val HIT_SYNTH = "synth.waterwave_hit"
        private const val MISS_SYNTH = "synth.spellfail"
        private const val BOSS_ARISE_SYNTH = "synth.sea_troll_queen_attack"
        private const val TENTACLE_ARISE_SYNTH = "synth.poh_tentacle"

        private const val ATTACK_RATE = 4
        private const val MAGIC_MAX = 28
        private const val TENTACLE_ATTACK_RATE = 4
        private const val TENTACLE_MAX = 2

        // UNVERIFIED: tentacle search radius
        private const val TENTACLE_ZONE_RADIUS = 2
        private const val TYPELESS_PENETRATION = 100
        private const val BOSS_WAKE_ATTACK_DELAY = 3
        private const val TENTACLE_WAKE_ATTACK_DELAY = 1
        private const val RESPAWN_JITTER = 4
    }
}
