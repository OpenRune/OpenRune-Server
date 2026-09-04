package org.rsmod.content.other.special.weapons

import org.rsmod.api.weapons.WeaponMap
import org.rsmod.content.other.special.weapons.ranged.BlowpipeWeapons
import org.rsmod.content.other.special.weapons.ranged.CrawsBowWeapons
import org.rsmod.content.other.special.weapons.ranged.DarkBowWeapons
import org.rsmod.content.other.special.weapons.ranged.WebweaverBowWeapons
import org.rsmod.plugin.module.PluginModule

class RangedWeaponsModule : PluginModule() {
    override fun bind() {
        addSetBinding<WeaponMap>(BlowpipeWeapons::class.java)
        addSetBinding<WeaponMap>(CrawsBowWeapons::class.java)
        addSetBinding<WeaponMap>(DarkBowWeapons::class.java)
        addSetBinding<WeaponMap>(WebweaverBowWeapons::class.java)
    }
}
