package org.rsmod.content.generic.killcount

import jakarta.inject.Inject
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.plugin.module.PluginModule

public class KillcountModule : PluginModule() {
    override fun bind() {
        addSetBinding<NpcDeathKillHook>(KillcountNpcKillHook::class.java)
    }
}

public class KillcountNpcKillHook
@Inject
constructor(private val registry: KillcountRegistry) : NpcDeathKillHook {
    override fun onKill(context: NpcDeathKillContext) {
        val npc = context.npc.type.internalName
        for (entry in registry.entriesFor(npc)) {
            val count = context.hero.vars[entry.varbit] + 1
            VarPlayerIntMapSetter.set(context.hero, entry.varbit, count)
            if (entry.notify) {
                context.hero.mes("Your ${context.npc.name} kill count is: <col=ff0000>$count</col>")
            }
        }
    }
}
