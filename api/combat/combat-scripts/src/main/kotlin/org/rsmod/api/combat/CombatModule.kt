package org.rsmod.api.combat

import org.rsmod.api.combat.commons.npc.NpcMeleeRangeHook
import org.rsmod.plugin.module.PluginModule

internal class CombatModule : PluginModule() {
    override fun bind() {
        newSetBinding<NpcMeleeRangeHook>()
        bindInstance<NvPCombat>()
        bindInstance<PvNCombat>()
        bindInstance<PvPCombat>()
    }
}
