package org.rsmod.content.bosses.demonicgorilla

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.SpotanimType
import jakarta.inject.Inject
import org.rsmod.annotations.InternalApi
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.bossProjectile
import org.rsmod.api.bosses.runtime.encounter
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.ProjectileConfig
import org.rsmod.api.combat.commons.player.finishNpcHit
import org.rsmod.api.npc.events.NpcHitEvents
import org.rsmod.api.player.events.PlayerHitEvents
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.script.onEvent
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.hit.Hit
import org.rsmod.game.hit.HitType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private const val PHASE_MELEE = "melee"
private const val PHASE_RANGED = "ranged"
private const val PHASE_MAGIC = "magic"
private val PHASES = listOf(PHASE_MELEE, PHASE_RANGED, PHASE_MAGIC)

private val GORILLA_TYPE_NAMES: List<String> =
    listOf(
        "npc.mm2_demon_gorilla_1_melee",
        "npc.mm2_demon_gorilla_1_ranged",
        "npc.mm2_demon_gorilla_1_magic",
        "npc.mm2_demon_gorilla_2_melee",
        "npc.mm2_demon_gorilla_2_ranged",
        "npc.mm2_demon_gorilla_2_magic",
    )

class DemonicGorilla @Inject constructor(private val deps: BossDeps, private val npcList: NpcList) :
    PluginScript() {

    private val nameByTypeId: Map<Int, String> =
        GORILLA_TYPE_NAMES.associateBy {
            requireNotNull(it.npcTypeId()) { "Missing npc type: $it" }
        }

    val spec: BossSpec =
        boss(*GORILLA_TYPE_NAMES.toTypedArray()) {
            stats(attackRate = ATTACK_RATE, aggressionRadius = 8)

            val meleeAttack =
                ability("melee_attack") {
                    anim(MELEE_ATTACK_SEQ)
                    hit {
                        damage(Accuracy(Roll(0..MAX_HIT), meleeAttackType = MeleeAttackType.Crush))
                        type(Melee)
                        delay = MELEE_HIT_DELAY
                    }
                }
            val rangedAttack =
                ability("ranged_attack") {
                    anim(RANGED_ATTACK_SEQ)
                    projectile {
                        spotanim = RANGED_PROJECTILE_SPOT
                        config = RANGED_PROJECTILE_CONFIG
                        hit {
                            damage(Accuracy(Roll(0..MAX_HIT)))
                            type(Ranged)
                            spotanim(RANGED_IMPACT_SPOT)
                        }
                    }
                }
            val magicAttack =
                ability("magic_attack") {
                    anim(MAGIC_ATTACK_SEQ)
                    projectile {
                        spotanim = MAGIC_PROJECTILE_SPOT
                        config = MAGIC_PROJECTILE_CONFIG
                        hit {
                            damage(Accuracy(Roll(0..MAX_HIT)))
                            type(Magic)
                            spotanim(MAGIC_IMPACT_SPOT, height = MAGIC_IMPACT_HEIGHT)
                        }
                    }
                }

            val boulder =
                ability("boulder") {
                    anim(BOULDER_SEQ)
                    include(external(BOULDER_HANDLER))
                }

            phase(PHASE_MELEE) { weightedSelectorRandom { +random(meleeAttack, weight = 1) } }
            phase(PHASE_RANGED) {
                weightedSelectorRandom {
                    +random(rangedAttack, weight = 3)
                    +random(boulder, weight = 1)
                }
            }
            phase(PHASE_MAGIC) {
                weightedSelectorRandom {
                    +random(magicAttack, weight = 3)
                    +random(boulder, weight = 1)
                }
            }
        }

    override fun ScriptContext.startup() {
        BossCombat.register(this, spec, deps, onModifyHit = { onModifyProtectionHit(this) })
        deps.extensionRegistry.register(BOULDER_HANDLER) { _, npc, target, _ ->
            throwBoulder(npc, target)
        }

        val bossIds = spec.npcTypes.mapNotNullTo(mutableSetOf()) { it.npcTypeId() }
        onEvent<NpcStateEvents.Create> { if (npc.type.id in bossIds) resetGorilla(npc) }
        onEvent<NpcStateEvents.Respawn> { if (npc.type.id in bossIds) resetGorilla(npc) }
        onEvent<PlayerHitEvents.Impact> { onAttackImpact(bossIds, hit) }
    }

    private fun throwBoulder(npc: Npc, target: Player) {
        val tile = target.coords
        deps.bossProjectile(
            spotanim = BOULDER_TELEGRAPH_SPOT.asRSCM(RSCMType.SPOTANIM),
            src = tile,
            target = tile,
            startHeight = BOULDER_PROJ_START_HEIGHT,
            endHeight = BOULDER_PROJ_END_HEIGHT,
            delay = BOULDER_PROJ_START_DELAY,
            travel = BOULDER_PROJ_TRAVEL,
            curve = BOULDER_PROJ_ANGLE,
        )
        deps.worldQueues.add(BOULDER_WINDUP_TICKS) {
            deps.worldRepo.spotanimMap(
                SpotanimType(BOULDER_IMPACT_SPOT.asRSCM(RSCMType.SPOTANIM)),
                tile,
                BOULDER_IMPACT_HEIGHT,
            )
            deps.worldRepo.soundArea(tile, BOULDER_IMPACT_SOUND, radius = BOULDER_SOUND_RADIUS)
            if (target.hitpoints > 0 && target.coords == tile) {
                val damage = (target.hitpoints * BOULDER_DAMAGE_FRACTION).toInt()
                target.finishNpcHit(npc, 1, HitType.Typeless, damage, deps.playerHitModifier)
            }
        }
    }

    private fun resetGorilla(npc: Npc) {
        val protectStyle = nameByTypeId[npc.type.id]?.substringAfterLast('_') ?: return
        npc.vars["varn.gorilla_protect_damage"] = 0
        npc.vars["varn.gorilla_protect_hits"] = 0
        npc.vars["varn.gorilla_protect_switching"] = 0
        npc.vars["varn.gorilla_miss_streak"] = 0
        applyImmunity(npc, protectStyle)

        val startStyle = PHASES[deps.random.of(PHASES.size)]
        deps.encounter(npc).transitionTo(startStyle, deps.mapClock.cycle)
        applyStyleRange(npc, startStyle)
    }

    private fun applyStyleRange(npc: Npc, style: String) {
        npc.apRangeOverride = if (style == PHASE_MELEE) null else RANGED_MAGIC_AP_RANGE
    }

    private fun onModifyProtectionHit(event: NpcHitEvents.Modify) {
        if (!event.hit.isFromPlayer) return
        val style =
            when (event.hit.type) {
                HitType.Melee -> PHASE_MELEE
                HitType.Ranged -> PHASE_RANGED
                HitType.Magic -> PHASE_MAGIC
                else -> return
            }
        val npc = event.npc

        if (style == currentProtectStyle(npc)) return

        val damage = npc.vars["varn.gorilla_protect_damage"] + event.hit.damage
        val hits = npc.vars["varn.gorilla_protect_hits"] + 1
        npc.vars["varn.gorilla_protect_damage"] = damage
        npc.vars["varn.gorilla_protect_hits"] = hits

        val switchDue = hits >= PROTECT_HIT_THRESHOLD && damage >= PROTECT_DAMAGE_THRESHOLD
        if (switchDue && npc.vars["varn.gorilla_protect_switching"] == 0) {
            npc.vars["varn.gorilla_protect_switching"] = 1
            deps.worldQueues.add(1) {
                applyImmunity(npc, style)
                transmogToStyle(npc, style)
                npc.vars["varn.gorilla_protect_damage"] = 0
                npc.vars["varn.gorilla_protect_hits"] = 0
                npc.vars["varn.gorilla_protect_switching"] = 0
            }
        }
    }

    private fun currentProtectStyle(npc: Npc): String =
        when {
            npc.vars["varn.immune_melee"] == 1 -> PHASE_MELEE
            npc.vars["varn.immune_ranged"] == 1 -> PHASE_RANGED
            else -> PHASE_MAGIC
        }

    private fun applyImmunity(npc: Npc, style: String) {
        npc.vars["varn.immune_melee"] = if (style == PHASE_MELEE) 1 else 0
        npc.vars["varn.immune_ranged"] = if (style == PHASE_RANGED) 1 else 0
        npc.vars["varn.immune_magic"] = if (style == PHASE_MAGIC) 1 else 0
    }

    @OptIn(InternalApi::class)
    private fun transmogToStyle(npc: Npc, style: String) {
        val spawnName = nameByTypeId[npc.type.id] ?: return
        val newName = spawnName.substringBeforeLast('_') + "_" + style
        val npcType = newName.npcTypeId()?.let(ServerCacheManager::getNpc) ?: return
        npc.transmog(npcType, Int.MAX_VALUE)
        npc.assignUid()
    }

    private fun onAttackImpact(bossIds: Set<Int>, hit: Hit) {
        if (!hit.isFromNpc || hit.type == HitType.Typeless) return
        val npc = hit.resolveNpcSource(npcList) ?: return
        if (npc.type.id !in bossIds) return

        if (hit.damage > 0) {
            npc.vars["varn.gorilla_miss_streak"] = 0
            return
        }

        val encounter = deps.encounter(npc)
        val style = encounter.currentPhaseName
        val missStreak = npc.vars["varn.gorilla_miss_streak"] + 1
        if (missStreak >= MISS_STREAK_THRESHOLD) {
            val others = PHASES.filter { it != style }
            val next = others[deps.random.of(others.size)]
            encounter.transitionTo(next, deps.mapClock.cycle)
            applyStyleRange(npc, next)
            npc.vars["varn.gorilla_miss_streak"] = 0
        } else {
            npc.vars["varn.gorilla_miss_streak"] = missStreak
        }
    }

    private fun String.npcTypeId(): Int? = ServerCacheManager.getNpc(this.asRSCM(RSCMType.NPC))?.id

    private companion object {
        private const val ATTACK_RATE = 5
        private const val RANGED_MAGIC_AP_RANGE = 7
        private const val MAX_HIT = 30
        private const val MELEE_HIT_DELAY = 1

        private const val MISS_STREAK_THRESHOLD = 3
        private const val PROTECT_HIT_THRESHOLD = 4
        private const val PROTECT_DAMAGE_THRESHOLD = 70

        private const val BOULDER_WINDUP_TICKS = 5
        private const val BOULDER_DAMAGE_FRACTION = 0.33

        private const val BOULDER_SEQ = "seq.demonic_gorilla_smash_chest"
        private const val BOULDER_HANDLER = "demonicgorilla.boulder"
        private const val BOULDER_TELEGRAPH_SPOT = "spotanim.myarm_rock_roc_travel"
        private const val BOULDER_IMPACT_SPOT = "spotanim.castlewars_catapult_splash"
        private const val BOULDER_IMPACT_SOUND = "synth.mm2_gorilla_boulder_impact"
        private const val BOULDER_SOUND_RADIUS = 10
        private const val BOULDER_IMPACT_HEIGHT = 10
        private const val BOULDER_PROJ_START_HEIGHT = 600
        private const val BOULDER_PROJ_END_HEIGHT = 10
        private const val BOULDER_PROJ_START_DELAY = 15
        private const val BOULDER_PROJ_TRAVEL = 135
        private const val BOULDER_PROJ_ANGLE = 10

        private const val MELEE_ATTACK_SEQ = "seq.demonic_gorilla_punch"
        private const val RANGED_ATTACK_SEQ = "seq.demonic_gorilla_range"
        private const val MAGIC_ATTACK_SEQ = "seq.demonic_gorilla_magic"

        private const val RANGED_IMPACT_SPOT = "spotanim.mm2_gorilla_stonesplat"
        private const val MAGIC_IMPACT_SPOT = "spotanim.mm2_gorilla_spellsplat"
        private const val MAGIC_IMPACT_HEIGHT = 70

        private const val RANGED_PROJECTILE_SPOT = "spotanim.mm2_gorilla_stone"
        private const val MAGIC_PROJECTILE_SPOT = "spotanim.mm2_gorilla_spell"

        private val RANGED_PROJECTILE_CONFIG =
            ProjectileConfig(
                startHeight = 80,
                endHeight = 0,
                startDelay = 46,
                travelTime = 0,
                angle = 5,
                progress = 11,
                stepMultiplier = 5,
            )

        private val MAGIC_PROJECTILE_CONFIG =
            ProjectileConfig(
                startHeight = 80,
                endHeight = 70,
                startDelay = 46,
                travelTime = 0,
                angle = 5,
                progress = 11,
                stepMultiplier = 4,
            )
    }
}
