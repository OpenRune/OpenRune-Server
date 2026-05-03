package org.rsmod.content.generic.locs.staircase

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpContentLoc1
import org.rsmod.api.script.onOpContentLoc2
import org.rsmod.api.script.onOpContentLoc3
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.map.util.Translation
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class SpiralStaircaseScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpContentLoc1("content.spiralstaircase_down") { climbDown(it.loc) }
        onOpContentLoc1("content.spiralstaircase_up") { climbUp(it.loc) }
        onOpContentLoc1("content.spiralstaircase_option") { climOption(it.loc) }
        onOpContentLoc2("content.spiralstaircase_option") {
            arriveDelay()
            climbUp(it.loc)
        }
        onOpContentLoc3("content.spiralstaircase_option") {
            arriveDelay()
            climbDown(it.loc)
        }

        onOpLoc2("loc.spiralstairsbottom_3") { climbLumbridgeTop(it.loc) }
        onOpLoc2("loc.spiralstairstop_3") { climbLumbridgeBottom(it.loc) }
    }

    private fun ProtectedAccess.climbDown(loc: BoundLocInfo) =
        climb(loc, loc.climbDownTranslation())

    private fun BoundLocInfo.climbDownTranslation(): Translation =
        when (angle) {
            LocAngle.West -> Translation(x = adjustedWidth - 1, z = -1, level = -1)
            LocAngle.North -> Translation(x = -adjustedWidth, z = 0, level = -1)
            LocAngle.East -> Translation(x = 0, z = adjustedLength, level = -1)
            LocAngle.South -> Translation(x = adjustedWidth, z = adjustedLength - 1, level = -1)
        }

    private fun ProtectedAccess.climbUp(loc: BoundLocInfo) = climb(loc, loc.climbUpTranslation())

    private fun BoundLocInfo.climbUpTranslation(): Translation =
        when (angle) {
            LocAngle.West -> Translation(x = adjustedWidth, z = 0, level = 1)
            LocAngle.North -> Translation(x = 0, z = -(adjustedLength - 1), level = 1)
            LocAngle.East ->
                Translation(x = -(adjustedWidth - 1), z = adjustedLength - 1, level = 1)
            LocAngle.South -> Translation(x = adjustedWidth - 1, z = adjustedLength, level = 1)
        }

    private fun ProtectedAccess.climb(loc: BoundLocInfo, translation: Translation) {
        val dest = loc.coords.translate(translation)
        telejump(dest)
    }

    private suspend fun ProtectedAccess.climOption(loc: BoundLocInfo) {
        startDialogue {
            val translation =
                choice2(
                    "Climb up the stairs.",
                    loc.climbUpTranslation(),
                    "Climb down the stairs.",
                    loc.climbDownTranslation(),
                    title = "Climb up or down the stairs?",
                )
            val dest = loc.coords.translate(translation)
            telejump(dest)
        }
    }

    private fun ProtectedAccess.climbLumbridgeTop(loc: BoundLocInfo) {
        climb(loc, loc.climbUpTranslation().copy(level = 2))
    }

    private fun ProtectedAccess.climbLumbridgeBottom(loc: BoundLocInfo) {
        climb(loc, loc.climbDownTranslation().copy(level = -2))
    }
}
