package org.rsmod.content.skills.magic.arceuus

import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.plugin.module.PluginModule

class ArceuusModule : PluginModule() {
    override fun bind() {
        addSetBinding<NpcDeathKillHook>(ReanimationKillHook::class.java)
        addSetBinding<NpcAttackValidateHook>(ReanimationAttackHook::class.java)
    }
}
