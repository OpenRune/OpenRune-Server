package org.rsmod.content.bosses.dukesucellus

import jakarta.inject.Inject
import org.rsmod.api.config.refs.BaseParams
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.plugin.module.PluginModule

public class DukeSucellusModule : PluginModule() {
    override fun bind() {
        addSetBinding<NpcDeathKillHook>(DukeSucellusAwakenedKillHook::class.java)
    }
}

public class DukeSucellusAwakenedKillHook @Inject constructor() : NpcDeathKillHook {
    override fun onKill(context: NpcDeathKillContext) {
        if (context.npc.vars["varn.skip_killcount"] != 1) return
        if (!context.npc.isType("npc.duke_sucellus_asleep")) return
        val varp = context.npc.paramOrNull(BaseParams.killcount_varp_awakened) ?: return
        val count = context.hero.vars[varp] + 1
        VarPlayerIntMapSetter.set(context.hero, varp, count)
        val notify = context.npc.paramOrNull(BaseParams.killcount_notify) ?: true
        if (notify) {
            context.hero.mes("Your ${context.npc.name} (Awakened) kill count is: <col=ff0000>$count</col>")
        }
    }
}
