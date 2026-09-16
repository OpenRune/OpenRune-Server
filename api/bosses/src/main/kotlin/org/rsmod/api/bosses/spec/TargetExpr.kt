package org.rsmod.api.bosses.spec

sealed interface TargetExpr {
    sealed interface Single : TargetExpr
    sealed interface Multi : TargetExpr

    data object CurrentTarget : Single
    data object CurrentTargetTile : Single
    data object Self : Single
    data object HighestDamageDealer : Single
    data object LowestPrayer : Single
    data object RandomNearby : Single

    data class RandomWalkableTile(val radius: Int, val of: Single = Self) : Single

    /**
     * The tile a [Effect.Projectile] just landed on — only meaningful inside that same
     * [Effect.Projectile.onImpact]. Resolves to the caster's tile if used anywhere else.
     */
    data object ImpactTile : Single

    data class AllInRadius(val radius: Int, val of: Single = Self) : Multi
    data class TopN(val n: Int, val by: Single) : Multi
}
