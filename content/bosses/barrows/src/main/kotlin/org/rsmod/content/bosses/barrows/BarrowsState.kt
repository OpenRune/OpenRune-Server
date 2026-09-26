package org.rsmod.content.bosses.barrows

import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.content.drops.hasObjInInventoryOrBank
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

internal var Player.hiddenBrotherIndex by intVarBit("varbit.barrows_hidden_brother")
internal var Player.cornerIndex by intVarBit("varbit.barrows_corner")
internal var Player.barrowsLooted by boolVarBit("varbit.barrows_looted")
internal var Player.puzzleSolved by boolVarBit("varbit.barrows_puzzle_solved")
internal var Player.barrowsRolled by boolVarBit("varbit.barrows_rolled")
internal var Player.monsterPotential by intVarBit("varbit.barrows_killed_monster")
internal var Player.chestOpen by boolVarBit("varbit.barrows_chest_open")
internal var Player.ladderVisible by boolVarBit("varbit.barrows_ladder_visible")
internal var Player.hfsStage by intVarBit("varbit.hfs")
internal val Player.cryptMapStudied by boolVarBit("varbit.barrows_map")

internal val Player.needsHfsIcon: Boolean
    get() = hfsStage in HFS_STARTED..HFS_ICON_LOOTED && !hasObjInInventoryOrBank("obj.barrows_icon")

internal const val HFS_NOT_STARTED = 0
internal const val HFS_STARTED = 2
internal const val HFS_ICON_LOOTED = 4

internal var Player.hiddenBrother: BarrowsBrother
    get() = BarrowsBrother.entries[hiddenBrotherIndex.coerceIn(0, 5)]
    set(value) {
        hiddenBrotherIndex = value.ordinal
    }

internal var Player.corner: BarrowsCorner
    get() = BarrowsCorner.entries[cornerIndex.coerceIn(0, 3)]
    set(value) {
        cornerIndex = value.ordinal
    }

internal fun Player.isSlain(brother: BarrowsBrother): Boolean = vars[brother.killedVarbit] == 1

internal fun Player.setSlain(brother: BarrowsBrother, slain: Boolean) {
    VarPlayerIntMapSetter.set(this, brother.killedVarbit, if (slain) 1 else 0)
    VarPlayerIntMapSetter.set(this, "varbit.barrows_killed_count", slainBrothers.size)
}

internal fun Player.refreshLadder() {
    val near = coords.level == 0 && coords.chebyshevDistance(corner.ladder) <= LADDER_DISTANCE
    if (ladderVisible != near) ladderVisible = near
}

private const val LADDER_DISTANCE = 16

internal val Player.slainBrothers: List<BarrowsBrother>
    get() = BarrowsBrother.entries.filter { isSlain(it) }

internal val Player.aliveBrothers: List<BarrowsBrother>
    get() = BarrowsBrother.entries.filterNot { isSlain(it) }

internal var Player.chestCount by intVarp("varp.total_barrows_chests")

internal val Player.fullPotential: Int
    get() = monsterPotential + slainBrothers.size * 2

internal fun Player.addPotential(combatLevel: Int) {
    monsterPotential = (monsterPotential + combatLevel).coerceAtMost(MAX_POTENTIAL)
}

internal fun Player.rollBarrows() {
    hiddenBrother = BarrowsBrother.entries.random()
    corner = BarrowsCorner.entries.random()
    shiftDoorways()
    puzzleSolved = false
    barrowsRolled = true
}

internal fun Player.resetBarrows() {
    for (brother in BarrowsBrother.entries) {
        setSlain(brother, false)
    }
    monsterPotential = 0
    barrowsLooted = false
    chestOpen = false
    rollBarrows()
}

enum class BarrowsCorner(val ladder: CoordGrid) {
    SOUTH_WEST(CoordGrid(3534, 9678, 0)),
    NORTH_WEST(CoordGrid(3534, 9712, 0)),
    NORTH_EAST(CoordGrid(3568, 9712, 0)),
    SOUTH_EAST(CoordGrid(3568, 9678, 0));

    companion object {
        val entries: List<BarrowsCorner> = values().toList()
    }
}

private val SURFACE_X = 3546..3584
private val SURFACE_Z = 3268..3312
private val CRYPT_X = 3524..3580
private val CRYPT_Z = 9667..9724

internal fun Player.inBarrowsCrypt(): Boolean = coords.inBarrowsCrypt()

internal fun CoordGrid.inBarrowsCrypt(): Boolean = x in CRYPT_X && z in CRYPT_Z

internal fun Player.inBarrowsSurface(): Boolean =
    coords.level == 0 && coords.x in SURFACE_X && coords.z in SURFACE_Z

internal const val MAX_POTENTIAL = 1000
internal const val MAX_REWARD_POTENTIAL = 1012
