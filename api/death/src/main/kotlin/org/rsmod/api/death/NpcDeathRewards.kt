package org.rsmod.api.death

/** Optional encounter reward choice; omitted values preserve ordinary NPC death behaviour. */
public data class NpcDeathRewards(
    public val tableNpc: String? = null,
    public val elapsedTicks: Int? = null,
    public val includeRemains: Boolean = true,
)
