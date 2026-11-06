package org.rsmod.content.quest.area.morytania.priestinperil

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.player.stat.statDrain
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** The temple monks use physical punches as well as spells, at their native four-tick rate. */
class PriestMonkCombat @Inject constructor(private val deps: BossDeps) : PluginScript() {
    override fun ScriptContext.startup() {
        deps.extensionRegistry.register(WEAKEN) { _, npc, target, _ ->
            if (!npc.isValidTarget() || !target.isValidTarget()) return@register
            if (deps.accuracy.rollMagicAccuracy(npc, target, deps.random)) {
                target.statDrain("stat.strength", 0, 5)
                target.spotanim("spotanim.weaken_impact", height = 124)
            } else target.spotanim("spotanim.failedspell_impact", height = 124)
        }
        BossCombat.register(this, ordinary, deps)
        BossCombat.register(this, hooded, deps)
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
                    anim("seq.human_castweaken")
                    sound("synth.weaken_all")
                    projectile(
                        spotanim = "spotanim.weaken_travel",
                        travel = "projanim.magic_spell",
                        launch = "spotanim.weaken_casting",
                        onImpact = external(WEAKEN),
                    )
                }
            val fire =
                if (hooded)
                    ability("fire_bolt") {
                        anim("seq.human_caststrike")
                        sound("synth.firebolt_cast_and_fire")
                        projectile {
                            spotanim = "spotanim.firebolt_travel"
                            travel = "projanim.magic_spell"
                            launch = "spotanim.firebolt_casting"
                            hit {
                                damage(Accuracy(Roll(0..12)))
                                type(Magic)
                                spotanim("spotanim.firebolt_impact", height = 124)
                                missSpotanim("spotanim.failedspell_impact")
                            }
                        }
                    }
                else null
            phase("combat") {
                // Both styles are supported; exact native selection odds await a trace comparison.
                weightedSelectorRandom {
                    +random(punch, weight = 1, requires = WithinMeleeRange)
                    +random(weaken, weight = 1, requires = WithinMeleeRange)
                    if (fire != null) +random(fire, weight = 1, requires = WithinMeleeRange)
                }
            }
        }
    }

    private companion object {
        const val WEAKEN = "priest_in_peril_weaken"
    }
}
