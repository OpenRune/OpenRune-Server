package org.rsmod.content.bosses.barrows

import jakarta.inject.Inject
import kotlin.random.Random
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.DamageExpr
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.ScriptContext

private const val MELEE_HIT_DELAY = 1

private fun meleeHit(type: MeleeAttackType, block: HitBuilder.() -> Unit = {}): Effect.Hit = hit {
    damage(Accuracy(NpcMaxHit(type), meleeAttackType = type))
    type(Melee)
    delay = MELEE_HIT_DELAY
    block()
}

private fun meleeBrother(
    brother: BarrowsBrother,
    attackRate: Int,
    anim: String,
    type: MeleeAttackType,
    effectSpot: String,
    effect: HitBuilder.() -> Unit,
): BossSpec =
    boss(brother.npc) {
        stats(attackRate = attackRate)
        val attack =
            ability("attack") {
                anim(anim)
                include(meleeHit(type))
            }
        val setEffect =
            ability("set_effect") {
                anim(anim)
                include(
                    meleeHit(type) {
                        spotanim(effectSpot)
                        effect()
                    }
                )
            }
        phase("combat") {
            weightedSelectorRandom(noRepeatBias = 0.0) {
                +random(attack, weight = 3)
                +random(setEffect, weight = 1)
            }
        }
    }

class ToragTheCorrupted @Inject constructor(deps: BossDeps) : BossPluginScript(deps) {
    override fun ScriptContext.startup() {
        deps.extensionRegistry.register(CORRUPTION) { _, _, target, _ ->
            target.runEnergy -= target.runEnergy / 5
        }
        BossCombat.register(this, spec, deps)
    }

    override val spec =
        meleeBrother(
            BarrowsBrother.TORAG,
            attackRate = 5,
            anim = "seq.barrow_torag_crush",
            type = MeleeAttackType.Crush,
            effectSpot = "spotanim.barrows_torag_effect",
        ) {
            onHit(external(CORRUPTION))
        }

    private companion object {
        const val CORRUPTION = "barrows_torag_corruption"
    }
}

class GuthanTheInfested @Inject constructor(deps: BossDeps) : BossPluginScript(deps) {
    override val spec =
        meleeBrother(
            BarrowsBrother.GUTHAN,
            attackRate = 5,
            anim = "seq.barrows_war_spear_stab",
            type = MeleeAttackType.Crush,
            effectSpot = "spotanim.barrows_guthan_effect",
        ) {
            lifesteal(100)
        }
}

class DharokTheWretched @Inject constructor(deps: BossDeps) : BossPluginScript(deps) {
    override val spec =
        boss(BarrowsBrother.DHAROK.npc) {
            stats(attackRate = 7)
            val attack =
                ability("attack") {
                    anim("seq.barrow_dharok_crush")
                    hit {
                        val wretched = DamageExpr.Custom { npc, _ -> Random.nextInt(maxHit(npc) + 1) }
                        damage(Accuracy(wretched, meleeAttackType = MeleeAttackType.Slash))
                        type(Melee)
                        delay = MELEE_HIT_DELAY
                    }
                }
            phase("combat") { weightedSelectorRandom { +random(attack, weight = 1) } }
        }

    private fun maxHit(npc: Npc): Int {
        val max = npc.baseHitpointsLvl
        val missing = (max - npc.hitpoints).coerceAtLeast(0)
        return (BASE_MAX_HIT * (1.0 + (missing / 100.0) * (max / 100.0))).toInt()
    }

    private companion object {
        const val BASE_MAX_HIT = 29
    }
}

class VeracTheDefiled @Inject constructor(deps: BossDeps) : BossPluginScript(deps) {
    override val spec =
        boss(BarrowsBrother.VERAC.npc) {
            stats(attackRate = 5)
            val attack =
                ability("attack") {
                    anim(ANIM)
                    include(meleeHit(MeleeAttackType.Stab))
                }
            val defiler =
                ability("defiler") {
                    anim(ANIM)
                    hit {
                        damage(Roll(0..MAX_HIT))
                        type(Melee)
                        penetration(PRAYER_PENETRATION)
                        spotanim("spotanim.barrows_verac_desolation")
                        delay = MELEE_HIT_DELAY
                    }
                }
            phase("combat") {
                weightedSelectorRandom(noRepeatBias = 0.0) {
                    +random(attack, weight = 3)
                    +random(defiler, weight = 1)
                }
            }
        }

    private companion object {
        const val ANIM = "seq.barrow_guthan_crush"
        const val MAX_HIT = 23
        const val PRAYER_PENETRATION = 67
    }
}

class KarilTheTainted @Inject constructor(deps: BossDeps) : BossPluginScript(deps) {
    override val spec =
        boss(BarrowsBrother.KARIL.npc) {
            stats(attackRate = 4)
            val attack = ability("attack") { shot() }
            val taintedShot =
                ability("tainted_shot") {
                    shot {
                        spotanim("spotanim.barrows_karil_tainted_shot", height = 96)
                        onHit(statDrainPercent("stat.agility", percent = 20))
                    }
                }
            phase("combat") {
                weightedSelectorRandom(noRepeatBias = 0.0) {
                    +random(attack, weight = 3)
                    +random(taintedShot, weight = 1)
                }
            }
        }

    private fun AbilityBuilder.shot(effect: HitBuilder.() -> Unit = {}) {
        anim("seq.barrows_repeating_crossbow_fire")
        projectile {
            spotanim = "spotanim.crossbowbolt_travel"
            travel = "projanim.bolt"
            hit {
                damage(Accuracy(NpcMaxHit()))
                type(Ranged)
                effect()
            }
        }
    }
}

internal enum class AhrimSpell(
    val anim: String,
    val cast: String,
    val travel: String,
    val impact: String,
    val drainedStat: String?,
) {
    FIRE_WAVE(
        "seq.human_castwave",
        "spotanim.firewave_casting",
        "spotanim.firewave_travel",
        "spotanim.firewave_impact",
        null,
    ),
    CONFUSE(
        "seq.human_castconfuse",
        "spotanim.confuse_casting",
        "spotanim.confuse_travel",
        "spotanim.confuse_impact",
        "stat.attack",
    ),
    WEAKEN(
        "seq.human_castweaken",
        "spotanim.weaken_casting",
        "spotanim.weaken_travel",
        "spotanim.weaken_impact",
        "stat.strength",
    ),
    CURSE(
        "seq.human_castcurse",
        "spotanim.curse_casting",
        "spotanim.curse_travel",
        "spotanim.curse_impact",
        "stat.defence",
    ),
}

class AhrimTheBlighted @Inject constructor(deps: BossDeps) : BossPluginScript(deps) {
    /**
     * Each spell has a Blighted Aura variant cast 1 in 5 times. Live captures show the aura
     * replacing the spell's impact graphic, sent without a delay on the cast tick, and only on
     * casts that hit.
     */
    override val spec =
        boss(BarrowsBrother.AHRIM.npc) {
            stats(attackRate = 6)
            val casts =
                AhrimSpell.entries.flatMap { spell ->
                    val weight = if (spell == AhrimSpell.FIRE_WAVE) FIRE_WAVE_WEIGHT else CURSE_WEIGHT
                    val plain =
                        ability(spell.name.lowercase()) { cast(spell, blighted = false) }
                    val blighted =
                        ability("${spell.name.lowercase()}_blighted") { cast(spell, blighted = true) }
                    listOf(plain to weight * (AURA_ODDS - 1), blighted to weight)
                }
            phase("combat") {
                weightedSelectorRandom(noRepeatBias = 0.0) {
                    for ((ability, weight) in casts) +random(ability, weight = weight)
                }
            }
        }

    private fun AbilityBuilder.cast(spell: AhrimSpell, blighted: Boolean) {
        anim(spell.anim)
        spotanim(spell.cast, height = CAST_HEIGHT)
        projectile {
            spotanim = spell.travel
            travel = "projanim.magic_spell"
            hit {
                damage(Accuracy(Roll(0..MAX_HIT)))
                type(Magic)
                missSpotanim("spotanim.failedspell_impact")
                if (blighted) {
                    spotanim("spotanim.barrows_ahirm_blighted_aura", AURA_HEIGHT, delay = 0)
                } else {
                    spotanim(spell.impact, IMPACT_HEIGHT)
                }
                landing(spell, blighted)?.let(::onHit)
            }
        }
    }

    private fun landing(spell: AhrimSpell, blighted: Boolean): Effect? {
        val effects =
            listOfNotNull(
                spell.drainedStat?.let { statDrainPercent(it, percent = CURSE_DRAIN_PERCENT) },
                if (blighted) statDrain("stat.strength", amount = AURA_STRENGTH_DRAIN) else null,
            )
        return if (effects.isEmpty()) null else sequence(*effects.toTypedArray())
    }

    private companion object {
        const val MAX_HIT = 20
        const val CAST_HEIGHT = 92
        const val IMPACT_HEIGHT = 124
        const val AURA_HEIGHT = 92
        const val CURSE_DRAIN_PERCENT = 5
        const val AURA_STRENGTH_DRAIN = 5
        const val AURA_ODDS = 5
        const val FIRE_WAVE_WEIGHT = 18
        const val CURSE_WEIGHT = 1
    }
}
