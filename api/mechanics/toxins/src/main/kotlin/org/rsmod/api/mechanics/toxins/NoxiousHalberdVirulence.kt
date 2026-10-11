package org.rsmod.api.mechanics.toxins

import org.rsmod.api.attr.AttributeKey
import org.rsmod.game.entity.Player

/**
 * Transient state granted by the noxious halberd's Virulence special attack.
 *
 * It is intentionally not persisted, and equipment content clears it as soon as the player
 * changes their right-hand weapon.
 */
public object NoxiousHalberdVirulence {
    private val minimumHit =
        AttributeKey<Int>(
            resetOnDeath = true,
            temp = true,
        )

    public fun activate(
        player: Player,
        damage: Int,
    ) {
        require(damage > 0) {
            "Virulence requires a positive cured poison or venom damage value."
        }
        player.attr[minimumHit] = damage
    }

    /**
     * Returns and clears the minimum damage for the next accurate halberd attack.
     */
    public fun consume(player: Player): Int? {
        val damage = player.attr[minimumHit] ?: return null
        player.attr.remove(minimumHit)
        return damage
    }

    public fun clear(player: Player) {
        player.attr.remove(minimumHit)
    }
}
