package org.rsmod.content.areas.city.lumbridge

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.agilityLvl
import org.rsmod.api.script.onApLoc1
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** The broken raft shortcut across the River Lum, between Lumbridge and Al Kharid. */
class BrokenRaft : PluginScript() {
    override fun ScriptContext.startup() {
        onApLoc1("loc.xbows_raft_tr") { jump(eastbound = true) }
        onApLoc1("loc.xbows_raft_tl") { jump(eastbound = false) }
        onApLoc1("loc.xbows_raft_br") { grapple() }
    }

    private suspend fun ProtectedAccess.jump(eastbound: Boolean) {
        apRange(RAFT_AP_RANGE)
        if (player.agilityLvl < JUMP_AGILITY) {
            mes("You need an Agility level of $JUMP_AGILITY to negotiate this obstacle.")
            return
        }
        cross(eastbound)
    }

    private suspend fun ProtectedAccess.grapple() {
        apRange(RAFT_AP_RANGE)
        mes("You need a mithril grapple tipped bolt with a rope to do that.")
    }

    private suspend fun ProtectedAccess.cross(eastbound: Boolean) {
        val facing = if (eastbound) FACE_EAST else FACE_WEST
        val steps = if (eastbound) EASTBOUND else WESTBOUND
        for ((index, step) in steps.withIndex()) {
            exactMove(coords, step, delay1 = 0, delay2 = if (index % 2 == 0) SHORT_STEP else LONG_STEP, dir = facing)
            when (index) {
                steps.lastIndex -> {
                    anim("seq.human_climbing_loop")
                    soundSynth(CLIMB_SOUND, delay = 5)
                }
                else ->
                    if (index % 2 == 0) {
                        anim("seq.human_running")
                    } else {
                        anim("seq.human_longjump")
                        soundSynth(JUMP_SOUND)
                    }
            }
            delay(1)
        }
        resetAnim()
    }

    private companion object {
        const val JUMP_AGILITY = 48
        const val RAFT_AP_RANGE = 10
        const val SHORT_STEP = 30
        const val LONG_STEP = 60
        const val FACE_EAST = 1536
        const val FACE_WEST = 512

        const val JUMP_SOUND = "synth.raft_jump"
        const val CLIMB_SOUND = "synth.raft_climb"

        val EASTBOUND =
            listOf(
                CoordGrid(3248, 3180, 0),
                CoordGrid(3252, 3180, 0),
                CoordGrid(3253, 3180, 0),
                CoordGrid(3257, 3180, 0),
                CoordGrid(3259, 3180, 0),
            )
        val WESTBOUND =
            listOf(
                CoordGrid(3258, 3180, 0),
                CoordGrid(3253, 3180, 0),
                CoordGrid(3252, 3180, 0),
                CoordGrid(3248, 3180, 0),
                CoordGrid(3246, 3180, 0),
            )
    }
}
