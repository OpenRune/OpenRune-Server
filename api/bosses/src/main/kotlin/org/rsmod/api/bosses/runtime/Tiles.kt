package org.rsmod.api.bosses.runtime

import org.rsmod.api.bosses.spec.TargetExpr
import org.rsmod.api.random.GameRandom
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

internal val Npc.centreTile: CoordGrid
    get() = coords.translate(size / 2, size / 2)

/**
 * Tiles bound by the effects enclosing the one running: `onImpact` ([impactTile]), `onTiles`
 * ([currentTile]), `withTile` ([tiles]) and `withTiles` ([sets]). Immutable; children add to it.
 */
internal data class TileBindings(
    val impactTile: CoordGrid? = null,
    val currentTile: CoordGrid? = null,
    val tiles: Map<String, CoordGrid> = emptyMap(),
    val sets: Map<String, List<CoordGrid>> = emptyMap(),
) {
    fun set(name: String): List<CoordGrid> =
        sets[name] ?: error("Tile set '$name' resolved outside a withTiles(\"$name\").")

    companion object {
        val NONE = TileBindings()
    }
}

/** Where a condition is evaluated inside an effect: its tile resolution and bound tile sets. */
interface TileScope {
    fun tile(expr: TargetExpr.Single): CoordGrid

    fun set(name: String): List<CoordGrid>
}

/**
 * The one place a [TargetExpr.Single] becomes a tile, shared by effects and conditions. Without
 * [randomWalkable] (conditions have no collision), [TargetExpr.RandomWalkableTile] resolves to
 * its centre; [TargetExpr.RandomOfBound] needs [random].
 */
internal fun Npc.resolveTile(
    expr: TargetExpr.Single,
    target: Player,
    bindings: TileBindings = TileBindings.NONE,
    random: GameRandom? = null,
    randomWalkable: ((center: CoordGrid, radius: Int) -> CoordGrid?)? = null,
): CoordGrid {
    fun resolve(inner: TargetExpr.Single) =
        resolveTile(inner, target, bindings, random, randomWalkable)
    return when (expr) {
        is TargetExpr.CurrentTarget,
        is TargetExpr.CurrentTargetTile,
        is TargetExpr.HighestDamageDealer,
        is TargetExpr.LowestPrayer,
        is TargetExpr.RandomNearby -> target.coords
        is TargetExpr.Self -> coords
        is TargetExpr.Centre -> centreTile
        is TargetExpr.SpawnTile -> spawnCoords.translate(expr.dx, expr.dz)
        is TargetExpr.Offset -> resolve(expr.of).translate(expr.dx, expr.dz)
        is TargetExpr.Custom -> expr.tile(this, target)
        is TargetExpr.Toward -> {
            val from = resolve(expr.from)
            Angles.step(from, Angles.bearing(from, resolve(expr.to)), expr.distance)
        }
        is TargetExpr.RandomWalkableTile -> {
            val center = resolve(expr.of)
            randomWalkable?.invoke(center, expr.radius) ?: center
        }
        is TargetExpr.ImpactTile ->
            checkNotNull(bindings.impactTile) { "ImpactTile resolved outside an onImpact." }
        is TargetExpr.CurrentTile ->
            checkNotNull(bindings.currentTile) { "CurrentTile resolved outside an OnTiles." }
        is TargetExpr.Bound -> {
            val name = expr.name
            bindings.tiles[name] ?: error("tile(\"$name\") resolved outside a withTile(\"$name\").")
        }
        is TargetExpr.RandomOfBound -> {
            val name = expr.name
            val set = bindings.set(name)
            check(set.isNotEmpty()) {
                "randomOf(\"$name\") on an empty tile set; check tilesEmpty(\"$name\") first."
            }
            val random = checkNotNull(random) { "randomOf(\"$name\") resolved without a random." }
            set[random.of(set.size)]
        }
    }
}
