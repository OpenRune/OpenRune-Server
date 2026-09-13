package org.rsmod.content.bosses.zulrah

import dev.or2.central.account.Rights
import jakarta.inject.Inject
import org.rsmod.api.instances.InstanceAccess
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.player.cinematic.Cinematic
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.events.EventBus
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal class ZulrahBoatScript @Inject constructor(
    private val instances: InstanceManager,
    private val encounters: ZulrahEncounterController,
    private val eventBus: EventBus,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1("loc.snakeboss_boat_1op") { board() }
        onOpLoc1("loc.snakeboss_boat_2ops") { board() }
        onOpLoc2("loc.snakeboss_boat_2ops") { board() }
        onOpLoc1("loc.snakeboss_exit") {
            if (encounters.mayUseExit(player, it.loc.coords)) {
                val session = instances.sessionForPlayer(player) ?: return@onOpLoc1
                telejump(instances.leave(player, session, mapClock), TeleportType.Exempt)
            }
        }
    }

    private suspend fun ProtectedAccess.board() {
        arriveDelay()
        if (!player.modLevel.isAtLeast(Rights.ADMINISTRATOR)) return
        if (instances.sessionForPlayer(player) != null) return
        val result = instances.create(
            owner = player,
            key = ZulrahIsland.KEY,
            spec = ZulrahIsland.spec(player.coords),
            access = InstanceAccess.Private,
            currentTick = mapClock,
        )
        val (session, arrival) = when (result) {
            is InstanceManager.Result.Failed -> return
            is InstanceManager.Result.Created -> result.session to result.enter
            is InstanceManager.Result.Joined -> result.session to result.enter
        }
        var continued = false
        var promptReady = false
        try {
            fadeOverlay(0, 255, 0, 0, 50)
            mesboxNp(ROWING_MESSAGE)
            delay(3)
            if (instances.sessionForId(session.id) !== session) return
            telejump(arrival, TeleportType.Exempt)
            instances.finalizeEntry(player, session, mapClock)
            delay(1)
            camForceAngle(280, 1780)
            fadeOverlay(0, 0, 0, 255, 50)
            promptReady = true
            mesbox(ROWING_MESSAGE)
            continued = encounters.continueEntry(player, session.id, mapClock)
            if (continued) closeFadeOverlay(1)
        } finally {
            if (!continued && promptReady) {
                continued = encounters.continueEntry(player, session.id, mapClock)
                if (continued) closeFadeOverlay(1)
            }
            if (!continued) {
                Cinematic.closeFadeOverlay(player, eventBus)
                if (instances.sessionForPlayer(player) === session) {
                    val stillOnIsland = player.coords.level == arrival.level &&
                        player.coords.chebyshevDistance(arrival) <= 64
                    val returnTo = instances.leave(player, session, mapClock)
                    if (stillOnIsland) telejump(returnTo, TeleportType.Exempt)
                } else if (instances.sessionForId(session.id) === session && session.occupants.isEmpty()) {
                    instances.cancelPendingEntry(player, mapClock)
                }
            }
        }
    }

    companion object {
        internal const val ROWING_MESSAGE =
            "The priestess rows you to Zulrah's shrine,<br>then hurriedly paddles away."
    }
}
