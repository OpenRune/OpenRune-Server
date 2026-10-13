package org.rsmod.content.raids.toa.raid.encounter.scabaras.puzzle

import org.rsmod.api.player.protect.ProtectedAccess
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
        onOpLoc1(ScabarasLocs.STEPPING_STONE) { jumpStone() }
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

    private suspend fun ProtectedAccess.jumpStone() {
        val room = room ?: return
        if (!room.playing()) return
        val south = room.staticCoords(player.coords).z < ScabarasCoords.STONE.z
        val far = if (south) ScabarasCoords.STONE_NORTH_SIDE else ScabarasCoords.STONE_SOUTH_SIDE
        hop(room.coords(ScabarasCoords.STONE))
        hop(room.coords(far))
    }

    private suspend fun ProtectedAccess.hop(dest: CoordGrid) {
        anim(ScabarasSeqs.JUMP)
        soundSynth(ScabarasSynths.JUMP)
        telejump(dest)
        delay(HOP_TICKS)
    }

    private companion object {
        const val CRAWL_START = 3
        const val CRAWL_TICKS = 2
        const val HOP_TICKS = 2
    }
}
