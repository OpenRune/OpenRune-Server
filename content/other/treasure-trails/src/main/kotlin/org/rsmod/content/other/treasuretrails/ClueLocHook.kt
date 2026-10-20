package org.rsmod.content.other.treasuretrails

import dev.openrune.ServerCacheManager
import org.rsmod.api.player.hook.PlayerLocOpHook
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.treasuretrails.TrailFlow.advance
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.loc.BoundLocInfo

class ClueLocHook : PlayerLocOpHook {
    override fun claims(player: Player, loc: BoundLocInfo, op: InteractionOp): Boolean =
        isSearchOp(loc, op) && searchTrail(player, loc) != null

    override suspend fun ProtectedAccess.op(loc: BoundLocInfo, op: InteractionOp) {
        arriveDelay()
        faceLoc(loc)
        val trail = searchTrail(player, loc) ?: return
        val target = ClueSteps.target(player, trail.step)
        if (!ClueOutfits.wearing(player, trail.step)) {
            mes("Nothing interesting happens.")
            return
        }
        if (target is ClueTarget.OpenWithKey) {
            val key = target.row.key.symbol()
            if (inv.count(key) == 0) {
                mes("It's locked.")
                return
            }
            if (invDel(inv, key, 1).failure) {
                return
            }
        }
        val given = advance(trail.tier) ?: return
        val text = TrailTiers.foundText(trail.tier, given)
        startDialogue { objbox(given.symbol(), text) }
    }

    private fun searchTrail(player: Player, loc: BoundLocInfo): HeldTrail? =
        TrailFlow.heldTrails(player).firstOrNull { trail ->
            when (val target = ClueSteps.target(player, trail.step)) {
                is ClueTarget.Search ->
                    loc.bounds().asSequence().any { it == target.row.coord } &&
                        loc.id in setOfNotNull(target.row.loc.id, target.row.fallbackLoc?.id)
                is ClueTarget.OpenWithKey ->
                    loc.coords == target.row.locCoord && loc.id == target.row.loc.id
                else -> false
            }
        }

    private fun isSearchOp(loc: BoundLocInfo, op: InteractionOp): Boolean {
        val actions = ServerCacheManager.getObject(loc.id)?.actions ?: return false
        val search =
            (0 until MAX_OPS).firstOrNull {
                actions.getOpOrNull(it).equals(SEARCH_OP, ignoreCase = true)
            }
        return if (search != null) op.slot - 1 == search else op == InteractionOp.Op1
    }

    private companion object {
        const val MAX_OPS = 5
        const val SEARCH_OP = "Search"
    }
}
