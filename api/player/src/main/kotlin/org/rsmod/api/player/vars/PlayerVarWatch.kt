package org.rsmod.api.player.vars

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.varp.baseVar
import dev.openrune.types.varp.bits
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import org.rsmod.events.EventBus
import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.PlayerEvent
import org.rsmod.utils.bits.getBits

/**
 * [varp] changed from [previous] to [value] after the player's first processed tick. Loading an
 * account and switching world type write vars directly and publish nothing. For a varbit subscriber
 * [previous] and [value] are the varbit's own bits.
 */
public data class PlayerVarChangedEvent(
    override val player: Player,
    public val varp: Int,
    public val previous: Int,
    public val value: Int,
) : UnboundEvent, PlayerEvent

/**
 * Var writes are a hot path, so events are only published for varps something asked to watch. An
 * unwatched varp costs one set lookup and allocates nothing. There is deliberately no way to watch
 * every var. Varbits are tracked by their base varp so a write through an overlapping varbit or the
 * varp itself is still seen.
 */
public object PlayerVarWatch {
    @Volatile public var eventBus: EventBus? = null

    private val watched = IntOpenHashSet()

    public fun watch(name: String): VarWatch {
        if (name.startsWith("varp.")) {
            val varp = checkNotNull(ServerCacheManager.getVarp(name.asRSCM(RSCMType.VARP))) { name }
            return watch(varp.id)
        }
        val varbit = checkNotNull(ServerCacheManager.getVarbit(name.asRSCM(RSCMType.VARBIT))) { name }
        return watch(varbit.baseVar.id, varbit.bits)
    }

    public fun watch(varp: Int, bits: IntRange? = null): VarWatch {
        watched.add(varp)
        return VarWatch(varp, bits)
    }

    internal fun changed(player: Player, varp: Int, previous: Int, value: Int) {
        if (!watched.contains(varp)) {
            return
        }
        eventBus?.publish(PlayerVarChangedEvent(player, varp, previous, value))
    }
}

public class VarWatch internal constructor(private val varp: Int, private val bits: IntRange?) {
    /** The event as this watch sees it, or null if it is about something else. */
    public fun matching(event: PlayerVarChangedEvent): PlayerVarChangedEvent? {
        if (event.varp != varp) {
            return null
        }
        val range = bits ?: return event
        val previous = event.previous.getBits(range)
        val value = event.value.getBits(range)
        return if (previous == value) null else event.copy(previous = previous, value = value)
    }
}
