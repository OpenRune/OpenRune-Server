package org.rsmod.content.events.demonicpacts

import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.plugin.module.PluginModule

class DemonicPactsModule : PluginModule() {
    override fun bind() {
        addSetBinding<NpcDeathKillHook>(LeagueKillHook::class.java)
    }
}

internal class LeagueKillHook : NpcDeathKillHook {
    override fun onKill(context: NpcDeathKillContext) {
        val player = context.hero
        if (player.isDemonicPactsLeague) {
            LeagueTasks.onKill(player, context.npc.name)
        }
    }
}
