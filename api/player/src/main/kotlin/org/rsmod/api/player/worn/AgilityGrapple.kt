package org.rsmod.api.player.worn

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.util.Wearpos
import org.rsmod.api.random.GameRandom
import org.rsmod.game.entity.Player

/** Travel grapples accept every native crossbow, independently of its combat ammo rules. */
public object AgilityGrapple {
    private const val GRAPPLE = "obj.xbows_grapple_tip_bolt_mithril_rope"

    private fun ordinary(player: Player): Boolean = player.worn[Wearpos.Quiver.slot]?.let { it.id == GRAPPLE.asRSCM() && it.count > 0 } == true

    private fun extra(player: Player): Boolean = DizanasQuiver.recognises(player.worn[Wearpos.Back.slot]) && DizanasQuiver.stored(player)?.id == GRAPPLE.asRSCM()

    public fun equipped(player: Player): Boolean {
        val weapon = player.worn[Wearpos.RightHand.slot]?.id?.let { ServerCacheManager.getItem(it) } ?: return false
        return weapon.isEquipable && weapon.isCategoryType("category.crossbow") && (ordinary(player) || extra(player))
    }

    /** Called after a completed grapple use, never after denial/cancellation or barehanded travel. */
    public fun breakAfterUse(player: Player, random: GameRandom): Boolean {
        if (!equipped(player) || random.of(25) != 0) return false
        return consume(player)
    }

    /** The Observatory keeps the first grapple permanently as part of its rope. */
    public fun consume(player: Player): Boolean {
        if (!equipped(player)) return false
        if (ordinary(player)) {
            val item = player.worn[Wearpos.Quiver.slot] ?: return false
            player.worn[Wearpos.Quiver.slot] = if (item.count == 1) null else item.copy(count = item.count - 1)
            player.rebuildAppearance()
        } else {
            val item = DizanasQuiver.stored(player) ?: return false
            DizanasQuiver.store(player, if (item.count == 1) null else item.copy(count = item.count - 1))
        }
        return true
    }
}
