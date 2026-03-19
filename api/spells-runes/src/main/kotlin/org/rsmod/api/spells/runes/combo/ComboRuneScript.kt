package org.rsmod.api.spells.runes.combo

import jakarta.inject.Inject
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class ComboRuneScript @Inject constructor(private val repo: ComboRuneRepository) :
    PluginScript() {
    override fun ScriptContext.startup() {
        repo.init()
    }
}
