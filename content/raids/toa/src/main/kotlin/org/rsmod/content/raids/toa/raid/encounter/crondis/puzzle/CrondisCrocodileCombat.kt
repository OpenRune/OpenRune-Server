package org.rsmod.content.raids.toa.raid.encounter.crondis.puzzle

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.Accuracy
import org.rsmod.api.bosses.dsl.CurrentTarget
import org.rsmod.api.bosses.dsl.Melee
import org.rsmod.api.bosses.dsl.MeleeAttackType
import org.rsmod.api.bosses.dsl.TargetPraying
import org.rsmod.api.bosses.dsl.anim
import org.rsmod.api.bosses.dsl.boss
import org.rsmod.api.bosses.dsl.external
import org.rsmod.api.bosses.dsl.hit
import org.rsmod.api.bosses.dsl.sequence
import org.rsmod.api.bosses.dsl.sound
import org.rsmod.api.bosses.dsl.statDrain
import org.rsmod.api.bosses.dsl.whenever
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.runtime.suppressAttacks
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.DamageExpr
import org.rsmod.plugin.scripts.ScriptContext

class CrondisCrocodileCombat @Inject constructor(deps: BossDeps) : BossPluginScript(deps) {
    override fun ScriptContext.startup() {
        BossCombat.register(
            this,
            spec,
            deps,
            onCombatTick = { target ->
                if (!CrondisPuzzleEncounter.onCrocodileCombatTick(npc, target)) deps.suppressAttacks(npc, 1)
            },
        )
        deps.extensionRegistry.register(BITE_WATER) { _, croc, target, _ ->
            CrondisPuzzleEncounter.onCrocodileBite(croc, target)
        }
    }

    override val spec: BossSpec =
        boss(CrondisNpcs.CROCODILE) {
            stats(attackRate = ATTACK_RATE)
            ability(
                BITE,
                sequence(
                    anim(CrondisSeqs.CROC_ATTACK),
                    sound(CrondisSynths.CROC_ATTACK, target = CurrentTarget),
                    hit {
                        noReaction()
                        type(Melee)
                        delay = HIT_DELAY
                        damage(Accuracy(biteDamage, meleeAttackType = MeleeAttackType.Crush))
                        penetration(PRAYER_IN_DAMAGE)
                        onHit(
                            sequence(
                                whenever(TargetPraying(Melee), statDrain(PRAYER, amount = PRAYER_DRAIN)),
                                external(BITE_WATER),
                            ),
                        )
                    },
                ),
            )
            phase(HUNT) { rotationSelector { +then(BITE) } }
        }

    internal companion object {
        const val ATTACK_RATE = 7
        private const val BITE = "bite"
        private const val HUNT = "hunt"
        private const val BITE_WATER = "crondis.croc_bite_water"
        private const val HIT_DELAY = 1
        private const val PRAYER_IN_DAMAGE = 100
        private const val PRAYER = "stat.prayer"
        private const val PRAYER_DRAIN = 12

        private val biteDamage =
            DamageExpr.Custom { croc, target -> CrondisPuzzleEncounter.crocodileBiteDamage(croc, target) }
    }
}
