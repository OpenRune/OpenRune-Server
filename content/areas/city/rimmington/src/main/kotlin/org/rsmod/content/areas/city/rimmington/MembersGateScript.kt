package org.rsmod.content.areas.city.rimmington

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid
import org.rsmod.map.util.Translation
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The metal members gates, such as the one south of Falador on the Rimmington road. Opening swaps
 * both halves for the open metal gate one tile towards the side they face; closing puts plain
 * closed metal gates back in their place.
 */
class MembersGateScript @Inject constructor(private val locRepo: LocRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        for (half in Half.entries) {
            for (closed in half.closed) {
                onOpLoc1(closed) { open(it.loc, half) }
            }
            onOpLoc1(half.open) { close(it.loc, half) }
        }
    }

    private fun ProtectedAccess.open(clicked: BoundLocInfo, half: Half) {
        val closedAngle = clicked.angle
        val rightCoords = if (half == Half.Right) clicked.coords else clicked.coords - pair(closedAngle)
        val leftCoords = rightCoords + pair(closedAngle)
        val swing = swing(closedAngle)

        for (coords in listOf(rightCoords, leftCoords)) {
            clearGate(coords, clicked.shape)
        }
        locRepo.add(rightCoords + swing, Half.Right.open, DURATION, closedAngle.turn(1), clicked.shape)
        locRepo.add(leftCoords + swing, Half.Left.open, DURATION, closedAngle.turn(3), clicked.shape)
        soundSynth("synth.iron_door_open")
    }

    private fun ProtectedAccess.close(clicked: BoundLocInfo, half: Half) {
        val closedAngle = clicked.angle.turn(if (half == Half.Right) 3 else 1)
        val openRight = if (half == Half.Right) clicked.coords else clicked.coords - pair(closedAngle)
        val openLeft = openRight + pair(closedAngle)
        val swing = swing(closedAngle)

        for (coords in listOf(openRight, openLeft)) {
            clearGate(coords, clicked.shape)
        }
        locRepo.add(openRight - swing, Half.Right.closed.last(), DURATION, closedAngle, clicked.shape)
        locRepo.add(openLeft - swing, Half.Left.closed.last(), DURATION, closedAngle, clicked.shape)
        soundSynth("synth.grate_close")
    }

    /**
     * Deleting a gate that was added over the map's own gate brings the map's gate back, so keep
     * deleting until the wall slot is empty.
     */
    private fun clearGate(coords: CoordGrid, shape: LocShape) {
        repeat(MAX_GATE_LAYERS) {
            val gate = locRepo.findExact(coords, shape) ?: return
            locRepo.del(gate, DURATION)
        }
    }

    private fun pair(closedAngle: LocAngle): Translation =
        when (closedAngle) {
            LocAngle.West -> Translation(z = -1)
            LocAngle.North -> Translation(x = -1)
            LocAngle.East -> Translation(z = 1)
            LocAngle.South -> Translation(x = 1)
        }

    private fun swing(closedAngle: LocAngle): Translation =
        when (closedAngle) {
            LocAngle.West -> Translation(x = -1)
            LocAngle.North -> Translation(z = 1)
            LocAngle.East -> Translation(x = 1)
            LocAngle.South -> Translation(z = -1)
        }

    private enum class Half(val closed: List<String>, val open: String) {
        Left(listOf("loc.membergatel", "loc.metalgateclosedl"), "loc.metalgateopenl"),
        Right(listOf("loc.membergater", "loc.metalgateclosedr"), "loc.metalgateopenr"),
    }

    private companion object {
        const val DURATION = 500
        const val MAX_GATE_LAYERS = 3
    }
}
