package org.rsmod.content.areas.city.portsarim

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.agilityLvl
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.content.generic.locs.doors.DoorTranslations
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class PortSarimLocScript @Inject constructor(private val locRepo: LocRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1("loc.fai_trapdoor") { climbDownTrapdoor() }
        onOpLoc1("loc.vc_manhole_open") { climbDownManhole() }
        onOpLoc1("loc.vc_ladder") { climbUpFromSewer() }
        onOpLoc1("loc.jewellersdoor") { walkThroughDoor(it.loc) }
        onOpLoc1("loc.cavewall_shortcut_wyvern_west") { wyvernTunnel() }
        onOpLoc1("loc.draynor_diary_under_wall_w") { underwallTunnel(UNDERWALL_EAST_EXIT) }
        onOpLoc1("loc.draynor_diary_under_wall_e") { underwallTunnel(UNDERWALL_WEST_EXIT) }
        onOpLoc1("loc.farming_style") { climbStile(it.loc) }
    }

    private suspend fun ProtectedAccess.climbDownTrapdoor() {
        arriveDelay()
        spam("You climb down through the trapdoor.")
        telejump(player.coords.translateZ(UNDERGROUND_OFFSET))
    }

    private suspend fun ProtectedAccess.climbDownManhole() {
        arriveDelay()
        anim("seq.human_pickupfloor")
        delay(1)
        mes("You climb through the manhole")
        telejump(SEWER_LANDING)
    }

    private suspend fun ProtectedAccess.climbUpFromSewer() {
        arriveDelay()
        anim("seq.human_reachforladder")
        delay(1)
        telejump(MANHOLE_EXIT)
    }

    private suspend fun ProtectedAccess.walkThroughDoor(door: BoundLocInfo) {
        arriveDelay()
        val inside = door.coords.translateX(-1)
        val dest = if (coords.x >= door.coords.x) inside else door.coords
        val openCoords = DoorTranslations.translateOpen(door.coords, door.shape, door.angle)
        locRepo.del(door, DOOR_OPEN_TICKS)
        locRepo.add(openCoords, OPEN_DOOR, DOOR_OPEN_TICKS, door.turnAngle(rotations = 1), door.shape)
        soundSynth("synth.door_open")
        playerMove(dest)
    }

    private suspend fun ProtectedAccess.wyvernTunnel() {
        arriveDelay()
        if (player.agilityLvl < WYVERN_TUNNEL_LEVEL) {
            objbox(
                "obj.agility_contortion",
                zoom = 400,
                "The tunnel is very dark and dangerous. You'll need an Agility level of " +
                    "$WYVERN_TUNNEL_LEVEL to try this.",
            )
            return
        }
        mes("Nothing interesting happens.")
    }

    private suspend fun ProtectedAccess.underwallTunnel(exit: CoordGrid) {
        if (player.agilityLvl < UNDERWALL_TUNNEL_LEVEL) {
            mes("You need an Agility level of $UNDERWALL_TUNNEL_LEVEL to negotiate this tunnel.")
            return
        }
        anim("seq.human_crawling")
        soundSynth("synth.underwall_tunnel_crawl")
        delay(1)
        teleport(exit)
        resetAnim()
    }

    private suspend fun ProtectedAccess.climbStile(stile: BoundLocInfo) {
        val alongZ = stile.angle == LocAngle.West || stile.angle == LocAngle.East
        val near = stile.coords
        val far = if (alongZ) near.translateZ(1) else near.translateX(1)
        val fromNear = if (alongZ) coords.z <= near.z else coords.x <= near.x
        val (start, end) = if (fromNear) near to far else far to near
        if (coords != start) {
            playerMove(start)
        }
        val facing =
            when {
                alongZ && fromNear -> FACE_NORTH
                alongZ -> FACE_SOUTH
                fromNear -> FACE_EAST
                else -> FACE_WEST
            }
        anim("seq.human_walk_style", delay = STILE_START_CYCLES)
        exactMove(start, end, STILE_START_CYCLES, STILE_END_CYCLES, facing)
        delay(STILE_TICKS)
    }

    private companion object {
        const val UNDERGROUND_OFFSET = 6400
        const val OPEN_DOOR = "loc.inactiveposhdoor"
        const val DOOR_OPEN_TICKS = 3
        const val WYVERN_TUNNEL_LEVEL = 82
        const val UNDERWALL_TUNNEL_LEVEL = 42
        const val STILE_START_CYCLES = 30
        const val STILE_END_CYCLES = 94
        const val STILE_TICKS = 3
        const val FACE_SOUTH = 0
        const val FACE_WEST = 512
        const val FACE_NORTH = 1024
        const val FACE_EAST = 1536

        val SEWER_LANDING = CoordGrid(2962, 9650, 0)
        val MANHOLE_EXIT = CoordGrid(3018, 3233, 0)
        val UNDERWALL_EAST_EXIT = CoordGrid(3070, 3257, 0)
        val UNDERWALL_WEST_EXIT = CoordGrid(3066, 3257, 0)
    }
}
