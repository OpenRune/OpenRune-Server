package org.rsmod.api.instance

import org.rsmod.api.attr.AttributeMap
import org.rsmod.game.entity.Player
import org.rsmod.game.region.Region
import org.rsmod.map.CoordGrid

public class Instance
internal constructor(
    public val region: Region,
    public val fallbackPosition: CoordGrid?,
    public val attrs: AttributeMap,
) {
    internal val players: MutableList<Player> = mutableListOf()
    private val teardownCallbacks: MutableList<() -> Unit> = mutableListOf()

    public val playerCount: Int
        get() = players.size

    public fun onTeardown(block: () -> Unit) {
        teardownCallbacks += block
    }

    internal fun invokeTeardown() {
        teardownCallbacks.forEach { it() }
        teardownCallbacks.clear()
    }
}
