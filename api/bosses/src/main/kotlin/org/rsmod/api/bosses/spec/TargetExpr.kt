package org.rsmod.api.bosses.spec

import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

sealed interface TargetExpr {
    sealed interface Single : TargetExpr
    sealed interface Multi : TargetExpr

    data object CurrentTarget : Single
    data object CurrentTargetTile : Single
    data object Self : Single
    data object HighestDamageDealer : Single
    data object LowestPrayer : Single
    data object RandomNearby : Single

    /** The caster's centre tile (its south-west tile shifted by half its size). */
    data object Centre : Single

    /** The tile [distance] tiles from [from] along the bearing towards [to]. */
    data class Toward(val from: Single, val to: Single, val distance: Double) : Single

    /** The boss's spawn tile, shifted by ([dx], [dz]). */
    data class SpawnTile(val dx: Int = 0, val dz: Int = 0) : Single

    data class RandomWalkableTile(val radius: Int, val of: Single = Self) : Single

    data class Offset(val of: Single, val dx: Int, val dz: Int) : Single

    data class Custom(val tile: (npc: Npc, target: Player) -> CoordGrid) : Single

    data class Bound(val name: String) : Single

    /** Re-rolled on every resolve; throws on an empty set, so check [Condition.TilesEmpty] first. */
    data class RandomOfBound(val name: String) : Single

    /**
     * The tile a [Effect.Projectile] just landed on. Only valid inside that same
     * [Effect.Projectile.onImpact]; resolving it anywhere else throws.
     */
    data object ImpactTile : Single

    /**
     * The tile an [Effect.OnTiles] is currently running its effect for. Only valid inside that
     * [Effect.OnTiles]; resolving it anywhere else throws.
     */
    data object CurrentTile : Single

    data class PlayersIn(val area: Area) : Multi

    data class PlayersOn(val tile: Single) : Multi

    data class AllInRadius(val radius: Int, val of: Single = Self) : Multi
    data class TopN(val n: Int, val by: Single) : Multi

    data class FacingQuadrant(val reach: Int = 1) : Multi
}
