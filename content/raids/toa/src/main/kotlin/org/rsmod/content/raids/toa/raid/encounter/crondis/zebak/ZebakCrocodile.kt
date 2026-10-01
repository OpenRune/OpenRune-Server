package org.rsmod.content.raids.toa.raid.encounter.crondis.zebak

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.Accuracy
import org.rsmod.api.bosses.dsl.Melee
import org.rsmod.api.bosses.dsl.boss
import org.rsmod.api.bosses.dsl.hit
import org.rsmod.api.bosses.dsl.npcMaxHit
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.runtime.suppressAttacks
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.plugin.scripts.ScriptContext

class ZebakCrocodile @Inject constructor(deps: BossDeps) : BossPluginScript(deps) {
    override fun ScriptContext.startup() {
        BossCombat.register(
            this,
            spec,
            deps,
            onCombatTick = { target ->
                if (!ZebakEncounter.onCrocCombatTick(npc, target)) deps.suppressAttacks(npc, 1)
            },
        )
    }

    override val spec: BossSpec =
        boss(ZebakNpcs.WATER_CROC) {
            stats(attackRate = BITE_RATE)
            ability(
                BITE,
                hit {
                    hazard()
                    type(Melee)
                    delay = HIT_DELAY
                    damage(Accuracy(npcMaxHit()))
                },
            )
            phase(HUNT) { rotationSelector { +then(BITE) } }
        }

    private companion object {
        const val BITE = "bite"
        const val HUNT = "hunt"
        const val BITE_RATE = 2
        const val HIT_DELAY = 1
    }
}
