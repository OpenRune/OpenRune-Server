package org.rsmod.content.skills.magic.spell.attacks.ancient

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.random.GameRandom
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.map.CoordGrid

internal object TestCache {
    private val loaded by lazy { ServerCacheManager.init(240).close() }

    fun load() = loaded
}

internal class FixedRandom(private val value: Int) : GameRandom {
    override fun of(maxExclusive: Int): Int = value.coerceIn(0, maxExclusive - 1)

    override fun of(minInclusive: Int, maxInclusive: Int): Int =
        value.coerceIn(minInclusive, maxInclusive)

    override fun randomDouble(): Double = 0.0
}

internal val TestCoords = CoordGrid(0, 50, 50, 10, 10)

internal fun npc(internal: String, clock: Int = 100): Npc {
    val type = checkNotNull(ServerCacheManager.getNpc(internal.asRSCM(RSCMType.NPC)))
    return Npc(type, TestCoords).apply { currentMapClock = clock }
}

internal fun player(clock: Int = 100): Player =
    Player().apply {
        coords = TestCoords
        currentMapClock = clock
        inv = Inventory.create("inv.inv")
        worn = Inventory.create("inv.worn")
    }

internal fun Player.setLevel(stat: String, base: Int, current: Int = base) {
    statMap.setBaseLevel(stat, base.toByte())
    statMap.setCurrentLevel(stat, current.toByte())
}
