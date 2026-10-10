package org.rsmod.content.other.home

import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.player.hook.PlayerTeleportValidator
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.game.map.collision.isZoneValid
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

internal class HomeTravel @Inject constructor(
    private val validator: PlayerTeleportValidator,
    private val areas: AreaChecker,
    private val collision: CollisionFlagMap,
) : PluginScript() {
    private data class Pending(val destination: CoordGrid, val origin: CoordGrid)
    override fun ScriptContext.startup() {
        onPlayerQueueWithArgs<Pending>(QUEUE) {
            resetSpotanim()
            if (player.hitpoints == 0) return@onPlayerQueueWithArgs
            resetAnim()
            if (player.pendingLogout || coords != it.args.origin || !canTravel() || !landingOpen(it.args.destination)) {
                validator.cancel(player)
                return@onPlayerQueueWithArgs
            }
            telejump(it.args.destination, TeleportType.Standard, onArrival = {
                resetAnim()
                resetSpotanim()
                anim("seq.human_castteleport_reverse")
            })
        }
        onPlayerLogout { validator.cancel(player) }
    }

    suspend fun travel(access: ProtectedAccess, route: TeleportDestination) = with(access) {
        val destination = route.destination ?: run { mes("${route.name}: coming soon."); return@with }
        if (actionDelay > mapClock || player.pendingLogout || player.hitpoints == 0 || !canTravel()) return@with
        val origin = coords
        ifClose()
        if (route.dangerous) {
            val accepted = choice2("Teleport into the Wilderness", true, "Cancel", false,
                title = "Wilderness: other players can attack you and you risk your items.")
            ifCloseChat()
            if (!accepted || coords != origin || player.pendingLogout || player.hitpoints == 0 || !canTravel()) return@with
        }
        if (!landingOpen(destination)) { mes("That destination is currently unavailable."); return@with }
        arriveDelay()
        beginTeleport()
        actionDelay = mapClock + 5
        resetAnim()
        resetSpotanim()
        anim("seq.human_castteleport")
        spotanim("spotanim.teleport_casting", height = 92)
        soundSynth("synth.teleport_all")
        clearQueue(QUEUE)
        queue(QUEUE, 4, Pending(destination, origin))
    }

    private fun ProtectedAccess.canTravel(): Boolean {
        val denial = validator.validate(player, TeleportType.Standard, areas) ?: return true
        mes(denial)
        return false
    }

    private fun landingOpen(tile: CoordGrid): Boolean = collision.isZoneValid(tile) &&
        collision[tile.x, tile.z, tile.level] and BLOCKED == 0

    internal companion object {
        const val QUEUE = "queue.home_teleport"
        const val BLOCKED = CollisionFlag.LOC or CollisionFlag.GROUND_DECOR or CollisionFlag.BLOCK_WALK
    }
}
