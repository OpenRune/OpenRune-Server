package org.rsmod.api.combat.commons

import dev.openrune.types.ItemServerType
import org.rsmod.api.combat.commons.magic.MagicSpellChecks
import org.rsmod.game.hit.HitType

public object DemonbaneChecks {
    public fun isDemonbaneWeapon(weapon: ItemServerType): Boolean =
        weapon.isAnyType(
            "obj.silverlight",
            "obj.agrith_silverlight_dyed",
            "obj.darklight",
            "obj.arclight",
            "obj.emberlight",
            "obj.bone_claws",
            "obj.scorching_bow",
        )

    /** Whether an attack of [type] is demonbane: by its spell for magic, else by its weapon. */
    public fun isDemonbane(
        type: HitType,
        righthand: ItemServerType?,
        secondary: ItemServerType?,
    ): Boolean =
        when (type) {
            HitType.Magic -> secondary != null && MagicSpellChecks.isDemonbaneSpell(secondary)
            HitType.Melee,
            HitType.Ranged -> righthand != null && isDemonbaneWeapon(righthand)
            HitType.Typeless -> false
        }
}
