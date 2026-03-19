package org.rsmod.api.spells

import com.google.inject.Provider
import org.rsmod.plugin.module.PluginModule

public class MagicSpellModule : PluginModule() {
    override fun bind() {
        bindProvider(RepositoryProvider::class.java)
    }

    // Since other plugin scripts rely on `MagicSpellRegistry` always being populated with the
    // correct data, we need to ensure its instance calls `init` before any other script startup.
    // To do this, we manually construct the instance and call `init` before handing it off for
    // injection.
    private class RepositoryProvider : Provider<MagicSpellRegistry> {
        override fun get(): MagicSpellRegistry {
            val registry = MagicSpellRegistry()
            registry.init()
            return registry
        }
    }
}
