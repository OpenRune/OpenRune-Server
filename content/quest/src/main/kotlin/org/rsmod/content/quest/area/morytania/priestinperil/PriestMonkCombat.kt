package org.rsmod.content.quest.area.morytania.priestinperil

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.ProjectileConfig
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.player.stat.statDrain
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** The temple monks use physical punches as well as spells, at their native four-tick rate. */
class PriestMonkCombat @Inject constructor(private val deps: BossDeps) : PluginScript() {
    override fun ScriptContext.startup() {
        registerDebuff(WEAKEN, "stat.strength", "spotanim.weaken_impact")
        registerDebuff(CONFUSE, "stat.attack", "spotanim.confuse_impact")
        registerDebuff(CURSE, "stat.defence", "spotanim.curse_impact")
        BossCombat.register(this, ordinary, deps)
        BossCombat.register(this, hooded, deps)
    }

    private fun registerDebuff(key: String, stat: String, impact: String) {
        deps.extensionRegistry.register(key) { _, npc, target, _ ->
            if (!npc.isValidTarget() || !target.isValidTarget()) return@register
            if (deps.accuracy.rollMagicAccuracy(npc, target, deps.random)) {
                target.statDrain(stat, 0, 5)
                target.spotanim(impact, height = 124)
            } else target.spotanim("spotanim.failedspell_impact", height = 124)
        }
    }

    internal val ordinary = monkSpec(false)
    internal val hooded = monkSpec(true)

    private fun monkSpec(hooded: Boolean): BossSpec {
        val types =
            if (hooded) listOf("npc.priestperilevilmonk3", "npc.priestperilevilmonk3_vis")
            else
                listOf(
                    "npc.priestperilevilmonk1",
                    "npc.priestperilevilmonk2",
                    "npc.priestperilevilmonk1_vis",
                    "npc.priestperilevilmonk2_vis",
                )
        return boss(*types.toTypedArray()) {
            stats(attackRate = 4)
            val punch =
                ability("punch") {
                    anim("seq.human_unarmedpunch")
                    hit {
                        damage(
                            Accuracy(
                                NpcMaxHit(MeleeAttackType.Crush),
                                meleeAttackType = MeleeAttackType.Crush,
                            )
                        )
                        type(Melee)
                        delay = 1
                    }
                }
            val weaken =
                ability("weaken") {
                    anim("seq.human_castweaken_walkmerge")
                    spotanim("spotanim.weaken_casting", height = 92)
                    sound("synth.weaken_all")
                    projectile(
                        spotanim = "spotanim.weaken_travel",
                        config = ProjectileConfig.fixed(172, 124, 44, 16, 16, 64),
                        onImpact = external(WEAKEN),
                    )
                }
            val confuse = if (!hooded) ability("confuse") {
                anim("seq.human_castconfuse_walkmerge")
                spotanim("spotanim.confuse_casting", height = 92)
                sound("synth.confuse_all")
                projectile(
                    spotanim = "spotanim.confuse_travel",
                    config = ProjectileConfig.fixed(172, 124, 61, 29, 16, 64),
                    onImpact = external(CONFUSE),
                )
            } else null
            val curse = if (!hooded) ability("curse") {
                anim("seq.human_castcurse_walkmerge")
                spotanim("spotanim.curse_casting", height = 92)
                sound("synth.curse_all")
                projectile(
                    spotanim = "spotanim.curse_travel",
                    config = ProjectileConfig.fixed(172, 124, 51, 39, 16, 64),
                    onImpact = external(CURSE),
                )
            } else null
            val fire =
                if (hooded)
                    ability("fire_bolt") {
                        anim("seq.human_caststrike_walkmerge")
                        spotanim("spotanim.firebolt_casting", height = 92)
                        sound("synth.firebolt_cast_and_fire")
                        projectile {
                            spotanim = "spotanim.firebolt_travel"
                            config = ProjectileConfig.fixed(172, 124, 51, 39, 16, 64)
                            hit {
                                damage(Accuracy(Roll(0..8)))
                                type(Magic)
                                spotanim("spotanim.firebolt_impact", height = 124)
                                missSpotanim("spotanim.failedspell_impact")
                            }
                        }
                    }
                else null
            phase("combat") {
                // These weights are provisional; the captures do not establish selection odds.
                weightedSelectorRandom {
                    +random(punch, weight = if (hooded) 1 else 3, requires = WithinMeleeRange)
                    +random(weaken, weight = 1, requires = WithinMeleeRange)
                    if (confuse != null) +random(confuse, weight = 1, requires = WithinMeleeRange)
                    if (curse != null) +random(curse, weight = 1, requires = WithinMeleeRange)
                    if (fire != null) +random(fire, weight = 1, requires = WithinMeleeRange)
                }
            }
        }
    }

    private companion object {
        const val WEAKEN = "priest_in_peril_weaken"
        const val CONFUSE = "priest_in_peril_confuse"
        const val CURSE = "priest_in_peril_curse"
    }
}
