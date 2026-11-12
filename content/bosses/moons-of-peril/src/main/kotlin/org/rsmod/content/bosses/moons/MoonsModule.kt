package org.rsmod.content.bosses.moons

import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.PlayerDeathPreDropHook
import org.rsmod.api.player.hook.HeldEquipValidateHook
import org.rsmod.api.player.hook.NeypotzliHook
import org.rsmod.content.bosses.moons.blue.BlueWearPreventHook
import org.rsmod.content.bosses.moons.blue.FrozenWeaponDeathHook
import org.rsmod.content.bosses.moons.blue.IcicleAttackHook
import org.rsmod.api.player.hook.PlayerPostTickHook
import org.rsmod.content.skills.herblore.HerbloreHook
import org.rsmod.content.bosses.moons.neypotzli.MoonsHerbloreHook
import org.rsmod.content.bosses.moons.neypotzli.MoonsConsumableGate
import org.rsmod.content.bosses.moons.neypotzli.NeypotzliFizzleHook
import org.rsmod.content.other.consumables.ConsumableActivityGate
import org.rsmod.plugin.module.PluginModule

class MoonsModule : PluginModule() {
    override fun bind() {
        newSetBinding<NeypotzliHook>()
        addSetBinding<ConsumableActivityGate>(MoonsConsumableGate::class.java)
        addSetBinding<PlayerPostTickHook>(NeypotzliFizzleHook::class.java)
        addSetBinding<HerbloreHook>(MoonsHerbloreHook::class.java)
        addSetBinding<HeldEquipValidateHook>(BlueWearPreventHook::class.java)
        addSetBinding<NpcAttackValidateHook>(IcicleAttackHook::class.java)
        addSetBinding<PlayerDeathPreDropHook>(FrozenWeaponDeathHook::class.java)
    }
}
