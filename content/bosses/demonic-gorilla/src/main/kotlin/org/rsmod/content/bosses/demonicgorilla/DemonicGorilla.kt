package org.rsmod.content.bosses.demonicgorilla

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.encounter
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.DamageExpr
import org.rsmod.api.npc.events.NpcHitEvents
import org.rsmod.api.player.events.PlayerHitEvents
import org.rsmod.api.script.onEvent
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.hit.Hit
import org.rsmod.game.hit.HitType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private const val PHASE_MELEE = "melee"
private const val PHASE_RANGED = "ranged"
private const val PHASE_MAGIC = "magic"
private val PHASES = listOf(PHASE_MELEE, PHASE_RANGED, PHASE_MAGIC)

class DemonicGorilla
@Inject
constructor(private val deps: BossDeps, private val npcList: NpcList) : PluginScript() {

    val spec: BossSpec =
        boss(
            "npc.mm2_demon_gorilla_1_melee",
            "npc.mm2_demon_gorilla_1_ranged",
            "npc.mm2_demon_gorilla_1_magic",
            "npc.mm2_demon_gorilla_2_melee",
            "npc.mm2_demon_gorilla_2_ranged",
            "npc.mm2_demon_gorilla_2_magic",
        ) {
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
                        hit {
                            damage(Accuracy(Roll(0..MAX_HIT)))
                            type(Magic)
                            spotanim(MAGIC_IMPACT_SPOT)
                        }
                    }
                }
            val boulder =
                ability("boulder") {
                    anim(BOULDER_SEQ)
                    debris(
                        telegraph = BOULDER_TELEGRAPH_SPOT,
                        impact = BOULDER_IMPACT_SPOT,
                        damage = DamageExpr.PercentOfTargetHp(BOULDER_DAMAGE_FRACTION),
                        type = Typeless,
                        windup = BOULDER_WINDUP_TICKS,
                        targetRadius = 0,
                        scatterRadius = 0,
                        count = 1..1,
                        center = CurrentTargetTile,
                    )
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
        BossCombat.register(this, spec, deps, onModifyHit = { GorillaProtection.onModifyHit(this) })

        val bossIds = spec.npcTypes.mapNotNullTo(mutableSetOf()) { it.npcTypeId() }
        onEvent<NpcStateEvents.Create> { if (npc.type.id in bossIds) GorillaProtection.reset(npc) }
        onEvent<NpcStateEvents.Respawn> {
            if (npc.type.id in bossIds) GorillaProtection.reset(npc)
        }
        onEvent<PlayerHitEvents.Impact> { onAttackImpact(bossIds, hit) }
    }

    /** Tracks the outgoing miss streak that drives the gorilla's style-switch trigger. */
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
            npc.say(SWITCH_SCREAM)
            val others = PHASES.filter { it != style }
            val next = others[deps.random.of(others.size)]
            encounter.transitionTo(next, deps.mapClock.cycle)
            npc.vars["varn.gorilla_miss_streak"] = 0
        } else {
            npc.vars["varn.gorilla_miss_streak"] = missStreak
        }
    }

    private fun String.npcTypeId(): Int? = ServerCacheManager.getNpc(this.asRSCM(RSCMType.NPC))?.id

    private companion object {
        private const val ATTACK_RATE = 5
        private const val MAX_HIT = 31
        private const val MELEE_HIT_DELAY = 1

        private const val MISS_STREAK_THRESHOLD = 3

        private const val BOULDER_WINDUP_TICKS = 2
        private const val BOULDER_DAMAGE_FRACTION = 0.33
        private const val SWITCH_SCREAM = "Rhaaaaaaa!"

        private const val BOULDER_SEQ = "seq.demonic_gorilla_smash_chest"
        private const val BOULDER_TELEGRAPH_SPOT = "spotanim.mm2_gorilla_stone"
        private const val BOULDER_IMPACT_SPOT = "spotanim.mm2_gorilla_stonesplat"

        private const val MELEE_ATTACK_SEQ = "seq.demonic_gorilla_punch"
        private const val RANGED_ATTACK_SEQ = "seq.demonic_gorilla_range"
        private const val MAGIC_ATTACK_SEQ = "seq.demonic_gorilla_magic"

        private const val RANGED_IMPACT_SPOT = "spotanim.mm2_gorilla_stonesplat"
        private const val MAGIC_IMPACT_SPOT = "spotanim.mm2_gorilla_spellsplat"

        private const val RANGED_PROJECTILE_SPOT = "spotanim.mm2_gorilla_stone"
        private const val MAGIC_PROJECTILE_SPOT = "spotanim.mm2_gorilla_spell"
    }
}

private object GorillaProtection {
    private const val PROTECT_HIT_THRESHOLD = 4
    private const val PROTECT_DAMAGE_THRESHOLD = 70

    fun reset(npc: Npc) {
        npc.vars["varn.gorilla_protect_damage"] = 0
        npc.vars["varn.gorilla_protect_hits"] = 0
        npc.vars["varn.gorilla_miss_streak"] = 0
        applyImmunity(npc, PHASES.random())
    }

    fun onModifyHit(event: NpcHitEvents.Modify) {
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
        if (hits >= PROTECT_HIT_THRESHOLD && damage >= PROTECT_DAMAGE_THRESHOLD) {
            npc.vars["varn.gorilla_protect_damage"] = 0
            npc.vars["varn.gorilla_protect_hits"] = 0
            applyImmunity(npc, style)
        } else {
            npc.vars["varn.gorilla_protect_damage"] = damage
            npc.vars["varn.gorilla_protect_hits"] = hits
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
}
