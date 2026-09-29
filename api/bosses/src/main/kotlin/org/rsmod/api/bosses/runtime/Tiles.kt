package org.rsmod.api.bosses.runtime

import org.rsmod.api.bosses.spec.TargetExpr
import org.rsmod.game.entity.Npc
import org.rsmod.map.CoordGrid

internal val Npc.centreTile: CoordGrid
    get() = coords.translate(size / 2, size / 2)

internal fun resolveToward(expr: TargetExpr.Toward, resolve: (TargetExpr.Single) -> CoordGrid): CoordGrid {
    val from = resolve(expr.from)
    return Angles.step(from, Angles.bearing(from, resolve(expr.to)), expr.distance)
}
