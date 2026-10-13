package org.rsmod.content.other.nerocurator

import org.rsmod.api.death.PlayerDeathDropHook
import org.rsmod.api.player.hook.PlayerPostTickHook
import org.rsmod.plugin.module.PluginModule

class CompletionistModule : PluginModule() {
    override fun bind() {
        addSetBinding<PlayerPostTickHook>(CompletionistVariantScript::class.java)
        addSetBinding<PlayerDeathDropHook>(CompletionistDeathHook::class.java)
    }
}
