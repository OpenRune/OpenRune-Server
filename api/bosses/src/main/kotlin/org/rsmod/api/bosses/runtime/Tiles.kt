package org.rsmod.api.bosses.runtime

import org.rsmod.api.bosses.spec.TargetExpr
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

internal val Npc.centreTile: CoordGrid
    get() = coords.translate(size / 2, size / 2)

/**
 * The one place a [TargetExpr.Single] becomes a tile, shared by effects and conditions. Bound
 * tiles are only set inside a projectile's `onImpact` / an `onTiles`. Without [randomWalkable]
 * (conditions have no collision or random), [TargetExpr.RandomWalkableTile] resolves to its centre.
 */
internal fun Npc.resolveTile(
    expr: TargetExpr.Single,
    target: Player,
    impactTile: CoordGrid? = null,
    currentTile: CoordGrid? = null,
    randomWalkable: ((center: CoordGrid, radius: Int) -> CoordGrid?)? = null,
): CoordGrid {
    fun resolve(inner: TargetExpr.Single) =
        resolveTile(inner, target, impactTile, currentTile, randomWalkable)
    return when (expr) {
        is TargetExpr.CurrentTarget,
        is TargetExpr.CurrentTargetTile,
        is TargetExpr.HighestDamageDealer,
        is TargetExpr.LowestPrayer,
        is TargetExpr.RandomNearby -> target.coords
        is TargetExpr.Self -> coords
        is TargetExpr.Centre -> centreTile
        is TargetExpr.SpawnTile -> spawnCoords.translate(expr.dx, expr.dz)
        is TargetExpr.Toward -> {
            val from = resolve(expr.from)
            Angles.step(from, Angles.bearing(from, resolve(expr.to)), expr.distance)
        }
        is TargetExpr.RandomWalkableTile -> {
            val center = resolve(expr.of)
            randomWalkable?.invoke(center, expr.radius) ?: center
        }
        is TargetExpr.ImpactTile ->
            checkNotNull(impactTile) { "ImpactTile resolved outside a Projectile.onImpact." }
        is TargetExpr.CurrentTile ->
            checkNotNull(currentTile) { "CurrentTile resolved outside an OnTiles." }
    }
}
