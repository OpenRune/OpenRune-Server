package org.rsmod.content.bosses.moons.neypotzli

import jakarta.inject.Inject
import java.util.IdentityHashMap
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.Condition
import org.rsmod.api.bosses.spec.DamageExpr
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.bosses.spec.StatDrainEntry
import org.rsmod.api.script.onEvent
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.plugin.scripts.ScriptContext

class SulphurNagua @Inject constructor(deps: BossDeps) : BossPluginScript(deps) {
    private val secondSplat = IdentityHashMap<Npc, Int>()

    override fun ScriptContext.startup() {
        BossCombat.register(this, spec, deps)
        onEvent<NpcStateEvents.Create> { if (npc.type.internalName == NAGUA) npc.setFlatArmour() }
        onEvent<NpcStateEvents.Respawn> { if (npc.type.internalName == NAGUA) npc.setFlatArmour() }
    }

    private fun Npc.setFlatArmour() {
        vars["varn.flat_armour"] = FLAT_ARMOUR
    }

    private val firstSplat =
        DamageExpr.Accuracy(
            on =
                DamageExpr.Custom { npc, _ ->
                    val total = deps.random.of(0, MAX_HIT)
                    val first = total / 2
                    secondSplat[npc] = total - first
                    first
                }
        )

    private val followingSplat = DamageExpr.Custom { npc, _ -> secondSplat.remove(npc) ?: 0 }

    private val prayerDrain =
        Effect.StatDrain(listOf(StatDrainEntry("stat.prayer", PRAYER_DRAIN, 1, 1)))

    override val spec =
        boss(NAGUA) {
            stats(attackRate = 4)

            val attack =
                ability("attack") {
                    anim(ATTACK_SEQ)
                    hit {
                        damage(firstSplat)
                        type(Melee)
                    }
                    hit {
                        damage(followingSplat)
                        type(Melee)
                    }
                }

            val smash =
                ability("prayer_smash") {
                    anim(SMASH_SEQ)
                    spotanim(SMASH_SPOTANIM, height = SMASH_HEIGHT, delay = SMASH_SPOTANIM_DELAY)
                    include(
                        after(
                            SMASH_DELAY,
                            onEach(
                                playersIn(area(offset(Self, -1, -1), offset(Self, 2, 2))),
                                whenever(
                                    Condition.Custom { npc, player ->
                                        player != null && npc.distanceTo(player) <= 1
                                    },
                                    prayerDrain,
                                ),
                            ),
                        )
                    )
                }

            phase("default") {
                weightedSelectorRandom { +random(attack, weight = 1) }
                forceEveryAttacks(SMASH_EVERY, SMASH_EVERY, smash)
            }
        }

    private companion object {
        private const val NAGUA = "npc.pmoon_sulphur_nagua"
        private const val ATTACK_SEQ = "seq.npc_djinn_04_melee"
        private const val SMASH_SEQ = "seq.npc_djinn_01_magic"
        private const val SMASH_SPOTANIM = "spotanim.lizardshaman_spawn_explode"
        private const val SMASH_HEIGHT = 150
        private const val SMASH_SPOTANIM_DELAY = 75
        private const val MAX_HIT = 11
        private const val FLAT_ARMOUR = -4
        private const val SMASH_EVERY = 5
        private const val SMASH_DELAY = 3
        private const val PRAYER_DRAIN = 5
    }
}
