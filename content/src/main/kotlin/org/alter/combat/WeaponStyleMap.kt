package org.alter.combat

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap


class WeaponStyleMap(private val backing: Map<Int, Int> = emptyMap()) {

    operator fun get(weapon: WeaponCategory): WeaponStyleList {
        val packedStyles = backing[weapon.id] ?: return WeaponStyleList(null, null, null, null)
        val styles = PackedStyles(packedStyles)
        val (style1, style2, style3, style4) = styles
        return WeaponStyleList(style1, style2, style3, style4)
    }
}