package org.rsmod.content.areas.city.lumbridge

import jakarta.inject.Inject
import org.rsmod.api.player.cinematic.Cinematic
import org.rsmod.api.player.cinematic.MinimapState
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private var Player.darkness by intVarBit("varbit.darkness_level")

class LumbridgeSwampLocs @Inject constructor(private val locRepo: LocRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1("loc.goblin_cave_entrance") { enterCave() }
        onIfModalButton("component.cws_warning_13:warn1") {
            ifClose()
            descend(dark = !hasLight())
        }
        onIfModalButton("component.cws_warning_13:warn2") { ifClose() }
        onOpLoc1("loc.swamp_cave_climbing_rope") { climbRope() }
        onPlayerSoftTimer(INSECT_TIMER) { player.insectWarning() }
        onOpLoc1("loc.ham_multi_trapdoor") {
            arriveDelay()
            mes("You try to open the trap door.", ChatType.Spam)
            mes("This trapdoor seems totally locked.", ChatType.Spam)
            soundSynth("synth.locked")
        }
        onOpLoc1("loc.zanarisdoor") { shedDoor(it.loc) }
    }

    private fun ProtectedAccess.hasLight(): Boolean =
        LIGHT_SOURCES.any { inv.count(it) > 0 || player.worn.count(it) > 0 }

    private suspend fun ProtectedAccess.enterCave() {
        arriveDelay()
        val light = hasLight()
        if (inv.count(TINDERBOX) > 0) {
            descend(dark = !light)
            return
        }
        ifOpenMainModal(WARNING)
        ifSetHide("component.cws_warning_13:cws_warning_layer", hide = false)
        if (!light) {
            ifSetText(
                "component.cws_warning_13:cws_test_warning",
                "The cave is very dark and you don't have a light source or a tinderbox. Are you sure " +
                    "you want to go down?",
            )
        }
    }

    private fun ProtectedAccess.descend(dark: Boolean) {
        telejump(CAVE_LANDING)
        if (!dark) return
        player.darkness = DARK
        Cinematic.setMinimapState(player, MinimapState.MinimapHidden)
        ifOpenFullOverlay(DARKNESS_OVERLAY)
        softTimer(INSECT_TIMER, INSECT_WARNING_TICKS)
    }

    private suspend fun ProtectedAccess.climbRope() {
        arriveDelay()
        anim("seq.human_reachforladder")
        delay(1)
        if (player.darkness != 0) {
            player.darkness = 0
            Cinematic.setMinimapState(player, MinimapState.Normal)
            ifCloseSub(DARKNESS_OVERLAY)
        }
        clearSoftTimer(INSECT_TIMER)
        telejump(SWAMP_EXIT)
    }

    private fun Player.insectWarning() {
        clearSoftTimer(INSECT_TIMER)
        if (darkness == 0) return
        mes("You hear tiny insects skittering over the ground...")
        soundSynth("synth.swamp_cave_insects")
    }

    private suspend fun ProtectedAccess.shedDoor(door: BoundLocInfo) {
        arriveDelay()
        val inside = player.coords.x >= door.coords.x
        if (!inside && ZANARIS_STAVES.any { player.worn.count(it) > 0 }) {
            telejump(ZANARIS)
            return
        }
        locRepo.del(door, DOOR_TICKS)
        locRepo.add(SHED_DOOR_OPEN, "loc.zanarisdoor_open", DOOR_TICKS, LocAngle.North, LocShape.WallStraight)
        soundSynth("synth.door_open")
        val dest = if (inside) door.coords.translateX(-1) else door.coords
        playerWalkWithMinDelay(dest)
    }

    private companion object {
        const val WARNING = "interface.cws_warning_13"
        const val DARKNESS_OVERLAY = "interface.darkness_dark"
        const val INSECT_TIMER = "timer.swamp_cave_insects"
        const val INSECT_WARNING_TICKS = 16
        const val DARK = 3
        const val DOOR_TICKS = 2
        const val TINDERBOX = "obj.tinderbox"

        val CAVE_LANDING = CoordGrid(3167, 9573, 0)
        val SWAMP_EXIT = CoordGrid(3168, 3172, 0)
        val SHED_DOOR_OPEN = CoordGrid(3201, 3169, 0)
        val ZANARIS = CoordGrid(2412, 4434, 0)

        val ZANARIS_STAVES = listOf("obj.dramen_staff", "obj.lunar_moonclan_liminal_staff")
        val LIGHT_SOURCES =
            listOf(
                "obj.lit_candle",
                "obj.lit_black_candle",
                "obj.torch_lit",
                "obj.candle_lantern_lit",
                "obj.candle_lantern_black_lit",
                "obj.oil_lamp_lit",
                "obj.oil_lantern_lit",
                "obj.bullseye_lantern_lit",
                "obj.cave_goblin_mining_helmet_lit",
            )
    }
}
