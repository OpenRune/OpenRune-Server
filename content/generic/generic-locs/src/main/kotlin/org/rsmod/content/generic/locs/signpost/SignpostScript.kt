package org.rsmod.content.generic.locs.signpost

import org.rsmod.api.enums.NamedEnums.signpost_directions
import org.rsmod.api.player.output.Camera.camReset
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onApLoc1
import org.rsmod.api.script.onIfClose
import org.rsmod.api.script.onOpLoc1
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.map.Direction
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class SignpostScript : PluginScript() {

    override fun ScriptContext.startup() {
        onApLoc1("loc.aide_signpost_1") { apReadSignpost(it.loc) }
        onOpLoc1("loc.aide_signpost_1") { readSignpost(it.loc) }
        onIfClose("interface.aide_compass") { player.exitSignpost() }
    }

    private fun ProtectedAccess.apReadSignpost(loc: BoundLocInfo) {
        if (isWithinApRange(loc, distance = 5)) {
            readSignpost(loc)
        }
    }

    private fun ProtectedAccess.readSignpost(loc: BoundLocInfo) {
        camForceAngle(rate = 280, rate2 = 0)

        val camMoveTo = coords.translateZ(-6)
        camMoveTo(camMoveTo, height = 1500, rate = 2, rate2 = 10)

        val camLookAt = coords.translateZ(3)
        camLookAt(camLookAt, height = 450, rate = 2, rate2 = 10)

        val directions = signpost_directions.getValue(loc.coords).split("|")
        val (west, south, north, east) = directions
        ifSetText("component.aide_compass:aide_west_text_2", west)
        ifSetText("component.aide_compass:aide_south_text_2", south)
        ifSetText("component.aide_compass:aide_north_text_2", north)
        ifSetText("component.aide_compass:aide_east_text_2", east)
        ifOpenMainModal("interface.aide_compass")

        faceDirection(Direction.North)
    }

    private fun Player.exitSignpost() {
        // TODO: Investigate when/how this gets sent sometimes.
        // faceDirection(Direction.North)
        camReset(this)
    }
}
