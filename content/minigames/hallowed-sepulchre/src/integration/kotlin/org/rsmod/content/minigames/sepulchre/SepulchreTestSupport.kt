package org.rsmod.content.minigames.sepulchre

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

internal fun GameTestScope.place(
    loc: String,
    coords: CoordGrid,
    angle: Int,
    shape: Int = 10,
): BoundLocInfo {
    placeMapLoc(coords, loc, LocShape[shape], LocAngle[angle])
    val type = checkNotNull(ServerCacheManager.getObject(loc.asRSCM(RSCMType.LOC)))
    val info = findLocs(coords).first { it.coords == coords && it.id == type.id }
    return BoundLocInfo(info, type)
}

internal fun GameTestScope.startAt(coords: CoordGrid) {
    player.teleport(coords)
    advance()
}

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

internal fun GameTestScope.setVar(name: String, value: Int) {
    VarPlayerIntMapSetter.set(player, name, value)
}

internal fun GameTestScope.setLevel(stat: String, level: Int) {
    player.statMap.setBaseLevel(stat, level.toByte())
    player.statMap.setCurrentLevel(stat, level.toByte())
}

internal fun GameTestScope.xp(stat: String): Int = player.statMap.getFineXP(stat)

/** Answers the active dialogue: menus with [options] in order, everything else with continue. */
internal fun GameTestScope.answer(vararg options: Int) {
    val choices = options.iterator()
    repeat(MAX_DIALOGUE_STEPS) {
        val coroutine = player.activeCoroutine
        if (coroutine == null || !coroutine.isSuspended) {
            return
        }
        if (!coroutine.isAwaiting(ResumePauseButtonInput::class)) {
            advance()
            return@repeat
        }
        val input =
            if (player.ui.containsModal("interface.chatmenu")) {
                if (!choices.hasNext()) {
                    return
                }
                ResumePauseButtonInput("component.chatmenu:options", choices.next())
            } else {
                val parent = CHAT_PARENTS.first { player.ui.containsModal("interface.${it.first}") }
                ResumePauseButtonInput("component.${parent.first}:${parent.second}", -1)
            }
        coroutine.resumeWith(input)
    }
}

/** Enters [floor] as live leaves it the tick its overlay opens, with [remaining] ticks left. */
internal fun GameTestScope.onFloor(floor: Int, at: CoordGrid, remaining: Int) {
    startAt(at)
    setVar(FLOOR, floor)
    setVar(REMAINING, remaining)
}

private const val MAX_DIALOGUE_STEPS = 80
private val CHAT_PARENTS =
    listOf(
        "chat_left" to "continue",
        "chat_right" to "continue",
        "messagebox" to "continue",
        "objectbox" to "universe",
    )

internal const val AGILITY_STAT = "stat.agility"
internal const val TUTORIAL = "varbit.hallowed_tutorial"
internal const val ENTRY_CHECK = "varbit.hallowed_entry_check"
internal const val REMAINING = "varbit.hallowed_time_remaining"
internal const val FLOOR = "varbit.hallowed_current_floor"
internal const val FLOOR_TIME = "varbit.hallowed_current_floor_time_spent"
internal const val PAUSED = "varbit.hallowed_block_climbover"
