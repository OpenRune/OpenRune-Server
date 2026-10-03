package org.rsmod.content.other.special.attacks

import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.content.other.special.attacks.boost.StatBoostSpecialAttacks
import org.rsmod.content.other.special.attacks.magic.AyakSpecialAttack
import org.rsmod.content.other.special.attacks.magic.NightmareStaffSpecialAttacks
import org.rsmod.content.other.special.attacks.melee.DemonbaneSpecialAttacks
import org.rsmod.content.other.special.attacks.melee.DragonClawsSpecialAttack
import org.rsmod.content.other.special.attacks.melee.DragonLongswordSpecialAttack
import org.rsmod.content.other.special.attacks.melee.DragonTwoHandedSpecialAttack
import org.rsmod.content.other.special.attacks.melee.HalberdSpecialAttacks
import org.rsmod.content.other.special.attacks.melee.MeleeWeaponSpecialAttacks
import org.rsmod.content.other.special.attacks.melee.VoidwakerSpecialAttack
import org.rsmod.content.other.special.attacks.ranged.BlowpipeSpecialAttacks
import org.rsmod.content.other.special.attacks.ranged.DarkBowSpecialAttack
import org.rsmod.content.other.special.attacks.ranged.RangedWeaponSpecialAttacks
import org.rsmod.content.other.special.attacks.shield.DragonfireShieldSpecialAttacks
import org.rsmod.plugin.module.PluginModule

class SpecialAttackModule : PluginModule() {
    override fun bind() {
        addSetBinding<SpecialAttackMap>(DragonfireShieldSpecialAttacks::class.java)
        addSetBinding<SpecialAttackMap>(StatBoostSpecialAttacks::class.java)
        addSetBinding<SpecialAttackMap>(DarkBowSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(BlowpipeSpecialAttacks::class.java)
        addSetBinding<SpecialAttackMap>(DragonLongswordSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(DragonClawsSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(DemonbaneSpecialAttacks::class.java)
        addSetBinding<SpecialAttackMap>(HalberdSpecialAttacks::class.java)
        addSetBinding<SpecialAttackMap>(MeleeWeaponSpecialAttacks::class.java)
        addSetBinding<SpecialAttackMap>(VoidwakerSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(DragonTwoHandedSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(RangedWeaponSpecialAttacks::class.java)
        addSetBinding<SpecialAttackMap>(NightmareStaffSpecialAttacks::class.java)
        addSetBinding<SpecialAttackMap>(AyakSpecialAttack::class.java)
    }
}
