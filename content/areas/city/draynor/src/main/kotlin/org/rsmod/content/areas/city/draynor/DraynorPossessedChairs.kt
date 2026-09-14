package org.rsmod.content.areas.city.draynor

import jakarta.inject.Inject
import java.util.WeakHashMap
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.player.PlayerRepository
import org.rsmod.api.route.RayCastValidator
import org.rsmod.api.route.RouteFactory
import org.rsmod.api.script.onAiTimer
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class DraynorPossessedChairs @Inject constructor(
    private val players: PlayerRepository,
    private val random: GameRandom,
    private val routes: RouteFactory,
    private val rays: RayCastValidator,
) : PluginScript() {
    private val chairs = WeakHashMap<Npc, Following>()

    override fun ScriptContext.startup() {
        onAiTimer(Chair) { follow(npc) }
    }

    internal fun follow(npc: Npc) {
        if (!inside(npc.spawnCoords) || !npc.isVisible || npc.isBusy) return
        val state = chairs.getOrPut(npc) { Following() }
        if (state.rest > 0) {
            state.rest--
            return
        }
        val target = state.target
        if (target == null) {
            if (!random.randomBoolean(4)) return
            val nearby = players.findAll(ZoneKey.from(npc.coords), 1)
                .filter { eligible(npc, it) }.toList()
            state.target = random.pickOrNull(nearby) ?: return
            state.remaining = random.of(10, 30)
        } else if (--state.remaining <= 0 || !eligible(npc, target)) {
            rest(npc, state)
            return
        }
        val route = routes.create(npc.avatar, checkNotNull(state.target).avatar)
        val waypoints = route.map { CoordGrid(it.x, it.z, it.level) }
        if (!route.success || route.alternative || waypoints.any { !inside(it) }) {
            rest(npc, state)
        } else {
            npc.walk(waypoints)
        }
    }

    private fun eligible(npc: Npc, player: Player): Boolean =
        player.isValidTarget() && inside(player.coords) &&
            npc.level == player.level && npc.isWithinDistance(player, 6) &&
            rays.hasLineOfSight(npc.coords, player.coords)

    private fun rest(npc: Npc, state: Following) {
        npc.resetMovement()
        state.target = null
        state.rest = random.of(8, 20)
    }

    private class Following(var target: Player? = null, var remaining: Int = 0, var rest: Int = 0)

    internal companion object {
        const val Chair = "npc.draynor_possesed_chair"
        fun inside(coords: CoordGrid): Boolean =
            coords.level in 0..1 && coords.x in 3092..3123 && coords.z in 3354..3373
    }
}
