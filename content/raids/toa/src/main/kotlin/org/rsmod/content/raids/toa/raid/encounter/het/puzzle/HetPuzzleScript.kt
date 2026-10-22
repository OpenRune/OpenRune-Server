package org.rsmod.content.raids.toa.raid.encounter.het.puzzle

import kotlin.math.abs
import kotlin.math.sign
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.miningLvl
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onOpLoc4
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.raids.toa.raid.encounter.ToaRooms
import org.rsmod.content.raids.toa.raid.encounter.ToaStage
import org.rsmod.game.entity.Npc
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class HetPuzzleScript : PluginScript() {

    override fun ScriptContext.startup() {
        onOpHeld1(HetObjs.MIRROR) { placeMirror() }
        onOpLoc1(HetLocs.MIRROR) { pickUpMirror(it.loc.coords) }
        onOpLoc2(HetLocs.MIRROR) { rotateMirror(it.loc.coords, delta = 1) }
        onOpLoc3(HetLocs.MIRROR) { rotateMirror(it.loc.coords, delta = -1) }
        onOpLoc4(HetLocs.MIRROR) { pushMirror(it.loc.coords) }
        onOpLoc1(HetLocs.MIRROR_DIRTY) { cleanMirror(it.loc.coords) }

        for (barrier in HetBarrierType.BREAKABLE_LOCS) {
            onOpLoc1(barrier) { breakBarrier(it.loc.coords) }
        }

        onOpNpc1(HetNpcs.SEAL) { mineSeal(it.npc) }
        onOpNpc1(HetNpcs.SEAL_WEAK) { mineSeal(it.npc) }
    }

    private val ProtectedAccess.room: HetPuzzleEncounter?
        get() = ToaRooms.of<HetPuzzleEncounter>(player)

    private fun ProtectedAccess.placeMirror() {
        val room = room ?: return
        val tile = player.coords
        if (!room.insidePuzzle(tile) || !room.board.canHold(tile)) {
            mes(CANNOT_PLACE)
            return
        }
        if (invDel(inv, HetObjs.MIRROR).failure) return
        room.board.place(tile, HetMirror(rotation = 0, loose = true, dirty = false))
        soundSynth(HetSynths.PLACE)
    }

    private fun ProtectedAccess.pickUpMirror(tile: CoordGrid) {
        val room = room ?: return
        if (inv.isFull()) {
            mes(NO_SPACE)
            return
        }
        val mirror = room.board.take(tile) ?: return
        if (invAdd(inv, HetObjs.MIRROR).failure) {
            room.board.restore(tile, mirror)
            return
        }
        anim(HetSeqs.PICKUP)
        soundSynth(HetSynths.PICK_UP)
    }

    private fun ProtectedAccess.rotateMirror(tile: CoordGrid, delta: Int) {
        room?.board?.rotate(tile, delta)
    }

    private fun ProtectedAccess.pushMirror(tile: CoordGrid) {
        val room = room ?: return
        val dx = tile.x - player.coords.x
        val dz = tile.z - player.coords.z
        if (dx == 0 && dz == 0) return
        val dir =
            if (abs(dx) >= abs(dz)) HetDir.of(dx.sign, 0) else HetDir.of(0, dz.sign)
        if (!room.board.push(tile, dir)) mes(CANNOT_PUSH)
    }

    private suspend fun ProtectedAccess.cleanMirror(tile: CoordGrid) {
        val room = room ?: return
        anim(HetSeqs.PICKUP)
        delay(CLEAN_TICKS)
        if (room.board.clean(tile)) mes(CLEANED)
    }

    private suspend fun ProtectedAccess.breakBarrier(tile: CoordGrid) {
        val room = room ?: return
        if (!room.board.isBreakable(tile)) return
        val pickaxe = player.bestPickaxe()
        if (pickaxe == null) {
            mes(NEED_PICKAXE)
            return
        }
        anim(pickaxe.animation)
        delay(breakTicks(pickaxe.tier))
        room.board.breakBarrier(tile)
    }

    private suspend fun ProtectedAccess.mineSeal(npc: Npc) {
        val room = ToaRooms.of<HetPuzzleEncounter>(npc) ?: return
        if (!room.weakened) {
            mes(SEAL_PROTECTED)
            return
        }
        val pickaxe = player.bestPickaxe()
        if (pickaxe == null) {
            mes(NEED_PICKAXE)
            return
        }
        while (room.weakened && room.stage == ToaStage.STARTED) {
            anim(pickaxe.animation)
            val roll = random.of(0, HetPickaxeDamage.RANDOM_SPREAD)
            room.damageSeal(HetPickaxeDamage.minimum(player.miningLvl, pickaxe.tier) + roll)
            statAdvance(MINING, SEAL_XP)
            delay(SWING_TICKS)
        }
    }

    private fun breakTicks(tier: Int): Int =
        when {
            tier >= HetPickaxeDamage.DRAGON_TIER -> BREAK_TICKS_FAST
            tier >= HetPickaxeDamage.RUNE_TIER -> BREAK_TICKS_MEDIUM
            else -> BREAK_TICKS_SLOW
        }

    private companion object {
        const val MINING = "stat.mining"
        const val SEAL_XP = 35.0
        const val SWING_TICKS = 3
        const val CLEAN_TICKS = 2
        const val BREAK_TICKS_FAST = 2
        const val BREAK_TICKS_MEDIUM = 3
        const val BREAK_TICKS_SLOW = 4
        const val SEAL_PROTECTED = "The seal is protected."
        const val NEED_PICKAXE = "You need a pickaxe to do that."
        const val CANNOT_PLACE = "You can't place a mirror here."
        const val CANNOT_PUSH = "The mirror won't move that way."
        const val NO_SPACE = "You don't have enough inventory space."
        const val CLEANED = "You clean the mirror."
    }
}
