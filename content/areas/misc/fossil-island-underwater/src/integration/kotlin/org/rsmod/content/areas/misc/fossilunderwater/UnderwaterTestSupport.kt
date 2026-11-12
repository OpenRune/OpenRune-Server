package org.rsmod.content.areas.misc.fossilunderwater

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import net.rsprot.protocol.game.outgoing.misc.player.MessageGame
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

internal fun GameTestScope.setVar(name: String, value: Int) {
    VarPlayerIntMapSetter.set(player, name, value)
}

internal fun GameTestScope.setLevel(stat: String, level: Int) {
    player.statMap.setBaseLevel(stat, level.toByte())
    player.statMap.setCurrentLevel(stat, level.toByte())
}

internal fun GameTestScope.xp(stat: String): Int = player.statMap.getFineXP(stat)

internal fun GameTestScope.gameMessages(): List<String> =
    client.filterIsInstance<MessageGame>().map { it.message }

/** `advance` clears the capture client every tick, so this keeps each tick's game messages. */
internal fun GameTestScope.advanceCollecting(ticks: Int): List<String> {
    val all = mutableListOf<String>()
    repeat(ticks) {
        advance()
        all += gameMessages()
    }
    return all
}

/** Continues the open chat box, if any. */
internal fun GameTestScope.continueChat() {
    val coroutine = player.activeCoroutine ?: return
    if (!coroutine.isSuspended || !coroutine.isAwaiting(ResumePauseButtonInput::class)) {
        return
    }
    coroutine.resumeWith(ResumePauseButtonInput("component.chat_left:continue", -1))
}

/** Places [loc] as the map has it; the test world loads no locs on the sea floor. */
internal fun GameTestScope.place(loc: String, coords: CoordGrid, angle: Int): BoundLocInfo {
    placeMapLoc(coords, loc, LocShape.CentrepieceStraight, LocAngle[angle])
    return boundLoc(coords, loc)
}

internal fun GameTestScope.boundLoc(coords: CoordGrid, loc: String): BoundLocInfo {
    val type = checkNotNull(ServerCacheManager.getObject(loc.asRSCM(RSCMType.LOC)))
    val info = findLocs(coords).first { it.coords == coords && it.id == type.id }
    return BoundLocInfo(info, type)
}

/** Waits for the chest the tests place on the sea floor to open, and returns it. */
internal fun GameTestScope.openChest(): BoundLocInfo {
    val open = CHEST_OPEN.asRSCM(RSCMType.LOC)
    advanceUntil(
        predicate = { findLocs(CHEST).any { it.coords == CHEST && it.id == open } },
        timeoutTicks = WINDOW_TICKS * 2,
    )
    return boundLoc(CHEST, CHEST_OPEN)
}

internal const val AGILITY = "stat.agility"
internal const val THIEVING = "stat.thieving"
internal const val OXYGEN = "varbit.fossil_underwater_oxygen"
internal const val FOUND = "varbit.fossil_underwater_found"
internal const val FOUND_WINDOW = "varbit.fossil_underwater_found_window"
internal const val SEARCHED_WINDOW = "varbit.fossil_underwater_window"
internal const val MERMAID_TEAR = "obj.fossil_mermaid_tear"
internal const val GLISTENING_TEAR = "obj.fossil_glistening_tear"
internal const val CHEST_CLOSED = "loc.fossil_chest_closed"
internal const val CHEST_OPEN = "loc.fossil_chest_open"

/** A chest as the map places it, and the tile south of it it is searched from. */
internal val CHEST = CoordGrid(3732, 10248, 1)
internal val CHEST_SEARCH_TILE = CoordGrid(3732, 10247, 1)

private const val WINDOW_TICKS = 103
