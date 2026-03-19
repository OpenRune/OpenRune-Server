package org.rsmod.api.combat.weapon.types

import dev.openrune.types.ItemServerType
import dev.openrune.util.WeaponCategory
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap
import org.rsmod.api.combat.commons.types.AttackType
import org.rsmod.api.combat.weapon.righthand
import org.rsmod.api.combat.weapon.types.configs.type_enums
import org.rsmod.api.config.refs.varps
import org.rsmod.game.entity.Player
import org.rsmod.game.type.getOrNull

public class AttackTypes {
    private lateinit var weaponTypes: WeaponTypeMap

    public fun get(player: Player): AttackType? {
        val type = getOrNull(player.righthand)
        val stance = player.vars[varps.com_mode]
        return resolve(type = type, combatStance = stance)
    }

    public fun resolve(type: ItemServerType?, combatStance: Int): AttackType? {
        val weapon = WeaponCategory.getOrUnarmed(type?.weaponCategory?.id)
        return resolve(weapon = weapon, combatStance = combatStance)
    }

    public fun resolve(weapon: WeaponCategory, combatStance: Int): AttackType? {
        require(combatStance in 0..3) { "Combat stance must be within range [0..3]" }
        val types = weaponTypes[weapon]
        return types[combatStance]
    }

    internal fun startup() {
        val weaponTypes = loadWeaponTypeMap()
        this.weaponTypes = weaponTypes
    }

    private fun loadWeaponTypeMap(): WeaponTypeMap {
        val typesEnum = type_enums.weapon_attack_types.filterValuesNotNull()
        return WeaponTypeMap(Int2IntOpenHashMap(typesEnum.backing))
    }

    private data class WeaponTypeList(
        val one: AttackType?,
        val two: AttackType?,
        val three: AttackType?,
        val four: AttackType?,
    ) {
        operator fun get(index: Int): AttackType? =
            when (index) {
                0 -> one
                1 -> two
                2 -> three
                3 -> four
                else -> throw IndexOutOfBoundsException("Invalid index: $index")
            }
    }

    private class WeaponTypeMap(private val backing: Int2IntOpenHashMap = Int2IntOpenHashMap()) {
        operator fun get(weapon: WeaponCategory): WeaponTypeList {
            val packedStyles = backing[weapon.id]
            if (packedStyles == backing.defaultReturnValue()) {
                return WeaponTypeList(null, null, null, null)
            }
            val styles = PackedTypes(packedStyles)
            val (style1, style2, style3, style4) = styles
            return WeaponTypeList(style1, style2, style3, style4)
        }
    }
}
