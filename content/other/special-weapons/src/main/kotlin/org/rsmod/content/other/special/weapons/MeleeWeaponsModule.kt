package org.rsmod.content.other.special.weapons

import org.rsmod.api.weapons.WeaponMap
import org.rsmod.content.other.special.weapons.melee.DualMacuahuitlWeapons
import org.rsmod.content.other.special.weapons.melee.MultiHitMeleeWeapons
import org.rsmod.content.other.special.weapons.melee.ScytheOfViturWeapons
import org.rsmod.plugin.module.PluginModule

class MeleeWeaponsModule : PluginModule() {
    override fun bind() {
        addSetBinding<WeaponMap>(ScytheOfViturWeapons::class.java)
        addSetBinding<WeaponMap>(MultiHitMeleeWeapons::class.java)
        addSetBinding<WeaponMap>(DualMacuahuitlWeapons::class.java)
    }
}
