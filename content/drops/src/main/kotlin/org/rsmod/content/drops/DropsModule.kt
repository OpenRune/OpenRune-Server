package org.rsmod.content.drops

import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.droptable.DropRateBoosts
import org.rsmod.api.droptable.KillRollContext
import org.rsmod.api.droptable.toml.DropTableTomlResolver
import org.rsmod.content.drops.toml.ContentDropTableTomlResolver
import org.rsmod.content.slayer.core.MortimerAssignment
import org.rsmod.plugin.module.PluginModule

public class DropsModule : PluginModule() {
    override fun bind() {
        DropRateBoosts.clueScrollBoostPercent = { player, args ->
            MortimerAssignment.clueDropRateBoostPercent(player, args[KillRollContext.npc])
        }
        bind(DropTableTomlResolver::class.java).to(ContentDropTableTomlResolver::class.java)
        addSetBinding<NpcDeathKillHook>(NpcDropTableKillHook::class.java)
    }
}
