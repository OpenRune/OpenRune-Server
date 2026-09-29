package org.rsmod.content.raids.toa.raid.encounter.crondis.zebak

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.boss
import org.rsmod.api.bosses.dsl.external
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.plugin.scripts.ScriptContext

class ZebakCrocodile @Inject constructor(deps: BossDeps) : BossPluginScript(deps) {
    override fun ScriptContext.startup() {
        BossCombat.register(this, spec, deps)
        deps.extensionRegistry.register(BITE_HANDLER) { _, croc, target, _ ->
            ZebakEncounter.onCrocBite(croc, target)
        }
    }

    override val spec: BossSpec =
        boss(ZebakNpcs.WATER_CROC) {
            stats(attackRate = BITE_RATE)
            ability(BITE, external(BITE_HANDLER))
            phase(HUNT) { rotationSelector { +then(BITE) } }
        }

    private companion object {
        const val BITE = "bite"
        const val HUNT = "hunt"
        const val BITE_HANDLER = "zebak.croc_bite"
        const val BITE_RATE = 2
    }
}
