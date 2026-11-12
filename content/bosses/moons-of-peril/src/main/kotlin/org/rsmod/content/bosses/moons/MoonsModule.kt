package org.rsmod.content.bosses.moons

import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.PlayerDeathPreDropHook
import org.rsmod.api.player.hook.HeldEquipValidateHook
import org.rsmod.api.player.hook.NeypotzliHook
import org.rsmod.content.bosses.moons.blue.BlueWearPreventHook
import org.rsmod.content.bosses.moons.blue.FrozenWeaponDeathHook
import org.rsmod.content.bosses.moons.blue.IcicleAttackHook
import org.rsmod.plugin.module.PluginModule

class MoonsModule : PluginModule() {
    override fun bind() {
        newSetBinding<NeypotzliHook>()
        addSetBinding<HeldEquipValidateHook>(BlueWearPreventHook::class.java)
        addSetBinding<NpcAttackValidateHook>(IcicleAttackHook::class.java)
        addSetBinding<PlayerDeathPreDropHook>(FrozenWeaponDeathHook::class.java)
    }
}
