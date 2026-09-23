package org.rsmod.content.areas.misc.miningguild.scripts

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.content.areas.misc.miningguild.MiningGuild.denyEntry
import org.rsmod.content.areas.misc.miningguild.MiningGuild.meetsEntryLevel
import org.rsmod.content.generic.locs.doors.DoorTranslations
import org.rsmod.content.interfaces.bank.tryOpenBank
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class MiningGuildLocScript @Inject constructor(private val locRepo: LocRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1("loc.mguild_door") { passDoor(it.loc, guardedFromNorth = true) }
        onOpLoc1("loc.mguild_door_inner1") { passDoor(it.loc, guardedFromNorth = false) }
        onOpLoc1("loc.mguild_door_inner2") { passDoor(it.loc, guardedFromNorth = false) }

        onOpLoc1("loc.mguild_ladder") { climbDown() }

        onOpLoc1("loc.castlewars_bankchest") { tryOpenBank() }
    }

    /**
     * Guild doors never open for long: the door is lifted for two cycles, an open door is shown
     * beside it, and the player takes a single step through.
     */
    private suspend fun ProtectedAccess.passDoor(door: BoundLocInfo, guardedFromNorth: Boolean) {
        val onNorthSide = coords.z > door.coords.z
        if (guardedFromNorth && onNorthSide && !meetsEntryLevel()) {
            denyEntry(DOOR_DWARF)
            return
        }
        val dest = if (onNorthSide) door.coords else door.coords.translateZ(1)
        val openCoords = DoorTranslations.translateOpen(door.coords, door.shape, door.angle)

        locRepo.del(door, DOOR_OPEN_CYCLES)
        locRepo.add(openCoords, OPEN_DOOR, DOOR_OPEN_CYCLES, door.turnAngle(rotations = 1), door.shape)
        soundSynth(DOOR_SOUND)
        playerWalk(dest)
    }

    private suspend fun ProtectedAccess.climbDown() {
        arriveDelay()
        if (!meetsEntryLevel()) {
            denyEntry(LADDER_DWARF)
            return
        }
        anim("seq.human_reachforladder")
        delay(1)
        telejump(coords.translateZ(UNDERGROUND_OFFSET))
    }

    private companion object {
        const val OPEN_DOOR = "loc.inactivepoordoor"
        const val DOOR_OPEN_CYCLES = 2
        const val DOOR_SOUND = 81
        const val UNDERGROUND_OFFSET = 6400
        const val DOOR_DWARF = "npc.mguild_dwarf1"
        const val LADDER_DWARF = "npc.mguild_dwarf2"
    }
}
