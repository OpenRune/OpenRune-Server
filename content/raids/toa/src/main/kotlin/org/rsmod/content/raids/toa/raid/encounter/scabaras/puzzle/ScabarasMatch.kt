package org.rsmod.content.raids.toa.raid.encounter.scabaras.puzzle

import org.rsmod.api.player.output.soundSynth
import org.rsmod.content.raids.toa.raid.shuffled
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

internal class ScabarasMatch(private val room: ScabarasPuzzleEncounter) {
    private val locs = LocSet(room)
    private val deps = room.raid.deps
    private val grids = listOf(ScabarasCoords.FINAL_SOUTH, ScabarasCoords.FINAL_NORTH)
    private val symbols = Array(grids.size) { IntArray(PLATES) }
    private val matched = BooleanArray(PLATES + 1)
    private val revealed = IntArray(grids.size) { NONE }
    private var comparing = false

    fun build() {
        matched.fill(false)
        revealed.fill(NONE)
        comparing = false
        if (room.raid.players.size <= 1) {
            for (symbol in ScabarasCoords.SOLO_SOLVED_SYMBOLS) matched[symbol] = true
        }
        for (grid in grids.indices) {
            val order = deps.random.shuffled((1..PLATES).toList())
            for (index in 0 until PLATES) {
                symbols[grid][index] = order[index]
                show(grid, index)
            }
        }
    }

    fun clear() {
        locs.clear()
    }

    fun click(player: Player, static: CoordGrid) {
        if (!room.playing() || comparing) return
        val grid = grids.indexOfFirst { gridIndex(it, static) >= 0 }
        if (grid < 0) return
        val index = gridIndex(grids[grid], static)
        if (symbols[grid][index].let { matched[it] } || revealed[grid] == index) return
        val previous = revealed[grid]
        revealed[grid] = index
        if (previous != NONE) show(grid, previous)
        show(grid, index)
        if (revealed.any { it == NONE }) return
        comparing = true
        room.schedule(1) { compare(player) }
    }

    private fun compare(player: Player) {
        comparing = false
        val first = symbols[0][revealed[0]]
        val second = symbols[1][revealed[1]]
        val equal = first == second
        val indices = revealed.copyOf()
        revealed.fill(NONE)
        if (equal) {
            matched[first] = true
            player.soundSynth(ScabarasSynths.STEP)
        } else {
            player.soundSynth(ScabarasSynths.MISMATCH, delay = MISMATCH_DELAY)
        }
        for (grid in grids.indices) show(grid, indices[grid])
        if ((1..PLATES).all { matched[it] }) room.completeFinal()
    }

    private fun show(grid: Int, index: Int) {
        val tile = plateTile(grid, index)
        val symbol = symbols[grid][index]
        val open = matched[symbol] || revealed[grid] == index
        locs.set(
            tile,
            if (open) ScabarasLocs.matchShown(symbol) else ScabarasLocs.matchHidden(symbol),
        )
        if (matched[symbol]) {
            locs.set(tile, ScabarasLocs.glow(MATCH_GLOW[symbol]), LocShape.CentrepieceStraight)
        } else {
            locs.remove(tile, LocShape.CentrepieceStraight)
        }
    }

    private fun plateTile(grid: Int, index: Int): CoordGrid {
        val offset = ScabarasCoords.FINAL_PLATES[index]
        return grids[grid].translate(offset.first, offset.second)
    }

    private fun gridIndex(base: CoordGrid, static: CoordGrid): Int =
        ScabarasCoords.FINAL_PLATES.indexOfFirst {
            base.translate(it.first, it.second) == static
        }

    private companion object {
        const val PLATES = 9
        const val NONE = -1
        const val MISMATCH_DELAY = 4
    }
}
