package org.rsmod.api.combat.weapon.styles

import dev.openrune.types.ItemServerType
import dev.openrune.util.WeaponCategory
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap
import org.rsmod.api.combat.commons.styles.AttackStyle
import org.rsmod.api.combat.weapon.righthand
import org.rsmod.api.enums.WeaponAttackEnums.weapon_attack_styles
import org.rsmod.game.entity.Player
import org.rsmod.game.type.getOrNull

public class AttackStyles {
    private lateinit var weaponStyles: WeaponStyleMap

    public fun get(player: Player): AttackStyle? {
        val type = getOrNull(player.righthand)
        val stance = player.vars["varp.com_mode"]
        return resolve(type = type, combatStance = stance)
    }

    public fun resolve(type: ItemServerType?, combatStance: Int): AttackStyle? {
        val weapon = WeaponCategory.getOrUnarmed(type?.weaponCategory?.id)
        return resolve(weapon = weapon, combatStance = combatStance)
    }

    public fun resolve(weapon: WeaponCategory, combatStance: Int): AttackStyle? {
        require(combatStance in 0..3) { "Combat stance must be within range [0..3]" }
        val styles = weaponStyles[weapon]
        return styles[combatStance]
    }

    internal fun startup() {
        val weaponStyles = loadWeaponStylesMap()
        this.weaponStyles = weaponStyles
    }

    private fun loadWeaponStylesMap(): WeaponStyleMap {
        val stylesEnum = weapon_attack_styles.filterValuesNotNull()
        return WeaponStyleMap(Int2IntOpenHashMap(stylesEnum.backing))
    }

    private data class WeaponStyleList(
        val one: AttackStyle?,
        val two: AttackStyle?,
        val three: AttackStyle?,
        val four: AttackStyle?,
    ) {
        operator fun get(index: Int): AttackStyle? =
            when (index) {
                0 -> one
                1 -> two
                2 -> three
                3 -> four
                else -> throw IndexOutOfBoundsException("Invalid index: $index")
            }
    }

    private class WeaponStyleMap(private val backing: Int2IntOpenHashMap = Int2IntOpenHashMap()) {
        operator fun get(weapon: WeaponCategory): WeaponStyleList {
            val packedStyles = backing[weapon.id]
            if (packedStyles == backing.defaultReturnValue()) {
                return WeaponStyleList(null, null, null, null)
            }
            val styles = PackedStyles(packedStyles)
            val (style1, style2, style3, style4) = styles
            return WeaponStyleList(style1, style2, style3, style4)
        }
    }
}
