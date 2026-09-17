package org.rsmod.content.areas.city.draynor

import jakarta.inject.Inject
import kotlin.math.abs
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class DraynorManorDoors @Inject constructor(private val locRepo: LocRepository) : PluginScript() {

    override fun ScriptContext.startup() {
        for (leaf in FrontLeaves) {
            onOpLoc1(leaf.closed) { enterFront(it.loc) }
        }
        onOpLoc1(BackDoor) { useBackDoor(it.loc) }
    }

    private suspend fun ProtectedAccess.enterFront(door: BoundLocInfo) {
        if (player.coords.z > FrontDoorZ) {
            mes("The doors won't open.")
            return
        }

        val level = door.coords.level
        val leafX = FrontLeaves.minBy { abs(it.tile.x - player.coords.x) }.tile.x
        val route = listOf(CoordGrid(leafX, FrontDoorZ, level), CoordGrid(leafX, FrontDoorZ + 1, level))
        val openTicks = crossingOpenTicks(crossingTiles(route))

        for (leaf in FrontLeaves) {
            openLeaf(leaf.tile, leaf.inactive, leaf.inactiveAngle, inwardZ = 1, openTicks = openTicks)
        }
        soundSynth(FrontOpenSound)

        crossDoorway(route)

        mes("The doors slam shut behind you.")
        soundSynth(FrontCloseSound)
    }

    private suspend fun ProtectedAccess.useBackDoor(door: BoundLocInfo) {
        val route = crossingRoute(door)
        openLeaf(
            door.coords,
            BackDoorInactive,
            LocAngle.West,
            inwardZ = -1,
            openTicks = crossingOpenTicks(crossingTiles(route)),
        )
        soundSynth(BackOpenSound)
        crossDoorway(route)
    }

    private fun ProtectedAccess.openLeaf(
        tile: CoordGrid,
        inactive: String,
        inactiveAngle: LocAngle,
        inwardZ: Int,
        openTicks: Int,
    ) {
        val closed = locRepo.findExact(tile, LocShape.WallStraight)
        if (closed != null) {
            locRepo.del(closed, openTicks)
        }
        locRepo.add(
            CoordGrid(tile.x, tile.z + inwardZ, tile.level),
            inactive,
            openTicks,
            inactiveAngle,
            LocShape.WallStraight,
        )
    }

    private class Leaf(
        val tile: CoordGrid,
        val closed: String,
        val inactive: String,
        val inactiveAngle: LocAngle,
    )

    private companion object {

        private const val BackDoor = "loc.hauntedbackdoor"
        private const val BackDoorInactive = "loc.hauntedbackdoor_inactive"

        private const val FrontOpenSound = "synth.bigdoor_open"
        private const val FrontCloseSound = "synth.bigdoor_close"
        private const val BackOpenSound = "synth.creakydoor_open"

        private const val FrontDoorZ = 3353

        private val FrontLeaves =
            listOf(
                Leaf(
                    CoordGrid(3108, FrontDoorZ, 0),
                    "loc.haunteddoorl",
                    "loc.haunteddoorl_inactive",
                    LocAngle.West,
                ),
                Leaf(
                    CoordGrid(3109, FrontDoorZ, 0),
                    "loc.haunteddoorr",
                    "loc.haunteddoorr_inactive",
                    LocAngle.East,
                ),
            )
    }
}
