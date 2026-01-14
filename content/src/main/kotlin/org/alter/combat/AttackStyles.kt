package org.alter.combat

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap
import org.alter.api.Wearpos
import org.alter.api.ext.getEquipment
import org.alter.api.ext.getVarp
import org.alter.game.model.combat.AttackStyle
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.util.enum
import org.alter.game.util.vars.IntType

object AttackStyles {

    var weaponStyles: WeaponStyleMap = loadWeaponStylesMap()

    init {
        println(weaponStyles.get(WeaponCategory.Whip))
    }

    public fun get(player: Player): AttackStyle? {
        val type = player.getEquipment(Wearpos.RightHand)?.id ?: return null
        val stance = player.getVarp("varp.com_mode")
        return resolve(type = type, combatStance = stance)
    }

    public fun resolve(type: Int, combatStance: Int): AttackStyle? {
        val weapon = WeaponCategory.getOrUnarmed(type)
        return resolve(weapon = weapon, combatStance = combatStance)
    }

    public fun resolve(weapon: WeaponCategory, combatStance: Int): AttackStyle? {
        require(combatStance in 0..3) { "Combat stance must be within range [0..3]" }
        val styles = weaponStyles[weapon]
        return styles[combatStance]
    }

    private fun loadWeaponStylesMap(): WeaponStyleMap {
        val stylesEnum = enum("enums.weapon_attack_styles", IntType, IntType)
        val map = stylesEnum.associate { it.value to it.key }
        return WeaponStyleMap(map)
    }

}