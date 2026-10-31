package org.rsmod.content.bosses.archaeologists

import jakarta.inject.Inject
import org.rsmod.api.config.refs.BaseParams
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.player.output.mes
import org.rsmod.plugin.module.PluginModule

class ArchaeologistsModule : PluginModule() {
    override fun bind() {
        addSetBinding<NpcDeathKillHook>(ArchaeologistsKillMessageHook::class.java)
    }
}

/**
 * The generic killcount hook increments the varp but uses the cache display name, so the NPC config
 * disables its notification and this hook prints the capitalised collection log name instead. It
 * runs after the generic hook to read the incremented value.
 */
class ArchaeologistsKillMessageHook @Inject constructor() : NpcDeathKillHook {
    override val priority: Int = 9

    override fun onKill(context: NpcDeathKillContext) {
        val npc = context.npc
        val name =
            when {
                npc.isType(CRAZY_ARCHAEOLOGIST) -> CRAZY_KILL_NAME
                npc.isType(DERANGED_ARCHAEOLOGIST) -> DERANGED_KILL_NAME
                else -> return
            }
        val varp = npc.paramOrNull(BaseParams.killcount_varp) ?: return
        val count = context.hero.vars[varp]
        context.hero.mes("Your $name kill count is: <col=ff0000>$count</col>.")
    }
}

private const val CRAZY_ARCHAEOLOGIST = "npc.crazy_archaeologist"
private const val DERANGED_ARCHAEOLOGIST = "npc.fossil_crazy_archaeologist"
private const val CRAZY_KILL_NAME = "Crazy Archaeologist"
private const val DERANGED_KILL_NAME = "Deranged Archaeologist"
