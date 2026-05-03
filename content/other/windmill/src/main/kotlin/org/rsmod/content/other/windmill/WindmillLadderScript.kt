package org.rsmod.content.other.windmill

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class WindmillLadderScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1("loc.qip_cook_ladder_top") { climbDown() }
        onOpLoc1("loc.qip_cook_ladder") { climbUp() }
        onOpLoc1("loc.qip_cook_ladder_middle") { climbOption() }
        onOpLoc2("loc.qip_cook_ladder_middle") { climbUp() }
        onOpLoc3("loc.qip_cook_ladder_middle") { climbDown() }
    }

    private suspend fun ProtectedAccess.climbUp(): Unit = climb(1)

    private suspend fun ProtectedAccess.climbDown(): Unit = climb(-1)

    private suspend fun ProtectedAccess.climb(translateLevel: Int) {
        arriveDelay()
        val dest = player.coords.translateLevel(translateLevel)
        anim("seq.human_reachforladder")
        delay(1)
        telejump(dest)
    }

    private suspend fun ProtectedAccess.climbOption() {
        arriveDelay()
        startDialogue {
            val translate =
                choice2("Climb Up.", 1, "Climb Down.", -1, title = "Climb up or down the ladder?")
            val dest = player.coords.translateLevel(translate)
            anim("seq.human_reachforladder")
            delay(2)
            telejump(dest)
        }
    }
}
