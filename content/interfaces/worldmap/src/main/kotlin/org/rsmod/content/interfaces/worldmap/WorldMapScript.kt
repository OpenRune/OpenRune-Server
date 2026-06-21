package org.rsmod.content.interfaces.worldmap

import dev.openrune.types.aconverted.interf.IfButtonOp
import jakarta.inject.Inject
import org.rsmod.api.player.hook.WildernessTeleportType
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onPlayerCoordsChanged
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onWorldMapClick
import org.rsmod.content.interfaces.gameframe.script.GameframeScript.Companion.resolveGameframeMove
import org.rsmod.content.interfaces.gameframe.script.gameframeTopLevel
import org.rsmod.content.interfaces.gameframe.script.gameframes
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class WorldMapScript @Inject constructor(
    private val eventBus: EventBus
) : PluginScript() {

    private var Player.isFullScreenMap by boolVarBit("varbit.fullscreen_worldmap")
    private var Player.orbsMinimized by boolVarBit("varbit.minimap_toggle")

    private val mapInterface = "interface.worldmap"
    private val synthSound = "synth.interface_select"

    private val worldMapOrb = "component.orbs:worldmap"
    private val worldMapClose = "component.worldmap:close"

    private val fullscreenTopLevel = "interface.toplevel_display"

    override fun ScriptContext.startup() {
        onPlayerLogin {
            player.orbsMinimized = false
        }
        onIfOverlayButton(worldMapOrb) { player.openMap(it.op) }
        onIfOverlayButton("component.orbs_nomap:worldmap") { player.openMap(it.op) }

        onIfOverlayButton(worldMapClose) { player.closeMap() }
        onPlayerCoordsChanged { player.runClientScript(1749, player.coords.packed) }
        onWorldMapClick("modlevel.owner") { telejump(it.coord, WildernessTeleportType.Exempt) }
    }

    private fun Player.openMap(option: IfButtonOp) {
        if (ui.contains(mapInterface)) return

        soundSynth(synthSound)

        when (option) {
            IfButtonOp.Op2 -> openMapOverlay()
            IfButtonOp.Op3 -> openFullscreen()
            IfButtonOp.Op4 -> {
                this.ifOpenOverlay(if (orbsMinimized) "interface.orbs" else "interface.orbs_nomap", "component.toplevel_osrs_stretch:orbs", eventBus)
                orbsMinimized = !orbsMinimized
            }
            else -> error("Invalid option on world map: $option")
        }
    }

    private fun Player.closeMap() {
        if (isFullScreenMap) {
            moveGameframe(toFullscreen = false)
            isFullScreenMap = false
            anim("seq.qip_watchtower_stop_reading_scroll")
        }
        ifCloseOverlay(mapInterface, eventBus)
    }

    private fun Player.openMapOverlay() {
        runClientScript(1749, coords.packed)
        ifOpenOverlay(mapInterface, eventBus)
    }

    private fun Player.openFullscreen() {
        anim("seq.qip_watchtower_read_scroll")
        openMapOverlay()
        moveGameframe(toFullscreen = true)
        isFullScreenMap = true
    }

    private fun Player.moveGameframe(toFullscreen: Boolean) {
        val fullscreen = gameframes.values.first { it.topLevel == fullscreenTopLevel }
        val normal = gameframes.getValue(gameframeTopLevel)

        val (from, to) = if (toFullscreen) {
            normal to fullscreen
        } else {
            fullscreen to normal
        }

        val move = resolveGameframeMove(from = from, dest = to)
        softQueue("queue.fullscreen_map", 1, move)

        delay(2)
    }
}
