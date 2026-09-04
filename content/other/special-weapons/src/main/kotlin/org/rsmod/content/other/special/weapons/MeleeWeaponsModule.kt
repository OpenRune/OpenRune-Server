package org.rsmod.content.other.special.weapons

import org.rsmod.api.weapons.WeaponMap
import org.rsmod.content.other.special.weapons.melee.CrimsonKistenWeapons
import org.rsmod.content.other.special.weapons.melee.DogswordWeapons
import org.rsmod.content.other.special.weapons.melee.DualMacuahuitlWeapons
import org.rsmod.content.other.special.weapons.melee.FangOfTheHoundWeapons
import org.rsmod.content.other.special.weapons.melee.MultiHitMeleeWeapons
import org.rsmod.content.other.special.weapons.melee.NoxiousHalberdWeapons
import org.rsmod.content.other.special.weapons.melee.ScytheOfViturWeapons
import org.rsmod.content.other.special.weapons.melee.SoulreaperAxeWeapons
import org.rsmod.content.other.special.weapons.melee.SunspearWeapons
import org.rsmod.content.other.special.weapons.melee.ThunderKhopeshWeapons
import org.rsmod.plugin.module.PluginModule

class MeleeWeaponsModule : PluginModule() {
    override fun bind() {
        addSetBinding<WeaponMap>(ScytheOfViturWeapons::class.java)
        addSetBinding<WeaponMap>(MultiHitMeleeWeapons::class.java)
        addSetBinding<WeaponMap>(DualMacuahuitlWeapons::class.java)
        addSetBinding<WeaponMap>(SoulreaperAxeWeapons::class.java)
        addSetBinding<WeaponMap>(ThunderKhopeshWeapons::class.java)
        addSetBinding<WeaponMap>(FangOfTheHoundWeapons::class.java)
        addSetBinding<WeaponMap>(CrimsonKistenWeapons::class.java)
        addSetBinding<WeaponMap>(DogswordWeapons::class.java)
        addSetBinding<WeaponMap>(SunspearWeapons::class.java)
        addSetBinding<WeaponMap>(NoxiousHalberdWeapons::class.java)
    }
}
