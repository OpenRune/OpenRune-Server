package org.rsmod.content.other.special.weapons

import org.rsmod.api.weapons.WeaponMap
import org.rsmod.content.other.special.weapons.magic.BoneStaffWeapons
import org.rsmod.content.other.special.weapons.magic.PoweredStaffWeapons
import org.rsmod.content.other.special.weapons.magic.RevenantSceptreWeapons
import org.rsmod.content.other.special.weapons.magic.StarterStaffWeapons
import org.rsmod.content.other.special.weapons.magic.TumekensShadowWeapons
import org.rsmod.plugin.module.PluginModule

class MagicWeaponsModule : PluginModule() {
    override fun bind() {
        addSetBinding<WeaponMap>(TumekensShadowWeapons::class.java)
        addSetBinding<WeaponMap>(PoweredStaffWeapons::class.java)
        addSetBinding<WeaponMap>(RevenantSceptreWeapons::class.java)
        addSetBinding<WeaponMap>(BoneStaffWeapons::class.java)
        addSetBinding<WeaponMap>(StarterStaffWeapons::class.java)
    }
}
