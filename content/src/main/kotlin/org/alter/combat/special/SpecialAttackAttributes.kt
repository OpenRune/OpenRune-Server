package org.alter.combat.special

import org.alter.api.CombatAttributes
import org.alter.api.EquipmentType
import org.alter.api.ext.hasEquipped
import org.alter.game.model.entity.Pawn
import org.alter.game.model.entity.Player

/**
 * Checks that the current attack is an active special and the player has
 * one of the specified weapons equipped.
 */
fun isActiveSpecial(attacker: Pawn, vararg weapons: String): Boolean {
    val player = attacker as? Player ?: return false
    if (player.attr[CombatAttributes.SPECIAL_ATTACK_ACTIVE] != true) return false
    return player.hasEquipped(EquipmentType.WEAPON, *weapons)
}
