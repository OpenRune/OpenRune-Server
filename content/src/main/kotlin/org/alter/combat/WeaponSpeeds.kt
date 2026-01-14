package org.alter.combat

import org.alter.api.Wearpos
import org.alter.api.ext.getEquipment
import org.alter.game.model.combat.AttackStyle
import org.alter.game.model.entity.Player
import kotlin.math.max

object WeaponSpeeds {

    fun base(player: Player): Int = player.getEquipment(Wearpos.RightHand)?.getDef()?.weapon?.attackSpeed ?: 4

    public fun actual(player: Player, style: AttackStyle?): Int {
        val baseSpeed = base(player)
        return if (style == AttackStyle.RapidRanged) {
            max(1, baseSpeed - 1)
        } else {
            baseSpeed
        }
    }

    // Avoids needing the [AttackStyle] dependency when calling `actual` unless explicitly required.
    public fun actual(player: Player): Int = actual(player, player.currentAttackStyle())

    private fun Player.currentAttackStyle(): AttackStyle? = AttackStyles.get(this)
}
