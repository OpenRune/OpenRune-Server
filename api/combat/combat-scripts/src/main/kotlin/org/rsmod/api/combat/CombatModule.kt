package org.rsmod.api.combat

import com.google.inject.multibindings.Multibinder
import org.rsmod.api.combat.commons.NpcRangedAccuracyHook
import org.rsmod.api.combat.commons.npc.NpcAttackHook
import org.rsmod.api.combat.commons.npc.NpcAttackOverrideHook
import org.rsmod.plugin.module.PluginModule

internal class CombatModule : PluginModule() {
    override fun bind() {
        Multibinder.newSetBinder(binder(), NpcAttackHook::class.java)
        Multibinder.newSetBinder(binder(), NpcAttackOverrideHook::class.java)
        Multibinder.newSetBinder(binder(), NpcRangedAccuracyHook::class.java)

        bindInstance<NvPCombat>()
        bindInstance<PvNCombat>()
        bindInstance<PvPCombat>()
    }
}
