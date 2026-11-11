package org.rsmod.content.areas.misc.warriorsguild

import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.death.PlayerDeathPreDropHook
import org.rsmod.content.areas.misc.warriorsguild.animator.AnimatedArmourAttackHook
import org.rsmod.content.areas.misc.warriorsguild.animator.AnimatedArmourKillHook
import org.rsmod.content.areas.misc.warriorsguild.kegs.KegDeathHook
import org.rsmod.plugin.module.PluginModule

class WarriorsGuildModule : PluginModule() {
    override fun bind() {
        addSetBinding<NpcDeathKillHook>(AnimatedArmourKillHook::class.java)
        addSetBinding<NpcAttackValidateHook>(AnimatedArmourAttackHook::class.java)
        addSetBinding<PlayerDeathPreDropHook>(KegDeathHook::class.java)
    }
}
