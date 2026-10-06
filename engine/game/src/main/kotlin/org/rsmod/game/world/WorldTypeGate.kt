package org.rsmod.game.world

/** Decides whether a world-type scoped plugin script applies to a given character. */
public class WorldTypeGate(public val worldTypes: Collection<WorldType>) {
    private val allowed: Set<WorldType> = worldTypes.toSet()

    public val unrestricted: Boolean = allowed.isEmpty()

    public fun allows(worldType: WorldType): Boolean = unrestricted || worldType in allowed

    public companion object {
        /** `true` when [worldTypes] is empty, or this world serves at least one of them. */
        public fun servedBy(
            served: Collection<WorldType>,
            worldTypes: Collection<WorldType>,
        ): Boolean = worldTypes.isEmpty() || worldTypes.any(served::contains)
    }
}
