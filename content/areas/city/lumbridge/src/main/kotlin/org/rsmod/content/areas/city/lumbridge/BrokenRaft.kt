package org.rsmod.content.areas.city.lumbridge

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.agilityLvl
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.stat.rangedLvl
import org.rsmod.api.player.stat.strengthLvl
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
        if (player.agilityLvl < 8 || player.rangedLvl < 37 || player.strengthLvl < 19) { mes("You need 8 Agility, 37 Ranged and 19 Strength to grapple across."); return }
        if (!grappleEquipped()) { mes("Equip a crossbow and mith grapple first."); return }
        val origin = coords
        val destination = if (coords.x < 3253) EASTBOUND.last() else WESTBOUND.last()
        var expected = origin
        var finished = false
        try {
            anim("seq.xbows_human_fire_and_climb_grapple")
            delay(2)
            if (!active(origin) || !grappleEquipped() || player.agilityLvl < 8 || player.rangedLvl < 37 || player.strengthLvl < 19) return
            val face = if (destination.x > origin.x) FACE_EAST else FACE_WEST
            exactMove(origin, destination, 0, 90, face, TeleportType.Exempt)
            expected = destination
            delay(3)
            finished = active(destination)
        } finally {
            resetAnim()
            if (!finished && active(expected)) teleport(origin, TeleportType.Exempt)
        }
    }
    private fun ProtectedAccess.grappleEquipped() =
        player.worn[13]?.id == "obj.xbows_grapple_tip_bolt_mithril_rope".asRSCM() &&
            player.worn[3]?.id?.let { ServerCacheManager.getItem(it)?.name?.contains("crossbow", true) } == true

    private suspend fun ProtectedAccess.cross(eastbound: Boolean) {
        val facing = if (eastbound) FACE_EAST else FACE_WEST
        val steps = if (eastbound) EASTBOUND else WESTBOUND
        val origin = coords; var expected = origin; var finished = false
        try {
            for ((index, step) in steps.withIndex()) {
                if (!active(expected))return
                exactMove(coords, step, delay1 = 0, delay2 = if (index % 2 == 0) SHORT_STEP else LONG_STEP, dir = facing, teleportType = TeleportType.Exempt)
                expected = step
                when (index) {
                    steps.lastIndex -> { anim("seq.human_climbing_loop"); soundSynth(CLIMB_SOUND, delay = 5) }
                    else -> if (index % 2 == 0)anim("seq.human_running")else { anim("seq.human_longjump"); soundSynth(JUMP_SOUND) }
                }
                delay(1)
                if (!active(expected))return
            }
            finished = true
        } finally {
            resetAnim()
            if (!finished && active(expected))teleport(origin, TeleportType.Exempt)
        }
    }
    private fun ProtectedAccess.active(position: CoordGrid) = coords == position && player.hitpoints > 0 && !player.pendingLogout && !player.loggingOut

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
