package org.rsmod.api.player.ui

import dev.openrune.util.WeaponCategory
import org.rsmod.api.player.output.ClientScripts
import org.rsmod.api.player.righthand
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.game.entity.Player
import org.rsmod.game.type.getOrNull

private var Player.combatTabWeaponStyle: Int by intVarBit("varbit.combat_weapon_category")
private var Player.combatLvlWhole: Int by intVarBit("varbit.combatlevel_transmit")
private var Player.combatLvlDecimal: Int by intVarBit("varbit.combatlevel_decimal_transmit")

public object PlayerInterfaceUpdates {
    public fun updateCombatTab(
        player: Player,
        weaponName: String?,
        categoryId: Int,
        categoryName: String,
    ) {
        player.combatTabWeaponStyle = categoryId
        player.ifSetText("component.combat_interface:title", weaponName ?: "Unarmed")
        ClientScripts.pvpIconsComLevelRange(player, player.combatLevel)
        player.ifSetText("component.combat_interface:category", "Category: $categoryName")
        ClientScripts.pvpIconsComLevelRange(player, player.combatLevel)
    }

    public fun updateCombatTab(player: Player) {
        val righthandType = getOrNull(player.righthand)
        val weaponCategory = WeaponCategory.getOrUnarmed(righthandType?.weaponCategory?.id)
        updateCombatTab(player, righthandType?.name, weaponCategory.id, weaponCategory.text)
    }

    public fun updateWeaponCategoryText(player: Player) {
        val righthandType = getOrNull(player.righthand)
        val weaponCategory = WeaponCategory.getOrUnarmed(righthandType?.weaponCategory?.id)
        player.ifSetText("component.combat_interface:category", "Category: ${weaponCategory.text}")
    }

    public fun updateCombatLevel(player: Player) {
        player.combatLvlWhole = player.combatLevel
        player.combatLvlDecimal = player.combatLevelDecimal
    }
}
