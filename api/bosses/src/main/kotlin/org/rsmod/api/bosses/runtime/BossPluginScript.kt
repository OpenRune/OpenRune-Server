package org.rsmod.api.bosses.runtime

import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

abstract class BossPluginScript(protected val deps: BossDeps) : PluginScript() {

    abstract val spec: BossSpec

    override fun ScriptContext.startup() {
        BossCombat.register(this, spec, deps)
    }
}
