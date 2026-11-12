package org.rsmod.content.raids.toa.raid.encounter.scabaras.puzzle

import org.rsmod.api.config.Constants
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onApLoc1
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc5
import org.rsmod.content.raids.toa.raid.encounter.ToaRooms
import org.rsmod.content.raids.toa.raid.encounter.npcType
import org.rsmod.content.raids.toa.raid.encounter.onRoomNpcHit
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ScabarasPuzzleScript : PluginScript() {

    override fun ScriptContext.startup() {
        val obelisk = npcType(ScabarasNpcs.OBELISK)
        obelisk.actions = obelisk.actions.toBuilder().op(1, "Attack").build()
        onRoomNpcHit<ScabarasPuzzleEncounter>(ScabarasNpcs.OBELISK) { npc, hit ->
            pillars.hit(npc, hit)
        }

        onOpLoc1(ScabarasLocs.MEMORY_BUTTON) { room?.memory?.press(player) }
        onOpLoc1(ScabarasLocs.SUM_TABLET) { room?.sum?.read(player) }
        onOpLoc5(ScabarasLocs.LIGHT_OFF) { flip(it.loc) }
        for (hidden in ScabarasLocs.MATCH_HIDDEN) {
            onOpLoc1(hidden) { match(it.loc) }
        }
        onOpLoc1(ScabarasLocs.HOLE) { crawl(it.loc) }
        onApLoc1(ScabarasLocs.STEPPING_STONE) { jumpStone(it.loc) }
    }

    private val ProtectedAccess.room: ScabarasPuzzleEncounter?
        get() = ToaRooms.of<ScabarasPuzzleEncounter>(player)

    private fun ProtectedAccess.flip(loc: BoundLocInfo) {
        val room = room ?: return
        room.lights.flip(player, room.staticCoords(loc.coords))
    }

    private fun ProtectedAccess.match(loc: BoundLocInfo) {
        val room = room ?: return
        room.match.click(player, room.staticCoords(loc.coords))
    }

    private suspend fun ProtectedAccess.crawl(loc: BoundLocInfo) {
        val room = room ?: return
        if (!room.playing()) return
        val lower = room.staticCoords(loc.coords).z == ScabarasCoords.HOLE_LOWER.z
        val dest = if (lower) ScabarasCoords.HOLE_LOWER_EXIT else ScabarasCoords.HOLE_UPPER_EXIT
        delay(CRAWL_START)
        anim(ScabarasSeqs.CRAWL)
        soundSynth(ScabarasSynths.CRAWL, loops = 2, delay = 4)
        delay(CRAWL_TICKS)
        telejump(room.coords(dest))
        resetAnim()
    }

    private suspend fun ProtectedAccess.jumpStone(stone: BoundLocInfo) {
        if (!isWithinApRange(stone, STONE_AP_RANGE)) return
        val room = room ?: return
        if (!room.playing()) return
        val south = room.staticCoords(player.coords).z < ScabarasCoords.STONE.z
        val near = if (south) ScabarasCoords.STONE_SOUTH_SIDE else ScabarasCoords.STONE_NORTH_SIDE
        val far = if (south) ScabarasCoords.STONE_NORTH_SIDE else ScabarasCoords.STONE_SOUTH_SIDE
        val facing = if (south) Constants.em_face_north else Constants.em_face_south
        val start = room.coords(near)
        if (coords != start) {
            playerWalk(start)
            if (coords != start) return
        }
        hop(room.coords(ScabarasCoords.STONE), facing, FIRST_HOP)
        hop(room.coords(far), facing, SECOND_HOP)
    }

    private suspend fun ProtectedAccess.hop(dest: CoordGrid, facing: Int, hop: Hop) {
        anim(ScabarasSeqs.JUMP, hop.animDelay)
        soundSynth(ScabarasSynths.JUMP, delay = hop.soundDelay)
        exactMove(coords, dest, hop.moveStart, hop.moveEnd, facing, TeleportType.Exempt)
        delay(HOP_TICKS)
    }

    private class Hop(val animDelay: Int, val soundDelay: Int, val moveStart: Int, val moveEnd: Int)

    private companion object {
        const val CRAWL_START = 3
        const val CRAWL_TICKS = 2
        const val HOP_TICKS = 2
        const val STONE_AP_RANGE = 3
        val FIRST_HOP = Hop(animDelay = 15, soundDelay = 20, moveStart = 30, moveEnd = 45)
        val SECOND_HOP = Hop(animDelay = 0, soundDelay = 5, moveStart = 15, moveEnd = 28)
    }
}
