package org.rsmod.content.skills.sailing

import jakarta.inject.Inject
import org.rsmod.api.player.events.SailingEvent
import org.rsmod.api.player.output.InteractionModes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpLoc1
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class HelmEvents
@Inject
constructor(private val boats: BoatManager) : PluginScript() {
    override fun ScriptContext.startup() {
        for (loc in HELM_IDLE_LOCS) {
            onOpLoc1(loc) { navigate() }
        }
        for (loc in HELM_IN_USE_LOCS) {
            onOpLoc1(loc) { stopNavigating() }
        }
        onEvent<SailingEvent.SetHeading> { steer(player, heading) }
    }

    private fun ProtectedAccess.navigate() {
        val boat = boats.boatOf(player) ?: return
        if (boat.helmsman != null || player.helmLockedIn != 0) {
            return
        }
        boat.helmsman = player
        boat.moveMode = SailingMoveModes.HELM_IDLE
        player.helmLockedIn = LOCKEDIN_NAVIGATING
        InteractionModes.setInteractionMode(
            player,
            InteractionModes.WORLD_DEFAULT,
            InteractionModes.TILE_MODE_HEADING,
            InteractionModes.ENTITY_MODE_ALL,
        )
        InteractionModes.setInteractionMode(
            player,
            boat.entity.slotId,
            InteractionModes.TILE_MODE_WALK,
            InteractionModes.ENTITY_MODE_ALL,
        )
        anim(boat.type.helmPlayerGrab)
        soundSynth(boat.type.helmSynthGrab)
    }

    private fun ProtectedAccess.stopNavigating() {
        val boat = boats.boatOf(player) ?: return
        if (boat.helmsman != player) {
            if (boat.helmsman == null && player.helmLockedIn != 0) {
                player.helmLockedIn = 0
            }
            return
        }
        boats.releaseHelm(player)
        resetAnim()
        soundSynth(boat.type.helmSynthRelease)
    }

    private fun steer(player: Player, heading: Int) {
        val boat = boats.boatOf(player) ?: return
        if (boat.helmsman != player || player.helmLockedIn != LOCKEDIN_NAVIGATING) {
            return
        }
        if (boat.moveMode == SailingMoveModes.HELM_IDLE) {
            boat.moveMode = SailingMoveModes.FULL
            boat.targetSpeed = boat.type.baseSpeed
            boat.targetAngle = boat.entity.angle
            return
        }
        boat.targetAngle = heading * HEADING_TO_ANGLE
    }

    private companion object {
        private const val LOCKEDIN_NAVIGATING = 3
        private const val HEADING_TO_ANGLE = 128

        private val HELM_IDLE_LOCS =
            listOf(
                "loc.sailing_boat_steering_kandarin_1x3_wood_idle",
                "loc.sailing_boat_steering_kandarin_1x3_oak_idle",
                "loc.sailing_boat_steering_kandarin_1x3_teak_idle",
                "loc.sailing_boat_steering_kandarin_1x3_mahogany_idle",
                "loc.sailing_boat_steering_kandarin_1x3_camphor_idle",
                "loc.sailing_boat_steering_kandarin_1x3_ironwood_idle",
                "loc.sailing_boat_steering_kandarin_1x3_rosewood_idle",
                "loc.sailing_boat_steering_kandarin_2x5_wood_idle",
                "loc.sailing_boat_steering_kandarin_2x5_oak_idle",
                "loc.sailing_boat_steering_kandarin_2x5_teak_idle",
                "loc.sailing_boat_steering_kandarin_2x5_mahogany_idle",
                "loc.sailing_boat_steering_kandarin_2x5_camphor_idle",
                "loc.sailing_boat_steering_kandarin_2x5_ironwood_idle",
                "loc.sailing_boat_steering_kandarin_2x5_rosewood_idle",
                "loc.sailing_boat_steering_kandarin_3x8_wood_idle",
                "loc.sailing_boat_steering_kandarin_3x8_oak_idle",
                "loc.sailing_boat_steering_kandarin_3x8_teak_idle",
                "loc.sailing_boat_steering_kandarin_3x8_mahogany_idle",
                "loc.sailing_boat_steering_kandarin_3x8_camphor_idle",
                "loc.sailing_boat_steering_kandarin_3x8_ironwood_idle",
                "loc.sailing_boat_steering_kandarin_3x8_rosewood_idle",
            )

        private val HELM_IN_USE_LOCS =
            listOf(
                "loc.sailing_boat_steering_kandarin_1x3_wood_in_use",
                "loc.sailing_boat_steering_kandarin_1x3_oak_in_use",
                "loc.sailing_boat_steering_kandarin_1x3_teak_in_use",
                "loc.sailing_boat_steering_kandarin_1x3_mahogany_in_use",
                "loc.sailing_boat_steering_kandarin_1x3_camphor_in_use",
                "loc.sailing_boat_steering_kandarin_1x3_ironwood_in_use",
                "loc.sailing_boat_steering_kandarin_1x3_rosewood_in_use",
                "loc.sailing_boat_steering_kandarin_2x5_wood_in_use",
                "loc.sailing_boat_steering_kandarin_2x5_oak_in_use",
                "loc.sailing_boat_steering_kandarin_2x5_teak_in_use",
                "loc.sailing_boat_steering_kandarin_2x5_mahogany_in_use",
                "loc.sailing_boat_steering_kandarin_2x5_camphor_in_use",
                "loc.sailing_boat_steering_kandarin_2x5_ironwood_in_use",
                "loc.sailing_boat_steering_kandarin_2x5_rosewood_in_use",
                "loc.sailing_boat_steering_kandarin_3x8_wood_in_use",
                "loc.sailing_boat_steering_kandarin_3x8_oak_in_use",
                "loc.sailing_boat_steering_kandarin_3x8_teak_in_use",
                "loc.sailing_boat_steering_kandarin_3x8_mahogany_in_use",
                "loc.sailing_boat_steering_kandarin_3x8_camphor_in_use",
                "loc.sailing_boat_steering_kandarin_3x8_ironwood_in_use",
                "loc.sailing_boat_steering_kandarin_3x8_rosewood_in_use",
            )
    }
}
