package org.rsmod.content.other.moonsets

import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.spells.attack.SpellCastHook
import org.rsmod.api.weapons.WeaponMap
import org.rsmod.plugin.module.PluginModule

class MoonSetsModule : PluginModule() {
    override fun bind() {
        addSetBinding<WeaponMap>(EclipseAtlatlWeapons::class.java)
        addSetBinding<WeaponMap>(BloodrageWeapons::class.java)
        addSetBinding<SpecialAttackMap>(MoonSpecialAttacks::class.java)
        addSetBinding<SpellCastHook>(Frostweaver::class.java)
    }
}
