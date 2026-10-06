package org.rsmod.content.quest.area.alkharid.princealirescue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.CellDoor
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.Key
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.StageAliEscaped
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.StageKeliTied
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The gate of the Prince's cell in the Draynor jail. Only the copied key opens it from outside,
 * and only once Keli is out of the way; anyone inside can always let themselves out. The wall
 * pieces are swapped for an open gate while the player steps across, which the client draws as an
 * ordinary walk.
 */
class JailCellDoor
@Inject
constructor(private val princeAli: PrinceAliRescueQuest, private val locRepo: LocRepository) :
    PluginScript() {

    override fun ScriptContext.startup() {
        onOpLoc1(CellDoor) { open(it.loc) }
        onOpLocU(CellDoor, Key) { unlock(it.loc) }
    }

    private suspend fun ProtectedAccess.open(door: BoundLocInfo) {
        arriveDelay()
        if (!insideCell(door)) {
            mes("The gate is locked.")
            return
        }
        passThrough(door)
    }

    private suspend fun ProtectedAccess.unlock(door: BoundLocInfo) {
        arriveDelay()
        if (insideCell(door)) {
            passThrough(door)
            return
        }
        val stage = princeAli.stage(player)
        if (stage < StageKeliTied) {
            mesbox("You'll need to deal with Lady Keli before freeing the Prince.")
            return
        }
        if (stage >= StageAliEscaped) {
            mes("The gate is locked.")
            return
        }
        soundSynth("synth.unlock")
        passThrough(door)
    }

    private suspend fun ProtectedAccess.passThrough(door: BoundLocInfo) {
        val inside = insideCell(door)
        soundSynth("synth.iron_door_open")
        locRepo.del(door, OpenTicks)
        locRepo.add(
            OpenDoorCoords,
            "loc.inactiveprisondoor",
            OpenTicks,
            LocAngle.East,
            LocShape.WallStraight,
        )
        val destZ = if (inside) door.coords.z + 1 else door.coords.z
        playerWalkWithMinDelay(CoordGrid(door.coords.x, destZ, door.coords.level))
    }

    private fun ProtectedAccess.insideCell(door: BoundLocInfo): Boolean = coords.z <= door.coords.z

    private companion object {
        const val OpenTicks = 5
        val OpenDoorCoords = CoordGrid(3123, 3244, 0)
    }
}
