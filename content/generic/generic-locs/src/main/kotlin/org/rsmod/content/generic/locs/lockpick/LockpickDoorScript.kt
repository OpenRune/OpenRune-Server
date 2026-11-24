package org.rsmod.content.generic.locs.lockpick

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onOpLoc4
import org.rsmod.api.script.onOpLoc5
import org.rsmod.api.table.LockpickDoorsRow
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class LockpickDoorScript : PluginScript() {
    override fun ScriptContext.startup() {
        for (door in LockpickDoorsRow.all()) {
            val loc = RSCM.getReverseMapping(RSCMType.LOC, door.loc.id)
            for (op in 1..5) {
                when (door.loc.actions.getOpOrNull(op - 1)?.lowercase()) {
                    "open" -> bind(op, loc) { mes("This door is locked.") }
                    "pick-lock" -> bind(op, loc) { pickLock(door, it) }
                }
            }
        }
    }

    private fun ScriptContext.bind(
        op: Int,
        loc: String,
        action: suspend ProtectedAccess.(BoundLocInfo) -> Unit,
    ) {
        when (op) {
            1 -> onOpLoc1(loc) { action(it.loc) }
            2 -> onOpLoc2(loc) { action(it.loc) }
            3 -> onOpLoc3(loc) { action(it.loc) }
            4 -> onOpLoc4(loc) { action(it.loc) }
            else -> onOpLoc5(loc) { action(it.loc) }
        }
    }

    private suspend fun ProtectedAccess.pickLock(door: LockpickDoorsRow, loc: BoundLocInfo) {
        if (statBase(THIEVING) < door.level) {
            mes("You need a Thieving level of ${door.level} to pick this lock.")
            return
        }
        if (door.needsLockpick && LOCKPICK !in inv) {
            mes("You need a lockpick for this lock.")
            return
        }
        arriveDelay()
        mes("You attempt to pick the lock.")
        anim(PICK_ANIM)
        delay(PICK_TICKS)
        // UNVERIFIED: the wiki gives no lockpicking odds; these mirror low-level pickpocketing.
        if (!statRandom(THIEVING, SUCCESS_LOW, SUCCESS_HIGH, invisibleBoost = 0)) {
            mes("You fail to pick the lock.")
            return
        }
        mes("You manage to pick the lock.")
        statAdvance(THIEVING, door.xpTenths / 10.0)
        if (door.exit != WALK_THROUGH) {
            telejump(door.exit)
            return
        }
        teleport(WallCrossing.across(coords, loc.coords, loc.angleId))
    }

    private companion object {
        const val THIEVING = "stat.thieving"
        const val LOCKPICK = "obj.lockpick"
        const val PICK_ANIM = "seq.human_picklock_chest"
        const val PICK_TICKS = 3
        const val SUCCESS_LOW = 80
        const val SUCCESS_HIGH = 256
        val WALK_THROUGH = CoordGrid(0, 0, 0)
    }
}
